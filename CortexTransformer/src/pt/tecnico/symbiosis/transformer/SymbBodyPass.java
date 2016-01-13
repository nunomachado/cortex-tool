package pt.tecnico.symbiosis.transformer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.omg.PortableInterceptor.SUCCESSFUL;

import pt.tecnico.symbiosis.context.LHSContextImpl;
import pt.tecnico.symbiosis.context.RHSContextImpl;
import pt.tecnico.symbiosis.context.RefContext;
import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ConcreteRef;
import soot.jimple.Constant;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NumericConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.util.Chain;

public class SymbBodyPass extends BodyTransformer{

	public static boolean bbAfterAssert; //indicates if we are in the successor of the assert_fail block
	public static boolean flagAssert; //flag that indicates if we've passed through a assert_fail block and, therefore, should instrument the subsequent one (which corresponds to the non-failing branch of the assertion)
	public static SymbBodyPass instance = new SymbBodyPass();
	public SymbBodyPass() {}

	public static SymbBodyPass v() { return instance; }


	protected void internalTransform(Body body, String phase, Map options) 
	{								
		Chain units = body.getUnits();
		bbAfterAssert = false;

		SootMethod m = body.getMethod();
		
		//check if we should instrument this method
		if(!shouldInstruThisMethod(m.getName()))
			return;
		
		//instrument basic blocks
		addBBTrace(body);

		//instrument support for thread consistent identification
		if(m.toString().contains("void main(java.lang.String[])"))
		{
			addCallMainMethodEnterInsert(m, units);
		}
		else if(m.toString().contains("void run()")) //&& m.getDeclaringClass().implementsInterface("java.lang.Runnable"))
		{
			addCallThreadStartRun(m,units);
		}

		Iterator stmtIt = units.snapshotIterator();  
		IfStmt lastGotoStmt = null; //stores the goto stmt of the assertion condition
		while (stmtIt.hasNext()) 
		{
			Stmt s = (Stmt) stmtIt.next();
			//System.out.println("STMT: "+s+"  type: ");
			if(s instanceof InvokeStmt)
			{
				InvokeExpr invokeExpr = ((InvokeStmt) s).getInvokeExpr();
				String sig = invokeExpr.getMethod().getSubSignature();
				String sig2 = invokeExpr.getMethod().getSignature();
				String cname = ((InvokeStmt) s).getInvokeExpr().getMethod().getDeclaringClass().getName();

				if (sig.equals("void start()") && cname.equals("java.lang.Thread")) 
				{
					addCallThreadStartRunBefore(units, s, ((InstanceInvokeExpr)invokeExpr).getBase());
				}
				else if(sig2.contains("java.lang.AssertionError: void <init>()"))
				{
					addCallAssertHandler(units, s, false);
					bbAfterAssert = true;

					//System.out.println("Assertion GOTO STMT: "+lastGotoStmt);
					System.out.println("[SymbiosisTransformer] Found assertion! Inject calls to assertProbe() and assertHandler()");
					addCallAssertProbe(units, lastGotoStmt);
					addCallAssertHandler(units, (Stmt)lastGotoStmt.getTarget(), true);
				}
			}
			else if(s instanceof IfStmt && !bbAfterAssert) //using GotoStmt does not work because assertions are implemented using "if (cond) goto label"
			{
				lastGotoStmt = (IfStmt) s;
				//System.out.println("LAST GOTO STMT: "+lastGotoStmt);
			}

			//collect accesses to shared variables
			if(SymbiosisTransformer.JPF_MODE)
				checkForSharedAccesses(m,s);
		}
	}

	/**
	 * Instruments each basic block to record its id.
	 * @param body
	 */
	public static void addBBTrace(Body body)
	{
		String bbEntrySig;

		SootMethod m = body.getMethod();
		
		if(SymbiosisTransformer.JPF_MODE)
			bbEntrySig ="<" + SymbiosisTransformer.jpfClass +": void symbiosisBBEntry(long)>"; 
		else
			bbEntrySig ="<" + SymbiosisTransformer.runtimeClass +": void symbiosisBBEntry(long)>"; 

		BriefBlockGraph bbg = new BriefBlockGraph(body);
		for(Block b : bbg.getBlocks())
		{
			SootMethodRef bbEntryRef = Scene.v().getMethod(bbEntrySig).makeRef();	
			InvokeStmt bbEntryStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(bbEntryRef, LongConstant.v(SymbiosisTransformer.bbIdCounter))); //static call (implies implemented methods to be static)

			//** find first non-identity stmt
			Stmt insertStmt = (Stmt) b.getHead();

			//don't instrument catch-exception blocks (it's not possible to guide symbolic execution towards these blocks)
			//however, add a monitor call when the catch block has a goto stmt
			//(this because we might jump to another block due to the exception, and we don't want to trace it at runtime)
			if(insertStmt.toString().contains("@caughtexception"))
			{
				if(!SymbiosisTransformer.JPF_MODE)
				{
					System.out.println("[SymbiosisTransformer] skip catch exception BB: "+insertStmt);

					Iterator bit = b.iterator();
					while(bit.hasNext())
					{
						Stmt s = (Stmt) bit.next();
						if(s instanceof GotoStmt)
						{
							System.out.println("[SymbiosisTransformer] has goto stmt -> instrument call to monitor");
							SootMethodRef excRef = Scene.v().getMethod("<" + SymbiosisTransformer.runtimeClass +": void symbiosisCaughtException()>").makeRef();	
							InvokeStmt excStmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(excRef));
							b.insertBefore(excStmt,s);

							break;
						}
					}


				}
				continue; 
			}

