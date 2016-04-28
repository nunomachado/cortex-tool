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
 * Non MJI level class for modeling AtomicIntegerFieldUpdater.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public abstract class AtomicIntegerFieldUpdater<T> {

    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl<U>(tclass, fieldName);
    }

    protected AtomicIntegerFieldUpdater() {
    }

    public abstract boolean compareAndSet(T obj, int expect, int update);

    public abstract boolean weakCompareAndSet(T obj, int expect, int update);

    public abstract void set(T obj, int newValue);

    public abstract void lazySet(T obj, int newValue);

    public abstract int get(T obj);

    public native int getAndSet(T obj, int newValue);

    public native int getAndIncrement(T obj);
    
    public native int getAndDecrement(T obj);

    public native int getAndAdd(T obj, int delta);

    public native int incrementAndGet(T obj);

    public native int decrementAndGet(T obj);

    public native int addAndGet(T obj, int delta);

    private static class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
        private final long offset = 0;

        AtomicIntegerFieldUpdaterImpl(Class<T> tclass, String fieldName) {
        }

        public native boolean compareAndSet(T obj, int expect, int update);

        public native boolean weakCompareAndSet(T obj, int expect, int update);

        public native void set(T obj, int newValue);

        public native void lazySet(T obj, int newValue);

        public native final int get(T obj);

    }
}
