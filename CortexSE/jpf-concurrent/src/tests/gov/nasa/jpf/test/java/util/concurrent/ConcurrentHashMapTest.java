//
// Copyright (C) 2008 United States Government as represented by the
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
package gov.nasa.jpf.test.java.util.concurrent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;
import gov.nasa.jpf.jvm.Verify;
import java.lang.ref.WeakReference;

/**
 * JPF test driver for java.util.concurrent.ConcurrentHashMap
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 * @author Nathan Reynolds <nathanila@gmail.com>
 */
public class ConcurrentHashMapTest extends TestCaseHelpers {

  private final static String[] JPF_ARGS = {};

  public static void main(String[] args) {
    runTestsOfThisClass(args);
  }

  /*
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/licenses/publicdomain
   * Other contributors include Andrew Wright, Jeffrey Hayes,
   * Pat Fisher, Mike Judd.
   */
  /**
   * Create a map from Integers 1-5 to Strings "A"-"E".
   */
  private static ConcurrentHashMap map5() {
    ConcurrentHashMap map = new ConcurrentHashMap(5);
    assertTrue(map.isEmpty());
    map.put(one, "A");
    map.put(two, "B");
    map.put(three, "C");
    map.put(four, "D");
    map.put(five, "E");
    assertFalse(map.isEmpty());
    assertEquals(5, map.size());
    return map;
  }

