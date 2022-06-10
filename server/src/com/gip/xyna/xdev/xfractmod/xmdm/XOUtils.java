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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.utils.misc.DataRangeCollection.DataSource;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

/**
 * hilfsmethoden und klassen, die vom generierten code aus verwendet werden
 */
public class XOUtils {
  
  private static final transient Logger logger = CentralFactoryLogging.getLogger(XOUtils.class);
  
  public static final Object VARNAME_NOTFOUND = new Object();

  private static final String LIST_INDEX_START = "[\"";
  private static final String LIST_INDEX_END = "\"]";

  /**
   * @return {@link #VARNAME_NOTFOUND} falls objekt nicht in varnames gefunden
   * @throws InvalidObjectPathException falls dem pfad nicht gefolgt werden kann
   * @throws NullPointerException falls der pfad zwar gültig scheint, aber das objekt null ist 
   */
  public static Object getIfNameIsInVarNames(String[] varNames, Object[] vars, String path) throws InvalidObjectPathException {
    boolean isPath = false;
    int idx = path.indexOf(".");
    String childPath = null;
    String varName = path;
    if (idx > -1) {
      isPath = true;
      childPath = path.substring(idx + 1, path.length());
      varName = path.substring(0, idx);
    }
    Object ret = null;
    boolean found = false;
    for (int i = 0; i < varNames.length; i++) {
      if (varNames[i].equals(varName)) {
        ret = vars[i];
        found = true;
        break;
      }
    }
    if (!found) {
      if (varName.endsWith(LIST_INDEX_END)) {
        int indexStart = varName.indexOf(LIST_INDEX_START);
        if (indexStart > 0) {
          String listIndex = varName.substring(indexStart + LIST_INDEX_START.length(), varName.indexOf(LIST_INDEX_END));
          try {
            int numericListIndex = Integer.parseInt(listIndex);
            Object listObj = getIfNameIsInVarNames(varNames, vars, varName.substring(0, indexStart));
            if (listObj != null &&
                listObj instanceof List) {
              ret = ((List)listObj).get(numericListIndex);
              found = true;
            }
          } catch (NumberFormatException e) {
            // handle as not found
          }
        }
      }
      if (!found) {
        return VARNAME_NOTFOUND;
      }
    }
    if (isPath) {
      if (ret == null) {
        throw new NullPointerException("Can not evaluate path '" + childPath + "' on null object.");
      }
      if (ret instanceof XynaObject) {
        return ((XynaObject) ret).get(childPath);
      }
      //FIXME: nur aus abwärtskompatibilitätsgründen so verschachtelt
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
    }
    return ret;
  }


  /**
   *  @deprecated use generated <code>get(String)</code> or {@link #getIfNameIsInVarNames(String[], Object[], String)}
   */
  @Deprecated
  public static Object get(String[] varNames, Object[] vars, String path, XynaObject parent) throws InvalidObjectPathException {
    Object o = getIfNameIsInVarNames(varNames, vars, path);
    if (o == VARNAME_NOTFOUND) {
      //nicht gefunden
      if (parent != null) {
        o = parent.get(path);
      } else {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
      }
    }
    return o;
  }


  public static final void checkCastability(Object toBeCasted, Class<?> toBeCastedTo,
                                        String targetMemberVar) {

    if (toBeCasted == null) {
      return;
    }

    if (!toBeCastedTo.isAssignableFrom(toBeCasted.getClass())) {
      String errorMsg =
          "Cannot cast " + toBeCasted.getClass().getName() + " to " + toBeCastedTo.getName()
              + " while setting member variable " + targetMemberVar;
      throw new IllegalArgumentException(errorMsg);
    }

  }

  @SuppressWarnings("unchecked")
  protected <T> List<T> castToList(Class<T> typeClass, String varName, Object object) {
    if( object == null ) {
      return null;
    }
    if (!(object instanceof List)) {
      throw new IllegalArgumentException("Error while setting member variable " + varName + ", expected list, got "
          + object.getClass().getName());
    }
    if (((List<?>) object).size() > 0) {
      checkCastability(((List<?>) object).get(0), typeClass, varName);
      return (List<T>)object;
    } else {
      return Collections.emptyList();
    }
  }
  
  @SuppressWarnings("unchecked")
  protected <T> T castTo(Class<T> typeClass, String varName, Object object) {
    checkCastability(object, typeClass, varName);
    return (T) object;
  }

  public static abstract class ObjectVersionBase {

    protected final GeneralXynaObject xo;
    protected final long version;

