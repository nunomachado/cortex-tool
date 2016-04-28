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

import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.classfile.ClassFile;
import gov.nasa.jpf.classfile.ClassFileException;
import gov.nasa.jpf.classfile.ClassFileReaderAdapter;
import gov.nasa.jpf.classfile.ClassPath;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * the JPF counterpart for Java Annotations
 * 
 * we could just store the bcel constructs, but for consistencies sake, we
 * introduce our own enumClassName here (like we do for classes, methods, fields etc)
 */
public class AnnotationInfo {

  // NOTE - never modify an Entry object since it might be shared between
  // different instances of the same annotation type
  public static class Entry {
    String key;
    Object value;
    
    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }
    
    public Entry (String key, Object value){
      this.key = key;
      this.value = value;
    }
  }
  
  static class AnnotationReader extends ClassFileReaderAdapter {
    String annotationName;

    String key;
    Object[] valElements;
    ArrayList<Entry> entries;
    AnnotationAttribute curAttr;

    Entry[] getDefaultValueEntries() {
      if (entries == null){
        return NONE;
      } else {
        return entries.toArray(new Entry[entries.size()]);
      }
    }

    protected ArrayList<Entry> getEntries(){
      if (entries == null){
        entries = new ArrayList<Entry>();
      }
      return entries;
    }
    
    public void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount) {
      entries = null;
      annotationName = Types.getClassNameFromTypeName(clsName);
      curAttr = new AnnotationAttribute(null, false);
      if (!"java/lang/Object".equals(superClsName)){
        throw new JPFException("illegal annotation superclass of: " + annotationName + " is " + superClsName);
      }
    }

    public void setInterface(ClassFile cf, int ifcIndex, String ifcName) {
      if (!"java/lang/annotation/Annotation".equals(ifcName)){
        throw new JPFException("illegal annotation interface of: " + annotationName + " is " + ifcName);
      }
    }

    public void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String descriptor) {
      key = name;
    }

    public void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.ANNOTATIONDEFAULT_ATTR){
        cf.parseAnnotationDefaultAttr(this, key);
      }
    }

    public void setMethodsDone(ClassFile cf) {
    }

    @Override
    public void setClassAttribute(ClassFile cf, int attrIndex, String name, int attrLength) {
      if (name == ClassFile.RUNTIME_VISIBLE_ANNOTATIONS_ATTR) {
        key = null;
        cf.parseAnnotationsAttr(this, null);
      }
    }
    
    public void setAnnotation(ClassFile cf, Object tag, int annotationIndex, String annotationType) {
      if (annotationType.equals("Ljava/lang/annotation/Inherited;")) {
        curAttr.isInherited = true;
      }
    }
    
    public void setAnnotationsDone(ClassFile cf, Object tag) {
      curAttr.defaultEntries = getDefaultValueEntries();
      annotationAttributes.put(annotationName, curAttr);
    }
    
    public void setPrimitiveAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, Object val) {  
      if (arrayIndex >= 0){
        valElements[arrayIndex] = val;
      } else {
        getEntries().add(new Entry(key, val));
      }
    }

    public void setStringAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String val) {
      if (arrayIndex >= 0){
        valElements[arrayIndex] = val;
      } else {
        getEntries().add(new Entry(key, val));
      }
    }

    public void setClassAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String typeName) {
    }

    public void setEnumAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int arrayIndex, String enumType, String enumValue) {
    }

    public void setAnnotationValueElementCount(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName, int elementCount) {
      valElements = new Object[elementCount];
    }

    public void setAnnotationValueElementsDone(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
            String elementName) {
      if (key != null){
        getEntries().add( new Entry(key, valElements));
      }
    }
  }

  public static class EnumValue {
    String eClassName;
    String eConst;
    
    EnumValue (String clsName, String constName){
      eClassName = clsName;
      eConst = constName;
    }
    public String getEnumClassName(){
      return eClassName;
    }
    public String getEnumConstName(){
      return eConst;
    }
    public String toString(){
      return eClassName + '.' + eConst;
    }
  }

  public static class ClassValue {
    String name;

    ClassValue (String cn){
      name = cn;
    }

    public String getName(){
      return name;
    }
    public String toString(){
      return name;
    }
  }

  
  
  static final Entry[] NONE = new Entry[0];
  
  // we have to jump through a lot of hoops to handle default annotation parameter values
  // this is not ideal, since it causes the classfile to be re-read if the SUT
  // uses annotation reflection (which creates a ClassInfo), but this is rather
  // exotic, so we save some time by not creating a ClassInfo (which would hold
  // the default vals as method annotations) and directly store the default values here

  static HashMap<String, AnnotationAttribute> annotationAttributes = new HashMap<String, AnnotationAttribute>();

  static class AnnotationAttribute {
    Entry[] defaultEntries;
    boolean isInherited;

    AnnotationAttribute (Entry[] defaultEntries, boolean isInherited) {
      this.defaultEntries = defaultEntries;
      this.isInherited = isInherited;
    }
  }

  static AnnotationReader valueCollector = new AnnotationReader();

  
  String name;
  Entry[] entries;
  boolean inherited = false;


  
  public static Entry[] getDefaultEntries (ClassPath locator, String annotationType){

    Entry[] def = null;
        
    if (annotationAttributes.get(annotationType) != null) {
      def = annotationAttributes.get(annotationType).defaultEntries;
    }

    if (def == null){ // Annotation not seen yet - we have to dig it out from the classfile
      try {
        byte[] data = locator.getClassData(annotationType);
        if (data == null){
          throw new JPFException("annotation class not found: " + annotationType);
        }

        ClassFile cf = new ClassFile( annotationType, data);
        cf.parse(valueCollector);

        def = valueCollector.getDefaultValueEntries();
        annotationAttributes.put(annotationType, new AnnotationAttribute(def, false));

      } catch (ClassFileException cfx){
        throw new JPFException("malformed annotation classfile");
      }

    }

    return def;
  }
  
  public static Object getEnumValue(String eType, String eConst){
    return new EnumValue( Types.getClassNameFromTypeName(eType), eConst);
  }

  public static Object getClassValue(String type){
    return new ClassValue( Types.getClassNameFromTypeName(type));
  }



  protected AnnotationInfo(String name){
    this.name = name;
    // entries will follow later, so this object is only partially initialized
  }
  
  protected AnnotationInfo (String name, ClassPath cp) {
    this.name = name;
    
    try {
      byte[] data = cp.getClassData(this.name);
      if (data == null){ throw new JPFException("annotation class not found: " + this.name); }

      ClassFile cf = new ClassFile( name, data);
      cf.parse(valueCollector);

    } catch (ClassFileException cfx) {
      throw new JPFException("malformed annotation classfile");
    }
  }

  protected void startEntries (int count){
    entries = new Entry[count];
  }

  protected void setValue(int index, String key, Object value){
    entries[index] = new Entry(key,value);
  }



  public AnnotationInfo (String name, Entry[] entries){
    this.name = name;
    this.entries = entries;
  }

  private AnnotationInfo (String name, Entry[] entries, boolean inherited) {
    this.name = name;
    this.entries = entries;
    this.inherited = inherited;
  }

  public boolean isInherited (){
    return this.inherited;
  }
  
  public void setInherited (boolean inherited){
    this.inherited = inherited;
  }

  /**
   * check if this AnnotationInfo instance has not-yet-specified default
   * values, which we have to load from the annotation classfile itself
   *
   * NOTE - this is set AFTER we got the explicitly specified values from
   * the annotation expression
   */
  public void checkDefaultValues(ClassPath contextCp){
    Entry[] defEntries = getDefaultEntries(contextCp, name);
    int elen = entries.length;
    
    outer:
    for (int i=0; i<defEntries.length; i++){
      Entry de = defEntries[i];

      // check if we already have an explicitly specified value for this entry
      for (int j=0; j<elen; j++){
        if (entries[j].key.equals(de.key)){
          continue outer;
        }
      }

      // add the default value
      if (elen == 0){
        entries = defEntries.clone(); // just set them all at once
        return;

      } else {
        Entry[] newEntries = new Entry[elen + 1];
        System.arraycopy(entries,0, newEntries,0, elen);
        newEntries[elen] = de;
        entries = newEntries;
      }
    }
  }

  public String getName() {
    return name;
  }

  public Entry[] getEntries() {
    return entries;
  }
  
  // convenience method for single-attribute annotations
  public Object value() {
    return getValue("value");
  }
  
  public String valueAsString(){
    Object v = value();
    return (v != null) ? v.toString() : null;
  }
  
  public String getValueAsString (String key){
    Object v = getValue(key);
    return (v != null) ? v.toString() : null;
  }
  
  public String[] getValueAsStringArray() {
    String a[] = null; 
    Object v = value();
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;    
  }
  
  public String[] getValueAsStringArray (String key) {
    // <2do> not very efficient
    String a[] = null; 
    Object v = getValue(key);
    if (v != null && v instanceof Object[]) {
      Object[] va = (Object[])v;
      a = new String[va.length];
      for (int i=0; i<a.length; i++) {
        if (va[i] != null) {
          a[i] = va[i].toString();
        }
      }
    }
    
    return a;
  }
  
  
  public <T> T getValue (String key, Class<T> type){
    Object v = getValue(key);
    if (type.isInstance(v)){
      return (T)v;
    } else {
      return null;
    }
  }
  
  public boolean getValueAsBoolean (String key){
    Object v = getValue(key);
    if (v instanceof Boolean){
      return ((Boolean)v).booleanValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a boolean: " + v);
    }
  }
  
  public int getValueAsInt (String key){
    Object v = getValue(key);
    if (v instanceof Integer){
      return ((Integer)v).intValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not an int: " + v);
    }
  }

  public long getValueAsLong (String key){
    Object v = getValue(key);
    if (v instanceof Long){
      return ((Long)v).longValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a long: " + v);
    }
  }

  public float getValueAsFloat (String key){
    Object v = getValue(key);
    if (v instanceof Float){
      return ((Float)v).floatValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a float: " + v);
    }
  }
  
  public double getValueAsDouble (String key){
    Object v = getValue(key);
    if (v instanceof Double){
      return ((Double)v).doubleValue();
    } else {
      throw new JPFException("annotation element @" + name + '.' + key + "() not a double: " + v);
    }
  }

  
  public Object getValue (String key){
    for (int i=0; i<entries.length; i++){
      if (entries[i].getKey().equals(key)){
        return entries[i].getValue();
      }
    }
    return null;
  }
  
  public String asString() {
    StringBuilder sb = new StringBuilder();
    sb.append('@');
    sb.append(name);
    sb.append('[');
    for (int i=0; i<entries.length; i++){
      if (i > 0){
        sb.append(',');
      }
      sb.append(entries[i].getKey());
      sb.append('=');
      sb.append(entries[i].getValue());
    }
    sb.append(']');
    
    return sb.toString();
  }
  
  public AnnotationInfo cloneInherited() {
    return new AnnotationInfo(name, entries, true);
  }
}
