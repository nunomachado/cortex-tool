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
 * Non MJI level class for modeling AtomicInteger.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicInteger implements java.io.Serializable {

  private static final long serialVersionUID = 6214790243416807050L;
  private volatile int value;

  public AtomicInteger(int initialValue) {
  }

  public AtomicInteger() {
  }

  public native final int get();

  public native final void set(int newValue);

  public native final void lazySet(int newValue);

  public native final int getAndSet(int newValue);

  public native final boolean compareAndSet(int expect, int update);

  public native final boolean weakCompareAndSet(int expect, int update);

  public native final int getAndIncrement();

  public native final int getAndDecrement();

  public native final int getAndAdd(int delta);

  public native final int incrementAndGet();

  public native final int decrementAndGet();

  public native final int addAndGet(int delta);

  public native String toString();

  public native int intValue();

  public native long longValue();

  public native float floatValue();

  public native double doubleValue();
}
