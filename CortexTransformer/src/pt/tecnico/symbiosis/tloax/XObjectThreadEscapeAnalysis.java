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
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.util.ArrayNumberer;
import soot.util.Chain;

public class XObjectThreadEscapeAnalysis {
//	private Local thisRef;
	private Set<SootMethod> analyzedMethods;
	private XMHPAnalysis mhp;
	private PAG pag;
	private List<XAbstractRuntimeThread> threads;
	private XFieldThreadEscapeAnalysis xfta;
	private Map<AllocNode, Boolean> objectSharedOrNot;
	private ArrayList<AllocNode> allocNodeList;
	private Set<AllocNode> sharedAllocNodeSet;
	public XObjectThreadEscapeAnalysis(XFieldThreadEscapeAnalysis fta)
	{
		this.mhp = XG.v().getMHPAnalysis();
		threads = mhp.getThreads();
		xfta = fta;
		objectSharedOrNot = new HashMap<AllocNode,Boolean>();
		allocNodeList = new ArrayList<AllocNode>();
		sharedAllocNodeSet = new HashSet<AllocNode>();
		analyzedMethods = new HashSet<SootMethod>();
		pag = XG.v().getPAG();
		doAnalysis();
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
		
		System.out.println("----------------------------------------------------- ");//OKAY, DONE!
		
		ArrayNumberer arrnum = pag.getAllocNodeNumberer();
		Iterator arrIt = arrnum.iterator();//AllocNode new sth
		while(arrIt.hasNext())
		{
			AllocNode nd = (AllocNode) arrIt.next();
			SootMethod method = nd.getMethod();
			if(method!=null)
			{
				SootClass sootClass = method.getDeclaringClass();			
				if(sootClass.isApplicationClass())
				{
					if(sharedAllocNodeSet.contains(nd))
					{
						objectSharedOrNot.put(nd, true);
						//System.err.println("--- "+nd+" --- "+"TRUE");//OKAY, DONE!
					}
					else
					{
						objectSharedOrNot.put(nd, false);
						//System.err.println("--- "+nd+" --- "+"FALSE");//OKAY, DONE!
					}
					//System.out.println(nd);//OKAY, DONE!
				}
			}
		}
		
		Iterator<AllocNode> osonIt = objectSharedOrNot.keySet().iterator();
		while(osonIt.hasNext())
		{
			AllocNode node = osonIt.next();
			System.err.println("--- "+node+" --- "+objectSharedOrNot.get(node));//OKAY, DONE!
		}
		
	}
	
//	Value startObject = ((InstanceInvokeExpr) (start).getInvokeExpr()).getBase();
//	PointsToSetInternal pts = (PointsToSetInternal) pag.reachingObjects((Local) startObject);//that's good!!
	
