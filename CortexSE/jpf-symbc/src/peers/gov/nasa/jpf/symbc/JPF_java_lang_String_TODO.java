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

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.DynamicArea;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringExpression;

/**
 * MJI NativePeer class for java.lang.String library abstraction
 */
public class JPF_java_lang_String_TODO {

// delegate to JPF's methods
  public static int intern____Ljava_lang_String_2 (MJIEnv env, int robj) {
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.intern____Ljava_lang_String_2 (env, robj);
  }

  public static int toCharArray_____3C (MJIEnv env, int objref){
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.toCharArray_____3C(env, objref);
  }



  public static int hashCode____I (MJIEnv env, int objref) {
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.hashCode____I(env, objref);
  }

  public static boolean matches__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int regexRef){
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.matches__Ljava_lang_String_2__Z(env, objRef, regexRef);
  }

  // <2do> we also need startsWith, endsWith, indexOf etc. - all not relevant from
  // a model checking perspective (unless we want to compute execution budgets)


  public static int format__Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2 (MJIEnv env, int clsObjRef,
          int fmtRef, int argRef){
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.format__Ljava_lang_String_2_3Ljava_lang_Object_2__Ljava_lang_String_2(env, clsObjRef, fmtRef, argRef);
  }

  //
  public static int getBytes__Ljava_lang_String_2___3B(MJIEnv env, int ObjRef, int str){
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.getBytes__Ljava_lang_String_2___3B(env, ObjRef, str);
  }

  public static int split__Ljava_lang_String_2___3Ljava_lang_String_2(MJIEnv env,int clsObjRef,int strRef){
	  return gov.nasa.jpf.jvm.JPF_java_lang_String.split__Ljava_lang_String_2___3Ljava_lang_String_2(env, clsObjRef, strRef);
  }

  // symbolic handling

  public static int concat__Ljava_lang_String_2__Ljava_lang_String_2(MJIEnv env, int objRef, int strRef) {
	  System.out.println("String.concat");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(strRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))  // concrete
		  return env.newString(strThis.concat(str));

	  // Symbolic
	  assert (attrs.length == 2);
	  StringExpression sym_v1 = (StringExpression) attrs[0];
	  StringExpression sym_v2 = (StringExpression) attrs[1];

	  if(sym_v1 == null & sym_v2 == null)
		  throw new RuntimeException("ERROR: symbolic string method must have one symbolic operand: concat__Ljava_lang_String_2__Ljava_lang_String_2");

	  StringExpression result = null;
	  if (sym_v1 == null) // operand 0 is concrete
		  result = sym_v2._concat(strThis);

	  else if (sym_v2 == null){ // operand 1 is concrete
		  sym_v2 = new StringConstant(str);
		  result = sym_v2._concat(sym_v1);
	  }
	  else   // both operands are symbolic
		  result = sym_v2._concat(sym_v1);


	  env.setReturnAttribute(result);
	  return env.newString(""); // don't care

  }

  public static boolean equals__Ljava_lang_Object_2__Z (MJIEnv env, int objRef, int argRef) {

	  System.out.println("ERROR: String.equals");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))  // concrete
		  return gov.nasa.jpf.jvm.JPF_java_lang_String.equals__Ljava_lang_Object_2__Z (env, objRef, argRef);

	  Expression sym_v1 = (Expression) attrs[0];
	  Expression sym_v2 = (Expression) attrs[1];
	  if(sym_v1 == null & sym_v2 == null)
		  throw new RuntimeException("ERROR: symbolic string method must have one symbolic operand: equals__Ljava_lang_Object_2__Z");

	  if (sym_v1 != null && !(sym_v1 instanceof StringExpression))
		  throw new RuntimeException("ERROR: expressiontype not handled: equals__Ljava_lang_Object_2__Z");

	  if(sym_v2 != null && !(sym_v2 instanceof StringExpression))
		  throw new RuntimeException("ERROR: expressiontype not handled: equals__Ljava_lang_Object_2__Z");

	  //handleEquals(invInst, ss, th);
	  // symbolic case
	  ThreadInfo ti = env.getVM().getCurrentThread();
		SystemState ss = env.getVM().getSystemState();
		ChoiceGenerator<?> cg;

		if (!ti.isFirstStepInsn()) {
			  cg = new PCChoiceGenerator(2);  //new
			  ss.setNextChoiceGenerator(cg);
			  env.repeatInvocation();
			  return true;  // not used anyways
		  }
		//else this is what really returns results

		cg = ss.getChoiceGenerator();
		assert (cg instanceof PCChoiceGenerator) :
			"expected PCChoiceGenerator, got: " + cg;

		boolean conditionValue = (Integer)cg.getNextChoice()==0 ? false: true;

        //  System.out.println("conditionValue: " + conditionValue);


         PathCondition pc;

         // pc is updated with the pc stored in the choice generator above
         // get the path condition from the
         // previous choice generator of the same type

         ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
         while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
           prev_cg = prev_cg.getPreviousChoiceGenerator();
         }

         if (prev_cg == null) {
           pc = new PathCondition();
         }
         else {
           pc = ((PCChoiceGenerator)prev_cg).getCurrentPC();
         }

         assert pc != null;

         if (conditionValue) {
           if (sym_v1 != null){
             if (sym_v2 != null){ //both are symbolic values
               pc.spc._addDet(StringComparator.EQUALS,(StringExpression)sym_v1,(StringExpression)sym_v2);
             }else {

               String val = env.getStringObject(argRef);
               pc.spc._addDet(StringComparator.EQUALS,(StringExpression)sym_v1,val);
             }
           }else {
             String val = env.getStringObject(objRef);
             pc.spc._addDet(StringComparator.EQUALS, val, (StringExpression)sym_v2);
           }
            if(!pc.simplify())  {// not satisfiable
              ss.setIgnored(true);
             }else{
             //pc.solve();
             ((PCChoiceGenerator) cg).setCurrentPC(pc);
              // System.out.println(((PCChoiceGenerator) cg).getCurrentPC());
           }
         } else {
           if (sym_v1 != null){
             if (sym_v2 != null){ //both are symbolic values
               pc.spc._addDet(StringComparator.NOTEQUALS,(StringExpression)sym_v1,(StringExpression)sym_v2);
             }else {
               String val = env.getStringObject(argRef);
               pc.spc._addDet(StringComparator.NOTEQUALS,(StringExpression)sym_v1,val);
             }
           }else {
             String val = env.getStringObject(objRef);
             pc.spc._addDet(StringComparator.NOTEQUALS, val, (StringExpression)sym_v2);
           }
           if(!pc.simplify())  {// not satisfiable
               ss.setIgnored(true);
             }else {
             ((PCChoiceGenerator) cg).setCurrentPC(pc);
           }
         }



	  return conditionValue;
  }

