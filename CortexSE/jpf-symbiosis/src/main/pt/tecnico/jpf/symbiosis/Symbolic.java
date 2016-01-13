package pt.tecnico.jpf.symbiosis;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.AALOAD;
import gov.nasa.jpf.jvm.bytecode.AASTORE;
import gov.nasa.jpf.jvm.bytecode.IALOAD;
import gov.nasa.jpf.jvm.bytecode.IASTORE;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;

import java.util.HashMap;

public class Symbolic {

	static int maxInt = 1000000;
	static int minInt = -1000000;
	static double minDouble = -8;
	static double maxDouble = 7;

	// Stores the fresh symbolic variables.
	private static HashMap<String,Integer> mapSymVarIds = new HashMap<String, Integer>(); //map: var name + thread_pathid -> number of times it was accessed by the same thread
	
	public static void setBoundaries(Config config) {
		int x = config.getInt("symbolic.min_int", Integer.MAX_VALUE);
		if (x != Integer.MAX_VALUE) {
			minInt = x;
		}

		x = config.getInt("symbolic.max_int", Integer.MIN_VALUE);
		if (x != Integer.MIN_VALUE) {
			maxInt = x;
		}

		double z = config.getDouble("symbolic.min_double", Double.MAX_VALUE);
		if (z != Double.MAX_VALUE) {
			minDouble = z;
		}

		z = config.getDouble("symbolic.max_double", Double.MIN_VALUE);
		if (z != Double.MIN_VALUE) {
			maxDouble = z;
		}

		// Display the bounds collected from the configuration
		/*System.out.println("MINE:symbolic.min_int=" + minInt);

		System.out.println("MINE:symbolic.max_int=" + maxInt);

		System.out.println("MINE:symbolic.min_double=" + minDouble);

		System.out.println("MINE:symbolic.max_double=" + maxDouble);*/

	}

	public static String newSymbolic(String type, String symbvar, JVM vm) {
		//System.out.println(" -- type: "+type+" , symbvar: "+symbvar);
		if ((type.compareTo("java.lang.Integer") == 0)
				|| (type.compareTo("int") == 0)) {

			return newSymbolicInteger(symbvar, vm, minInt, maxInt);

		} else if ((type.compareTo("java.lang.String") == 0)) {
			System.out.println("Creating Symbolic String");
			return newSymbolicString(symbvar, vm);

		} else if ((type.compareTo("java.lang.Boolean") == 0)
				|| (type.compareTo("boolean") == 0)) {
			return newSymbolicInteger(symbvar, vm, 0, 1);

		} else if ((type.compareTo("java.lang.Float") == 0)
				|| (type.compareTo("float") == 0)) {

			double min = minDouble;
			double max = maxDouble;
			if (minDouble<Float.MIN_VALUE){
				min = Float.MIN_VALUE;
			}
			if (maxDouble>Float.MAX_VALUE){
				max = Float.MAX_VALUE;
			}
			return newSymbolicReal(symbvar, vm, min, max);

		} else if ((type.compareTo("java.lang.Double") == 0)
				|| (type.compareTo("double") == 0)) {

			return newSymbolicReal(symbvar, vm, minDouble, maxDouble);

		} else if ((type.compareTo("java.lang.Short") == 0)
				|| (type.compareTo("short") == 0)) {

			int min = minInt;
			int max = maxInt;
			if (minInt<Short.MIN_VALUE){
				min = Short.MIN_VALUE;
			}
			if (maxInt>Short.MAX_VALUE){
				max = Short.MAX_VALUE;
			}
			return newSymbolicInteger(symbvar, vm, min, max);

		} else if ((type.compareTo("java.lang.Byte") == 0)
				|| (type.compareTo("byte") == 0)) {

			int min = minInt;
			int max = maxInt;
			if (minInt<Byte.MIN_VALUE){
				min = Byte.MIN_VALUE;
			}
			if (maxInt>Byte.MAX_VALUE){
				max = Byte.MAX_VALUE;
			}
			return newSymbolicInteger(symbvar, vm, min, max);

		} else if ((type.compareTo("java.lang.Long") == 0)
				|| (type.compareTo("long") == 0)) {

			// Cannot be greater than Integer.MAX_VALUE because
			// SymbolicInteger(String name, int min, int max)
			return newSymbolicInteger(symbvar, vm, minInt, maxInt);

		} else if ((type.compareTo("java.lang.Character") == 0)
				|| (type.compareTo("char") == 0)) {

			int min = minInt;
			int max = maxInt;
			if (minInt<0){
				min = 0;
			}
			if (maxInt>65535){
				max = 65535;
			}
			return newSymbolicInteger(symbvar, vm, min, max);

		} /*else if(type.equalsIgnoreCase("int[]") || type.equalsIgnoreCase("long[]")){
			System.out.println("--> symbvar "+symbvar+" (newSymbolicArrayInteger)");
			return newSymbolicArrayInteger(symbvar, vm);
			
		} else if(type.equalsIgnoreCase("float[]") || type.equalsIgnoreCase("double[]")){
			return newSymbolicArrayReal(symbvar, vm);
			
		} else if(type.equalsIgnoreCase("boolean[]")){
			return newSymbolicArrayBoolean(symbvar, vm);
		}	*/	
		else {
			System.out.println("WARNING: Creating a symbolic reference, type: " + type);
			return newSymbolicReference(symbvar, vm);
		}
	}

