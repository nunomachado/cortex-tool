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
 * Base for classes that handles version, contains only methods that sets
 * and retrieve version id.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public abstract class Version {

  protected int id = 0;

  protected int state = 0;

  public Version () {
  }

  public int getId () {
    return id;
  }

  public void setId (int id) {
    this.id = id;
  }

  public int getState() {
    return state;
  }

  public void setState(int newState) {
    state = newState;
  }

  public boolean equals (Object o) {
    if(o == null) return false;
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(state != version.getState()) return false;
    return true;
  }

  public int hashCode() {
    return state;
  }

  protected void internalCopy (Version version) {
    id = version.getId();
    state = version.getState();
  }
}
