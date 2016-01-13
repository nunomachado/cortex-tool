package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.choice.ThreadChoiceFromSet;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import latte.LatteException;
import omega.exceptions.OmegaException;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FileUtils;

import scale.common.RuntimeException;
import utils.BigRational;
import utils.Configuration;
import analysis.AnalysisException;
import analysis.Analyzer;
import analysis.DummyUniformSettings;
import analysis.ParallelAnalyzer;
import analysis.SchedulesHolder;
import analysis.SchedulesHolder.Schedule;
import analysis.SchedulesHolder.ScheduleStatistics;
import analysis.SequentialAnalyzer;

import com.google.common.collect.Sets;

import domain.ProblemSetting;
import domain.exceptions.InvalidUsageProfileException;

public class AntoniosListener extends PropertyListenerAdapter implements
		PublisherExtension {
	private static final int DEFAULT_MIN_INT = -1000;
	private static final int DEFAULT_MAX_INT = 1000;
	private static final int DEFAULT_CACHE_FILLERS_PERCENTAGE=5;

	private long timeListenerStarted;
	private long SPFCompleted;
	private long probabilityAnalysisCompleted;
	private final SchedulesHolder schedulesHolder;
	private Config config;
	private StringBuilder jointPCs = null;
	private int minInt = DEFAULT_MIN_INT;
	private int maxInt = DEFAULT_MAX_INT;
	private int numOfSolvers = 1;
	private Analyzer analyzer = null;
	private String temporaryWorkingDir = null;

	public AntoniosListener(Config conf, JPF jpf) {
		timeListenerStarted = System.currentTimeMillis();
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		this.schedulesHolder = new SchedulesHolder();
		this.config = conf;
		if (conf.getProperty("symbolic.antonio.dummyProfile") != null
				&& conf.getProperty("symbolic.antonio.dummyProfile")
						.equalsIgnoreCase("true")) {
			jointPCs = new StringBuilder();
			if (conf.getProperty("symbolic.antonio.minInt") != null) {
				minInt = Integer.parseInt(conf
						.getProperty("symbolic.antonio.minInt"));
			}
			if (conf.getProperty("symbolic.antonio.maxInt") != null) {
				maxInt = Integer.parseInt(conf
						.getProperty("symbolic.antonio.maxInt"));
			}
		}
	}

	@Override
	public void searchFinished(Search search) {
		super.searchFinished(search);
		analysis();
		printResults();
		cleanUp();
		//printSuccessfulPCsForMaximal();
	}

	private void analysis() {
		this.SPFCompleted = System.currentTimeMillis();

		Configuration configuration = new Configuration();
		configuration.setTemporaryDirectory(this.config
				.getProperty("symbolic.antonio.tmpDir"));
		configuration.setOmegaExectutablePath(this.config
				.getProperty("symbolic.antonio.omegaPath"));
		configuration.setLatteExecutablePath(this.config
				.getProperty("symbolic.antonio.lattePath"));

		this.temporaryWorkingDir = configuration.getTemporaryDirectory();

		ProblemSetting problemSettings = null;
		if (jointPCs != null) {
			// create dummy profile
			String jointPCSString = jointPCs.toString();
			if (jointPCSString.endsWith("&&")) {
				jointPCSString = jointPCSString.substring(0,
						jointPCSString.length() - 2);
			}

			DummyUniformSettings dummyUniformSettings = new DummyUniformSettings(
					this.minInt, this.maxInt);

			try {
				problemSettings = dummyUniformSettings.generateFromTraces(Sets
						.newHashSet(jointPCSString));
			} catch (RecognitionException e) {
				e.printStackTrace();
			} catch (InvalidUsageProfileException e) {
				e.printStackTrace();
			}
		} else {
			// load problemSettings from file
			String problemSettingsPath = this.config
					.getProperty("symbolic.antonio.problemSettings");
			if (problemSettingsPath == null) {
				throw new RuntimeException(
						"Problem settings must be dummy or privided by file.");
			}
			try {
				problemSettings = ProblemSetting
						.loadFromFile(problemSettingsPath);
				System.out.println("Problem settings loaded from: "
						+ problemSettingsPath);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RecognitionException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Problem settings:\n"+problemSettings+"\n\n");
		try {
			String numOfKernelsProp = this.config
					.getProperty("symbolic.antonio.numOfKernels");
			int numOfKernels=1;
			if(numOfKernelsProp!=null){
				numOfKernels=Integer.parseInt(numOfKernelsProp);
			}
			
			String numOfThreads = this.config
					.getProperty("symbolic.antonio.numOfSolvers");
			if (numOfThreads == null) {
				analyzer = new SequentialAnalyzer(configuration,
						problemSettings.getDomain(),
						problemSettings.getUsageProfile(),numOfKernels);
			}else{
				int numOfSolversFromConfiguration = Integer
						.parseInt(numOfThreads);
				if (numOfSolversFromConfiguration <= 1) {
					analyzer = new SequentialAnalyzer(configuration,
							problemSettings.getDomain(),
							problemSettings.getUsageProfile(),numOfKernels);
					this.numOfSolvers = 1;
				} else {
					String cacheFillersPercentage = this.config
							.getProperty("symbolic.antonio.cacheFillersPercentage");
					int cacheFillersPercent = (cacheFillersPercentage==null)?DEFAULT_CACHE_FILLERS_PERCENTAGE:Integer.parseInt(cacheFillersPercentage);
					analyzer = new ParallelAnalyzer(configuration,
							problemSettings.getDomain(),
							problemSettings.getUsageProfile(),
							numOfSolversFromConfiguration,cacheFillersPercent,numOfKernels);
					this.numOfSolvers = numOfSolversFromConfiguration;
				}
			}
			System.out.println("Starting analysis with " + this.numOfSolvers
					+ " solvers");
			schedulesHolder.analyze(analyzer);
			System.out.println("Analysis complete.");
		} catch (AnalysisException e) {
			e.printStackTrace();
		} catch (LatteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (OmegaException e) {
			e.printStackTrace();
		} catch (RecognitionException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		this.probabilityAnalysisCompleted = System.currentTimeMillis();
	}

	private void cleanUp() {
		String keepTempFiles = this.config
				.getProperty("symbolic.antonio.keepTempFiles");
		if (keepTempFiles == null || !keepTempFiles.equalsIgnoreCase("true")) {
			try {
				long startCleanUp = System.currentTimeMillis();
				System.out.println("Cleaning up temporary files...");
				FileUtils.deleteDirectory(new File(this.temporaryWorkingDir));
				System.out.println("done in "
						+ (System.currentTimeMillis() - startCleanUp) + " ms.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final void printResults() {
		Set<Schedule> allSchedules = schedulesHolder.getAllSchedules();
		System.out.println("Total number of schedules: " + allSchedules.size());
		Set<Schedule> maximalSchedules = schedulesHolder.getMaximalSchedules();
		System.out.println("of whom the maximal are: "
				+ maximalSchedules.size());
		System.out.println("with the following statistics: ");

		ArrayList<Schedule> sortedSchedules = new ArrayList<SchedulesHolder.Schedule>(maximalSchedules);
		Collections.sort(sortedSchedules,new SchedulesComparator(schedulesHolder,SchedulesComparator.Mood.PESSIMISTIC));
		
		for (Schedule schedule : maximalSchedules) {
			System.out.println("Schedule: " + schedule);
			System.out.println(schedulesHolder
					.getStatisticsPerSchedule(schedule));
		}

		System.out
				.println("-----------------------------------------------------");
		
		System.out.println("The best schedule is : "+sortedSchedules.get(0)+"\n with statistics:\n"+schedulesHolder.getStatisticsPerSchedule(sortedSchedules.get(0)));
		System.out.println("The worst schedule is : "+sortedSchedules.get(sortedSchedules.size()-1)+"\n with statistics:\n"+schedulesHolder.getStatisticsPerSchedule(sortedSchedules.get(sortedSchedules.size()-1)));
		
		System.out.println("Analysis stats:");
		System.out.println("SPF exploration time: "
				+ (this.SPFCompleted - this.timeListenerStarted) + " ms");
		System.out.println("For a total of: ");
		System.out.println("\t"
				+ schedulesHolder.getNumOfSuccessfulPCsRecorded()
				+ "\tsuccessful PCs");
		System.out.println("\t" + schedulesHolder.getNumOfFailurePCsRecorded()
				+ "\tfailure PCs");
		/*System.out.println("\t" + schedulesHolder.getNumOfGreyPCsRecorded()
				+ "\tgrey PCs");*/
		System.out.println("\nProbabilistc analysis completed in "
				+ (this.probabilityAnalysisCompleted - this.SPFCompleted)
				+ " ms");
		if (this.numOfSolvers > 1) {
			System.out.println("performed with " + this.numOfSolvers
					+ " analyzers in parallel");
		} else {
			System.out.println("performed with a sequential analyzer");
		}
		System.out.println("\nChaches statistics:\n" + analyzer.chachesStats());
		System.out.println("\nSolvers statistics:\n" + analyzer.solversStats());
	}

	private void printSuccessfulPCsForMaximal(){
		Set<Schedule> maximalSchedules = schedulesHolder.getMaximalSchedules();
		for(Schedule schedule : maximalSchedules){
			System.out.println("Schedule: "+schedule);
			Set<String> successfulPCs=schedulesHolder.getSuccessfulPCs(schedule);
			for(String pc : successfulPCs){
				System.out.println("\t"+pc);
			}
			System.out.println("\n\n");
		}
	}

	@Override
	public void stateAdvanced(Search search) {
		super.stateAdvanced(search);
		if (search.isEndState()) {
			JVM vm = search.getVM();
			ThreadChoiceFromSet[] threadChoiceGenerators = vm
					.getChoiceGeneratorsOfType(ThreadChoiceFromSet.class);
			Schedule currentSchedule = new Schedule(
					normalizeThreadIds(threadChoiceGenerators));
			PCChoiceGenerator choiceGenerator = vm
					.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);

			if (choiceGenerator != null
					&& choiceGenerator.getCurrentPC() != null) {
				schedulesHolder.addSuccessfulPC(currentSchedule,
						cleanConstraint(choiceGenerator.getCurrentPC().header
								.toString()));
			}
		}
	}

	@Override
	public void exceptionThrown(JVM vm) {
		super.exceptionThrown(vm);
		ThreadChoiceFromSet[] threadChoiceGenerators = vm
				.getChoiceGeneratorsOfType(ThreadChoiceFromSet.class);
		Schedule currentSchedule = new Schedule(
				normalizeThreadIds(threadChoiceGenerators));
		PCChoiceGenerator choiceGenerator = vm
				.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
		
		if (choiceGenerator != null && choiceGenerator.getCurrentPC() != null) {
					schedulesHolder.addFailedPC(currentSchedule,
					cleanConstraint(choiceGenerator.getCurrentPC().header
							.toString()));
		}
	}

	@Override
	public void searchConstraintHit(Search search) {
		PCChoiceGenerator choiceGenerator = search.getVM()
				.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);

		if (!search.isEndState() && !search.isErrorState()) {
			if (choiceGenerator != null
					&& choiceGenerator.getCurrentPC() != null) {
				this.schedulesHolder.capturedGrey();
				/*schedulesHolder.addGreyPC(currentSchedule,
						cleanConstraint(choiceGenerator.getCurrentPC().header
								.toString()));*/
				
			}
		}
	}

	private int[] normalizeThreadIds(
			ThreadChoiceFromSet[] threadChoiceGenerators) {
		int[] normalized = new int[threadChoiceGenerators.length];
		for (int i = 0; i < threadChoiceGenerators.length; i++) {
			normalized[i] = threadChoiceGenerators[i].getNextChoice().getId();
		}
		return normalized;
	}

	private String cleanConstraint(String constraint) {
		String clean = constraint.replaceAll("\\s+", "");
		clean = clean.replaceAll("CONST_(\\d+)", "$1");
		clean = clean.replaceAll("CONST_-(\\d+)", "-$1");
		// TODO remove
		if (jointPCs != null) {
			jointPCs.append(clean + "&&");
		}
		return clean;
	}

	
	private static class SchedulesComparator implements Comparator<Schedule>{
		public enum Mood {OPTIMISTIC,PESSIMISTIC,INDIFFERENT}
		private final SchedulesHolder schedulesHolder;
		private final Mood mood;
		
		public SchedulesComparator(SchedulesHolder schedulesHolder, Mood mood) {
			super();
			this.schedulesHolder = schedulesHolder;
			this.mood = mood;
		}

		public SchedulesComparator(SchedulesHolder schedulesHolder){
			this(schedulesHolder,Mood.INDIFFERENT);
		}
		
		@Override
		public int compare(Schedule o1, Schedule o2) {
			BigRational score1,score2;
			switch(mood){
			case OPTIMISTIC: score1=getOptimisticScore(o1);score2=getOptimisticScore(o2);break;
			case PESSIMISTIC: score1=getPessimisticScore(o1);score2=getPessimisticScore(o2);break;
			case INDIFFERENT: score1=getIndifferentScore(o1);score2=getIndifferentScore(o2);break;
			default: score1=getIndifferentScore(o1);score2=getIndifferentScore(o2);break;
			}
			
			int candidateResult = score1.compareTo(score2);
			
			if(candidateResult==0){
				//Comparing size
				int size1=o1.getSequenceOfChoices().length;
				int size2=o2.getSequenceOfChoices().length;
				if(size1==size2){
					return countSwitches(o1)-countSwitches(o2);
				}else{
					return size1-size2;
				}
			}else{
				return -1*score1.compareTo(score2);
			}
		}
		
		
		private int countSwitches(Schedule schedule){
			int[] choices = schedule.getSequenceOfChoices();
			int switches=0;
			for(int i=0;i<choices.length-1;i++){
				if(choices[i]!=choices[i+1]){
					switches++;
				}
			}
			return switches;
		}

		private BigRational getOptimisticScore(Schedule schedule){
			ScheduleStatistics stats = schedulesHolder.getStatisticsPerSchedule(schedule);
			return stats.getSuccessProbability().plus(stats.getGreyProbability());
		}
		
		private BigRational getPessimisticScore(Schedule schedule){
			ScheduleStatistics stats = schedulesHolder.getStatisticsPerSchedule(schedule);
			return stats.getSuccessProbability();
		}
		
		private BigRational getIndifferentScore(Schedule schedule){
			ScheduleStatistics stats = schedulesHolder.getStatisticsPerSchedule(schedule);
			return stats.getSuccessProbability().minus(stats.getFailureProbability());
		}
		
	}
	
}
