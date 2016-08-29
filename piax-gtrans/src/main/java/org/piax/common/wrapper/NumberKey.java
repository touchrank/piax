/*
 * NumberKey.java - A key wrapper of number type.
 * 
 * Copyright (c) 2012-2015 National Institute of Information and 
 * Communications Technology
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIAX package for more in detail.
 *
 * $Id: NumberKey.java 1172 2015-05-18 14:31:59Z teranisi $
 */

package org.piax.common.wrapper;

/**
 * A key wrapper of number type.
 */
 public class NumberKey<N extends Number & Comparable<?>> extends
        WrappedComparableKeyImpl<N> {
    private static final long serialVersionUID = 1L;

    public NumberKey(N key) {
        super(key);
    }
}
