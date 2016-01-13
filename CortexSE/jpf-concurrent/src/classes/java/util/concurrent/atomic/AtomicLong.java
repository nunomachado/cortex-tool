//
// Copyright (C) 2008 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package java.util.concurrent.atomic;

/*
 * Non MJI level class for modeling AtomicLong.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicLong extends Number implements java.io.Serializable {

    private static final long serialVersionUID = 1927816293512124184L;

    private volatile long value;

    static final boolean VM_SUPPORTS_LONG_CAS = false;

    public AtomicLong(long initialValue) {
    }

    public AtomicLong() {
    }

    public native final long get();

    public native final void set(long newValue);

    public native final void lazySet(long newValue);

    public native final long getAndSet(long newValue);

    public native final boolean compareAndSet(long expect, long update);

    public native final boolean weakCompareAndSet(long expect, long update);

    public native final long getAndIncrement();

    public native final long getAndDecrement();

    public native final long getAndAdd(long delta);

    public native final long incrementAndGet();

    public native final long decrementAndGet();

    public native final long addAndGet(long delta);

    public native String toString();

    public native int intValue();

    public native long longValue();

    public native float floatValue();

    public native double doubleValue();
}