  /**
   *  clear removes all pairs
   */
  @Test
  public void testClear() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      map.clear();
      assertEquals(map.size(), 0);
    }
    printFinish();
  }

  @Test
  public void testClear_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      Object o = new Object();
      WeakReference wr = new WeakReference(o);
      ConcurrentHashMap map = map5();
      assertEquals(o, wr.get());
      map.put("1234", o);
      o = null;                                                         // Clear all references except the one in the ConcurrentHashMap so it can be GCed.
      map.clear();

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen
      
      assertEquals(map.size(), 0);
      assertNull(wr.get());
    }
    printFinish();
  }

  /**
   *  Maps with same contents are equal
   */
  @Test
  public void testEquals() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map1 = map5();
      ConcurrentHashMap map2 = map5();
      assertEquals(map1, map2);
      assertEquals(map2, map1);
      map1.clear();
      assertFalse(map1.equals(map2));
      assertFalse(map2.equals(map1));
    }
    printFinish();
  }

  /**
   *  contains returns true for contained value
   */
  @Test
  public void testContains() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertTrue(map.contains("A"));
      assertFalse(map.contains("Z"));
    }
    printFinish();
  }

  /**
   *  containsKey returns true for contained key
   */
  @Test
  public void testContainsKey() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertTrue(map.containsKey(one));
      assertFalse(map.containsKey(zero));
    }
    printFinish();
  }

  /**
   *  containsValue returns true for held values
   */
  @Test
  public void testContainsValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertTrue(map.containsValue("A"));
      assertFalse(map.containsValue("Z"));
    }
    printFinish();
  }

  /**
   *   enumeration returns an enumeration containing the correct
   *   elements
   */
  @Test
  public void testEnumeration() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Enumeration e = map.elements();
      int count = 0;
      while (e.hasMoreElements()) {
        count++;
        e.nextElement();
      }
      assertEquals(5, count);
    }
    printFinish();
  }

  /**
   *  get returns the correct element at the given key,
   *  or null if not present
   */
  @Test
  public void testGet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertEquals("A", (String) map.get(one));
      ConcurrentHashMap empty = new ConcurrentHashMap();
      assertNull(map.get("anything"));
    }
    printFinish();
  }

  /**
   *  isEmpty is true of empty map and false for non-empty
   */
  @Test
  public void testIsEmpty() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap empty = new ConcurrentHashMap();
      ConcurrentHashMap map = map5();
      assertTrue(empty.isEmpty());
      assertFalse(map.isEmpty());
    }
    printFinish();
  }

  /**
   *   keys returns an enumeration containing all the keys from the map
   */
  @Test
  public void testKeys() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Enumeration e = map.keys();
      int count = 0;
      while (e.hasMoreElements()) {
        count++;
        e.nextElement();
      }
      assertEquals(5, count);
    }
    printFinish();
  }

  /**
   *   keySet returns a Set containing all the keys
   */
  @Test
  public void testKeySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Set s = map.keySet();
      assertEquals(5, s.size());
      assertTrue(s.contains(one));
      assertTrue(s.contains(two));
      assertTrue(s.contains(three));
      assertTrue(s.contains(four));
      assertTrue(s.contains(five));
    }
    printFinish();
  }

  /**
   *  keySet.toArray returns contains all keys
   */
  @Test
  public void testKeySetToArray() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Set s = map.keySet();
      Object[] ar = s.toArray();
      assertTrue(s.containsAll(Arrays.asList(ar)));
      assertEquals(5, ar.length);
      ar[0] = m10;
      assertFalse(s.containsAll(Arrays.asList(ar)));
    }
    printFinish();
  }

  /**
   *  Values.toArray contains all values
   */
  @Test
  public void testValuesToArray() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Collection v = map.values();
      Object[] ar = v.toArray();
      ArrayList s = new ArrayList(Arrays.asList(ar));
      assertEquals(5, ar.length);
      assertTrue(s.contains("A"));
      assertTrue(s.contains("B"));
      assertTrue(s.contains("C"));
      assertTrue(s.contains("D"));
      assertTrue(s.contains("E"));
    }
    printFinish();
  }

  /**
   *  entrySet.toArray contains all entries
   */
  @Test
  public void testEntrySetToArray() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Set s = map.entrySet();
      Object[] ar = s.toArray();
      assertEquals(5, ar.length);
      for (int i = 0; i < 5; ++i) {
        assertTrue(map.containsKey(((Map.Entry) (ar[i])).getKey()));
        assertTrue(map.containsValue(((Map.Entry) (ar[i])).getValue()));
      }
    }
    printFinish();
  }

  /**
   * values collection contains all values
   */
  @Test
  public void testValues() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Collection s = map.values();
      assertEquals(5, s.size());
      assertTrue(s.contains("A"));
      assertTrue(s.contains("B"));
      assertTrue(s.contains("C"));
      assertTrue(s.contains("D"));
      assertTrue(s.contains("E"));
    }
    printFinish();
  }

  /**
   * entrySet contains all pairs
   */
  @Test
  public void testEntrySet() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Set s = map.entrySet();
      assertEquals(5, s.size());
      Iterator it = s.iterator();
      while (it.hasNext()) {
        Map.Entry e = (Map.Entry) it.next();
        assertTrue(
                (e.getKey().equals(one) && e.getValue().equals("A")) ||
                (e.getKey().equals(two) && e.getValue().equals("B")) ||
                (e.getKey().equals(three) && e.getValue().equals("C")) ||
                (e.getKey().equals(four) && e.getValue().equals("D")) ||
                (e.getKey().equals(five) && e.getValue().equals("E")));
      }
    }
    printFinish();
  }

  /**
   *   putAll  adds all key-value pairs from the given map
   */
  @Test
  public void testPutAll() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap empty = new ConcurrentHashMap();
      ConcurrentHashMap map = map5();
      empty.putAll(map);
      assertEquals(5, empty.size());
      assertTrue(empty.containsKey(one));
      assertTrue(empty.containsKey(two));
      assertTrue(empty.containsKey(three));
      assertTrue(empty.containsKey(four));
      assertTrue(empty.containsKey(five));
    }
    printFinish();
  }

  /**
   *   putIfAbsent works when the given key is not present
   */
  @Test
  public void testPutIfAbsent() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      map.putIfAbsent(six, "Z");
      assertTrue(map.containsKey(six));
    }
    printFinish();
  }

  /**
   *   putIfAbsent does not add the pair if the key is already present
   */
  @Test
  public void testPutIfAbsent2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertEquals("A", map.putIfAbsent(one, "Z"));
    }
    printFinish();
  }

  /**
   *   replace fails when the given key is not present
   */
  @Test
  public void testReplace() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertNull(map.replace(six, "Z"));
      assertFalse(map.containsKey(six));
    }
    printFinish();
  }

  /**
   *   replace succeeds if the key is already present
   */
  @Test
  public void testReplace2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertNotNull(map.replace(one, "Z"));
      assertEquals("Z", map.get(one));
    }
    printFinish();
  }

  @Test
  public void testReplace2_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Object o = new Object();
      WeakReference wr = new WeakReference(o);
      map.put("12345", o);
      o = null;                                                         // Clear all references except the one in the ConcurrentHashMap so it can be GCed.
      map.replace("12345", new Object());

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assertNull(wr.get());
    }
    printFinish();
  }

  /**
   * replace value fails when the given key not mapped to expected value
   */
  @Test
  public void testReplaceValue() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertEquals("A", map.get(one));
      assertFalse(map.replace(one, "Z", "Z"));
      assertEquals("A", map.get(one));
    }
    printFinish();
  }

  /**
   * replace value succeeds when the given key mapped to expected value
   */
  @Test
  public void testReplaceValue2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      assertEquals("A", map.get(one));
      assertTrue(map.replace(one, "A", "Z"));
      assertEquals("Z", map.get(one));
    }
    printFinish();
  }

  /**
   *   remove removes the correct key-value pair from the map
   */
  @Test
  public void testRemove() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      map.remove(five);
      assertEquals(4, map.size());
      assertFalse(map.containsKey(five));
    }
    printFinish();
  }

  @Test
  public void testRemove_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Object o = new Object();
      WeakReference wr = new WeakReference(o);
      map.put("12345", o);
      map.remove("12345");
      o = null;                                                         // Clear all references except the one in the ConcurrentHashMap so it can be GCed.

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assertNull(wr.get());
    }
    printFinish();
  }

  /**
   * remove(key,value) removes only if pair present
   */
  @Test
  public void testRemove2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      map.remove(five, "E");
      assertEquals(4, map.size());
      assertFalse(map.containsKey(five));
      map.remove(four, "A");
      assertEquals(4, map.size());
      assertTrue(map.containsKey(four));
    }
    printFinish();
  }

  /**
   *   size returns the correct values
   */
  @Test
  public void testSize() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      ConcurrentHashMap empty = new ConcurrentHashMap();
      assertEquals(0, empty.size());
      assertEquals(5, map.size());
    }
    printFinish();
  }

  /**
   * toString contains toString of elements
   */
  @Test
  public void testToString() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      String s = map.toString();
      for (int i = 1; i <= 5; ++i) {
        assertTrue(s.indexOf(String.valueOf(i)) >= 0);
      }
    }
    printFinish();
  }

  // Exception tests
  /**
   * Cannot create with negative capacity
   */
  @Test
  public void testConstructor1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new ConcurrentHashMap(-1, 0, 1);
        shouldThrow();
      } catch (IllegalArgumentException e) {
      }
    }
    printFinish();
  }

  /**
   * Cannot create with negative concurrency level
   */
  @Test
  public void testConstructor2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new ConcurrentHashMap(1, 0, -1);
        shouldThrow();
      } catch (IllegalArgumentException e) {
      }
    }
    printFinish();
  }

  /**
   * Cannot create with only negative capacity
   */
  @Test
  public void testConstructor3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        new ConcurrentHashMap(-1);
        shouldThrow();
      } catch (IllegalArgumentException e) {
      }
    }
    printFinish();
  }

  /**
   * get(null) throws NPE
   */
  @Test
  public void testGet_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.get(null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * containsKey(null) throws NPE
   */
  @Test
  public void testContainsKey_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.containsKey(null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * containsValue(null) throws NPE
   */
  @Test
  public void testContainsValue_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.containsValue(null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * contains(null) throws NPE
   */
  @Test
  public void testContains_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.contains(null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * put(null,x) throws NPE
   */
  @Test
  public void testPut1_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.put(null, "whatever");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * put(x, null) throws NPE
   */
  @Test
  public void testPut2_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.put("whatever", null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  @Test
  public void testPut_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Object o = new Object();
      WeakReference wr = new WeakReference(o);
      map.put("12345", o);
      o = null;                                                         // Clear all references except the one in the ConcurrentHashMap so it can be GCed.

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assertNotNull(wr.get());
    }
    printFinish();
  }

  /**
   * putIfAbsent(null, x) throws NPE
   */
  @Test
  public void testPutIfAbsent1_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.putIfAbsent(null, "whatever");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * replace(null, x) throws NPE
   */
  @Test
  public void testReplace_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.replace(null, "whatever");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  @Test
  public void testReplace_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Object o1 = new Object();
      Object o2 = new Object();
      WeakReference wr1 = new WeakReference(o1);
      WeakReference wr2 = new WeakReference(o2);
      map.put("12345", o1);
      map.replace("12345", o2);
      o1 = null;                                                        // Clear all references except the one in the ConcurrentHashMap so it can be GCed.
      o2 = null;                                                        // Clear all references except the one in the ConcurrentHashMap so it can be GCed.

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assertNull(wr1.get());
      assertNotNull(wr2.get());
    }
    printFinish();
  }

  /**
   * replace(null, x, y) throws NPE
   */
  @Test
  public void testReplaceValue_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.replace(null, one, "whatever");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * putIfAbsent(x, null) throws NPE
   */
  @Test
  public void testPutIfAbsent2_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.putIfAbsent("whatever", null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * replace(x, null) throws NPE
   */
  @Test
  public void testReplace2_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.replace("whatever", null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * replace(x, null, y) throws NPE
   */
  @Test
  public void testReplaceValue2_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.replace("whatever", null, "A");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * replace(x, y, null) throws NPE
   */
  @Test
  public void testReplaceValue3_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.replace("whatever", one, null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * remove(null) throws NPE
   */
  @Test
  public void testRemove1_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.put("sadsdf", "asdads");
        c.remove(null);
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * remove(null, x) throws NPE
   */
  @Test
  public void testRemove2_NullPointerException() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.put("sadsdf", "asdads");
        c.remove(null, "whatever");
        shouldThrow();
      } catch (NullPointerException e) {
      }
    }
    printFinish();
  }

  /**
   * remove(x, null) returns false
   */
  @Test
  public void testRemove3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      try {
        ConcurrentHashMap c = new ConcurrentHashMap(5);
        c.put("sadsdf", "asdads");
        assertFalse(c.remove("sadsdf", null));
      } catch (NullPointerException e) {
        fail();
      }
    }
    printFinish();
  }

  /**
   * A deserialized map equals original
   */
  //@Test
  public void testSerialization() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap q = map5();

      try {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(q);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        ConcurrentHashMap r = (ConcurrentHashMap) in.readObject();
        assertEquals(q.size(), r.size());
        assertTrue(q.equals(r));
        assertTrue(r.equals(q));
      } catch (Exception e) {
        e.printStackTrace();
        unexpectedException();
      }
    }
    printFinish();
  }

  /**
   * SetValue of an EntrySet entry sets value in the map.
   */
  @Test
  public void testSetValueWriteThrough() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      // Adapted from a bug report by Eric Zoerner
      ConcurrentHashMap map = new ConcurrentHashMap(2, 5.0f, 1);
      assertTrue(map.isEmpty());
      for (int i = 0; i < 20; i++) {
        map.put(new Integer(i), new Integer(i));
      }
      assertFalse(map.isEmpty());
      Map.Entry entry1 = (Map.Entry) map.entrySet().iterator().next();

      // assert that entry1 is not 16
      assertTrue("entry is 16, test not valid",
              !entry1.getKey().equals(new Integer(16)));

      // remove 16 (a different key) from map
      // which just happens to cause entry1 to be cloned in map
      map.remove(new Integer(16));
      entry1.setValue("XYZ");
      assertTrue(map.containsValue("XYZ")); // fails
    }
    printFinish();
  }

  /**
   * Testing iterators shouldn't be here but as we introduced new iterators is seems
   * reasonable to add new tests.
   *
   * If iterator goes through whole collection there is no element to remove;
   */
  @Test
  public void testIterator1() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Iterator<Map.Entry<Integer,String>> i = map.entrySet().iterator();
      try {
        while(i.hasNext()) i.next();
        i.next();
        shouldThrow();
      }catch(NoSuchElementException success) {}
    }
    printFinish();
  }

  /**
   *
   * We cannot remove element from not initialized iterator
   */
  @Test
  public void testIterator2() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Iterator<Map.Entry<Integer,String>> i = map.entrySet().iterator();
      try {
        i.remove();
        shouldThrow();
      }catch(IllegalStateException success) {}
    }
    printFinish();
  }

  /**
   *
   * Removing element using iterator, removes also from map.
   */
  @Test
  public void testIterator3() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Iterator<Map.Entry<Integer,String>> i = map.entrySet().iterator();
      Map.Entry<Integer,String> e = i.next();
      assertTrue(map.containsKey(e.getKey()));
      i.remove();
      assertFalse(map.containsKey(e.getKey()));
    }
    printFinish();
  }

  /**
   *
   * The same for values collection.
   */
  @Test
  public void testIterator4() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = map5();
      Iterator<String> i = map.values().iterator();
      String s = i.next();
      assertTrue(map.containsValue(s));
      i.remove();
      assertFalse(map.containsValue(s));
    }
    printFinish();
  }

  @Test
  public void testIterator_NoLeak() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap map = new ConcurrentHashMap();
      Object o1 = new Object();
      WeakReference wr = new WeakReference(o1);
      map.put("12345", o1);
      Iterator<Object> i = map.values().iterator();
      Object o2 = i.next();
      assertEquals(o1, o2);
      i.remove();
      o1 = null;                                                        // Clear all references except the one in the ConcurrentHashMap so it can be GCed.
      o2 = null;

      System.gc();                                                      // Set the GCNeeded flag
      Verify.getBoolean();                                              // Cause the state to be examined and hence GC to happen

      assertNull(wr.get());
    }
    printFinish();
  }
  
  @Test
  public void testCheckCast() {
    if (verifyNoPropertyViolation(JPF_ARGS)) {
      ConcurrentHashMap<ArrayList, LinkedList<Integer>> map;
      ArrayList key;
      LinkedList<Integer> value;
  
      map   = new ConcurrentHashMap<ArrayList, LinkedList<Integer>>();
      key   = new ArrayList();
      value = new LinkedList<Integer>();
      
      value.add(1);
      map.put(key, value);
      
      value = null;             // Clear all references except the one in the ConcurrentHashMap so it can be GCed.
      
      System.gc();              // Set the GCNeeded flag
      Verify.getBoolean();      // Cause the state to be examined and hence the gc to happen
      
      value = map.get(key);
      
      assertEquals(1, value.size());
      assertEquals(1, (int) value.getFirst());
    }
    printFinish();
  }
}

