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
 * Non MJI level class for modeling AtomicLongFieldUpdater.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public abstract class AtomicLongFieldUpdater<T> {

  public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
      return new LockedUpdater<U>(tclass, fieldName);
  }

  protected AtomicLongFieldUpdater() {
  }

  public abstract boolean compareAndSet(T obj, long expect, long update);

  public abstract boolean weakCompareAndSet(T obj, long expect, long update);

  public abstract void set(T obj, long newValue);

  public abstract void lazySet(T obj, long newValue);

  public abstract long get(T obj);

  public native long getAndSet(T obj, long newValue);

  public native long getAndIncrement(T obj);

  public native long getAndDecrement(T obj);

  public native long getAndAdd(T obj, long delta);

  public native long incrementAndGet(T obj);

  public native long decrementAndGet(T obj);

  public native long addAndGet(T obj, long delta);

  private static class LockedUpdater<T> extends AtomicLongFieldUpdater<T> {

    private final long offset = 0;

    LockedUpdater(Class<T> tclass, String fieldName) {
    }

    public native boolean compareAndSet(T obj, long expect, long update);

    public native boolean weakCompareAndSet(T obj, long expect, long update);

    public native void set(T obj, long newValue);

    public native void lazySet(T obj, long newValue);

    public native long get(T obj);

  }
}
