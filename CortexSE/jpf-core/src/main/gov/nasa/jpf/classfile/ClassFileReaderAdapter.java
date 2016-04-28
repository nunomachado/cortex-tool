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

/**
 * adapter class implementing the ClassFileReader interface
 */
public class ClassFileReaderAdapter implements ClassFileReader {

  public void setClass(ClassFile cf, String clsName, String superClsName, int flags, int cpCount) {}

  public void setInterfaceCount(ClassFile cf, int ifcCount) {}

  public void setInterface(ClassFile cf, int ifcIndex, String ifcName) {}

  public void setInterfacesDone(ClassFile cf) {};

  public void setFieldCount(ClassFile cf, int fieldCount) {}

  public void setField(ClassFile cf, int fieldIndex, int accessFlags, String name, String descriptor) {}

  public void setFieldAttributeCount(ClassFile cf, int fieldIndex, int attrCount) {}

  public void setFieldAttribute(ClassFile cf, int fieldIndex, int attrIndex, String name, int attrLength) {}

  public void setFieldAttributesDone(ClassFile cf, int fieldIndex) {}

  public void setFieldDone(ClassFile cf, int fieldIndex) {}

  public void setFieldsDone(ClassFile cf) {}

  public void setConstantValue(ClassFile cf, Object tag, Object value) {}

  public void setMethodCount(ClassFile cf, int methodCount) {}

  public void setMethod(ClassFile cf, int methodIndex, int accessFlags, String name, String descriptor) {}

  public void setMethodAttributeCount(ClassFile cf, int methodIndex, int attrCount) {}

  public void setMethodAttribute(ClassFile cf, int methodIndex, int attrIndex, String name, int attrLength) {}

  public void setMethodAttributesDone(ClassFile cf, int methodIndex){}

  public void setMethodDone(ClassFile cf, int methodIndex) {}

  public void setMethodsDone(ClassFile cf) {}

  public void setExceptionCount(ClassFile cf, Object tag, int exceptionCount) {}

  public void setException(ClassFile cf, Object tag, int exceptionIndex, String exceptionType) {}

  public void setExceptionsDone(ClassFile cf, Object tag) {}

  public void setCode(ClassFile cf, Object tag, int maxStack, int maxLocals, int codeLength) {}

  public void setExceptionHandlerTableCount(ClassFile cf, Object tag, int exceptionTableCount) {}

  public void setExceptionHandler(ClassFile cf, Object tag, int exceptionIndex,
          int startPc, int endPc, int handlerPc, String catchType) {}

  public void setExceptionHandlerTableDone(ClassFile cf, Object tag) {}

  public void setCodeAttributeCount(ClassFile cf, Object tag, int attrCount) {}

  public void setCodeAttribute(ClassFile cf, Object tag, int attrIndex, String name, int attrLength) {}

  public void setCodeAttributesDone (ClassFile cf, Object tag) {}

  public void setLineNumberTableCount(ClassFile cf, Object tag, int lineNumberCount) {}

  public void setLineNumber(ClassFile cf, Object tag, int lineIndex, int lineNumber, int startPc) {}

  public void setLineNumberTableDone(ClassFile cf, Object tag) {}

  public void setLocalVarTableCount(ClassFile cf, Object tag, int localVarCount) {}

  public void setLocalVar(ClassFile cf, Object tag, int localVarIndex,
          String varName, String descriptor, int scopeStartPc, int scopeEndPc, int slotIndex) {}

  public void setLocalVarTableDone (ClassFile cf, Object tag) {}

  public void setClassAttributeCount(ClassFile cf, int attrCount) {}

  public void setClassAttribute(ClassFile cf, int attrIndex, String name, int attrLength) {}

  public void setClassAttributesDone(ClassFile cf) {}

  public void setSourceFile(ClassFile cf, Object tag, String pathName) {}

  public void setInnerClassCount(ClassFile cf, Object tag, int innerClsCount) {}

  public void setInnerClass(ClassFile cf, Object tag, int innerClsIndex,
          String outerName, String innerName, String innerSimpleName, int accessFlags) {}

  public void setInnerClassesDone(ClassFile cf, Object tag) {}
  
  public void setEnclosingMethod(ClassFile cf, Object tag, String enclosingClass, String enclosingMethod, String descriptor) {}

  public void setAnnotationCount(ClassFile cf, Object tag, int annotationCount){}

  public void setAnnotationsDone(ClassFile cf, Object tag) {}

  public void setAnnotation(ClassFile cf, Object tag, int annotationIndex, String annotationType){}

  public void setAnnotationValueCount(ClassFile cf, Object tag, int annotationIndex, int annotationCount) {}

  public void setPrimitiveAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, Object val){}

  public void setStringAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String s){}

  public void setClassAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String typeName){}

  public void setEnumAnnotationValue(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName, int arrayIndex, String enumType, String enumValue){}

  public void setAnnotationValueElementCount(ClassFile cf, Object tag, int annotationIndex, int valueIndex, 
          String elementName, int elementCount) {}

  public void setAnnotationValueElementsDone(ClassFile cf, Object tag, int annotationIndex, int valueIndex,
          String elementName) {}

  public void setAnnotationValuesDone(ClassFile cf, Object tag, int annotationIndex) {}

  public void setParameterCount(ClassFile cf, Object tag, int parameterCount) {}

  public void setParameterAnnotationCount(ClassFile cf, Object tag, int paramIndex, int annotationCount) {}

  public void setParameterAnnotation(ClassFile cf, Object tag, int annotationIndex, String annotationType) {}

  public void setParameterAnnotationsDone(ClassFile cf, Object tag, int paramIndex) {}

  public void setParametersDone(ClassFile cf, Object tag) {}

  public void setSignature(ClassFile cf, Object tag, String signature) {}
}
