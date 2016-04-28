package pt.tecnico.symbiosis.runtime;

public abstract class SymbiosisRuntime {

	/**
	 * Abstract class that contains the signatures for all 
	 * tracing methods of the Symbiosis Runtime component
	 * @author nunomachado
	 *
	 */
	public static void symbiosisBBEntry(long bbid){}
	public static void mainThreadStartRun(){}
	public static void threadStartRunBefore(Thread t){}
	public static void threadStartRun()	{}
	public static void symbiosisCaughtException() {}
	public static void assertHandler(int success) {}
	public static void assertProbe() {}
}
