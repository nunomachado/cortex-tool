package pt.tecnico.jpf.symbiosis;

import gov.nasa.jpf.symbc.numeric.solvers.ProblemGeneral;


/**
 * Integration of the Z3
 * 
 * @author Nuno Machado
 * 
 */

public class Z3Solver extends ProblemGeneral {

	
	public Z3Solver() {}

	public Object and(int value, Object exp) {
		return "(and "+value+" "+exp+")";
	}

	public Object and(Object exp, int value) {
		return "(and "+exp+" "+value+")";
	}

	public Object and(Object exp1, Object exp2) {
		return "(and "+exp1+" "+exp2+")";
	}
	
	public Object and(String concat){
		return "(and "+concat+")";
	}

	public Object div(int value, Object exp) {

		return "(div "+value+" "+exp+")";
	}

	public Object div(Object exp, int value) {
		return "(div "+exp+" "+value+")";
	}

	public Object div(Object exp1, Object exp2) {
		return "(div "+exp1+" "+exp2+")";
	}

	public Object div(double value, Object exp) {
		return "(div "+value+" "+exp+")";
	}

	public Object div(Object exp, double value) {
		return "(div "+exp+" "+value+")";
	}

	public Object mod(Object exp1, Object exp2) {
		return "(mod "+exp1+" "+exp2+")";
	}

	public Object mod(int value, Object exp) {
		return "(mod "+value+" "+exp+")";
	}

	public Object mod(Object exp, int value) {
		return "(mod "+exp+" "+value+")";
	}

	public Object eq(int value, Object exp) {
		return "(= "+value+" "+exp+")";
	}

	public Object eq(Object exp, int value) {
		return "(= "+exp+" "+value+")";
	}

	public Object eq(Object exp1, Object exp2) {
		return "(= "+exp1+" "+exp2+")";
	}

	public Object eq(double value, Object exp) {
		return "(= "+value+" "+exp+")";
	}

	public Object eq(Object exp, double value) {
		return "(= "+exp+" "+value+")";
	}

	public Object geq(int value, Object exp) {
		return "(>= "+value+" "+exp+")";
	}

	public Object geq(Object exp, int value) {
		return "(>= "+exp+" "+value+")";
	}

	public Object geq(Object exp1, Object exp2) {
		return "(>= "+exp1+" "+exp2+")";
	}

	public Object geq(double value, Object exp) {
		return "(>= "+value+" "+exp+")";
	}

	public Object geq(Object exp, double value) {
		return "(>= "+exp+" "+value+")";
	}

	public double getRealValue(Object dpVar) {
		throw new RuntimeException("## Unsupported get real value ");
	}

	public double getRealValueInf(Object dpvar) {
		throw new RuntimeException("## Unsupported get real value ");
	}

	public double getRealValueSup(Object dpVar) {
		throw new RuntimeException("## Unsupported get real value ");
	}

	public Object gt(int value, Object exp) {
		return "(> "+value+" "+exp+")";
	}

	public Object gt(Object exp, int value) {
		return "(> "+exp+" "+value+")";
	}

	public Object gt(Object exp1, Object exp2) {
		return "(> "+exp1+" "+exp2+")";
	}

	public Object gt(double value, Object exp) {
		return "(> "+value+" "+exp+")";
	}

	public Object gt(Object exp, double value) {
		return "(> "+exp+" "+value+")";
	}

	public Object leq(int value, Object exp) {
		return "(<= "+value+" "+exp+")";
	}

	public Object leq(Object exp, int value) {
		return "(<= "+exp+" "+value+")";
	}

	public Object leq(Object exp1, Object exp2) {
		return "(<= "+exp1+" "+exp2+")";
	}

	public Object leq(double value, Object exp) {
		return "(<= "+value+" "+exp+")";
	}

	public Object leq(Object exp, double value) {
		return "(<= "+exp+" "+value+")";
	}

	public Object lt(int value, Object exp) {
		return "(< "+value+" "+exp+")";
	}

	public Object lt(Object exp, int value) {
		return "(< "+exp+" "+value+")";
	}

	public Object lt(Object exp1, Object exp2) {
		return "(< "+exp1+" "+exp2+")";
	}

	public Object lt(double value, Object exp) {
		return "(< "+value+" "+exp+")";
	}

	public Object lt(Object exp, double value) {
		return "(< "+exp+" "+value+")";
	}

	public String summation(String[] sum){
		StringBuilder finalString = new StringBuilder("(+");
		for (String s : sum){
			finalString.append(" "+s);
		}
		finalString.append(")");
		return finalString.toString();
	}
	
