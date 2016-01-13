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
 * This class handles version for ConcurrentHashMapVersion objects.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class ConcurrentHashMapVersion extends Version {

  /*
   * Using concurrent version of HashMap seems to kill performance a little bit (JPF runs in one thread)
   * but it really simplifies writing the model class because most the time we will be delegating calls to this map.
   */
  private java.util.concurrent.ConcurrentHashMap<Integer, Integer> map = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();

  public ConcurrentHashMapVersion() {
    super();
  }

  public ConcurrentHashMapVersion(Version version) {
    internalCopy(version);
  }

  public java.util.concurrent.ConcurrentHashMap<Integer, Integer> getMap() {
    return map;
  }

  public void setMap(java.util.concurrent.ConcurrentHashMap<Integer, Integer> m) {
    map = m;
  }

  public boolean equals(Object o) {
    if(!(o instanceof Version)) return false;
    Version version = (Version)o;
    if(!super.equals(version)) return false;
    ConcurrentHashMapVersion version2 = (ConcurrentHashMapVersion) version;
    return map.equals(version2.getMap());
  }

  public int hashCode() {
    return super.hashCode() + map.hashCode();
  }

  protected void internalCopy(Version version) {
    super.internalCopy(version);
    ConcurrentHashMapVersion version2 = (ConcurrentHashMapVersion) version;
    map = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>(version2.getMap());
  }
}
