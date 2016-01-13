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
 * Non MJI level class for modeling AtomicReferenceFieldUpdater.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public abstract class AtomicReferenceFieldUpdater<T, V> {

  public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
    return new AtomicReferenceFieldUpdaterImpl<U, W>(tclass,
            vclass,
            fieldName);
  }

  protected AtomicReferenceFieldUpdater() {
  }

  public abstract boolean compareAndSet(T obj, V expect, V update);

  public abstract boolean weakCompareAndSet(T obj, V expect, V update);

  public abstract void set(T obj, V newValue);

  public abstract void lazySet(T obj, V newValue);

  public abstract V get(T obj);

  public native V getAndSet(T obj, V newValue);

  private static final class AtomicReferenceFieldUpdaterImpl<T, V>
          extends AtomicReferenceFieldUpdater<T, V> {

    private final long offset = 0;

    AtomicReferenceFieldUpdaterImpl(Class<T> tclass,
            Class<V> vclass,
            String fieldName) {
    }

    public native boolean compareAndSet(T obj, V expect, V update);

    public native boolean weakCompareAndSet(T obj, V expect, V update);

    public native void set(T obj, V newValue);

    public native void lazySet(T obj, V newValue);

    public native V get(T obj);
  }
}
