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
 * MJI NativePeer class for java.lang.Character library abstraction
 * Whoever is using this seriously is definitely screwed, performance-wise
 */
public class JPF_java_lang_Character {
  // <2do> at this point we deliberately do not override clinit

  public static boolean isDefined__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isDefined(c);
  }

  public static boolean isDigit__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isDigit(c);
  }

  public static boolean isISOControl__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isISOControl(c);
  }

  public static boolean isIdentifierIgnorable__C__Z (MJIEnv env, int clsObjRef, 
                                                  char c) {
    return Character.isIdentifierIgnorable(c);
  }

  public static boolean isJavaIdentifierPart__C__Z (MJIEnv env, int clsObjRef, 
                                                 char c) {
    return Character.isJavaIdentifierPart(c);
  }

  public static boolean isJavaIdentifierStart__C__Z (MJIEnv env, int clsObjRef, 
                                                  char c) {
    return Character.isJavaIdentifierStart(c);
  }

  public static boolean isJavaLetterOrDigit__C__Z (MJIEnv env, int clsObjRef, 
                                                char c) {
    return Character.isJavaIdentifierPart(c);
  }

  public static boolean isJavaLetter__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isJavaIdentifierStart(c);
  }

  public static boolean isLetterOrDigit__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isLetterOrDigit(c);
  }

  public static boolean isLetter__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isLetter(c);
  }

  public static boolean isLowerCase__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isLowerCase(c);
  }

  public static int getNumericValue__C__I (MJIEnv env, int clsObjRef, char c) {
    return Character.getNumericValue(c);
  }

  public static boolean isSpaceChar__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isSpaceChar(c);
  }

  public static boolean isSpace__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isWhitespace(c);
  }

  public static boolean isTitleCase__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isTitleCase(c);
  }

  public static int getType__C__I (MJIEnv env, int clsObjRef, char c) {
    return Character.getType(c);
  }

  public static boolean isUnicodeIdentifierPart__C__Z (MJIEnv env, int clsObjRef, 
                                                    char c) {
    return Character.isUnicodeIdentifierPart(c);
  }

  public static boolean isUnicodeIdentifierStart__C__Z (MJIEnv env, int clsObjRef, 
                                                     char c) {
    return Character.isUnicodeIdentifierStart(c);
  }

  public static boolean isUpperCase__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isUpperCase(c);
  }

  public static boolean isWhitespace__C__Z (MJIEnv env, int clsObjRef, char c) {
    return Character.isWhitespace(c);
  }

  // pcm - we keep this in here to avoid the potentially expensive
  // real clinit. This has changed a lot in Java 1.4.2 (deferred init, i.e.
  // we could actually use it now), but in <= 1.4.1 it executes some
  // 200,000 insns, and some people who didn't grew up with Java might
  // deduce that JPF is hanging. It's not, it just shows why a real VM has to
  // be fast.
  // It is actually Ok to bypass the real clinit if we turn all the
  // important methods into native ones, i.e. delegate to the real thing.
  public static void $clinit____V (MJIEnv env, int clsObjRef) {
    env.setStaticByteField("java.lang.Character", "UNASSIGNED", (byte) 0);
    env.setStaticByteField("java.lang.Character", "UPPERCASE_LETTER", (byte) 1);
    env.setStaticByteField("java.lang.Character", "LOWERCASE_LETTER", (byte) 2);
    env.setStaticByteField("java.lang.Character", "TITLECASE_LETTER", (byte) 3);
    env.setStaticByteField("java.lang.Character", "MODIFIER_LETTER", (byte) 4);
    env.setStaticByteField("java.lang.Character", "OTHER_LETTER", (byte) 5);
    env.setStaticByteField("java.lang.Character", "NON_SPACING_MARK", (byte) 6);
    env.setStaticByteField("java.lang.Character", "ENCLOSING_MARK", (byte) 7);
    env.setStaticByteField("java.lang.Character", "COMBINING_SPACING_MARK", (byte) 8);
    env.setStaticByteField("java.lang.Character", "DECIMAL_DIGIT_NUMBER", (byte) 9);
    env.setStaticByteField("java.lang.Character", "LETTER_NUMBER", (byte) 10);
    env.setStaticByteField("java.lang.Character", "OTHER_NUMBER", (byte) 11);
    env.setStaticByteField("java.lang.Character", "SPACE_SEPARATOR", (byte) 12);
    env.setStaticByteField("java.lang.Character", "LINE_SEPARATOR", (byte) 13);
    env.setStaticByteField("java.lang.Character", "PARAGRAPH_SEPARATOR", (byte) 14);
    env.setStaticByteField("java.lang.Character", "CONTROL", (byte) 15);
    env.setStaticByteField("java.lang.Character", "FORMAT", (byte) 16);
    env.setStaticByteField("java.lang.Character", "PRIVATE_USE", (byte) 18);
    env.setStaticByteField("java.lang.Character", "SURROGATE", (byte) 19);
    env.setStaticByteField("java.lang.Character", "DASH_PUNCTUATION", (byte) 20);
    env.setStaticByteField("java.lang.Character", "START_PUNCTUATION", (byte) 21);
    env.setStaticByteField("java.lang.Character", "END_PUNCTUATION", (byte) 22);
    env.setStaticByteField("java.lang.Character", "CONNECTOR_PUNCTUATION", (byte) 23);
    env.setStaticByteField("java.lang.Character", "OTHER_PUNCTUATION", (byte) 24);
    env.setStaticByteField("java.lang.Character", "MATH_SYMBOL", (byte) 25);
    env.setStaticByteField("java.lang.Character", "CURRENCY_SYMBOL", (byte) 26);
    env.setStaticByteField("java.lang.Character", "MODIFIER_SYMBOL", (byte) 27);
    env.setStaticByteField("java.lang.Character", "OTHER_SYMBOL", (byte) 28);
    env.setStaticIntField("java.lang.Character", "MIN_RADIX", 2);
    env.setStaticIntField("java.lang.Character", "MAX_RADIX", 36);
    env.setStaticCharField("java.lang.Character", "MIN_VALUE", '\u0000');
    env.setStaticCharField("java.lang.Character", "MAX_VALUE", '\uffff');

    ClassInfo ci = ClassInfo.getResolvedClassInfo("char");
    env.setStaticReferenceField("java.lang.Character", "TYPE", 
                             ci.getClassObjectRef());

    env.setBooleanField(ci.getClassObjectRef(), "isPrimitive", true);
  }

  public static int digit__CI__I (MJIEnv env, int clsObjRef, char c, int radix) {
    return Character.digit(c, radix);
  }

  public static char forDigit__II__C (MJIEnv env, int clsObjRef, int digit, 
                                   int radix) {
    return Character.forDigit(digit, radix);
  }

  public static char toLowerCase__C__C (MJIEnv env, int clsObjRef, char c) {
    return Character.toLowerCase(c);
  }

  public static char toTitleCase__C__C (MJIEnv env, int clsObjRef, char c) {
    return Character.toTitleCase(c);
  }

  public static char toUpperCase__C__C (MJIEnv env, int clsObjRef, char c) {
    return Character.toUpperCase(c);
  }

  public static int valueOf__C__Ljava_lang_Character_2 (MJIEnv env, int clsRef, char val) {
    return env.valueOfCharacter(val);
  }
}
