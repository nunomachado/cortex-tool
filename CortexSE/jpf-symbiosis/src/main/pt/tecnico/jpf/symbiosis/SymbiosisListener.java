package pt.tecnico.jpf.symbiosis;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.AALOAD;
import gov.nasa.jpf.jvm.bytecode.AASTORE;
import gov.nasa.jpf.jvm.bytecode.ALOAD;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.jvm.bytecode.ArrayLoadInstruction;
import gov.nasa.jpf.jvm.bytecode.ArrayStoreInstruction;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.GETFIELD;
import gov.nasa.jpf.jvm.bytecode.GETSTATIC;
import gov.nasa.jpf.jvm.bytecode.IALOAD;
import gov.nasa.jpf.jvm.bytecode.IASTORE;
import gov.nasa.jpf.jvm.bytecode.INVOKEINTERFACE;
import gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.INVOKEVIRTUAL;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.jvm.bytecode.MONITORENTER;
import gov.nasa.jpf.jvm.bytecode.MONITOREXIT;
import gov.nasa.jpf.jvm.bytecode.PUTFIELD;
import gov.nasa.jpf.jvm.bytecode.PUTSTATIC;
import gov.nasa.jpf.jvm.bytecode.ReturnInstruction;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import com.sun.xml.internal.bind.v2.model.core.MaybeElement;

import pt.tecnico.jpf.symbiosis.util.Type;
import pt.tecnico.jpf.symbiosis.util.Utilities;

public class SymbiosisListener extends PropertyListenerAdapter{

	public static Config config; //configuration parameters
	public static boolean DEBUG = true; 
	public static HashMap<String,Vector<String>> bbtrace; 	//"symbiosis.bbtrace" - path to the log containing each thread's execution path, in terms of basic block ids 
	public static HashSet<String> sharedAccesses;			//"symbiosis.sharedAccesses" - set of strings indicating the shared accesses identified by the static analysis
	public static String symbTraceFolder;					//"symbiosis.tracefolder" - path to the output folder where we will store the symbolic event traces
	public static HashSet<String> threadsFinished;			//set used to mark threads as finished, i.e. threads which have already printed their Path Conditions to the file
	public static HashSet<String> threadsStarted;			//set used to count threads that already started, i.e. threads which have already printed their start event into the file	
	public static HashSet<String> daemonThreads;			//set that stores the deamon threads that are still alive; this is important to prevent all user threads from finishing before the deamon threads have completed their execution paths 
	public boolean hasForked = false;						//bool indicating whether a thread has forked a children or not (used to ensure that all threads are executed right after being forked)
	public static HashMap<String, Integer> canLogBranch; 	//map: thread id -> int flag indicating the choice taken in the previous branch (-1 -> don't trace branch; 0 -> trace choice 0; 1 -> trace choice 1) 
	public static boolean failedExec;						//flag indicating whether the BB trace file is from a successful or failing execution
	
	//data structures for collecting information to generate per-thread symbolic traces
	public HashMap<String,List<String>> pathPerThread; 		//map: "thread id_path id" -> list of path conditions of that thread (JPF stores all threads' path conditions into one single string)
	public static String executionId; 							//indicates the unique id of this execution (used to distinguish symbolic traces from different executions) 
	public static String assertThread;						//indicates what thread contains the assertion condition
	
	//data structures to handle consistent state identification
	public HashMap<String, String> stateTree;				//map: "thread id-state id" -> parent "thread id-state id" (this allows to know the parent state when storing new states)
	public HashMap<String, String> prevState;				//map: thread id -> previous "thread id-state id" (used to build stateTree) 
	public static HashMap<String,Integer> lastNumBBs; 		//map: symb var name -> number of BBs (consumed from the trace) when the symbolic variable was created for the first time 
	public static HashMap<String,Boolean> stateOkToLog;	//map: state id -> boolean indicating whether it is ok to log (i.e. if the state corresponds to a basic block that conforms with the trace)
	public static HashMap<String,String> writtenValues;		//map: write operation -> written value -> used to resolve write operations whose values are references to other writes
	public static HashMap<String, List<StateInfo>> mapStateInfo;	//map: "thread id_state id" -> list if object containing the state info (there two possible states at most, corresponding to the two branches)
	
	//data structures to handle object monitors
	public static HashMap<String, Stack<String>> methodMonitor; //map: thread id -> stack with the last monitor acquired (used to identify the monitor of a given synchronized method, when leaving that method)

	//data structures to guide symbolic execution	
	public Search pointerToSearch=null;
	public SystemState pointerToSS = null;
	public JVM pointerToVM = null;
	public static HashMap<String, Integer> flipBranchMap;	//map: thread id -> branch counter (when the counter is 0, then we stop guiding the symbolic execution for that thread)
	
	//measure elapsed time
	long startTime, endTime;

	public SymbiosisListener(Config conf, JPF jpf)
	{
		startTime = System.nanoTime();

		config = conf; 
		pointerToSearch = jpf.getSearch();
		pointerToSS = jpf.getVM().getSystemState();
		pointerToVM = jpf.getVM();
		//initialize data structures
		bbtrace = new HashMap<String, Vector<String>>();
		sharedAccesses = new HashSet<String>();
		threadsFinished = new HashSet<String>();
		threadsStarted = new HashSet<String>();
		lastNumBBs = new HashMap<String, Integer>();
		methodMonitor = new HashMap<String, Stack<String>>();
		stateOkToLog = new HashMap<String, Boolean>();
		writtenValues = new HashMap<String, String>();
		daemonThreads = new HashSet<String>();
		canLogBranch = new HashMap<String, Integer>();
		//executionId = System.currentTimeMillis();
		flipBranchMap = new HashMap<String, Integer>();
		prevState = new HashMap<String, String>();
		stateTree = new HashMap<String, String>();
		pathPerThread = new HashMap<String, List<String>>();
		mapStateInfo = new HashMap<String, List<StateInfo>>();
		
		//create output folder if it doesn't exist
		symbTraceFolder = config.getString("symbiosis.tracefolder");
		File tempFile = new File(symbTraceFolder);
		if(!(tempFile.exists()))
			tempFile.mkdir();

		//load basic block trace and shared access locations
		loadBBTrace();
		loadSharedAccesses();
		
		//load flip branch file (if exists one)
		loadFlipBranchFile();
	}


