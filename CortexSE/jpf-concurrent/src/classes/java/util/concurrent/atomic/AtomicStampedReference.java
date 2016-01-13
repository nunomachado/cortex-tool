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
 * Non MJI level class for modeling AtomicStampedReference.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicStampedReference<V> {

  private static class ReferenceIntegerPair<T> {

    private final T reference;
    private final int integer;

    ReferenceIntegerPair(T r, int i) {
      reference = r;
      integer = i;
    }
  }
  private final AtomicReference<ReferenceIntegerPair<V>> atomicRef = null;

  public AtomicStampedReference(V initialRef, int initialStamp) {
  }

  public native V getReference();

  public native int getStamp();

  public native V get(int[] stampHolder);

  public native boolean weakCompareAndSet(V expectedReference,
          V newReference,
          int expectedStamp,
          int newStamp);

  public native boolean compareAndSet(V expectedReference,
          V newReference,
          int expectedStamp,
          int newStamp);

  public native void set(V newReference, int newStamp);

  public native boolean attemptStamp(V expectedReference, int newStamp);
}
