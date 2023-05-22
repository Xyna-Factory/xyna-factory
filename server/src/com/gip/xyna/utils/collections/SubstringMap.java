/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.ReusableIndexProvider;



/**
 * performantes nachschlagen von: welche strings haben x als substring
 * 
 * dabei wird ein hash-index aufgebaut, der in seiner gr��e beschr�nkt ist. diese gr��e kann angegeben werden.
 * 
 * beispiel: wird der string "hallo welt" in die map eingef�gt, gibt es in der internen datenhaltung folgende indizes:
 * "hal" -&gt; "hallo welt"
 * "all" -&gt; "hallo welt"
 * "llo" -&gt; "hallo welt"
 * ...
 * "elt" -&gt; "hallo welt"
 * das gleiche f�r alle 2buchstabige und 1buchstabige substrings.
 * 
 * damit dieser index nicht zu gro� wird, werden die keys gehashed und per modulo in ein "kleines" array gepackt (hashcodemap)
 * 
 * hash("hal")%1000 -&gt; "hallo welt"
 * hash("all")%1000 -&gt; "hallo welt"
 * ...
 * 
 * beim nachschlagen der superstrings zu einem substring kann man dann schnell alle kandidaten herausfinden.
 * die kandidaten sind aber keine echten treffer, in der datenhaltung k�nnen nun auch strings enthalten sein, die nur durch eine hash-kollision/modulo-kollision
 * an der gleichen array-position gelandet sind.
 * 
 * deshalb schnittmenge von kandidatenmengen bilden und auf das hoffentlich kleine ergebnis f�r die kandidaten nochmal "contains" zur validierung nutzen.
 * 
 * beispiel:
 * getSuperStrings("allo")
 * =&gt; candidates("all") geschnitten mit candidates("llo") geschnitten mit candidates ("al") usw.
 * 
 * die schnittmengenbildung ist effizienter als contains() aufzurufen.
 */
public class SubstringMap {
  
  /*
   * speicherverbrauch tests:
   *   n=200k, avglen=50
   *              alphabet                   50         5         500  
   *   maxlistlenforgrowth 100/500         160 MB    330 MB      185 MB
   *                       1000/5000       175 MB    230 MB      130 MB
   *                       10000/50000     135 MB    130 MB      130 MB
   *   
   *   n=50k, avglen=200
   *                       100/500         125 MB    270 MB
   *                       1000/5000       115 MB    160 MB
   *                       10000/50000     80 MB     30 MB
   *                       
   * Was lernen wir?
   *  Kleinere keymaxlen -> weniger speicher (aber nat�rlich auch langsameres getSuperStrings())
   *  Speicherverbrauch schwankt dadurch um etwa Faktor 2-3
   *  
   * 
   */

  private static final Logger logger = CentralFactoryLogging.getLogger(SubstringMap.class);
  /*
   * anzahl von substrings bzgl der die mengen von superstrings gesucht werden.
   */
  private static final int maxSubstrings = 30;
  /*
   * wenn das verbleibende ergebnis set so klein ist, nicht mehr mit dem n�chsten set schnittmenge bilden, 
   * sondern alle verbleibenden elemente per "contains" validieren.
   * 
   * bei sehr langen strings ist ein kleinerer cutoff besser. bei sehr kurzen strings darf er auch h�her sein.
   */
  private static final int cutoff = 100; //mit len=50 getestet, dass sogar cutoff=300 noch gut ist.

  private static final int charMin = 32;
  private static final int charMax = 126;
  
  private static final int maximumListLengthBaseForDefaults = 500;

  private static final char[] chars;
  static {
    List<Character> l = new ArrayList<Character>();
    for (int i = charMin; i <= charMax; i++) {
      l.add((char) i);
    }
    chars = new char[l.size()];
    for (int i = 0; i < l.size(); i++) {
      chars[i] = l.get(i);
    }
  }


  private static class UniqueSortedIntList {

    private int[] arr;
    private int size;
    private static final float growthfactor = 1.25f;


    public UniqueSortedIntList() {
      arr = new int[2];
      size = 0;
    }


    public UniqueSortedIntList(UniqueSortedIntList orig) {
      arr = new int[orig.size];
      size = orig.size;
      System.arraycopy(orig.arr, 0, arr, 0, size);
    }


    public void add(int value) {
      if (arr.length <= size) {
        expand();
      }
      if (size == 0) {
        size = 1;
        arr[0] = value;
        return;
      }
      int idx = Arrays.binarySearch(arr, 0, size, value);
      if (idx >= 0) {
        return;
      }
      idx = -idx - 1;
      if (idx == size) {
        //hinten anh�ngen
      } else {
        //verschieben
        System.arraycopy(arr, idx, arr, idx + 1, size - idx);
      }
      arr[idx] = value;
      size++;
    }


