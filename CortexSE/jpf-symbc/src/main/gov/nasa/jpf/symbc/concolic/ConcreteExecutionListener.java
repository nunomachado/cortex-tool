package gov.nasa.jpf.symbc.concolic;

import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.numeric.PathCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.AnnotationInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.LocalVarInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.EXECUTENATIVE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.InvokeInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.RealConstant;
public class ConcreteExecutionListener extends PropertyListenerAdapter {

	Config config;
	public static boolean debug = false;
	long ret;
	Object resultAttr;
	String[] partitions;

	public enum type {
		INT, DOUBLE, FLOAT, BYTE,
		SHORT, LONG, BOOLEAN, CHAR
	}

	public ConcreteExecutionListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		this.config = conf;
	}

	public void instructionExecuted(JVM vm) {
		Instruction lastInsn =  vm.getLastInstruction();
		MethodInfo mi = vm.getCurrentThread().getMethod();
		if(lastInsn != null && lastInsn instanceof InvokeInstruction) {
			boolean foundAnote = checkConcreteAnnotation(mi);
			if(foundAnote) {
				ThreadInfo ti = vm.getCurrentThread();
				StackFrame sf = ti.popFrame();
				FunctionExpression result =
					generateFunctionExpression(mi, (InvokeInstruction)
													lastInsn, ti);
				checkReturnType(ti, mi, result);
				Instruction nextIns = sf.getPC().getNext();
			    vm.getCurrentThread().skipInstruction(nextIns);
			}
		}
	}


	private boolean checkConcreteAnnotation(MethodInfo mi) {
		AnnotationInfo[] ai = mi.getAnnotations();
		boolean retVal = false;
		if(ai == null || ai.length == 0)  return retVal;
		for(int aIndex = 0; aIndex < ai.length; aIndex++) {
			AnnotationInfo annotation = ai[aIndex];
			if(annotation.getName().equals
							("gov.nasa.jpf.symbc.Concrete")) {
				if(annotation.valueAsString().
									equalsIgnoreCase("true"))
					retVal = true;
				else
					retVal = false;
			} else if (annotation.getName().equals
					("gov.nasa.jpf.symbc.Partition"))	 {

				partitions = annotation.getValueAsStringArray();
//				if (SymbolicInstructionFactory.debugMode)
//					for(int i = 0; i < partitions.length; i++)
//						System.out.println("discovered partition "+partitions[i]);
			}
		}
		return retVal;
	}

	private FunctionExpression generateFunctionExpression(MethodInfo mi,
			InvokeInstruction ivk, ThreadInfo ti){
		Object [] attrs = ivk.getArgumentAttrs(ti);
		Object [] values = ivk.getArgumentValues(ti);
		String [] types = mi.getArgumentTypeNames();

		assert (attrs != null);

		assert (attrs.length == values.length &&
						values.length == types.length);
		int size = attrs.length;

		Class<?>[] args_type = new Class<?> [size];
		Expression[] sym_args = new Expression[size];

		Map<String,Expression> expressionMap =
			new HashMap<String, Expression>();
		LocalVarInfo[] localVars = mi.getLocalVars();

		for(int argIndex = 0; argIndex < size; argIndex++) {
			Object attribute = attrs[argIndex];
			if(attribute == null) {
				sym_args[argIndex] = this.generateConstant(
								types[argIndex],
								values[argIndex]);
			} else {
				sym_args[argIndex] = (Expression) attribute;
				if(localVars.length > argIndex)
					expressionMap.put(localVars[argIndex].getName(),
						sym_args[argIndex]);


			}
			args_type[argIndex] = checkArgumentTypes(types[argIndex]);
		}

		ArrayList<PathCondition> conditions = Partition.
							createConditions(partitions, expressionMap);


		FunctionExpression result = new FunctionExpression(
				  mi.getClassName(),
				  mi.getName(), args_type, sym_args, conditions);

		return result;
	}


	private void checkReturnType(ThreadInfo ti, MethodInfo mi, Object resultAttr) {
		String retTypeName = mi.getReturnTypeName();
		ti.removeArguments(mi);
		if(retTypeName.equals("double") || retTypeName.equals("long")) {
			ti.longPush(0);
			ti.setLongOperandAttr(resultAttr);
		} else {
			ti.push(0);
			ti.setOperandAttr(resultAttr);
		}
	}



	private Class<?> checkArgumentTypes(String typeVal) {
		if(typeVal.equals("int")) {
			return Integer.TYPE;
		} else if (typeVal.equals("double")) {
			return Double.TYPE;
		} else if (typeVal.equals("float")) {
			return Float.TYPE;
		} else if (typeVal.equals("long")) {
			return Long.TYPE;
		} else if (typeVal.equals("short")) {
			return Short.TYPE;
		}  else if (typeVal.equals("boolean")) {
			return Boolean.TYPE;
		} else {
			throw new RuntimeException("the type not handled :" + typeVal);
		}
	}

	private Expression generateConstant(String typeVal, Object value) {
		if(typeVal.equals("int")) {
			return new IntegerConstant(Integer.parseInt
					(value.toString()));
		} else if (typeVal.equals("double")) {
			return new RealConstant(Double.parseDouble
					(value.toString()));
		} else if (typeVal.equals("float")) {
			return new RealConstant(Float.parseFloat
					(value.toString()));
		} else if (typeVal.equals("long")) {
			return new IntegerConstant((int) Long.parseLong
					(value.toString()));
		} else if (typeVal.equals("short")) {
			return new IntegerConstant((int) Short.parseShort
					(value.toString()));
		} else if (typeVal.equals("boolean")) {
			if(value.toString().equals("true")) {
				return new IntegerConstant(1);
			} else {
				return new IntegerConstant(0);
			}
		} else {
			throw new RuntimeException("the type not handled :" + typeVal);
		}
	}

}