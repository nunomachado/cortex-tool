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
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Filter;
import soot.jimple.toolkits.callgraph.TransitiveTargets;
import soot.jimple.toolkits.scalar.EqualUsesAnalysis;
import soot.jimple.toolkits.thread.mhp.RunMethodsPred;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.toolkits.graph.UnitGraph;

public class XStartJoinAnalysis {
	private UnitGraph graph;
	private SootMethod method;
	private CallGraph cg;
	private PAG pag;
	
	Set<Stmt> startStatements;
	Set<Stmt> joinStatements;
	Hierarchy hierarchy;
	Map<Stmt, List<SootMethod>> startToRunMethods;
	Map<Stmt, List<AllocNode>> startToAllocNodes;
	Map<Stmt, Stmt> startToJoin;
	
	public XStartJoinAnalysis(UnitGraph g, SootMethod sm)
	{
		this.graph = g;
		this.method = sm;
		this.pag = XG.v().getPAG();
		this.cg = Scene.v().getCallGraph(); 
		this.startStatements = new HashSet<Stmt>();
		this.joinStatements = new HashSet<Stmt>();
		
		this.hierarchy = Scene.v().getActiveHierarchy();
		
		this.startToRunMethods = new HashMap<Stmt, List<SootMethod>>();
		this.startToAllocNodes = new HashMap<Stmt, List<AllocNode>>();
		this.startToJoin = new HashMap<Stmt, Stmt>();
		
		// Get lists of start and join statements		
		doFlowInsensitiveSingleIterationAnalysis();//right
		run();
	}
	private void run()
	{
		if(!startStatements.isEmpty())
		{

			TransitiveTargets runMethodTargets = new TransitiveTargets( cg, new Filter(new RunMethodsPred()) );
			
			// Build a map from start stmt to possible run methods, 
			// and a map from start stmt to possible allocation nodes,
			// and a map from start stmt to guaranteed join stmt
			Iterator<Stmt> startIt = startStatements.iterator();
			while (startIt.hasNext())
			{
				Stmt start = startIt.next();
				
				List<SootMethod> runMethodsList = new ArrayList<SootMethod>(); // will be a list of possible run methods called by this start stmt
				List<AllocNode> allocNodesList = new ArrayList<AllocNode>(); // will be a list of possible allocation nodes for the thread object that's getting started
				
				// Get possible thread objects (may alias)
				Value startObject = ((InstanceInvokeExpr) (start).getInvokeExpr()).getBase();
				PointsToSetInternal pts = (PointsToSetInternal) pag.reachingObjects((Local) startObject);//that's good!!
				List<AllocNode> mayAlias = getMayAliasList(pts);
				if( mayAlias.size() < 1 )
					continue; // If the may alias is empty, this must be dead code
					
				// For each possible thread object, get run method
				Iterator<MethodOrMethodContext> mayRunIt = runMethodTargets.iterator( start ); // fails for some call graphs
				while( mayRunIt.hasNext() )
				{
					SootMethod runMethod = (SootMethod) mayRunIt.next();
					if( runMethod.getSubSignature().equals("void run()") )
					{
						runMethodsList.add(runMethod);
					}
				}
				
				// If haven't found any run methods, then use the type of the startObject,
				// and add run from it and all subclasses
				if(runMethodsList.isEmpty() && ((RefType) startObject.getType()).getSootClass().isApplicationClass())
				{
					List<SootClass> threadClasses = hierarchy.getSubclassesOfIncluding( ((RefType) startObject.getType()).getSootClass() );
					Iterator<SootClass> threadClassesIt = threadClasses.iterator();
					while(threadClassesIt.hasNext())
					{
						SootClass currentClass = threadClassesIt.next();
						if( currentClass.declaresMethod("void run()") )							
						{
							runMethodsList.add(currentClass.getMethod("void run()"));
						}
					}
				}

				// For each possible thread object, get alloc node
				Iterator<AllocNode> mayAliasIt = mayAlias.iterator();
				while( mayAliasIt.hasNext() )
				{
					AllocNode allocNode = mayAliasIt.next();
					allocNodesList.add(allocNode);
					if(runMethodsList.isEmpty())
					{
						throw new RuntimeException("Can't find run method for: " + startObject);						
					}			
				}
				
				// Add this start stmt to both maps
				startToRunMethods.put(start, runMethodsList);
				startToAllocNodes.put(start, allocNodesList);
/*				
				// does this start stmt match any join stmt???
				
				// Get supporting info and analyses
				MHGPostDominatorsFinder pd = new MHGPostDominatorsFinder(new BriefUnitGraph(method.getActiveBody()));
				EqualUsesAnalysis lif = new EqualUsesAnalysis(graph);
				
				Iterator<Stmt> joinIt = joinStatements.iterator();
				while (joinIt.hasNext())
				{
					Stmt join = joinIt.next();
					Value joinObject = ((InstanceInvokeExpr) (join).getInvokeExpr()).getBase();
					
					// If startObject and joinObject MUST be the same, and if join post-dominates start
					if( lif.areEqualUses( start, (Local) startObject, join, (Local) joinObject ) )
					{
						if((pd.getDominators(start)).contains(join)) // does join post-dominate start?
						{
//							G.v().out.println("START-JOIN PAIR: " + start + ", " + join);
							startToJoin.put(start, join); // then this join always joins this start's thread
						}
					}
				}*/
			}
		}
	}
	private List<AllocNode> getMayAliasList(PointsToSetInternal pts)
	{
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
	private void doFlowInsensitiveSingleIterationAnalysis()
	{
		Iterator stmtIt = graph.iterator();
		while(stmtIt.hasNext())
		{
			Stmt s = (Stmt) stmtIt.next();
			flowThrough(s);
		}
	}
	protected void flowThrough(Stmt stmt)
	{
		// Search for start/join invoke expressions
		if(stmt.containsInvokeExpr())
		{
			// If this is a start stmt, add it to startStatements
			InvokeExpr ie = stmt.getInvokeExpr();
			if(ie instanceof InstanceInvokeExpr)
			{
				InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
				SootMethod invokeMethod = ie.getMethod();
				if(invokeMethod.getName().equals("start"))
				{
					RefType baseType = (RefType) iie.getBase().getType();
					if(!baseType.getSootClass().isInterface()) // the start method we're looking for is NOT an interface method
					{
						List<SootClass> superClasses = hierarchy.getSuperclassesOfIncluding(baseType.getSootClass());
						Iterator<SootClass> it = superClasses.iterator();
						while (it.hasNext())
						{
							if( it.next().getName().equals("java.lang.Thread") )
							{
								// This is a Thread.start()
								if(!startStatements.contains(stmt))
									startStatements.add(stmt);
							}
						}
					}
				}
				
				// If this is a join stmt, add it to joinStatements
				/*if(invokeMethod.getName().equals("join")) // the join method we're looking for is NOT an interface method
				{
					RefType baseType = (RefType) iie.getBase().getType();
					if(!baseType.getSootClass().isInterface())
					{
						List<SootClass> superClasses = hierarchy.getSuperclassesOfIncluding(baseType.getSootClass());
						Iterator<SootClass> it = superClasses.iterator();
						while (it.hasNext())
						{
							if( it.next().getName().equals("java.lang.Thread") )
							{
								// This is a Thread.join()
								if(!joinStatements.contains(stmt))
									joinStatements.add(stmt);
							}
						}
					}
				}*/
			}
		}
	}
	public Set<Stmt> getStartStatements()
	{
		return startStatements;
	}
	
	public Set<Stmt> getJoinStatements()
	{
		return joinStatements;
	}

	public Map<Stmt, List<SootMethod>> getStartToRunMethods()
	{
		return startToRunMethods;
	}

	public Map<Stmt, List<AllocNode>> getStartToAllocNodes()
	{
		return startToAllocNodes;
	}
	
	public Map<Stmt, Stmt> getStartToJoin()
	{
		return startToJoin;
	}
}