    private void expand() {
      int[] bigger = new int[(int) (arr.length * growthfactor + 1)];
      System.arraycopy(arr, 0, bigger, 0, size);
      arr = bigger;
    }


    public boolean remove(int value) {
      int idx = Arrays.binarySearch(arr, 0, size, value);
      if (idx < 0) {
        return false;
      }
      removeIdx(idx);
      return true;
    }


    public void removeIdx(int idx) {
      if (idx < size - 1) {
        System.arraycopy(arr, idx + 1, arr, idx, size - idx - 1);
      }
      size--;
    }


    public int size() {
      return size;
    }


  }


  private final UniqueSortedIntList[] singleCharMaps = new UniqueSortedIntList[chars.length];
  //schutz von hashcodemap bearbeitung
  private final ReadWriteLock rwl = new ReentrantReadWriteLock();

  private final boolean casesensitive;
  private final float mapGrowthFactor;//>1 -> h�her=map w�chst schneller=>add() ist durchschnittlich schneller
  private final float mapResizeThreshhold; //0.5-1 -> niedriger=map w�chst h�ufiger => add ist langsamer, aber daf�r ist 
  private final int maximumListLengthForMapGrowth; //>100
  private final int maximumListLengthForKeyMaxLenGrowth;//> maximumListLengthForMapGrowth*1.5

  /*
   * indizes (fortlaufende zahl) <-> in der map gespeicherte strings
   * in der hashcodemap werden nur die indizes verwendet
   */
  private final BijectiveMap<Integer, String> entries = new BijectiveMap<Integer, String>();


  private static class InnerMap {

    private final int keymaxlen;
    private final HashCodeMap<UniqueSortedIntList> map;
    /*
     * summe der l�nge der listen in der hashcodemap. falls die sehr gro� im vergleich zu der gr��e der map ist, muss entweder 
     * die map vergr��ert oder keymaxlen erh�ht werden.
     * falls sie sehr klein ist, dann andersherum.
     */
    private int valuecnt = 0;


    public InnerMap(int keymaxlen, int size) {
      this.keymaxlen = keymaxlen;
      map = new HashCodeMap<UniqueSortedIntList>(size);
    }

  }


  private volatile InnerMap innermap;

  private final ReusableIndexProvider next = new ReusableIndexProvider(1.4f, 16);


  /**
   * 
   * @param casesensitive Falls nein, findet getSuperStrings("asd") auch "xxASdxx"
   */
  public SubstringMap(boolean casesensitive) {
    this(casesensitive, 2, 2, 0.95f, maximumListLengthBaseForDefaults, maximumListLengthBaseForDefaults*8, 16);
  }


  /**
   * 
   * @param casesensitive Falls nein, findet getSuperStrings("asd") auch "xxASdxx"
   * @param keymaxlen Bestimmt, was die maximale gespeicherte L�nge von Substrings ist. Je gr��er, desto mehr Speicherverbrauch, 
   *        aber desto schneller die Suche
   * @param mapGrowthFactor Wenn die interne Map vergr��ert wird, dann mit diesem Faktor
   * @param mapResizeThreshhold Die interne Map wird vergr��ert, wenn sie um mapResizeThreshhold gef�llt ist
   * @param maximumListLengthForMapGrowth Die interne Map wird nur vergr��ert, wenn die durchschnittliche L�nge der Substring-Listen 
   *        darin mindestens so lang wie maximumListLengthForMapGrowth sind
   * @param maximumListLengthForKeyMaxLenGrowth keymaxlen wird erh�ht, wenn die interne Map nicht mehr w�chst, aber wegen Hashkollisionen 
   *        oder mangelnder Substring-Diversit�t trotzdem die durchschnittliche L�nge der darin enthaltenen Substring-Listen �ber diesen Wert w�chst. 
   * @param initialSize initiale Gr��e der internen Map
   */
  public SubstringMap(boolean casesensitive, int keymaxlen, float mapGrowthFactor, float mapResizeThreshhold,
                      int maximumListLengthForMapGrowth, int maximumListLengthForKeyMaxLenGrowth, int initialSize) {
    if (keymaxlen < 2) {
      throw new IllegalArgumentException("keymaxlen must be at least 2");
    }
    if (mapGrowthFactor < 1.2) {
      throw new IllegalArgumentException("mapGrowthFactor must be at least 1.2");
    }
    if (mapResizeThreshhold < 0.2 || mapResizeThreshhold > 1) {
      throw new IllegalArgumentException("mapResizeThreshhold must be between 0.2 and 1");
    }
    if (maximumListLengthForMapGrowth < 10) {
      throw new IllegalArgumentException("maximumListLengthForMapGrowth must be at least 10");
    }
    if (maximumListLengthForKeyMaxLenGrowth < maximumListLengthForMapGrowth * 1.2) {
      throw new IllegalArgumentException("maximumListLengthForKeyMaxLenGrowth must be at least maximumListLengthForMapGrowth*1.2");
    }
    if (initialSize < 16) {
      throw new IllegalArgumentException("initialSize must be at least 16");
    }
    this.casesensitive = casesensitive;
    this.mapGrowthFactor = mapGrowthFactor;
    this.mapResizeThreshhold = mapResizeThreshhold;
    this.maximumListLengthForMapGrowth = maximumListLengthForMapGrowth;
    this.maximumListLengthForKeyMaxLenGrowth = maximumListLengthForKeyMaxLenGrowth;
    innermap = new InnerMap(keymaxlen, initialSize);
    for (int i = 0; i < singleCharMaps.length; i++) {
      singleCharMaps[i] = new UniqueSortedIntList();
    }
  }