	/**
	 * Loads the file containing the branch that should be flipped w.r.t to the original path profile recorded at runtime
	 */
	private void loadFlipBranchFile() {
		String flipfile = config.getString("cortex.flipfile");
		
		if(flipfile!=null){
			System.out.println("[SymbiosisListener] Loading branch to flip from "+flipfile);
			
			try{
				BufferedReader br = new BufferedReader(new FileReader(flipfile));
				String line;
				while ((line = br.readLine()) != null) {
					String tid = line.substring(0,line.indexOf(' ')); 
					//we subtract 1 because we want to flip the n-th branch, meaning we allow n-1 branches to proceed according to the BB trace 
					Integer branch = Integer.valueOf(line.substring(line.indexOf(' ')+1)) - 1;
					flipBranchMap.put(tid, branch); 
					System.out.println("[SymbiosisListener] Flip branch #"+(branch)+" of thread "+tid+"\n");	   
				}
				//if we are running with branches flipped, store traces into a particular folder
				symbTraceFolder += (System.getProperty("file.separator")+"sts");
				File tempFile = new File(symbTraceFolder);
				if(!(tempFile.exists()))
					tempFile.mkdir();
				
				br.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Loads the file containing the references to the shared accesses identified by the static analysis
	 */
	private void loadSharedAccesses() {
		String fname = config.getString("symbiosis.sharedAccesses");
		System.out.println("[SymbiosisListener] Loading shared accesses from "+fname);
		try{
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = br.readLine()) != null) {
				sharedAccesses.add(line);
			}
			br.close();

			if(DEBUG)
			{
				for(String acc : sharedAccesses)
				{
					System.out.println("\t"+acc);
				}
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Loads the thread execution paths recorded at runtime.
	 */
	private void loadBBTrace()
	{
		String fname = config.getString("symbiosis.bbtrace");
		System.out.println("[SymbiosisListener] Loading BB trace from "+fname);
		try{
			 //check whether trace corresponds to a failing or successfull execution
            if(fname.contains(".fail"))
                failedExec = true;
            else 
                failedExec = false;
			
            //parse execution id
            int end = fname.lastIndexOf('_');
            int init = fname.substring(0,end).lastIndexOf('_');
            executionId = fname.substring(init+1, end);
            
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line;
			while ((line = br.readLine()) != null) {
				if(line.startsWith("[")){
					line = line.substring(1, line.length()-1); //transforms "[tid bbid]" into "tid bbid"
					assertThread = line.split(" ")[0];
					
					if(failedExec)
						logSymbEvent(assertThread, "<assertThread_fail>");
					else
						logSymbEvent(assertThread, "<assertThread_ok>");
				}

				String[] vals = line.split(" "); 
				String tid = vals[0];
				String bbid = vals[1]; 

				if(bbtrace.containsKey(tid)){
					bbtrace.get(tid).add(bbid);
				}
				else{
					Vector<String> tmp = new Vector<String>();
					tmp.add(bbid);
					bbtrace.put(tid, tmp);
				}
			}
			br.close();

			if(DEBUG)
			{
				for(Entry<String, Vector<String>> entry : bbtrace.entrySet())
				{
					System.out.print("\tT"+entry.getKey()+": ");
					for(String bbid : entry.getValue())
					{
						System.out.print(bbid+" ");
					}
					System.out.println("");
				}
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public void stateAdvanced(Search search) 
	{
		ThreadInfo ti = search.getVM().getCurrentThread();
		String tid = search.getVM().getCurrentThread().getName();

		//account for calls to monitor inside <clini> and <init> methods,
		//where t-main name has not yet been changed
		if(tid.equals("main"))
			tid = "0";

		if(!stateOkToLog.containsKey(getCurStateId(tid)))
			stateOkToLog.put(getCurStateId(tid),true);
	}

	public void stateBacktracked (Search search)
	{
		String tid = search.getVM().getCurrentThread().getName();
		
		//store symbolic trace if this state is backtracking because it is an end state
		String file = search.getVM().getLastInstruction().getFileLocation();
		if(file.contains("[synthetic] [") && 
				(canRunFree(tid) || !hasBBsToVisit(tid))){
			StateInfo curState = getCurStateInfo(tid);
			
			//save path conditions
			String cond = "";
			PCChoiceGenerator pccg = search.getVM().getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
			String pathkey =  tid+"_"+getPathId(tid); //get the map key corresponding to the path id of the last state
			if (pccg != null 
					&& pccg.getThreadInfo().getName()==tid 
					&& pathPerThread.containsKey(pathkey)){
				cond = "<pathjpf>";
				for(String cnst : pathPerThread.get(pathkey)){
					cond += "\nT"+tid+":("+cnst+")";
				}
				cond += "\npathid"+getPathId(tid);

				//System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] Thread finished execution. Save PC: \n"+ cond+"\n");
			}
			//save the complete thread log into file
			if(isCorrectTrace(tid, curState)){
				System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] No more states to explore! Store symbolic trace.");
				Utilities.storeSymbLog(symbTraceFolder, tid, curState.symbTrace+cond, curState.pathid);
			}
		}
		
		//backtrack state info if this state has logged any branch
		String parentState = stateTree.get(tid+"_"+pointerToSS.getId());
		if(//canRunFree(tid) &&    //Nuno: this needs to be commented, otherwise JPF may not copy the parent information in some necessary cases
			mapStateInfo.containsKey(getCurStateId(tid)) 
			&& mapStateInfo.containsKey(parentState) 
			&& getCurStateInfo(tid).pathid.length() > mapStateInfo.get(parentState).get(0).pathid.length()){
				System.out.println("["+getStatePathId(tid,file)+"] state backtracked (copy state information from parent "+parentState+") prevState: "+getPrevStateId(tid));
				backtrackStateInfo(tid);
		}
		else{
			System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] state backtracked (don't copy parent state)");
			if(mapStateInfo.containsKey(getCurStateId(tid)) && mapStateInfo.containsKey(parentState)){
				System.out.println("-- curPath: "+getCurStateInfo(tid).pathid+" > parentPath: "+mapStateInfo.get(parentState).get(0).pathid);
			}
		}
	}

	public void stateProcessed(Search search) 
	{
		String tid = search.getVM().getCurrentThread().getName();
		//System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] State processed (is end state? "+pointerToSS.isEndState()+")");
	}
	
	
	public void stateStored(Search search) 
	{
		String tid = search.getVM().getCurrentThread().getName();
		System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"]  Store state "+getCurStateId(tid)+" (parent state: "+getPrevStateId(tid)+")");
		
		//copy parent state to the new state
		copyParentStateInfo(tid);
		
		//store trace if there are no more BBs to visit 
		if(!hasBBsToVisit(tid) && !threadsFinished.contains(tid)){
			StateInfo curState = getCurStateInfo(tid);
			
			String pathkey =  tid+"_"+curState.pathid; //get the map key corresponding to the path id of the current state
			String cond = "";
			if(pathPerThread.containsKey(pathkey)){
				cond = "<pathjpf>";
				System.out.println("PATHKEY: "+pathkey);//+"  ;  "+pathPerThread);
				for(String cnst : pathPerThread.get(pathkey)){
					cond += "\nT"+tid+":("+cnst+")";
				}
				cond += "\npathid"+getPathId(tid);
			}
			//save the complete thread log into file
			if(isCorrectTrace(tid, curState)){
				System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] Thread finished execution. Store symbolic trace.\n");
				Utilities.storeSymbLog(symbTraceFolder, tid, curState.symbTrace+cond, curState.pathid);
			}
			search.getVM().getCurrentThread().setTerminated();
			threadsFinished.add(tid);

			//if all threads have already finished, then exit
			if(threadsFinished.size() == bbtrace.keySet().size()){
				System.out.println("[SymbiosisJPF] All thread have consumed their logs. Terminate execution.");
				endTime = System.nanoTime();
				double time = (((double)(endTime - startTime)/1000000000));
				System.out.println("[SymbiosisJPF] EXECUTION TIME: "+time+"s\n");
				System.exit(1);
			}
		}
	}
	
	public void stateRestored(Search search) 
	{
		String tid = search.getVM().getCurrentThread().getName();
		System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"]  Restore state "+getCurStateId(tid));//+" (prevstate: "+getPrevStateId(tid)+")");
	}

	/**
	 * 	This method is called every time there is a decision to take regarding the execution path.
	 */
	public void choiceGeneratorAdvanced(JVM jvm) 
	{
		SystemState ss = jvm.getSystemState();
		ChoiceGenerator<?> cg = ss.getChoiceGenerator();
		String tid = jvm.getCurrentThread().getName();

		// In case the choice is related to a symbolic variable
		if (cg instanceof PCChoiceGenerator) {

			PCChoiceGenerator pccg = (PCChoiceGenerator) cg;
			int choice = pccg.getNextChoice();
			canLogBranch.put(pccg.getThreadInfo().getName(), choice);	
			System.out.println("ADD CHOICE "+getCurStateId(tid)+": canLogBranch["+pccg.getThreadInfo().getName()+"] = "+choice);
		}
		else if (cg instanceof ThreadChoiceGenerator) {

			if(hasForked){
				hasForked = false;
				ThreadChoiceFromSet threadCg = (ThreadChoiceFromSet) cg;
				ThreadInfo[] threads = threadCg.getChoices();
				for (ThreadInfo t : threads){
					//System.out.println("      - choice: "+t.getName());
					if(t.isDaemon() && !daemonThreads.contains(t.getName())){
						daemonThreads.add(t.getName());
						System.out.println("[SymbiosisJPF] add new deamon thread "+t.getName()+" (size: "+daemonThreads.size()+")");
					}
				}
			}
			
			//if thread is daemon thread, then terminate and remove entry from daemonThreads
			//otherwise, wait until all daemon threads have finished
			/*if(bbtrace.get(tid)!=null && bbtrace.get(tid).isEmpty()){

				System.out.println(" THREAD IS ABOUT TO FINISH - CHOICE GEN");
				if(ti.isDaemon()){
					daemonThreads.remove(tid);
				}
				else{
					System.out.println("["+getStatePathId(tid,search.getVM().getLastInstruction().getFileLocation())+"] there are daemon threads running. Delay finishing this thread.");
					while(!daemonThreads.isEmpty()){
						ti.yield();
					}
				}	
			}*/
		}
	}


	public void executeInstruction(JVM vm)
	{
		// Gets the next instruction scheduled in the VM
		Instruction nextIns = vm.getNextInstruction();
		ThreadInfo ti = vm.getCurrentThread();
		String tid = ti.getName();
		String file = nextIns.getFileLocation();

		//we only want the thread with the branch flip to proceed when we execute in flipFile mode
		String target = config.getString("target");
		if(threadsFinished.contains(tid)
				|| (!flipBranchMap.isEmpty() && !flipBranchMap.containsKey(tid) && !(tid.equals("0") || tid.equals("main")) && !target.equals("bouvlesort.Loader")))
		{
			ti.setTerminated();
			ti.skipInstruction(); //Nuno: this needs to be like this, because with ti.skipInstruction(nextIns) JPF hangs (?)
			return;
		}

		if (nextIns instanceof INVOKESTATIC){
			INVOKESTATIC virtualIns = (INVOKESTATIC) nextIns;
			String method = virtualIns.getInvokedMethod().getName();

			if(method.contains("symbiosisBBEntry"))
			{	
				if(tid.equals("main"))
					tid = "0";
				
				String bbid = virtualIns.getArgumentValues(ti)[0].toString();
				
				handleSpecialRunFree(tid,vm); 
				if(!canRunFree(tid))
				{
					//is not free-run mode, so the thread must adhere to the original path profile in the BB trace
					checkBBTrace(tid, bbid, vm);
				}
				else{
					//is free-run mode
					System.out.println("["+getStatePathId(tid,file)+"] bbid: "+bbid+" -> has flipped a branch, so it is allowed to proceed freely.");
					addNewBranch(tid, vm);
					stateOkToLog.put(getCurStateId(tid), true);
					return;
				}
			}
			else if(method.contains("assertHandler") && assertThread.equals(tid))
			{
				StateInfo curState = getCurStateInfo(tid);
				curState.hitAssert = true;
				System.out.println("["+getStatePathId(tid,file)+"] Assertion Hit: "+curState.hitAssert);
			}
		}
		//SPECIAL INVOCATIONS *** Used to detect AssertionError invocations
		else if(nextIns instanceof INVOKESPECIAL){
			//delay assertion error instruction if all threads haven't finished yet
			if(((INVOKESPECIAL) nextIns).toString().contains("java.lang.AssertionError"))
			{
				System.out.println("["+getStatePathId(tid,file)+"] Reached assertion error -> store failing trace");
				
				//store symbolic trace if this thread can run free
				StateInfo curState = getCurStateInfo(tid);
				if((canRunFree(tid) || !hasBBsToVisit(tid))
						&& assertThread.equals(tid)) //checking assertThread is probably not necessary...
				{
					//change tag <assertThread_ok> to <assertThread_fail> if necessary
					try{ 
						curState.symbTrace = curState.symbTrace.replace("<assertThread_ok>", "<assertThread_fail>");
					}catch(Exception e)
					{
						System.out.println("["+getStatePathId(tid,file)+"] ERROR replacing assert");
					}

					//save path conditions
					String cond = "";
					PCChoiceGenerator pccg = vm.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
					String pathkey =  tid+"_"+getPathId(tid); //get the map key corresponding to the path id of the last state
					if (pccg != null 
							&& pccg.getThreadInfo().getName()==tid 
							&& pathPerThread.containsKey(pathkey)){
						cond = "<pathjpf>";
						for(String cnst : pathPerThread.get(pathkey)){
							cond += "\nT"+tid+":("+cnst+")";
						}
						cond += "\npathid"+getPathId(tid);
					}
					if(isCorrectTrace(tid, curState)){
						System.out.println("["+getStatePathId(tid,file)+"] Assert error! Store symbolic trace.");
						Utilities.storeSymbLog(symbTraceFolder, tid, curState.symbTrace+cond, curState.pathid);
					}
				}
				pointerToSearch.setIgnoredState(true); 
			}
		}
		/*else if(nextIns instanceof ATHROW){
			System.out.println("["+getStatePathId(tid,file)+"] Throw exception -> ignore instruction");
		    vm.getCurrentThread().skipInstruction(nextIns);
		    pointerToSearch.setIgnoredState(true); 
		}*/
		prevState.put(tid, getCurStateId(tid));
		//System.out.println("PREVSTATE["+tid+"] = "+getCurStateId(tid));
	}
	
	
	/**
	 * Called after the execution of an instruction.
	 * - Marks shared variables as symbolic (according to the shared accesses log)
	 * - Generates the log file with the symbolic operations executed
	 */
	public void instructionExecuted(JVM vm) 
	{
		// Info that it is going to be used throughout the method
		Instruction lastIns = vm.getLastInstruction();	// The last instruction executed
		ThreadInfo ti = vm.getLastThreadInfo();	// Information regarding the thread that executed the last instruction
		String tid = ti.getName();
		ElementInfo ei = vm.getLastElementInfo(); // Information regarding the last element used. It completely depends on the instruction.
		MethodInfo mi = lastIns.getMethodInfo(); // Information regarding the method to which the last instruction belongs
		
		if (mi == null){
			System.out.println("[SymbiosisJPF] There might be a problem, MethodInfo is not set for the last instruction!");
			return;
		}

		// We always need to check whether the instruction is completed or not in order to avoid transition breaks
		// These breaks sometimes forces an instruction to be re-executed
		if (lastIns.isCompleted(ti)){
			int line = lastIns.getLineNumber();
			String file = lastIns.getFileLocation();

			//for some weird reason, it might happen that a thread executes an instruction after being terminated
			if(threadsFinished.contains(tid)){
				return;
			}

			//System.out.println("--> "+lastIns+" ("+lastIns.getFileLocation()+")");
			//vm.getCurrentThread().printStackTrace();
			
			//account for calls to monitor inside <clinit> and <init> methods,
			//where t-main name has not yet been changed
			if(tid.equals("main"))
				tid = "0";
			
			//Handle reads
			if ((lastIns instanceof GETFIELD) || (lastIns instanceof GETSTATIC)) {
				FieldInstruction getfieldIns = (FieldInstruction) lastIns;
				String access = getAccessSig(getfieldIns, line);

				if(getfieldIns.getLastElementInfo() == null){
					System.out.println("["+getStatePathId(tid,file)+"] There might be a problem, field info is null!");
					return;
				}

				//log read event if it is a shared access
				if(sharedAccesses.contains(access)){		
					//System.out.println("-- R_ACCESS "+access);
					StateInfo curState = getCurStateInfo(tid);
					String symbvar = curState.getRWSymbName(getfieldIns, false);
					handleSharedRW(tid, symbvar, getfieldIns, vm, false);
				}
				//else System.out.println(" -- NO R_ACCESS "+access);
			}
			//Handle writes
			else if ((lastIns instanceof PUTSTATIC)||(lastIns instanceof PUTFIELD)){
				FieldInstruction putfieldIns = (FieldInstruction) lastIns;
				
				String access = getAccessSig(putfieldIns, line);

				if(putfieldIns.getLastElementInfo() == null){
					System.out.println("["+getStatePathId(tid,file)+"] There might be a problem, field info is null!");
					return;
				}

				//log write event if it is a shared access
				if(sharedAccesses.contains(access)){
					//System.out.println("-- W_ACCESS "+access);
					StateInfo curState = getCurStateInfo(tid);
					String symbvar = curState.getRWSymbName(putfieldIns, true);
					handleSharedRW(tid, symbvar, putfieldIns, vm, true);
				}
				//else System.out.println(" -- NO W_ACCESS "+access);
			}
			//handle writes on arrays
			else if(lastIns instanceof IASTORE || lastIns instanceof AASTORE){
				String access = "";
				try{
					//get array field name
					int fieldPointer = 1; //indicates the correct getfield/getstatic that we should point to; allows to handle cases where we write into an array the value of another array
					Instruction fieldInst = lastIns;
					while(fieldPointer > 0){
						fieldInst = fieldInst.getPrev();
						if(fieldInst instanceof IALOAD)
							fieldPointer++;
						
						if(fieldInst instanceof GETFIELD || fieldInst instanceof GETSTATIC)
							fieldPointer--;
						/*System.out.println("=== fieldInst: "+fieldInst+", iaload? "+(fieldInst instanceof IALOAD)+
								", getfield/getstatic? "+(fieldInst instanceof GETFIELD || fieldInst instanceof GETSTATIC)+
								", fieldPointer: "+fieldPointer);*/
					}
					
					access = getAccessSig((FieldInstruction)fieldInst, line); 
					if(sharedAccesses.contains(access)){
						//System.out.println("-- W_IASTORE_ACCESS "+access);
						StateInfo curState = getCurStateInfo(tid);
						String symbvar = curState.getRWArraySymbName(lastIns, (FieldInstruction)fieldInst, ti);
						handleSharedArrayRW(tid, symbvar, (FieldInstruction)fieldInst, vm, true);
					}
					//else System.out.println(" -- NO W_IASTORE_ACCESS "+access);
				}
				catch(Exception e)
				{
					//System.out.println("[EXCEPTION] IASTORE ("+access+"):"+e.getMessage());
					//e.printStackTrace();
				}
			}
			//handle reads on arrays
			else if(lastIns instanceof IALOAD || lastIns instanceof AALOAD)
			{
				String access = "";
				try{
					//get array field name
					Instruction fieldInst = lastIns.getPrev();
					while(!(fieldInst instanceof GETFIELD)
							&& !(fieldInst instanceof GETSTATIC)){
						fieldInst = fieldInst.getPrev();
					}
					
					access = getAccessSig((FieldInstruction)fieldInst, line); 
					if(sharedAccesses.contains(access)){
						StateInfo curState = getCurStateInfo(tid);
						String symbvar = curState.getRWArraySymbName(lastIns, (FieldInstruction)fieldInst, ti);
						handleSharedArrayRW(tid, symbvar, (FieldInstruction)fieldInst, vm, false);
					}
					//else System.out.println(" -- NO RA_ACCESS (iaload) "+access);
				}catch(Exception e)
				{
					//System.out.println("[EXCEPTION] IALOAD ("+access+"):"+e.getMessage());
					//e.printStackTrace();
				}
			}
			else if (lastIns instanceof ALOAD) {
				try{
					String name = ((ALOAD) lastIns).getLocalVariableName();
					String type = ((ALOAD) lastIns).getLocalVariableType();
					String loc = lastIns.getFileLocation();
					
					/*if(lastIns.getFileLocation().equals("org/apache/log4j/helpers/Loader.java:94")
							|| loc.equals("org/apache/log4j/helpers/Loader.java:97")
							|| loc.equals("org/apache/commons/pool/impl/CursorableLinkedList.java:746")
							|| loc.equals("org/apache/commons/pool/impl/CursorableLinkedList.java:748"))
					{
						String symbvar = "R-_next_637-2-2";
						//System.out.println("-- symbvar: "+symbvar);
						Symbolic.newSymbolic(type,symbvar,vm);
					}//*/
					Object attr = ti.getOperandAttr();
					if(attr == null && 
							(lastIns.getFileLocation().equals("org/apache/commons/pool/impl/CursorableLinkedList.java:748")
								|| lastIns.getFileLocation().equals("org/apache/log4j/helpers/Loader.java:94")
								|| lastIns.getFileLocation().equals("org/apache/log4j/helpers/Loader.java:98")
							))
					{
						System.out.println(" -- attr is null, make it symbolic");
						Symbolic.newSymbolic(type,"R-TEMP",vm);
					}
					//System.out.println(" -- ALOAD "+lastIns+" var: "+name+", attr: "+attr+", type: "+type+" ("+loc+")");
				}
				catch(Exception e){
					//System.out.println(" -- EXCEPTION ALOAD "+lastIns);
				}//*/
			}
			else if(lastIns instanceof MONITORENTER){
				//System.out.println("--> MONITOR ENTER: "+lastIns);
				MONITORENTER monEnterIns = (MONITORENTER) lastIns;
				if (monEnterIns.getSourceLine()!=null){					
					ElementInfo obj = vm.getElementInfo(monEnterIns.getLastLockRef()); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(monEnterIns, line, ti, "lock",object);
				}
			}
			else if(lastIns instanceof MONITOREXIT){
				//System.out.println("--> MONITOR EXIT: "+lastIns);
				MONITOREXIT monExitIns = (MONITOREXIT) lastIns;
				if (monExitIns.getSourceLine()!=null){
					ElementInfo obj = vm.getElementInfo(monExitIns.getLastLockRef()); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(monExitIns, line, ti, "unlock",object);
				}
			}

			//STATIC INVOCATIONS *** Used to detect calls to symbiosisBBEntry
			else if (lastIns instanceof INVOKESTATIC){
				INVOKESTATIC virtualIns = (INVOKESTATIC) lastIns;
				String method = virtualIns.getInvokedMethod().getName();
			}
			//Used to detect the run method of a new thread
			if (lastIns.isFirstInstruction()){
				String methodName = ti.getMethod().getName()+ti.getMethod().getSignature();

				// Identifying when a new thread is starting in order to trace start events
				if (methodName.equals("run()V") && !lastIns.getFileLocation().contains("synthetic"))
				{
					//make sure we only log one start event per thread
					if(!threadsStarted.contains(getCurStateId(tid)))
					{
						HashSet<String> vars = new HashSet<String>();
						vars.add("start-"+tid);
						threadsStarted.add(getCurStateId(tid));

						//log event
						String event = Utilities.getFileShortName(lastIns.getFileLocation()).replace(':', '@')+":S-start-"+tid;
						
						if(tid.equals(assertThread)){ //store success/failure if this is the thread with the assertion
							if(failedExec)
								logSymbEvent(tid, "<assertThread_fail>");
							else
								logSymbEvent(tid, "<assertThread_ok>");
						}
						logSymbEvent(tid, event);		

						if(DEBUG) 
							System.out.println("["+getStatePathId(tid,String.valueOf(line))+"] Log event "+event);	
					}
				}
			}
			//VIRTUAL INVOCATIONS *** Used to detect start, join, lock, unlock, newCondition
			else if (lastIns instanceof INVOKEVIRTUAL){

				INVOKEVIRTUAL virtualIns = (INVOKEVIRTUAL) lastIns;
				String method = virtualIns.getInvokedMethod().getName();
				String invokedMethod = virtualIns.getInvokedMethodName();

				// Start method invocation
				if ((method.equals("start")) && (virtualIns.getInvokedMethod().getClassInfo().getName().equals("java.lang.Thread")))
				{
					logPOSyncEvent(virtualIns, line, ti, "fork");
					hasForked = true;
				}
				//Join method invocation
				else if ((method.equals("join")) && (virtualIns.getInvokedMethod().getClassInfo().getName().equals("java.lang.Thread")))
				{
					logPOSyncEvent(virtualIns, line, ti, "join");

					System.out.println("["+ti.getName()+"] skip JOIN");
					/*StackFrame sf = ti.popFrame();
					Instruction nextIns = sf.getPC().getNext();*/
					vm.getCurrentThread().skipInstruction();
				}
				//Wait method invocation
				else if (invokedMethod.equals("wait()V")||invokedMethod.equals("wait(I)V")||invokedMethod.equals("wait(IJ)V"))
				{
					ElementInfo obj = vm.getElementInfo(virtualIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(virtualIns, line, ti, "wait",object);

					System.out.println("["+ti.getName()+"] skip WAIT");
					/*StackFrame sf = ti.popFrame();
					Instruction nextIns = sf.getPC().getNext();*/
					vm.getCurrentThread().skipInstruction();
				}
				//notify method invocation
				else if (invokedMethod.equals("notify()V")||invokedMethod.equals("notifyAll()V"))
				{
					ElementInfo obj = vm.getElementInfo(virtualIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());

					if(invokedMethod.equals("notify()V"))
						logLockSyncEvent(virtualIns, line, ti, "signal",object);
					else
						logLockSyncEvent(virtualIns, line, ti, "signalall",object);
				}
				//Lock method invocation
				else if(invokedMethod.equals("lock()V"))
				{
					ElementInfo obj = vm.getElementInfo(virtualIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(virtualIns, line, ti, "lock",object);
				}
				//Unlock method invocation
				else if(invokedMethod.equals("unlock()V"))
				{
					ElementInfo obj = vm.getElementInfo(virtualIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(virtualIns, line, ti, "unlock",object);
				}
				else if(!virtualIns.getInvokedMethod().getClassName().startsWith("java."))
				{
					//System.out.println("-- method "+virtualIns.getInvokedMethod()+" is sync? "+virtualIns.getInvokedMethod().isSynchronized());
					// Synchronized method invocation
					if (virtualIns.getInvokedMethod().isSynchronized()){
						//System.out.println("--> SYNC METHOD ENTER: "+lastIns);
						ElementInfo obj = vm.getElementInfo(virtualIns.getCalleeThis(ti)); 
						String object = Integer.toHexString(obj.getObjectRef());
						logLockSyncEvent(virtualIns, line, ti, "lock",object);

						//save monitor obj to store the unlock operation when returning from the sync method
						if(methodMonitor.containsKey(tid)){
							methodMonitor.get(tid).push(object);
						}
						else{
							Stack<String> tmp = new Stack<String>();
							tmp.push(object);
							methodMonitor.put(tid, tmp);
						}
					}
				}

			}//end if invokevirtual
			else if(lastIns instanceof INVOKEINTERFACE)
			{
				INVOKEINTERFACE interfaceIns = (INVOKEINTERFACE) lastIns;
				String invokedMethod = interfaceIns.getInvokedMethodName();
				if (invokedMethod.equals("await()V")||invokedMethod.equals("awaitNanos(J)V"))
				{
					ElementInfo obj = vm.getElementInfo(interfaceIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());
					logLockSyncEvent(interfaceIns, line, ti, "wait",object);
				}
				else if (invokedMethod.equals("signal()V")||invokedMethod.equals("signalAll()V"))
				{
					ElementInfo obj = vm.getElementInfo(interfaceIns.getCalleeThis(ti)); 
					String object = Integer.toHexString(obj.getObjectRef());

					if(invokedMethod.equals("signal()V"))
						logLockSyncEvent(interfaceIns, line, ti, "signal",object);
					else
						logLockSyncEvent(interfaceIns, line, ti, "signalall",object);
				}
			}
			//RETURN INSTRUCTION *** Used to detect the end of synchronized methods
			else if (lastIns instanceof ReturnInstruction){
				ReturnInstruction genReturnIns = (ReturnInstruction) lastIns;
				MethodInfo me = genReturnIns.getMethodInfo();
				if(!me.getClassName().startsWith("java.")){
					if (me.isSynchronized() && methodMonitor.containsKey(tid) && !methodMonitor.get(tid).isEmpty()){
						//System.out.println("--> SYNC METHOD EXIT: "+lastIns);
						String object = methodMonitor.get(tid).pop();
						logLockSyncEvent(genReturnIns, line, ti, "unlock", object);
					}
				}
			}
			//SPECIAL INVOCATIONS *** Used to detect AssertionError invocations
			else if(lastIns instanceof INVOKESPECIAL){
				INVOKESPECIAL specialIns = (INVOKESPECIAL) lastIns;	
				if(!specialIns.getInvokedMethod().getClassName().startsWith("java."))
				{
					// Synchronized method invocation
					if (specialIns.getInvokedMethod().isSynchronized()){
						//System.out.println("--> SYNC METHOD ENTER: "+lastIns);
						ElementInfo obj = vm.getElementInfo(specialIns.getCalleeThis(ti)); 
						String object = Integer.toHexString(obj.getObjectRef());
						logLockSyncEvent(specialIns, line, ti, "lock",object);

						//save monitor obj to store the unlock operation when returning from the sync method
						if(methodMonitor.containsKey(tid)){
							methodMonitor.get(tid).push(object);
						}
						else{
							Stack<String> tmp = new Stack<String>();
							tmp.push(object);
							methodMonitor.put(tid, tmp);
						}
					}
				}
				//delay assertion error instruction if all threads haven't finished yet
				/*if(((INVOKESPECIAL) lastIns).toString().contains("java.lang.AssertionError"))
				{
					logSymbEvent(tid, "<assertThread_fail>");
					System.out.println("["+getStatePathId(tid,file)+"] Assertion Error -> proceed");
					//check if all threads have already finished
					//if(threadsFinished.size()!=bbtrace.keySet().size())
					//{
					//	vm.getSystemState().setBoring(true);
					//}
				}*/
			}
		}//end instruction.isCompleted()
	}
	
	
	public String getAccessSig(FieldInstruction fieldInst, int line)
	{
		String sig = fieldInst.getFieldInfo().getFullName();
		if(sig.contains("$")){
			//System.out.println(" -- sig: "+sig);
			int init = sig.indexOf("$");
			int end = sig.indexOf(".",init);
			if(end!=-1)
				sig = sig.substring(0,init)+sig.substring(end);
		}
		
		String access = sig+"@"+line;
		return access;
	}
	
	/**
	 * Check whether the basic block conforms with the thread path profile.
	 * @param tid
	 * @param bbid
	 * @param vm
	 */
	public void checkBBTrace(String tid, String bbid, JVM vm){
	
		//in case the thread is not in the logfile
		if(!bbtrace.containsKey(tid)){
			System.out.println(" --> checkBBTrace "+tid+" not in bbtrace -> ignore state");
			pointerToSearch.setIgnoredState(true); 
			stateOkToLog.put(getCurStateId(tid), false);
			return;
		}
		
		String file = vm.getNextInstruction().getFileLocation();
		if(hasBBsToVisit(tid))
		{
			StateInfo curState = getCurStateInfo(tid); 
			String nextbbid = bbtrace.get(tid).elementAt(curState.bbsReached);
			if(bbid.equals(nextbbid))
			{
				incBBsVisited(tid);
				if(DEBUG)
					System.out.println("["+getStatePathId(tid,file)+"] bbid: "+bbid+" == tracebbid: "+nextbbid+" -> OK ("+curState.bbsReached+" out of "+(bbtrace.get(tid).size())+")");
				pointerToSS.setInteresting(true);
				stateOkToLog.put(getCurStateId(tid), true);
				
				//log branch if necessary
				addNewBranch(tid, vm);
			}
			/*else if(getCurStateId(tid).equals("0_0")
					|| getCurStateId(tid).equals("0_-1"))
			{
				System.out.println("["+getStatePathId(tid,file)+"] bbid: "+bbid+" != tracebbid: "+nextbbid+" -> DON'T STOP (init state, "+curState.bbsReached+" out of "+(bbtrace.get(tid).size())+")");
			}//*/
			else
			{
				if(DEBUG)
					System.out.println("["+getStatePathId(tid,file)+"] bbid: "+bbid+" != tracebbid: "+nextbbid+" -> STOP ("+curState.bbsReached+" out of "+(bbtrace.get(tid).size())+")");
				
				//make sure that we don't trace this path condition on another state
				if(canLogBranch.containsKey(tid) && canLogBranch.get(tid) >= 0)
					canLogBranch.put(tid,-1);
				
				pointerToSearch.setIgnoredState(true); 
				stateOkToLog.put(getCurStateId(tid), false);
			}
		}
	}

	/**
	 * Adds a new branch (along with the corresponding path condition) to a thread's symbolic trace
	 * @param tid
	 * @param vm
	 */
	public void addNewBranch(String tid, JVM vm){

		if(canLogBranch.containsKey(tid) && canLogBranch.get(tid) >= 0){
			
			String constraint = getPathConditionConstraint(tid, vm);
			if(!constraint.isEmpty()	//there might be cases where canLogBranch is positive due to state backtracking
					&& !constraint.contains("W-"))				//we don't want PCs that are on write events (TODO: is this correct?) <- this causes JPF to still explore unwanted branches 
			{ 
				//save current state for future backtracks
				StateInfo curState = getCurStateInfo(tid);
				mapStateInfo.get(getCurStateId(tid)).add(new StateInfo(pointerToSS.getId(), curState));
				
				int choice = canLogBranch.get(tid);
				System.out.println("["+getStatePathId(tid,vm.getLastInstruction().getFileLocation())+"] Log branch (update state with "+choice+")");

				updatePathId(tid,choice);	//update path id according to the choice taken 
				logBranch(tid);
				canLogBranch.put(tid, -1);

				//add this branch condition to the corresponding thread's path in pathPerThread data structure
				try{
					System.out.println("PATH CONDITION: "+constraint);
					pathPerThread.get(tid+"_"+getPathId(tid)).add(constraint);
				}
				catch(NullPointerException e){
					ArrayList<String> pathT = new ArrayList<String>();
					pathT.add(constraint);
					pathPerThread.put(tid+"_"+getPathId(tid), pathT);
				}
				System.out.println("["+getCurStateId(tid)+"] PCs: "+pathPerThread.get(tid+"_"+getPathId(tid)));
				
				//update number of branches reached, if necessary
				if(flipBranchMap.containsKey(tid) && curState.brchsReached < flipBranchMap.get(tid)){
					incBranchesVisited(tid);
					System.out.println("["+getStatePathId(tid,vm.getLastInstruction().getFileLocation())+"] Branches remaining until flip: "+(flipBranchMap.get(tid)-curState.brchsReached));
				}  
			}

		}

	}
	
	
	/**
	 * Returns true if the thread hasn't visited all BBs indicated in its path trace.
	 * @param tid
	 * @return
	 */
	private boolean hasBBsToVisit(String tid)
	{
		try{
			return getCurStateInfo(tid).bbsReached < bbtrace.get(tid).size();
		}
		catch(NullPointerException e){
			return true; //necessary for the case where the data structures weren't initialized yet
		}
	}
	
	/**
	 * Increments the number of BBs visited by this thread.
	 * @param tid
	 */
	private void incBBsVisited(String tid)
	{
		getCurStateInfo(tid).bbsReached++;
	}
	
	private void incBranchesVisited(String tid)
	{
		getCurStateInfo(tid).brchsReached++;
	}
	
	private boolean canRunFree(String tid)
	{
		if(!flipBranchMap.containsKey(tid))
			return false;
		
		
		if(flipBranchMap.get(tid) == 0 && !getCurStateInfo(tid).mayRunFree){
			System.out.println("["+getCurStateId(tid)+"] Cannot run free yet!");
			return false;
		}
		
		return (getCurStateInfo(tid).brchsReached >= flipBranchMap.get(tid));
	}

	/**
	 * Guarantees that all BBs are correctly followed until the first branch.
	 * @param tid
	 */
	private void handleSpecialRunFree(String tid, JVM vm)
	{
		if(!flipBranchMap.containsKey(tid) 
				|| flipBranchMap.get(tid) > 0)
			return;
		
		String constraint = getPathConditionConstraint(tid, vm);
		if(!constraint.isEmpty()	//there might be cases where canLogBranch is positive due to state backtracking
				&& !constraint.contains("W-"))				//we don't want PCs that are on write events (TODO: is this correct?) <- this causes JPF to still explore unwanted branches 
		{ 
			getCurStateInfo(tid).mayRunFree = true;
			System.out.println("["+getCurStateId(tid)+"] Can run free!");
		}
	}
	
	/**
	 * Returns the value written by a write operation on a symbolic variable
	 * @param lastIns
	 * @param ei
	 * @param vm
	 * @return
	 */
	public Object extractWrittenValue(FieldInstruction lastIns, ElementInfo ei, JVM vm)
	{
		FieldInfo fi = lastIns.getFieldInfo();
		String type = fi.getType();
		Object attr = null;
		long lvalue = lastIns.getLastValue();

		if (lastIns instanceof PUTSTATIC){	
			attr = fi.getClassInfo().getStaticElementInfo().getFieldAttr(fi);
		}
		else if (lastIns instanceof PUTFIELD){
			ei = lastIns.getLastElementInfo();
			attr = ei.getFieldAttr(fi);			
		}
		Type itype = Utilities.typeToInteger(type);
		Type rtype = Utilities.simplifyTypeFromType(itype);
		Object value = null;
		// Giving the proper shape to the read value.
		if (itype==Type.INT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
				//System.out.println("Writing symint "+((SymbolicInteger)((BinaryLinearIntegerExpression)value).getLeft())._max);
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.BOOLEAN){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
				//System.out.println("Writing symint "+((SymbolicInteger)((BinaryLinearIntegerExpression)value).getLeft())._max);
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.BYTE){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.CHAR){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.LONG){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.SHORT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.REAL){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMREAL;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.FLOAT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMREAL;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			// TODO: Not working for String. lastValue is going to be the object reference.
		}else if (itype==Type.STRING){
			if (attr != null){
				rtype = Type.SYMSTRING;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			//System.out.println("WARNING: String variable. Not ready for it yet.");
		}else if (itype==Type.REFERENCE){
			if (attr != null){
				rtype = Type.SYMREF;
				value = attr;
			}else{
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			//System.out.println("WARNING: Ref value for field "+fi.getName()+": "+type+" (value: "+value+")");
		}
		
		try{
			System.out.print(" type: "+type);
			System.out.print(" | attr: "+(attr != null ? attr : "null"));
			System.out.print(" | lvalue: "+lvalue);
			System.out.println(" | value: "+(value!=null ? value : "null"));
			
			//remove "valueOf"
			if(value.toString().startsWith(".valueof[")){
				String newval = value.toString();
				newval = newval.substring(9,newval.length()-1);
				value = newval;
			}
		}
		catch(Exception e){ System.out.println(" | null");}
		
		
		return value;
	}
	
	/**
	 * Returns the value written by a write operation on a symbolic variable
	 * @param type
	 * @param vm
	 * @return
	 */
	public Object extractWrittenArrayValue(String type, JVM vm)
	{
		Instruction lastInst = vm.getLastInstruction();
		int arrayRef = -1;
		int index = -1;
		if(lastInst instanceof IASTORE){
			arrayRef = ((IASTORE)lastInst).getArrayRef(vm.getLastThreadInfo());
			index = ((IASTORE)lastInst).getIndex(vm.getLastThreadInfo());
		}
		else if(lastInst instanceof AASTORE){
			arrayRef = ((AASTORE)lastInst).getArrayRef(vm.getLastThreadInfo());
			index = ((AASTORE)lastInst).getIndex(vm.getLastThreadInfo());
		}
		
		Type itype = Utilities.typeToInteger(type);
		Type rtype = Utilities.simplifyTypeFromType(itype);
		ElementInfo eiArray = vm.getElementInfo(arrayRef);
		long lvalue;
		Object attr = eiArray.getElementAttr(index);
		Object value = null;
		//System.out.println(" type: "+type+" | attr: "+attr+" | itype: "+itype);
		
		// Giving the proper shape to the read value.
		if (itype==Type.INT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
				//System.out.println("Writing symint "+((SymbolicInteger)((BinaryLinearIntegerExpression)value).getLeft())._max);
			}else{
				lvalue = eiArray.getIntElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.BOOLEAN){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
				//System.out.println("Writing symint "+((SymbolicInteger)((BinaryLinearIntegerExpression)value).getLeft())._max);
			}else{
				boolean b = eiArray.getBooleanElement(index);
				lvalue = b ? 1 : 0;
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.BYTE){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
			}else{
				lvalue = eiArray.getByteElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.CHAR){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes){
				rtype = Type.SYMINT;
				value = attr;
			}else{
				lvalue = eiArray.getCharElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.LONG){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
			}else{
				lvalue = eiArray.getLongElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.SHORT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMINT;
				value = attr;
			}else{
				lvalue = eiArray.getShortElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.REAL){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.SYMREAL;
				value = attr;
			}else{
				lvalue = eiArray.getLongElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
		}else if (itype==Type.FLOAT){
			if (attr != null && !attr.toString().startsWith("W-")){ //we don't want references to writes
				rtype = Type.FLOAT;
				value = attr;
			}else{
				lvalue = (long) eiArray.getFloatElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			// TODO: Not working for String. lastValue is going to be the object reference.
		}else if (itype==Type.STRING){
			if (attr != null){
				rtype = Type.SYMSTRING;
				value = attr;
			}else{
				lvalue = eiArray.getLongElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			System.out.println("WARNING: String variable. Not ready for it yet.");
		}else if (itype==Type.REFERENCE){
			if (attr != null){
				rtype = Type.SYMREF;
				value = attr;
			}else{
				lvalue = eiArray.getReferenceElement(index);
				value = Utilities.transformValueFromLong(vm, lvalue, type);
			}
			//System.out.println("WARNING: Ref value for field "+fi.getName()+": "+type+" (value: "+value+")");
		}
		//System.out.println(" type: "+type+" | attr: "+attr+" | lvalue: "+lvalue+" | value: "+value);
		
		return value;
	}
	
	/**
	 *  Creates a symbolic variable for the RW access and logs the corresponding event.
	 * @param tid
	 * @param symbvar
	 * @param vm
	 * @param fieldInst
	 * @param isWrite
	 */
	public void handleSharedRW(String tid, String symbvar, FieldInstruction fieldInst, JVM vm, boolean isWrite)
	{
		ElementInfo ei = vm.getLastElementInfo();
		String type = fieldInst.getFieldInfo().getType();		
		
		//if field is of type array, then don't log anything (the access is properly handled by Xastore/Xaload instructions)
		if(type.contains("[]")){
			return;
		}
		
		//mark variable as symbolic
		if(!isWrite){
			//System.out.println("["+getStatePathId(tid,file)+"] symbvar: "+symbvar);
			Symbolic.newSymbolic(type,symbvar,vm);
		}
		
		//update id for next symbolic var
		getCurStateInfo(tid).incrementSymbVarId(symbvar); 

		//log event
		String event = Utilities.getFileShortName(fieldInst.getFileLocation()).replace(':', '@')+":"+symbvar;
		if(isWrite){
			Object valueObj = extractWrittenValue(fieldInst, ei, vm);
			String value;
			try{
				value = valueObj.toString();
			}catch(NullPointerException e){
				value = "0";
			}

			//check whether written value is a reference to other write
			if(value.contains("W-")){
				int initW = value.indexOf("W-");
				while(initW!=-1){
					int endW = value.indexOf(' ', initW);
					if(endW==-1){
						endW = value.indexOf(')', initW);
						if(endW==-1){
							endW = value.length();
						}
					}
					String writeRef = value.substring(initW,endW); 
					//System.out.print("\t-- writeRef = "+writeRef+" writtenValue = "+writtenValues.get(writeRef)+"; reference to "+value+" translated to ");
					value = value.replace(writeRef, writtenValues.get(writeRef));
					//System.out.println(value);
					initW = value.indexOf("W-");
				}
				writtenValues.put(symbvar, value);
			}
			else{
				//System.out.println("\t-- "+symbvar+" -> "+value);
				writtenValues.put(symbvar, value);
			}
			event += "\n$"+value+"$";
		}
		logSymbEvent(tid, event);	

		if(DEBUG) 
			System.out.println("["+getStatePathId(tid,fieldInst.getFileLocation())+"] Log event "+event);
	}
	
	
	public void handleSharedArrayRW(String tid, String symbvar, FieldInstruction fieldInst, JVM vm, boolean isWrite)
	{
		ElementInfo ei = vm.getLastElementInfo();
		String type = fieldInst.getFieldInfo().getType();		
		
		//mark variable as symbolic if it is read
		if(!isWrite){
			//System.out.println("["+getStatePathId(tid,file)+"] symbvar: "+symbvar);
			Symbolic.newSymbolic(type,symbvar,vm);
		}
		
		//update id for next symbolic var
		getCurStateInfo(tid).incrementSymbVarId(symbvar); 
		
		//log event
		String event = Utilities.getFileShortName(fieldInst.getFileLocation()).replace(':', '@')+":"+symbvar;
		if(isWrite){
			Object valueObj = extractWrittenArrayValue(type, vm);
			String value = valueObj.toString();

			//check whether written value is a reference to other write
			if(value.contains("W-")){
				int initW = value.indexOf("W-");
				while(initW!=-1){
					int endW = value.indexOf(' ', initW);
					if(endW==-1){
						endW = value.indexOf(')', initW);
						if(endW==-1){
							endW = value.length();
						}
					}
					String writeRef = value.substring(initW,endW); 
					//System.out.print("\t-- writeRef = "+writeRef+" writtenValue = "+writtenValues.get(writeRef)+"; reference to "+value+" translated to ");
					value = value.replace(writeRef, writtenValues.get(writeRef));
					//System.out.println(value);
					initW = value.indexOf("W-");
				}
				writtenValues.put(symbvar, value);
			}
			else{
				//System.out.println("\t-- "+symbvar+" -> "+value);
				writtenValues.put(symbvar, value);
			}
			event += "\n$"+value+"$";	
		}
		//System.out.println(" > sharedArray attr: "+vm.getLastThreadInfo().getTopFrame().getOperandAttr());
		logSymbEvent(tid, event);	

		if(DEBUG) 
			System.out.println("["+getStatePathId(tid,fieldInst.getFileLocation())+"] Log event "+event);

	}

	/**
	 * Stores into the log file a given partial order synchronization event (e.g. join, fork)
	 * @param virtualIns
	 * @param line
	 * @param ti
	 * @param vm
	 * @param synctype
	 */
	protected void logPOSyncEvent(INVOKEVIRTUAL virtualIns, int line, ThreadInfo ti, String synctype)
	{
		String tid = ti.getName();
		String child = pointerToVM.getThreadList().getThreadInfoForObjRef(virtualIns.getCalleeThis(ti)).getName();

		if(threadsFinished.contains(tid))
			return;

		//lazy way of making sure that we don't log the fork operation when states backtrack
		String symbvar = "S-"+synctype+"_"+child+"-"+tid;
		//if(isNewSymbolicVar(virtualIns.getFileLocation(), (symbvar+"-"),tid)){
			//log event
			String event = Utilities.getFileShortName(virtualIns.getFileLocation()).replace(':', '@')+":S-"+synctype+"_"+child+"-"+tid;
			logSymbEvent(ti.getName(), event);		

			if(DEBUG) 
				System.out.println("["+getStatePathId(tid,String.valueOf(line))+"] Log event "+event);
		//}
	}


	protected void logLockSyncEvent(Instruction virtualIns, int line, ThreadInfo ti, String synctype, String lockobj)
	{
		String tid = ti.getName();
		String file = Utilities.getFileShortName(virtualIns.getFileLocation());
		if(threadsFinished.contains(tid)
				|| file.startsWith("ReentrantLock.java"))
			return;

		String symbvar = "S-"+synctype+"_"+lockobj+"-"+tid;

		try{
			if(stateOkToLog.get(getCurStateId(tid))){
				//log event
				String event = file.replace(':', '@')+":"+symbvar;
				logSymbEvent(ti.getName(), event);	

				if(DEBUG) 
					System.out.println("["+getStatePathId(tid,String.valueOf(line))+"] Log event "+event);
			}
		}catch(NullPointerException e){
			stateOkToLog.put(getCurStateId(tid), true);
			
			String event = file.replace(':', '@')+":"+symbvar;
			logSymbEvent(ti.getName(), event);	

			if(DEBUG) 
				System.out.println("["+getStatePathId(tid,String.valueOf(line))+"] Log event "+event);
		}
	}

	protected void logBranch(String tid)
	{
		if(threadsFinished.contains(tid))
			return;

		String event = "branch-"+tid;
		getCurStateInfo(tid).symbTrace += event+"\n";

	}

	protected void logSymbEvent(String tid, String event)
	{
		if(threadsFinished.contains(tid))
			return;
		
		getCurStateInfo(tid).symbTrace += event+"\n";
	}

	/**
	 * Guarantees that every thread's trace to be stored is correct, i.e. begins with a "start" event 
	 * @param tid
	 * @param curState
	 * @return
	 */
	public boolean isCorrectTrace(String tid, StateInfo curState){
		
		if(assertThread.equals(tid) && canRunFree(tid) && !curState.hitAssert)
		{
			System.out.println(" -- Don't store this log! Thread hasn't hit the assertion.");
			return false;
		}
		
		if(curState.symbTrace.isEmpty()) //the log is empty
			return true;
		
		int end = curState.symbTrace.indexOf("\n");
		String firstLine = curState.symbTrace.substring(0, end);
		if(firstLine.contains("<assertThread_")) //for the trace of the assertion thread, the start event is in the second line
		{
			firstLine = curState.symbTrace.substring(end+1, curState.symbTrace.indexOf("\n", end+1));
		}
		if(tid.equals("0") || (!tid.equals("0") && firstLine.contains("start"))
				|| firstLine.contains("R-val$pool_704-1-1") //for pool107 -> ugly!!! FIX THIS IN JPF
				){
			System.out.println("LOG OK: first line: "+firstLine);
			return true;
		}
		else{
			System.out.println("LOG NOT OK: first line: "+firstLine);
			System.out.println(curState.symbTrace);
		}
		
		return false;
	}
		
	
	private String getStatePathId(String tid, String file)
	{		
		PCChoiceGenerator pccg = pointerToVM.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
		if (pccg != null && pccg.getThreadInfo().getName()==tid){
			return (tid+"_"+getPathId(tid)+":"+pointerToSS.getId()+"@"+file);
		}
		else
			return (tid+"_-1"+":"+pointerToSS.getId()+"@"+file);
	}

	public String getPathId(String tid){
		return getCurStateInfo(tid).pathid;
	}

	/*
	 * Updates the path id of a state.
	 */
	public void updatePathId(String tid, int decision){
		StateInfo curState = getCurStateInfo(tid);
		String oldpath = curState.pathid;
		curState.pathid += decision;

		try{
			//copy the path conditions from the previous path id to the new one
			ArrayList<String> pathT = new ArrayList<String>();
			pathT.addAll(pathPerThread.get(tid+"_"+oldpath));
			pathPerThread.put(tid+"_"+curState.pathid, pathT);
		}
		catch(NullPointerException e){
			ArrayList<String> pathT = new ArrayList<String>();
			pathPerThread.put(tid+"_"+curState.pathid, pathT);
		}
			
	}

	public String getCurStateId(String tid){
		String ret = tid+"_"+pointerToSS.getId();
		return ret;
	}
	
	public String getPrevStateId(String tid){
		return prevState.get(tid);
	}
	
	public StateInfo getCurStateInfo(String tid){
		try{
			return mapStateInfo.get(getCurStateId(tid)).get(0);	
		}
		catch(NullPointerException e){
			StateInfo s = new StateInfo();
			s.tid = tid;
			s.jpfid = pointerToSS.getId();
			ArrayList<StateInfo> l = new ArrayList<StateInfo>();
			l.add(s);
			mapStateInfo.put(getCurStateId(tid), l);
			return s;
		}
	}

	
	/**
	 * Copies the information (i.e. path id, symbolic trace, bbs reached) from the parent state to the current state.
	 * @param tid
	 */
	public void copyParentStateInfo(String tid){
		
		try{
			String parKey = getPrevStateId(tid);
			String curKey = getCurStateId(tid);
			stateTree.put(curKey, parKey);
			
			//copy parent state to the new state
			StateInfo parState = mapStateInfo.get(parKey).get(0);
			ArrayList<StateInfo> l = new ArrayList<StateInfo>();
			l.add(new StateInfo(pointerToSS.getId(), parState));
			mapStateInfo.put(curKey, l); 	
			System.out.println(" -- "+getCurStateInfo(tid).toString());
			
			threadsStarted.add(curKey); //to avoid tracing the start event more than once
		}
		catch(NullPointerException e){  //there is no parent state
			StateInfo s = new StateInfo();
			s.tid = tid;
			s.jpfid = pointerToSS.getId();
			ArrayList<StateInfo> l = new ArrayList<StateInfo>();
			l.add(s);
			mapStateInfo.put(getCurStateId(tid), l);
			System.out.println(" -- (no parent state) "+getCurStateInfo(tid).toString());
		}
	}
	
	/**
	 * Backtracks the state, which corresponds to copying the information (i.e. path id, symbolic trace, bbs reached) 
	 * from the parent state to the current state. This method differs from "copyParentStateInfo" because it does not
	 * update stateTree.
	 * @param tid
	 */
	public void backtrackStateInfo(String tid){
		
		String curKey = getCurStateId(tid);
		StateInfo curState = getCurStateInfo(tid);

		//make sure that we backtrack to the correct previous state
		int i = 0;
		if(mapStateInfo.get(curKey).size() > 1){  //if the current state has crossed any branches, backtrack to the original state
			i = 1;
		}
		StateInfo prevState = mapStateInfo.get(curKey).get(i);
		//System.out.println("BACKTRACK to previous["+i+"]: "+prevState.toString());
		
		//if prevState and curState have the same number of BBs reached, but curState has a longer 
		//path, then we have to subtract one to the number of BBs reached to ensure consistency
		if(prevState.bbsReached == curState.bbsReached
				&& prevState.pathid.length() < curState.pathid.length())
		{
			prevState.bbsReached--;
			System.out.println(" -- decrement number of BBs reached to "+prevState.bbsReached+"(curPath: "+curState.pathid+" > prevPath: "+prevState.pathid+")");
		}
		
		//when we reach an assertion and backtrack to prevState, we know that we are 
		//going to follow the branch that validates the assertion (so we set hitAssert to true)
		if(curState.hitAssert)
			prevState.hitAssert = true;
		
		mapStateInfo.get(curKey).set(0, new StateInfo(pointerToSS.getId(), prevState));
		System.out.println(" -- "+getCurStateInfo(tid).toString()+"  mapStateInfo["+curKey+"].size = "+mapStateInfo.get(curKey).size());
	}
	
	public String getPathConditionConstraint(String tid, JVM vm){
		String constraint = ""; 
		ChoiceGenerator <?> cg = vm.getChoiceGenerator();
		
		if (!(cg instanceof PCChoiceGenerator) 
				|| !cg.getThreadInfo().getName().equals(tid)){
			ChoiceGenerator <?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator)) && !prev_cg.getThreadInfo().getName().equals(tid)) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}
		
		if (cg instanceof PCChoiceGenerator && ((PCChoiceGenerator) cg).getCurrentPC() != null && cg.getThreadInfo().getName().equals(tid))
		{
			PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
			
			if(pc.stringPC().contains(" &&"))
				constraint = pc.stringPC().substring(pc.stringPC().indexOf("\n")+1,pc.stringPC().indexOf(" &&"));
			else
				constraint = pc.stringPC().substring(pc.stringPC().indexOf("\n")+1);
		}
		
		return constraint;
	}
}
