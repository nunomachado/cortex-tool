package pt.tecnico.symbiosis.runtime;

import java.lang.reflect.Method;

import pt.tecnico.symbiosis.runtime.ArgParserRuntime.Option;

public class Main {
	public static final ArgParserRuntime CONFIG = new ArgParserRuntime();
	public static String mainclass;
	public static String tracefile;
	public static String storeFullTrace;
	
	/**
	 * Parse input arguments. 
	 * @param args
	 */
	public static void parseArgs(String[] args)
	{
		try{
			CONFIG.parse(args);
			CONFIG.validate();
			
			mainclass = CONFIG.getValue(Option.MAIN_CLASS);
			tracefile = CONFIG.getValue(Option.SYMBTRACE);
			storeFullTrace = CONFIG.getValue(Option.FULLTRACE);
			System.out.println(">> Main Class: "+mainclass);
			System.out.println(">> Trace File: "+tracefile);
			if(storeFullTrace != null)
				System.out.println(">> Store full trace [ON]");
		}
		catch(IllegalArgumentException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Record an execution of the program
	 * @param args
	 */
	private static void run()
	{
		try 
		{
			//** parse main input parameters
			String[] args = mainclass.split(" ");
			mainclass = args[0];
			
			MonitorThread monThread = new MonitorThread();
			Runtime.getRuntime().addShutdownHook(monThread);

			SymbiosisRuntime.initialize();
			
			Class<?> c = Class.forName(mainclass);
			Class[] argTypes = new Class[] { String[].class };
			Method main = c.getDeclaredMethod("main", argTypes);

			String[] mainArgs = {};
			if(args.length>1)
			{
				mainArgs = new String[args.length-1];
				for(int k=0;k<args.length-1;k++)
					mainArgs[k] = args[k+1];
			}
			main.invoke(null, (Object)mainArgs);
			
		} catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		parseArgs(args);
		run();
	}
	
}
