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
 * Non MJI level class for modeling AtomicMarkableReference.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicMarkableReference<V> {

  private static class ReferenceBooleanPair<T> {

    private final T reference;
    private final boolean bit;

    ReferenceBooleanPair(T r, boolean i) {
      reference = r;
      bit = i;
    }
  }
  private final AtomicReference<ReferenceBooleanPair<V>> atomicRef = null;

  public AtomicMarkableReference(V initialRef, boolean initialMark) {
  }

  public native V getReference();

  public native boolean isMarked();

  public native V get(boolean[] markHolder);

  public native boolean weakCompareAndSet(V expectedReference,
          V newReference,
          boolean expectedMark,
          boolean newMark);

  public native boolean compareAndSet(V expectedReference,
          V newReference,
          boolean expectedMark,
          boolean newMark);

  public native void set(V newReference, boolean newMark);

  public native boolean attemptMark(V expectedReference, boolean newMark);
}