//TODO: to implement
  public static boolean equalsIgnoreCase__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int argRef) {
	  System.out.println("String.equalsIgnoreCase");

	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))  // concrete
		  return strThis.equalsIgnoreCase(str);
	  //else
	  throw new RuntimeException("ERROR: symbolic case not handled: equalsIgnoreCase__Ljava_lang_Object_2__Z");

  }

  public static boolean endsWith__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int argRef) {
	  System.out.println("String.endsWith");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.endsWith(str);
	  // else
	  throw new RuntimeException("ERROR: symbolic case not handled: endsWith__Ljava_lang_String_2__Z");

  }
  public static boolean startsWith__Ljava_lang_String_2__Z (MJIEnv env, int objRef, int argRef) {
	  System.out.println("String.startsWith");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.startsWith(str);
	  // else
	  throw new RuntimeException("ERROR: symbolic case not handled: startsWith__Ljava_lang_String_2__Z");
  }
  public static boolean startsWith__Ljava_lang_String_2I__Z (MJIEnv env, int objRef, int argRef, int offset) {
	  System.out.println("String.startsWith");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.startsWith(str,offset);
	  //else
	  throw new RuntimeException("ERROR: symbolic case not handled: startsWith__Ljava_lang_String_2I__Z");
  }
  public static int length (MJIEnv env, int objRef) {
	  System.out.println("ERROR: String.length");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.length();
	  //else Symbolic
	  assert (attrs.length == 1);
	  StringExpression sym_v = (StringExpression) attrs[0];

	  if(sym_v == null)
		  throw new RuntimeException("ERROR: symbolic string method must have one symbolic operand: length");

	  IntegerExpression result = sym_v._length();
	  env.setReturnAttribute(result);
	  return 0; // don't care

  }

  public static int indexOf__I__I (MJIEnv env, int objRef, int c) {
	  System.out.println("String.indexOf");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.indexOf(c);
	  //else
	  throw new RuntimeException("ERROR: symbolic string method not handled: indexOf__I__I");
  }
  public static int indexOf__II__I(MJIEnv env, int objRef, int c, int from) {
	  System.out.println("String.indexOf");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.indexOf(c,from);
	  //else
	  throw new RuntimeException("ERROR: symbolic string method not handled: indexOf__II__I");
  }
  public static int indexOf__Ljava_lang_String_2__I(MJIEnv env, int objRef, int argRef) {
	  System.out.println("String.indexOf");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.indexOf(str);
	  //else
	  throw new RuntimeException("ERROR: symbolic string method not handled: indexOf__Ljava_lang_String_2__I");
  }
  public static int indexOf__Ljava_lang_String_2I__I (MJIEnv env, int objRef, int argRef, int from) {
	  System.out.println("String.indexOf");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String str = env.getStringObject(argRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return strThis.indexOf(str,from);
	  //else
	  throw new RuntimeException("ERROR: symbolic string method not handled: indexOf__Ljava_lang_String_2I__I");
  }
  public static int replace__CC__Ljava_lang_String_2 (MJIEnv env, int objRef, char oldC, char newC) {
	  System.out.println("String.replace");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.replace(oldC,newC));

	  //else Symbolic
//	  assert (attrs.length == 3);
//	  StringExpression sym_v1 = (StringExpression) attrs[0];
//	  StringExpression sym_v2 = (StringExpression) attrs[1];
//	  StringExpression sym_v3 = (StringExpression) attrs[2];
//
//		if ((sym_v1 == null) & (sym_v2 == null) & (sym_v3 == null))
//			throw new RuntimeException("ERROR: symbolic string method must have one symbolic operand: replace__CC__Ljava_lang_String_2");
//
//		StringExpression result = null;
//		if (sym_v1 == null) { // operand 0 is concrete
//				String val = env.getStringObject(objRef);
//				if (sym_v2 == null) { // sym_v3 has to be symbolic
//					// TODO: to review
//					result = sym_v3._replace(val, oldC+""); // converted a char into a string; not sure if this is what we want
//				} else {
//					if (sym_v3 == null) { // only sym_v2 is symbolic
//						sym_v3 = new StringConstant(oldC+"");// TODO: to review
//						result = sym_v3._replace(val, sym_v2);
//					} else {
//						result = sym_v3._replace(val, sym_v2);
//					}
//				}
//		} else { // sym_v1 is symbolic
//				if (sym_v2 == null) {
//					if (sym_v3 == null) {
//						ElementInfo e2 = DynamicArea.getHeap().get(s2);
//						String val1 = e2.asString();
//						ElementInfo e3 = DynamicArea.getHeap().get(s3);
//						String val2 = e3.asString();
//						sym_v3 = new StringConstant(val2);
//						result = sym_v3._replace(sym_v1, val1);
//					} else {
//						ElementInfo e2 = DynamicArea.getHeap().get(s2);
//						String val1 = e2.asString();
//						result = sym_v3._replace(sym_v1, val1);
//					}
//				} else {
//					if (sym_v3 == null) {
//						ElementInfo e3 = DynamicArea.getHeap().get(s3);
//						String val2 = e3.asString();
//						sym_v3 = new StringConstant(val2);
//						result = sym_v3._replace(sym_v1, sym_v2);
//					} else {
//						result = sym_v3._replace(sym_v1, sym_v2);
//					}
//				}
//			}
//			int objRef = th.getVM().getDynamicArea().newString("", th); /*
//																																	 * dummy
//																																	 * String
//																																	 * Object
//																																	 */
//			th.push(objRef, true);
//			sf.setOperandAttr(result);
//		}
//		return null;
//
//
//
//
//
//
//
//	  IntegerExpression result = sym_v._length();
//	  env.setReturnAttribute(result);
//	  return 0; // don't care
	  return -1;
  }
  public static int replaceAll__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int regExRef, int replaceRef) {
	  System.out.println("String.replaceAll");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String regEx = env.getStringObject(regExRef);
	  String replace = env.getStringObject(replaceRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.replaceAll(regEx,replace));
	  // else
	  return -1;
  }
  public static int replaceFirst__Ljava_lang_String_2Ljava_lang_String_2__Ljava_lang_String_2 (MJIEnv env, int objRef, int regExRef, int replaceRef) {
	  System.out.println("String.replaceFirst");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  String regEx = env.getStringObject(regExRef);
	  String replace = env.getStringObject(replaceRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.replaceFirst(regEx,replace));
	  // else
	  return -1;
  }
  public static int trim(MJIEnv env, int objRef) {
	  System.out.println("String.trim");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.trim());
	  // else
	  return -1;
  }
  public static int substring__I__Ljava_lang_String_2(MJIEnv env, int objRef, int b) {
	  System.out.println("String.substring");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.substring(b));
	  // else
	  return -1;
  }
  public static int substring__II__Ljava_lang_String_2(MJIEnv env, int objRef, int b, int e) {
	  System.out.println("String.substring");
	  Object [] attrs = env.getArgAttributes();
	  String strThis = env.getStringObject(objRef);
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(strThis.substring(b,e));
	  // else
	  return -1;
  }
  public static int valueOf__Z__Ljava_lang_String_2(MJIEnv env, int objRef, boolean b) {
	  System.out.println("String.valueOf1");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(b));
	  // else
	  return -1;
  }
  public static int valueOf__C__Ljava_lang_String_2(MJIEnv env, int objRef, char c) {
	  System.out.println("String.valueOf2");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(c));
	  // else
	  return -1;
  }
  public static int valueOf__D__Ljava_lang_String_2(MJIEnv env, int objRef, double d) {
	  System.out.println("String.valueOf3");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(d));
	  // else
	  return -1;
  }
  public static int valueOf__F__Ljava_lang_String_2(MJIEnv env, int objRef, float f) {
	  System.out.println("String.valueOf4");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null|| attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(f));
	  // else
	  return -1;
  }
  public static int valueOf__I__Ljava_lang_String_2(MJIEnv env, int objRef, int i) {
	  System.out.println("String.valueOf5");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null || attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(i));
	  // else
	  return -1;
  }
  public static int valueOf__J__Ljava_lang_String_2(MJIEnv env, int objRef, long l) {
	  System.out.println("String.valueOf6");
	  Object [] attrs = env.getArgAttributes();
	  if (attrs == null || attrs.length==0 || (attrs.length==1 && attrs[0]==null))
		  return env.newString(String.valueOf(l));
	  // else
	  return -1;
  }
  public static int valueOf__Ljava_lang_Object_2__Ljava_lang_String_2(MJIEnv env, int objRef, int argRef) {
	  System.out.println("String.valueOf7");
	  Object [] attrs = env.getArgAttributes();
	  Object o = env.getStringObject(argRef);
	  if (attrs == null || attrs.length==0 || (attrs.length==1 && attrs[0]==null)) {
		  return env.newString(String.valueOf(o));
	  }
	  // else
	  return -1;
  }



  // TODO: to do the rest of the methods
}
