package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.ExceptionInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.util.test.TestJPF;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.util.JPFSiteUtils;

import java.util.List;
import java.util.Map;

/**
 * This class Runs JPF with the symbolic listener and returns
 * a map from path conditions to outputs. Each entry in the map 
 * is the path condition for a path through the program and the
 * standard output for that path.
 *  
 * @author jwu
 *
 */
public class TestSymbolicOutput extends TestJPF {
  
  /**
   * Run JPF with the <code>TestSymbolicListener</code> and 
   * output at <code>PathCondition</code> to <code>String</code>
   * mapping.
   *  
   * @param args Arguments for running the symbolic engine
   * 
   * @return PathCondition to Output mapping
   */
  public Map<PathCondition, String> runSymbolicJPF(String[] args) {

    Config conf = JPF.createConfig(args);

    if (conf.getTarget() != null) {
      ExceptionInfo xi = null;

       //--- initialize the classpath from <projectId>.test_classpath
    String projectId = JPFSiteUtils.getCurrentProjectId();
    if (projectId != null) {
      String testCp = conf.getString(projectId + ".test_classpath");
      if (testCp != null) {
        conf.append("classpath", testCp, ",");
      }
    }

      JPF jpf = new JPF(conf);
      TestSymbolicListener listener = new TestSymbolicListener(conf, jpf);
      jpf.addListener(listener);
      jpf.run();

      List<Error> errors = jpf.getSearchErrors();
      if ((errors != null) && (errors.size() > 0)) {
        fail("JPF found unexpected errors: " + (errors.get(0)).getDescription());
      }

      JVM vm = jpf.getVM();
      if (vm != null) {
        xi = vm.getPendingException();
      }

      if (xi != null) {
        fail("JPF caught exception executing: " + args[0] + " " + args[1]
            + " : " + xi.getExceptionClassname());
      }

      return listener.getOutputs();
    }
    
    throw new IllegalArgumentException("arguments must have a class name");
   
  }
}
