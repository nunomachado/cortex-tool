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
package gov.nasa.jpf.concurrent.version;

/*
 * Base for classes that works with threads and need to provide fair,unfair
 * lock obtaining policy.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public abstract class FairnessVersion extends ThreadVersion{

  protected boolean fair = false;

  public FairnessVersion () {
    super();
  }

  public FairnessVersion (Version version) {
    internalCopy(version);
  }

  public void setFair(boolean f) {
    fair = f;
  }

  public boolean isFair() {
    return fair;
  }

  public boolean equals (Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    FairnessVersion version2 = (FairnessVersion)version;
    if(fair  != version2.isFair()) return false;
    return true;
  }

  public int hashCode() {
    return super.hashCode() + (fair ? 1 : 0);
  }

  protected void internalCopy (Version version) {
    super.internalCopy(version);
    FairnessVersion version2 = (FairnessVersion)version;
    fair = version2.isFair();
  }
}
