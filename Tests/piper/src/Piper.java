package Piper;
//**********************************************************************************************
//********** Software Testing Project ****************************************************
//*** by: Alon Alapi 032091290 & Erez Engel 027338821 *************************
//***********************************************************************************************
import java.io.*;

//**********************************************************************************************
//******************* class Piper ************************************************************
public class  Piper {

	static final int NUM_OF_SEATS = 2;                                                         // so capacity = number_of_seats -1
	private int _first, _last;
	private String[] _passengers;
	private String _fileName;
	private FileWriter _fw;
	private PrintWriter _pw;
	static int _fillCount;
	static int  _emptyCount;
	//**************** C'tor ***********************************************************************
	public Piper( String fileName, int fillCount ) {

		_first = 0;
		_last = 0;
		_passengers = new String[NUM_OF_SEATS];
		_fileName = fileName;
		_fillCount = fillCount;
		_emptyCount = fillCount;
		System.out.println("fill count:" +_fillCount);
		setFile();
	}

	//**************** setFile ********************************************************************
	public void setFile() {

		try  {
			_fw = new FileWriter (_fileName);
			_pw = new PrintWriter (_fw);
		} catch (IOException e)  { e.printStackTrace(); }

		_pw.println("Program name: Piper.java");
		_pw.print("Results: ");
		//System.out.println("Program name: Piper.java");
		System.out.print("Results: ");

	}

	//**************** function fillPlane ********************************************************
	public synchronized void fillPlane( String name) throws InterruptedException {

		if (( _last + 1) % NUM_OF_SEATS == _first)                                         // BUG - should be while, not if !!!
		{
			synchronized (this) {
				wait();
			}

		}

		_passengers[_last] = name;                                                                // load passenger to plane

		//_pw.println("Load "+_passengers[_last]+ " to plane");                      // write to file
		synchronized(this)
		{   
			_fillCount--; 
			if(_fillCount == 0) 
				closeFile();   
		}                         //close the file, no more passengers
		//System.out.println("Load "+_passengers[_last]+ " to plane");

		_last = (_last + 1) % NUM_OF_SEATS;
		synchronized (this) {
			notifyAll();
		}
	}

	//**************** function emptyPlane ***************************************************
	public synchronized String emptyPlane() throws InterruptedException {

		while ( _first == _last)
		{
			synchronized (this) {
				wait();
			}
		}


		String  name = _passengers[_first];                                                     // get passenger off the plane
		synchronized(this)
		{
			this._emptyCount--; 
		}

		//_pw.println("Empty the plane");
		//System.out.println("Empty the plane");

		_first = ( _first + 1) % NUM_OF_SEATS;
		synchronized (this) {
			notifyAll();
		}

		return name;
	}

	//**************** closeFile ********************************************************************
	public void closeFile() {

		//_pw.println("Empty the plane");
		//System.out.println("Empty the plane");
		try{
			assert(_emptyCount < NUM_OF_SEATS);
			if ( _emptyCount >= NUM_OF_SEATS )
			{
				// _pw.println("The bug was found");
				System.out.println("Bug");
				// _pw.println("Bug-pattern indicate: Denial bug tales [Condition-For-Wait]");
				_pw.close();
				throw new Exception();
			}
			else   
			{
				//_pw.println("The bug was not found");
				System.out.println("None");
				/*IBM_Airlines.end = System.nanoTime(); //** end timestamp
				double time = (((double)(IBM_Airlines.end - IBM_Airlines.start)/1000000000));
				System.out.println("\nEXECUTION TIME: "+time+"s");*/
			}

			//System.out.println("Bug-pattern indicate: Denial bug tales [Condition-For-Wait]");
			//  _pw.println("Bug-pattern indicate: Denial bug tales [Condition-For-Wait]");
			_pw.close();
		}
		catch(Exception e)
		{
			"Crashed_with".equals(e);
			/*IBM_Airlines.end = System.nanoTime(); //** end timestamp
			double time = (((double)(IBM_Airlines.end - IBM_Airlines.start)/1000000000));
			System.out.println("\nEXECUTION TIME: "+time+"s");*/
			System.exit(-1);
		}
	}
}
