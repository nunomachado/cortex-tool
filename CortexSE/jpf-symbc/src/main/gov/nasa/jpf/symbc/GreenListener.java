package gov.nasa.jpf.symbc;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;

public class GreenListener extends ListenerAdapter {

	public GreenListener() { }

	@Override
	public void searchFinished(Search s) {
		SymbolicInstructionFactory.solver.shutdown();
		for (String str : SymbolicInstructionFactory.solver.getSolverReport()) {
			System.out.println(str);
		}
	}

}
