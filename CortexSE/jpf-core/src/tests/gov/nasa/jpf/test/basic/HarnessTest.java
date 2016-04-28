package gov.nasa.jpf.test.basic;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.TypeRef;
import gov.nasa.jpf.util.test.TestJPF;

import org.junit.Test;

/**
 * basic test to test the test harness
 */
public class HarnessTest extends TestJPF {

  int d;

  @Test
  public void noViolation() {
    if (verifyNoPropertyViolation()) {
      d += 42;

      System.out.println("** this is noViolation() - it should succeed");
    }
  }

  @Test
  public void verifyAssert() {
    if (verifyAssertionErrorDetails("java.lang.AssertionError : wrong answer..")) {
      System.out.println("** this is verifyAssert() - JPF should find an AssertionError");

      assert d == 42 : "wrong answer..";
    }
  }

  @Test
  public void verifyNullPointerException() {
    if (verifyUnhandledException("java.lang.NullPointerException")) {
      System.out.println("** this is verifyNullPointerException() - JPF should find an NPE");

      String s = null;

      s.length();
    }
  }

  @Test
  public void verifyRuntimeException() {
    if (verifyPropertyViolation(new TypeRef("gov.nasa.jpf.jvm.NoUncaughtExceptionsProperty"))) {
      System.out.println("** this is verifyRuntimeException() - JPF should find an unhandled exception");

      throw new RuntimeException("Bang!");
    }
  }

  @Test
  public void verifyJPFExcept() {
    if (verifyJPFException(new TypeRef("gov.nasa.jpf.JPFConfigException"), "+vm.class=InvalidVMClass", "+pass_exceptions")) {
      fail("** JPF should not run");
    }
  }

  @Test
  public void recursive() {
    JPF jpf;

    jpf = noPropertyViolation("gov.nasa.jpf.util.test.TestJPFHelper", HarnessTest.class.getName(), "recursive");

    if (jpf == null) {
      System.out.println("** this is low level API recursive - it should succeed");
    } else {
      assert jpf.getSearchErrors().isEmpty() : "unexpected JPF search errors";
    }
  }
}
