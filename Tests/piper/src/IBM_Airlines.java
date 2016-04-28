package Piper;
//**********************************************************************************************
//********** Software Testing Project ****************************************************
//*** by: Alon Alapi 032091290 & Erez Engel 027338821 *************************
//**********************************************************************************************
import java.io.*;

//**********************************************************************************************
//******************* class IBM_Airlines ***************************************************
public class IBM_Airlines{

	static final int NUM_OF_SEATS = 2;                                                         // number of seats on the plane
	private static int _numberOfThreads;                                                    // num of threads from input
	private static String _fileName;
	//public static long start, end;
	//******************** main *******************************************************************
	public static void main(String[] args) {

		//start = System.nanoTime(); //start timestamp
		IBM_Airlines airline = new  IBM_Airlines();
		try{
			_fileName =  airline.inputManager(args);    // handle input
			//** CHANGE { add thread arrays
			Thread[] producers = new Thread[_numberOfThreads];
			Thread[] consumers = new Thread[_numberOfThreads];
			//** CHANGE }

			Piper p = new Piper(_fileName,_numberOfThreads * NUM_OF_SEATS);

			// new Thread(new Consumer(p,_numberOfThreads)).start();               // Create Consumer thread

			//  for( int i = 1; i <= 4; i++){                                                                         // Create all Consumer threads
			//   new Thread(new Consumer(p,_numberOfThreads)).start();
			//   }

			for( int i = 1; i <= _numberOfThreads; i++){                                         // Create all Producer threads
				consumers[i-1] = new Thread(new Consumer(p,_numberOfThreads));
				producers[i-1] = new Thread(new Producer(p,"passenger"+i));
				consumers[i-1].start();
				producers[i-1].start();
			}

			for( int i = 0; i < _numberOfThreads; i++){                                         // Create all Producer threads
				consumers[i].join();
				producers[i].join();
			}

			DataOutputStream output = new DataOutputStream(new FileOutputStream(new File(_fileName)));
			//System.out.println("exit");
			//System.exit(1);


		} catch ( NumberFormatException e ){
			System.out.println("illegal input: input need to be in numeric format");
			System.exit(1);
		} catch ( ioException e ){                                                                         // input exc. we created
			System.out.println(e.getMessage());
			System.exit(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	//**************** function inputManager *************************************************
	private String inputManager(String args[]) throws NumberFormatException,ioException {

		if( args.length != 2 ) throw new ioException ("illegal input: please try again");

		_fileName =  args[0];

		_numberOfThreads = Integer.valueOf(args[1]).intValue();                 // number of threads in the program
		System.out.println("#Threads: "+_numberOfThreads);
		
		if(_numberOfThreads < 1) throw new ioException
		("illegal input: need at least one thread");

		return _fileName;
	}

	//************************************** class ioException *********************************
	public class ioException extends Exception{                                            // our exception

		ioException(String s){super(s);}                                                              // use super class C'tor
	}


	//**********************************************************************************************
	//********************* class Producer *****************************************************
	static class Producer implements Runnable {

		private Piper _piper;
		private String _name;

		//******************** C'tor *******************************************************************
		public Producer( Piper p, String n) {

			_piper = p;
			_name = n;
		}

		//**************** function run **************************************************************
		public void run() {

			try {
				for ( int i = 0; i < NUM_OF_SEATS; i++)                                         // fill the piper with passengers
					_piper.fillPlane(_name);

			} catch ( InterruptedException i)  { System.err.println(i); }
		}
	}

	//**********************************************************************************************
	//************** class Consumer **********************************************************
	static class Consumer implements Runnable {

		private Piper _Piper;
		private int _numberOfThreads;

		//******************** C'tor *******************************************************************
		public Consumer ( Piper p, int numberOfThreads) {

			_numberOfThreads = numberOfThreads;
			_Piper = p;
		}

		//**************** function run **************************************************************
		public void run() {

			try {
				for ( int i = 0; i < NUM_OF_SEATS ; i++)    // empty the plane
					_Piper.emptyPlane();

			} catch ( InterruptedException i) { System.err.println(i); }
		}
	}

}
