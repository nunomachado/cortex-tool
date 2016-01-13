package exMCR;

public class T3 extends Thread {
	
	public void run()
	{
		for(int i = 0; i < 2;i++){
			if(Main.x>1)
			{
				System.out.println("[T3] x > 1");
				if(Main.y == 3){
					Main.bug = true;
					System.out.println("ERROR: y == 3");
				}
				else{
					System.out.println("[T3] y != 3");
					Main.y=2;
				}
			}
		}
	}

}
