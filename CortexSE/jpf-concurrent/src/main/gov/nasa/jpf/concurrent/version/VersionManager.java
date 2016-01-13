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

import gov.nasa.jpf.concurrent.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * This class manages version objects for each non-MJI level object.
 * In short it saves and retrieves from map given version. In non-MJI level
 * versions are represented by integer values.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */

public class VersionManager {

  private Map<Integer, Version> versionMap = new HashMap<Integer, Version>();

  public Version saveVersion (Version v) {
    Version v2 = getVersionIfSeen(v);
    if (v2 == null) {
      v.setId(versionMap.size());
      versionMap.put(v.getId(), v);
      return v;
    } else {
      return v2;
    }
  }

  protected Version getVersionIfSeen (Version v) {
    for (int i = 0; i < versionMap.size(); i++) {
      if (v.equals(versionMap.get(i))) return versionMap.get(i);
    }
    return null;
  }

  public Version getVersion (int version, Model model) {
    Version v = versionMap.get(version);
    if (v == null) {
      v = model.newVersionInstance();
      v.setId(versionMap.size());
      versionMap.put(version, v);
    }
    v = model.newVersionInstance(v);
    return v;
  }

  public VersionManager doClone(Model m) {
    VersionManager newVersionManager = new VersionManager();
    Set<Entry<Integer,Version>> s = versionMap.entrySet();
    Iterator<Entry<Integer,Version>> i = s.iterator();
    Map<Integer,Version> newVersionMap = new HashMap<Integer,Version>();
    while(i.hasNext()) {
      Entry<Integer,Version> e = i.next();
      newVersionMap.put(e.getKey(), m.newVersionInstance(e.getValue()));
    }
    newVersionManager.versionMap = newVersionMap;
    return newVersionManager;
  }

  public boolean equals(Object o) {
    if(o == null) return false;
    if(!(o instanceof VersionManager)) return false;
    VersionManager v = (VersionManager)o;
    for(int i=0;i < versionMap.size();i++) {
      if(!versionMap.get(i).equals(v.versionMap.get(i))) return false;
    }
    return true;
  }

  public int hashCode() {
    return versionMap.hashCode();
  }

}
