//
// Copyright (C) 2007 United States Government as represented by the
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

import java.util.ArrayList;

/**
 * common root for ClassInfo, MethodInfo, FieldInfo (and maybe more to follow)
 * 
 * so far, it's used to factorize the annotation support, but we can also
 * move the attributes up here
 */
public abstract class InfoObject {

  static AnnotationInfo[] NO_ANNOTATIONS = new AnnotationInfo[0];
  
  // he number of annotations per class/method/field is usually
  // small enough so that simple arrays are more efficient than HashMaps
  protected AnnotationInfo[] annotations;

  protected void startAnnotations(int count){
    annotations = new AnnotationInfo[count];
  }

  protected void setAnnotation(int index, AnnotationInfo ai){
    annotations[index] = ai;
  }

  public void addAnnotation (AnnotationInfo newAnnotation){
    AnnotationInfo[] ai = annotations;
    if (ai == null){
      ai = new AnnotationInfo[1];
      ai[0] = newAnnotation;

    } else {
      int len = annotations.length;
      ai = new AnnotationInfo[len+1];
      System.arraycopy(annotations, 0, ai, 0, len);
      ai[len] = newAnnotation;
    }

    annotations = ai;
  }

  public boolean hasAnnotations(){
    return (annotations != null);
  }
  
  public AnnotationInfo[] getAnnotations() {
    if (annotations == null){
      return NO_ANNOTATIONS; // make life a bit easier for clients and keep it similar to the model class API
    } else {
      return annotations;
    }
  }
  
  public AnnotationInfo getAnnotation (String name){
    AnnotationInfo[] ai = annotations;
    if (ai != null){
      for (int i=0; i<ai.length; i++){
        if (ai[i].getName().equals(name)){
          return ai[i];
        }
      }
    }
    return null;
  }

  /**
   * return the ClassInfo this object represents or belongs to
   */
  public abstract ClassInfo getClassInfo();

  public void computeInheritedAnnotations (InfoObject superClass){
    if (superClass != null){
      AnnotationInfo[] superClassAnn = superClass.getAnnotations();
      ArrayList<AnnotationInfo> inheritedAnn = new ArrayList<AnnotationInfo>();
      for (AnnotationInfo ai : superClassAnn){
        if (AnnotationInfo.annotationAttributes.get(ai.getName()).isInherited){
          if (ai.isInherited()){
            inheritedAnn.add(ai);
          } else{
            inheritedAnn.add(ai.cloneInherited());
          }
        }
      }
      annotations = inheritedAnn.toArray(new AnnotationInfo[inheritedAnn.size()]);
    }
  }
}
