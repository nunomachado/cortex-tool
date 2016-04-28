package pt.tecnico.jpf.symbiosis;

import java.util.HashMap;
import java.util.Map;

public class InstrumentationHandler {

	//** data structures for thread consistent identification
	public static HashMap<String,Integer> threadChildrenCounter;	//** allows to generate deterministic thread identifiers by counting the number of children threads spawned
	public static Map<Thread, String> MapBackupThreadName;					//** used to keep the thread name consistent during the execution (because the name can be reset by the target program after the thread initialization)


	public static void symbiosisBBEntry(long bbid)
	{
		//System.out.println("symbiosisBBEntry: "+Thread.currentThread().getName()+" -> "+bbid);
	}

	public static void mainThreadStartRun()
	{
		try{
			threadChildrenCounter = new HashMap<String, Integer>();
			MapBackupThreadName = new HashMap<Thread, String>();

			Thread.currentThread().setName("0");
			String mainthreadname = Thread.currentThread().getName();

			MapBackupThreadName.put(Thread.currentThread(),mainthreadname);	//** save the name for handling future setNames

			//**to generate deterministic thread identifiers
			threadChildrenCounter.put("0", 1);

		}catch(Exception e)
		{
			System.err.println("[SymbiosisJPF] "+e.getMessage());
			e.printStackTrace();
		}
	}

	public static void threadStartRunBefore(Thread t)
	{
		try{
			String parentId = Thread.currentThread().getName();  //** as the instrumented code to get the thread name is executed before changing the name, we need to do this
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
			System.err.println("[SymbiosisJPF] "+e.getMessage());
			e.printStackTrace();
		}
	}

	public static void threadStartRun()	
	{
		try{
			String threadId = Thread.currentThread().getName();
			threadChildrenCounter.put(threadId, 1);
			MapBackupThreadName.put(Thread.currentThread(),threadId);

			//System.out.println("[SymbiosisJPF] T"+threadId+" started running");
		}catch(Exception e)
		{
			System.err.println("[SymbiosisJPF] "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void assertHandler(int success) {}
	public static void assertProbe() {}

}
