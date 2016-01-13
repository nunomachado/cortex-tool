package jdk_StringBuffer;
import jdk_Varios.StringBuffer;

public class StringBufferTest {
	public static StringBuffer buf;

	public static void main(String[] args) throws Exception {

		try{
			buf = new StringBuffer(100);
			buf.insert(0,"abcefghijk");

			Thread t1 = new Thread(new Runnable(){
				public void run(){

					for (int i = 0; i < 10; i++) {
						StringBuffer sb = new StringBuffer(100);
						sb.append(buf);
					}
				}
			});

			Thread t2 = new Thread(new Runnable() {
				public void run(){
					for (int i = 0; i < 10; i++) {
						buf.delete(0, i);
						buf.append("abcefghijk");
					}
				}
			});

			t1.start();
			t2.start();
			t1.join();
			t2.join();
			System.out.println("end");
		}
		catch(Exception e)
		{
			System.out.println("Bug");
		}
	}

}
