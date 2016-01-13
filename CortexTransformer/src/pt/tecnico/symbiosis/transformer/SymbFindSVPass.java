package pt.tecnico.symbiosis.transformer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import pt.tecnico.symbiosis.context.RefContext;
import soot.Body;
import soot.BodyTransformer;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ConcreteRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.util.Chain;


/**
 * Body transformer that finds shared variables.
 * This pass is used as a pre-processing phase to
 * log information on accesses to shared variables.
 * @author nunomachado
 *
 */
public class SymbFindSVPass extends BodyTransformer{
	
	public static SymbFindSVPass instance = new SymbFindSVPass();
	public SymbFindSVPass() {}

	public static SymbFindSVPass v() { return instance; }

	@Override
	protected void internalTransform(Body body, String phase, Map options) 
	{
		Chain units = body.getUnits();
		Iterator stmtIt = units.snapshotIterator();  

		SootMethod m = body.getMethod();
		
		while (stmtIt.hasNext()) 
		{
			Stmt s = (Stmt) stmtIt.next();
			checkForSharedVars(m,s);
		}
	}

	
	/**
	 * Checks whether the current stmt has an access to a shared variable.
	 * @param s
	 */
	public static void checkForSharedVars(SootMethod sm, Stmt s)
	{
		if(s instanceof AssignStmt)
		{
			Value left = ((AssignStmt)s).getLeftOp();
			Value right = ((AssignStmt)s).getRightOp();
			RefContext context;
			
			//handle write accesses
			if (left instanceof ConcreteRef) {
				
				//context = LHSContextImpl.getInstance();
				//if (context != RHSContextImpl.getInstance())
				//{
					if (((ConcreteRef) left) instanceof InstanceFieldRef)
					{
						InstanceFieldRef fieldRef = ((InstanceFieldRef) left);
						SootField field  = fieldRef.getField();
						String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".INSTANCE";
						
						if(shouldInstrument(fieldRef, sm))
						{
							SymbiosisTransformer.sharedVars.add(sig);
						}
					}
					else if(((ConcreteRef) left) instanceof StaticFieldRef)
					{
						StaticFieldRef fieldRef = ((StaticFieldRef) left);
						SootField field  = fieldRef.getField();
						String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".STATIC";
						
						if(shouldInstrument(fieldRef, sm))
						{
							SymbiosisTransformer.sharedVars.add(sig);
						}
					}
				//}
		    }
		    
		  //handle read accesses
		    if (right instanceof ConcreteRef) {
		    	if (((ConcreteRef) right) instanceof InstanceFieldRef)
		    	{
		    		InstanceFieldRef fieldRef = ((InstanceFieldRef) right);
		    		SootField field  = fieldRef.getField();
		    		String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".INSTANCE";
		    		
		    		if(shouldInstrument(fieldRef, sm))
		    		{
		    			SymbiosisTransformer.sharedVars.add(sig);
		    		}
		    	}
		    	else if(((ConcreteRef) right) instanceof StaticFieldRef)
		    	{
		    		StaticFieldRef fieldRef = ((StaticFieldRef) right);
		    		SootField field  = fieldRef.getField();
		    		String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".STATIC";
		    		
		    		if(shouldInstrument(fieldRef, sm))
		    		{
		    			SymbiosisTransformer.sharedVars.add(sig);
		    		}
		    	}
		    }
		}
	}
	
	
	/**
	 * Returns true if the field is shared and should be instrumented.
	 * @param field
	 * @return
	 */
	public static boolean shouldInstrument(FieldRef fieldRef, SootMethod sm)
	{
		SootField field = fieldRef.getField();
		
		//just to exclude some particular cases...
		String sig = field.getDeclaringClass().toString();
		if(sig.contains("$"))
			sig = sig.substring(0,sig.indexOf("$"));
		//System.out.println(" -- SHOULD INSTRUMENT "+sig+"."+field.getName()+"? "+!SymbiosisTransformer.tlo.isObjectThreadLocal(fieldRef, sm));
		
		//list with hardcoded variables to mark as symbolic
		HashSet<String> hardCodedList = new HashSet<String>();
		hardCodedList.add("bubbleSortOriginal.OneBubble.arr");
		hardCodedList.add("bubbleSortOriginal.Reporter.printedArray");
		hardCodedList.add("bubbleSortOriginal.SoftWareVerificationHW.arr");
		hardCodedList.add("bubbleSortOriginal.SoftWareVerificationHW.hasBug");
		
		return (hardCodedList.contains(sig+"."+field.getName()) || (
				SymbiosisTransformer.ftea.isFieldThreadShared(field) &&  			  //uncommented: this may exclude some relevant variables*/
			    // !SymbiosisTransformer.tlo.isObjectThreadLocal(fieldRef, sm) &&		  //uncommented: this may exclude some relevant variables*/
			    !field.getType().toString().contains("java.io")
				&& !(sig+"."+field.getName()).equals("bouvlesort.NewThread.priority") //for bouvlesort.Loader (JPF crashes if this variable is marked as symbolic)
				&& !field.getName().contains("assertionsDisabled")));
	}
	
}
