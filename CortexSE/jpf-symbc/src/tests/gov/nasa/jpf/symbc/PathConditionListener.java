package gov.nasa.jpf.symbc;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class PathConditionListener extends ListenerAdapter {

	public void instructionExecuted(JVM vm) {
		PathCondition pc = PathCondition.getPC(vm);
		if (pc != null) {
			System.out.println("Path Condition:\n" + pc);
		}
	}
}
