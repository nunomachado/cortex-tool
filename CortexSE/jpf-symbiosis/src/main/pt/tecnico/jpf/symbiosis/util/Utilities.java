package pt.tecnico.jpf.symbiosis.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import pt.tecnico.jpf.symbiosis.SymbiosisListener;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.Types;

public class Utilities {
	
	public static HashSet<String> storedLogs = new HashSet<String>();
	
	/**
	 * Logs a symbolic event into the corresponding thread's trace file
	 * @param path
	 * @param tid
	 * @param event
	 */
	public static void storeSymbEvent(String folder, String tid, String event, boolean append) {
		try {
			String file = folder+System.getProperty("file.separator")+"T"+tid+"_"+SymbiosisListener.executionId;
			FileWriter fw = new FileWriter(file, append);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(event);
			bw.newLine();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Stores a thread's symbolic trace into a file.
	 * @param path
	 * @param tid
	 * @param event
	 */
	public static void storeSymbLog(String folder, String tid, String trace, String pathid) {
		try {
			//only store if is a new path
			if(storedLogs.contains(tid+pathid.hashCode())){
				System.out.println("[CortexJPF] Redundant trace file. Don't store!");
				return;
			}
			
			String file = folder+System.getProperty("file.separator")+"T"+tid+"_"+SymbiosisListener.executionId+"_"+pathid.hashCode();
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(trace);
			bw.newLine();
			bw.close();
			
			storedLogs.add(tid+pathid.hashCode());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * From a file name with the full path, returns only the name of the file.
	 * @param path
	 * @return
	 */
	public static String getFileShortName(String path)
	{
		return path.substring(path.lastIndexOf('/')+1,path.length());
	}
	
	public static Object transformValueFromLong(JVM vm, long value, String type){
		
		if (type.compareTo("int") == 0){
			return (int)value;
		}else if (type.compareTo("java.lang.Integer") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return (Integer) obj.getFieldValueObject("value");
		
		}else if (type.compareTo("boolean") == 0){
			// 0 : false
			// 1 : true
			return  (int)value;
		
		}else if (type.compareTo("java.lang.Boolean") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return Types.booleanToInt((Boolean) obj.getFieldValueObject("value"));
		
		}else if (type.compareTo("short") == 0){
			return (int) value;
		
		}else if((type.compareTo("java.lang.Short") == 0)){
			ElementInfo obj = vm.getElementInfo((int)value);
			return (Integer) obj.getFieldValueObject("value");
		
		}else if (type.compareTo("byte") == 0){
			return (int) value;
		
		}else if(type.compareTo("java.lang.Byte") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return (Integer) obj.getFieldValueObject("value");
		
		}else if (type.compareTo("long")==0){
			if (value>Long.MAX_VALUE){
				System.out.println("ERROR: Integer value out of range (MAX)");
				return null;
			}else if (value<Long.MIN_VALUE){
				System.out.println("ERROR: Integer value out of range (MIN)");
				return null;
			}
			return (long) value;
		
		}else if (type.compareTo("java.lang.Long") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			int v = (Integer) obj.getFieldValueObject("value");

			if (v>Integer.MAX_VALUE){
				System.out.println("ERROR: Integer value out of range (MAX)");
				return null;
			}else if (v<Integer.MIN_VALUE){
				System.out.println("ERROR: Integer value out of range (MIN)");
				return null;
			}
			return v;
		
		}else if (type.compareTo("char") == 0){
			return (int) value;
		
		}else if(type.compareTo("java.lang.Character") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return Character.getNumericValue((Character) obj.getFieldValueObject("value"));
		
		}else if  (type.compareTo("double") == 0){
			return Types.longToDouble(value);
		
		}else if (type.compareTo("java.lang.Double") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return (Double) obj.getFieldValueObject("value");
		
		}else if (type.compareTo("float") == 0){
			return (Double) ((Float)Types.intToFloat((int)value)).doubleValue();
		
		}else if(type.compareTo("java.lang.Float") == 0){
			ElementInfo obj = vm.getElementInfo((int)value);
			return (Double) obj.getFieldValueObject("value");
		
		}else if ((type.compareTo("java.lang.String") == 0)){
			ElementInfo obj = vm.getElementInfo((int)value);
			String v = obj.asString(); 
			if(v.isEmpty())
				v = "0";
			else if(v.equals("\n")){
				System.out.println(" (Utilities) string is '\\n', so store its numerical value instead.");
				v = String.valueOf(value);
			}
			System.out.println(" (Utilities) WARNING: String: "+v);
			return v;
		
		}else{
			if ((Long)value>Integer.MAX_VALUE){
				System.out.println("ERROR: Integer value out of range (MAX). When creating a reference");
				return null;
			}else if ((Long)value<Integer.MIN_VALUE){
				System.out.println("ERROR: Integer value out of range (MIN). When creating a reference");
				return null;
			}
			System.out.println("WARNING: The value written is not a basic reference. I do not know what to do with it.");
			
			return (int)value;
		}
	}
	
	public static Object transformValue(Object value, String type){
		if (type.compareTo("int") == 0){
			return (Integer)value;
		}else if (type.compareTo("java.lang.Integer") == 0){
			return (Integer) ((ElementInfo)value).getFieldValueObject("value");
		}else if (type.compareTo("boolean") == 0){
			// 0 : false
			// 1 : true
			return Types.booleanToInt((Boolean) value);
		}else if (type.compareTo("java.lang.Boolean") == 0){
			return Types.booleanToInt((Boolean) ((ElementInfo)value).getFieldValueObject("value"));
		}else if (type.compareTo("short") == 0){
			return ((Integer) value);
		}else if((type.compareTo("java.lang.Short") == 0)){
			return (Integer) ((ElementInfo)value).getFieldValueObject("value");
		}else if (type.compareTo("byte") == 0){
			return ((Integer) value);
		}else if(type.compareTo("java.lang.Byte") == 0){
			return (Integer) ((ElementInfo)value).getFieldValueObject("value");
		}else if (type.compareTo("long")==0){
			if ((Long)value>Integer.MAX_VALUE){
				System.out.println("ERROR: Integer value out of range (MAX)");
				return null;
			}else if ((Long)value<Integer.MIN_VALUE){
				System.out.println("ERROR: Integer value out of range (MIN)");
				return null;
			}
			return (Integer)value;
		}else if (type.compareTo("java.lang.Long") == 0){

			int v = (Integer) ((ElementInfo)value).getFieldValueObject("value");

			if (v>Integer.MAX_VALUE){
				System.out.println("ERROR: Integer value out of range (MAX)");
				return null;
			}else if (v<Integer.MIN_VALUE){
				System.out.println("ERROR: Integer value out of range (MIN)");
				return null;
			}
			return v;
		}else if (type.compareTo("char") == 0){
			return Character.getNumericValue((Character)value);
		}else if(type.compareTo("java.lang.Character") == 0){
			return Character.getNumericValue((Character) ((ElementInfo)value).getFieldValueObject("value"));
		}else if  (type.compareTo("double") == 0){
			return (Double)value;
		}else if (type.compareTo("java.lang.Double") == 0){
			return (Double) ((ElementInfo)value).getFieldValueObject("value");
		}else if (type.compareTo("float") == 0){
			return (Double)value;
		}else if(type.compareTo("java.lang.Float") == 0){
			return (Double) ((ElementInfo)value).getFieldValueObject("value");
		}else if ((type.compareTo("java.lang.String") == 0)){
			String v = ((ElementInfo)value).asString();
			System.out.println("WARNING: String: "+v);
			return v;
		}else{
			//DynamicElementInfo dinfo = (DynamicElementInfo) value;
			//System.out.println("WARNING: Not basic reference. I do not know what to do with it");
			if (value==null){
				//System.out.println("WARNING: reference == null");
				return -1;
			}else{
				return ((ElementInfo) value).getObjectRef();
			}
			//return dinfo.getObjectRef();
		}
	}

	public static Type simplifyTypeFromType(Type t){
		int type = t.getCode();
		if ((4<=type)&&(type<=8)){
			return Type.INT;
		}else if (type == 9){
			return Type.REAL;
		}else if (type == 0){
			return Type.INT;
		}else{
			return t;
		}
	}

	public static Type simplifyTypeFromString(String t){
		Type type = Utilities.typeToInteger(t);
		return Utilities.simplifyTypeFromType(type);
	}

	public static Type typeToInteger(String type){
		if ((type.compareTo("java.lang.Integer") == 0) || (type.compareTo("int") == 0) || (type.equals("int[]"))){
			return Type.INT;
		}else if ((type.compareTo("java.lang.Boolean") == 0) || (type.compareTo("boolean") == 0)){
			return Type.BOOLEAN;
		}else if ((type.compareTo("java.lang.Short") == 0) || (type.compareTo("short") == 0) || (type.equals("short[]"))){
			return Type.SHORT;
		}else if ((type.compareTo("java.lang.Byte") == 0) || (type.compareTo("byte") == 0) || (type.equals("byte[]"))){
			return Type.BYTE;
		}else if ((type.compareTo("java.lang.Long") == 0) || (type.compareTo("long") == 0) || (type.equals("long[]"))){
			return Type.LONG;
		}else if ((type.compareTo("java.lang.Character") == 0) || (type.compareTo("char") == 0) || (type.equals("char[]"))){
			return Type.CHAR;
		}else if ((type.compareTo("java.lang.Double") == 0) || (type.compareTo("double") == 0) || (type.equals("double[]"))){
			return Type.REAL;
		}else if ((type.compareTo("java.lang.Float") == 0) || (type.compareTo("float") == 0) || (type.equals("float[]"))){
			return Type.FLOAT;
		}else if ((type.compareTo("java.lang.String") == 0)){
			return Type.STRING;
		}else{
			return Type.REFERENCE;
		}
	}
}
