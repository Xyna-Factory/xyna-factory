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
package gip.base.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class OBListObject<O extends OBObject> implements Serializable, Collection<O>, List<O> {
  
  private transient static Logger logger = Logger.getLogger(OBListObject.class);
  
  protected Vector<O> content=null;

  protected int currentNumber=0;

  protected int totalLines=0;
  protected int firstLine=0;


  /** Standard-Constructor */
  public OBListObject() {
    content = new Vector<O>();
    currentNumber=0;
    firstLine=0;
  }

  /** Constructor mit einem Vector von Elementen 
      @param newContent Vector mit den OBDBObject
   */
  public OBListObject(Vector<O> newContent) {
    content = newContent;
    reset();
  }
  
  /** Constructor mit einem ARRAY von Elementen 
      @param newContent ARRAY von OBDBObject
   */
  public OBListObject(O[] newContent) {
    this();
    for (int i=0; i<newContent.length; i++) {
      content.addElement(newContent[i]);
    }
    reset();
  }
  
  /** Hinzuf&uuml;gen eines OBDBObject. 
      Die Aufz&auml;hlung wir dadurch invalidiert
      @param entry neuer Eintrag
   */
  public boolean add(O entry) {
    currentNumber=OBConstants.IRREGULAR_INT;
    return content.add(entry);
  }

  /** Hinzuf&uuml;gen eines OBDBObject an der Stelle i. 
      Die Aufz&auml;hlung wir dadurch invalidiert
      @param index Numer des Eintrags
      @param entry neuer Eintrag
   */
  public void add(int index, O entry) {
    content.add(index,entry);
    currentNumber=OBConstants.IRREGULAR_INT;
  }

  /** Hinzuf&uuml;gen eines Vectors von OBDBObject. 
      Die Aufz&auml;hlung wir dadurch invalidiert
      @param newContent Vector neuer Eintr&auml;ge
   */
  public void add(Vector<O> newContent) {
    for (int i=0; i<newContent.size(); i++) {
      content.addElement(newContent.elementAt(i));
    }
  }

  /** Hinzuf&uuml;gen eines ARRAYs von OBDBObject. 
      Die Aufz&auml;hlung wir dadurch invalidiert.
      @param newContent Array neuer Eintr&auml;ge
   */
  public void add(O[] newContent) {
    for (int i=0; i<newContent.length; i++) {
      content.addElement(newContent[i]);
    }
  }

  /** L&ouml;schen eines OBObject. 
      Die Aufz&auml;hlung wir dadurch invalidiert
      @param i Position
   */
  public void removeElementAt(int i) {
    content.removeElementAt(i);
    currentNumber=OBConstants.IRREGULAR_INT;
  }

  /** L&ouml;scht den Inhalt. 
      Die Aufz&auml;hlung wir dadurch invalidiert. */
  public void clear() {
    content= new Vector<O>();
  }

  /** Die Aufz&auml;hlung wird validiert und startet von vorne. */
  public void reset() {
    currentNumber=0;
  }

  /** Gibt es weitere Elemente in der Liste? 
      @return true, wenn mindestens ein weiteres Element vorhanden ist
      @deprecated Ersetzen durch die for...-Variante
  */
  public boolean hasMoreElements() {
    return currentNumber!=OBConstants.IRREGULAR_INT && 
      currentNumber<content.size();
  }

  /** Liefert die Anzahl der Elemente in der Liste.
      @return Anzahl der Elemente
  */
  public int size() {
    return content.size();
  }

  /** Gibt es n&auml;chste Element der Liste? Wenn ja, wird weitergezaehlt.  
      @return true, wenn es ein weiteres Element gibt
      @deprecated Ersetzen durch die for...-Variante
  */
  public boolean next() {
    if (currentNumber!=OBConstants.IRREGULAR_INT && 
        currentNumber>=0 && 
        currentNumber<content.size()) {
      currentNumber++;
      return true;
    }
    else {
      return false;
    }
  }


  /** Gibt das n&auml;chste Element der Liste, wenn noch mindestens eins vorhanden ist.  
      @return das gew&uuml;nschte Element
      @throws OBException wenn kein weiteres Element existiert
      @deprecated Ersetzen durch die for...-Variante
  */
  public O nextBaseElement() throws OBException {
    if (next()) {
      return currentBaseElement();
    }
    else {
      throw new OBException(OBException.OBErrorNumber.noSuchElementException1, new String[] {""}); //$NON-NLS-1$
    }
  }

  /** Gibt das aktuelle Element der Liste.  
      @return das gew&uuml;nschte Element
      @throws OBException wenn das Element nicht existiert
      @deprecated Ersetzen durch die for...-Variante
  */
  public O currentBaseElement() throws OBException {
    if (currentNumber!=OBConstants.IRREGULAR_INT && 
        currentNumber>0 && 
        currentNumber<=content.size()) {
      return content.elementAt(currentNumber-1);
    }
    else {
      throw new OBException(OBException.OBErrorNumber.noSuchElementException1, new String[] {""}); //$NON-NLS-1$
    }
  }


  /** Gibt das gew&uuml;nschte Element der Liste, wenn es vorhanden ist.  
      @param index Nummer des Wertes
      @deprecated use elementAt instead
      @return das gew&uuml;nschte Element
      @throws OBException wenn das Element nicht existiert
  */
  public O baseElementAt(int index) throws OBException {
    if (index>=0 && index<content.size()) {
      return content.elementAt(index);
    }
    else {
      throw new OBException(OBException.OBErrorNumber.noSuchElementException1, new String[] {String.valueOf(index)});
    }
  }
  
  /** Gibt das gew&uuml;nschte Element der Liste, wenn es vorhanden ist.  
    @param index Nummer des Wertes
    @return das gew&uuml;nschte Element
    @throws OBException wenn das Element nicht existiert
   */
  public O elementAt(int index) throws OBException {
    if (index>=0 && index<content.size()) {
      return content.elementAt(index);
    }
    else {
      throw new OBException(OBException.OBErrorNumber.noSuchElementException1, new String[] {String.valueOf(index)});
    }
  }

  /** Liefert ein Array mit den in der Liste vorhandenen PrimaryKeys zurueck
   * @return Array von pks
   * @throws OBException die bei getPrimaryKey eines Elementes auftreten kann
   */
  public long[] getPKArray() throws OBException {
    long[] back = new long[content.size()];
    for (int i=0; i<content.size();i++) {
      back[i] = content.elementAt(i).getPrimaryKey();
    }
    return back;
  }

  /** Liefert eine Liste mit den im Listenobjekt vorhandenen PrimaryKeys zurueck.
   * Form des Rueckgabewertes: "(pk1,pk2,....,pkn)"
   * @return Liste der pks
   * @throws OBException die bei getPrimaryKey() eines Elementes auftreten kann
   */
  public String getPKList() throws OBException {
    long[] pkArray = getPKArray();
    StringBuffer back=new StringBuffer("("); //$NON-NLS-1$
    for (int i=0; i<pkArray.length;i++) {
      if(i>0) {
        back.append(","); //$NON-NLS-1$
      }
      back.append(pkArray[i]);
    }
    back.append(")"); //$NON-NLS-1$
    return back.toString();
  }


  /** Gibt den gesamten Inhalt als Vector zur&uuml;ck.
      @return gesamter Inhalt
  */
  public Vector<O> getVector() {
    return content;
  }

  /** Gibt den gesamten Inhalt als List zur&uuml;ck.
      @return gesamter Inhalt
   */
  public List<O> getList() {
    return content;
  }

  public Vector<Hashtable<String, String>> getHashVector() {
    Vector<Hashtable<String, String>> retVal = new Vector<Hashtable<String, String>>();
    for (int i=0; i<content.size(); i++) {
      O obdb = content.elementAt(i);
      retVal.add(obdb.convertToHashtable());
    }
    return retVal;
  }

  public Hashtable<String, Vector<Hashtable<String, String>>> convertToHashtable() {
    Vector<Hashtable<String, String>> list = getHashVector();
    Hashtable<String,  Vector<Hashtable<String, String>>> retVal = new Hashtable<String,  Vector<Hashtable<String, String>>>();
    retVal.put("list",list); //$NON-NLS-1$
    return retVal;
  }

  public List<HashMap<String, Object>> convertToList() {
    List<HashMap<String, Object>> retVal = new ArrayList<HashMap<String, Object>>();
    for (int i=0; i<content.size(); i++) {
      O obdb = content.elementAt(i);
      retVal.add(obdb.convertToHashMap());
    }
    return retVal;
  }
  
  public Iterator<O> iterator() {
    return content.iterator();
  }
  
  /** Wandelt ein OBDBOject in eine HashMap um (key = attribute.name, value = attribute.value) 
   * @param key Key in der Hashmap
    @return HashMap, der dem OBDBObject entspricht
   */
  public HashMap<String,Object> convertToHashMap(String key) {
    HashMap<String,Object> retVal = new HashMap<String,Object>();
    retVal.put(key, convertToList());
    return retVal;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void addHashVector(O example, Vector hashVec) throws OBException {
    for (int i=0; i<hashVec.size(); i++) {
      Hashtable ht = (Hashtable) hashVec.elementAt(i);
      
      Class cl = example.getClass();
      O obo;
      try {
        obo = (O) cl.newInstance();
        obo.convertFromHashtable(ht);
        content.add(obo);
      }
      catch (InstantiationException e) {
        logger.error("addHashVector", e); //$NON-NLS-1$
      }
      catch (IllegalAccessException e) {
        logger.error("addHashVector", e); //$NON-NLS-1$
      }

    }
  }

  @SuppressWarnings("unchecked")
  public void convertFromHashtable(O example, Hashtable<String,Object> hash) throws OBException {
    Vector<Object> list = (Vector<Object>) hash.get("list"); //$NON-NLS-1$
    addHashVector(example,list);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<size(); i++) {
      try {
        if (elementAt(i) != null) {
          sb.append("Objekt ").append(i).append(":\n").append(elementAt(i).toString()).append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        else {
          sb.append("Objekt ").append(i).append(": null\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
      catch (OBException e) {
        logger.debug("toString", e); //$NON-NLS-1$
      }
    }
    return sb.toString();
  }

  /** Liefert die Anzahl der Zeilen in der Datenbank (denn evtl. linesTotal&gt;maxRows)
   * @return Anzahl der Zeilen
   */
  public int getTotalLines() {
    return totalLines;
  }

  /**
   * @param i Gesamtzahl der Zeilen
   */
  public void setTotalLines(int i) {
    totalLines = i;
  }

  /**
   * @return Returns the firstLine.
   */
  public int getFirstLine() {
    return firstLine;
  }
  
  /**
   * @param fl The firstLine to set.
   */
  public void setFirstLine(int fl) {
    this.firstLine = fl;
  }

  /** Liefert die Anzahl der Elemente in der Liste.
      @return Anzahl der Elemente
  */
  public int getSize() {
    return content.size();
  }

  /**
   * Vertauscht die Reihenfolge der Elemente i,j
   * @param i Ein Element
   * @param j Anderes Element
   * @throws Exception Fehler
   */
  @SuppressWarnings("unchecked")
  public void swap(int i, int j) throws Exception {
    O w1 = (O) elementAt(i).clone();
    elementAt(i).copyAll(elementAt(j));
    elementAt(j).copyAll(w1);
  }
  
  
  /** Quicksort Aufsteigend, rekursiv
   * @param L linke Grenze
   * @param R rechte Grenze
   * @param columnName Spaltenname
   * @throws Exception Fehler
   */
  public void qsort_Ascend(int L, int R, String columnName) throws Exception {
    int i=L;
    int j=R-1;
    
    O w2;
    w2=elementAt(R);
     
    do {
      while (elementAt(i).compareTo(w2,columnName) <= 0 && i<R) // data[][] < w2       
        i++;
      while (w2.compareTo(elementAt(j),columnName) <= 0 && j>L) // w2 < data[][]
        j--;
      if (i<j) {
        swap(i,j);
        i++;
        j--;
        //OBLog.log.finer("XOBTableModel.qsort_Ascend():" + data);
      }
    } while (i<j); // Ende DO-WHILE
    if (elementAt(i).compareTo(w2,columnName) > 0) {
      // Tausche i mit w2
      swap(i,R);
    }
    if ( L < j ) qsort_Ascend(L,j,columnName);
    if ( i < R ) qsort_Ascend(i,R,columnName);
  }
   
  /** Quicksort Absteigend, rekursiv
   * @param L linke Grenze
   * @param R rechte Grenze
   * @param columnName Spaltenname
   * @throws Exception Fehler
   */
  public void qsort_Descend(int L, int R, String columnName) throws Exception {
    int i=L;
    int j=R-1;
    
    O w2;
    w2=elementAt(R);
     
    do {
      while (elementAt(i).compareTo(w2,columnName) >= 0 && i<R) // data[][] < w2       
        i++;
      while (w2.compareTo(elementAt(j),columnName) >= 0 && j>L) // w2 < data[][]
        j--;
      if (i<j) {
        swap(i,j);
        i++;
        j--;
        //OBLog.log.finer("XOBTableModel.qsort_Ascend():" + data);
      }
    } while (i<j); // Ende DO-WHILE
    if (elementAt(i).compareTo(w2,columnName) < 0) {
      // Tausche i mit w2
      swap(i,R);
    }
    if ( L < j ) qsort_Descend(L,j,columnName);
    if ( i < R ) qsort_Descend(i,R,columnName);
  }

  
  /* --------------------------------------------------------------------------------------- */
  /* Methoden von Collection implementieren */
  /* --------------------------------------------------------------------------------------- */
  public boolean addAll(Collection<? extends O> c) {
    return content.addAll(c);
  }

  public boolean contains(Object o) {
    return content.contains(o);
  }

  public boolean containsAll(Collection<?> c) {
    return content.containsAll(c);
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }

  public boolean remove(Object o) {
    return content.remove(o);
  }

  public boolean removeAll(Collection<?> c) {
    return content.removeAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return content.retainAll(c);
  }

  public Object[] toArray() {
    return content.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return content.toArray(a);
  }
   
  /* --------------------------------------------------------------------------------------- */
  /* Methoden von List implementieren */
  /* --------------------------------------------------------------------------------------- */

  public boolean addAll(int index, Collection<? extends O> c) {
    return getVector().addAll(index, c);
  }

  public O get(int index) {
    return getVector().get(index);
  }

  public int indexOf(Object o) {
    return getVector().indexOf(o);
  }

  public int lastIndexOf(Object o) {
    return getVector().lastIndexOf(o);
  }

  public ListIterator<O> listIterator() {
    return getVector().listIterator();
  }

  public ListIterator<O> listIterator(int index) {
    return getVector().listIterator(index);
  }

  public O remove(int index) {
    return getVector().remove(index);
  }

  public O set(int index, O element) {
    return getVector().set(index, element);
  }

  public List<O> subList(int fromIndex, int toIndex) {
    return getVector().subList(fromIndex, toIndex);
  }
}

