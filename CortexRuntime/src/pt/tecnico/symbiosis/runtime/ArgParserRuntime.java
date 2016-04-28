package pt.tecnico.symbiosis.runtime;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;


public class ArgParserRuntime {
	public static String defaultFile = System.getProperty("user.dir")+System.getProperty("file.separator")+"traceBB";
	private final Map<Option, String> argsValues;
	private final static NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(); 
	
	
	public ArgParserRuntime()
	{
		argsValues = new EnumMap<Option, String>(Option.class);

		for(Option opt : Option.values())
		{
			argsValues.put(opt, opt.defaultVal);
		}
	}

	
	public void parse(String[] args) {
		int idx = 0;

		if(args.length == 0)
		{
			System.out.println("[SymbiosisRuntime] No new settings inserted. Running recorder with default configuration:");
			System.out.println(this+"\n\n");
		}

		boolean isMain = false; //** used to parse main class' parameters
		Option mainOpt = null; 	//** used to parse main class' parameters
		while (idx < args.length) {
			Option option = Option.fromString(args[idx]);
			if (option == null) {
				if(isMain) {
					String prev = argsValues.get(mainOpt);
					prev = prev +" " + args[idx];
					argsValues.put(mainOpt,prev);
					idx++;
					continue;
				}
				else {
					throw new IllegalArgumentException("[SymbiosisRuntime] Unkown option: " + args[idx] + ". Possible options are: " +
							Arrays.asList(Option.values()));
				}
			}
			idx++;
			if (option.isBoolean()) {
				isMain = false;
				argsValues.put(option, "true");
				continue;
			}
			if (idx >= args.length) {
				throw new IllegalArgumentException("expected a value for option " + option);
			}
			if(option == Option.MAIN_CLASS){
				isMain = true;
				mainOpt = option;
			}
			else
				isMain = false;
			
			argsValues.put(option, args[idx++]);
		}
	}
	
	public void validate() 
	{
		if(getValue(Option.MAIN_CLASS).isEmpty()) 
		{
			throw new IllegalArgumentException("[SymbiosisRuntime] No main class introduced. Please indicate the program's main class as follows: "+Option.MAIN_CLASS+" [path-to-main-class]");
		}
	}
	
	public final String getValue(Option option) {
		return argsValues.get(option);
	}
	
	/*
	 * Class Option - defines all possible input options for this component
	 */
	public static enum Option {	
		//** NAME_OPTION( flag, is only flag?, description, config parameter name, default value) 
		SYMBTRACE("--bb-trace",false,"usage: --bb-traces [path-to-trace-file] | Generates a trace file with each thread's control flow in the path indicated.",null,defaultFile),
		MAIN_CLASS("--main-class",false,"usage: --main-class [path-to-main-class] [parameters] | Program's main class, previoulsy instrumented, along with the corresponding parameters.","mainClass",null),
		FULLTRACE("-full",true,"usage: -full | Stores the basic block trace for the entire execution in addition to the assertion",null,null)
		;

		private final String arg;
		private final boolean isBoolean;
		private final String description;
		private final String configParamName;
		private final String defaultVal;

		Option(String arg, boolean isBoolean, String description, String configParamName, String defaultVal) {
			if (arg == null) {
				throw new IllegalArgumentException("Null not allowed in Option name");
			}
			this.arg = arg;
			this.isBoolean = isBoolean;
			this.description = description;
			this.configParamName = configParamName;
			this.defaultVal = defaultVal;
		}

		public final String getArgName() {
			return arg;
		}

		public final boolean isBoolean() {
			return isBoolean;
		}

		public final String toString() {
			return arg;
		}

		public final String getDescription(){
			return this.description;
		}

		public static Option fromString(String optionName) {
			for (Option option : values()) {
				if (option.getArgName().equalsIgnoreCase(optionName)) {
					return option;
				}
			}
			return null;
		}
	}
}
