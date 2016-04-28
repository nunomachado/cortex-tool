//
// Copyright (C) 2010 United States Government as represented by the
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

package gov.nasa.jpf.classfile;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.JPFLogger;

import java.io.File;
import java.util.ArrayList;

/**
 * this is a lookup mechanism for class files that is based on an ordered
 * list of directory or jar entries
 */
public class ClassPath {

  public static class Match {
    public final byte[] data;
    public final ClassFileContainer container;
    
    Match (ClassFileContainer c, byte[] d){
      container = c;
      data = d;
    }
    
    public byte[] getBytes() {
      return data;
    }
  }
  
  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.classfile");
  
  ArrayList<ClassFileContainer> pathElements;


  public ClassPath(){
    pathElements = new ArrayList<ClassFileContainer>();
  }

  public ClassPath (String[] pathNames) {
    pathElements = new ArrayList<ClassFileContainer>();

    for (String e : pathNames){
      addPathName(e);
    }
  }

  public void addPathName(String pathName){
    ClassFileContainer pe = ClassFileContainer.getClassFileContainer(pathName);

    if (pe != null) {
      pathElements.add(pe);
    } else {
      // would like to turn this into a warning, but the java.class.path at least
      // on OS X 10.6 contains non-existing elements
      logger.info("illegal classpath element ", pathName);
    }
  }

  public String[] getPathNames(){
    String[] pn = new String[pathElements.size()];

    for (int i=0; i<pn.length; i++){
      pn[i] = pathElements.get(i).getName();
    }

    return pn;
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    int len = pathElements.size();
    int i=0;

    for (ClassFileContainer e : pathElements){
      sb.append(e.getName());
      if (++i < len){
        sb.append(File.pathSeparator);
      }
    }
    return sb.toString();
  }

  protected static void error(String msg) throws ClassFileException {
    throw new ClassFileException(msg);
  }

  public Match findMatch (String clsName) throws ClassFileException {
    for (ClassFileContainer e : pathElements){
      byte[] data = e.getClassData(clsName);
      if (data != null){
        logger.fine("loading ", clsName, " from ", e.getName());
        return new Match( e, data);
      }
    }

    return null;    
  }

  public byte[] getClassData(String clsName) throws ClassFileException {
    for (ClassFileContainer e : pathElements){
      byte[] data = e.getClassData(clsName);
      if (data != null){
        logger.fine("loading ", clsName, " from ", e.getName());
        return data;
      }
    }

    return null;
  }

  public static void main(String[] args){
    String[] pe = args[0].split(":");

    long t1 = System.currentTimeMillis();
    ClassPath cp = new ClassPath(pe);

    for (int i=0; i<2000; i++){
      try {
        byte[] b = cp.getClassData(args[1]);
        if (b != null){
          //System.out.println("found classfile: " + b.length);
        }

      } catch (ClassFileException cfx) {
        cfx.printStackTrace();
      }
    }

    long t2 = System.currentTimeMillis();
    System.out.println("elapsed time: " + (t2 - t1));
  }

}