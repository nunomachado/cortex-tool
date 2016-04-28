//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.symbc;

import za.ac.sun.cs.solver.Solver;
import za.ac.sun.cs.solver.canonization.DefaultCanonizer;
import za.ac.sun.cs.solver.csp.CVC3;
import za.ac.sun.cs.solver.csp.Choco;
import za.ac.sun.cs.solver.csp.LattE;
import za.ac.sun.cs.solver.slicing.DefaultSlicer;
import za.ac.sun.cs.solver.store.RedisLevelStore;
import za.ac.sun.cs.solver.store.RedisStore;
import za.ac.sun.cs.solver.store.Store;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.symbc.bytecode.*;
import gov.nasa.jpf.symbc.numeric.MinMax;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemChoco;
import gov.nasa.jpf.symbc.numeric.solvers.ProblemCoral;
import gov.nasa.jpf.util.InstructionFactoryFilter;


public class SymbolicInstructionFactory extends gov.nasa.jpf.jvm.bytecode.InstructionFactory {

	  public Instruction aload(int localVarIndex) {
	    return filter.isInstrumentedClass(ci) ? new ALOAD(localVarIndex) : super.aload(localVarIndex);
	  }

	  public Instruction aload_0() {
	    return (filter.isInstrumentedClass(ci) ? new ALOAD(0): super.aload_0());
	  }


	  public Instruction aload_1() {
	    return (filter.isInstrumentedClass(ci) ? new ALOAD(1): super.aload_1());
	  }


	  public Instruction aload_2() {
	    return (filter.isInstrumentedClass(ci) ? new ALOAD(2): super.aload_2());
	  }


	  public Instruction aload_3() {
	    return (filter.isInstrumentedClass(ci) ? new ALOAD(3): super.aload_3());
	  }


	  public Instruction iadd() {
	    return (filter.isInstrumentedClass(ci) ? new IADD(): super.iadd());
	  }


	  public Instruction iand() {
	    return (filter.isInstrumentedClass(ci) ? new IAND(): super.iand()) ;
	  }


	  public Instruction iinc(int localVarIndex, int incConstant) {
		    return (filter.isInstrumentedClass(ci) ? new IINC(localVarIndex, incConstant) :super.iinc(localVarIndex, incConstant));
	  }


	  public Instruction isub() {
	    return (filter.isInstrumentedClass(ci) ? new ISUB() : super.isub());
	  }


	  public Instruction imul() {
	    return (filter.isInstrumentedClass(ci) ? new IMUL() : super.imul());
	  }


	  public Instruction ineg() {
	    return (filter.isInstrumentedClass(ci) ? new INEG() : super.ineg());
	  }


