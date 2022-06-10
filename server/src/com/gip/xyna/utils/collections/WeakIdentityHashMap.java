/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.utils.collections;



import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;



/*
 * kopiert + leichte änderungen von WeakHashMap:
 * - HashMap.hash() ersetzt durch System.identityHashCode()
 * - memberVariablen values und keySet ergänzt, weil auf packageprivate members von parentklasse zugegriffen wurde
 * - AbstractMap.SimpleEntry als innere klasse eingebaut, weil in 1.5 private
 */
public class WeakIdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

  /**
   * The default initial capacity -- MUST be a power of two.
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 16;

  /**
   * The maximum capacity, used if a higher value is implicitly specified
   * by either of the constructors with arguments.
   * MUST be a power of two &lt;= 1&lt;&lt;30.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * The load fast used when none specified in constructor.
   */
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /**
   * The table, resized as necessary. Length MUST Always be a power of two.
   */
  private Entry[] table;

  /**
   * The number of key-value mappings contained in this weak hash map.
   */
  private int size;

  /**
   * The next size value at which to resize (capacity * load factor).
   */
  private int threshold;

  /**
   * The load factor for the hash table.
   */
  private final float loadFactor;

  /**
   * Reference queue for cleared WeakEntries
   */
  private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

  /**
   * The number of times this WeakHashMap has been structurally modified.
   * Structural modifications are those that change the number of
   * mappings in the map or otherwise modify its internal structure
   * (e.g., rehash).  This field is used to make iterators on
   * Collection-views of the map fail-fast.
   *
   * @see ConcurrentModificationException
   */
  private volatile int modCount;


  /**
   * Constructs a new, empty <code>WeakHashMap</code> with the given initial
   * capacity and the given load factor.
   *
   * @param  initialCapacity The initial capacity of the <code>WeakHashMap</code>
   * @param  loadFactor      The load factor of the <code>WeakHashMap</code>
   * @throws IllegalArgumentException if the initial capacity is negative,
   *         or if the load factor is nonpositive.
   */
  public WeakIdentityHashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
      initialCapacity = MAXIMUM_CAPACITY;

    if (loadFactor <= 0 || Float.isNaN(loadFactor))
      throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
    int capacity = 1;
    while (capacity < initialCapacity)
      capacity <<= 1;
    table = new Entry[capacity];
    this.loadFactor = loadFactor;
    threshold = (int) (capacity * loadFactor);
  }


  /**
   * Constructs a new, empty <code>WeakHashMap</code> with the given initial
   * capacity and the default load factor (0.75).
   *
   * @param  initialCapacity The initial capacity of the <code>WeakHashMap</code>
   * @throws IllegalArgumentException if the initial capacity is negative
   */
  public WeakIdentityHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }


  /**
   * Constructs a new, empty <code>WeakHashMap</code> with the default initial
   * capacity (16) and load factor (0.75).
   */
  public WeakIdentityHashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    threshold = (int) (DEFAULT_INITIAL_CAPACITY);
    table = new Entry[DEFAULT_INITIAL_CAPACITY];
  }


  /**
   * Constructs a new <code>WeakHashMap</code> with the same mappings as the
   * specified map.  The <code>WeakHashMap</code> is created with the default
   * load factor (0.75) and an initial capacity sufficient to hold the
   * mappings in the specified map.
   *
   * @param   m the map whose mappings are to be placed in this map
   * @throws  NullPointerException if the specified map is null
   * @since 1.3
   */
  public WeakIdentityHashMap(Map<? extends K, ? extends V> m) {
    this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1, 16), DEFAULT_LOAD_FACTOR);
    putAll(m);
  }


  // internal utilities

  /**
   * Value representing null keys inside tables.
   */
  private static final Object NULL_KEY = new Object();


  /**
   * Use NULL_KEY for key if it is null.
   */
  private static Object maskNull(Object key) {
    return (key == null ? NULL_KEY : key);
  }


  /**
   * Returns internal representation of null key back to caller as null.
   */
  private static <K> K unmaskNull(Object key) {
    return (K) (key == NULL_KEY ? null : key);
  }


  /**
   * Checks for equality of non-null reference x and possibly-null y.  By
   * default uses Object.equals.
   */
  static boolean eq(Object x, Object y) {
    return x == y || x.equals(y);
  }


  /**
   * Returns index for hash code h.
   */
  static int indexFor(int h, int length) {
    return h & (length - 1);
  }


  /**
   * Expunges stale entries from the table.
   */
  private void expungeStaleEntries() {
    Entry<K, V> e;
    while ((e = (Entry<K, V>) queue.poll()) != null) {
      int h = e.hash;
      int i = indexFor(h, table.length);

      Entry<K, V> prev = table[i];
      Entry<K, V> p = prev;
      while (p != null) {
        Entry<K, V> next = p.next;
        if (p == e) {
          if (prev == e)
            table[i] = next;
          else
            prev.next = next;
          e.next = null; // Help GC
          e.value = null; //  "   "
          size--;
          break;
        }
        prev = p;
        p = next;
      }
    }
  }


  /**
   * Returns the table after first expunging stale entries.
   */
  private Entry[] getTable() {
    expungeStaleEntries();
    return table;
  }


  /**
   * Returns the number of key-value mappings in this map.
   * This result is a snapshot, and may not reflect unprocessed
   * entries that will be removed before next attempted access
   * because they are no longer referenced.
   */
  public int size() {
    if (size == 0)
      return 0;
    expungeStaleEntries();
    return size;
  }


  /**
   * Returns <code>true</code> if this map contains no key-value mappings.
   * This result is a snapshot, and may not reflect unprocessed
   * entries that will be removed before next attempted access
   * because they are no longer referenced.
   */
  public boolean isEmpty() {
    return size() == 0;
  }


  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   *
   * <p>A return value of {@code null} does not <i>necessarily</i>
   * indicate that the map contains no mapping for the key; it's also
   * possible that the map explicitly maps the key to {@code null}.
   * The {@link #containsKey containsKey} operation may be used to
   * distinguish these two cases.
   *
   * @see #put(Object, Object)
   */
  public V get(Object key) {
    Object k = maskNull(key);
    int h = System.identityHashCode(k);
    Entry[] tab = getTable();
    int index = indexFor(h, tab.length);
    Entry<K, V> e = tab[index];
    while (e != null) {
      if (e.hash == h && eq(k, e.get()))
        return e.value;
      e = e.next;
    }
    return null;
  }


  /**
   * Returns <code>true</code> if this map contains a mapping for the
   * specified key.
   *
   * @param  key   The key whose presence in this map is to be tested
   * @return <code>true</code> if there is a mapping for <code>key</code>;
   *         <code>false</code> otherwise
   */
  public boolean containsKey(Object key) {
    return getEntry(key) != null;
  }


  /**
   * Returns the entry associated with the specified key in this map.
   * Returns null if the map contains no mapping for this key.
   */
  Entry<K, V> getEntry(Object key) {
    Object k = maskNull(key);
    int h = System.identityHashCode(k);
    Entry[] tab = getTable();
    int index = indexFor(h, tab.length);
    Entry<K, V> e = tab[index];
    while (e != null && !(e.hash == h && eq(k, e.get())))
      e = e.next;
    return e;
  }


  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for this key, the old
   * value is replaced.
   *
   * @param key key with which the specified value is to be associated.
   * @param value value to be associated with the specified key.
   * @return the previous value associated with <code>key</code>, or
   *         <code>null</code> if there was no mapping for <code>key</code>.
   *         (A <code>null</code> return can also indicate that the map
   *         previously associated <code>null</code> with <code>key</code>.)
   */
  public V put(K key, V value) {
    K k = (K) maskNull(key);
    int h = System.identityHashCode(k);
    Entry[] tab = getTable();
    int i = indexFor(h, tab.length);

    for (Entry<K, V> e = tab[i]; e != null; e = e.next) {
      if (h == e.hash && eq(k, e.get())) {
        V oldValue = e.value;
        if (value != oldValue)
          e.value = value;
        return oldValue;
      }
    }

    modCount++;
    Entry<K, V> e = tab[i];
    tab[i] = new Entry<K, V>(k, value, queue, h, e);
    if (++size >= threshold)
      resize(tab.length * 2);
    return null;
  }


  /**
   * Rehashes the contents of this map into a new array with a
   * larger capacity.  This method is called automatically when the
   * number of keys in this map reaches its threshold.
   *
   * If current capacity is MAXIMUM_CAPACITY, this method does not
   * resize the map, but sets threshold to Integer.MAX_VALUE.
   * This has the effect of preventing future calls.
   *
   * @param newCapacity the new capacity, MUST be a power of two;
   *        must be greater than current capacity unless current
   *        capacity is MAXIMUM_CAPACITY (in which case value
   *        is irrelevant).
   */
  void resize(int newCapacity) {
    Entry[] oldTable = getTable();
    int oldCapacity = oldTable.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {
      threshold = Integer.MAX_VALUE;
      return;
    }

    Entry[] newTable = new Entry[newCapacity];
    transfer(oldTable, newTable);
    table = newTable;

    /*
     * If ignoring null elements and processing ref queue caused massive
     * shrinkage, then restore old table.  This should be rare, but avoids
     * unbounded expansion of garbage-filled tables.
     */
    if (size >= threshold / 2) {
      threshold = (int) (newCapacity * loadFactor);
    } else {
      expungeStaleEntries();
      transfer(newTable, oldTable);
      table = oldTable;
    }
  }


  /** Transfers all entries from src to dest tables */
  private void transfer(Entry[] src, Entry[] dest) {
    for (int j = 0; j < src.length; ++j) {
      Entry<K, V> e = src[j];
      src[j] = null;
      while (e != null) {
        Entry<K, V> next = e.next;
        Object key = e.get();
        if (key == null) {
          e.next = null; // Help GC
          e.value = null; //  "   "
          size--;
        } else {
          int i = indexFor(e.hash, dest.length);
          e.next = dest[i];
          dest[i] = e;
        }
        e = next;
      }
    }
  }


  /**
   * Copies all of the mappings from the specified map to this map.
   * These mappings will replace any mappings that this map had for any
   * of the keys currently in the specified map.
   *
   * @param m mappings to be stored in this map.
   * @throws  NullPointerException if the specified map is null.
   */
  public void putAll(Map<? extends K, ? extends V> m) {
    int numKeysToBeAdded = m.size();
    if (numKeysToBeAdded == 0)
      return;

    /*
     * Expand the map if the map if the number of mappings to be added
     * is greater than or equal to threshold.  This is conservative; the
     * obvious condition is (m.size() + size) >= threshold, but this
     * condition could result in a map with twice the appropriate capacity,
     * if the keys to be added overlap with the keys already in this map.
     * By using the conservative calculation, we subject ourself
     * to at most one extra resize.
     */
    if (numKeysToBeAdded > threshold) {
      int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
      if (targetCapacity > MAXIMUM_CAPACITY)
        targetCapacity = MAXIMUM_CAPACITY;
      int newCapacity = table.length;
      while (newCapacity < targetCapacity)
        newCapacity <<= 1;
      if (newCapacity > table.length)
        resize(newCapacity);
    }

    for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
      put(e.getKey(), e.getValue());
  }


  /**
   * Removes the mapping for a key from this weak hash map if it is present.
   * More formally, if this map contains a mapping from key <code>k</code> to
   * value <code>v</code> such that <code>(key==null ?  k==null :
   * key.equals(k))</code>, that mapping is removed.  (The map can contain
   * at most one such mapping.)
   *
   * <p>Returns the value to which this map previously associated the key,
   * or <code>null</code> if the map contained no mapping for the key.  A
   * return value of <code>null</code> does not <i>necessarily</i> indicate
   * that the map contained no mapping for the key; it's also possible
   * that the map explicitly mapped the key to <code>null</code>.
   *
   * <p>The map will not contain a mapping for the specified key once the
   * call returns.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous value associated with <code>key</code>, or
   *         <code>null</code> if there was no mapping for <code>key</code>
   */
  public V remove(Object key) {
    Object k = maskNull(key);
    int h = System.identityHashCode(k);
    Entry[] tab = getTable();
    int i = indexFor(h, tab.length);
    Entry<K, V> prev = tab[i];
    Entry<K, V> e = prev;

    while (e != null) {
      Entry<K, V> next = e.next;
      if (h == e.hash && eq(k, e.get())) {
        modCount++;
        size--;
        if (prev == e)
          tab[i] = next;
        else
          prev.next = next;
        return e.value;
      }
      prev = e;
      e = next;
    }

    return null;
  }


  /** Special version of remove needed by Entry set */
  Entry<K, V> removeMapping(Object o) {
    if (!(o instanceof Map.Entry))
      return null;
    Entry[] tab = getTable();
    Map.Entry entry = (Map.Entry) o;
    Object k = maskNull(entry.getKey());
    int h = System.identityHashCode(k);
    int i = indexFor(h, tab.length);
    Entry<K, V> prev = tab[i];
    Entry<K, V> e = prev;

    while (e != null) {
      Entry<K, V> next = e.next;
      if (h == e.hash && e.equals(entry)) {
        modCount++;
        size--;
        if (prev == e)
          tab[i] = next;
        else
          prev.next = next;
        return e;
      }
      prev = e;
      e = next;
    }

    return null;
  }


  /**
   * Removes all of the mappings from this map.
   * The map will be empty after this call returns.
   */
  public void clear() {
    // clear out ref queue. We don't need to expunge entries
    // since table is getting cleared.
    while (queue.poll() != null);

    modCount++;
    Entry[] tab = table;
    for (int i = 0; i < tab.length; ++i)
      tab[i] = null;
    size = 0;

    // Allocation of array may have caused GC, which may have caused
    // additional entries to go stale.  Removing these entries from the
    // reference queue will make them eligible for reclamation.
    while (queue.poll() != null);
  }


  /**
   * Returns <code>true</code> if this map maps one or more keys to the
   * specified value.
   *
   * @param value value whose presence in this map is to be tested
   * @return <code>true</code> if this map maps one or more keys to the
   *         specified value
   */
  public boolean containsValue(Object value) {
    if (value == null)
      return containsNullValue();

    Entry[] tab = getTable();
    for (int i = tab.length; i-- > 0;)
      for (Entry e = tab[i]; e != null; e = e.next)
        if (value.equals(e.value))
          return true;
    return false;
  }


  /**
   * Special-case code for containsValue with null argument
   */
  private boolean containsNullValue() {
    Entry[] tab = getTable();
    for (int i = tab.length; i-- > 0;)
      for (Entry e = tab[i]; e != null; e = e.next)
        if (e.value == null)
          return true;
    return false;
  }


  /**
   * The entries in this hash table extend WeakReference, using its main ref
   * field as the key.
   */
  private static class Entry<K, V> extends WeakReference<K> implements Map.Entry<K, V> {

    private V value;
    private final int hash;
    private Entry<K, V> next;


    /**
     * Creates new entry.
     */
    Entry(K key, V value, ReferenceQueue<K> queue, int hash, Entry<K, V> next) {
      super(key, queue);
      this.value = value;
      this.hash = hash;
      this.next = next;
    }


    public K getKey() {
      return WeakIdentityHashMap.<K> unmaskNull(get());
    }


    public V getValue() {
      return value;
    }


    public V setValue(V newValue) {
      V oldValue = value;
      value = newValue;
      return oldValue;
    }


    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      Map.Entry e = (Map.Entry) o;
      Object k1 = getKey();
      Object k2 = e.getKey();
      if (k1 == k2 || (k1 != null && k1.equals(k2))) {
        Object v1 = getValue();
        Object v2 = e.getValue();
        if (v1 == v2 || (v1 != null && v1.equals(v2)))
          return true;
      }
      return false;
    }


    public int hashCode() {
      Object k = getKey();
      Object v = getValue();
      return ((k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode()));
    }


    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  private abstract class HashIterator<T> implements Iterator<T> {

    int index;
    Entry<K, V> entry = null;
    Entry<K, V> lastReturned = null;
    int expectedModCount = modCount;

    /**
     * Strong reference needed to avoid disappearance of key
     * between hasNext and next
     */
    Object nextKey = null;

    /**
     * Strong reference needed to avoid disappearance of key
     * between nextEntry() and any use of the entry
     */
    Object currentKey = null;


    HashIterator() {
      index = (size() != 0 ? table.length : 0);
    }


    public boolean hasNext() {
      Entry[] t = table;

      while (nextKey == null) {
        Entry<K, V> e = entry;
        int i = index;
        while (e == null && i > 0)
          e = t[--i];
        entry = e;
        index = i;
        if (e == null) {
          currentKey = null;
          return false;
        }
        nextKey = e.get(); // hold on to key in strong ref
        if (nextKey == null)
          entry = entry.next;
      }
      return true;
    }


    /** The common parts of next() across different types of iterators */
    protected Entry<K, V> nextEntry() {
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
      if (nextKey == null && !hasNext())
        throw new NoSuchElementException();

      lastReturned = entry;
      entry = entry.next;
      currentKey = nextKey;
      nextKey = null;
      return lastReturned;
    }


    public void remove() {
      if (lastReturned == null)
        throw new IllegalStateException();
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();

      WeakIdentityHashMap.this.remove(currentKey);
      expectedModCount = modCount;
      lastReturned = null;
      currentKey = null;
    }

  }

  private class ValueIterator extends HashIterator<V> {

    public V next() {
      return nextEntry().value;
    }
  }

  private class KeyIterator extends HashIterator<K> {

    public K next() {
      return nextEntry().getKey();
    }
  }

  private class EntryIterator extends HashIterator<Map.Entry<K, V>> {

    public Map.Entry<K, V> next() {
      return nextEntry();
    }
  }


  // Views

  private transient Set<Map.Entry<K, V>> entrySet = null;


  /**
   * Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <code>remove</code> operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <code>Iterator.remove</code>, <code>Set.remove</code>,
   * <code>removeAll</code>, <code>retainAll</code>, and <code>clear</code>
   * operations.  It does not support the <code>add</code> or <code>addAll</code>
   * operations.
   */
  public Set<K> keySet() {
    Set<K> ks = keySet;
    return (ks != null ? ks : (keySet = new KeySet()));
  }


  private class KeySet extends AbstractSet<K> {

    public Iterator<K> iterator() {
      return new KeyIterator();
    }


    public int size() {
      return WeakIdentityHashMap.this.size();
    }


    public boolean contains(Object o) {
      return containsKey(o);
    }


    public boolean remove(Object o) {
      if (containsKey(o)) {
        WeakIdentityHashMap.this.remove(o);
        return true;
      } else
        return false;
    }


    public void clear() {
      WeakIdentityHashMap.this.clear();
    }
  }


  //aus abstractmap kopiert. sollte für die verwendung hier keinen unterschied machen
  transient volatile Set<K> keySet = null;
  transient volatile Collection<V> values = null;

  /**
   * Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <code>remove</code> operation),
   * the results of the iteration are undefined.  The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <code>Iterator.remove</code>,
   * <code>Collection.remove</code>, <code>removeAll</code>,
   * <code>retainAll</code> and <code>clear</code> operations.  It does not
   * support the <code>add</code> or <code>addAll</code> operations.
   */
  public Collection<V> values() {
    Collection<V> vs = values;
    return (vs != null ? vs : (values = new Values()));
  }


  private class Values extends AbstractCollection<V> {

    public Iterator<V> iterator() {
      return new ValueIterator();
    }


    public int size() {
      return WeakIdentityHashMap.this.size();
    }


    public boolean contains(Object o) {
      return containsValue(o);
    }


    public void clear() {
      WeakIdentityHashMap.this.clear();
    }
  }


  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <code>remove</code> operation, or through the
   * <code>setValue</code> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined.  The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <code>Iterator.remove</code>,
   * <code>Set.remove</code>, <code>removeAll</code>, <code>retainAll</code> and
   * <code>clear</code> operations.  It does not support the
   * <code>add</code> or <code>addAll</code> operations.
   */
  public Set<Map.Entry<K, V>> entrySet() {
    Set<Map.Entry<K, V>> es = entrySet;
    return es != null ? es : (entrySet = new EntrySet());
  }


  private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

    public Iterator<Map.Entry<K, V>> iterator() {
      return new EntryIterator();
    }


    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      Map.Entry e = (Map.Entry) o;
      Object k = e.getKey();
      Entry candidate = getEntry(e.getKey());
      return candidate != null && candidate.equals(e);
    }


    public boolean remove(Object o) {
      return removeMapping(o) != null;
    }


    public int size() {
      return WeakIdentityHashMap.this.size();
    }


    public void clear() {
      WeakIdentityHashMap.this.clear();
    }


    private List<Map.Entry<K, V>> deepCopy() {
      List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(size());
      for (Map.Entry<K, V> e : this)
        list.add(new SimpleEntry<K, V>(e));
      return list;
    }


    public Object[] toArray() {
      return deepCopy().toArray();
    }


    public <T> T[] toArray(T[] a) {
      return deepCopy().toArray(a);
    }
  }
  
  //FIXME AbstractMap.SimpleEntry ist erst seit 1.6. public, und da factory mit 1.5 kompiliert wird, gibt es hier eine lokale kopie davon

  /**
   * An Entry maintaining a key and a value.  The value may be
   * changed using the <code>setValue</code> method.  This class
   * facilitates the process of building custom map
   * implementations. For example, it may be convenient to return
   * arrays of <code>SimpleEntry</code> instances in method
   * <code>Map.entrySet().toArray</code>.
   *
   * @since 1.6
   */
  public static class SimpleEntry<K, V> implements Map.Entry<K, V>, java.io.Serializable {

    private static final long serialVersionUID = -8499721149061103585L;

    private final K key;
    private V value;


    /**
     * Creates an entry representing a mapping from the specified
     * key to the specified value.
     *
     * @param key the key represented by this entry
     * @param value the value represented by this entry
     */
    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }


    /**
     * Creates an entry representing the same mapping as the
     * specified entry.
     *
     * @param entry the entry to copy
     */
    public SimpleEntry(Map.Entry<? extends K, ? extends V> entry) {
      this.key = entry.getKey();
      this.value = entry.getValue();
    }


    /**
    * Returns the key corresponding to this entry.
    *
    * @return the key corresponding to this entry
    */
    public K getKey() {
      return key;
    }


    /**
    * Returns the value corresponding to this entry.
    *
    * @return the value corresponding to this entry
    */
    public V getValue() {
      return value;
    }


    /**
    * Replaces the value corresponding to this entry with the specified
    * value.
    *
    * @param value new value to be stored in this entry
    * @return the old value corresponding to the entry
       */
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }


    /**
     * Compares the specified object with this entry for equality.
     * Returns {@code true} if the given object is also a map entry and
     * the two entries represent the same mapping.  More formally, two
     * entries {@code e1} and {@code e2} represent the same mapping
     * if<pre>
     *   (e1.getKey()==null ?
     *    e2.getKey()==null :
     *    e1.getKey().equals(e2.getKey()))
     *   &amp;&amp;
     *   (e1.getValue()==null ?
     *    e2.getValue()==null :
     *    e1.getValue().equals(e2.getValue()))</pre>
     * This ensures that the {@code equals} method works properly across
     * different implementations of the {@code Map.Entry} interface.
     *
     * @param o object to be compared for equality with this map entry
     * @return {@code true} if the specified object is equal to this map
     *     entry
     * @see    #hashCode
     */
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry))
        return false;
      Map.Entry e = (Map.Entry) o;
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }


    /**
     * Returns the hash code value for this map entry.  The hash code
     * of a map entry {@code e} is defined to be: <pre>
     *   (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
     *   (e.getValue()==null ? 0 : e.getValue().hashCode())</pre>
     * This ensures that {@code e1.equals(e2)} implies that
     * {@code e1.hashCode()==e2.hashCode()} for any two Entries
     * {@code e1} and {@code e2}, as required by the general
     * contract of {@link Object#hashCode}.
     *
     * @return the hash code value for this map entry
     * @see    #equals
     */
    public int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }


    /**
     * Returns a String representation of this map entry.  This
     * implementation returns the string representation of this
     * entry's key followed by the equals character ("<code>=</code>")
     * followed by the string representation of this entry's value.
     *
     * @return a String representation of this map entry
     */
    public String toString() {
      return key + "=" + value;
    }

  }

}
