
import gov.nasa.jpf.symbc.Debug;

public class SimpleDecisions {
	
	public int test(int x) {
		if (x>5){
			if (x>7){
				return 2;
			}else{
				return 1;
			}
		}else{
			return 0;
		}
	}
	
	// The test driver
	public static void main(String[] args) {
		SimpleDecisions testinst = new SimpleDecisions();
		int x = testinst.test(1);
		System.out.println("symbolic value of x: "+Debug.getSymbolicIntegerValue(x));
		//Debug.printPC("\n Path Condition: ");
	}
	
}
