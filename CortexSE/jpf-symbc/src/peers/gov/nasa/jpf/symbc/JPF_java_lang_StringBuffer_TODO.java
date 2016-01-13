//
// Copyright (C) 2006 United States Government as represented by the
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
package gov.nasa.jpf.symbc;

import gov.nasa.jpf.jvm.MJIEnv;

/**
 * MJI NativePeer class for java.lang.StringBuffer library abstraction
 */
public class JPF_java_lang_StringBuffer_TODO {



  public static void $clinit____V (MJIEnv env, int clsObjRef) {
    // apparently, Java 1.5 has changed the implementation of class
    // StringBuffer so that it doesn't use the 'shared' state anymore
    // (which was a performance hack to avoid copying the char array
    // data when creating String objects from subsequently unmodified
    // StringBuffers
    // adding this little extra logic here also serves the purpose of
    // avoiding a native ObjectStreamClass method which is called during
    // the static StringBuffer init

	gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.$clinit____V(env, clsObjRef);
  }

  public static int append__Ljava_lang_String_2__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, int sref) {
    return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__Ljava_lang_String_2__Ljava_lang_StringBuffer_2(env, objref, sref);
  }

  public static int append__I__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, int i) {
    return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__I__Ljava_lang_StringBuffer_2(env, objref, i);
  }

  public static int append__F__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, float f) {
    return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__F__Ljava_lang_StringBuffer_2(env, objref, f);
  }

  public static int append__D__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, double d) {
    return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__D__Ljava_lang_StringBuffer_2 (env, objref, d);
  }

  public static int append__J__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, long l) {
	  return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__J__Ljava_lang_StringBuffer_2 (env, objref, l);
  }

  public static int append__Z__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, boolean b) {
	  return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__Z__Ljava_lang_StringBuffer_2 (env, objref, b);
  }

/*
  public static int append__B__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, byte b) {
    return append__C__Ljava_lang_StringBuffer_2(env, objref, (char)b);
  }
*/

  public static int append__C__Ljava_lang_StringBuffer_2 (MJIEnv env, int objref, char c) {
    return gov.nasa.jpf.jvm.JPF_java_lang_StringBuffer.append__C__Ljava_lang_StringBuffer_2(env,objref,c);

  }
}

