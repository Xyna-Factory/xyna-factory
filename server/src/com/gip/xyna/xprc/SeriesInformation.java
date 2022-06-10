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

package com.gip.xyna.xprc;



import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xsched.OrderSeriesManagement;



/**
 * Kapselt alle Informationen und Logik zur Zugehörigkeit des Parentauftrags zu einer Auftragsserie.
 * Der größte Teil dieser Daten hier wird nicht mehr benötigt, da er durch 
 * {@link com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable SeriesInformationStorable} 
 * gespeichert wird.
 * Leider kann hier an der Klasse wenig geändert werden, da:
 * <ul>
 * <li>diese Klasse die veröffentlichte Schnittstelle für die Projekte, welche Auftragsserien 
 * verwenden, ist. Diese verwenden noch die alte Übergabe der Serien in Form von XynaOrder-Bündeln 
 * (Huckepack-Aufträge). Dies soll aber abgelöst werden durch Einzelaufträge mit angepasster 
 * SeriesInformation.</li>
 * <li>frühere Serienaufträge mit dieser SeriesInformation serialisiert im OrderInstanceBackup stehen.
 * Leider ist es nicht möglich, diese während eines Updates zu migrieren, da dafür die ClassLoader 
 * fehlen. Daher können die SeriesInformation erst zur Laufzeit migriert werden (siehe 
 * {@link com.gip.xyna.xprc.xsched.orderseries.OrderSeriesSeparator OrderSeriesSeparator}) und müssen 
 * zuvor noch auf die alte Weise deserialisiert werden.</li>
 * </ul> 
 */
public class SeriesInformation implements Serializable {
  
  private static final long serialVersionUID = -7248584858817494408L;

  private static final Logger logger = CentralFactoryLogging.getLogger(SeriesInformation.class);

  private static final String CORRELATION_ID_PREFIX = "AutoGenCorrId_XynaOrder_";
  
  /**
   * The order belonging to this series information
   */
  private XynaOrder parent;

  @SuppressWarnings("unused")
  private transient OrderSeriesManagement orderSeriesManagement;
  private ArrayList<XynaOrder> predecessors = new ArrayList<XynaOrder>();
  private ArrayList<XynaOrder> successors = new ArrayList<XynaOrder>();
  @SuppressWarnings("unused")
  private ConnectionStatus connectionStatus;
  
  //serialisierung geht nur nach presucaquisition. zu diesem zeitpunkt sind prefinder und sucfinder leer.
  @SuppressWarnings({"unused", "deprecation"})
  private transient ArrayList<PredecessorFinder> presNotFound = new ArrayList<PredecessorFinder>();
  @SuppressWarnings({"unused", "deprecation"})
  private transient ArrayList<SuccessorFinder> sucsNotFound = new ArrayList<SuccessorFinder>();
  // ist auftrag am prescheduler vorbeigekommen, wo finishPreSuc aufgerufen wird?
  @SuppressWarnings("unused")
  private AtomicBoolean finishedPreSucAquisition = new AtomicBoolean(false);
  // ist auftrag vom scheduler beendet wurden?
  private AtomicBoolean finishedFlag = new AtomicBoolean(false);
  // hat scheduler bei beendigung fehlerflag übergeben?
  @SuppressWarnings("unused")
  private boolean hadError = false;
  // userdefined: soll auftrag bei fehler (oder cancel) folgeaufträge canceln?
  private boolean autoCancel = true;
  // wurde festgestellt, dass dieser auftrag als nachfolger eines fehlgeschlagenen auftrags mit autocancelflag gecancelt
  // werden soll? (gesetzt vom prescheduler beim connecten)
  @SuppressWarnings("unused")
  private boolean inheritedCancel = false;

  //neue Speicherung der Predecessor/Successor-Abhängigkeiten
  private String correlationId;
  private ArrayList<String> predecessorsCorrIds = new ArrayList<String>();
  private ArrayList<String> successorsCorrIds = new ArrayList<String>();
  
  
  
  /**
   * neuer, vereinfachter Konstruktor: correlationId ist Pflicht
   * 
   * @param correlationId die externe id dieses serienauftrags
   * @throws IllegalArgumentException wenn correlationId==null
   */
  public SeriesInformation(String correlationId) {
    this.setCorrelationId(correlationId);
  }

  public static SeriesInformation copyOf( SeriesInformation si) {
    SeriesInformation copy = new SeriesInformation(si.getCorrelationId());
    copy.parent = si.parent;

    copy.predecessors = copyOfList( si.predecessors );
    copy.successors = copyOfList( si.successors );
    
    copy.finishedFlag = new AtomicBoolean(si.finishedFlag.get()); //nötig?

    copy.autoCancel = si.autoCancel;
    copy.correlationId = si.correlationId;
    copy.predecessorsCorrIds = copyOfList(si.predecessorsCorrIds);
    copy.successorsCorrIds = copyOfList(si.successorsCorrIds);
    return copy;
  }
  
