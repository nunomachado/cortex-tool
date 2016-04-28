package pt.tecnico.symbiosis.tloax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import soot.*;
import soot.jimple.*;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.util.ArrayNumberer;
import soot.util.Chain;

public class XFieldThreadEscapeAnalysis {
//	private Local thisRef;
	final private AllocNode initAllocNode;
	private Set<SootMethod> analyzedMethods;
	private XMHPAnalysis mhp;
	private PAG pag;
	private List<XAbstractRuntimeThread> threads;
	private Map<ArrayList<AllocNode>,XClassLocalObjectsAnalysis> contextToXClassLocalObjectsAnalysis;
	private Map<SootClass, XClassLocalObjectsAnalysis> classToXClassLocalObjectsAnalysis;
	private Map<SootField, Boolean> fieldSharedOrNot;
	
	public XFieldThreadEscapeAnalysis()
	{
		this.mhp = XG.v().getMHPAnalysis();
		threads = mhp.getThreads();
		classToXClassLocalObjectsAnalysis = new HashMap<SootClass, XClassLocalObjectsAnalysis>();
		contextToXClassLocalObjectsAnalysis =  new HashMap<ArrayList<AllocNode>,XClassLocalObjectsAnalysis>();
		fieldSharedOrNot = new HashMap<SootField,Boolean>();
		analyzedMethods = new HashSet<SootMethod>();
		pag = XG.v().getPAG();
		initAllocNode = pag.makeStringConstantNode("<INIT>");
		
		doAnalysis();
		printAllInfo();
	}
	public void printAllInfo()
	{
		Iterator<SootField> fieldIt = fieldSharedOrNot.keySet().iterator();
		while(fieldIt.hasNext())
		{
			SootField field = fieldIt.next();
			System.err.println(field+" --- "+fieldSharedOrNot.get(field));
		}
	}
	public void printPAGAllocInfo()
	{
		ArrayNumberer arrnum = pag.getAllocNodeNumberer();
		Iterator arrIt = arrnum.iterator();//AllocNode new sth
		while(arrIt.hasNext())
		{
			AllocNode nd = (AllocNode) arrIt.next();
//			Type type = nd.getType();
//			if(type instanceof RefType)
//			{
//				RefType rtype = (RefType)type;
//				if(rtype.getSootClass().isApplicationClass())
//					System.out.println(nd);
//			}
			SootMethod method = nd.getMethod();
			if(method!=null)
			{
				SootClass sootClass = method.getDeclaringClass();
			
				if(sootClass.isApplicationClass())
					System.out.println(nd);//OKAY, DONE!
			}
//			PointsToSetInternal ptSet = nd.getP2Set();
//			if(ptSet.contains(nd))
//			System.out.println(nd);
//			if(!(ptSet instanceof EmptyPointsToSet))
//				System.out.println("New "+nd);
		}
	}
	private void doAnalysis()
	{
		if(threads.size() > 1)
		{
			for(XAbstractRuntimeThread thread : threads)
		    {
		        for(Object meth : thread.getRunMethods())
		        {
		            SootMethod runMethod = (SootMethod) meth;

					if( runMethod.getDeclaringClass().isApplicationClass())
					{
						doAnalysis(thread,runMethod);
					}
				}
			}
		}
		
		/** process instance fields */
		Iterator<XClassLocalObjectsAnalysis> xcloaIt = contextToXClassLocalObjectsAnalysis.values().iterator();
		while(xcloaIt.hasNext())
		{
			XClassLocalObjectsAnalysis xcloa = xcloaIt.next();
			
			Iterator<SootField> fieldIt = xcloa.iterator();
			while(fieldIt.hasNext())
			{
				SootField field = fieldIt.next();
				Boolean shareOrLocal = fieldSharedOrNot.get(field);
				if(shareOrLocal==null)
				{
					boolean isShared = xcloa.isFieldShared(field);
					fieldSharedOrNot.put(field, isShared);
				}
				else if(!shareOrLocal)
				{
					boolean isShared = xcloa.isFieldShared(field);
					fieldSharedOrNot.put(field,isShared);
				}
			}			
		}
		
		/** process static fields*/
		xcloaIt = classToXClassLocalObjectsAnalysis.values().iterator();
		while(xcloaIt.hasNext())
		{
			XClassLocalObjectsAnalysis xcloa = xcloaIt.next();
			
			Iterator<SootField> fieldIt = xcloa.iterator();
			while(fieldIt.hasNext())
			{
				SootField field = fieldIt.next();
				Boolean shareOrLocal = fieldSharedOrNot.get(field);
				if(shareOrLocal==null)
				{
					boolean isShared = xcloa.isFieldShared(field);
					fieldSharedOrNot.put(field, isShared);
				}
				else if(!shareOrLocal)
				{
					boolean isShared = xcloa.isFieldShared(field);
					fieldSharedOrNot.put(field,isShared);
				}
			}			
		}
		
		
	}
	
//	Value startObject = ((InstanceInvokeExpr) (start).getInvokeExpr()).getBase();
//	PointsToSetInternal pts = (PointsToSetInternal) pag.reachingObjects((Local) startObject);//that's good!!
	
