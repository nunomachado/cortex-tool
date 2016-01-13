package gov.nasa.jpf.symbc;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.bytecode.Instruction;

public class LineNumberListener extends ListenerAdapter {
	String lastLoc = "";

	public void executeInstruction(JVM vm) {
		Instruction instr = vm.getNextInstruction();
		if (instr != null) {
			String loc = instr.getFileLocation();
//			String clazz = instr.getMethodInfo().getClassName();
//			System.out.println(loc);
			if (loc != null && ! loc.startsWith("java")) {
				if (lastLoc.equals(loc)) {
				} else {
					System.out.println(loc);
					lastLoc = loc;
					// System.out.println(clazz + ": " + instr.getMnemonic().toUpperCase());
				}
				System.out.println("      " + instr.getMnemonic().toUpperCase());
			}
		}
	}
}