  public static SeriesInformation copyOfWithNewParent( SeriesInformation si, XynaOrder newParent) {
    SeriesInformation copy = copyOf(si);
    copy.parent = newParent;
    return copy;
  }
  
  private static <T> ArrayList<T> copyOfList(ArrayList<T> list) {
    if( list == null ) {
     return null;
    }
    ArrayList<T> copy = new ArrayList<T>();
    copy.addAll(list);
    return copy;
  }

  /**
   * Eindeutiger Name der Auftrags, zu dem dieses SeriesInformation-Objekt gehört
   * @param correlationId
   * @throws IllegalArgumentException wenn correlationId==null
   */
  public void setCorrelationId(String correlationId) {
    if( correlationId == null ) {
      throw new IllegalArgumentException("correlationId must not be null");
    }
    this.correlationId = correlationId;
  }
  
  /**
   * Hinzufügen des Serienbezugs: Wer ist Predecessor, wer Successor?
   * @param predecessorsCorrIds
   * @param successorsCorrIds
   */
  public void addToSeries( List<String> predecessorsCorrIds, List<String> successorsCorrIds ) {
    this.predecessorsCorrIds.addAll( predecessorsCorrIds );
    this.successorsCorrIds.addAll( successorsCorrIds );
  }
  /**
   * Hinzufügen des Serienbezugs: Wer ist Predecessor, wer Successor?
   * @param predecessorsCorrIds
   * @param successorsCorrIds
   */
  public void addToSeries( String[] predecessorsCorrIds, String[] successorsCorrIds ) {
    if( predecessorsCorrIds != null ) {
      this.predecessorsCorrIds.addAll( Arrays.asList( predecessorsCorrIds ) );
    }
    if( successorsCorrIds != null ) {
      this.successorsCorrIds.addAll( Arrays.asList( successorsCorrIds ) );
    }
  }

  /**
   * 
   * @param corrId die externe id des predecessor auftrags
   * @return
   */
  public SeriesInformation addPredecessor(String corrId) {
    this.predecessorsCorrIds.add(corrId);
    return this;
  }

  /**
   * @param corrId die externe id des successor auftrags
   * @return
   */
  public SeriesInformation addSuccessor(String corrId) {
    this.successorsCorrIds.add(corrId);
    return this;
  }

  public List<String> getPredecessorsCorrIds() {
    return Collections.unmodifiableList(predecessorsCorrIds);
  }
  public List<String> getSuccessorsCorrIds() {
    return Collections.unmodifiableList(successorsCorrIds);
  }
  
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * falls auftrag schiefgeht, werden nachfolge aufträge auch abgebrochen. default = true.
   * @return the autoCancel
   */
  public boolean isAutoCancel() {
    return autoCancel;
  }

  /**
   * falls auftrag schiefgeht, werden nachfolge aufträge auch abgebrochen. default = true
   * @param autoCancel the autoCancel to set
   */
  public void setAutoCancel(boolean autoCancel) {
    this.autoCancel = autoCancel;
  }

  
  @Override
  public String toString() {
    return "SeriesInformation("+correlationId+",autoCancel="+autoCancel
                    +",pre="+predecessorsCorrIds+",suc="+successorsCorrIds+")";
  }

  ////////////////////////////////////////////////////////////////////
  //
  // Nun folgen alte Methoden, die nicht mehr verwendet werden sollten
  //
  ////////////////////////////////////////////////////////////////////
  
  
  
  /**
   * hängt dieses objekt an die übergebene xynaorder und cached orderseriesmanagement
   * (deprecated)
   * @param parent
   */
  public SeriesInformation(XynaOrder parent) {
    changeParent(parent);
  }
  
  /**
   * @param parent
   * Achtung package private, Zugriff nur in XynaOrder und XynaOrderServerExtension
   */
  void changeParent(XynaOrder parent) {
    if (this.parent != parent) {
      this.parent = parent;
      if( correlationId == null || correlationId.startsWith(CORRELATION_ID_PREFIX) ) {
        correlationId = createCorrelationId(parent);
      }
      parent.setSeriesInformation(this);
      fixReferencesForNewParent();
    }
  }
  
  public static String createCorrelationId( XynaOrder parent ) {
    return CORRELATION_ID_PREFIX + parent.getId();
  }