    protected final IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers;

    protected ObjectVersionBase(GeneralXynaObject xo, long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
      this.xo = xo;
      this.version = version;
      this.changeSetsOfMembers = changeSetsOfMembers;
    }

    protected boolean hasChanges(long otherversion) {
      long start = Math.min(version, otherversion);
      long end = Math.max(version, otherversion);
      DataRangeCollection cs = getDataCollection();
      cs.insertDataPoints(start, end);
      //wenn es datapoints gibt, dann gibt es wohl changes (zumindest kann man sie nicht ausschliessen)
      return cs.hasDataPoints(start, end);
    }
    
    protected DataRangeCollection getDataCollection() {
      DataRangeCollection cs = changeSetsOfMembers.get(xo);
      if (cs == null) {
        cs = new DataRangeCollection(new DataSource() {

          public void addDataPoints(long start, long end, Set<Long> datapoints) {
            xo.collectChanges(start, end, changeSetsOfMembers, datapoints);
          }
        });
        changeSetsOfMembers.put(xo, cs);
        traceDataRangeCollectionCreation(cs, xo);
      }
      return cs;
    }

    public int hashCode() {
      Stack<GeneralXynaObject> stack = new Stack<GeneralXynaObject>();
      return hashCode(stack);
    }
    
    //wird im generierten code aufgerufen
    public int hashCode(Stack<GeneralXynaObject> stack) {
      DataRangeCollection cs = getDataCollection();
      cs.updateInterval(version);
      int hashCode = cs.getValue(version);
      if (hashCode == 0) {
        //teure hash berechnung cachen
        stack.push(xo);
        hashCode = calcHashOfMembers(stack);
        hashCode = 31 * hashCode + xo.getClass().hashCode();
        /*
         * TODO exception stacktraces korrekt im workflow verwenden. vgl auch FIXME/TODO in StepThrow.
         * bis dahin macht diese behandlung keinen sinn
        if (xo instanceof Exception) {
          hashCode = 31 * hashCode + Arrays.hashCode(((Exception) xo).getStackTrace());
        }
         */
        stack.pop();
        cs.setValue(version, hashCode);
      }
      return hashCode;
    }


    public abstract int calcHashOfMembers(Stack<GeneralXynaObject> stack);


    protected int hashList(List<?> l) {
      if (l == null) {
        return 0;
      }
      int h = 0;
      int s = l.size();
      for (int i = 0; i < s; i++) {
        Object e = l.get(i);
        if (e == null) {
          h = h * 31;
        } else {
          h = h * 31 + e.hashCode();
        }
      }
      return h;
    }


    protected <T extends GeneralXynaObject> int hashList(List<? extends T> l, long version,
                                               IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers, Stack<GeneralXynaObject> stack) {
      if (l == null) {
        return 0;
      }
      int h = 0;
      int s = l.size();
      for (int i = 0; i < s; i++) {
        T e = l.get(i);
        if (e == null) {
          h = h * 31;
        } else {
          ObjectVersionBase ovb = e.createObjectVersion(version, changeSetsOfMembers);
          h = h * 31 + ovb.hashCode(stack);
        }
      }
      return h;
    }

    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (o.getClass() != getClass()) {
        return false;
      }
      ObjectVersionBase other = (ObjectVersionBase) o;
      if (other.xo == xo) {
        if (other.version == version || !hasChanges(other.version)) {
          if (logger.isTraceEnabled()) {
            logger.trace("EQUAL: " + xo + " no version change in (" + version + "-" + other.version + ")");
          }
          return true;
        }
        if (logger.isTraceEnabled()) {
          logger.trace("??EQUAL: " + xo + " version change in (" + version + "-" + other.version + ")");
        }
      }
      /*
       * TODO s.o. (equals)
      if (xo instanceof Exception) {
        if (!Arrays.equals(((Exception) xo).getStackTrace(), ((Exception) other.xo).getStackTrace())) {
          return false;
        }
      }
       */
      if (!memberEquals(other)) {
        if (logger.isTraceEnabled()) {
          logger.trace("NOTEQUAL: " + xo + "@" + version + " vs " + other.xo + "@" + other.version);
        }
        return false;
      }
      if (logger.isTraceEnabled()) {
        logger.trace("EQUAL: " + xo + "@" + version + " vs " + other.xo + "@" + other.version);
      }
      return true;
    }


    protected abstract boolean memberEquals(ObjectVersionBase other);


