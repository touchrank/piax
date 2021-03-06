/*
 * NeighborSet.java - NeighborSet implementation of DDLL.
 * 
 * Copyright (c) 2009-2015 Kota Abe / PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIAX package for more in detail.
 *
 * $Id: NeighborSet.java 1172 2015-05-18 14:31:59Z teranisi $
 */

package org.piax.gtrans.ov.ddll;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.piax.gtrans.RPCException;
import org.piax.util.KeyComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a class for implementing a (left) neighbor set.
 */
public class NeighborSet {
    /*
     * (NEIGHBOR_SET_SIZE = 4 case)
     * 
     * inserting case
     *   0 1 2   4 5 (initially 4 knows {2 1 0 5})
     *   0 1 2 3 4 5 (3 is inserted)
     *               3 recvs {2 1 0 5} via SetRAck
     *               3 sends {3 2 1 0} to node 4
     *               4 sends {4 3 2 1} to node 5
     *
     * deleting case
     *   0 1 2 3 4 5 (initially 4 knows {3 2 1 0})
     *   0 1 2   4 5 (3 is deleted)
     *               when 2 receives SetR, 2 sends {2 1 0 5} to node 4
     *               4 sends {4 2 1 0} to node 5
     *
     * failure-recovery case
     *   0 1 2 3  (initially 3 knows {2 1 0}
     *  [0]1 2 3  (0 is failed)
     *            when 3 receives SetR, 3 sends {3 2} to node 1
     *            (node 1 and 0 should be removed from the forwarding set)
     * 
     * TODO: improve multiple keys on a single node case
     *   0 1 2 3 4 5 6 (0=1=2, 3=4=5, 6)
     *               5, 4 and 3 know {2 6}
     *               2, 1 and 0 know {6 5}
     *               6 knows {5 2}
     *   0 1 2 3 4 5 6 (0=2=4, 1=3=5, 6)
     *               Node0=2=4 knows {Node6 Node1=3=5}
     *               Node1=3=5 knows {Node0=2=4 Node6}
     */
    /*--- logger ---*/
    private static final Logger logger = 
        LoggerFactory.getLogger(NeighborSet.class);

    private static int DEFAULT_NEIGHBOR_SET_SIZE = 6;
    final int capacity;
    final private Link me;
    final private DdllKey key;
    final private NodeManager manager;
    private static KeyComparator keyComp = KeyComparator.getInstance();
    // 最後に送った右ノード
    private Link prevRight;
    // 最後に prevRight に送った集合
    private Set<Link> prevset;

    /**
     * 左側近隣ノード集合．
     * 例えばノード4のleftNbrSetは {3, 0, 95, 90, 88, 4}のような順序で並ぶ．
     */
    private ConcurrentSkipListSet<Link> leftNbrSet
        = new ConcurrentSkipListSet<Link>(new LinkComparator());
    
    @SuppressWarnings("serial")
    class LinkComparator implements Comparator<Link>, Serializable {
        // o1 の方が前に来るならば負の数を返す．
        public int compare(Link o1, Link o2) {
            int c = keyComp.compare(o2.key, o1.key);
            if (c == 0) {
                return 0;
            }
            if (keyComp.compare(o1.key, key) < 0) {
                // o1 < key
                if (keyComp.compare(o2.key, key) < 0) {
                    // o1 < key && o2 < key
                    return c;
                }
                // o1 < key < o2
                return -1;  // o1の方が前
            }
            if (keyComp.compare(o2.key, key) < 0) {
                // o2 < key < o1
                return +1;  // o2の方が前
            }
            return c;
        }
    }

    NeighborSet(Link me, NodeManager manager, int capacity) {
        this.me = me;
        this.key = me.key;
        this.manager = manager;
        this.capacity = capacity;
    }

    NeighborSet(Link me, NodeManager manager) {
        this(me, manager, DEFAULT_NEIGHBOR_SET_SIZE);
    }

    @Override
    public String toString() {
        return leftNbrSet.toString();
    }
    
    public static void setDefaultNeighborSetSize(int size) {
        DEFAULT_NEIGHBOR_SET_SIZE = size;
    }

    public static int getDefaultNeighborSetSize() {
        return DEFAULT_NEIGHBOR_SET_SIZE;
    }

