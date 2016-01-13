package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.ThreadChoiceGenerator;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import latte.LatteException;
import omega.exceptions.OmegaException;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FileUtils;

import utils.BigRational;
import utils.Configuration;
import analysis.Analyzer;
import analysis.ParallelAnalyzer;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import domain.Domain;
import domain.UsageProfile;
import domain.exceptions.InvalidUsageProfileException;

public class AntoniosListener extends PropertyListenerAdapter implements
		PublisherExtension {

	HashMultimap<Schedule, String> successfulExecutions;
	HashMultimap<Schedule, String> failedExecutions;
	HashMultimap<Schedule, String> greyExecutions;
	HashMap<Schedule, StatisticsReport> stats;

	Table<Schedule, Constraint, BigRational> detailedStatsSuccess;
	Table<Schedule, Constraint, BigRational> detailedStatsFailure;
	Table<Schedule, Constraint, BigRational> detailedStatsGrey;

	private long timeListenerStarted;

	private int exceptionCounter=0;
	
	private Config config;

	public AntoniosListener(Config conf, JPF jpf) {
		timeListenerStarted = System.currentTimeMillis();
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		successfulExecutions = HashMultimap.<Schedule, String> create();
		failedExecutions = HashMultimap.<Schedule, String> create();
		greyExecutions = HashMultimap.<Schedule, String> create();
		stats = new HashMap<Schedule, AntoniosListener.StatisticsReport>();
		detailedStatsSuccess = HashBasedTable
				.<Schedule, Constraint, BigRational> create();
		detailedStatsFailure = HashBasedTable
				.<Schedule, Constraint, BigRational> create();
		detailedStatsGrey = HashBasedTable
				.<Schedule, Constraint, BigRational> create();
		this.config = conf;
		unreportedCounter=0;
		exceptionCounter=0;
	}

	@Override
	public void searchFinished(Search search) {
		super.searchFinished(search);
		

		System.out.println("SEARCH FINISHED: ");
		System.out.println("SPF time: "
				+ (System.currentTimeMillis() - timeListenerStarted) + " ms.");
		System.out.println("Successful PCs: " + successfulExecutions.size());
		System.out.println("Failure PCs:    " + failedExecutions.size());
		System.out.println("Grey PCs:       " + greyExecutions.size());

		System.out.println("Num of exceptions: "+exceptionCounter);
		System.out.println("Unreported exceptions: "+unreportedCounter);
		
		// System.out.println(failedExecutions);

		System.out.println('\n');
		long startTime = System.currentTimeMillis();
		try {
			if (this.config == null) {
				System.out.println("problem");
			}
			System.out.println("TMP: "
					+ this.config.getProperty("symbolic.antonio.tmpDir"));
			Configuration configuration = new Configuration();
			configuration.setTemporaryDirectory(this.config
					.getProperty("symbolic.antonio.tmpDir"));
			configuration.setOmegaExectutablePath(this.config
					.getProperty("symbolic.antonio.omegaPath"));
			configuration.setLatteExecutablePath(this.config
					.getProperty("symbolic.antonio.lattePath"));

			// String pc=
			// "stage2_hpot_speed_12_SYMINT != CONST_22496 && stage2_lpot_speed_16_SYMINT == CONST_4120 && stage2_efi_pres_9_SYMINT == CONST_4800 && pegConverged_34_SYMINT <= CONST_1 && pegConverged_34_SYMINT >= CONST_0 && stage2_fail_18_SYMINT == CONST_0 && stage1_sep_fail_7_SYMINT <= CONST_1 && stage1_sep_fail_7_SYMINT >= CONST_0 && stage1_fail_6_SYMINT <= CONST_1 && stage1_fail_6_SYMINT >= CONST_0 && las_jettison_cmd_22_SYMINT == CONST_0 && las_jettison_cmd_22_SYMINT != CONST_1";

			/*
			 * CharStream psStream = new ANTLRFileStream(
			 * this.config.getProperty("symbolic.antonio.settings"));
			 * ProblemSettingsLexer psLexer = new
			 * ProblemSettingsLexer(psStream); TokenStream psTokenStream = new
			 * CommonTokenStream(psLexer); ProblemSettingsParser psParser = new
			 * ProblemSettingsParser( psTokenStream); ProblemSetting output =
			 * psParser.problemSettings();
			 */

			/*
			 * StringBuilder jointPCForDummuyProfiler = new StringBuilder(); for
			 * (Constraint constraint : successfulExecutions.values()) {
			 * jointPCForDummuyProfiler.append(constraint.toString() + "&&"); if
			 * (constraint.toString().contains(")2)")) {
			 * System.out.println("MALFORMEDCONSTRAINT: " +
			 * constraint.toString()); throw new
			 * scale.common.RuntimeException("Just to stop"); } } for
			 * (Constraint constraint : failedExecutions.values()) {
			 * jointPCForDummuyProfiler.append(constraint.toString() + "&&"); if
			 * (constraint.toString().contains(")2)")) {
			 * System.out.println("MALFORMEDCONSTRAINT: " +
			 * constraint.toString()); throw new
			 * scale.common.RuntimeException("Just to stop"); } } for
			 * (Constraint constraint : greyExecutions.values()) {
			 * jointPCForDummuyProfiler.append(constraint.toString() + "&&"); if
			 * (constraint.toString().contains(")2)")) {
			 * System.out.println("MALFORMEDCONSTRAINT: " +
			 * constraint.toString()); throw new
			 * scale.common.RuntimeException("Just to stop"); } }
			 * 
			 * jointPCForDummuyProfiler = jointPCForDummuyProfiler.delete(
			 * jointPCForDummuyProfiler.length() - 2,
			 * jointPCForDummuyProfiler.length()); String clean =
			 * jointPCForDummuyProfiler.toString().replaceAll( "\\s+", "");
			 * clean = clean.replaceAll("CONST_(\\d+)", "$1"); clean =
			 * clean.replaceAll("CONST_-(\\d+)", "-$1");
			 * 
			 * DummyUniformSettings dummyUniformSettings = new
			 * DummyUniformSettings( 0, 30);
			 * 
			 * ProblemSetting problemSetting = dummyUniformSettings
			 * .generateFromTraces(Sets.newHashSet(clean));
			 */

			// oae
			/*
			 * public void setSYMINPUTS_JPF(int cev_cm_cabin_pres, int cev_cm_cabin_pres_rate, int stage1_chmbr_pres,
            int stage1_tvc_actual, int stage1_tvc_commanded, int stage1_fail, int stage1_sep_fail, int stage2_apu_volt,
            int stage2_efi_pres, int stage2_helium_tnk_pres, int stage2_hpft_speed, int stage2_hpot_speed,
            int stage2_lh2_tnk_pres, int stage2_lox_tnk_pres, int stage2_lpft_speed, int stage2_lpot_speed,
            int stage2_thrust, int stage2_fail, int stage2_engine_cutoff_flag, int current_inert_vel_mag, int delayed_inert_vel_mag, int las_jettison_cmd,
            int vmissmag, int pitch, int pitch_rate, int roll, int roll_rate, int yaw, int yaw_rate, int geod_alt,
            int vmissmag_eo_now, int current_vgo_Mag, int delayed_vgo_Mag, int pegConverged, int egil, int delayed_is_good) {

			cev_cm_cabin_pres_1_SYMINT, 
			cev_cm_cabin_pres_rate_2_SYMINT, 
			stage1_chmbr_pres_3_SYMINT, 
			stage1_tvc_actual_4_SYMINT, 
			stage1_tvc_commanded_5_SYMINT, 
			stage1_fail_6_SYMINT, 
			stage1_sep_fail_7_SYMINT, 
			stage2_apu_volt_8_SYMINT, 
			stage2_efi_pres_9_SYMINT, 
			stage2_helium_tnk_pres_10_SYMINT, 
			stage2_hpft_speed_11_SYMINT, 
			stage2_hpot_speed_12_SYMINT, 
			stage2_lh2_tnk_pres_13_SYMINT, 
			stage2_lox_tnk_pres_14_SYMINT, 
			stage2_lpft_speed_15_SYMINT, 
			stage2_lpot_speed_16_SYMINT, 
			stage2_thrust_17_SYMINT, 
			stage2_fail_18_SYMINT, 
			stage2_engine_cutoff_flag_19_SYMINT, 
			current_inert_vel_mag_20_SYMINT, 
			delayed_inert_vel_mag_21_SYMINT, 
			las_jettison_cmd_22_SYMINT, 
			vmissmag_23_SYMINT, 
			pitch_24_SYMINT, 
			pitch_rate_25_SYMINT, 
			roll_26_SYMINT, 
			roll_rate_27_SYMINT, 
			yaw_28_SYMINT, 
			yaw_rate_29_SYMINT, 
			geod_alt_30_SYMINT, 
			vmissmag_eo_now_31_SYMINT, 
			current_vgo_Mag_32_SYMINT, 
			delayed_vgo_Mag_33_SYMINT, 
			pegConverged_34_SYMINT, 
			egil_35_SYMINT, 
			delayed_is_good_36_SYMINT )  

			 */
			Domain.Builder domainBuilder = new Domain.Builder();
	
				domainBuilder.addVariable("cev_cm_cabin_pres_1_SYMINT", 0,100);// TODO don't know what to put here 
				domainBuilder.addVariable("cev_cm_cabin_pres_rate_2_SYMINT",-20,0); // need to check this
				domainBuilder.addVariable("stage1_chmbr_pres_3_SYMINT", 630, 970);//?? min,max+-10
				domainBuilder.addVariable("stage1_tvc_actual_4_SYMINT",0,100);
				domainBuilder.addVariable("stage1_tvc_commanded_5_SYMINT",0,100); 
				domainBuilder.addVariable("stage1_fail_6_SYMINT",0,1); 
				domainBuilder.addVariable("stage1_sep_fail_7_SYMINT",0,1);
				domainBuilder.addVariable("stage2_apu_volt_8_SYMINT",13,43);//?? 
				domainBuilder.addVariable("stage2_efi_pres_9_SYMINT",4790,7210);//?? 
				domainBuilder.addVariable("stage2_helium_tnk_pres_10_SYMINT",550,850);//??  
				domainBuilder.addVariable("stage2_hpft_speed_11_SYMINT", 28278, 42442);//??
				domainBuilder.addVariable("stage2_hpot_speed_12_SYMINT", 22486, 33754);//??
				domainBuilder.addVariable("stage2_lh2_tnk_pres_13_SYMINT", 17, 49);//??
				domainBuilder.addVariable("stage2_lox_tnk_pres_14_SYMINT", 7,35);//??
				domainBuilder.addVariable("stage2_lpft_speed_15_SYMINT", 12938, 19432);//??
				domainBuilder.addVariable("stage2_lpot_speed_16_SYMINT", 4110, 6190);//??
				domainBuilder.addVariable("stage2_thrust_17_SYMINT", 221913, 332893);//??
				domainBuilder.addVariable("stage2_fail_18_SYMINT",0,1); 
				domainBuilder.addVariable("stage2_engine_cutoff_flag_19_SYMINT",0,1); 
				domainBuilder.addVariable("current_inert_vel_mag_20_SYMINT",20860, 22860);//??
				domainBuilder.addVariable("delayed_inert_vel_mag_21_SYMINT",20860, 22860);//??
				domainBuilder.addVariable("las_jettison_cmd_22_SYMINT",0,1); 
				domainBuilder.addVariable("vmissmag_23_SYMINT", 0,20);// bndry 10 
				domainBuilder.addVariable("pitch_24_SYMINT",0, 195); // bndry 185
				domainBuilder.addVariable("pitch_rate_25_SYMINT", 0, 110);// bndry 100
				domainBuilder.addVariable("roll_26_SYMINT",0,195);// bndry 185 
				domainBuilder.addVariable("roll_rate_27_SYMINT",0,60);// bndry 50 
				domainBuilder.addVariable("yaw_28_SYMINT", 0,195);//bndry 185
				domainBuilder.addVariable("yaw_rate_29_SYMINT",0, 51);//bndry 41
				domainBuilder.addVariable("geod_alt_30_SYMINT", 0, 120010);// 10000-10,120010);//??
				domainBuilder.addVariable("vmissmag_eo_now_31_SYMINT", 0, 520);//bndry 510 
				domainBuilder.addVariable("current_vgo_Mag_32_SYMINT",0,100); //dont know what to put here; i only know >=0
				domainBuilder.addVariable("delayed_vgo_Mag_33_SYMINT",0,100); // dont know what to put here; i only know >=0
				domainBuilder.addVariable("pegConverged_34_SYMINT",0,1); 
				domainBuilder.addVariable("egil_35_SYMINT", 0,1);
				domainBuilder.addVariable("delayed_is_good_36_SYMINT",0,1);
				
				
			Domain domain = domainBuilder.build();

			UsageProfile.Builder usageProfileBuilder = new UsageProfile.Builder();

			usageProfileBuilder.addScenario("+1*stage1_tvc_actual_4_SYMINT<=100&& -1*stage2_helium_tnk_pres_10_SYMINT<=-550&& -1*stage2_lpft_speed_15_SYMINT<=-12938&& -1*yaw_28_SYMINT<=0&& +1*stage1_fail_6_SYMINT<=1&& -1*stage1_tvc_commanded_5_SYMINT<=0&& +1*egil_35_SYMINT<=1&& +1*stage2_hpft_speed_11_SYMINT<=42442&& +1*vmissmag_23_SYMINT<=20&& -1*cev_cm_cabin_pres_1_SYMINT<=0&& +1*stage2_apu_volt_8_SYMINT<=43&& +1*las_jettison_cmd_22_SYMINT<=1&& -1*roll_26_SYMINT<=0&& +1*pegConverged_34_SYMINT<=1&& -1*pitch_24_SYMINT<=0&& +1*current_vgo_Mag_32_SYMINT<=100&& +1*geod_alt_30_SYMINT<=120010&& -1*cev_cm_cabin_pres_rate_2_SYMINT<=20&& -1*geod_alt_30_SYMINT<=0&& -1*delayed_vgo_Mag_33_SYMINT<=0&& +1*delayed_vgo_Mag_33_SYMINT<=100&& -1*vmissmag_23_SYMINT<=0&& +1*stage2_helium_tnk_pres_10_SYMINT<=850&& -1*vmissmag_eo_now_31_SYMINT<=0&& -1*delayed_is_good_36_SYMINT<=0&& +1*cev_cm_cabin_pres_rate_2_SYMINT<=0&& +1*stage2_lh2_tnk_pres_13_SYMINT<=49&& -1*stage2_fail_18_SYMINT<=0&& +1*stage2_lox_tnk_pres_14_SYMINT<=35&& +1*stage1_chmbr_pres_3_SYMINT<=970&& -1*delayed_inert_vel_mag_21_SYMINT<=-20860&& -1*roll_rate_27_SYMINT<=0&& +1*yaw_rate_29_SYMINT<=51&& +1*stage2_hpot_speed_12_SYMINT<=33754&& +1*stage2_efi_pres_9_SYMINT<=7210&& +1*pitch_24_SYMINT<=195&& -1*las_jettison_cmd_22_SYMINT<=0&& -1*stage2_hpft_speed_11_SYMINT<=-28278&& +1*roll_26_SYMINT<=195&& -1*pitch_rate_25_SYMINT<=0&& +1*stage2_lpft_speed_15_SYMINT<=19432&& +1*delayed_inert_vel_mag_21_SYMINT<=22860&& +1*current_inert_vel_mag_20_SYMINT<=22860&& -1*egil_35_SYMINT<=0&& -1*stage2_hpot_speed_12_SYMINT<=-22486&& +1*stage2_thrust_17_SYMINT<=332893&& -1*stage2_apu_volt_8_SYMINT<=-13&& +1*roll_rate_27_SYMINT<=60&& +1*stage1_tvc_commanded_5_SYMINT<=100&& -1*stage2_thrust_17_SYMINT<=-221913&& +1*delayed_is_good_36_SYMINT<=1&& +1*vmissmag_eo_now_31_SYMINT<=520&& +1*stage2_lpot_speed_16_SYMINT<=6190&& -1*stage1_sep_fail_7_SYMINT<=0&& +1*stage1_sep_fail_7_SYMINT<=1&& +1*cev_cm_cabin_pres_1_SYMINT<=100&& -1*stage2_engine_cutoff_flag_19_SYMINT<=0&& -1*yaw_rate_29_SYMINT<=0&& +1*stage2_engine_cutoff_flag_19_SYMINT<=1&& -1*pegConverged_34_SYMINT<=0&& -1*stage1_fail_6_SYMINT<=0&& +1*stage2_fail_18_SYMINT<=1&& -1*stage2_lpot_speed_16_SYMINT<=-4110&& -1*current_vgo_Mag_32_SYMINT<=0&& -1*stage2_efi_pres_9_SYMINT<=-4790&& +1*yaw_28_SYMINT<=195&& -1*stage1_chmbr_pres_3_SYMINT<=-630&& +1*pitch_rate_25_SYMINT<=110&& -1*stage2_lh2_tnk_pres_13_SYMINT<=-17&& -1*stage1_tvc_actual_4_SYMINT<=0&& -1*stage2_lox_tnk_pres_14_SYMINT<=-7&& -1*current_inert_vel_mag_20_SYMINT<=-20860", 1);

			UsageProfile usageProfile=usageProfileBuilder.build();
			
			String pc="+1*stage1_tvc_actual_4_SYMINT<=100&& -1*stage2_helium_tnk_pres_10_SYMINT<=-550&& -1*stage2_lpft_speed_15_SYMINT<=-12938&& -1*yaw_28_SYMINT<=0&& +1*stage1_fail_6_SYMINT<=1&& -1*stage1_tvc_commanded_5_SYMINT<=0&& +1*egil_35_SYMINT<=1&& +1*stage2_hpft_speed_11_SYMINT<=42442&& +1*vmissmag_23_SYMINT<=20&& -1*cev_cm_cabin_pres_1_SYMINT<=0&& +1*stage2_apu_volt_8_SYMINT<=43&& +1*las_jettison_cmd_22_SYMINT<=1&& -1*roll_26_SYMINT<=0&& +1*pegConverged_34_SYMINT<=1&& -1*pitch_24_SYMINT<=0&& +1*current_vgo_Mag_32_SYMINT<=100&& +1*geod_alt_30_SYMINT<=120010&& -1*cev_cm_cabin_pres_rate_2_SYMINT<=20&& -1*geod_alt_30_SYMINT<=0&& -1*delayed_vgo_Mag_33_SYMINT<=0&& +1*delayed_vgo_Mag_33_SYMINT<=100&& -1*vmissmag_23_SYMINT<=0&& +1*stage2_helium_tnk_pres_10_SYMINT<=850&& -1*vmissmag_eo_now_31_SYMINT<=0&& -1*delayed_is_good_36_SYMINT<=0&& +1*cev_cm_cabin_pres_rate_2_SYMINT<=0&& +1*stage2_lh2_tnk_pres_13_SYMINT<=49&& -1*stage2_fail_18_SYMINT<=0&& +1*stage2_lox_tnk_pres_14_SYMINT<=35&& +1*stage1_chmbr_pres_3_SYMINT<=970&& -1*delayed_inert_vel_mag_21_SYMINT<=-20860&& -1*roll_rate_27_SYMINT<=0&& +1*yaw_rate_29_SYMINT<=51&& +1*stage2_hpot_speed_12_SYMINT<=33754&& +1*stage2_efi_pres_9_SYMINT<=7210&& +1*pitch_24_SYMINT<=195&& -1*las_jettison_cmd_22_SYMINT<=0&& -1*stage2_hpft_speed_11_SYMINT<=-28278&& +1*roll_26_SYMINT<=195&& -1*pitch_rate_25_SYMINT<=0&& +1*stage2_lpft_speed_15_SYMINT<=19432&& +1*delayed_inert_vel_mag_21_SYMINT<=22860&& +1*current_inert_vel_mag_20_SYMINT<=22860&& -1*egil_35_SYMINT<=0&& -1*stage2_hpot_speed_12_SYMINT<=-22486&& +1*stage2_thrust_17_SYMINT<=332893&& -1*stage2_apu_volt_8_SYMINT<=-13&& +1*roll_rate_27_SYMINT<=60&& +1*stage1_tvc_commanded_5_SYMINT<=100&& -1*stage2_thrust_17_SYMINT<=-221913&& +1*delayed_is_good_36_SYMINT<=1&& +1*vmissmag_eo_now_31_SYMINT<=520&& +1*stage2_lpot_speed_16_SYMINT<=6190&& -1*stage1_sep_fail_7_SYMINT<=0&& +1*stage1_sep_fail_7_SYMINT<=1&& +1*cev_cm_cabin_pres_1_SYMINT<=100&& -1*stage2_engine_cutoff_flag_19_SYMINT<=0&& -1*yaw_rate_29_SYMINT<=0&& +1*stage2_engine_cutoff_flag_19_SYMINT<=1&& -1*pegConverged_34_SYMINT<=0&& -1*stage1_fail_6_SYMINT<=0&& +1*stage2_fail_18_SYMINT<=1&& -1*stage2_lpot_speed_16_SYMINT<=-4110&& -1*current_vgo_Mag_32_SYMINT<=0&& -1*stage2_efi_pres_9_SYMINT<=-4790&& +1*yaw_28_SYMINT<=195&& -1*stage1_chmbr_pres_3_SYMINT<=-630&& +1*pitch_rate_25_SYMINT<=110&& -1*stage2_lh2_tnk_pres_13_SYMINT<=-17&& -1*stage1_tvc_actual_4_SYMINT<=0&& -1*stage2_lox_tnk_pres_14_SYMINT<=-7&& -1*current_inert_vel_mag_20_SYMINT<=-20860";
			Analyzer domainAnalyzer = new Analyzer(configuration, domain, usageProfile);
			
			BigRational result = domainAnalyzer.analyzeSpfPC("+1*stage1_tvc_actual_4_SYMINT<=100&& -1*stage2_helium_tnk_pres_10_SYMINT<=-550&& -1*stage2_lpft_speed_15_SYMINT<=-12938&& -1*yaw_28_SYMINT<=0&& +1*stage1_fail_6_SYMINT<=1&& -1*stage1_tvc_commanded_5_SYMINT<=0&& +1*egil_35_SYMINT<=1&& +1*stage2_hpft_speed_11_SYMINT<=42442&& +1*vmissmag_23_SYMINT<=20&& -1*cev_cm_cabin_pres_1_SYMINT<=0&& +1*stage2_apu_volt_8_SYMINT<=43&& +1*las_jettison_cmd_22_SYMINT<=1&& -1*roll_26_SYMINT<=0&& +1*pegConverged_34_SYMINT<=1&& -1*pitch_24_SYMINT<=0&& +1*current_vgo_Mag_32_SYMINT<=100&& +1*geod_alt_30_SYMINT<=120010&& -1*cev_cm_cabin_pres_rate_2_SYMINT<=20&& -1*geod_alt_30_SYMINT<=0&& -1*delayed_vgo_Mag_33_SYMINT<=0&& +1*delayed_vgo_Mag_33_SYMINT<=100&& -1*vmissmag_23_SYMINT<=0&& +1*stage2_helium_tnk_pres_10_SYMINT<=850&& -1*vmissmag_eo_now_31_SYMINT<=0&& -1*delayed_is_good_36_SYMINT<=0&& +1*cev_cm_cabin_pres_rate_2_SYMINT<=0&& +1*stage2_lh2_tnk_pres_13_SYMINT<=49&& -1*stage2_fail_18_SYMINT<=0&& +1*stage2_lox_tnk_pres_14_SYMINT<=35&& +1*stage1_chmbr_pres_3_SYMINT<=970&& -1*delayed_inert_vel_mag_21_SYMINT<=-20860&& -1*roll_rate_27_SYMINT<=0&& +1*yaw_rate_29_SYMINT<=51&& +1*stage2_hpot_speed_12_SYMINT<=33754&& +1*stage2_efi_pres_9_SYMINT<=7210&& +1*pitch_24_SYMINT<=195&& -1*las_jettison_cmd_22_SYMINT<=0&& -1*stage2_hpft_speed_11_SYMINT<=-28278&& +1*roll_26_SYMINT<=195&& -1*pitch_rate_25_SYMINT<=0&& +1*stage2_lpft_speed_15_SYMINT<=19432&& +1*delayed_inert_vel_mag_21_SYMINT<=22860&& +1*current_inert_vel_mag_20_SYMINT<=22860&& -1*egil_35_SYMINT<=0&& -1*stage2_hpot_speed_12_SYMINT<=-22486&& +1*stage2_thrust_17_SYMINT<=332893&& -1*stage2_apu_volt_8_SYMINT<=-13&& +1*roll_rate_27_SYMINT<=60&& +1*stage1_tvc_commanded_5_SYMINT<=100&& -1*stage2_thrust_17_SYMINT<=-221913&& +1*delayed_is_good_36_SYMINT<=1&& +1*vmissmag_eo_now_31_SYMINT<=520&& +1*stage2_lpot_speed_16_SYMINT<=6190&& -1*stage1_sep_fail_7_SYMINT<=0&& +1*stage1_sep_fail_7_SYMINT<=1&& +1*cev_cm_cabin_pres_1_SYMINT<=100&& -1*stage2_engine_cutoff_flag_19_SYMINT<=0&& -1*yaw_rate_29_SYMINT<=0&& +1*stage2_engine_cutoff_flag_19_SYMINT<=1&& -1*pegConverged_34_SYMINT<=0&& -1*stage1_fail_6_SYMINT<=0&& +1*stage2_fail_18_SYMINT<=1&& -1*stage2_lpot_speed_16_SYMINT<=-4110&& -1*current_vgo_Mag_32_SYMINT<=0&& -1*stage2_efi_pres_9_SYMINT<=-4790&& +1*yaw_28_SYMINT<=195&& -1*stage1_chmbr_pres_3_SYMINT<=-630&& +1*pitch_rate_25_SYMINT<=110&& -1*stage2_lh2_tnk_pres_13_SYMINT<=-17&& -1*stage1_tvc_actual_4_SYMINT<=0&& -1*stage2_lox_tnk_pres_14_SYMINT<=-7&& -1*current_inert_vel_mag_20_SYMINT<=-20860");
			
			System.out.println("Domain probability: "+result);
			
			//if(1==1) throw new scale.common.RuntimeException("Just to stop");
			
			/*
			 * Plexill
			 * 
			 * int numVars = 20;
			 * 
			 * Domain.Builder domainBuilder = new Domain.Builder(); for (int i =
			 * 1; i <= numVars; i++) { domainBuilder.addVariable("in_" + i +
			 * "_SYMINT", 0, 2); } Domain domain = domainBuilder.build();
			 * 
			 * UsageProfile.Builder usageProfileBuilder = new
			 * UsageProfile.Builder(); StringBuilder fakeProfile = new
			 * StringBuilder(); for (int i = 2; i <= numVars; i++) {
			 * fakeProfile.append("in_" + i + "_SYMINT>=0&&in_" + i +
			 * "_SYMINT<=2&&"); } String in1_0 = fakeProfile.toString() +
			 * "in_1_SYMINT=0"; String in1_1 = fakeProfile.toString() +
			 * "in_1_SYMINT=1"; String in1_2 = fakeProfile.toString() +
			 * "in_1_SYMINT=2";
			 * 
			 * usageProfileBuilder.addScenario(in1_0, 0.2);
			 * 
			 * usageProfileBuilder.addScenario(in1_1, 0.3);
			 * usageProfileBuilder.addScenario(in1_2, 0.5); UsageProfile
			 * usageProfile = usageProfileBuilder.build();
			 */

			/*
			 * TRIANGLES
			 * 
			 * Domain.Builder domainBuilder = new Domain.Builder();
			 * domainBuilder.addVariable("a_1_SYMINT", -1000, 1000);
			 * domainBuilder.addVariable("b_2_SYMINT", -1000, 1000);
			 * domainBuilder.addVariable("c_3_SYMINT", -1000, 1000); Domain
			 * domain = domainBuilder.build();
			 * 
			 * UsageProfile.Builder usageProfileBuilder = new
			 * UsageProfile.Builder(); // usageProfileBuilder.addScenario(
			 * "a_1_SYMINT>=-1000&&a_1_SYMINT<=1000&&b_2_SYMINT>=-1000&&b_2_SYMINT<=1000&&c_3_SYMINT>=-1000&&c_3_SYMINT<=1000"
			 * ,1); usageProfileBuilder .addScenario(
			 * "a_1_SYMINT!=b_2_SYMINT&&a_1_SYMINT!=c_3_SYMINT&&b_2_SYMINT!=c_3_SYMINT"
			 * , 0.001); usageProfileBuilder.addScenario(
			 * "a_1_SYMINT==b_2_SYMINT&&a_1_SYMINT!=c_3_SYMINT", 0.001);
			 * usageProfileBuilder.addScenario(
			 * "a_1_SYMINT!=b_2_SYMINT&&a_1_SYMINT==c_3_SYMINT", 0.001);
			 * usageProfileBuilder.addScenario(
			 * "a_1_SYMINT!=b_2_SYMINT&&b_2_SYMINT==c_3_SYMINT", 0.001);
			 * usageProfileBuilder .addScenario(
			 * "a_1_SYMINT==b_2_SYMINT&&a_1_SYMINT==c_3_SYMINT&&a_1_SYMINT<=0",
			 * 0.001); usageProfileBuilder .addScenario(
			 * "a_1_SYMINT==b_2_SYMINT&&a_1_SYMINT==c_3_SYMINT&&a_1_SYMINT>0", 1
			 * - 5 * (0.001));
			 * 
			 * UsageProfile usageProfile = usageProfileBuilder.build();
			 * 
			 * /* Domain.Builder domainBuilder = new Domain.Builder();
			 * domainBuilder.addVariable("x_1_SYMINT", 0, 10); Domain domain =
			 * domainBuilder.build();
			 * 
			 * UsageProfile.Builder usageProfileBuilder = new
			 * UsageProfile.Builder();
			 * usageProfileBuilder.addScenario("x_1_SYMINT>=0&&x_1_SYMINT<=10",
			 * BigRational.ONE); UsageProfile usageProfile =
			 * usageProfileBuilder.build();
			 */

			// parallelAnalysis(configuration, domain, usageProfile, 4);
			sequentialAnalysis(configuration, domain,
					usageProfile);
			long finishTime = System.currentTimeMillis();
			System.out.println("Analysis completed in "
					+ (finishTime - startTime) + " ms.");
			
			try {
				FileUtils.deleteDirectory(new
				File(configuration.getTemporaryDirectory()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RecognitionException e) {
			e.printStackTrace();
		} catch (InvalidUsageProfileException e) {
			e.printStackTrace();
		} catch (LatteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OmegaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("DONE.");

	}

	private final void parallelAnalysis(Configuration configuration,
			Domain domain, UsageProfile usageProfile, int numThreads)
			throws OmegaException, RecognitionException {
		try {
			ParallelAnalyzer parallelAnalyzer = new ParallelAnalyzer(
					configuration, domain, usageProfile, numThreads);

			Set<Schedule> schedules = new HashSet<Schedule>(
					successfulExecutions.keySet());
			schedules.addAll(greyExecutions.keySet());
			schedules.addAll(failedExecutions.keySet());

			System.out.println("Total number of schedules: " + schedules.size()
					+ "\n");

			for (Schedule schedule : schedules) {
				System.out.println("Analyzing schedule: " + schedule);
				Set<String> successfulTraces = Sets.newHashSet(Collections2
						.transform(successfulExecutions.get(schedule),
								constraintToString));
				BigRational probabilityOfSuccess = parallelAnalyzer
						.analyze(successfulTraces);
				System.out.println("\tProbability of success: "
						+ probabilityOfSuccess);

				Set<String> failureTraces = Sets.newHashSet(Collections2
						.transform(failedExecutions.get(schedule),
								constraintToString));
				BigRational probabilityOfFailure = parallelAnalyzer
						.analyze(failureTraces);
				System.out.println("\tProbability of failure: "
						+ probabilityOfFailure);

				/*
				 * Set<String> greyTraces = Sets.newHashSet(Collections2
				 * .transform(greyExecutions.get(schedule),
				 * constraintToString)); BigRational probabilityOfGrey =
				 * parallelAnalyzer .analyze(greyTraces);
				 * System.out.println("\tProbability of grey: " +
				 * probabilityOfGrey);
				 * 
				 * BigRational sum =
				 * probabilityOfSuccess.plus(probabilityOfGrey)
				 * .plus(probabilityOfFailure);
				 * System.out.println("\n\tSum of probabilities: " + sum +
				 * "\n");
				 */
				BigRational probabilityOfGrey = BigRational.ONE.minus(
						probabilityOfSuccess).minus(probabilityOfFailure);
				System.out.println("\tProbability of grey: "
						+ probabilityOfGrey);
			}
		} catch (LatteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private final void sequentialAnalysis(Configuration configuration,
			Domain domain, UsageProfile usageProfile) {
		try {
			Analyzer analyzer = new Analyzer(configuration, domain,
					usageProfile);

			Set<Schedule> schedules = new HashSet<Schedule>(
					successfulExecutions.keySet());
			schedules.addAll(greyExecutions.keySet());
			schedules.addAll(failedExecutions.keySet());

			System.out.println("Total number of schedules: " + schedules.size()
					+ "\n");

			for (Schedule schedule : schedules) {
				System.out.println("Analyzing schedule: " + schedule);
				BigRational probabilityOfSuccess = BigRational.ZERO;
				Set<String> successfulTraces = Sets.newHashSet(Collections2
						.transform(successfulExecutions.get(schedule),
								constraintToString));
				for (String trace : successfulTraces) {
					try {
						BigRational traceProbability = analyzer
								.analyzeSpfPC(trace);
						probabilityOfSuccess = probabilityOfSuccess
								.plus(traceProbability);
					} catch (Exception e) {
						System.out.println("Exception: " + e.getMessage());
						System.out.println(trace);
						throw new RuntimeException("Just to stop");
					}

				}
				System.out.println("\tProbability of success: "
						+ probabilityOfSuccess);

				BigRational probabilityOfFailure = BigRational.ZERO;
				Set<String> failureTraces = Sets.newHashSet(Collections2
						.transform(failedExecutions.get(schedule),
								constraintToString));
				for (String trace : failureTraces) {
					try {
						probabilityOfFailure = probabilityOfFailure
								.plus(analyzer.analyzeSpfPC(trace));
					} catch (Exception e) {
						System.out.println("ExceptionF: " + e.getMessage());
						System.out.println(trace);
						throw new RuntimeException("Just to stop");
					}

				}
				System.out.println("\tProbability of failure: "
						+ probabilityOfFailure);

				BigRational probabilityOfGrey = BigRational.ZERO;
				Set<String> greyTraces = Sets.newHashSet(Collections2
						.transform(greyExecutions.get(schedule),
								constraintToString));
				for (String trace : greyTraces) {
					try {
						probabilityOfGrey = probabilityOfGrey.plus(analyzer
								.analyzeSpfPC(trace));
					} catch (Exception e) {
						System.out.println("ExceptionF: " + e.getMessage());
						System.out.println(trace);
						throw new RuntimeException("Just to stop");
					}

				}
				System.out.println("\tProbability of grey: "
						+ probabilityOfGrey);

				BigRational sum = probabilityOfSuccess.plus(probabilityOfGrey)
						.plus(probabilityOfFailure);
				System.out.println("\n\tSum of probabilities: " + sum + "\n");
				if (!sum.equals(BigRational.ONE) && !sum.equals(BigRational.ZERO)) {
					System.out.println("Success conditions: \n"
							+ successfulExecutions.get(schedule) + "\n\n");
					System.out.println("Failure conditions: \n"
							+ failedExecutions.get(schedule) + "\n\n");
					System.out.println("Grey conditions: \n"
							+ greyExecutions.get(schedule) + "\n\n");
					throw new scale.common.RuntimeException("Just to stop");
				}
				// analyzer.checkPCsIntersection();
				// System.out.println("Traces verified.");
			}
		} catch (LatteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (RecognitionException e) {
			e.printStackTrace();
		} catch (OmegaException e) {
			e.printStackTrace();
		}
	}

	private final Function<Constraint, String> constraintToStringOld = new Function<Constraint, String>() {

		@Override
		public String apply(Constraint constraint) {
			String clean = constraint.toString().replaceAll("\\s+", "");
			clean = clean.replaceAll("CONST_(\\d+)", "$1");
			clean = clean.replaceAll("CONST_-(\\d+)", "-$1");
			return clean;
		}
	};
	
	private final Function<String, String> constraintToString = new Function<String, String>() {

		@Override
		public String apply(String constraint) {
			String clean = constraint.replaceAll("\\s+", "");
			clean = clean.replaceAll("CONST_(\\d+)", "$1");
			clean = clean.replaceAll("CONST_-(\\d+)", "-$1");
			return clean;
		}
	};
	private int unreportedCounter;

	@Override
	public void threadTerminated(JVM vm) {
		super.threadTerminated(vm);
		ThreadChoiceGenerator[] threadChoiceGenerators = vm
				.getChoiceGeneratorsOfType(ThreadChoiceGenerator.class);
		Schedule currentSchedule = Schedule.create(threadChoiceGenerators);
		PCChoiceGenerator choiceGenerator = vm
				.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);

		if (choiceGenerator != null && choiceGenerator.getCurrentPC() != null
				&& vm.getAliveThreadCount() == 0) {
			System.out.println("TerminatedSuccess: " + vm.getThreadNumber());
			successfulExecutions.put(currentSchedule,
					choiceGenerator.getCurrentPC().header.toString());
		}
	}

	@Override
	public void stateAdvanced(Search search) {
		super.stateAdvanced(search);
		if (search.isEndState()) {
			JVM vm = search.getVM();
			ThreadChoiceGenerator[] threadChoiceGenerators = vm
					.getChoiceGeneratorsOfType(ThreadChoiceGenerator.class);
			Schedule currentSchedule = Schedule.create(threadChoiceGenerators);
			PCChoiceGenerator choiceGenerator = vm
					.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);

			if (choiceGenerator != null
					&& choiceGenerator.getCurrentPC() != null) {
				System.out.println("SUCCESSADDED"
						+ choiceGenerator.getCurrentPC().header);
				successfulExecutions.put(currentSchedule,
						choiceGenerator.getCurrentPC().header.toString());
			} else {
				System.out.println("SUCCESSNOPC");
			}
		}
	}

	@Override
	public void exceptionThrown(JVM vm) {
		super.exceptionThrown(vm);
		ThreadChoiceGenerator[] threadChoiceGenerators = vm
				.getChoiceGeneratorsOfType(ThreadChoiceGenerator.class);
		Schedule currentSchedule = Schedule.create(threadChoiceGenerators);
		PCChoiceGenerator choiceGenerator = vm
				.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);
		exceptionCounter++;
		if (choiceGenerator != null && choiceGenerator.getCurrentPC() != null) {
			failedExecutions.put(currentSchedule,
					choiceGenerator.getCurrentPC().header.toString());
		}else{
			unreportedCounter++;
		}
	}

	@Override
	public void searchConstraintHit(Search search) {
		ThreadChoiceGenerator[] threadChoiceGenerators = search.getVM()
				.getChoiceGeneratorsOfType(ThreadChoiceGenerator.class);
		Schedule currentSchedule = Schedule.create(threadChoiceGenerators);
		PCChoiceGenerator choiceGenerator = search.getVM()
				.getLastChoiceGeneratorOfType(PCChoiceGenerator.class);

		if (!search.isEndState() && !search.isErrorState()) {
			if (choiceGenerator != null
					&& choiceGenerator.getCurrentPC() != null) {
				System.out.println("ADDEDGREY: " + choiceGenerator + "\t"
						+ choiceGenerator.getCurrentPC().header);
				greyExecutions.put(currentSchedule,
						choiceGenerator.getCurrentPC().header.toString());
			} else {
				System.out.println("HITTEDNOTCATCH: " + currentSchedule + "\t"
						+ choiceGenerator);
			}
		}
	}

	private String cleanConstraint(Constraint constraint) {
		String clean = constraint.toString().replaceAll("\\s+", "");
		clean = clean.replaceAll("CONST_(\\d+)", "$1");
		clean = clean.replaceAll("CONST_-(\\d+)", "-$1");
		return clean;
	}

	public void printReport() {
		System.out.println("Successful paths: ");
		for (Schedule schedule : successfulExecutions.keySet()) {
			System.out.println("\tSchedule: " + schedule);
			int pathsCounter = 0;
			for (String pathCondition : successfulExecutions.get(schedule)) {
				System.out.println("\t\tPath[" + pathsCounter + "]: "
						+ pathCondition);
				pathsCounter++;
			}
		}
		System.out.println("Failure paths: ");
		for (Schedule schedule : failedExecutions.keySet()) {
			System.out.println("\tSchedule: " + schedule);
			int pathsCounter = 0;
			for (String pathCondition : failedExecutions.get(schedule)) {
				System.out.println("\t\tPath[" + pathsCounter + "]: "
						+ pathCondition);
				pathsCounter++;
			}
		}
		System.out.println("Grey paths: ");
		for (Schedule schedule : greyExecutions.keySet()) {
			System.out.println("\tSchedule: " + schedule);
			int pathsCounter = 0;
			for (String pathCondition : greyExecutions.get(schedule)) {
				System.out.println("\t\tPath[" + pathsCounter + "]: "
						+ pathCondition);
				pathsCounter++;
			}
		}

		System.out.println("Statistics: ");
		for (Schedule schedule : stats.keySet()) {
			System.out.println(schedule + " \t " + stats.get(schedule));
		}
	}

	public class StatisticsReport {
		private final int numOfSuccessfulExecutions;
		private final BigRational probabilityOfSuccess;
		private final int numOfFailureExecutions;
		private final BigRational probabilityOfFailure;
		private final int numOfGreyExecutions;
		private final BigRational probabilityOfGreyZone;

		public StatisticsReport(int numOfSuccessfulExecutions,
				BigRational probabilityOfSuccess, int numOfFailureExecutions,
				BigRational probabilityOfFailure, int numOfGreyExecutions,
				BigRational probabilityOfGreyZone) {
			super();
			this.numOfSuccessfulExecutions = numOfSuccessfulExecutions;
			this.probabilityOfSuccess = probabilityOfSuccess;
			this.numOfFailureExecutions = numOfFailureExecutions;
			this.probabilityOfFailure = probabilityOfFailure;
			this.numOfGreyExecutions = numOfGreyExecutions;
			this.probabilityOfGreyZone = probabilityOfGreyZone;
		}

		public int getNumOfSuccessfulExecutions() {
			return numOfSuccessfulExecutions;
		}

		public BigRational getProbabilityOfSuccess() {
			return probabilityOfSuccess;
		}

		public int getNumOfFailureExecutions() {
			return numOfFailureExecutions;
		}

		public BigRational getProbabilityOfFailure() {
			return probabilityOfFailure;
		}

		public int getNumOfGreyExecutions() {
			return numOfGreyExecutions;
		}

		public BigRational getProbabilityOfGreyZone() {
			return probabilityOfGreyZone;
		}

		@Override
		public String toString() {
			return "#oS: " + numOfSuccessfulExecutions + "\tPoS: "
					+ probabilityOfSuccess + "\n#oF: " + numOfFailureExecutions
					+ "\tPoF: " + probabilityOfFailure + "\n#oG: "
					+ numOfGreyExecutions + "\tPoG: " + probabilityOfGreyZone;
		}
	}
}
