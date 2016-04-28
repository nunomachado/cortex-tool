package gov.nasa.jpf.util;

import java.util.NoSuchElementException;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

public class SortedArrayIntSetTest extends TestJPF {

  @Test
  public void testInsert(){
    SortedArrayIntSet s = new SortedArrayIntSet();
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
    SortedArrayIntSet s = new SortedArrayIntSet();
    s.add(42);
    assertTrue(s.size() == 1);
    assertTrue(s.contains(42));
    
    s.remove(42);
    assertFalse(s.contains(42));
    assertTrue(s.size() == 0);
    
    s.add(42);
    s.add(42000);
    s.add(0);
    assertTrue(s.size() == 3);
    s.remove(42000);
    assertTrue(s.size() == 2);
    assertFalse(s.contains(42000));
    s.remove(0);
    assertFalse(s.contains(0));
    assertTrue(s.size() == 1);
  }
  
  @Test
  public void testIterator(){
    SortedArrayIntSet s = new SortedArrayIntSet();
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
    SortedArrayIntSet s1 = new SortedArrayIntSet();
    s1.add(42);
    s1.add(0);
    s1.add(41);
    
    SortedArrayIntSet s2 = new SortedArrayIntSet();
    
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
    IntSet s3 = new UnsortedArrayIntSet();
    s3.add(0);
    s3.add(41);
    s3.add(42);
    
    assertTrue( s1.hashCode() == s3.hashCode());
    assertTrue( s1.equals(s3));
  }
}
