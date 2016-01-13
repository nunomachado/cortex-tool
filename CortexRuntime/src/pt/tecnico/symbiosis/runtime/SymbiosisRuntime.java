package pt.tecnico.symbiosis.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;



public class SymbiosisRuntime {
	public static boolean isCrashed = false;

	//** data structures for thread consistent identification
	public volatile static HashMap<String,Integer> threadChildrenCounter;	//** allows to generate deterministic thread identifiers by counting the number of children threads spawned
	public static Map<Thread, String> MapBackupThreadName;					//** used to keep the thread name consistent during the execution (because the name can be reset by the target program after the thread initialization)

	//** data structures for tracing log file events
	public static HashMap<String, LinkedList<Long>> traceBB;
	public static ReentrantLock l; //** to avoid concurrent modifications when writing the trace, for some programs
	public static int assertCounter; //counts the number of times we hit an assertion
	public static HashSet<String> assertProbeList; //contains the ids of the threads that executed the assertion 
	public static String execLabel = "";	//labels the trace file as failing ".fail" or correct ".ok"
	//public static HashMap<String,Boolean> skipBB; //** map: tid -> skipBB : used to skip a given BB entry when we arrive from a goto stmt in a catch exception block (the symbolic execution cannot guide threads towards catch blocks)

	public static void initialize()
	{
		//** initialize thread consistent identification data structures
		threadChildrenCounter = new HashMap<String, Integer>();
		MapBackupThreadName = new HashMap<Thread, String>();
		assertProbeList = new HashSet<String>();
		//skipBB = new HashMap<String, Boolean>();

		assertCounter = 0;
		traceBB = new HashMap<String, LinkedList<Long>>(); 		
		l = new ReentrantLock();
	}

	public static void symbiosisBBEntry(long bbid)
	{	
		if(Thread.currentThread().getName().equals("main"))
			Thread.currentThread().setName("0");

		String tid = Thread.currentThread().getName();
		
		l.lock();
		try{	
			traceBB.get(tid).add(bbid);
			//System.out.println("-- ["+tid+"] BB:"+bbid);
			//System.out.println(traceBB.get(tid));
		}
		catch(NullPointerException e){
			if(!traceBB.containsKey(tid)){
				traceBB.put(tid, new LinkedList<Long>());
				//System.out.println("-- ["+tid+"] NEW BB:"+bbid);
			}
			traceBB.get(tid).add(bbid);
			//System.out.println(traceBB.get(tid));
		}
		finally{l.unlock();}
	}


	public static void symbiosisCaughtException()
	{
		//skipBB.put(Thread.currentThread().getName(), true);
	}

	//** thread handling
	public static void mainThreadStartRun()
	{
		try{
			Thread.currentThread().setName("0");
			String mainthreadname = Thread.currentThread().getName();
			MapBackupThreadName.put(Thread.currentThread(),mainthreadname);	//** save the name for handling future setNames

			//**to generate deterministic thread identifiers
			threadChildrenCounter.put("0", 1);
			if(!traceBB.containsKey("0")){
				l.lock();
				traceBB.put("0", new LinkedList<Long>());
				l.unlock();
			}
			//skipBB.put("0", false);

		}catch(Exception e)
		{
			System.err.println("[SymbiosisRuntime] "+e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Override thread creation for consistent thread identification across runs. 
	 * The new thread identifier will be the thread's parent thread ID associated with the counter value. 
	 * For instance, suppose a thread ti forks its j-th child thread, this child thread will be identified as ti:j .
	 * @param t
	 * @param parentId
	 */
	public synchronized static void threadStartRunBefore(Thread t)
	{	
		try{
			String parentId = Thread.currentThread().getName();  //** as the instrumented code to get the thread name is executed before changing the name, we need to do this
			//System.out.println(parentId+" "+threadChildrenCounter.containsKey(parentId));
			int childCounter = threadChildrenCounter.get(parentId);
			String newThreadName;

			//** the ith thread spawned by the main thread should have thread id = i
			if(!parentId.equals("0"))
				newThreadName= parentId+"."+childCounter;
			else
				newThreadName = String.valueOf(childCounter);

			t.setName(newThreadName);
			childCounter++;
			threadChildrenCounter.put(parentId,childCounter);

		}catch(Exception e)
		{
			System.err.println("[SymbiosisRuntime] "+e.getMessage());
			e.printStackTrace();
		}
	}


	public synchronized static void threadStartRun()
	{
		try{
			String threadId = Thread.currentThread().getName();
			threadChildrenCounter.put(threadId, 1);
			MapBackupThreadName.put(Thread.currentThread(),threadId);
			//skipBB.put(threadId, false);
			if(!traceBB.containsKey(threadId)){
				l.lock();
				traceBB.put(threadId, new LinkedList<Long>());
				l.unlock();
			}

			System.out.println("[SymbiosisRuntime] T"+threadId+" started running");
		}catch(Exception e)
		{
			System.err.println("[SymbiosisRuntime] "+e.getMessage());
			e.printStackTrace();
		}
	}

	public static void assertHandler(int success)
	{
		if(!assertProbeList.contains(Thread.currentThread().getName()))
			return;
			
		String filename;
		if(success == 0)
			execLabel = assertCounter++ + ".fail";//filename = Main.tracefile + assertCounter++ + ".fail";
		else
			execLabel = assertCounter++ + ".ok";//filename = Main.tracefile + assertCounter++ + ".ok";

		saveTrace(Main.tracefile);
	}
	
	public static void assertProbe()
	{
		assertProbeList.add(Thread.currentThread().getName());
	}

	public static void saveTrace(String filename)
	{
		int execHash = 0; //hash to identify this execution path
		try {
			OutputStreamWriter outstream = new OutputStreamWriter(new FileOutputStream(filename));
			String tid = Thread.currentThread().getName();
			
			l.lock(); 
			for(Entry<String,LinkedList<Long>> entry : traceBB.entrySet())
			{
				Iterator<Long> it = entry.getValue().iterator();
				while(it.hasNext())
				{
					Long bbid = (Long)it.next();
					execHash = (String.valueOf(execHash)+bbid.toString()).hashCode();
					if(!it.hasNext() && tid.equals(entry.getKey())){
						outstream.write("["+entry.getKey()+" "+bbid+"]"+"\n"); //mark the basic block corresponding to the assertion with [ ]
					}
					else{
						outstream.write(entry.getKey()+" "+bbid+"\n");
					}
				}
				//Nuno: handle join case in Critical (ugly!) {--
				if(entry.getKey().equals("0") && 
						(Main.mainclass.equals("critical.Critical")
						|| Main.mainclass.equals("jdk_StringBuffer.StringBufferTest"))){
					outstream.write("0 2\n");	
				}
				//--}
			}
			outstream.close();
			System.out.println("[SymbiosisRuntime] Execution hash: "+execHash);
			String newFileName = filename+"_"+execHash+"_"+execLabel;
			
			//rename trace file
			File oldfile = new File(filename);
			File newfile = new File(newFileName);
			if(!oldfile.renameTo(newfile)){
				System.out.println("[SymbiosisRuntime] Failed to rename logfile.");
			}
			l.unlock();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			l.unlock();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			l.unlock();
		}
	}
}
