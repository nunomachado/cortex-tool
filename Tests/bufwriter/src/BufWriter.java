package bufwriter;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author
 * @version 1.0
 */
import java.io.*;
import java.util.*;

public class BufWriter extends Thread {
	public static int res;
	public static void main (String[] args)
	{
		//long start, end;
		//start = System.nanoTime(); //start timestamp
		
		Buffer buf = new Buffer(1000);
		//File outFile;
		Thread[] wr;
		int threadNum = 5;
		Checker checker = new Checker (buf);
		Thread tCheck = new Thread(checker);
		Random rGen = new Random();
		double noSyncProbability = 0.1;

		//Output file creation
		/*if (args.length > 0)
      outFile = new File(args[0]);
    else return;*/

		//Optional concurrency parameter
		if (args.length > 1)
		{
			if (args[1].equalsIgnoreCase("little")) threadNum = 3;
			if (args[1].equalsIgnoreCase("average")) threadNum = 5;
			if (args[1].equalsIgnoreCase("lot")) threadNum = 10;
		}

		//Thread array creation
		wr = new Thread [threadNum];

		//Threads creation
		for (int i=0;i<threadNum;i++)
		{
			if (i%2==0)
				wr[i] = new Thread(new SyncWriter(buf,i+1));
			else
				wr[i] = new Thread(new Writer(buf,i+1));
		}

		// Checker thread starting
		tCheck.start();

		//Starting threads ...
		for (int i=0;i<threadNum;i++)
		{
			//System.out.println("Start "+(i+1));
			wr[i].start();     
		}

		try {
			//Stopping threads ...
			for (int i=0;i<threadNum;i++)
			{
				//System.out.println("Stop "+(i+1));
				wr[i].join();
			}
			//Stopping checker thread
			// System.out.println("Stop main");
			checker.stop = true;//tCheck.stop(); //** stop is deprecated
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		//Outputting results ...
		try
		{
			//assert((buf._count - (checker.getWrittenCount()+buf._pos))==0);
			res = buf._count - (checker.getWrittenCount()+buf._pos);
			System.out.print("<BufWriter,"+res+",");
			//outStream.writeChars("<BufWriter,");
			//outStream.writeChars(res+",");
			assert(res == 0);
			if (res != 0)
			{
				System.out.println("[Wrong/No-Lock]>");
				throw new Exception();
			}
			else
			{
				System.out.println("[None]>");
			}
		}
		catch (IOException e) {}
		catch(Exception e)
		{
			System.out.println("Bug");
			"Crashed_with".equals(e);
		}
		/*end = System.nanoTime(); //** end timestamp
		double time = (((double)(end - start)/1000000000));
		System.out.println("\nEXECUTION TIME: "+time+"s");
		*/
		return;
	}
}