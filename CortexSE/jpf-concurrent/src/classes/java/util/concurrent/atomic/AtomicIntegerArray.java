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
 * Non MJI level class for modeling AtomicIntegerArray.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicIntegerArray implements java.io.Serializable {

  private static final long serialVersionUID = 2862133569453604235L;
  private final int[] array = null;

  public AtomicIntegerArray(int length) {
  }

  public AtomicIntegerArray(int[] array) {
  }

  public native final int length();

  public native final int get(int i);

  public native final void set(int i, int newValue);

  public native final void lazySet(int i, int newValue);

  public native final int getAndSet(int i, int newValue);

  public native final boolean compareAndSet(int i, int expect, int update);

  public native final boolean weakCompareAndSet(int i, int expect, int update);

  public native final int getAndIncrement(int i);

  public native final int getAndDecrement(int i);

  public native final int getAndAdd(int i, int delta);

  public native final int incrementAndGet(int i);

  public native final int decrementAndGet(int i);

  public native final int addAndGet(int i, int delta);

  public native String toString();
}