  public static SubstringMap create(boolean casesensitive, Collection<String> strings) {
    int[] stats = getStatistics(strings);
    SubstringMap m = create(casesensitive, stats[0], stats[1], stats[2]);
    for (String s : strings) {
      m.add(s);
    }
    return m;
  }


  private static int[] getStatistics(Iterable<String> strings) {
    int len = 0;
    int n = 0;
    boolean[] chars = new boolean[512];
    int al = 0;
    for (String s : strings) {
      if (s != null) {
        len += s.length();
        n++;
        if (al < 100) {
          for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!chars[c]) {
              chars[c] = true;
              al++;
            }
          }
        }
      }
    }
    if (n == 0) {
      return new int[] {100, 20, 100};
    }
    len /= n;
    if (len == 0) {
      len = 3;
    }
    if (al == 0) {
      al = 5;
    }
    return new int[] {n, len, al};
  }


  private static int[] calcSizes(int numberOfStrings, int averageStringLength, int alphabetSize) {
    if (alphabetSize < 1) {
      throw new IllegalArgumentException("alphabetSize must be at least 1");
    }
    if (numberOfStrings < 1) {
      throw new IllegalArgumentException("numberOfStrings must be at least 1");
    }
    if (averageStringLength < 1) {
      throw new IllegalArgumentException("averageStringLength must be at least 1");
    }
    int kml = 2;
    int mapsize = 10;
    while (true) {
      double bucketsOfAlphabetSize = Math.pow(alphabetSize, kml);
      int substrings = numberOfStrings * (kml * averageStringLength - (kml - 1) * kml / 2);
      mapsize = substrings / 1000;
      if (bucketsOfAlphabetSize >= mapsize / 10) {
        break;
      }
      kml++;
    }
    return new int[] {kml, Math.max(16, (int) (mapsize * 1.5f))};
  }


  /**
   * Erzeugt Map mit initialen sinnvoll gesetzten Parametern f�r keymaxlen und initialsize der Map. Diese werden anhand der �bergebenen Parametern
   * berechnet.
   * @param casesensitive
   * @param numberOfStrings
   * @param averageStringLength
   * @param alphabetSize
   * @return
   */
  public static SubstringMap create(boolean casesensitive, int numberOfStrings, int averageStringLength, int alphabetSize) {
    int[] sizes = calcSizes(numberOfStrings, averageStringLength, alphabetSize);
    return new SubstringMap(casesensitive, sizes[0], 2f, 0.95f, maximumListLengthBaseForDefaults, maximumListLengthBaseForDefaults*8, sizes[1]);
  }


  private volatile ResizeQueue queue;
  private volatile boolean asynchronousResizeRunning;


  private static class ResizeQueue implements Runnable {

    private static class MapEntry implements Map.Entry<Integer, String> {

      private final String s;
      private final Integer idx;


      public MapEntry(String s, Integer idx) {
        this.s = s;
        this.idx = idx;
      }


      @Override
      public Integer getKey() {
        return idx;
      }


      @Override
      public String getValue() {
        return s;
      }


      @Override
      public String setValue(String value) {
        throw new RuntimeException();
      }


      @Override
      public int hashCode() {
        return s.hashCode();
      }


      @Override
      public boolean equals(Object obj) {
        if (this == obj)
          return true;
        if (obj == null)
          return false;
        if (getClass() != obj.getClass())
          return false;
        MapEntry other = (MapEntry) obj;
        return s.equals(other.s);
      }


    }


    private final ReentrantLock l = new ReentrantLock();
    private final Set<Entry<Integer, String>> added = new HashSet<Entry<Integer, String>>();
    private final Set<Entry<Integer, String>> removed = new HashSet<Entry<Integer, String>>();
    private SubstringMap map;
    private final int newCap;
    private final int newKeyMaxLen;
    private final long start = System.currentTimeMillis();
    private final AtomicInteger cnt = new AtomicInteger(0);
    private volatile int slowdownMS = 50;
    private volatile int slowdownInterval = 64;


    private ResizeQueue(SubstringMap m, float factor) {
      this(m, factor < 0 ? m.innermap.keymaxlen + 1 : m.innermap.keymaxlen, calcNewCap(m, factor));
    }


    private static int calcNewCap(SubstringMap m, float factor) {
      if (factor == resizequeue_factor_not_set) {
        return m.innermap.map.capacity();
      }
      int newCap = (int) (m.innermap.map.capacity() * factor);
      if (newCap == m.innermap.map.capacity()) {
        newCap += 1;
      }
      return newCap;
    }


    public ResizeQueue(SubstringMap m, int newKeyMaxLen, int newMapCapacity) {
      map = m;
      this.newKeyMaxLen = newKeyMaxLen;
      this.newCap = newMapCapacity;
      map.queue = this;
      //init
      l.lock();
      try {
        map.asynchronousResizeRunning = true;
      } finally {
        l.unlock();
      }
    }


    private void increaseHashCodeMapSize() {
      InnerMap mnew = new InnerMap(newKeyMaxLen, newCap);

      //alle entries neu gehashed eintragen
      List<Entry<Integer, String>> addList;
      synchronized (map.entries) {
        addList = new ArrayList<Entry<Integer, String>>(map.entries.entrySet());
      }
      addToMap(addList, mnew);

      sharedQueueProcessing(mnew);
    }


    private void sharedQueueProcessing(InnerMap mnew) {
      //queue (asynchron) abarbeiten
      int size = -1;
      List<Integer> processed = new ArrayList<Integer>();
      List<Integer> durations = new ArrayList<Integer>();
      do {
        long t = System.currentTimeMillis();
        List<Entry<Integer, String>> addList;
        List<Entry<Integer, String>> removeList;
        l.lock();
        try {
          addList = new ArrayList<Entry<Integer, String>>(added);
          removeList = new ArrayList<Entry<Integer, String>>(removed);
          removed.clear();
          added.clear();
        } finally {
          l.unlock();
        }
        size = addList.size() + removeList.size();
        processed.add(size);
        slowDownAdd(processed, durations);
        addToMap(addList, mnew);
        removeFromMap(removeList, mnew);
        durations.add((int) (System.currentTimeMillis() - t));
        if (logger.isTraceEnabled()) {
          logger.trace("processed " + size + " entries from queue");
        }
      } while (size > 1000);

      //swap maps
      l.lock();
      try {
        //rest wird synchron abgearbeitet
        addToMap(added, mnew);
        removeFromMap(removed, mnew);
        map.innermap = mnew;
        map.asynchronousResizeRunning = false;
        map.queue = null;
      } finally {
        l.unlock();
      }
      slowdownInterval = 1000000;
      slowdownMS = 0;
      if (logger.isTraceEnabled()) {
        logger.trace("[" + (System.currentTimeMillis() - start) + "ms] map size=" + mnew.map.capacity() + ", keymaxlen=" + mnew.keymaxlen
            + ", entries=" + mnew.valuecnt);
      }
      l.lock();
      try {
        removed.clear();
        added.clear();
      } finally {
        l.unlock();
      }

      map = null;
    }


    private void slowDownAdd(List<Integer> processed, List<Integer> durations) {
      //was tun, wenn die verarbeitung andauernd langsamer ist, als das neue hinzuf�gen von objekten. dann wird man hier nie fertig.
      //idee: queue verschieben: neue map "ver�ffentlichen", und bei "getSuperStrings" dann die Objekte aus der queue zus�tzlich ber�cksichtigen
      //andere idee: das hinzuf�gen langsamer machen, indem man ein kleines offset erzeugt, welches das add verlangsamt und diesem thread das aufholen erlaubt.
      if (processed.size() <= 2) {
        return;
      }
      if (processed.get(processed.size() - 1) * 1.25 > processed.get(processed.size() - 2)) {
        //nicht mindestens 25% abgenommen -> add verz�gern
        int processedCnt = 0;
        int duration = 0;
        for (int i = 0; i < processed.size() - 1; i++) {
          processedCnt += processed.get(i);
          duration += durations.get(i);
        }
        float processingMsPerEntry = 1.0f * duration / processedCnt;
        float addingMsPerEntry = 1.0f * duration / (processedCnt + processed.get(processed.size() - 1));
        float diff = processingMsPerEntry - addingMsPerEntry;
        diff *= 10f;
        int mod = 1;
        while (diff < 30) {
          diff *= 2;
          mod *= 2;
        }
        slowdownInterval = mod;
        slowdownMS = (int) diff;
        if (logger.isTraceEnabled()) {
          logger.trace("slowdown: " + slowdownMS + " / " + slowdownInterval + " (processed: " + processedCnt + " / " + duration + ", new: "
              + processed.get(processed.size() - 1) + ")");
        }
      } else {
        //verz�gerung hat ausgereicht, dann kann man wieder etwas weniger verz�gern
        slowdownInterval *= 1.3;
        if (logger.isTraceEnabled()) {
          logger.trace("slowdown: " + slowdownMS + " / " + slowdownInterval);
        }
      }
    }


    private void removeFromMap(Collection<Entry<Integer, String>> list, InnerMap m) {
      HashCodeMap<UniqueSortedIntList> newMap = m.map;
      for (Entry<Integer, String> e : list) {
        String value = e.getValue();
        int idxAsInt = e.getKey();
        String valueForKeyCreation;
        if (map.casesensitive) {
          valueForKeyCreation = value;
        } else {
          valueForKeyCreation = value.toLowerCase();
        }
        int len = value.length();
        for (int substringlen = 1; substringlen <= m.keymaxlen; substringlen++) {
          for (int start = 0; start <= len - substringlen; start++) {
            String substr = valueForKeyCreation.substring(start, start + substringlen);
            UniqueSortedIntList s = newMap.get(substr);
            if (s != null) {
              if (s.remove(idxAsInt)) {
                m.valuecnt--;
              }
              if (s.size == 0) {
                newMap.remove(substr);
              }
            }
          }
        }
      }
    }


    private void addToMap(Collection<Entry<Integer, String>> entriesList, InnerMap m) {
      HashCodeMap<UniqueSortedIntList> newMap = m.map;
      for (Entry<Integer, String> e : entriesList) {
        String value = e.getValue();
        int idxAsInt = e.getKey();
        String valueForKeyCreation;
        if (map.casesensitive) {
          valueForKeyCreation = value;
        } else {
          valueForKeyCreation = value.toLowerCase();
        }
        int len = value.length();
        for (int substringlen = 1; substringlen <= m.keymaxlen; substringlen++) {
          for (int start = 0; start <= len - substringlen; start++) {
            String substr = valueForKeyCreation.substring(start, start + substringlen);
            UniqueSortedIntList s = newMap.get(substr);
            if (s == null) {
              s = new UniqueSortedIntList();
              newMap.put(substr, s);
            }
            int lbefore = s.size;
            s.add(idxAsInt);
            if (s.size > lbefore) {
              m.valuecnt++;
            }
          }
        }
      }
    }


    @Override
    public void run() {
      if (newCap == map.innermap.map.capacity()) {
        increaseKeyMaxLen();
      } else {
        increaseHashCodeMapSize();
      }
    }


    private void increaseKeyMaxLen() {
      //alle substrings der l�nge keymaxvals+1 hinzuf�gen
      //wenn gleichzeitig ein anderer thread add() aufruft, wird schlimmstenfalls die arbeit doppelt durchgef�hrt

      InnerMap mnew = new InnerMap(newKeyMaxLen, map.innermap.map.capacity());

      //alle entries auf h�here keymaxlen erweitern
      List<Entry<Integer, String>> addList;
      synchronized (map.entries) {
        addList = new ArrayList<Entry<Integer, String>>(map.entries.entrySet());
      }

      //map clonen (enth�lt ggfs ein paar eintr�ge, die gequeued sind, egal...
      map.rwl.readLock().lock();
      try {
        HashCodeMap<UniqueSortedIntList> hcmold = map.innermap.map;
        for (int i = 0; i < hcmold.capacity(); i++) {
          UniqueSortedIntList usil = hcmold.get(i);
          if (usil != null) {
            mnew.map.put(i, new UniqueSortedIntList(usil));
          }
        }
      } finally {
        map.rwl.readLock().unlock();
      }

      increaseKeyMaxLen(addList, mnew);

      sharedQueueProcessing(mnew);
    }


    private void increaseKeyMaxLen(Collection<Entry<Integer, String>> coll, InnerMap m) {
      int oldkeymaxlen = map.innermap.keymaxlen;
      int newkeymaxlen = m.keymaxlen;
      for (Entry<Integer, String> e : coll) {
        String value = e.getValue();
        String valueForKeyCreation;
        if (map.casesensitive) {
          valueForKeyCreation = value;
        } else {
          valueForKeyCreation = value.toLowerCase();
        }
        int idx = e.getKey();
        int len = valueForKeyCreation.length();
        for (int substringlen = oldkeymaxlen + 1; substringlen <= newkeymaxlen; substringlen++) {
          for (int start = 0; start <= len - substringlen; start++) {
            String substr = valueForKeyCreation.substring(start, start + substringlen);
            UniqueSortedIntList list = m.map.get(substr);
            if (list == null) {
              list = new UniqueSortedIntList();
              m.map.put(substr, list);
            }
            int lbefore = list.size;
            list.add(idx);
            if (list.size > lbefore) {
              m.valuecnt++;
            }
          }
        }
      }
    }


    public void add(String s, int idx) {
      //warte, falls gerade datenhaltung ausgetauscht wird
      l.lock();
      try {
        added.add(new MapEntry(s, idx));
        removed.remove(new MapEntry(s, -1));
      } finally {
        l.unlock();
      }
      long slowDownLocal = slowdownMS;
      if (slowDownLocal > 0) {
        if (cnt.incrementAndGet() % slowdownInterval == 0) {
          try {
            Thread.sleep(slowDownLocal);
          } catch (InterruptedException e) {
          }
        }
      }
    }


    public void remove(String s, int idxAsInt) {
      l.lock();
      try {
        added.remove(new MapEntry(s, -1));
        removed.add(new MapEntry(s, idxAsInt));
      } finally {
        l.unlock();
      }
      long slowDownLocal = slowdownMS;
      if (slowDownLocal > 0) {
        if (cnt.incrementAndGet() % slowdownInterval == 0) {
          try {
            Thread.sleep(slowDownLocal);
          } catch (InterruptedException e) {
          }
        }
      }
    }

  }


  /**
   * vergleichsweise teuer (im vergleich zum hinzuf�gen zu einer normalen map), damit das getSuperStrings() performant ist
   */
  public void add(final String value) {
    int len = value.length();
    if (len == 0) {
      return;
    }
    Integer idx;
    synchronized (entries) {
      if (entries.getInverse(value) != null) {
        return;
      }
      idx = next.getNextFreeIdx();
      String old = entries.put(idx, value);
      if (old != null) {
        throw new RuntimeException();
      }
    }
    int idxAsInt = idx;
    //  System.out.println("add " + value + "->" + idxAsInt);
    if (asynchronousResizeRunning) {
      ResizeQueue q = queue;
      if (q != null) {
        q.add(value, idxAsInt);
      }
    }
    InnerMap m = innermap;

    String valueForKeyCreation;
    if (casesensitive) {
      valueForKeyCreation = value;
    } else {
      valueForKeyCreation = value.toLowerCase();
    }
    for (int substringlen = 1; substringlen <= m.keymaxlen; substringlen++) {
      for (int start = 0; start <= len - substringlen; start++) {
        if (substringlen == 1) {
          char c = valueForKeyCreation.charAt(start);
          if (c >= charMin && c <= charMax) {
            putSingle(c, idxAsInt);
            continue;
          }
          //hier kein continue, weitere einzelne zeichen sollen als key in der allgemeinen map auch verf�gbar sein
        }
        String substr = valueForKeyCreation.substring(start, start + substringlen);
        put(substr, idxAsInt, m);
      }
    }
  }


  private void putSingle(char c, int idx) {
    rwl.writeLock().lock();
    try {
      singleCharMaps[c - charMin].add(idx);
    } finally {
      rwl.writeLock().unlock();
    }
  }


  private UniqueSortedIntList getSingle(char c) {
    rwl.readLock().lock();
    try {
      return singleCharMaps[c - charMin];
    } finally {
      rwl.readLock().unlock();
    }
  }


  private void removeSingle(char c, int idx) {
    rwl.writeLock().lock();
    try {
      singleCharMaps[c - charMin].remove(idx);
    } finally {
      rwl.writeLock().unlock();
    }
  }
  
  private static final float resizequeue_factor_not_set = -1;


  private void put(String key, int idx, InnerMap m) {
    rwl.writeLock().lock();
    try {
      UniqueSortedIntList s = m.map.get(key);
      if (s == null) {
        s = new UniqueSortedIntList();
        m.map.put(key, s);
      }
      int lbefore = s.size;
      s.add(idx);
      if (s.size > lbefore) {
        m.valuecnt++;
        /*
         * gro�e map -> mehr speicherverbrauch, aber kleinere listen pro bucket
         * deshalb map dann gr��er machen, wenn sie recht voll ist und die listen gro� sind
         * falls die listen gro� werden, aber die map nicht voll wird, dann muss/kann keymaxlen erh�ht werden.
         */
        int ms = m.map.size();
        int vms = m.valuecnt / ms;
        if (ms > m.map.capacity() * mapResizeThreshhold && vms > maximumListLengthForMapGrowth) {
          if (!asynchronousResizeRunning) {
            ResizeQueue q = new ResizeQueue(this, (double)vms / maximumListLengthForMapGrowth > 4 ? 4 * mapGrowthFactor : mapGrowthFactor * vms
                / maximumListLengthForMapGrowth);
            Thread t = new Thread(q, "resize substringmap thread");
            t.setDaemon(true);
            t.start();
          }
        } else if (vms > maximumListLengthForKeyMaxLenGrowth) {
          if (!asynchronousResizeRunning) {
            ResizeQueue q = new ResizeQueue(this, resizequeue_factor_not_set);
            Thread t = new Thread(q, "resize substringmap thread (inckeymaxlen)");
            t.setDaemon(true);
            t.start();
          }
        }
      }
    } finally {
      rwl.writeLock().unlock();
    }
  }


  public void remove(String value) {
    int len = value.length();
    if (len == 0) {
      return;
    }
    Integer idx;
    synchronized (entries) {
      idx = entries.getInverse(value);
      if (idx == null) {
        return;
      }
      entries.remove(idx);
    }
    int idxAsInt = idx;
    // System.out.println("remove " + value + "->" + idxAsInt);

    if (asynchronousResizeRunning) {
      ResizeQueue q = queue;
      if (q != null) {
        q.remove(value, idxAsInt);
      }
    }
    InnerMap m = innermap;

    String valueForKeyCreation;
    if (casesensitive) {
      valueForKeyCreation = value;
    } else {
      valueForKeyCreation = value.toLowerCase();
    }

    int keymaxlenlocal = m.keymaxlen;

    for (int substringlen = 1; substringlen <= keymaxlenlocal; substringlen++) {
      for (int start = 0; start <= len - substringlen; start++) {
        if (substringlen == 1) {
          char c = valueForKeyCreation.charAt(start);
          if (c >= charMin && c <= charMax) {
            removeSingle(c, idxAsInt);
            continue;
          }
        }
        String substr = valueForKeyCreation.substring(start, start + substringlen);
        removePart(substr, idxAsInt, m);
      }
    }
    next.returnIdx(idx);
  }


  private void removePart(String key, int idx, InnerMap m) {
    rwl.writeLock().lock();
    try {
      UniqueSortedIntList s = m.map.get(key);
      if (s != null) {
        if (s.remove(idx)) {
          m.valuecnt--;
        }
        if (s.size == 0) {
          m.map.remove(key);
        }
      }
    } finally {
      rwl.writeLock().unlock();
    }
  }


  private static final float[] partition = new float[] {1, 1 / 2f, 0, 3 / 4f, 1 / 4f, 5 / 8f, 3 / 8f, 7 / 8f, 1 / 8f, 7 / 16f, 5 / 16f,
      9 / 16f, 3 / 16f, 11 / 16f, 13 / 16f, 1 / 16f, 15 / 16f, 1 / 32f, 3 / 32f, 5 / 32f, 7 / 32f, 9 / 32f, 11 / 32f, 13 / 32f, 15 / 32f,
      17 / 32f, 19 / 32f, 21 / 32f, 23 / 32f, 25 / 32f, 27 / 32f, 29 / 32f, 31 / 32f};


  private static final Comparator<UniqueSortedIntList> COMPARATOR_SETS2_SIZE = new Comparator<UniqueSortedIntList>() {

    @Override
    public int compare(UniqueSortedIntList o1, UniqueSortedIntList o2) {
      return o1.size - o2.size;
    }

  };


  /**
   * ermittelt alle vorher zur map hinzugef�gten strings, die part als substring enthalten
   */
  public List<String> getSuperStrings(String part) {
    int len = part.length();
    if (len == 0) {
      throw new RuntimeException("part must contain characters");
    }
    if (!casesensitive) {
      part = part.toLowerCase();
    }
    rwl.readLock().lock();
    try {
      if (len == 1) {
        char c = part.charAt(0);
        if (c >= charMin && c <= charMax) {
          //muss nicht validiert werden. allein dieser fall ist meist die zus�tzliche datenhaltung wert, die f�r das getSingle ben�tigt wird
          return getStrings(getSingle(part.charAt(0)));
        }
        return validate(getStrings(get(part, innermap)), part);
      }

      //substrings erzeugen
      InnerMap m = innermap;
      Set<String> substrings = createSubstringsToSearch(part, m);

      //substrings nachschlagen
      List<UniqueSortedIntList> sets = new ArrayList<UniqueSortedIntList>();
      for (String substring : substrings) {
        sets.add(get(substring, m));
      }
      if (sets.size() == 1) {
        return validate(getStrings(sets.get(0)), part);
      }

      Collections.sort(sets, COMPARATOR_SETS2_SIZE);

      //kleinste ergebnisse zuerst schneiden
      UniqueSortedIntList result = new UniqueSortedIntList(sets.get(0));
      int nochange = 0;
      for (int i = 1; i < sets.size(); i++) {
        int lastsize = result.size;
        retainAll(result, sets.get(i));
        if (result.size == lastsize) {
          nochange++;
          if (nochange >= 4) {
            return validate(getStrings(result), part);
          }
        } else {
          nochange = 0;
        }
        if (result.size() < cutoff) {
          return validate(getStrings(result), part);
        }
      }
      return validate(getStrings(result), part);
    } finally {
      rwl.readLock().unlock();
    }
  }


  private void retainAll(UniqueSortedIntList a, UniqueSortedIntList b) {
    int[] aarr = a.arr;
    int asize = a.size;
    int[] barr = b.arr;
    int bsize = b.size;
    if (asize * Math.log(bsize) * 1.5 > asize + bsize) {
      int idxA = 0;
      int idxB = 0;
      while (idxA < asize && idxB < bsize) {
        int aval = aarr[idxA];
        int bval = barr[idxB];
        if (aval > bval) {
          idxB++;
        } else if (bval > aval) {
          a.removeIdx(idxA);
          asize--;
        } else {
          idxA++;
          idxB++;
        }
      }
    } else {
      int idxA = 0;
      int idxB = 0;
      while (idxA < asize) {
        //aktuelles element@index1 auf existenz in b testen
        int aval = aarr[idxA];
        //finde position in b        
        int bpos = Arrays.binarySearch(barr, idxB, bsize, aval);
        if (bpos < 0) {
          //nicht gefunden
          a.removeIdx(idxA);
          asize--;
          idxB = -bpos - 1;
        } else {
          idxA++;
          idxB = bpos + 1;
        }
      }
    }
  }


  private List<String> getStrings(UniqueSortedIntList indizes) {
    int s = indizes.size;
    List<String> r = new ArrayList<String>(s);
    int[] arr = indizes.arr;
    synchronized (entries) {
      for (int i = 0; i < s; i++) {
        String e = entries.get(arr[i]);
        if (e != null) { //beim remove wird erst aus entries entfernt, und dann aus den arrays
          r.add(e);
        }
      }
    }
    return r;
  }


  private Set<String> createSubstringsToSearch(String part, InnerMap m) {
    int len = part.length();
    int l = Math.min(m.keymaxlen, len);
    Set<String> set = new HashSet<String>();
    if (len < partition.length) {
      while (set.size() < maxSubstrings) {
        for (int i = 0; i <= len - l; i++) {
          set.add(part.substring(i, i + l));
          if (set.size() >= maxSubstrings) {
            return set;
          }
        }
        l--;
        if (l == 0) {
          return set;
        }
      }
    } else {
      while (set.size() < maxSubstrings) {
        for (float f : partition) {
          int idx = (int) ((len - l) * f);
          set.add(part.substring(idx, idx + l));
          if (set.size() >= maxSubstrings) {
            return set;
          }
        }
        l--;
        if (l == 0) {
          return set;
        }
      }
    }
    return set;
  }


  private List<String> validate(List<String> candidates, String part) {
    List<String> result = new ArrayList<String>();
    if (casesensitive) {
      for (String s : candidates) {
        if (s.contains(part)) {
          result.add(s);
        }
      }
    } else {
      part = part.toLowerCase();
      for (String s : candidates) {
        if (s.toLowerCase().contains(part)) {
          result.add(s);
        }
      }

    }
    return result;
  }


  private UniqueSortedIntList get(String part, InnerMap imap) {
    if (part.length() == 1) {
      char c = part.charAt(0);
      if (c >= charMin && c <= charMax) {
        return getSingle(c);
      }
    }
    UniqueSortedIntList m = imap.map.get(part);
    if (m == null) {
      return new UniqueSortedIntList();
    } else {
      return m;
    }
  }


  /**
   * f�gt alle werte hinzu, aber macht nur ein resize auf die optimale datenhaltungsgr��e
   * anstatt mehrfacher sequentieller resizes bei iterativen hinzuf�gen
   * @param  maxWaitForRunningResizeInMilliseconds wie lange soll maximal auf ein bereits laufendes resize gewartet werden
   */
  public void addAll(final List<String> vals, int maxWaitForRunningResizeInMilliseconds) {
    final List<String> entriesVals;
    synchronized (entries) {
      entriesVals = new ArrayList<String>(entries.values());
    }
    int[] stats = getStatistics(new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return new CombinedIterator<String>(vals.iterator(), entriesVals.iterator());
      }
    });
    int[] sizes = calcSizes(stats[0], stats[1], stats[2]);
    if (innermap.map.capacity() < sizes[1] || innermap.keymaxlen < stats[0]) {
      rwl.writeLock().lock(); //lock holen, um threadsicher resizing anzustossen
      try {
        if (maxWaitForRunningResizeInMilliseconds > 0) {
          long timeout = System.currentTimeMillis() + maxWaitForRunningResizeInMilliseconds;
          while (asynchronousResizeRunning && System.currentTimeMillis() - timeout > 0) {
            rwl.writeLock().unlock(); //lock freigeben, w�hrend man wartet
            try {
              try {
                Thread.sleep(50);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            } finally {
              rwl.writeLock().lock();
            }
          }
        }
        if (!asynchronousResizeRunning) {
          ResizeQueue q = new ResizeQueue(this, sizes[0], sizes[1]);
          Thread t = new Thread(q, "resize substringmap thread all");
          t.setDaemon(true);
          t.start();
        }
      } finally {
        rwl.writeLock().unlock();
      }
    }
    for (String s : vals) {
      if (s != null) {
        add(s);
      }
    }
  }

}
