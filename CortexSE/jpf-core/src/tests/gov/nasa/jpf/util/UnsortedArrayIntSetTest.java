//
// Copyright (C) 2012 United States Government as represented by the
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
package gov.nasa.jpf.util;

import java.util.NoSuchElementException;

import org.junit.Test;

import gov.nasa.jpf.util.test.TestJPF;

/**
 * regression test for ArrayIntSet
 */
public class UnsortedArrayIntSetTest extends TestJPF {

  @Test
  public void testInsert(){
    UnsortedArrayIntSet s = new UnsortedArrayIntSet();
    s.add(42);
    s.add(43);
    s.add(41);
    s.add(42);
    s.add(0);
    
    System.out.println(s);
    
    assertTrue(s.size() == 4);
    assertTrue(s.contains(0));
    assertTrue(s.contains(41));
    assertTrue(s.contains(42));
    assertTrue(s.contains(43));
  }
  
  @Test
  public void testRemove(){
    UnsortedArrayIntSet s = new UnsortedArrayIntSet();
    s.add(42);
    assertTrue(s.size() == 1);
    assertTrue(s.contains(42));
    
    s.remove(42);
    assertFalse(s.contains(42));
    assertTrue(s.size() == 0);
    
    
    s.add(42);
    s.add(42000);
    s.add(0);
    System.out.println(s);
    
    s.remove(0); // remove last element
    assertFalse(s.contains(0));
    assertTrue(s.size() == 2);
    System.out.println(s);
    
    s.remove(42); // remove first element
    assertFalse(s.contains(42));
    assertTrue(s.size() == 1);
    System.out.println(s);
  }
  
  @Test
  public void testIterator(){
    UnsortedArrayIntSet s = new UnsortedArrayIntSet();
    s.add(1);
    s.add(2);
    s.add(3);
    
    int i=0;
    IntIterator it = s.intIterator();
    while (it.hasNext()){
      System.out.print(it.next());
      i++;
    }
    System.out.println();
    assertTrue(i == 3);
    
    assertTrue( !it.hasNext());
    try {
      it.next();
      fail("iterator failed to throw NoSuchElementException");
    } catch (NoSuchElementException nsex){
      // that's expected
    }
    
    it = s.intIterator(); // fresh one
    while (it.hasNext()){
      if (it.next() == 2){
        it.remove();
        assertTrue( s.size() == 2);
        break;
      }
    }
    i = it.next();
    assertTrue(i == 3);
    it.remove();
    assertTrue(s.size() == 1);
    assertTrue( !it.hasNext());
    
    s.add(42);
    it = s.intIterator();
    assertTrue(it.next() == 1);
    it.remove();
    assertTrue( it.next() == 42);
    it.remove();
    assertTrue( s.isEmpty());
  }

  @Test
  public void testComparison(){
    UnsortedArrayIntSet s1 = new UnsortedArrayIntSet();
    s1.add(42);
    s1.add(0);
    s1.add(41);
    
    UnsortedArrayIntSet s2 = new UnsortedArrayIntSet();
    
    assertFalse( s1.hashCode() == s2.hashCode());
    assertFalse( s1.equals(s2));
    
    s2.add(0);
    s2.add(41);
    s2.add(42);

    assertTrue( s1.hashCode() == s2.hashCode());
    assertTrue( s1.equals(s2));
    
    s2.remove(41);
    assertFalse( s1.hashCode() == s2.hashCode());
    assertFalse( s1.equals(s2));
    
    // all IntSets should hash/compare
    IntSet s3 = new SortedArrayIntSet();
    s3.add(0);
    s3.add(41);
    s3.add(42);
    
    assertTrue( s1.hashCode() == s3.hashCode());
    assertTrue( s1.equals(s3));
  }
}