	private List<AllocNode> getMayAliasList(Value base)
	{
		PointsToSetInternal pts = (PointsToSetInternal) pag.reachingObjects((Local) base);//that's good!!//DAMN IT, THIS STUPID PAG!!
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
	private void doAnalysis(XAbstractRuntimeThread thread, SootMethod runMethod)
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
//		if(method.getName().equals("<init>")||method.getName().equals("<clinit>"))
//		{
//			System.err.println("WOO, WE DON'T PROCESS CONSTRUCTORS: "+method);
//			return;
//		}
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
		    		visitStmtAssign(context,thread,(AssignStmt)s,body);
		    	}
		    	else if (s instanceof InvokeStmt) 
		        {
		        	//System.err.println("InvokeStmt: "+s);
		        	visitInvokeExpr(context,thread,s,s.getInvokeExpr(),body);    
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
           visitConcreteRef(context, thread,assignStmt, (ConcreteRef) left,body,true);
        } else if (left instanceof Local) {
           visitRHS(context, thread,assignStmt, right,body);
        } else {
        	 //throw new RuntimeException("UnknownASTNodeException");
        }
    }
    private void visitConcreteRef(ArrayList context, XAbstractRuntimeThread thread, Stmt s,ConcreteRef concreteRef, Body body, boolean writeAccess) {
        if (concreteRef instanceof InstanceFieldRef)
            visitInstanceFieldRef(context, thread, s,(InstanceFieldRef) concreteRef, body, writeAccess);
        else if (concreteRef instanceof StaticFieldRef)
            visitStaticFieldRef(context, thread,s,(StaticFieldRef) concreteRef,writeAccess);
//        else
//        	 throw new RuntimeException("UnknownASTNodeException");
    }
    private void visitStaticFieldRef(ArrayList context,
			XAbstractRuntimeThread thread, Stmt s, StaticFieldRef staticFieldRef, boolean writeAccess) 
    {
    	if(!writeAccess)
    	{

	    	SootField sf = staticFieldRef.getField();
	    	if(xfta.isFieldThreadShared(sf))
	    	{
	    		AssignStmt ass = (AssignStmt)s;
	    		Local local = (Local)ass.getLeftOp();
	    		List<AllocNode> nodes = getMayAliasList(local);
	    		sharedAllocNodeSet.addAll(nodes);
	    	}
	    	else
	    	{
	    		
	    	}
    	}
	}
	private void visitInstanceFieldRef(ArrayList context,
			XAbstractRuntimeThread thread, Stmt s,InstanceFieldRef instanceFieldRef, Body body, boolean writeAccess) 
	{
    	if(!writeAccess)
    	{

	    	SootField sf = instanceFieldRef.getField();
	    	if(xfta.isFieldThreadShared(sf))
	    	{
	    		AssignStmt ass = (AssignStmt)s;
	    		Local local = (Local)ass.getLeftOp();
	    		List<AllocNode> nodes = getMayAliasList(local);
	    		sharedAllocNodeSet.addAll(nodes);
	    	}
	    	else
	    	{
	    		
	    	}
    	}
	}
	private void visitRHS(ArrayList context, XAbstractRuntimeThread thread, Stmt s, Value right, Body body) {
        if (right instanceof ConcreteRef)
            visitConcreteRef(context, thread, s,(ConcreteRef) right,body,false);
        else if (right instanceof InvokeExpr)
        	visitInvokeExpr(context, thread,s, (InvokeExpr) right,body);
		else if (right instanceof NewExpr) {
			visitNewExpr(context,thread,(NewExpr)right,body);
		}
    }
    private void visitNewExpr(ArrayList context, XAbstractRuntimeThread thread,
			NewExpr newExpr, Body body) {
    	Type type = newExpr.getType();
    	SootMethod method = body.getMethod();
		AllocNode node = pag.makeAllocNode(newExpr, type,method);
    	allocNodeList.add(node);
	}
	private void visitInvokeExpr(ArrayList context, XAbstractRuntimeThread thread, Stmt s, InvokeExpr invokeExpr,Body body) {
        if (invokeExpr instanceof InstanceInvokeExpr) {
           visitInstanceInvokeExpr(context, thread, s,(InstanceInvokeExpr) invokeExpr, body);
        } else if (invokeExpr instanceof StaticInvokeExpr) {
           visitStaticInvokeExpr(context, thread, (StaticInvokeExpr) invokeExpr);
//        } else {
//        	 throw new RuntimeException("UnknownASTNodeException");
        }
    }
    private void visitInstanceInvokeExpr(ArrayList context, XAbstractRuntimeThread thread, Stmt s,InstanceInvokeExpr instanceInvokeExpr,Body body)
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
					//let's handle shared object call first
					if(sharedAllocNodeSet.contains(node))
					{
						int len = instanceInvokeExpr.getArgCount();
						for(int i=0;i<len;i++)
						{
							Value local = instanceInvokeExpr.getArg(i);							
							if(local.getType() instanceof RefType && local instanceof Local)
							{
					    		List<AllocNode> nds = getMayAliasList(local);
					    		sharedAllocNodeSet.addAll(nds);
							}
						}
					}
					
					//handle method invocation
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
	public boolean isObjectThreadShared(SootMethod method, NewExpr newExpr)
	{
		if(threads.size() <= 1)//it is a single-threaded program
			return false;
		Type type = newExpr.getType();
		AllocNode node = pag.makeAllocNode(newExpr, type, method);
		if(objectSharedOrNot.get(node)==null)
		{
			System.err.println("WOO, WE DON'T HAVE INFORMATION OF THIS NEW OBJECT: "+newExpr);
			return false;
		}
		else
		{
			return objectSharedOrNot.get(node);
		}
	}
}
