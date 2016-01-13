package gov.nasa.jpf.listener;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * generic choice tracker tool, to produce a list of choice values that can be
 * used to create readable replay scripts etc.
 */
public class ChoiceTracker extends ListenerAdapter implements
		PublisherExtension {

	enum Format {
		CG, CHOICE
	};

	Config config;
	JVM vm;
	Search search;
	int branchCounter;
	boolean[] log;

	protected PrintWriter pw;
	Class<?>[] cgClasses;
	boolean isReportExtension;

	boolean showLocation;
	Format format = Format.CHOICE;
	String[] excludes;

	// <2do> hardwired type specific tracker for use with some shells - check if
	// we can get rid of it
	public ChoiceTracker(JPF jpf, String traceFileName, Class<?> cgClass) {
		config = jpf.getConfig();
		vm = jpf.getVM();
		search = jpf.getSearch();
		branchCounter = 0;
		loadingLog();

		cgClasses = new Class<?>[1];
		cgClasses[0] = cgClass;

		try {
			pw = new PrintWriter(traceFileName);
		} catch (FileNotFoundException fnfx) {
			System.err.println("cannot write choice trace to file: "
					+ traceFileName);
			pw = new PrintWriter(System.out);
		}
	}

	public ChoiceTracker(Config config, JPF jpf) {
		this.config = config;
		vm = jpf.getVM();
		search = jpf.getSearch();
		branchCounter = 0;
		loadingLog();

		String fname = config.getString("choice.trace");
		if (fname == null) {
			isReportExtension = true;
			jpf.addPublisherExtension(ConsolePublisher.class, this);
			// pw is going to be set later
		} else {
			try {
				pw = new PrintWriter(fname);
			} catch (FileNotFoundException fnfx) {
				System.err.println("cannot write choice trace to file: "
						+ fname);
				pw = new PrintWriter(System.out);
			}
		}

		excludes = config.getStringArray("choice.exclude");
		cgClasses = config.getClasses("choice.class");

		format = config.getEnum("choice.format", Format.values(), Format.CG);
		showLocation = config.getBoolean("choice.show_location", true);
	}

	private boolean loadingLog() {
		String fname = config.getString("choice.log");
		try {
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line = br.readLine();
			String[] prev = line.split(" ");
			System.out.println("--------------Loading log-------------");
			log = new boolean[prev.length];
			for (int i = 0; i < prev.length; i++) {
				if (Integer.parseInt(prev[i]) == 0) {
					log[i] = true;
				} else {
					log[i] = false;
				}
				// System.out.println(prev[i]);
			}
			System.out.println("--------------------------------------");
			br.close();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public void setExcludes(String... ex) {
		excludes = ex;
	}

	boolean isRelevantCG(ChoiceGenerator cg) {
		if (cgClasses == null) {
			return true;
		} else {
			for (Class<?> cls : cgClasses) {
				if (cls.isAssignableFrom(cg.getClass())) {
					return true;
				}
			}

			return false;
		}
	}

	public void choiceGeneratorAdvanced(JVM jvm) {
		System.out.println("-------------new cg-----------");
		SystemState ss = vm.getSystemState();
		ChoiceGenerator<?> cg = ss.getChoiceGenerator();
		Object choice = cg.getNextChoice();
		System.out.println(choice.toString());
		if (!choice.toString().contains("Thread")) {
			int decision = 1;
			if (log[branchCounter]) {
				decision = 0;
			}
			cg.select(decision);
			branchCounter++;
		}
		System.out.println("---------------------------------");
	}

	/*
	 * public void stateAdvanced (Search search){
	 * System.out.println("-------------new state-----------");
	 * //this.printChoices();
	 * 
	 * SystemState ss = vm.getSystemState();
	 * System.out.println("State = "+vm.getSystemState().getId());
	 * 
	 * ChoiceGenerator<?> cg = ss.getChoiceGenerator(); Object choice =
	 * cg.getNextChoice(); System.out.println(choice.toString()); if
	 * (!choice.toString().contains("Thread")){
	 * System.out.println("Counter: "+branchCounter); if
	 * ((log[branchCounter]&&(Integer
	 * .parseInt(choice.toString())==0))||(!log[branchCounter
	 * ]&&(Integer.parseInt(choice.toString())==1))){ //Matches
	 * System.out.println("Matches"); //cg.setDone(); //branchCounter++; }else{
	 * //Backtrack, wrong decision //vm.backtrack(); //cg.setDone();
	 * System.out.println("Does not matches"); } }
	 * System.out.println("---------------------------------"); }
	 */

	public void propertyViolated(Search search) {

		if (!isReportExtension) {

			pw.print("// application: ");
			pw.print(config.getTarget());
			for (String s : config.getTargetArgs()) {
				pw.print(s);
				pw.print(' ');
			}
			pw.println();

			if (cgClasses == null) {
				pw.println("// trace over all CG classes");
			} else {
				pw.print("// trace over CG types: ");
				for (Class<?> cls : cgClasses) {
					pw.print(cls.getName());
					pw.print(' ');
				}
				pw.println();
			}

			pw.println("//------------------------- choice trace");
			printChoices();

			pw.println("//------------------------- end choice trace");
			pw.flush();
		}
	}

	void printChoices() {
		int i = 0;
		SystemState ss = vm.getSystemState();
		ChoiceGenerator<?>[] cgStack = ss.getChoiceGenerators();

		nextChoice: for (ChoiceGenerator<?> cg : cgStack) {
			if (isRelevantCG(cg) && !cg.isDone()) {

				Object choice = cg.getNextChoice();
				if (choice == null) {
					continue;
				} else {
					if (excludes != null) {
						for (String e : excludes) {
							if (choice.toString().startsWith(e)) {
								continue nextChoice;
							}
						}
					}
				}

				String line = null;

				switch (format) {
				case CHOICE:
					line = choice.toString();
					if (line.startsWith("gov.nasa.jpf.jvm.")) {
						line = line.substring(17);
					}
					break;
				case CG:
					line = cg.toString();
					if (line.startsWith("gov.nasa.jpf.jvm.choice.")) {
						line = line.substring(24);
					}
					break;
				}

				if (line != null) {
					System.out.print("printing line: ");
					System.out.print(String.format("%4d: ", i++));
					pw.print(String.format("%4d: ", i++));
					System.out.print(line);
					pw.print(line);

					if (showLocation) {
						String loc = cg.getSourceLocation();
						if (loc != null) {
							System.out.println();
							pw.println();
							System.out.print(" \tat ");
							pw.print(" \tat ");
							System.out.print(loc);
							pw.print(loc);
						}
					}
					System.out.println();
					pw.println();
				}
			}
		}
	}

	// --- the PublisherExtension interface

	public void publishPropertyViolation(Publisher publisher) {
		pw = publisher.getOut();
		publisher.publishTopicStart("choice trace "
				+ publisher.getLastErrorId());
		printChoices();
	}
}