	public Object minus(int value, Object exp) {
		return "(- "+value+" "+exp+")";
	}

	public Object minus(Object exp, int value) {
		return "(- "+exp+" "+value+")";
	}

	public Object minus(Object exp1, Object exp2) {
		return "(- "+exp1+" "+exp2+")";
	}

	public Object minus(double value, Object exp) {
		return "(- "+value+" "+exp+")";
	}

	public Object minus(Object exp, double value) {
		return "(- "+exp+" "+value+")";
	}

	public Object mixed(Object exp1, Object exp2) {
		throw new RuntimeException("## Unsupported mixed ");
	}

	public Object mult(int value, Object exp) {
		return "(* "+value+" "+exp+")";
	}

	public Object mult(Object exp, int value) {
		return "(* "+exp+" "+value+")";
	}

	public Object mult(Object exp1, Object exp2) {
		return "(* "+exp1+" "+exp2+")";
	}

	public Object mult(double value, Object exp) {
		return "(* "+value+" "+exp+")";
	}

	public Object mult(Object exp, double value) {
		return "(* "+exp+" "+value+")";
	}

	public Object neq(int value, Object exp) {
		return "(not (= "+value+" "+exp+"))";
	}

	public Object neq(Object exp, int value) {
		return "(not (= "+exp+" "+value+"))";
	}

	public Object neq(Object exp1, Object exp2) {
		return "(not (= "+exp1+" "+exp2+"))";
	}

	public Object neq(double value, Object exp) {
		return "(not (= "+value+" "+exp+"))";
	}

	public Object neq(Object exp, double value) {
		return "(not (= "+exp+" "+value+"))";
	}

	public Object or(int value, Object exp) {
		return "(or "+value+" "+exp+")";
	}

	public Object or(Object exp, int value) {
		return "(or "+exp+" "+value+")";
	}

	public Object or(Object exp1, Object exp2) {
		return "(or "+exp1+" "+exp2+")";
	}
	
	public Object or(String concat){
		return "(or "+concat+")";
	}

	public Object plus(int value, Object exp) {
		return "(+ "+value+" "+exp+")";
	}

	public Object plus(Object exp, int value) {
		return "(+ "+exp+" "+value+")";
	}

	public Object plus(Object exp1, Object exp2) {
		return "(+ "+exp1+" "+exp2+")";
	}

	public Object plus(double value, Object exp) {
		return "(+ "+value+" "+exp+")";
	}

	public Object plus(Object exp, double value) {
		return "(+ "+exp+" "+value+")";
	}

	public Object shiftL(int value, Object exp) {
		throw new RuntimeException("## Unsupported shiftL");
	}

	public Object shiftL(Object exp, int value) {
		throw new RuntimeException("## Unsupported shiftL");
	}

	public Object shiftL(Object exp1, Object exp2) {
		throw new RuntimeException("## Unsupported shiftL");
	}

	public Object shiftR(int value, Object exp) {
		throw new RuntimeException("## Unsupported shiftR");
	}

	public Object shiftR(Object exp, int value) {
		throw new RuntimeException("## Unsupported shiftR");
	}

	public Object shiftR(Object exp1, Object exp2) {
		throw new RuntimeException("## Unsupported shiftR");
	}

	public Object shiftUR(int value, Object exp) {
		throw new RuntimeException("## Unsupported shiftUR");
	}

	public Object shiftUR(Object exp, int value) {
		throw new RuntimeException("## Unsupported shiftUR");
	}

	public Object shiftUR(Object exp1, Object exp2) {
		throw new RuntimeException("## Unsupported shiftUR");
	}

	public Object xor(int value, Object exp) {
		throw new RuntimeException("## Unsupported XOR ");
	}

	public Object xor(Object exp, int value) {
		throw new RuntimeException("## Unsupported XOR");
	}

	public Object xor(Object exp1, Object exp2) {
		throw new RuntimeException("## Unsupported XOR");
	}

	
	//Nuno: we are not currently using the following method in Symbiosis, because we
	// don't want to solve the model directly in JPF
	@Override
	public void postLogicalOR(Object[] constraint) {
		// TODO Auto-generated method stub
		throw new RuntimeException("## Error Choco2 does not support LogicalOR");
	}

	@Override
	public Object makeIntVar(String name, int min, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object makeRealVar(String name, double min, double max) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean solve() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntValue(Object dpVar) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void post(Object constraint) {
		// TODO Auto-generated method stub
		
	}

}
