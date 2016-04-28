package exMCR;

public class T1 extends Thread {
	
	public void run()
	{
		for(int i = 0; i < 2; i++){
			Main.l.lock();
			Main.x = 1;
			Main.y = 1;
			Main.l.unlock();
		}
	}

}