	private static String newSymbolicInteger(String symbname, JVM vm, int min, int max) {
		IntegerExpression sym_v = new SymbolicInteger(symbname, min, max);
		
		if(vm.getLastThreadInfo().getTopFrame().getTopPos() >= 0){
			vm.getLastThreadInfo().getTopFrame().setOperandAttr(sym_v);
		}
		else{
			System.out.println("[SymbiosisJPF] stack frame is empty! Cannot set attribute "+sym_v);
		}

		return symbname;
	}

	private static String newSymbolicString(String symbname, JVM vm) {
		StringExpression sym_v = new StringSymbolic(symbname);
		
		vm.getLastThreadInfo().getTopFrame().setOperandAttr(sym_v);
		return symbname;
	}

	private static String newSymbolicReal(String symbname,JVM vm, double min, double max) {
		RealExpression sym_v = new SymbolicReal(symbname, min, max);
		vm.getLastThreadInfo().getTopFrame().setOperandAttr(sym_v);
		return symbname;
	}
	
	private static String newSymbolicReference(String symbname, JVM vm) {
		IntegerExpression sym_v = new SymbolicInteger(symbname);
		
		/*if(vm.getLastThreadInfo().getTopFrame().getObjectAttr() == null 
				&& !symbname.equals("R-_head_633-2-2")
				&& !symbname.equals("R-_head_633-2-3")
				&& !symbname.equals("R-_next_637-2-2")){
			System.out.println("[SymbiosisJPF] Object is null. Don't create symbolic reference ("+sym_v+")");
			return symbname;
		}*/
		if(vm.getLastThreadInfo().getTopFrame().getTopPos() >= 0){
			vm.getLastThreadInfo().getTopFrame().setOperandAttr(sym_v);
		}
		else{
			System.out.println("[SymbiosisJPF] stack frame is empty! Cannot set attribute "+sym_v);
		}
		return symbname;
	}
	
	private static String newSymbolicArrayInteger(String symbname, JVM vm)
	{
		ElementInfo eiArray = (ElementInfo)vm.getLastElementInfo();
		Instruction inst = vm.getLastInstruction();
		ThreadInfo ti = vm.getLastThreadInfo();
		int arrayRef = -1;
		int index = -1;
		if(inst instanceof IASTORE
				|| inst instanceof AASTORE){
			arrayRef = ((IASTORE)inst).getArrayRef(ti);
			index = ((IASTORE)inst).getIndex(ti);
		}
		else if(inst instanceof IALOAD
				|| inst instanceof AALOAD){
			arrayRef = ((IALOAD)inst).getArrayRef(ti);
			index = ((IALOAD)inst).getIndex(ti);
		}
		IntegerExpression sym_v = new SymbolicInteger(symbname);
		eiArray.addElementAttr(index, sym_v);
		System.out.println(" newSymbolicArrayInteger ("+symbname+")-> sym_v: "+sym_v+" eiArray: "+eiArray+" inst: "+inst);
		return symbname;
	}
	
	private static String newSymbolicArrayReal(String symbname, JVM vm)
	{
		ElementInfo eiArray = (ElementInfo)vm.getLastElementInfo();

		if(eiArray!=null)
			for(int i =0; i< eiArray.arrayLength(); i++) {
				RealExpression sym_v = new SymbolicReal(symbname+i);
				eiArray.addElementAttr(i, sym_v);
		}
		else
			System.out.println("Warning: input array empty! "+symbname);
		
		return symbname;
	}
	
	private static String newSymbolicArrayBoolean(String symbname, JVM vm)
	{
		ElementInfo eiArray = (ElementInfo)vm.getLastElementInfo();

		if(eiArray!=null)
			for(int i =0; i< eiArray.arrayLength(); i++) {
				IntegerExpression sym_v = new SymbolicInteger(symbname+i,0,1);
				eiArray.addElementAttr(i, sym_v);
		}
		else
			System.out.println("Warning: input array empty! "+symbname);
		
		return symbname;
	}
}
