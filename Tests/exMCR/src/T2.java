package exMCR;

public class T2 extends Thread {
	
	public void run()
	{
		for(int i = 0; i < 2;i++){
			Main.l.lock();
			Main.x = 0;
			Main.l.unlock();
			if(Main.x>0){
				System.out.println("[T2] x > 0");
				Main.y++;
				Main.x=2;
			}
		}
	}

}