    protected static boolean xoEqual(GeneralXynaObject o1, GeneralXynaObject o2, long version1, long version2,
                                     IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
      if (o1 == null) {
        if (o2 == null) {
          return true;
        } else {
          return false;
        }
      } else if (o2 == null) {
        return false;
      }
      return o1.createObjectVersion(version1, changeSetsOfMembers).equals(o2.createObjectVersion(version2, changeSetsOfMembers));
    }


    protected static boolean equal(Object o1, Object o2) {
      if (o1 == o2) {
        return true;
      }
      if (o1 == null) {
        return false;
      }
      return o1.equals(o2);
    }


    protected static boolean listEqual(List<?> l1, List<?> l2) {
      if (l1 == l2) {
        return true;
      }
      if (l1 == null || l2 == null) {
        return false;
      }
      int s = l1.size();
      if (s != l2.size()) {
        return false;
      }
      for (int i = 0; i < s; i++) {
        Object e1 = l1.get(i);
        Object e2 = l2.get(i);
        if (e1 == e2) {
          continue;
        }
        if (e1 == null) {
          return false;
        }
        if (!e1.equals(e2)) {
          return false;
        }
      }
      return true;
    }


    protected static <T extends GeneralXynaObject> boolean listEqual(List<? extends T> l1, List<? extends T> l2, long version1, long version2,
                                                              IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
      //hier nicht l1==l2, weil unterschiedliche versionen!
      if (l1 == null) {
        return l2 == null;
      } else if (l2 == null) {
        return false;
      }
      int s = l1.size();
      if (s != l2.size()) {
        return false;
      }
      for (int i = 0; i < s; i++) {
        T e1 = l1.get(i);
        T e2 = l2.get(i);
        if (e1 == null || e2 == null) {
          return false;
        }
        if (e1.getClass() != e2.getClass()) {
          return false;
        }
        ObjectVersionBase ovb1 = e1.createObjectVersion(version1, changeSetsOfMembers);
        ObjectVersionBase ovb2 = e2.createObjectVersion(version2, changeSetsOfMembers);
        if (!ovb1.equals(ovb2)) {
          return false;
        }
      }
      return true;
    }

  }

  //version ist jeweils das ende des gültigkeitsintervalls
  public static class Version<T> implements Comparable<Version<T>>, Serializable {

    private static final long serialVersionUID = 1L;
    //dieses objekt ist gültig gewesen bis versionOfNextObject-1
    private final long versionOfNextObject;
    public final T object;


    public Version(long versionOfNextObject, T object) {
      this.versionOfNextObject = versionOfNextObject;
      this.object = object;
    }


    public int compareTo(Version<T> o) {
      if (versionOfNextObject > o.versionOfNextObject) {
        return 1;
      }
      if (versionOfNextObject < o.versionOfNextObject) {
        return -1;
      }
      return 0;
    }
  } 
  
  static IDGenerator idgen; //muss nicht volatile sein. ist nicht schlimm, wenn man mehrfach setzt

  public static long nextVersion() {
    if (idgen == null) {
      synchronized (XOUtils.class) {
        if (idgen == null) {
          IDGenerator tmp = XynaFactory.getInstance().getIDGenerator();
          tmp.setBlockSize("objectversion", 500000);
          idgen = tmp;
        }
      }
    }
    return idgen.getUniqueId("objectversion");
  }

  public static class VersionedObject<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final List<Version<T>> versions = new ArrayList<Version<T>>(1);


    public void add(T t) {
      //synchronized, damit die liste sortiert nach version bleibt. ansonsten kann es passieren, dass eine neuere version vor eine ältere kommt.
      synchronized (this) {
        long version = XOUtils.nextVersion();
        versions.add(new Version<T>(version, t));
      }
    }


    public Version<T> getVersion(long version) {
      if (version == -1) {
        return null;
      }
      int idx;
      synchronized (this) { //nicht gleichzeitig liste ändern
        idx = Collections.binarySearch(versions, new Version<T>(version, null));
      }
      /*
       * beispiel:
       * (object, version)
       * 
       * (A, 2)   -> A valid 0-1
       * (B, 7)   -> B valid 2-6
       * (C, 13)  -> C valid 7-12
       * current member valid 13-now
       */
      if (idx == -versions.size() - 1) {
        //einfügeposition ganz hinten > current member ist aktueller wert
        return null;
      }
      if (idx < 0) {
        return versions.get(-idx - 1);
      }
      idx++; //getVersion(7) muss C zurückgeben
      if (idx >= versions.size()) {
        //getVersion(13) muss null zurückgeben
        return null;
      }
      return versions.get(idx);
    }


    /**
     * gibt die versionen des objekts zurück, die zwischen den beiden angegebenen versionen (inklusive start und end) gültig waren.
     */
    public Version<T>[] getVersions(long rev1, long rev2) {
      /*
       * (A, 2)   -> A valid 0-1
       * (B, 7)   -> B valid 2-6
       * (C, 13)  -> C valid 7-12
       * current member valid 13-now
       * 
       * getVersions(4, 7) -> [B, C]
       * getVersions(13, 16) -> []
       * getVersions(12, 12) -> [C]
       * getVersions(11, 11) -> [C]
       */
      if (rev1 == rev2) {
        Version<T> v = getVersion(rev1);
        if (v == null) {
          return new Version[0];
        } else {
          return new Version[]{v};
        }
      }
      
      int idx1, idx2;
      synchronized (this) { //nicht gleichzeitig liste ändern
        idx1 = Collections.binarySearch(versions, new Version<T>(rev1, null));
        idx2 = Collections.binarySearch(versions, new Version<T>(rev2, null));
      }

      int s = versions.size();
      idx1 = transformBinarySearchResult(idx1, s);
      idx2 = transformBinarySearchResult(idx2, s);
      Version<T>[] ret = new Version[idx2 - idx1 + (idx2 < s ? 1 : 0)];
      for (int i = 0; i < ret.length; i++) {
        ret[i] = versions.get(idx1 + i);
      }
      return ret;
    }

    private int transformBinarySearchResult(int idx, int s) {
      if (idx == -s - 1) {
        return s;
      } else if (idx < 0) {
        return -idx - 1;
      } else {
        return idx + 1;
      }
    }


    public List<Version<T>> getAllVersions() {
      return versions;
    }
  }
  
  private static void traceDataRangeCollectionCreation(DataRangeCollection cs, GeneralXynaObject xo) {
    if (logger.isTraceEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Created DataRangeCollection ").append(System.identityHashCode(cs)).append(" for ").append(xo.toString());
      Set<Long> set = new HashSet<>();
      /*
       * in   private static void addChanges(final GeneralXynaObject value, long rev1, long rev2,
                          final IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSets, Set<Long> changes) {
                          wird auf Long.MAX_VALUE überprüft, damit hier keine endlosrekursion passiert.
       */
      xo.collectChanges(0, Long.MAX_VALUE, new IdentityHashMap<GeneralXynaObject, DataRangeCollection>(), set);
      List<Long> list = new ArrayList<>(set);
      Collections.sort(list);
      sb.append(", datapoints=").append(list.toString());
      logger.trace(sb.toString());
    }
  }
  
  //kann auch für listen von simpletypes aufgerufen werden
  /*
   * sammle alle versionen zwischen rev1 und rev2, in der sich das objekt geändert hat
   * 
   * Versionen
   * (A, 3)
   * (B, 10)
   * (C, 15)
   * (D, 16)
   * 
   * addChangesFor 3, 3 => fügt 3 hinzu
   * addChangesFor 3, 9 => fügt 3 hinzu
   * addChangesFor 3, 10 => fügt 3, 10 hinzu
   * 
   */
  public static <T> void addChangesForSimpleMember(VersionedObject<T> oldVersionsOfMember, long rev1, long rev2, Set<Long> changes) {
    if (oldVersionsOfMember != null) {
      //ohne das -1 würde (3,3) im obigen beispiel nur version B liefern, weil A ja nur bis 2 gültig war.
      //aber: bei abfrage nach (16,16) darf 15 nicht zurückgegeben werden
      Version<T>[] versions = oldVersionsOfMember.getVersions(rev1 - 1, rev2);
      for (int i = 0; i < versions.length; i++) {
        Version<T> v = versions[i];
        if (v.versionOfNextObject <= rev2) {
          changes.add(v.versionOfNextObject);
        } 
      }
    }
  }

  public static <T extends GeneralXynaObject> void addChangesForComplexListMember(List<T> member, VersionedObject<List<T>> oldVersionsOfMember, long rev1,
                                                                long rev2, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSets,
                                                                Set<Long> changes) {
    List<Triple<List<T>, Long, Long>> triples = getIntervalsOfAllVersions(member, oldVersionsOfMember, rev1, rev2, changes);
    for (Triple<List<T>, Long, Long> t : triples) {
      addChangesList(t.getFirst(), t.getSecond(), t.getThird(), changeSets, changes);
    }
  }
  
  /**
   * sammle alle gültigkeitsintervalle von current und von alten versionen) zwischen rev1 und rev2 (jeweils inklusive).
   * füge alle gültigkeitsintervallgrenzen (also versionen, wo sich eine änderung ergeben hat = start von intervall) zu changes hinzu
   *   dabei soll sowohl rev1 als auch rev2 als change festgestellt werden können (falls dort eine grenze liegt)
   */
  static <T> List<Triple<T, Long, Long>> getIntervalsOfAllVersions(T current, VersionedObject<T> vo, long rev1, long rev2,
                                                                  Set<Long> changes) {
    if (vo == null) {
      return Collections.singletonList(Triple.of(current, rev1, rev2));
    } else {
      Version<T>[] versions = vo.getVersions(rev1 - 1, rev2); //rev-1, damit man ein bei rev1-1 endendes intervall finden kann, und dann rev1 als change hinzufügen
      List<Triple<T, Long, Long>> ret = new ArrayList<>(versions.length + 1);
      for (int i = 0; i < versions.length; i++) {
        Version<T> v = versions[i];
        if (v.versionOfNextObject <= rev2) {
          changes.add(v.versionOfNextObject);
        } //ansonsten war die änderung erst nach rev2. für das objekt muss trotzdem noch die rekursion durchgeführt werden, weil es ja bei rev2 bereits gültig war.

        //komplexwertig rekursion:
        //für jede in dem intervall gültige version checken, was für changes es in den members gibt
        //dieses objekt ist nur bis zur nächsten version gültig, d.h. man muss in der rekursion auch nur changes bis dort ermitteln.        
        long versionValidEnd = Math.min(v.versionOfNextObject - 1, rev2);
        if (i == 0 && v.versionOfNextObject == rev1) {
          //nicht zu weit nach links verschieben, sonst ist end < start
          //wenn ein intervall genau bei rev1 beginnt, wird bei der nächsten iteration das ensprechende intervall geadded
          continue;
        }
        long versionValidStart;
        if (i > 0) {
          versionValidStart = Math.max(versions[i - 1].versionOfNextObject, rev1);
        } else {
          versionValidStart = rev1;
        }

        ret.add(Triple.of(v.object, versionValidStart, versionValidEnd));
      }

      //komplexwertig:
      long latestVersion;
      if (versions.length > 0) {
        latestVersion = versions[versions.length - 1].versionOfNextObject;
      } else {
        latestVersion = rev1;
      }
      if (latestVersion <= rev2) {
        //current version (bzgl zeitpunkt des aufrufs der methode, inzwischen kann current version anders sein) ist zu rev2 noch gültig
        long versionValidEnd = rev2;
        long versionValidStart = Math.max(latestVersion, rev1);

        ret.add(Triple.of(current, versionValidStart, versionValidEnd));
      }
      return ret;
    }
  }


  public static <T extends GeneralXynaObject> void addChangesForComplexMember(T member, VersionedObject<T> oldVersionsOfMember, long rev1, long rev2,
                                                                   IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSets,
                                                                   Set<Long> changes) {
    List<Triple<T, Long, Long>> triples = getIntervalsOfAllVersions(member, oldVersionsOfMember, rev1, rev2, changes);
    for (Triple<T, Long, Long> t : triples) {
      addChanges(t.getFirst(), t.getSecond(), t.getThird(), changeSets, changes);
    }
  }

  private static void addChangesList(final List<? extends GeneralXynaObject> list, long rev1, long rev2,
                          final IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSets, Set<Long> changes) {
    if (list == null) {
      return;
    }
    for (int i = 0; i<list.size(); i++) {
      GeneralXynaObject e = list.get(i);
      addChanges(e, rev1, rev2, changeSets, changes);
    }
  }


  private static void addChanges(final GeneralXynaObject value, long rev1, long rev2,
                          final IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSets, Set<Long> changes) {
    if (value == null || rev2 == Long.MAX_VALUE /*vgl traceDataRangeCollectionCreation*/) {
      return;
    }
    DataRangeCollection cs = changeSets.get(value);
    if (cs == null) {
      cs = new DataRangeCollection(new DataSource() {

        public void addDataPoints(long start, long end, Set<Long> datapoints) {
          value.collectChanges(start, end, changeSets, datapoints);
        }
      });
      changeSets.put(value, cs);
      traceDataRangeCollectionCreation(cs, value);
    }
    //eigentliche rekursion auf die membervariable. bei zyklen passiert hier kein stackoverflow, weil dann die datapoints bereits gesetzt sind
    cs.insertDataPoints(rev1, rev2);
    //bei der rekursion gefundene datapoints zum set hinzufügen
    cs.collectExistingDataPoints(rev1, rev2, changes);
  }
  

  //häufigster usecase ist add. für den muss man nicht die liste clonen, sondern kann sich einfach nur die zugehörige größe der liste merken. falls dann später elemente hinzugefügt werden, sieht man die einfach nicht.
  public static class ListWithConstantSize<T> extends AbstractList<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final List<T> innerList;
    private final int elementsInVersion;

    public ListWithConstantSize(List<T> list) {
      this.innerList = list;
      this.elementsInVersion = list.size();
    }

    public int size() {
      return elementsInVersion;
    }

    public T get(int index) {
      return innerList.get(index);
    }
  }

  public static class VersionedList<T> extends AbstractList<T> implements Serializable { //TODO RandomAccess kann man leider nicht dranschreiben, weil xynaobjekte nicht nur randomaccess listen enthalten müssen

    private static final long serialVersionUID = 1L;
    
    private volatile List<T> innerList;
    private final UpdateList<T> updateList;


    public VersionedList(List<T> innerList, UpdateList<T> updateList) {
      this.innerList = innerList;
      this.updateList = updateList;
    }

    //lesende operationen

    @Override
    public int size() {
      return innerList.size();
    }


    @Override
    public boolean isEmpty() {
      return innerList.isEmpty();
    }


    @Override
    public boolean contains(Object o) {
      return innerList.contains(o);
    }


    @Override
    public boolean containsAll(Collection<?> c) {
      return innerList.containsAll(c);
    }


    @Override
    public Object[] toArray() {
      return innerList.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a) {
      return innerList.toArray(a);
    }


    @Override
    public T get(int index) {
      return innerList.get(index);
    }


    @Override
    public int indexOf(Object o) {
      return innerList.indexOf(o);
    }


    @Override
    public int lastIndexOf(Object o) {
      return innerList.lastIndexOf(o);
    }


    //sublist+iteratoren haben potientielle changes als folge -> abstractlist implementiert die derart, dass die änderungen auf add/remove zurückgeführt werden

    //changes, add only

    @Override
    public boolean add(T o) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(new ListWithConstantSize<T>(innerList));
        return innerList.add(o);
      }
    }


    @Override
    public boolean addAll(Collection<? extends T> c) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(new ListWithConstantSize<T>(innerList));
        return innerList.addAll(c);
      }
    }


    //other changes

    @Override
    public boolean remove(Object o) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        boolean ret = innerList.remove(o);
        updateList.update(innerList);
        return ret;
      }
    }


    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        boolean ret = innerList.addAll(index, c);
        updateList.update(innerList);
        return ret;
      }
    }


    @Override
    public boolean removeAll(Collection<?> c) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        boolean ret = innerList.removeAll(c);
        updateList.update(innerList);
        return ret;
      }
    }


    @Override
    public boolean retainAll(Collection<?> c) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        boolean ret = innerList.retainAll(c);
        updateList.update(innerList);
        return ret;
      }
    }


    @Override
    public void clear() {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>();
        updateList.update(innerList);
      }
    }


    @Override
    public T set(int index, T element) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        T ret = innerList.set(index, element);
        updateList.update(innerList);
        return ret;
      }
    }


    @Override
    public void add(int index, T element) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        innerList.add(index, element);
        updateList.update(innerList);
      }
    }


    @Override
    public T remove(int index) {
      VersionedObject<List<T>> oldVersions = updateList.getOldVersions();
      synchronized (oldVersions) {
        oldVersions.add(innerList);
        innerList = new ArrayList<T>(innerList);
        T ret = innerList.remove(index);
        updateList.update(innerList);
        return ret;
      }
    }

  }

  public interface UpdateList<T> extends Serializable {

    public void update(List<T> newList);


    public VersionedObject<List<T>> getOldVersions();
  }

  public static boolean supportsObjectVersioningForInternalObjects() {
    if (!XynaFactory.isFactoryServer()) {
      return false;
    }
    if (com.gip.xyna.xfmg.xods.configuration.XynaProperty.useVersioningConfig.get() == 4) {
      return true;
    } else {
      return false;
    }
  }


  public static <T> List<T> substituteList(List<T> list) {
    if (list instanceof VersionedList) {
      return new ArrayList<T>(list);
    }
    return list;
  }

}
