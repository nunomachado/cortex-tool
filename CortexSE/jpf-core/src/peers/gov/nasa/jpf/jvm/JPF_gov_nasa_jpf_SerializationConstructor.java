package gov.nasa.jpf.jvm;


public class JPF_gov_nasa_jpf_SerializationConstructor {

  /**
   * create a new instance, but only call the ctor of the first
   * non-serializable superclass
   */
  public static int newInstance___3Ljava_lang_Object_2__Ljava_lang_Object_2 (MJIEnv env, int mthRef,
                                                                             int argsRef) {
    ThreadInfo ti = env.getThreadInfo();
    StackFrame frame = ti.getReturnedDirectCall();

    if (frame != null){
      return frame.pop();

    } else {
      int clsRef = env.getReferenceField(mthRef, "mdc");
      ClassInfo ci = env.getReferredClassInfo( clsRef);

      int superCtorRef = env.getReferenceField(mthRef, "firstNonSerializableCtor");
      MethodInfo mi = JPF_java_lang_reflect_Constructor.getMethodInfo(env,superCtorRef);

      if (ci.isAbstract()){
        env.throwException("java.lang.InstantiationException");
        return MJIEnv.NULL;
      }

      int objRef = env.newObject(ci);
      MethodInfo stub = mi.createDirectCallStub("[serialization]");
      frame = new DirectCallStackFrame(stub, 2,0);
      frame.push(objRef, true);
      frame.dup(); // we store the return object on the frame (don't do that with a normal frame)
      ti.pushFrame(frame);

      //env.repeatInvocation(); // we don't need this, direct calls don't advance their return frame
      return MJIEnv.NULL; // doesn't matter
    }
  }

}
