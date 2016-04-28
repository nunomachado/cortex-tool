package gov.nasa.jpf.symbc;

import gov.nasa.jpf.jvm.MJIEnv;
import gov.nasa.jpf.symbc.numeric.PathCondition;

public class JPF_gov_nasa_jpf_symbc_TestUtils {

	public static int getPathCondition____Ljava_lang_String_2(MJIEnv env, int objRef) {
		PathCondition pc = PathCondition.getPC(env);
		if (pc != null) {
			return env.newString(pc.stringPC());
		}
		return env.newString("");
	}
	
	public static int getSolvedPathCondition____Ljava_lang_String_2(MJIEnv env, int objRef) {
		PathCondition pc = PathCondition.getPC(env);
		if (pc != null) {
			pc.solve();
			return env.newString(pc.toString());
		}
		return env.newString("");
	}
}
