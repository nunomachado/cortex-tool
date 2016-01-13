package pt.tecnico.symbiosis.transformer;

import java.util.Iterator;
import java.util.Map;

import pt.tecnico.symbiosis.tloax.XFieldThreadEscapeAnalysis;
import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;
import soot.jimple.toolkits.thread.mhp.pegcallgraph.PegCallGraph;

public class SymbScenePass extends SceneTransformer{

	public static SymbScenePass instance = new SymbScenePass();
	public SymbScenePass() {}

	public static SymbScenePass v() { return instance; }

	protected void internalTransform(String pn, Map map)
	{
		SymbiosisTransformer.tlo = new ThreadLocalObjectsAnalysis(new SynchObliviousMhpAnalysis());
		SymbiosisTransformer.ftea = new XFieldThreadEscapeAnalysis();
		SymbiosisTransformer.pecg = new PegCallGraph(Scene.v().getCallGraph());

		Iterator<SootClass> classIt = Scene.v().getApplicationClasses().iterator();
		while (classIt.hasNext()) 
		{
			SootClass sc =  classIt.next();

			Iterator<SootMethod> methodIt = sc.getMethods().iterator();

			while (methodIt.hasNext()) 
			{
				SootMethod sm = methodIt.next();
				
				if(sm.isAbstract() || sm.isNative())
					continue;
				
				try
				{
					Body body = sm.retrieveActiveBody();
					SymbFindSVPass.v().internalTransform(body, pn, map);
				}
				catch(Exception e)
				{
					System.err.println("[SymbiosisTransformer] SymbScenePass: "+e.getMessage());
					e.printStackTrace();
					continue;
				}
			}
		}
	}

}