	private List<AllocNode> getMayAliasList(Value base)
	{
		PointsToSetInternal pts = (PointsToSetInternal) pag.reachingObjects((Local) base);//that's good!!
		List<AllocNode> list = new ArrayList<AllocNode>();
		final HashSet<AllocNode> ret = new HashSet<AllocNode>();
		pts.forall( new P2SetVisitor() {
			public void visit( Node n ) {
				ret.add( (AllocNode)n );
			}
		} );
		Iterator<AllocNode> it = ret.iterator();
		while (it.hasNext()){
			list.add( it.next() );
		}
		return list;
	}
	private void doAnalysis(XAbstractRuntimeThread thread,SootMethod runMethod)
	{
		analyzedMethods.clear();
		doAnalysis(new ArrayList(), thread, runMethod);
	}
	private void doAnalysis(ArrayList context, XAbstractRuntimeThread thread,SootMethod method)
	{
		if(analyzedMethods.contains(method))
			return;
		else
		{
			analyzedMethods.add(method);
		}
		
		ArrayList<AllocNode> newContext = new ArrayList<AllocNode>();
		newContext.addAll(context);
		
		if(method.getName().equals("<init>")||method.getName().equals("<clinit>"))
		{
			//System.err.println("WOO, WE DON'T PROCESS CONSTRUCTORS: "+method);
			//return;
			newContext.add(initAllocNode);			
		}
		try
		{
			if(!method.isConcrete())
				return;
			
			Body body = method.retrieveActiveBody();
			Chain units = body.getUnits();
		    Iterator iterator = units.snapshotIterator();
		    while(iterator.hasNext())
		    {
		    	Stmt s = (Stmt)iterator.next();
		    	if (s instanceof AssignStmt) 
		    	{
		    		//System.err.println("AssignStmt: "+s);
		    		visitStmtAssign(newContext,thread,(AssignStmt)s,body);
		    	}
		    	else if (s instanceof InvokeStmt) 
		        {
		        	//System.err.println("InvokeStmt: "+s);
		        	visitInvokeExpr(newContext,thread,s.getInvokeExpr(),body);    
		        } else if (s instanceof IdentityStmt) {
		        	//System.err.println("IdentityStmt: "+s);
		        } else if (s instanceof GotoStmt) {
		        	//System.err.println("GotoStmt: "+s);
		        } else if (s instanceof IfStmt) {
		        	//System.err.println("IfStmt: "+s);
		        } else if (s instanceof TableSwitchStmt) {
		        	//System.err.println("TableSwitchStmt: "+s);
		        } else if (s instanceof LookupSwitchStmt) {
		        	//System.err.println("LookupSwitchStmt: "+s);
		        } else if (s instanceof MonitorStmt) {
		        	//System.err.println("MonitorStmt: "+s);
		        } else if (s instanceof ReturnStmt) {
		        	//System.err.println("ReturnStmt: "+s);
		        } else if (s instanceof ReturnVoidStmt) {
		        	//System.err.println("ReturnVoidStmt: "+s);
		        } else if (s instanceof ThrowStmt) {
		        	//System.err.println("ThrowStmt: "+s);
		        } else if (s instanceof BreakpointStmt) {
		        	//System.err.println("BreakpointStmt: "+s);
		        } else if (s instanceof NopStmt) {
		        	//System.err.println("NopStmt: "+s);
		        } else {
		            //throw new RuntimeException("UnknownASTNodeException");
		        }
		    }

		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

    private void visitStmtAssign(ArrayList context, XAbstractRuntimeThread thread, AssignStmt assignStmt, Body body) 
    {
        Value left = assignStmt.getLeftOp();
        Value right = assignStmt.getRightOp();
        if (left instanceof ConcreteRef) {
           visitConcreteRef(context, thread, (ConcreteRef) left,body,true);
        } else if (left instanceof Local) {
           visitRHS(context, thread, right, body);
        } else {
        	 //throw new RuntimeException("UnknownASTNodeException");
        }
    }
    private void visitConcreteRef(ArrayList context, XAbstractRuntimeThread thread,ConcreteRef concreteRef, Body body, boolean writeAccess) {
        if (concreteRef instanceof InstanceFieldRef)
            visitInstanceFieldRef(context, thread, (InstanceFieldRef) concreteRef, body, writeAccess);
        else if (concreteRef instanceof StaticFieldRef)
            visitStaticFieldRef(context, thread,(StaticFieldRef) concreteRef,writeAccess);
//        else
//        	 throw new RuntimeException("UnknownASTNodeException");
    }
    private void handleFieldAccess(XClassLocalObjectsAnalysis xcloa, SootField field, XAbstractRuntimeThread thread, boolean writeAccess)
    {
    	if(writeAccess)
    	{
    		xcloa.fieldWrite(thread,field);
    	}
    	else
    	{
    		xcloa.fieldRead(thread, field);
    	}
    }
    private void visitStaticFieldRef(ArrayList context,
			XAbstractRuntimeThread thread, StaticFieldRef staticFieldRef, boolean writeAccess) 
    {
    	SootField sf = staticFieldRef.getField();
    	SootClass sc = sf.getDeclaringClass();
    	
		if(context.contains(initAllocNode))
		{
			//System.err.println("^^^^^ "+staticFieldRef+" ^^^^^^");//OKAY, DONE!
			return;
		}
		
		
    	XClassLocalObjectsAnalysis xcloa = classToXClassLocalObjectsAnalysis.get(sc);
    	if(xcloa == null)
    	{
    		xcloa = new XClassLocalObjectsAnalysis(sc);
    		classToXClassLocalObjectsAnalysis.put(sc, xcloa);
    	}
    	handleFieldAccess(xcloa,sf,thread,writeAccess);
	}
	private void visitInstanceFieldRef(ArrayList context,
			XAbstractRuntimeThread thread, InstanceFieldRef instanceFieldRef, Body body, boolean writeAccess) 
	{
		Value base = instanceFieldRef.getBase();
		SootField sootField = instanceFieldRef.getField();
		SootClass sc = sootField.getDeclaringClass();
		
		
		if(context.contains(initAllocNode))
		{
			//System.err.println("^^^^^ "+body.getMethod()+" ^^^^^^");//OKAY, DONE!
			return;
		}
		
		if(!body.getMethod().isStatic()&&base.equals(body.getThisLocal()))
		{
			ArrayList arrList = new ArrayList<AllocNode>();
			arrList.addAll(context);
			XClassLocalObjectsAnalysis xcloa = contextToXClassLocalObjectsAnalysis.get(arrList);
			if(xcloa == null)
			{
				xcloa = new XClassLocalObjectsAnalysis(sc);
				contextToXClassLocalObjectsAnalysis.put(arrList, xcloa);
			}
			handleFieldAccess(xcloa,sootField,thread,writeAccess);
		}
		else
		{
			List<AllocNode> list = getMayAliasList(base);
			Iterator it = list.iterator();
			while(it.hasNext())
			{
				AllocNode node = (AllocNode)it.next();
				ArrayList arrList = new ArrayList<AllocNode>();
				arrList.addAll(context);
				arrList.add(node);
				XClassLocalObjectsAnalysis xcloa = contextToXClassLocalObjectsAnalysis.get(arrList);
				if(xcloa == null)
				{
					xcloa = new XClassLocalObjectsAnalysis(sc);
					contextToXClassLocalObjectsAnalysis.put(arrList, xcloa);
				}
				handleFieldAccess(xcloa,sootField,thread,writeAccess);
			}
		}
	}
	private void visitRHS(ArrayList context, XAbstractRuntimeThread thread, Value right, Body body) {
        if (right instanceof ConcreteRef)
            visitConcreteRef(context, thread, (ConcreteRef) right,body,false);
        else if (right instanceof InvokeExpr)
        	visitInvokeExpr(context, thread, (InvokeExpr) right,body);
		else if (right instanceof NewExpr) {
			visitNewExpr(context,thread,(NewExpr)right,body);
		}
    }
    private void visitNewExpr(ArrayList context, XAbstractRuntimeThread thread,
			NewExpr right, Body body) {
		
		
	}
	private void visitInvokeExpr(ArrayList context, XAbstractRuntimeThread thread, InvokeExpr invokeExpr,Body body) {
        if (invokeExpr instanceof InstanceInvokeExpr) {
           visitInstanceInvokeExpr(context, thread, (InstanceInvokeExpr) invokeExpr, body);
        } else if (invokeExpr instanceof StaticInvokeExpr) {
           visitStaticInvokeExpr(context, thread, (StaticInvokeExpr) invokeExpr);
//        } else {
//        	 throw new RuntimeException("UnknownASTNodeException");
        }
    }
    private void visitInstanceInvokeExpr(ArrayList context, XAbstractRuntimeThread thread, InstanceInvokeExpr instanceInvokeExpr,Body body)
    {
    	Value base = instanceInvokeExpr.getBase();
    	if(!body.getMethod().isStatic()&&base.equals(body.getThisLocal()))
		{
    		SootMethod method = instanceInvokeExpr.getMethod();
    		if(method.getDeclaringClass().isApplicationClass())
    		//WOO, VIRTUALINVOKE IS OKAY, WE CAN HANDLE IT!
    			doAnalysis(context, thread, method);//is this method a correct one?? NOT NECESSARILY SO WE NEED TO HANDLE
		}
    	else//otherwise, it should be method invocation on local
    	{
    		List<AllocNode> list = getMayAliasList(base);
			Iterator it = list.iterator();
			while(it.hasNext())
			{
				AllocNode node = (AllocNode)it.next();
				Type type = node.getType();
				if(type instanceof RefType)
				{
					RefType rtype = (RefType)type;
					String methodSubSignature = instanceInvokeExpr.getMethod().getSubSignature();
					try{
						SootMethod method = retrieveMethod(rtype.getSootClass(),methodSubSignature);
						if(method.getDeclaringClass().isApplicationClass())
						{
							ArrayList arrList = new ArrayList<AllocNode>();
							arrList.addAll(context);
							arrList.add(node);
							doAnalysis(arrList, thread, method);
						}
					}catch(Exception e)
					{
						//e.printStackTrace();
						continue;
					}

				}
			}
    	}
    }
    private SootMethod retrieveMethod(SootClass sootClass,
			String methodSubSignature) {
    	
		while(sootClass.hasSuperclass())
		{
	    	try
			{
				SootMethod method = sootClass.getMethod(methodSubSignature);//MAY NOT HAVE THE METHOD??
			
	    		return method;
			}catch(Exception e)
			{
				sootClass = sootClass.getSuperclass();
				continue;
			}
		}
		
		throw new RuntimeException("Method "+methodSubSignature+ " does not exist in class "+sootClass);
	} 
	private void visitStaticInvokeExpr(ArrayList context, XAbstractRuntimeThread thread, StaticInvokeExpr staticInvokeExpr)
    {
		SootMethod method = staticInvokeExpr.getMethod();
		if(method.getDeclaringClass().isApplicationClass())
			doAnalysis(context, thread, method);
    }
	public boolean isFieldThreadShared(SootField sootField)
	{
		if(threads.size() <= 1)//it is a single-threaded program
			return false;

		if(fieldSharedOrNot.get(sootField)==null)
		{
			System.err.println("WOO, WE DON'T HAVE INFORMATION OF THIS FIELD: "+sootField);
			return false;
		}
		else
		{
			return fieldSharedOrNot.get(sootField);
		}
	}
}