	  public Instruction ifle(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFLE(targetPc) : super.ifle(targetPc));
	  }


	  public Instruction iflt(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFLT(targetPc) : super.iflt(targetPc));
	  }


	  public Instruction ifge(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFGE(targetPc): super.ifge(targetPc));
	  }


	  public Instruction ifgt(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFGT(targetPc): super.ifgt(targetPc));
	  }


	  public Instruction ifeq(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFEQ(targetPc): super.ifeq(targetPc));
	  }


	  public Instruction ifne(int targetPc) {
	    return (filter.isInstrumentedClass(ci) ? new IFNE(targetPc): super.ifne(targetPc));
	  }


	  public Instruction invokestatic(String clsName, String methodName, String methodSignature) {
		  /*if (methodName.equals("exit")){
			  System.out.println(clsName+" - "+methodName+" - "+methodSignature+" - "+ filter.isInstrumentedClass(ci));
		  }*/
	    return (filter.isInstrumentedClass(ci) ? new INVOKESTATIC(clsName, methodName, methodSignature): super.invokestatic(clsName, methodName, methodSignature))
	    		;
	  }


	  public Instruction invokevirtual(String clsName, String methodName, String methodSignature) {
		    return (filter.isInstrumentedClass(ci) ? new INVOKEVIRTUAL(clsName, methodName, methodSignature): super.invokevirtual(clsName, methodName, methodSignature));
	  }

	  public Instruction invokeinterface(String clsName, String methodName, String methodSignature) {
		    return (filter.isInstrumentedClass(ci) ? new INVOKEINTERFACE(clsName, methodName, methodSignature): super.invokeinterface(clsName, methodName, methodSignature));
	  }

	  public Instruction invokespecial(String clsName, String methodName, String methodSignature) {
		  
		    return (filter.isInstrumentedClass(ci) ? new INVOKESPECIAL(clsName, methodName, methodSignature): super.invokespecial(clsName, methodName, methodSignature));
	  }


	  public Instruction if_icmpge(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPGE(targetPc): super.if_icmpge(targetPc));
	  }


	  public Instruction if_icmpgt(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPGT(targetPc): super.if_icmpgt(targetPc));
	  }


	  public Instruction if_icmple(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPLE(targetPc): super.if_icmple(targetPc));
	  }


	  public Instruction if_icmplt(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPLT(targetPc): super.if_icmplt(targetPc));
	  }


	  public Instruction idiv() {
	    return (filter.isInstrumentedClass(ci) ? new IDIV(): super.idiv());
	  }


	  public Instruction ishl() {
	    return (filter.isInstrumentedClass(ci) ? new ISHL(): super.ishl());
	  }


	  public Instruction ishr() {
	    return (filter.isInstrumentedClass(ci) ? new ISHR(): super.ishr());
	  }


	  public Instruction iushr() {
	    return (filter.isInstrumentedClass(ci) ? new IUSHR(): super.iushr());
	  }


	  public Instruction ixor() {
	    return (filter.isInstrumentedClass(ci) ? new IXOR(): super.ixor());
	  }


	  public Instruction ior() {
	    return (filter.isInstrumentedClass(ci) ? new IOR(): super.ior());
	  }


	  public Instruction irem() {
	    return (filter.isInstrumentedClass(ci) ? new IREM(): super.irem());
	  }


	  public Instruction if_icmpeq(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPEQ(targetPc): super.if_icmpeq(targetPc));
	  }


	  public Instruction if_icmpne(int targetPc) {
		    return (filter.isInstrumentedClass(ci) ? new IF_ICMPNE(targetPc): super.if_icmpne(targetPc));
	  }



	  public Instruction fadd() {
	    return (filter.isInstrumentedClass(ci) ? new FADD(): super.fadd());
	  }


	  public Instruction fdiv() {
	    return (filter.isInstrumentedClass(ci) ? new FDIV(): super.fdiv());
	  }


	  public Instruction fmul() {
	    return (filter.isInstrumentedClass(ci) ? new FMUL(): super.fmul());
	  }


	  public Instruction fneg() {
	    return (filter.isInstrumentedClass(ci) ? new FNEG(): super.fneg());
	  }


	  public Instruction frem() {
	    return (filter.isInstrumentedClass(ci) ? new FREM(): super.frem());
	  }


	  public Instruction fsub() {
	    return (filter.isInstrumentedClass(ci) ? new FSUB(): super.fsub());
	  }


	  public Instruction fcmpg() {
	    return (filter.isInstrumentedClass(ci) ? new FCMPG(): super.fcmpg())
	    		;
	  }


	  public Instruction fcmpl() {
		    return (filter.isInstrumentedClass(ci) ? new FCMPL(): super.fcmpl());
		  }


	  public Instruction dadd() {
		    return (filter.isInstrumentedClass(ci) ? new DADD(): super.dadd());
		  }


	  public Instruction dcmpg() {
		    return (filter.isInstrumentedClass(ci) ? new DCMPG(): super.dcmpg());
		  }


	  public Instruction dcmpl() {
		    return (filter.isInstrumentedClass(ci) ? new DCMPL(): super.dcmpl());
		  }


	  public Instruction ddiv() {
		    return (filter.isInstrumentedClass(ci) ? new DDIV(): super.ddiv());
		  }


	  public Instruction dmul() {
		    return (filter.isInstrumentedClass(ci) ? new DMUL(): super.dmul());
		  }


	  public Instruction dneg() {
		    return (filter.isInstrumentedClass(ci) ? new DNEG(): super.dneg());
		  }


	  public Instruction drem() {
		    return (filter.isInstrumentedClass(ci) ? new DREM(): super.drem());
		  }


	  public Instruction dsub() {
		    return (filter.isInstrumentedClass(ci) ? new DSUB(): super.dsub());
		  }


	  public Instruction ladd() {
		    return (filter.isInstrumentedClass(ci) ? new LADD(): super.ladd());
		  }


	  public Instruction land() {
		    return  (filter.isInstrumentedClass(ci) ? new LAND(): super.land());
		  }


	  public Instruction lcmp() {
		    return (filter.isInstrumentedClass(ci) ? new LCMP(): super.lcmp());
		  }


	  public Instruction ldiv() {
		    return (filter.isInstrumentedClass(ci) ? new LDIV(): super.ldiv());
		  }


	  public Instruction lmul() {
		    return (filter.isInstrumentedClass(ci) ? new LMUL(): super.lmul());
		  }


	  public Instruction lneg() {
		    return (filter.isInstrumentedClass(ci) ? new LNEG(): super.lneg());
		  }


	  public Instruction lor() {
		  return (filter.isInstrumentedClass(ci) ? new LOR(): super.lor());
		  }


	  public Instruction lrem() {
		  return (filter.isInstrumentedClass(ci) ? new LREM(): super.lrem());
		  }


	  public Instruction lshl() {
		  return (filter.isInstrumentedClass(ci) ? new LSHL(): super.lshl());
		  }


	  public Instruction lshr() {
		  return (filter.isInstrumentedClass(ci) ? new LSHR(): super.lshr());
		  }


	  public Instruction lsub() {
		  return (filter.isInstrumentedClass(ci) ? new LSUB(): super.lsub());
		  }


	  public Instruction lushr() {
		  return (filter.isInstrumentedClass(ci) ? new LUSHR(): super.lushr());
		  }


	  public Instruction lxor() {
		  return (filter.isInstrumentedClass(ci) ? new LXOR(): super.lxor());
		  }


	  public Instruction i2d() {
		  return (filter.isInstrumentedClass(ci) ? new I2D(): super.i2d());
		  }


	  public Instruction d2i() {
		  return (filter.isInstrumentedClass(ci) ? new D2I(): super.d2i());
		  }


	  public Instruction d2l() {
		  return (filter.isInstrumentedClass(ci) ?  new D2L(): super.d2l());
		  }


	  public Instruction i2f() {
		  return (filter.isInstrumentedClass(ci) ?  new I2F(): super.i2f());
		  }


	  public Instruction l2d() {
		  return (filter.isInstrumentedClass(ci) ?  new L2D(): super.l2d());
		  }


	  public Instruction l2f() {
		  return (filter.isInstrumentedClass(ci) ?  new L2F(): super.l2f());
		  }


	  public Instruction f2l() {
		  return (filter.isInstrumentedClass(ci) ?  new F2L(): super.f2l());
		  }


	  public Instruction f2i() {
		  return (filter.isInstrumentedClass(ci) ?  new F2I(): super.f2i());
		  }


	  public Instruction lookupswitch(int defaultTargetPc, int nEntries) {
		  return (filter.isInstrumentedClass(ci) ?  new LOOKUPSWITCH(defaultTargetPc, nEntries): super.lookupswitch(defaultTargetPc, nEntries));
		  }


	  public Instruction tableswitch(int defaultTargetPc, int low, int high) {
		  return (filter.isInstrumentedClass(ci) ?  new TABLESWITCH(defaultTargetPc, low, high): super.tableswitch(defaultTargetPc, low, high));
		  }


	  public Instruction d2f() {
		  return (filter.isInstrumentedClass(ci) ?  new D2F(): super.d2f());
		  }


	  public Instruction f2d() {
		  return (filter.isInstrumentedClass(ci) ?  new F2D(): super.f2d());
		  }


	  public Instruction i2b() {
		  return (filter.isInstrumentedClass(ci) ?  new I2B(): super.i2b());
		  }


	  public Instruction i2c() {
		  return  (filter.isInstrumentedClass(ci) ?  new I2C(): super.i2c());
		  }


	  public Instruction i2s() {
		  return (filter.isInstrumentedClass(ci) ?  new I2S(): super.i2s());
		  }


	  public Instruction i2l() {
		  return  (filter.isInstrumentedClass(ci) ?  new I2L(): super.i2l());
		  }


	  public Instruction l2i() {
		  return  (filter.isInstrumentedClass(ci) ?  new L2I(): super.l2i());
		  }


	  public Instruction getfield(String fieldName, String clsName, String fieldDescriptor){
		  return (filter.isInstrumentedClass(ci) ?  new GETFIELD(fieldName, clsName, fieldDescriptor): super.getfield(fieldName, clsName, fieldDescriptor));
		  }

	  public Instruction getstatic(String fieldName, String clsName, String fieldDescriptor){
		  return (filter.isInstrumentedClass(ci) ?  new GETSTATIC(fieldName, clsName, fieldDescriptor): super.getstatic(fieldName, clsName, fieldDescriptor));
		  }

		//TODO: to review
        //From Fujitsu:


	  public Instruction new_(String clsName) {
		  return  (filter.isInstrumentedClass(ci) ?  new NEW(clsName): super.new_(clsName));
		  }

	  public Instruction ifnonnull(int targetPc) {
		  return  (filter.isInstrumentedClass(ci) ?  new IFNONNULL(targetPc): super.ifnonnull(targetPc));
		  }

	  public Instruction ifnull(int targetPc) {
		  return  (filter.isInstrumentedClass(ci) ?  new IFNULL(targetPc): super.ifnull(targetPc));
		  }
	  
	  public Instruction newarray(int typeCode) {
		  return (filter.isInstrumentedClass(ci) ? new NEWARRAY(typeCode) : super.newarray(typeCode));
	      }
	  
	  public Instruction multianewarray(String clsName, int dimensions){
		  return (filter.isInstrumentedClass(ci) ? new MULTIANEWARRAY(clsName,dimensions) : super.multianewarray(clsName,dimensions));
	      }

	static public String[] dp;

	/* Symbolic String configuration */
	static public String[] string_dp;
	static public int stringTimeout;
	static public boolean preprocesOnly;

	/*
	 * This is intended to serve as a catchall debug flag.
	 * If there's some debug printing/outputing, conditionally print using
	 * this flag.
	 */
	static public boolean debugMode;

	/*
	 * Enable logging of info used to detect regressions 
	 */
	static public boolean regressMode;
	
	/*
	 * If Green is enabled this solver will be used
	 * Later we just check if this is null to know if Green is enabled
	 */
	static public Solver solver = null;
	
	/*
	 * Concolic mode where we concrete execute for now
	 * only Math operations
	 */

	static public boolean concolicMode;
	static public boolean heuristicRandomMode;
	static public boolean heuristicPartitionMode;
	static public int MaxTries = 1;

	ClassInfo ci;
	InstructionFactoryFilter filter;

	 @Override
	 public void setClassInfoContext(ClassInfo ci){
		    this.ci = ci;
	 }

	 private void setupGreen(Config conf) {
		//------------------------------------
			// Construct the solver
			//------------------------------------
			solver = new Solver();

			// Slicing
			String s = conf.getProperty("symbolic.solver.slicing");
			Boolean b = (s != null) && (s.equals("true") || s.equals("yes") || s.equals("on"));
			System.out.println("symbolic.solver.slicing=" + b);
			if (b) {
				solver.setSlicer(new DefaultSlicer());
			}

			// Canonization
			s = conf.getProperty("symbolic.solver.canonization");
			b = (s != null) && (s.equals("true") || s.equals("yes") || s.equals("on"));
			System.out.println("symbolic.solver.canonization=" + b);
			if (b) {
				solver.setCanonizer(new DefaultCanonizer());
			}
			Boolean canonization = b;

			// Decision procedure
			s = conf.getProperty("symbolic.solver.dp");
			if (s == null) {
				System.out.println("symbolic.solver.dp=<<NONE>>");
			} else if (s.equals("cvc3")) {
				CVC3 cvc3 = new CVC3();
				solver.setDecisionProcedure(cvc3);
				solver.setConstraintSolver(cvc3);
				System.out.println("symbolic.solver.dp=" + s);
			} else if (s.equals("choco")) {
				Choco choco = new Choco();
				solver.setDecisionProcedure(choco);
				solver.setConstraintSolver(choco);
				System.out.println("symbolic.solver.dp=" + s);
			} else {
				System.out.println("symbolic.solver.dp=" + s + " <--- UNSUPPORTED");
			}

			// Store
			Store store = null;
			s = conf.getProperty("symbolic.solver.store");
			if (s == null) {
				System.out.println("symbolic.solver.store=<<NONE>>");
			} else if (s.equals("redis")) {
				String h = conf.getProperty("symbolic.solver.store.redis.host", "localhost");
				int p = conf.getInt("symbolic.solver.store.redis.port", 6379);
				RedisStore redis = new RedisStore(h, p);
				solver.setStore(redis);
				String d = conf.getProperty("symbolic.solver.store.redis.delay");
				b = (d != null) && (d.equals("true") || d.equals("yes") || d.equals("on"));
				redis.setDelayedWrites(b);
				System.out.println("symbolic.solver.store=" + s);
				System.out.println("symbolic.solver.store.redis.host=" + h);
				System.out.println("symbolic.solver.store.redis.port=" + p);
				System.out.println("symbolic.solver.store.redis.delay=" + b);
				store = redis;
			} else if (s.equals("redislevel")) {
				String[] h = conf.getStringArray("symbolic.solver.store.redislevel.host", new String[] { "localhost" } );
				int p[] = conf.getIntArray("symbolic.solver.store.redislevel.port", new int[] { 6379 } );
				RedisLevelStore redisLevel = new RedisLevelStore(h, p);
				solver.setStore(redisLevel);
				System.out.println("symbolic.solver.store=" + s);
				int n = h.length;
				if (p.length > n) {
					n = p.length;
				}
				if (n == 0) {
					System.out.println("symbolic.solver.store.redislevel.host[0]=localhost");
					System.out.println("symbolic.solver.store.redislevel.port[0]=6379");
				} else {
					for (int i = 0; i < n; i++) {
						String hh = (i < h.length) ? h[i] : "localhost";
						int pp = (i < p.length) ? p[i] : 6379;
						System.out.println("symbolic.solver.store.redislevel.host[" + i + "]=" + hh);
						System.out.println("symbolic.solver.store.redislevel.port[" + i + "]=" + pp);
					}
				}
				store = redisLevel;
			} else {
				System.out.println("symbolic.solver.store=" + s + " <--- UNSUPPORTED");
			}
			dp = new String[] {"green"};
//			s = conf.getProperty("symbolic.pcstore.no");
//			dontStoreInDB = (s != null) && (s.equals("true") || s.equals("yes") || s.equals("on"));
//			System.out.println("symbolic.pcstore.no=" + dontStoreInDB);

			// Model counter
			s = conf.getProperty("symbolic.solver.mc");
			if (s == null) {
				System.out.println("symbolic.solver.mc=<<NONE>>");
			} else if (s.equals("latte")) {
				String x = conf.getProperty("symbolic.solver.mc.tool", "count");
				LattE latte = new LattE(store);
				latte.setLatteExecutable(x);
				solver.setModelCounter(latte);
				System.out.println("symbolic.solver.mc=" + s);
				System.out.println("symbolic.solver.mc.tool=" + x);
				assert canonization : ">>> LattE cannot operate without canonization! <<<";
			} else {
				System.out.println("symbolic.solver.mc=" + s + " <--- UNSUPPORTED");
			}
			
			// fix to make sure when Green is used there is no NPE when poking at dp[0] in some bytecodes
			dp = new String[] {"green"};
	 }
	 
	 public  SymbolicInstructionFactory (Config conf){

		System.out.println("Running Symbolic PathFinder ...");

		filter = new InstructionFactoryFilter(null, new String[] {/*"java.*",*/ "javax.*" },null, null);

		if (conf.getBoolean("symbolic.green", false)) {
			System.out.println("Using Green Framework...");
			setupGreen(conf);
		} else {
			dp = conf.getStringArray("symbolic.dp");
			if (dp == null) {
				dp = new String[1];
				dp[0] = "choco";
			}
			System.out.println("symbolic.dp="+dp[0]);

			stringTimeout = conf.getInt("symbolic.string_dp_timeout_ms");
			System.out.println("symbolic.string_dp_timeout_ms="+stringTimeout);

			string_dp = conf.getStringArray("symbolic.string_dp");
			if (string_dp == null) {
				string_dp = new String[1];
				string_dp[0] = "none";
			}
			System.out.println("symbolic.string_dp="+string_dp[0]);

			preprocesOnly = conf.getBoolean("symbolic.string_preprocess_only", false);
			String[] concolic  = conf.getStringArray("symbolic.concolic");
			if (concolic != null) {
				concolicMode = true;
				System.out.println("symbolic.concolic=true");
			} else {
				concolicMode = false;
			}

			String[] concolicMaxTries  = conf.getStringArray("symbolic.concolic.MAX_TRIES");
			if (concolicMaxTries != null) {
				MaxTries = Integer.parseInt(concolicMaxTries[0]);
				assert (MaxTries > 0);
				System.out.println("symbolic.concolic.MAX_TRIES=" + MaxTries);
			} else {
				MaxTries = 1;
			}

			String[] heuristicRandom  = conf.getStringArray("symbolic.heuristicRandom");
			if (heuristicRandom != null) {
				heuristicRandomMode = true;
				System.out.println("symbolic.heuristicRandom=true");
			} else {
				heuristicRandomMode = false;
			}

			String[] heuristicPartition  = conf.getStringArray("symbolic.heuristicPartition");
			if (heuristicPartition != null) {
				assert(! heuristicRandomMode);
				heuristicPartitionMode = true;
				System.out.println("symbolic.heuristicPartition=true");
			} else {
				heuristicPartitionMode = false;
			}

			if(dp[0].equalsIgnoreCase("choco") || dp[0].equalsIgnoreCase("debug") || dp[0].equalsIgnoreCase("compare") || dp == null) { // default is choco
			  ProblemChoco.timeBound = conf.getInt("symbolic.choco_time_bound", 30000);
			  System.out.println("symbolic.choco_time_bound="+ProblemChoco.timeBound);
			}
			//load CORAL's parameters 
			if (dp[0].equalsIgnoreCase("coral") || dp[0].equalsIgnoreCase("debug") || dp[0].equalsIgnoreCase("compare")) {
				ProblemCoral.configure(conf);
			}
	
		}
		
		String regress = conf.getProperty("symbolic.regression_output");
		if (regress != null && regress.equals("true")) {
			regressMode = true;
		} else {
			regressMode = false;
		}
		
		//Just checking if set, don't care about any values
		String[] dummy = conf.getStringArray("symbolic.debug");
		if (dummy != null) {
			debugMode = true;
		} else {
			debugMode = false;
		}

	
		MinMax.collectMinMaxInformation(conf);
		/* no longer required here, now read in MinMax, see line above
		 
		String[] intmin, intmax, realmin, realmax, dontcare;
		intmin = conf.getStringArray("symbolic.minint");
		intmax = conf.getStringArray("symbolic.maxint");
		realmin = conf.getStringArray("symbolic.minreal");
		realmax = conf.getStringArray("symbolic.maxreal");
		dontcare = conf.getStringArray("symbolic.undefined");

		if (intmin != null && intmin[0] != null)
			MinMax.MININT = new Integer(intmin[0]);
		if (intmax != null && intmax[0] != null)
			MinMax.MAXINT = new Integer(intmax[0]);
		if (realmin != null && realmin[0] != null)
			MinMax.MINDOUBLE = new Double(realmin[0]);
		if (realmax != null && realmax[0] != null)
			MinMax.MAXDOUBLE = new Double(realmax[0]);
		if (dontcare != null && dontcare[0] != null) {
			SymbolicInteger.UNDEFINED = new Integer(dontcare[0]);
			SymbolicReal.UNDEFINED = new Double(dontcare[0]);
		}
		System.out.println("symbolic.minint="+MinMax.MININT);
		System.out.println("symbolic.maxint="+MinMax.MAXINT);
		System.out.println("symbolic.minreal="+MinMax.MINDOUBLE);
		System.out.println("symbolic.maxreal="+MinMax.MAXDOUBLE);
		System.out.println("symbolic.undefined="+SymbolicInteger.UNDEFINED);
		if((SymbolicInteger.UNDEFINED >= MinMax.MININT && SymbolicInteger.UNDEFINED <= MinMax.MAXINT) &&
			(SymbolicInteger.UNDEFINED >= MinMax.MINDOUBLE && SymbolicInteger.UNDEFINED <= MinMax.MAXDOUBLE))
			System.err.println("Warning: undefined value should be outside  min..max ranges");
		 */
	}


}
