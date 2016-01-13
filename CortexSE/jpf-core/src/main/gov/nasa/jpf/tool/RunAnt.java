//
// Copyright (C) 2009 United States Government as represented by the
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

package gov.nasa.jpf.tool;

import gov.nasa.jpf.util.JPFSiteUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


/**
 * starter class to use the (minimal) ant installation that comes with
 * jpf-core
 */
public class RunAnt {

  public static void main (String[] args){

    ArrayList<URL> urlList = new ArrayList<URL>();

    addJavac(urlList);
    addJPFToolJars(args, urlList);  // <2do> - Hmm, what if we boot with jpf.jar?

    URL[] urls = urlList.toArray(new URL[urlList.size()]);
    URLClassLoader cl = new URLClassLoader(urls, RunAnt.class.getClassLoader());

    try {
      Class<?> jpfCls = cl.loadClass("org.apache.tools.ant.Main");

      Class<?>[] argTypes = { String[].class };
		  Method mainMth = jpfCls.getMethod("main", argTypes);

      mainMth.invoke(null, new Object[] { args });

    } catch (ClassNotFoundException cnfx){
      abort("cannot find org.apache.tools.ant.Main");
    } catch (NoSuchMethodException nsmx){
      abort("no org.apache.tools.ant.Main.main(String[]) method found");
    } catch (IllegalAccessException iax){
      abort("no \"public static void main(String[])\" method in org.apache.tools.ant.Main");
    } catch (InvocationTargetException ix) {
      ix.getCause().printStackTrace();
    }

    // we let the InvocationTargetException pass
  }

  static void warning(String msg){
    System.err.println("WARNING: " + msg);
  }

  static void abort (String msg){
    System.err.println("ERROR: " + msg);
    System.exit(1);
  }

  static String getToolsJarPath(){
    char sc = File.separatorChar;
    String javaHome = System.getProperty("java.home");
    String toolsJarPath = null;
    
    if (javaHome.endsWith(sc + "jre")){
      toolsJarPath = javaHome.substring(0, javaHome.length()-4) + sc + "lib" + sc + "tools.jar";
    } else {
      toolsJarPath = javaHome + sc + "lib" + sc + "tools.jar";
    }
    
    return toolsJarPath;
  }
  
  static void addJavac(List<URL> list) {
    String os = System.getProperty("os.name");
    String version = System.getProperty("java.version");
    String toolsJarPath = null;

    if ("Mac OS X".equals(os)){
      // pre Java 1.7 it was part of classes.zip, but with OpenJDK, it got moved back into tools.jar
      if (version.compareTo("1.7") >= 0){  // I guess it will be a while until we reach Java 10
        toolsJarPath = getToolsJarPath();
      }
      
    } else {
      // on Linux and Windows it's in ${java.home}/lib/tools.jar
      toolsJarPath = getToolsJarPath();
    }
    
    if (toolsJarPath != null){
      File toolsJar = new File(toolsJarPath);
      if (toolsJar.isFile()){
        try {
          list.add(toolsJar.toURI().toURL());
        } catch (MalformedURLException ex) {
          abort("malformed URL: " + toolsJar.getAbsolutePath());
        }
      } else {
        abort("can't find javac, no " + toolsJar.getPath());
      }
    }
  }

  static void addJPFToolJars (String[] args, List<URL> list) {
    File toolsDir = null;

    // find the current project root dir
    File dir = new File(System.getProperty("user.dir"));
    while (dir != null && !(new File(dir,"jpf.properties").isFile())){
      dir = dir.getParentFile();
    }

    // check if the current project has an ant.jar
    for (File d : new File[] {dir, new File(dir, "tools"), new File(dir, "lib")}){
      if (hasAntJar(d)){
        toolsDir = d;
        break;
      }
    }

    // if we didn't find any, look for the tools in the site configured jpf-core
    if (toolsDir == null){
      dir = JPFSiteUtils.getSiteCoreDir();

      for (File d : new File[] {dir, new File(dir, "tools"), new File(dir, "lib")}) {
        if (hasAntJar(d)) {
          toolsDir = d;
          break;
        }
      }
    }

    if (toolsDir != null){
      addToolsJars(list, toolsDir);
    } else {
      abort("no ant.jar found in known tools dirs (check your site.properties)");
    }
  }


  static boolean hasAntJar (File toolsDir){
    return (new File(toolsDir, "ant.jar").isFile());
  }

  static void addToolsJars (List<URL> list, File toolsDir){
    for (File f : toolsDir.listFiles()) {
      String name = f.getName();
      if (name.endsWith(".jar")) {
        try {
          list.add(f.toURI().toURL());
        } catch (MalformedURLException ex) {
          abort("malformed URL: " + f.getAbsolutePath());
        }
      }
    }
  }

}