    /**
     * 指定されたノード集合を近隣ノード集合とする．
     * マージするのではなく，入れ替えることに注意．
     * @param nbrs 新しい近隣ノード集合
     */
    void set(Set<Link> nbrs) {
//        String b = null;
//        if (logger.isDebugEnabled()) {
//            b = leftNbrSet.toString();
//        }
        leftNbrSet.clear();
        leftNbrSet.addAll(nbrs);
//        logger.debug("set: {} -> {}", b, leftNbrSet);
    }

    void setPrevRightSet(Link prevRight, Set<Link> nset) {
        this.prevRight = prevRight;
        this.prevset = nset;
    }

    /**
     * 近隣ノード集合に n を追加する．
     * ただし，近隣ノード集合の大きさが capacity を超える場合には n が入らないこともある．
     * 
     * @param n 追加するノード
     */
    void add(Link n) {
        addAll(Collections.singleton(n));
    }

    void addAll(Collection<Link> nodes) {
        NavigableSet<Link> propset = leftNbrSet.clone();
        propset.addAll(nodes);
        while (propset.size() > capacity) {
            propset.remove(propset.last());
        }
        set(propset);
    }

    Set<Link> getNeighbors() {
        return leftNbrSet;
    }

    /**
     **/
    void removeNode(Link removed) {
        leftNbrSet.remove(removed);
        logger.debug("removeNode: {} is removed", removed);
    }
    
    /**
     **/
    void removeNodes(Collection<Link> toRemove) {
        leftNbrSet.removeAll(toRemove);
    }

    /**
     * 左ノードから新しい近隣ノード集合を受信した場合に呼ばれる．
     *
     * @param newset    左ノードから受信した近隣ノード集合
     * @param right     次に転送する右ノード
     * @param limit     限界
     */
    synchronized void receiveNeighbors(DdllKey src, Set<Link> newset,
            Link right, DdllKey limit) {
        set(newset);
        sendRight(src, right, limit);
    }

    /**
     * 右ノードに対して近隣ノード集合を送信する．
     * 右ノードが limit と等しいか，limit を超える場合は送信しない．
     * 送信する近隣ノード集合は，自ノードの近隣ノード集合に自分自身を加えたものである．
     * 
     * @param right     右ノード
     * @param limit     送信する限界キー．
     */
    synchronized void sendRight(DdllKey src, Link right, DdllKey limit) {
        if (Node.isOrdered(key, limit, right.key)) {
            logger.debug("right node {} reached to the limit {}", right, limit);
            return;
        }
        Set<Link> propset = computeNSForRight(right);
        if (right.equals(prevRight)) {
            logger.debug("me={}, newset={}, prevset={}", me, propset, prevset);
            if (propset.equals(prevset)) {
                return;
            }
        } else {
            logger.debug("right={}, prevRight={}", right, prevRight);
        }

        // return if neighbor set size == 0 (for experiments)
        if (propset.size() == 0) {
            return;
        }

        // propagate to the immediate right node
        logger.debug("propagate to right (src={}, right={}, set={})",
                src, right, propset);
        NodeManagerIf stub = manager.getStub(right.addr);
        try {
            stub.propagateNeighbors(src, right.key, propset, limit);
        } catch (RPCException e) {
            logger.info("", e);
        }
        prevRight = right;
        prevset = propset;
    }

    /**
     * 右ノードに送信するノード集合を計算する．
     *
     * @param right 右ノード
     * @return 右ノードに送信するノード集合．
     */
    Set<Link> computeNSForRight(Link right) {
        NeighborSet ns = new NeighborSet(right, null);
        //ns.addAll(leftNbrSet);
        // copy my neighbor set except links between me and my right node
        for (Link t : leftNbrSet) {
            if (!keyComp.isOrdered(me.key, t.key, right.key)) {
                ns.add(t);
            }
        }
        ns.add(me);
        // create a copy of ns because ns has a reference to our
        // customized Comparator, which has a reference to NeighborSet.
        Set<Link> copy = new HashSet<Link>();
        copy.addAll(ns.leftNbrSet);
        //copy.remove(right);     // remove the right node if exists
        return copy;
    }

    /*
    public static void main(String[] args) {
        Endpoint loc
            = new TcpLocator(new InetSocketAddress("127.0.0.1", 8080));
        Link me = new Link(loc, new DdllKey(10, UniqId.MINUS_INFINITY));
        NeighborSet ln = new NeighborSet(me, null);
        for (int k : new int[]{5, 3, 21, 8, 1, 10}) {
            ln.add(new Link(loc, new DdllKey(k, UniqId.MINUS_INFINITY)));
        }
        System.out.println(ln.leftNbrSet);
    }
    */
}
