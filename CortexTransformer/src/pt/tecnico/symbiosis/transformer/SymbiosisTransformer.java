package pt.tecnico.symbiosis.transformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import pt.tecnico.symbiosis.tloax.XFieldThreadEscapeAnalysis;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;
import soot.jimple.toolkits.thread.mhp.pegcallgraph.PegCallGraph;
import soot.options.Options;



public class SymbiosisTransformer {
	public static final String runtimeClass = "pt.tecnico.symbiosis.runtime.SymbiosisRuntime"; //name of the class that implements the tracing methods
	public static final String jpfClass = "pt.tecnico.jpf.symbiosis.InstrumentationHandler";
	public static long bbIdCounter = 0;
	public static boolean JPF_MODE = false; //indicates that we want to instrument the Java Path Finder version of the program
	public static String sharedAccLogPath; 
	public static String sharedAccLog = ""; 
	public static HashSet<String> sharedVars; //set containing references to shared variables
	
	//thread local escape analysis
	public static XFieldThreadEscapeAnalysis ftea;
	public static ThreadLocalObjectsAnalysis tlo;
	public static PegCallGraph pecg;
	
	public static void main(String[] args) {

		String mainclass = args[0];
		System.out.println(">> Main Class: "+mainclass);
		
		transformRuntimeVersion(mainclass);
		transformJPFVersion(mainclass);
	}

	/**
	 * Instruments the runtime version of the program.
	 * @param mainclass
	 */
	public static void transformRuntimeVersion(String mainclass)
	{
		PackManager.v().getPack("jtp").add(new Transform("jtp.intrumenter", SymbBodyPass.v()));
		setOptions(mainclass, false);
		
		Scene.v().setSootClassPath(System.getProperty("sun.boot.class.path")
				+ File.pathSeparator + System.getProperty("java.class.path"));

		Scene.v().loadClassAndSupport(runtimeClass);

		try{

			//** instrument runtime version
			String outpath = getOutputDir();
			String[] args1 = getArgs(mainclass,outpath);
		
			soot.Main.main(args1);
			System.err.println("***** Runtime version generated *****\n");

			//reset soot parameters
			soot.G.reset();
			bbIdCounter = 0;

		}catch (Exception e)
		{
			System.err.println(">> Exception: " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Instruments the Java Path Finder version of the program.
	 * @param mainclass
	 */
	public static void transformJPFVersion(String mainclass)
	{	
		sharedVars = new HashSet<String>();
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.transformer", new SymbScenePass()));
		PackManager.v().getPack("jtp").add(new Transform("jtp.transformer", new SymbBodyPass()));
		setOptions(mainclass, true);
		Scene.v().loadClassAndSupport(jpfClass);
		
		try{

			//** instrument JPF version
			JPF_MODE = true;
			String outpath = getOutputDir();
			sharedAccLogPath = outpath+mainclass+".accesses";
			String[] args1 = getArgs(mainclass,outpath);
			
			soot.Main.main(args1);
			System.err.println("***** JPF version generated *****");
			saveSharedAccessesLog();
			
		}catch (Exception e)
		{
			System.err.println(">> Exception: " + e.getMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Returns the path for the output directory according to the instrumentation mode
	 * @return
	 */
	public static String getOutputDir() 
	{
		String tempdir = System.getProperty("user.dir");
		if (!(tempdir.endsWith("/") || tempdir.endsWith("\\"))) {
			tempdir = tempdir + System.getProperty("file.separator");
		}

		if(JPF_MODE)
			tempdir = tempdir+"SymbiosisJPF"+System.getProperty("file.separator");
		else
			tempdir = tempdir+"SymbiosisRuntime"+System.getProperty("file.separator");

		File tempFile = new File(tempdir);
		if(!(tempFile.exists()))
			tempFile.mkdir();

		return tempdir;
	}

	/**
	 * @param line
	 * @param file
	 */
	public static void saveSharedAccessesLog() {
		try {
			FileWriter fw = new FileWriter(sharedAccLogPath, false);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(sharedAccLog);
			bw.close();
			System.out.println("[SymbiosisTransformer] Log saved to: "+sharedAccLogPath);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static String[] getArgs(String mainclass, String outpath)
	{
		String[] args1 = {"-cp",".","-pp", mainclass,"-keep-line-number","-d",outpath,
				//"-f","jimple",
				
				//** includes are not working here...add them in setOptions()
				"-i","org.apache.catalina", 			//bugTomcat37458
				"-i","org.apache.naming",				//bugTomcat37458
				"-i","org.apache.commons.logging", 		//bugTomcat37458
				"-i","org.apache.naming.resources", 	//bugTomcat37458*/
				
				"-i","org.apache.derby",													//derby2861 <- most important
				"-i","org.apache.derby.impl.jdbc.EmbedStatement",							//derby2861
				"-i","org.apache.derby.impl.jdbc.EmbedConnection",							//derby2861
				"-i","org.apache.derby.impl.services.daemon.SingleThreadDaemonFactory",		//derby2861
				"-i","org.apache.derby.iapi.services.daemon.DaemonService",					//derby2861
				"-i","org.apache.derby.jdbc.EmbeddedDriver",								//derby2861*/
				
				"-i","com.ibm.crawler.ConnectionManager",				//crawler
				"-i","com.ibm.crawler.Worker"							//crawler
		};
		
		return args1;
	}
	
	private static void setOptions(String mainclass, boolean isJPF)
	{
		PhaseOptions.v().setPhaseOption("jb", "enabled:true");
		PhaseOptions.v().setPhaseOption("tag.ln", "on");
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("jb", "use-original-names:true");
		Options.v().set_app(true);
		Options.v().set_whole_program(true);
		
		if(isJPF){	
			//Enable Spark
			HashMap<String,String> opt = new HashMap<String,String>();
			//opt.put("verbose","true");
			opt.put("propagator","worklist");
			opt.put("simple-edges-bidirectional","false");
			opt.put("on-fly-cg","true");
			opt.put("set-impl","double");
			opt.put("double-set-old","hybrid");
			opt.put("double-set-new","hybrid");
			opt.put("pre_jimplify", "true");
			SparkTransformer.v().transform("",opt);
			PhaseOptions.v().setPhaseOption("cg.spark", "enabled:true");

			Scene.v().setSootClassPath(System.getProperty("sun.boot.class.path")
					+ File.pathSeparator + System.getProperty("java.class.path"));
		}
		
		List excludes = new ArrayList();
		excludes.add("org.eclipse.");
		excludes.add("javax.");
		excludes.add("java.");
		excludes.add("pt.tecnico.");
		Options.v().set_exclude(excludes);
		
		List includes = new ArrayList();
		includes.add("org.apache.commons.pool."); 	//pool107
		includes.add("org.apache.log4j."); 			//log4j_3
		includes.add("org.apache.commons.lang."); 	//lang
		Options.v().set_include(includes);
		
		SootClass appclass = Scene.v().loadClassAndSupport(mainclass);

		try{
			Scene.v().setMainClass(appclass);
			Scene.v().getMainClass();
		}
		catch(Exception e)
		{
			System.out.println(">> Exception [No main class]: "+e.getMessage());
		}
	}
}
