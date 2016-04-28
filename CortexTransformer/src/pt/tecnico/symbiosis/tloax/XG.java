package pt.tecnico.symbiosis.tloax;

import soot.PointsToAnalysis;
import soot.Scene;
import soot.jimple.spark.pag.PAG;

public class XG {
	private static XG instance = new XG();
	private PAG pag;
	private XFieldThreadEscapeAnalysis fta;
//	private MyUseFinder uf;
//	private MyInfoFlowAnalysis ifa;
	private XMHPAnalysis mhp;
	private XPegCallGraph pcg;
	private XAllocNodesFinder anf;
	private XStartJoinFinder sjf;
	private XLoopFinder loopfinder;
	public static XG v() { return instance; }
    public PAG getPAG()
    {	
    	if(pag == null)
    	{
    		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
    		//Currently, WE need SPARK Point-To Analysis
    		if (!(pta instanceof PAG))
    		{
    		   throw new RuntimeException("You must use Spark for points-to analysis when computing MHP information!");
    		}
    		pag = (PAG) pta;
    	}
    	return pag;
    }
    public XFieldThreadEscapeAnalysis getFTA()
    {
    	if(fta == null)
    	{
    		fta = new XFieldThreadEscapeAnalysis();
    	}
    	return fta;
    }
    public XAllocNodesFinder getMyAllocNodesFinder()
    {
    	if(anf == null)
    	{
    		anf = new XAllocNodesFinder();
    	}
    	
    	return anf;
    }
    
    public XStartJoinFinder getMyStartJoinFinder()
    {
    	if(sjf == null)
    	{
    		sjf = new XStartJoinFinder();
    	}
    	
    	return sjf;
    }
    public XLoopFinder getMyLoopFinder()
    {
    	if(loopfinder == null)
    	{
    		loopfinder = new XLoopFinder();
    	}
    	return loopfinder;
    }
//    public MyUseFinder getMyUseFinder()
//    {
//    	if(uf == null)
//    	{
//    		uf = new MyUseFinder();
//    	}
//    	return uf; 
//    }
//	public MyInfoFlowAnalysis getMyInfoFlowAnalysis() {
//		if(ifa == null)
//    	{
//    		ifa = new MyInfoFlowAnalysis();
//    	}
//    	return ifa; 
//	}
	public XMHPAnalysis getMHPAnalysis()
	{
		if(mhp==null)
		{
			mhp = new XMHPAnalysis();
		}
		return mhp;
	}
	public XPegCallGraph getPegCallGraph()
	{
		if(pcg==null)
		{
			pcg = new XPegCallGraph();
		}
		return pcg;
	}
}
