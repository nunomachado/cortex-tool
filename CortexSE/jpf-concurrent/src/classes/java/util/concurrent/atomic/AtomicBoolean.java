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
 * Non MJI level class for modeling AtomicBoolean.
 * We need to have this classes even if they have only native methods,
 * because we need to override implementation from jpf-core.
 * 
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class AtomicBoolean implements java.io.Serializable {

  private static final long serialVersionUID = 4654671469794556979L;
  private volatile boolean value;

  public AtomicBoolean(boolean initialValue) {
  }

  public AtomicBoolean() {
  }

  public native final boolean get();

  public native final boolean compareAndSet(boolean expect, boolean update);

  public native boolean weakCompareAndSet(boolean expect, boolean update);

  public native final void set(boolean newValue);

  public native final void lazySet(boolean newValue);

  public native final boolean getAndSet(boolean newValue);

  public native String toString();
}
