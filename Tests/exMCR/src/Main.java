package exMCR;

import java.util.concurrent.locks.ReentrantLock;

public class Main {
	public static ReentrantLock l;
	public static int x, y;
	public static boolean bug;
	
	public static void main(String args[]) {
		l = new ReentrantLock();
		x = 0;
		y = 0;
		bug = false;
		
		T1 t1 = new T1();
		T2 t2 = new T2();
		T3 t3 = new T3();
		
		t1.start();
		t2.start();
		t3.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assert(!bug);
		System.out.println("[OK]");
		
	}
}

