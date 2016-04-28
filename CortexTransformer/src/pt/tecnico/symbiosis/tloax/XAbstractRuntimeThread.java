package pt.tecnico.symbiosis.tloax;

import java.util.ArrayList;
import java.util.List;

public class XAbstractRuntimeThread {

	private int tid;
	private boolean multipleInstance = false;
	public void setThreadId(int tid)
	{
		this.tid=tid;
	}
	public int getThreadId()
	{
		return tid;
	}
	public void setMultiInstance()
	{
		multipleInstance = true;
	}
	public boolean isMultiInstance()
	{
		return multipleInstance;
	}
	
	// What methods are in the thread
	//List<Object> methods;//it's better to be list, since we need to get 
	List<Object> runMethods; // the run methods that is possible to run by the abstract thread
	
	XAbstractRuntimeThread()
	{
		//methods = new ArrayList<Object>();
		runMethods = new ArrayList<Object>();
	}
	public List<Object> getRunMethods()
	{
		return runMethods;
	}
	
//	public boolean containsMethod(Object method)
//	{
//		return methods.contains(method);
//	}
//	
//	public void addMethod(Object method)
//	{
//		methods.add(method);
//	}
//	
	public void addRunMethod(Object method)
	{
		runMethods.add(method);
	}
//	public int methodCount()
//	{
//		return methods.size();
//	}
//	public Object getMethod(int methodNum)
//	{
//		return methods.get(methodNum);
//	}

}
