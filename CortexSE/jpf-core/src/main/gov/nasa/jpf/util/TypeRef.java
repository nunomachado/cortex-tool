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

package gov.nasa.jpf.util;

/**
 * this is a utility construct for lexical type references that should
 * not cause class loading when instantiated, but cannot use a
 * classname String because of argument type ambiguity (Strings are just
 * used everywhere)
 *
 * NOTE - loading and instantiation of TypeRefs is not allowed to cause loading of
 * any JPF classes that are not in jpf-classes.jar
 */
public class TypeRef {
  String clsName;

  public TypeRef (String clsName){
    this.clsName = clsName;
  }

  public <T> Class<? extends T> asSubclass(Class<T> superClazz) throws ClassNotFoundException, ClassCastException {
    Class<?> clazz = Class.forName(clsName);
    return clazz.asSubclass(superClazz);
  }

  public String toString(){
    return clsName;
  }
}
