//
// Copyright (C) 2011 United States Government as represented by the
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * abstract class that represents the source of a classfile, such
 * as (root) directories and jars
 */
public abstract class ClassFileContainer {

  String name;

  protected ClassFileContainer(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract byte[] getClassData(String clsName) throws ClassFileException;

  protected void readFully(InputStream is, byte[] buf) throws ClassFileException {
    try {
      int nRead = 0;

      while (nRead < buf.length) {
        int n = is.read(buf, nRead, (buf.length - nRead));
        if (n < 0) {
          error("premature end of classfile: " + buf.length + '/' + nRead);
        }
        nRead += n;
      }

    } catch (IOException iox) {
      error("failed to read classfile");
    }
  }
  
  protected static void error(String msg) throws ClassFileException {
    throw new ClassFileException(msg);
  }
  
  /**
   * our factory method 
   */
  public static ClassFileContainer getClassFileContainer (String spec){
    
    int i = spec.indexOf(".jar");
    
    if (i > 0){ // its a jar
      int j = i+4;
      int len = spec.length();
      String jarPath;
      String pathPrefix = null;
      File jarFile;
      
      if (j == len){  // no path prefix, plain jar
        jarPath = spec;
        
      } else { 
        if (spec.charAt(j) == '/'){
          pathPrefix = spec.substring(j);
          jarPath = spec.substring(0, j);
        } else {
          return null;
        }
      }
      
      jarFile = new File(jarPath);
      if (jarFile.isFile()){
        try {
          return new JarContainer(jarFile, pathPrefix);
        } catch (IOException ix) {
          return null;
        }
      } else {
        return null;
      }
      
    } else { // a dir
      File dir = new File(spec);
      if (dir.isDirectory()){
        return new DirContainer(dir);
      } else {
        return null;
      }
    }
  }
  
}


//--- our concrete types

class DirContainer extends ClassFileContainer {

  File dir;

  DirContainer(File dir) {
    super(dir.getPath());
    this.dir = dir;
  }

  public byte[] getClassData(String clsName) throws ClassFileException {
    String pn = clsName.replace('.', File.separatorChar) + ".class";
    File f = new File(dir, pn);

    if (f.isFile()) {
      FileInputStream fis = null;

      try {
        fis = new FileInputStream(f);
        long len = f.length();
        if (len > Integer.MAX_VALUE) {
          error("classfile too big: " + f.getPath());
        }
        byte[] data = new byte[(int) len];
        readFully(fis, data);

        return data;

      } catch (IOException iox) {
        error("cannot read " + f.getPath());

      } finally {
        if (fis != null) {
          try {
            fis.close();
          } catch (IOException iox) {
            error("cannot close input stream for file " + f.getPath());
          }
        }
      }
    }

    return null;
  }
}

class JarContainer extends ClassFileContainer {

  JarFile jar;
  String pathPrefix; // optional

  JarContainer(File file) throws IOException {
    super(file.getPath());

    jar = new JarFile(file);
  }

  JarContainer (File file, String pathPrefix) throws IOException {
    super(getPath(file, pathPrefix));

    jar = new JarFile(file);
    
    this.pathPrefix = getNormalizedPathPrefix(pathPrefix);
  }

  /**
   * make sure the return value ends with '/', and does NOT start with '/'. If
   * the supplied pathPrefix only contains '/' or an empty string, return null
   */
  static String getNormalizedPathPrefix(String pathPrefix){
    if (pathPrefix != null){
      int len = pathPrefix.length();
      if (len > 0){
        if (pathPrefix.charAt(0) == '/'){
          if (len == 1){
            return null; // no need for storing a single '/' prefix
          } else {
            pathPrefix = pathPrefix.substring(1); // skip the heading '/'
            len--;
          }
        }
        
        if (pathPrefix.charAt(len-1) != '/'){
          pathPrefix += '/';
        }
        
        return pathPrefix;
        
      } else {
        return null; // empty prefix
      }
    } else {
      return null; // null prefix
    }
  }

  /**
   * return our string representation of the complete spec, which is
   * 
   *   <jar-pathname>/pathPrefix
   */
  static String getPath(File file, String pathPrefix){
    String pn = file.getPath();
   
    if (pathPrefix != null){
      int len = pathPrefix.length();
      if (len > 0){
        if (pathPrefix.charAt(0) == '/'){
          if (len == 1){
            return pn; // no need to store a single '/'
          }
        } else {
          pn += '/';
        }
        
        pn += pathPrefix;
      }
    }
    
    return pn;
  }
    
  public byte[] getClassData(String clsName) throws ClassFileException {
    String pn = clsName.replace('.', '/') + ".class";
    
    if (pathPrefix != null){
      pn = pathPrefix + pn;
    }
    
    JarEntry e = jar.getJarEntry(pn);

    if (e != null) {
      InputStream is = null;
      try {
        long len = e.getSize();
        if (len > Integer.MAX_VALUE) {
          error("classfile too big: " + e.getName());
        }

        is = jar.getInputStream(e);

        byte[] data = new byte[(int) len];
        readFully(is, data);

        return data;

      } catch (IOException iox) {
        error("error reading jar entry " + e.getName());

      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException iox) {
            error("cannot close input stream for file " + e.getName());
          }
        }
      }
    }

    return null;
  }
}


