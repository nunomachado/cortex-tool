package pt.tecnico.symbiosis.tloax;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.PointsToAnalysis;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.thread.mhp.findobject.MultiCalledMethods;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.util.Chain;

public class XAllocNodesFinder {
	private PAG pag;
	private XPegCallGraph pcg;
	private CallGraph cg;
	private XLoopFinder loopfinder;
	private final Set<AllocNode> allocNodes;
	private final Set<AllocNode>  multiRunAllocNodes;
	private final Set<Object> multiCalledMethods;
	//private final HashMap methodsToMultiObjsSites;
	
	XAllocNodesFinder()
	{
		this.pag = XG.v().getPAG();
		this.pcg = XG.v().getPegCallGraph();
		this.cg = Scene.v().getCallGraph();
		this.loopfinder = XG.v().getMyLoopFinder();
		
		allocNodes = new HashSet<AllocNode>();
		multiRunAllocNodes = new HashSet<AllocNode>();
		multiCalledMethods = new HashSet<Object>();
//		methodsToMultiObjsSites = new HashMap();
		
		find();
		
	}

	private void find() 
	{
		findMultiCalledMethods();
		
		Set clinitMethods = pcg.getClinitMethods();
		Iterator it = pcg.iterator();
		while (it.hasNext())
		{
			SootMethod sm = (SootMethod)it.next();
		    Chain units = sm.getActiveBody().getUnits();
		    Iterator iterator = units.snapshotIterator();
		    while(iterator.hasNext())
		    {
		    	Stmt stmt = (Stmt)iterator.next();
		    	if (stmt instanceof AssignStmt)
		    	{
		    		Value rightOp = ((AssignStmt)stmt).getRightOp();
					if (rightOp instanceof NewExpr)
					{
						
						if (clinitMethods.contains(sm))
						{
							AllocNode allocNode = pag.makeAllocNode(
									PointsToAnalysis.STRING_NODE,
									RefType.v( "java.lang.String" ), null );
							allocNodes.add(allocNode);
						}
						else 
						{
							Type type = ((NewExpr)rightOp).getType();
							AllocNode allocNode = pag.makeAllocNode(rightOp, type, sm);
							allocNodes.add(allocNode);
							
							if(multiCalledMethods.contains(sm)||isInLoop(sm,stmt))
							{
								multiRunAllocNodes.add(allocNode);
							}
							else
							{
								
							}
						}
					}
		    	}
		    }
		}
	}

	private boolean isInLoop(SootMethod sm, Stmt stmt) 
	{
		Collection<List<Stmt>> loops = loopfinder.getLoops(sm);
		Iterator lpIt = loops.iterator(); 
        while (lpIt.hasNext()) 
        {
        	List<Stmt> loop = (List<Stmt>)lpIt.next();
        	if(loop.contains(stmt))
        		return true;
        }
        return false;
		
	}

	private void findMultiCalledMethods() {
		
		//Use breadth first search to find methods are called more than once in call graph
		Set clinitMethods = pcg.getClinitMethods();    
		Iterator it = pcg.iterator();
		while (it.hasNext())
		{
			Object head = it.next();
			//breadth first scan
			Set<Object> gray = new HashSet<Object>();
			LinkedList<Object> queue = new LinkedList<Object>();
			queue.add(head);
			
			while (queue.size()>0)
			{
				Object root = queue.getFirst();
				Iterator succsIt = pcg.getSuccsOf(root).iterator();
				while (succsIt.hasNext())
				{
					Object succ = succsIt.next();
					
					if (!gray.contains(succ)){
						gray.add(succ);
						queue.addLast(succ);
					}
					else if(clinitMethods.contains(succ))  continue;
					else
					{
						multiCalledMethods.add(succ);
					}
				}
				queue.remove(root);
			}
		}
		
		pcg.trim();
		
		Set<Object> first = new HashSet<Object>();
		Set<Object> second = new HashSet<Object>();
		
		// Visit each node
		it = pcg.iterator();
		while (it.hasNext()){
			Object s =it.next();
			
			if (!second.contains(s)){
				
				visitNode(s, first, second);
			}
		}
		
	}

	private void visitNode(Object node, Set<Object> first, Set<Object> second) 
	{
		if (first.contains(node))
		{
			second.add(node);
			if (!multiCalledMethods.contains(node))
			{
				multiCalledMethods.add(node);
			}
		}
		else
		{
			first.add(node);
		}
		
		Iterator it = pcg.getTrimSuccsOf(node).iterator();
		while (it.hasNext()){
			Object succ = it.next();
			if (!second.contains(succ)){
				visitNode(succ, first, second);
			}
		}
		
	}
	
	public Set<AllocNode> getMultiRunAllocNodes()
	{
		return  multiRunAllocNodes;
	}
	
	public Set<Object> getMultiCalledMethods()
	{
		return multiCalledMethods;
	}
}
