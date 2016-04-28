package gov.nasa.jpf.symbc;

import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class TestExJPF extends TestSymbolicOutput {
  public static final List<String> config = Arrays.asList(new String[] {
      "+vm.insn_factory.class=gov.nasa.jpf.symbc.SymbolicInstructionFactory",
      // "+jpf.listener=gov.nasa.jpf.symbc.TestSymbolicListener",
      "+vm.storage.class=", "+search.multiple_errors=true",
      "+jpf.report.console.finished=" });

  public static void main(String args[]) {
    System.out.println(org.junit.runner.JUnitCore
        .runClasses(gov.nasa.jpf.symbc.TestExJPF.class));
  }

  public String allSymbolicArgs(int numArgs) {
    String[] symArray = new String[numArgs];
    Arrays.fill(symArray, 0, numArgs, "sym");
    return StringUtils.join(symArray, "#");
  }

  public int getNumberofArguments(Method m) {
    return m.getParameterTypes().length;
  }

  public Method getTestMethod(Class<?> c) {
    for (Method m : c.getMethods()) {
      if (m.getName().equals("test")) {
        return m;
      }
    }

    return null;
  }

  /**
   * Reverse a mapping. The values become the new keys
   * and the keys become the new values
   *
   * @param <K> Key type
   * @param <V> Value type
   * @param map Original Map
   * @return A map which is the reversal of the parameter map
   */
  public static<K,V> Map<V,K> flipMap(Map<? extends K,? extends V> map) {
    Map<V,K> retmap = new TreeMap<V, K>();

    for(K key : map.keySet())
      retmap.put(map.get(key), key);

    return retmap;
  }

  public String[] JPFArgs(Class<?> c) {
    LinkedList<String> args = new LinkedList<String>();
    args.addAll(config);
    args.add("+symbolic.method=" + c.getName() +  ".test("
        + allSymbolicArgs(getNumberofArguments(getTestMethod(c))) + ")");
    args.add(c.getName());
    return args.toArray(new String[args.size()]);
  }

  @Test
  public void TestExSymExeD2I() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExeD2I.class));
  }

  @Test
  public void TestExSymExePrecondAndMath() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExePrecondAndMath.class));
  }

  @Test
  public void TestExSymExe31() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe31.class));
  }

  @Test
  public void TestExSymExe14() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe14.class));
  }

  @Test
  public void TestExSymExe21() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe21.class));
  }

  @Test
  public void TestExSymExeLongBCwGlob2() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExeLongBCwGlob2.class));
  }

  @Test
  public void TestExSymExeMathIAsolver() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExeMathIAsolver.class));
  }

  @Test
  public void TestExSymExeLCMP() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExeLCMP.class));
  }

  //@Test
  //on choco2 throws div by 0 because z_REAL == 0 is not satisfiable,
  //choco2 doesn' support real values
  public void TestExSymExe30() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe30.class));
  }

  /*
   * Problem: Can't work with private method ////@Test public void
   * TestExSymExeLongBCwGlob() {
   * runJPFnoException(JPFArgs(gov.nasa.jpf.symbc.ExSymExeLongBCwGlob.class)); }
   */

  @Test
  public void TestExSymExe5() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe5.class));
  }

  @Test
  public void TestExSymExe3() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe3.class)).values();

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe3\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe3\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExe13() {

    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe13.class)).values();
    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe13\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe13\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeLongBytecodes2() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeLongBytecodes2.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExeLongBytecodes2\n");
    correct.append("branch diff <= c\n");

    assertEquals(outputs.size(), 9);
  }

  @Test
  public void TestExSymExe27() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe27.class)).values();
    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe27\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe27\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeLNEG() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeLNEG.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing LNEG\nbranch -x > 0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing LNEG\nbranch -x <= 0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  @Test
  public void TestExSymExe26() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe26.class)).values();
    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe26\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe26\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe26\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe26\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  //@Test Choco exception Non Linear Integer Constraint not handled
  public void TestExSymExe18() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe18.class));
  }

  @Test
  public void TestExSymExe9() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe9.class)).values();

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe9\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe9\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO2\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExe12() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe12.class)).values();

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe12\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe12\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe12\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO2\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 3);
  }

  @Test
  public void TestExSymExe2() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe2.class)).values();

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe2\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe2\n");
    correct4.append("branch FOO1\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExe17() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe17.class));
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe17.class)).values();

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe17\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe17\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExe11() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe11.class)).values();
    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe11\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe11\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExe() {
	  noPropertyViolation(JPFArgs(gov.nasa.jpf.symbc.ExSymExe.class));
  }

  @Test
  public void TestExSymExe7() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe7.class)).values();
    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe7\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe7\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeFNEG() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeFNEG.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing FNEG\nbranch -x > 0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing FNEG\nbranch -x <= 0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  @Test
  public void TestExSymExeI2F() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeI2F.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("x >0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("x <=0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  //@Test Choco exception Non Linear Integer Constraint not handled
  public void TestExSymExe10() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe10.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe10\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe10\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe10\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe10\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  @Test
  public void TestExSymExe15() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe15.class)).values();


    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe15\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe15\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 2);
  }

  //@Test works on choco2, doesn't work on choco
  public void TestExSymExeSwitch() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeSwitch.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("branch Foo0\n");
    correct.append("default2\n");

    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("branch Foo1\n");
    correct2.append("default2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("default1\n");
    correct3.append("branch Foo2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("default1\n");
    correct4.append("branch Foo3000\n");
    assertTrue(outputs.contains(correct4.toString()));

    StringBuffer correct5 = new StringBuffer();
    correct5.append("default1\n");
    correct5.append("default2\n");
    assertTrue(outputs.contains(correct5.toString()));

    assertEquals(outputs.size(), 5);
  }

  @Test
  public void TestExSymExe6() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe6.class)).values();
    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe6\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe6\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeTestAssignments() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeTestAssignments.class)).values();

    assertTrue(outputs.contains("branch BOO1\n"));
    assertEquals(outputs.size(), 1);
  }

  @Test
  public void TestExSymExeF2L() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeF2L.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("x >0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("x <=0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  @Test
  public void TestExSymExe25() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe25.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe25\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe25\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe25\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe25\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  /*
   * No test method ////@Test public void TestExSymExeGetStatic() {
   * runJPFnoException(JPFArgs(gov.nasa.jpf.symbc.ExSymExeGetStatic.class)); }
   */

  //@Test both choco and choco2 cannot solve situation for FOO1,BOO1
  public void TestExSymExe19() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe19.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe19\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe19\n");
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe19\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe19\n");
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  @Test
  public void TestExSymExe20() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe20.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe20\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO2\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe20\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe20\n");
    correct3.append("branch FOO1\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 3);
  }

  @Test
  public void TestExSymExe28() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe28.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe28\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO2\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe28\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe28\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe28\n");
    correct4.append("branch FOO1\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  @Test
  public void TestExSymExeF2I() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeF2I.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("x >0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("x <=0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  @Test
  public void TestExSymExeD2L() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeD2L.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("x >0\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("x <=0\n");
    assertTrue(outputs.contains(correct2.toString()));
  }

  @Test
  public void TestExSymExe29() {

    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe29.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe29\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO2\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe29\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe29\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExe29\n");
    correct4.append("branch FOO1\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  //@Test works on choco2
  public void TestExSymExe16() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe16.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe16\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO2\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe16\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe16\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 3);

  }

  /*
   * Has testC method instead of test method ////@Test public void TestExSymExe34() {
   * runJPFnoException(JPFArgs(gov.nasa.jpf.symbc.ExSymExe34.class)); }
   */

  @Test
  public void TestExSymExeBool() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeBool.class)).values();
    assertTrue(outputs.contains("Testing ExSymExeBool\nbranch FOO1\n"));
    assertTrue(outputs.contains("Testing ExSymExeBool\nbranch FOO2\n"));
    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeI2D() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeI2D.class)).values();
    assertTrue(outputs.contains("x >0\n"));
    assertTrue(outputs.contains("x <=0\n"));
    assertEquals(outputs.size(), 3);
  }

  @Test
  public void TestExSymExeDDIV() {
	  unhandledException(ArithmeticException.class.getName(),null, JPFArgs(gov.nasa.jpf.symbc.ExSymExeDDIV.class));
  }

  @Test
  public void TestExSymExe32() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe32.class)).values();

    StringBuffer correct = new StringBuffer();

    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("branch FOO1\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("branch FOO2\n");
    correct4.append("branch BOO1\n");
    assertTrue(outputs.contains(correct4.toString()));

    assertEquals(outputs.size(), 4);
  }

  @Test
  public void TestExSymExeArrays() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeArrays.class)).values();
    assertTrue(outputs.contains("branch1 >=0\n"));
    assertTrue(outputs.contains("branch2 <0\n"));
    assertEquals(outputs.size(), 2);
  }

  @Test
  public void TestExSymExeResearch() {
    Map<PathCondition, String> outputs = runSymbolicJPF(JPFArgs(gov.nasa.jpf.symbc.ExSymExeResearch.class));
    assertEquals(outputs.size(), 6);
  }

  @Test
  public void TestExSymExe4() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe4.class)).values();

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe4\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe4\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO1\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe4\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO2\n");
    assertTrue(outputs.contains(correct3.toString()));

    assertEquals(outputs.size(), 3);
  }

  @Test
  //on choco2 does not finish in reasonable time
  public void TestExSymExeLongBytecodes() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExeLongBytecodes.class)).values();

    StringBuffer correct1 = new StringBuffer();
    correct1.append("Testing ExSymExeLongBytecodes\n");
    correct1.append("branch diff > c\n");
    correct1.append("branch sum < z\n");
    assertTrue(outputs.contains(correct1.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExeLongBytecodes\n");
    correct2.append("branch diff > c\n");
    correct2.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExeLongBytecodes\n");
    correct3.append("branch diff > c\n");
    correct3.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct3.toString()));

    StringBuffer correct4 = new StringBuffer();
    correct4.append("Testing ExSymExeLongBytecodes\n");
    correct4.append("branch diff <= c\n");
    correct4.append("branch sum < z\n");
    assertTrue(outputs.contains(correct4.toString()));

    StringBuffer correct5 = new StringBuffer();
    correct5.append("Testing ExSymExeLongBytecodes\n");
    correct5.append("branch diff <= c\n");
    correct5.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct5.toString()));

    StringBuffer correct6 = new StringBuffer();
    correct6.append("Testing ExSymExeLongBytecodes\n");
    correct6.append("branch diff <= c\n");
    correct6.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct6.toString()));

    StringBuffer correct7 = new StringBuffer();
    correct7.append("Testing ExSymExeLongBytecodes\n");
    correct7.append("branch diff <= c\n");
    correct7.append("branch sum < z\n");
    assertTrue(outputs.contains(correct7.toString()));

    StringBuffer correct8 = new StringBuffer();
    correct8.append("Testing ExSymExeLongBytecodes\n");
    correct8.append("branch diff <= c\n");
    correct8.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct8.toString()));

    StringBuffer correct9 = new StringBuffer();
    correct9.append("Testing ExSymExeLongBytecodes\n");
    correct9.append("branch diff <= c\n");
    correct9.append("branch sum >= z\n");
    assertTrue(outputs.contains(correct9.toString()));

    assertEquals(outputs.size(), 9);
  }

  @Test
  public void TestExSymExe8() {
    Collection<String> outputs = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe8.class)).values();

    StringBuffer correct = new StringBuffer();

    correct.append("Testing ExSymExe8\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO1\n");
    assertTrue(outputs.contains(correct.toString()));

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe8\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe8\n");
    correct3.append("branch FOO2\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));

  }

  @Test
  public void TestExSymExe1() {

    Map<PathCondition, String> results = runSymbolicJPF(
        JPFArgs(gov.nasa.jpf.symbc.ExSymExe1.class));

    Collection<String> outputs = results.values();
    Map<String, PathCondition>reverseResults = flipMap(results);

    StringBuffer correct = new StringBuffer();
    correct.append("Testing ExSymExe1\n");
    correct.append("branch FOO1\n");
    correct.append("branch BOO2\n");
    assertTrue(outputs.contains(correct.toString()));
    PathCondition condition = reverseResults.get(correct.toString()).make_copy();
    Constraint constraint = new LinearIntegerConstraint(new SymbolicInteger("z_2_SYMINT"),
        Comparator.LE, new IntegerConstant(0));
    //assertTrue(condition.hasConstraint(constraint));

    condition._addDet(Comparator.EQ, new SymbolicInteger("z_2_SYMINT"), new IntegerConstant(40));
    assertTrue(new SymbolicConstraintsGeneral().isSatisfiable(condition));
    assertTrue(condition.simplify());

    StringBuffer correct2 = new StringBuffer();
    correct2.append("Testing ExSymExe1\n");
    correct2.append("branch FOO2\n");
    correct2.append("branch BOO2\n");
    assertTrue(outputs.contains(correct2.toString()));

    StringBuffer correct3 = new StringBuffer();
    correct3.append("Testing ExSymExe1\n");
    correct3.append("branch FOO1\n");
    correct3.append("branch BOO1\n");
    assertTrue(outputs.contains(correct3.toString()));
  }
}
