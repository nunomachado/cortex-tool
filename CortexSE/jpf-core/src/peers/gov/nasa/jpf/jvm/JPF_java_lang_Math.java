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
package gov.nasa.jpf.jvm;

/**
 * MJI NativePeer class for java.lang.Math library abstraction
 */
public class JPF_java_lang_Math {
  
  // <2do> those are here to hide their implementation from traces, not to
  // increase performance. If we want to do that, we should probably inline
  // their real implementation here, instead of delegating (just a compromise)
  
  public static double abs__D__D (MJIEnv env, int clsObjRef, double a) {
    // return Math.abs(a);
    
    return (a <= .0) ? (.0 - a) : a;
  }

  public static float abs__F__F (MJIEnv env, int clsObjRef, float a) {
    return Math.abs(a);
  }

  public static int abs__I__I (MJIEnv env, int clsObjRef, int a) {
    //return Math.abs(a);
    return (a < 0) ? -a : a; // that's probably slightly faster
  }

  public static long abs__J__J (MJIEnv env, int clsObjRef, long a) {
    //return Math.abs(a);
    
    return (a < 0) ? -a : a;
  }

  public static double max__DD__D (MJIEnv env, int clsObjRef, double a, double b) {
    // that one has to handle inexact numbers, so it's probably not worth the hassle
    // to inline it
    return Math.max(a, b);
  }

  public static float max__FF__F (MJIEnv env, int clsObjRef, float a, float b) {
    return Math.max(a, b);
  }

  public static int max__II__I (MJIEnv env, int clsObjRef, int a, int b) {
    //return Math.max(a, b);
    
    return (a >= b) ? a : b;
  }

  public static long max__JJ__J (MJIEnv env, int clsObjRef, long a, long b) {
    //return Math.max(a, b);
    return (a >= b) ? a : b;
  }

  public static double min__DD__D (MJIEnv env, int clsObjRef, double a, double b) {
    return Math.min(a, b);
  }

  public static float min__FF__F (MJIEnv env, int clsObjRef, float a, float b) {
    return Math.min(a, b);
  }

  public static int min__II__I (MJIEnv env, int clsObjRef, int a, int b) {
    return Math.min(a, b);
  }

  public static long min__JJ__J (MJIEnv env, int clsObjRef, long a, long b) {
    return Math.min(a, b);
  }

  public static double pow__DD__D (MJIEnv env, int clsObjRef, double a, double b) {
    return Math.pow(a, b);
  }

  public static double sqrt__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.sqrt(a);
  }
  
  public static double random____D (MJIEnv env, int clsObjRef) {
    return Math.random();
  }
  
  public static long round__D__J (MJIEnv env, int clsObjRef, double a){
    return Math.round(a);
  }
  
  public static double exp__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.exp(a);
  }
  
  public static double asin__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.asin(a);
  }

  public static double acos__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.acos(a);
  }
  
  public static double atan__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.atan(a);
  }
  
  public static double atan2__DD__D (MJIEnv env, int clsObjRef, double a, double b) {
    return Math.atan2(a,b);
  }
  
  public static double ceil__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.ceil(a);
  }
  
  public static double cos__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.cos(a);
  }
  
  public static double floor__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.floor(a);
  }
  
  public static double log10__D__D (MJIEnv env, int clsObjRef, double a) {
	return Math.log10(a);
  }  
  
  public static double log__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.log(a);
  }
  
  public static double rint__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.rint(a);
  }
  
  public static double sin__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.sin(a);
  }
  
  public static double tan__D__D (MJIEnv env, int clsObjRef, double a) {
    return Math.tan(a);
  }
}
