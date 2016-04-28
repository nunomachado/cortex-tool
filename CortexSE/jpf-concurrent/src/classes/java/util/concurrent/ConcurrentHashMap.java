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
package java.util.concurrent;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/*
 * Implements logic for ConcurrentHashMap.
 *
 * @author Mateusz Ujma <mateusz.ujma@gmail.com>
 *
 */
public class ConcurrentHashMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V>, Serializable {

  private int version = 0;

  /*
   * Values for loadFactor and concurrentLevel are not important for us as JPF model
   * will be running in one thread environment.
   */

  public ConcurrentHashMap() {
    this(1, 2, 3);
  }

  public ConcurrentHashMap(int initialCapacity) {
    this(initialCapacity, 2, 3);
  }

  public ConcurrentHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, 3);
    if (initialCapacity < 0 || loadFactor <= 0) {
      throw new IllegalArgumentException();
    }
    newMap(initialCapacity);
  }

  public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
    super();
    if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) {
      throw new IllegalArgumentException();
    }
    newMap(initialCapacity);
  }

  public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
    this(1, 2, 3);
    putAll(m);
  }

  public native void newMap();

  public native void newMap(int initicalCapacity);

  public native boolean isEmpty();

  public native int size();

  public native V get(Object key);

  public native boolean containsKey(Object key);

  public native boolean containsValue(Object value);

  public native boolean contains(Object value);

  public native V put(K key, V value);

  public native V putIfAbsent(K key, V value);

  /*
   * putAll cannot be native because we would have to deal with internals
   * of many different Map implementations which is hard.
   */

  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  public native V remove(Object key);

  public native boolean remove(Object key, Object value);

  public native boolean replace(K key, V oldValue, V newValue);

  public native V replace(K key, V value);

  public native void clear();

  /*
   * keySet(),entrySet(),keys(), ... returns objects which methods can affect
   * internal state of the map object. Implementing them in native would meant
   * that on MJI level we would have to change parent (Map) when someone calls
   * remove on child(KeySet) which is very hard on MJI level and very simple on
     non-MJI level when using ConcurrentHashMap.this reference.
   *
  */

  public Set<K> keySet() {
    return new KeySet();
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  public Enumeration<K> keys() {
    return new KeyIterator();
  }

  public Enumeration<V> elements() {
    return new ValueIterator();
  }

  public Collection<V> values() {
    return new Values();
  }

  private native boolean hasNextEntry(int pos);

  private native Map.Entry<K, V> nextEntry(int pos);

  private native void removeEntry(int pos);

  final class KeySet extends AbstractSet<K> {

    public Iterator<K> iterator() {
      return new KeyIterator();
    }

    public int size() {
      return ConcurrentHashMap.this.size();
    }

    public boolean isEmpty() {
      return ConcurrentHashMap.this.isEmpty();
    }

    public boolean contains(Object o) {
      return ConcurrentHashMap.this.containsKey(o);
    }

    public boolean remove(Object o) {
      return ConcurrentHashMap.this.remove(o) != null;
    }

    public void clear() {
      ConcurrentHashMap.this.clear();
    }
  }

  final class Values extends AbstractCollection<V> {

    public Iterator<V> iterator() {
      return new ValueIterator();
    }

    public int size() {
      return ConcurrentHashMap.this.size();
    }

    public boolean isEmpty() {
      return ConcurrentHashMap.this.isEmpty();
    }

    public boolean contains(Object o) {
      return ConcurrentHashMap.this.containsValue(o);
    }

    public void clear() {
      ConcurrentHashMap.this.clear();
    }
  }

  final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      V v = ConcurrentHashMap.this.get(e.getKey());
      return v != null && v.equals(e.getValue());
    }

    public boolean remove(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      return ConcurrentHashMap.this.remove(e.getKey(), e.getValue());
    }

    public int size() {
      return ConcurrentHashMap.this.size();
    }

    public boolean isEmpty() {
      return ConcurrentHashMap.this.isEmpty();
    }

    public void clear() {
      ConcurrentHashMap.this.clear();
    }
  }

  final class KeyIterator extends BaseIterator implements Iterator<K>, Enumeration<K> {

    public K next() {
      return nextEntry(pos++).getKey();
    }

    public K nextElement() {
      return next();
    }
  }

  final class ValueIterator extends BaseIterator implements Iterator<V>, Enumeration<V> {

    public V next() {
      return nextEntry(pos++).getValue();
    }

    public V nextElement() {
      return next();
    }
  }

  final class EntryIterator extends BaseIterator implements Iterator<Map.Entry<K, V>> {

    public Map.Entry<K, V> next() {
      return nextEntry(pos++);
    }

  }

  abstract class BaseIterator {
    protected int pos = 0;

    public boolean hasNext() {
      return hasNextEntry(pos);
    }

    public void remove() {
      removeEntry(pos);
    }

    public boolean hasMoreElements() {
      return hasNext();
    }
  }
}
