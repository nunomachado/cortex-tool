//
// Copyright (C) 2011 United States Government as represented by the
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

import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.Invocation;

import java.util.List;

/**
 * interface for bytecode creation
 *
 * this deliberately uses the abstract Instruction as return type to allow
 * different instruction hierarchies in extensions.
 *
 * This shouldn't impose runtime overhead since mandatory parameters are
 * now passed in as factory method arguments. The only drawback is that
 * the compiler cannot check for Instruction class typos, but that seems less
 * important than allowing extension specific Instruction class hierarchies
 *
 * <2do> there are still direct references of LOOKUPSWITCH, TABLESWITCH. Once these
 * are removed, .jvm does not assume a particular Instruction hierarchy
 */
public interface InstructionFactory extends Cloneable {

void setClassInfoContext( ClassInfo ci);

Object clone();

  //--- the factory methods
Instruction aconst_null();

Instruction aload(int localVarIndex);

Instruction aload_0();

Instruction aload_1();

Instruction aload_2();

Instruction aload_3();

Instruction aaload();

Instruction astore(int localVarIndex);

Instruction astore_0();

Instruction astore_1();

Instruction astore_2();

Instruction astore_3();

Instruction aastore();

Instruction areturn();

Instruction anewarray(String clsName);

Instruction arraylength();

Instruction athrow();

Instruction baload();

Instruction bastore();

Instruction bipush(int b);

Instruction caload();

Instruction castore();

Instruction checkcast(String clsName);

Instruction d2f();

Instruction d2i();

Instruction d2l();

Instruction dadd();

Instruction daload();

Instruction dastore();

Instruction dcmpg();

Instruction dcmpl();

Instruction dconst_0();

Instruction dconst_1();

Instruction ddiv();

Instruction dload(int localVarIndex);

Instruction dload_0();

Instruction dload_1();

Instruction dload_2();

Instruction dload_3();

Instruction dmul();

Instruction dneg();

Instruction drem();

Instruction dreturn();

Instruction dstore(int localVarIndex);

Instruction dstore_0();

Instruction dstore_1();

Instruction dstore_2();

Instruction dstore_3();

Instruction dsub();

Instruction dup();

Instruction dup_x1();

Instruction dup_x2();

Instruction dup2();

Instruction dup2_x1();

Instruction dup2_x2();

Instruction f2d();

Instruction f2i();

Instruction f2l();

Instruction fadd();

Instruction faload();

Instruction fastore();

Instruction fcmpg();

Instruction fcmpl();

Instruction fconst_0();

Instruction fconst_1();

Instruction fconst_2();

Instruction fdiv();

Instruction fload(int localVarIndex);

Instruction fload_0();

Instruction fload_1();

Instruction fload_2();

Instruction fload_3();

Instruction fmul();

Instruction fneg();

Instruction frem();

Instruction freturn();

Instruction fstore(int localVarIndex);

Instruction fstore_0();

Instruction fstore_1();

Instruction fstore_2();

Instruction fstore_3();

Instruction fsub();

Instruction getfield(String fieldName, String clsName, String fieldDescriptor);

Instruction getstatic(String fieldName, String clsName, String fieldDescriptor);

Instruction goto_(int targetPc);

Instruction goto_w(int targetPc);

Instruction i2b();

Instruction i2c();

Instruction i2d();

Instruction i2f();

Instruction i2l();

Instruction i2s();

Instruction iadd();

Instruction iaload();

Instruction iand();

Instruction iastore();

Instruction iconst_m1();

Instruction iconst_0();

Instruction iconst_1();

Instruction iconst_2();

Instruction iconst_3();

Instruction iconst_4();

Instruction iconst_5();

Instruction idiv();

Instruction if_acmpeq(int targetPc);

Instruction if_acmpne(int targetPc);

Instruction if_icmpeq(int targetPc);

Instruction if_icmpne(int targetPc);

Instruction if_icmplt(int targetPc);

Instruction if_icmpge(int targetPc);

Instruction if_icmpgt(int targetPc);

Instruction if_icmple(int targetPc);

Instruction ifeq(int targetPc);

Instruction ifne(int targetPc);

Instruction iflt(int targetPc);

Instruction ifge(int targetPc);

Instruction ifgt(int targetPc);

Instruction ifle(int targetPc);

Instruction ifnonnull(int targetPc);

Instruction ifnull(int targetPc);

Instruction iinc(int localVarIndex, int incConstant);

Instruction iload(int localVarIndex);

Instruction iload_0();

Instruction iload_1();

Instruction iload_2();

Instruction iload_3();

Instruction imul();

Instruction ineg();

Instruction instanceof_(String clsName);

Instruction invokeinterface(String clsName, String methodName, String methodSignature);

Instruction invokespecial(String clsName, String methodName, String methodSignature);

Instruction invokestatic(String clsName, String methodName, String methodSignature);

Instruction invokevirtual(String clsName, String methodName, String methodSignature);

Instruction ior();

Instruction irem();

Instruction ireturn();

Instruction ishl();

Instruction ishr();

Instruction istore(int localVarIndex);

Instruction istore_0();

Instruction istore_1();

Instruction istore_2();

Instruction istore_3();

Instruction isub();

Instruction iushr();

Instruction ixor();

Instruction jsr(int targetPc);

Instruction jsr_w(int targetPc);

Instruction l2d();

Instruction l2f();

Instruction l2i();

Instruction ladd();

Instruction laload();

Instruction land();

Instruction lastore();

Instruction lcmp();

Instruction lconst_0();

Instruction lconst_1();

Instruction ldc(int v);
Instruction ldc(float v);
Instruction ldc(String v, boolean isClass);

Instruction ldc_w(int v);
Instruction ldc_w(float v);
Instruction ldc_w(String v, boolean isClass);

Instruction ldc2_w(long v);
Instruction ldc2_w(double v);

Instruction ldiv();

Instruction lload(int localVarIndex);

Instruction lload_0();

Instruction lload_1();

Instruction lload_2();

Instruction lload_3();

Instruction lmul();

Instruction lneg();

Instruction lookupswitch(int defaultTargetPc, int nEntries);

Instruction lor();

Instruction lrem();

Instruction lreturn();

Instruction lshl();

Instruction lshr();

Instruction lstore(int localVarIndex);

Instruction lstore_0();

Instruction lstore_1();

Instruction lstore_2();

Instruction lstore_3();

Instruction lsub();

Instruction lushr();

Instruction lxor();

Instruction monitorenter();

Instruction monitorexit();

Instruction multianewarray(String clsName, int dimensions);

Instruction new_(String clsName);

Instruction newarray(int typeCode);

Instruction nop();

Instruction pop();

Instruction pop2();

Instruction putfield(String fieldName, String clsName, String fieldDescriptor);

Instruction putstatic(String fieldName, String clsName, String fieldDescriptor);

Instruction ret(int localVarIndex);

Instruction return_();

Instruction saload();

Instruction sastore();

Instruction sipush(int val);

Instruction swap();

Instruction tableswitch(int defaultTargetPc, int low, int high);

Instruction wide();


  //--- the JPF specific ones (only used in synthetic methods)
Instruction invokecg(List<Invocation> invokes);

Instruction invokeclinit(ClassInfo ci);

Instruction directcallreturn();

Instruction executenative(NativeMethodInfo mi);

Instruction nativereturn();

  // this is never part of MethodInfo stored code
Instruction runstart(MethodInfo miRun);
}
