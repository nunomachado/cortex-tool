package pt.tecnico.symbiosis.tloax;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.DirectedGraph;
import soot.util.Chain;
import soot.util.HashChain;

public class XPegCallGraph implements DirectedGraph{
	private List heads;
	private List tails;
	private Chain chain;
	private final Map<Object,List> methodToSuccs;
	private final Map<Object,List> methodToPreds;
	private final Map<Object,List> methodToSuccsTrim;
	private final Set clinitMethods;
	private CallGraph cg;
	
	public XPegCallGraph()
	{
		clinitMethods = new HashSet();
		chain = new HashChain();
		heads = new ArrayList();
		tails = new ArrayList();
		methodToSuccs = new HashMap();
		methodToPreds = new HashMap();
		methodToSuccsTrim = new HashMap();
		cg = Scene.v().getCallGraph();
		
		buildChainAndSuccs();

		buildPreds();
	}
	private void buildChainAndSuccs()
	{
		Iterator it = cg.sourceMethods();
		while (it.hasNext())
		{
			SootMethod sm = (SootMethod)it.next();
			if (sm.getName().equals("main")) 
				heads.add(sm);
			
			if (sm.isConcrete() && sm.getDeclaringClass().isApplicationClass() )
			{
				if (!chain.contains(sm)){
					chain.add(sm);
				}
				
				List succsList = new ArrayList();
				Iterator edgeIt = cg.edgesOutOf(sm);
				while(edgeIt.hasNext())
				{
					Edge edge = (Edge)edgeIt.next();
					SootMethod target = edge.tgt();
					if(target.isConcrete() && target.getDeclaringClass().isApplicationClass())
					{
						succsList.add(target);
						if (!chain.contains(target))
						{
							chain.add(target);
						}
						if (edge.isClinit())
						{
							clinitMethods.add(target);
						}
					}
				}
				if (succsList.size()>0 ) methodToSuccs.put(sm, succsList);
			}
		}
		
		/* Because CallGraph.sourceMethods only "Returns an iterator 
		 * over all methods that are the sources of at least one edge",
		 * some application methods may not in methodToSuccs. So add them.
		 */
		Iterator chainIt = chain.iterator();
		while (chainIt.hasNext()){
			SootMethod sm = (SootMethod)chainIt.next();
			
			if (!methodToSuccs.containsKey(sm)){
				methodToSuccs.put(sm, new ArrayList());
			}
		}

		//remove the entry for those who's preds are null.
		//TODO: here
		
		
		
		//Make it unmodifiable
		chainIt = chain.iterator();
		while(chainIt.hasNext()){
			
			SootMethod s =  (SootMethod)chainIt.next();
			//		System.out.println(s);
			if (methodToSuccs.containsKey(s)){
				methodToSuccs.put(s, Collections.unmodifiableList(methodToSuccs.get(s)));
			}
		}
		
	}
	
	private void buildPreds()
	{
		// initialize the pred sets to empty
		Iterator unitIt = chain.iterator();
		
		while(unitIt.hasNext()){
			
			methodToPreds.put(unitIt.next(), new ArrayList());
		}
		
		unitIt = chain.iterator();
		while(unitIt.hasNext())
		{
			Object s =  unitIt.next();
			
			// Modify preds set for each successor for this statement
			List succList = methodToSuccs.get(s);
			if (succList.size()>0)
			{
				Iterator succIt = succList.iterator();
				while(succIt.hasNext())
				{
					Object successor =  succIt.next();
					List<Object> predList = methodToPreds.get(successor);
					try 
					{
						predList.add(s);
					} 
					catch(NullPointerException e) 
					{
						throw e;
					}
				}
			}
		}
		
		// Make pred lists unmodifiable.
		unitIt = chain.iterator();
		while(unitIt.hasNext())
		{
			SootMethod s =  (SootMethod)unitIt.next();
			if (methodToPreds.containsKey(s)){
				List predList = methodToPreds.get(s);
				methodToPreds.put(s, Collections.unmodifiableList(predList));
			}
		}	
	}
	
	//If there are multiple edges from one method to another, we only keeps one edge.  BROKEN
	public void trim()
	{
		
		Set maps = methodToSuccs.entrySet();
		for(Iterator iter=maps.iterator(); iter.hasNext();)
		{
			Map.Entry entry = (Map.Entry)iter.next();
			List list = (List)entry.getValue();
			List<Object> newList = new ArrayList<Object>();
			Iterator it = list.iterator();
			
			while (it.hasNext())
			{
				Object obj = it.next();
				if (!list.contains(obj)) newList.add(obj);
			}
			methodToSuccsTrim.put(entry.getKey(), newList);
		}
	}
	
	public Set getClinitMethods(){
		return clinitMethods;
	}
	public List getHeads(){
		return heads;
	}
	public List getTails(){
		return tails;
	}
	public List getSuccsOf(Object s){
		if (!methodToSuccs.containsKey(s))
			return java.util.Collections.EMPTY_LIST;
		return methodToSuccs.get(s);
	}
	public List getTrimSuccsOf(Object s){
		if (!methodToSuccsTrim.containsKey(s))
			return java.util.Collections.EMPTY_LIST;
		return methodToSuccsTrim.get(s);
	}
	public List getPredsOf(Object s){
		if (!methodToPreds.containsKey(s))	
			return java.util.Collections.EMPTY_LIST;
		return methodToPreds.get(s);
	}
	public Iterator iterator(){
		return chain.iterator();
	}
	public int size(){
		return chain.size();	
	}
}
