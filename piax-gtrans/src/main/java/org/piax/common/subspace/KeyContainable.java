/*
 * KeyContainable.java - An interface of a key that contains a key
 * 
 * Copyright (c) 2012-2015 National Institute of Information and 
 * Communications Technology
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIAX package for more in detail.
 *
 * $Id: KeyContainable.java 1172 2015-05-18 14:31:59Z teranisi $
 */

package org.piax.common.subspace;

import org.piax.common.Destination;
import org.piax.common.Key;

/**
 * An interface of a key that contains a key
 */
public interface KeyContainable<K extends Key> extends Destination {
    boolean contains(K key);
}
