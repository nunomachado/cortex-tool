//
//Copyright (C) 2009 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.
//
//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.
//
//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.concurrent.peers;

import gov.nasa.jpf.concurrent.ConcurrentHashMap;
import gov.nasa.jpf.jvm.MJIEnv;

/**
 * Peer for java.util.concurrent.ConcurrentHashMap
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 */
public class JPF_java_util_concurrent_ConcurrentHashMap extends ConcurrentPeer{

  public static void newMap____V(MJIEnv env, int objRef) {
    ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).newMap();
  }

  public static void newMap__I__V(MJIEnv env, int objRef, int initialCapacity) {
    ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).newMap(initialCapacity);
  }

  public static void clear____V(MJIEnv env, int objRef) {
    ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).clear();
  }

  public static boolean contains__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).contains(value);
  }

  public static boolean containsKey__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int key) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).containsKey(key);
  }

  public static boolean containsValue__Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).containsValue(value);
  }

  public static int get__Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int key) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).get(key);
  }

  public static boolean isEmpty____Z(MJIEnv env, int objRef) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).isEmpty();
  }

  public static int put__Ljava_lang_Object_2Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int key, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).put(key, value);
  }

  public static int putIfAbsent__Ljava_lang_Object_2Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int key, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).putIfAbsent(key, value);
  }

  public static int remove__Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int key) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).remove(key);
  }

  public static boolean remove__Ljava_lang_Object_2Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int key, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).remove(key, value);
  }

  public static int replace__Ljava_lang_Object_2Ljava_lang_Object_2__Ljava_lang_Object_2(MJIEnv env, int objRef, int key, int value) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).replace(key, value);
  }

  public static boolean replace__Ljava_lang_Object_2Ljava_lang_Object_2Ljava_lang_Object_2__Z(MJIEnv env, int objRef, int key, int oldValue, int newValue) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).replace(key, oldValue, newValue);
  }

  public static int size____I(MJIEnv env, int objRef) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).size();
  }

  public static boolean hasNextEntry__I__Z(MJIEnv env, int objRef, int pos) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).hasNextEntry(pos);
  }

  public static int nextEntry__I__Ljava_util_Map$Entry_2(MJIEnv env, int objRef, int pos) {
    return ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).nextEntry(pos);
  }

  public static void removeEntry__I__V(MJIEnv env, int objRef, int pos) {
    ConcurrentHashMap.getConcurrentHashMap(env, objRef, getVersion(env, objRef)).removeEntry(pos);
  }

}
