//
// Copyright (C) 2006 United States Government as represented by the
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

package gov.nasa.jpf.test.mc.basic;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.DoubleChoiceGenerator;
import gov.nasa.jpf.jvm.IntChoiceGenerator;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.jvm.choice.DoubleChoiceFromList;
import gov.nasa.jpf.jvm.choice.IntIntervalGenerator;
import gov.nasa.jpf.util.test.TestJPF;

import java.util.Comparator;

import org.junit.Test;

/**
 * regression test for choice reordering APIs 
 */
public class CGReorderTest extends TestJPF {

  public static class ReverseListener extends ListenerAdapter {  
    @Override
    public void choiceGeneratorSet (JVM vm){
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      if (cg instanceof IntIntervalGenerator){
        System.out.println("reverse choice enumeration order");
        ((IntIntervalGenerator)cg).reverse();
      }
    }

    int lastVal = Integer.MAX_VALUE;
    @Override
    public void choiceGeneratorAdvanced (JVM vm){
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      if (cg instanceof IntIntervalGenerator){
        int v = ((IntChoiceGenerator)cg).getNextChoice();
        if (v >= lastVal){
          fail("values not decreasing");
        }
        lastVal = v;
      }
    }
  }
  
  @Test
  public void testReverse(){
    if (verifyNoPropertyViolation("+listener=.test.mc.basic.CGReorderTest$ReverseListener")){
      int x = Verify.getInt(0,4);
      System.out.println(x);
    }
  }

  
  public static class ReorderListener extends ListenerAdapter {
    ChoiceGenerator<?> reorderedCG;
    
    @Override
    public void choiceGeneratorRegistered (JVM vm){
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      // make sure we are not getting recursive (could also use setId())
      if (cg instanceof DoubleChoiceFromList && cg != reorderedCG){ 
        System.out.println("reorder choices");
        reorderedCG = ((DoubleChoiceFromList)cg).reorder( new Comparator<Double>(){
          public int compare (Double d1, Double d2){
            return (int) (d2 - d1);
          }
        });
        
        System.out.println("replacing: " + cg);
        System.out.println("with: " + reorderedCG);
        SystemState ss = vm.getSystemState();
        ss.removeNextChoiceGenerator();
        ss.setNextChoiceGenerator(reorderedCG);
      }
    }

    double lastVal = Double.MAX_VALUE;
    @Override
    public void choiceGeneratorAdvanced (JVM vm){
      ChoiceGenerator<?> cg = vm.getLastChoiceGenerator();
      if (cg instanceof DoubleChoiceGenerator){
        double v = ((DoubleChoiceGenerator)cg).getNextChoice();
        if (v >= lastVal){
          fail("values not decreasing");
        }
        lastVal = v;
      }
    }
  }
  
  @Test
  public void testReorder(){
    if (verifyNoPropertyViolation("+listener=.test.mc.basic.CGReorderTest$ReorderListener")){
      double x = Verify.getDoubleFromList(1.0, 2.0, 3.0, 4.0);
      System.out.println(x);
    }
  }
  
}