  /**
   * @deprecated Use {@link #addToSeries(String[], String[])} or {@link #addToSeries(List, List)}
   */
  @Deprecated
 public void addToSeries(XynaOrder[] predecessors, XynaOrder[] successors) {
    if (predecessors != null) {
      synchronized (predecessors) {
        for (XynaOrder xo : predecessors) {
          this.predecessors.add(xo);
        }
      }
    }
    if (successors != null) {
      synchronized (successors) {
        for (XynaOrder xo : successors) {
          this.successors.add(xo);
        }
      }
    }
  }

  /**
   * @return
   * @deprecated
   */
 @Deprecated
  public XynaOrder[] getPredecessors() {
    synchronized (predecessors) {
      return predecessors.toArray(new XynaOrder[predecessors.size()]);
    }
  }

  
  /**
   * @deprecated
   */
 @Deprecated
  public static enum ConnectionStatus {
    predecessorFindersNotFound, fullyConnected, successorFindersNotFound, bothNotFound;
    
  }


  /**
   * @return
   * @deprecated
   */
 @Deprecated
  public XynaOrder[] getSuccessors() {
    synchronized (successors) {
      return successors.toArray(new XynaOrder[successors.size()]);
    }
  }




  /**
   * @deprecated Use {@link #addToSeries(String[], String[])} or {@link #addToSeries(List, List)}
   * @param predecessor
   */
  @Deprecated
  public void addPredecessor(XynaOrder predecessor) {
    synchronized (predecessors) {
      predecessors.add(predecessor);
    }
  }


  /**
   * @deprecated Use {@link #addToSeries(String[], String[])} or {@link #addToSeries(List, List)}
   * @param successor
   */
  @Deprecated
  public void addSuccessor(XynaOrder successor) {
    successors.add(successor);
  }

  public boolean hasFinished() {
    return finishedFlag.get();
  }


  /**
   * referenzen von vorgängern und nachfolgern reparieren
   */
  private void fixReferencesForNewParent() {
    if (predecessors != null) { //kann null sein, wenn das object beim deserialisieren noch nicht vollständig geladen ist
      synchronized (predecessors) {
        for (XynaOrder pre : predecessors) {
          if (pre.getSeriesInformation() != null && pre.getSeriesInformation().successors != null) {
            synchronized (pre.getSeriesInformation().successors) {
              for (int i = 0; i < pre.getSeriesInformation().successors.size(); i++) {
                XynaOrder sucOfPre = pre.getSeriesInformation().successors.get(i);
                if (sucOfPre.getId() == parent.getId()) {
                  if (sucOfPre != parent) {
                    pre.getSeriesInformation().successors.set(i, parent);
                    //logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ungleich SucOfPre: " + sucOfPre + " - " + parent);
                  }
                }
              }
            }
          }
        }
      }
    }
    if (successors != null) {
      synchronized (successors) {
        for (XynaOrder suc : successors) {      
          if (suc.getSeriesInformation() != null && suc.getSeriesInformation().predecessors != null) {
            synchronized (suc.getSeriesInformation().predecessors) {
              for (int i = 0; i < suc.getSeriesInformation().predecessors.size(); i++) {
                XynaOrder preOfSuc = suc.getSeriesInformation().predecessors.get(i);
                if (preOfSuc.getId() == parent.getId()) {
                  if (preOfSuc != parent) {
                    suc.getSeriesInformation().predecessors.set(i, parent);
                    //logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ungleich preOfSuc: " + preOfSuc + " - " + parent);
                  }
                }
              }
            }
          }
        }
      }
    }
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject(); //sollte nie mehr aufgerufen werden
  }  

  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    fixReferencesForNewParent();
  }

  
  public void checkReferences(Collection<XynaOrderServerExtension> orders) {
    // alle nachfolger und vorgänger durch elemente aus der liste austauschen, falls darin vorhanden
    logger.debug("updating series references");
    synchronized (predecessors) {
      for (int i = 0; i<predecessors.size(); i++) {
        XynaOrder pre = predecessors.get(i);
        Iterator<XynaOrderServerExtension> it = orders.iterator();
        while (it.hasNext()) {
          XynaOrderServerExtension order = it.next();
          if (order.getId() == pre.getId()) {
            predecessors.set(i, order);
            break;
          }
        }
      }
    }
    synchronized (successors) {
      for (int i = 0; i<successors.size(); i++) {
        XynaOrder suc = successors.get(i);
        Iterator<XynaOrderServerExtension> it = orders.iterator();
        while (it.hasNext()) {
          XynaOrderServerExtension order = it.next();
          if (order.getId() == suc.getId()) {
            successors.set(i, order);
            break;
          }
        }
      }
    }
    logger.debug("updated series references");
    parent.logDetailsOnTrace();
  }


}
