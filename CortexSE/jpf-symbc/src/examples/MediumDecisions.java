
import gov.nasa.jpf.symbc.Debug;

public class MediumDecisions {
	
	public int test(int x) {
		if (x>5){
			if (x>7){
				if (x>9){
					return 1;
				}else{
					return 2;
				}
			}else{
				return 3; 
			}
		}else{
			if (x<3){
				return 4;
			}else{
				return 5;
			}
		}
	}
	
	// The test driver
	public static void main(String[] args) {
		MediumDecisions testinst = new MediumDecisions();
		int x = testinst.test(1);
		System.out.println("symbolic value of x: "+Debug.getSymbolicIntegerValue(x));
		//Debug.printPC("\n Path Condition: ");
	}
	
}