			Stmt predStmt = null; //** stmt pointer to the last identity stmt
			while (insertStmt instanceof IdentityStmt)
			{
				predStmt = insertStmt;
				insertStmt = (Stmt) b.getSuccOf(insertStmt);
			}

			if (insertStmt != null)
			{
				b.insertBefore(bbEntryStmt,insertStmt);
			}
			else
			{
				insertStmt = predStmt;
				b.insertAfter(bbEntryStmt,insertStmt); 
			}
			SymbiosisTransformer.bbIdCounter++; //update bbIdCounter	
		} //end for basic blocks

	}



	/**
	 * Injects a monitor call before threads' start method
	 * @param units
	 * @param s
	 * @param v
	 */
	public static void addCallThreadStartRunBefore(Chain units, Stmt s, Value v)
	{
		LinkedList args = new LinkedList();
		args.addLast(v);
		SootMethodRef mr;
		if(SymbiosisTransformer.JPF_MODE)
			mr = Scene.v().getMethod("<" + SymbiosisTransformer.jpfClass + ": void threadStartRunBefore(java.lang.Thread)>").makeRef();
		else
			mr = Scene.v().getMethod("<" + SymbiosisTransformer.runtimeClass + ": void threadStartRunBefore(java.lang.Thread)>").makeRef();
		units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(mr, args)), s);
	}

	/**
	 * Injects a monitor call inside thread's run method
	 * @param sm
	 * @param units
	 */
	public static void addCallThreadStartRun(SootMethod sm, Chain units) {

		LinkedList args = new LinkedList();
		SootMethodRef mr;
		if(SymbiosisTransformer.JPF_MODE)
			mr = Scene.v().getMethod("<" + SymbiosisTransformer.jpfClass + ": void " + "threadStartRun" + "()>").makeRef();
		else
			mr = Scene.v().getMethod("<" + SymbiosisTransformer.runtimeClass + ": void " + "threadStartRun" + "()>").makeRef();
		Value staticInvoke = Jimple.v().newStaticInvokeExpr(mr, args);
		units.insertAfter(Jimple.v().newInvokeStmt(staticInvoke),units.getPredOf(getFirstNonIdentityStmt(units)));

	}

	/**
	 * Injects a monitor call after main method
	 * @param sm
	 * @param units
	 */
	public static void addCallMainMethodEnterInsert(SootMethod sm, Chain units) {

		LinkedList args = new LinkedList();
		String methodSig;
		if(SymbiosisTransformer.JPF_MODE)
			methodSig ="<" + SymbiosisTransformer.jpfClass +": void mainThreadStartRun()>";
		else
			methodSig ="<" + SymbiosisTransformer.runtimeClass +": void mainThreadStartRun()>";

		SootMethodRef mr = Scene.v().getMethod(methodSig).makeRef();
		Value staticInvoke = Jimple.v().newStaticInvokeExpr(mr, args);    
		units.insertAfter(Jimple.v().newInvokeStmt(staticInvoke), units.getPredOf(getFirstNonIdentityStmt(units)));
	}


	/**
	 * Injects a monitor call before assertion failures
	 * @param units
	 * @param s
	 * @param v
	 */
	public static void addCallAssertHandler(Chain units, Stmt s, boolean success)
	{
		LinkedList args = new LinkedList();
		if(success)
			args.add(IntConstant.v(1));
		else
			args.add(IntConstant.v(0));
		String methodSig;
		
		if(SymbiosisTransformer.JPF_MODE)
			methodSig ="<" + SymbiosisTransformer.jpfClass +": void assertHandler(int)>";
		else
			methodSig ="<" + SymbiosisTransformer.runtimeClass +": void assertHandler(int)>";

		SootMethodRef mr = Scene.v().getMethod(methodSig).makeRef();
		Value staticInvoke = Jimple.v().newStaticInvokeExpr(mr, args);    

		//for assertFail, insert before the assertion error invocation; 
		//for assertOk, insert after the symbiosisBBEntry call 
		if(success)
			units.insertAfter(Jimple.v().newInvokeStmt(staticInvoke), s);
		else
			units.insertBefore(Jimple.v().newInvokeStmt(staticInvoke), s);
	}

	/**
	 * Injects a monitor call for each assertion check in the code. 
	 * The probe allows to filter out threads that call the assertHandler but did not actually executed the assertion.
	 * @param units
	 * @param s
	 * @param v
	 */
	public static void addCallAssertProbe(Chain units, Stmt s)
	{
		LinkedList args = new LinkedList();
		String methodSig;
		
		if(SymbiosisTransformer.JPF_MODE)
			methodSig ="<" + SymbiosisTransformer.jpfClass+": void assertProbe()>";
		else
			methodSig ="<" + SymbiosisTransformer.runtimeClass +": void assertProbe()>";
		
		SootMethodRef mr = Scene.v().getMethod(methodSig).makeRef();
		Value staticInvoke = Jimple.v().newStaticInvokeExpr(mr, args);    

		units.insertBefore(Jimple.v().newInvokeStmt(staticInvoke), s);
		
		//check if we should add another call in the previous block 
		//(necessary for cases of type: if(cond){assert(true)}else{assert(false)}
		Stmt temp = (Stmt) units.getPredOf(s);
		while(!(temp instanceof IfStmt) && temp!=null)
		{
			temp = (Stmt) units.getPredOf(temp);
		}
		if(temp!=null){
			System.out.println("Insert assertProbe also in stmt: "+temp);
			units.insertBefore(Jimple.v().newInvokeStmt(staticInvoke), temp);
		}
	}

	public static void checkForSharedAccesses(SootMethod sm, Stmt s)
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

					if(SymbiosisTransformer.sharedVars.contains(sig) && !sig.contains("java.lang.System"))
					{
						logSharedAccess(sm,s,field);
					}
				}
				else if(((ConcreteRef) left) instanceof StaticFieldRef)
				{
					StaticFieldRef fieldRef = ((StaticFieldRef) left);
					SootField field  = fieldRef.getField();
					String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".STATIC";

					if(SymbiosisTransformer.sharedVars.contains(sig) && !sig.contains("java.lang.System"))
					{
						logSharedAccess(sm,s,field);
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

					if(SymbiosisTransformer.sharedVars.contains(sig) && !sig.contains("java.lang.System"))
					{
						logSharedAccess(sm,s,field);
					}
				}
				else if(((ConcreteRef) right) instanceof StaticFieldRef)
				{
					StaticFieldRef fieldRef = ((StaticFieldRef) right);
					SootField field  = fieldRef.getField();
					String sig = field.getDeclaringClass().getName()+"."+fieldRef.getField().getName()+".STATIC";

					if(SymbiosisTransformer.sharedVars.contains(sig) && !sig.contains("java.lang.System"))
					{
						logSharedAccess(sm,s,field);
					}
				}
			}
		}
	}



	/**
	 * Logs accesses to variables that are identified as shared by the TLE analysis.
	 */
	private static void logSharedAccess(SootMethod sm, Stmt s, SootField field)
	{
		String sig = field.getDeclaringClass().toString();

		if(sig.contains("$"))
			sig = sig.substring(0,sig.indexOf("$"));
		String tag;

		if(s.getTag("LineNumberTag")!=null)
			tag = s.getTag("LineNumberTag").toString();
		else
			tag = "0";
		String line =  sig + "."
				+ field.getName() + "@"
				+ tag
				;//+ " instance";
		System.out.println("[SymbiosisTransformer]  shared access: "+line);
		SymbiosisTransformer.sharedAccLog += (line+"\n");
	}


	/**
	 * Returns the first non-identity statement in a chain of units.
	 * @param units
	 * @return
	 */
	private static Stmt getFirstNonIdentityStmt(Chain units)
	{
		Stmt s = (Stmt)units.getFirst();
		while(s instanceof IdentityStmt)
			s = (Stmt) units.getSuccOf(s);
		return s;

	}
	
	
	private static boolean shouldInstruThisMethod(String smname)
	{    	   	
		if (smname.contains("<clinit>") || smname.contains("<init>"))
		{
			return false;	
		}

		return true;
	}

}
