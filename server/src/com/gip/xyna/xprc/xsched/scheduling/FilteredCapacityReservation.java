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
package com.gip.xyna.xprc.xsched.scheduling;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntryInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityManagementReservationInterface;



/**
 * Implementation der CapacityReservation, die vor der Versendung des CapacityDemands diesen filtert.
 * Diese Filterung dient dazu, �berfl�ssigen Demand nicht kommunizieren und den anderen Scheduler 
 * wecken zu m�ssen. (Ungefiltertes Verschicken kann zu dem Problem f�hren, dass sich zwei Scheduler 
 * gegenseitig mit unerf�llbaren CapacityDemands wachhalten.)
 *
 * Ablauf der CapacityDemand-Sammlung:
 * 1) Scheduler meldet Bedarf an durch �bergabe eines CapacityAllocationResult an 
 *    {@link #addDemand(CapacityAllocationResult, long)}
 * 2) Falls CapacityDemand noch nicht in ownDemands bekannt ist, wird neuer CapacityDemand angelegt.
 *    Dabei wird in {@link #createNewCapacityDemand(String)} ber�cksichtigt, wieviele Capacities �berhaupt 
 *    gefordert werden k�nnen
 * 3) Eintragen der CapacityAllocationResult-Daten in den CapacityDemand;
 *    R�ckgabe, ob CapacityDemand noch weiter erh�ht werden kann oder ob alle forderbaren 
 *    Capacities bereits gefordert werden.
 *
 *
 * Ablauf der Filterung:
 * Es gibt 3 CapacityDemand-Maps: 
 * a) ownDemand: aktuell beim letzen Schedulerlauf gesammelte Demands
 * b) lastSentDemand: beim letzen Mal verschickte Demands
 * c) ignoredDemand: bereits verschickte und daher zu ignorierende Demands
 * 
 * 1) Untersuchung aller Eintr�ge in ignoredDemand: Fehlt Demand (Namensgleichheit) in ownDemand?
 *    Wenn ja: Demand muss nicht mehr in ignoredDemand gespeichert werden, da kein Bedarf 
 *             mehr vorliegt, daher aus ignoredDemand l�schen. 
 *             Leeren Eintrag in ownDemand anlegen, damit anderer Knoten von der Bedarfsdeckung erf�hrt.
 * 2) Untersuchung aller Eintr�ge in lastSentDemand: Fehlt Demand (Namensgleichheit) in ownDemand?
 *    Wenn ja: Falls lastSentDemand-Demand leer ist:
 *             Leeren Eintrag in ownDemand anlegen, damit anderer Knoten von der Bedarfsdeckung erf�hrt.
 * 3) Untersuchung aller Eintr�ge in ownDemand: Ist Demand (Gleichheit) auch in ignoredDemands enthalten?
 *    wenn ja: aus der ownDemand-Map entfernen, da bereits verschickt
 * 4) Untersuchung aller Eintr�ge in ownDemand: Ist Demand (Gleichheit) auch in lastSentDemand enthalten?
 *    wenn ja: aus der ownDemand-Map entfernen, da bereits verschickt;
 *             in ignoredDemand-Map aufnehmen, damit auch in Zukunft ignoriert
 * 5) Verschicken der verbliebenen Eintr�ge in ownDemand
 * 6) lastSentDemand ist nun ownDemand; ownDemand wird f�r n�chsten Schedulerlauf geleert
 * 
 * Die Untersuchungen auf Gleichheit der CapacityDemands bewirken, dass �nderungen an der maximalen Urgency,
 * der Gesamtzahl der wartenden Auftr�ge und der maximal forderbaren Anzahl zu einem erneuten 
 * Verschicken f�hren.
 *    
 * �berlegungen:
 * 1) Was passiert bei gedecktem Bedarf? 
 *    A: Demand wurde vor langer Zeit geschickt, ist daher in ignoredDemands enthalten
 *    B: Demand wurde beim letzten Lauf geschickt, ist daher in lastSentDemand enthalten
 *       * ownDemand enth�lt den Demand nicht mehr
 *       * Schritt 1: A: aus ignoredDemand gel�scht und leer in ownDemand eingetragen
 *                    B: kein Treffer
 *       * Schritt 2: A: kein Treffer
 *                    B: Demand in lastSentDemand ist nicht leer, daher leer in ownDemand eingetragen 
 *       * Schritt 3: kein Treffer
 *       * Schritt 4: kein Treffer
 *       * Schritt 5: leerer Demand wird verschickt
 *       * Schritt 6: leerer Demand wird in lastSentDemand eingetragen
 *       * Beim n�chsten Aufruf:
 *       * Schritt 2: kein leerer Eintrag in ownDemand, daher kein erneutes Verschicken
 * 2) Was passiert bei ge�ndertem Bedarf?
 *    A: Demand wurde vor langer Zeit geschickt, ist daher in ignoredDemands enthalten
 *    B: Demand wurde beim letzten Lauf geschickt, ist daher in lastSentDemand enthalten
 *       * ownDemand enth�lt ge�nderten Demand
 *       * Schritt 1: A: kein Treffer (Namensgleichheit)
 *                    B: kein Treffer (kein Eintrag in ignoredDemand)
 *       * Schritt 2: A: kein Treffer (kein Eintrag in lastSentDemand)
 *                    B: kein Treffer (Namensgleichheit)
 *       * Schritt 3: kein Treffer (keine Gleicheit)
 *       * Schritt 4: kein Treffer (keine Gleicheit)
 *       * Schritt 5: Demand wird verschickt
 *       * Schritt 6: Demand wird in lastSentDemand eingetragen
 * 3) Was passiert, wenn anderer Knoten Bedarf nicht erf�llt?
 *    A: Demand wurde vor langer Zeit geschickt, ist daher in ignoredDemands enthalten
 *    B: Demand wurde beim letzten Lauf geschickt, ist daher in lastSentDemand enthalten
 *       * ownDemand enth�lt bereits bekannten Demand
 *       * Schritt 1: A: kein Treffer (Namensgleichheit)
 *                    B: kein Treffer (kein Eintrag in ignoredDemand)
 *       * Schritt 2: A: kein Treffer (kein Eintrag in lastSentDemand)
 *                    B: kein Treffer (Namensgleichheit)
 *       * Schritt 3: A: aus ownDemand entfernt
 *                    B: kein Treffer
 *       * Schritt 4: A: kein Treffer
 *                    B: aus ownDemand entfernt, in ignoredDemand eingetragen
 *       * Schritt 5: Demand wird nicht verschickt
 *       * Schritt 6: Demand fehlt in lastSentDemand
 * 
 */
public class FilteredCapacityReservation implements CapacityReservation {

  private static Logger logger = CentralFactoryLogging.getLogger(FilteredCapacityReservation.class);
  private static final TimestampFormat TIME_FORMAT = new TimestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  
  private Map<Integer,List<CapacityDemand>> newCapacityDemands; //eingehende Capacity-Forderungen der anderen Knoten zum Schreiben fuer andere Threads
  private ReentrantLock newCapacityDemandsLock = new ReentrantLock(); 
  private CapacityManagementReservationInterface capacityManagement;
  private Map<String,CapacityDemand> ownDemand;
  private Map<String,CapacityDemand> lastSentDemand;
  private Map<String,CapacityDemand> ignoredDemand;
  private List<String> capNameList;
  private Map<String,String> reservedCaps = new HashMap<String,String>();
  
  public FilteredCapacityReservation(CapacityManagementReservationInterface capacityManagement) {
    this.capacityManagement = capacityManagement;
    ownDemand = new ConcurrentHashMap<String, CapacityDemand>();
    lastSentDemand = new ConcurrentHashMap<String, CapacityDemand>();
    ignoredDemand = new ConcurrentHashMap<String,CapacityDemand>();
    newCapacityDemands = new ConcurrentHashMap<Integer, List<CapacityDemand>>();
    capNameList = new ArrayList<String>();
  }


  public int reserveCap(int binding, Capacity capacity) {
    int reserved = capacityManagement.reserveCapForForeignBinding(binding, capacity);
    if( reserved > 0 ) {
      //Capacity wurde f�r anderen Knoten reserviert.
      //daher kann sie auch wieder zur�ckgefordert werden: CapacityDemand hat einen Platz mehr
      CapacityDemand cd = ownDemand.get(capacity.getCapName());
      if( cd != null ) {
        cd.increaseMaxDemand(reserved);
      } else {
        //Problem: reservierte Caps sind in createNewCapacityDemand(..) noch nicht sichtbar, 
        //daher wird dort zu niedriger maxDemand erzeugt.
        //hier createNewCapacityDemand(..) ist aber auchg nicht gut, da dies dann evtl. unn�tigen
        //leeren Demand verschickt.
      }
      
      //nun noch Information loggen und speichern
      StringBuilder sb = new StringBuilder();
      sb.append("reserved ").append(reserved).append(" capacities ");
      sb.append("(").append(capacity.getCapName()).append(",").append(capacity.getCardinality()).append(")");
      sb.append(" for binding ").append(binding);
      if( logger.isDebugEnabled()) {
        logger.debug(sb.toString());
      }
      sb.append(" at ").append(TIME_FORMAT.currentTime());
      reservedCaps.put(capacity.getCapName(),sb.toString()); 
    }
    return reserved;
  }


  public int transportReservedCaps() {
    int transported = capacityManagement.transportReservedCaps();
    if( transported > 0 && logger.isDebugEnabled() ) {
      logger.debug("transportReservedCaps transported "+transported+" caps.");
    }
    return transported;
  }

  public boolean addDemand(CapacityAllocationResult car, long urgency) {
    CapacityDemand cd = ownDemand.get(car.getCapName());
    if( cd == null ) {
      cd = createNewCapacityDemand(car.getCapName());
    }
    boolean max = cd.addUrgency(urgency, car.getDemand());
    if( logger.isTraceEnabled() ) {
      logger.trace( "addDemand("+car +","+ urgency+") -> "+max);
    }
    return max;
  }


  /**
   * @return
   */
  private CapacityDemand createNewCapacityDemand(String capName) {
    CapacityEntryInformation cei = capacityManagement.getCapacityEntryInformation(capName);
    int other = cei.getOtherCardinality(); //alle fordern, die nicht lokal liegen
    
    int maxDemandPercent = XynaProperty.SCHEDULER_CAPACITY_DEMAND_MAX_PERCENT.get();
    int maxDemand = Math.min(other*maxDemandPercent/100+1, other);
    
    CapacityDemand cd = new CapacityDemand(capName, maxDemand);
    ownDemand.put(capName, cd);
    return cd;
  }

  protected void setOwnDemand(CapacityDemand demand) { //Zum Testen praktisch
    ownDemand.put(demand.getCapName(), demand);
  }
  

  public List<CapacityDemand> communicateOwnDemand() {
    //* 1) Untersuchung aller Eintr�ge in ignoredDemand: Fehlt Demand (Namensgleichheit) in ownDemand?
    //*    Wenn ja: Demand muss nicht mehr in ignoredDemand gespeichert werden, da kein Bedarf 
    //*             mehr vorliegt, daher aus ignoredDemand l�schen. 
    //*             Leeren Eintrag in ownDemand anlegen, damit anderer Knoten von der Bedarfsdeckung erf�hrt.
    for( String capName : missingInOwnDemand(ignoredDemand) ) {
      ignoredDemand.remove(capName);
      ownDemand.put( capName, new CapacityDemand(capName) );
    }
    
    //* 2) Untersuchung aller Eintr�ge in lastSentDemand: Fehlt Demand (Namensgleichheit) in ownDemand?
    //*    Wenn ja: Falls lastSentDemand-Demand leer ist:
    //*             Leeren Eintrag in ownDemand anlegen, damit anderer Knoten von der Bedarfsdeckung erf�hrt.
    for( String capName : missingInOwnDemand(lastSentDemand) ) {
      ownDemand.put( capName, new CapacityDemand(capName) );
    }
    
    //* 3) Untersuchung aller Eintr�ge in ownDemand: Ist Demand (Gleichheit) auch in ignoredDemands enthalten?
    //*    wenn ja: aus der ownDemand-Map entfernen, da bereits verschickt
    for( String capName : commonInOwnDemand(ignoredDemand) ) {
      ownDemand.remove(capName);
    }
    
    //* 4) Untersuchung aller Eintr�ge in ownDemand: Ist Demand (Gleichheit) auch in lastSentDemand enthalten?
    //*    wenn ja: aus der ownDemand-Map entfernen, da bereits verschickt;
    //*             in ignoredDemand-Map aufnehmen, damit auch in Zukunft ignoriert, wenn Demand nicht leer ist
    for( String capName : commonInOwnDemand(lastSentDemand) ) {
      CapacityDemand cd = ownDemand.remove(capName);
      if (cd.isFullfilled()) {
        //Demand ist leer: nicht ignorieren, da Demand auch nicht mehr erzeugt wird
        //(ansonsten wird beim n�chsten Lauf durch Punkt 1) wieder ein leerer Demand erzeugt und verschickt.)
      } else {
        CapacityDemand sent = lastSentDemand.get(capName);
        if( sent != null ) {
          ignoredDemand.put(capName,sent); //(get: nur lastSentDemand enth�lt SentTime)
        }
      }
    }
    
    //* 5) Verschicken der verbliebenen Eintr�ge in ownDemand
    List<CapacityDemand> demandToSend = new ArrayList<CapacityDemand>(ownDemand.values());
    if (demandToSend.isEmpty()) {
      logger.trace( "demandToSend is empty" );
    } else {
      if( logger.isDebugEnabled() ) {
        logger.debug( "demandToSend "+ demandToSend );
      }
      long now = System.currentTimeMillis();
      for( CapacityDemand cd : demandToSend ) {
        cd.setSentTime(now);
      }
      boolean success;
      if( Math.random() < XynaProperty.CAPACITY_DEMAND_IGNORING_PERCENTAGE.get() ) {
        success = false; //zuf�llig aussetzen, um Livelocks zu verhindern
      } else {
        success = capacityManagement.communicateDemand(capacityManagement.getOwnBinding(), demandToSend);
        if( !success ) {
          logger.info("Could not send capacityDemand successfully");
        }
      }
      if( !success ) {
        ownDemand.clear(); //wird neu erzeugt, keine Speicherung als versendet in lastSentDemand
        XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
      }
    }
    
    //* 6) lastSentDemand ist nun ownDemand; ownDemand wird f�r n�chsten Schedulerlauf geleert
    Map<String,CapacityDemand> map = lastSentDemand;
    lastSentDemand = ownDemand;
    ownDemand = map;
    ownDemand.clear();

    return demandToSend;
  }



  private List<String> missingInOwnDemand(Map<String, CapacityDemand> cdMap) {
    capNameList.clear();
    for( String capName : cdMap.keySet() ) {
      if( ! ownDemand.containsKey(capName) ) {
        capNameList.add(capName);
      }
    }
    return capNameList;
  }

  private List<String> commonInOwnDemand(Map<String, CapacityDemand> cdMap) {
    capNameList.clear();
    for( Map.Entry<String,CapacityDemand> entry : cdMap.entrySet() ) {
      String capName = entry.getKey();
      CapacityDemand cd = entry.getValue();
      if( cd.equals(ownDemand.get(capName) ) ) {
        capNameList.add(capName);
      }
    }
    return capNameList;
  }

  
  /**
   * Eintragen einer neuen Demand-Liste zu einem bestimmten Knoten. An fr�here Demands des gleichen Knotens wird die
   * neue Liste angeh�ngt, damit die Information, dass der Bedarf gestillt ist, auf jeden Fall ankommt.
   */
  public void setForeignDemand(int binding, List<CapacityDemand> demand) {
    newCapacityDemandsLock.lock();
    try {
      List<CapacityDemand> existingForNode = newCapacityDemands.get(binding);
      if (existingForNode != null) {
        existingForNode.addAll(demand);
      } else {
        newCapacityDemands.put(binding, demand);
      }
    } finally {
      newCapacityDemandsLock.unlock();
    }
  }


  public Map<Integer, List<CapacityDemand>> getForeignDemands() {
    Map<Integer, List<CapacityDemand>> ret = null;
    newCapacityDemandsLock.lock();
    try {
      if (newCapacityDemands.size() > 0) {
        ret = newCapacityDemands;
        newCapacityDemands = new HashMap<Integer, List<CapacityDemand>>();
      } else {
        ret = Collections.emptyMap();
      }
    } finally {
      newCapacityDemandsLock.unlock();
    }
    return ret;
  }


  public void listExtendedSchedulerInfo(StringBuilder sb, List<CapacityDemandForNode> unsatisfiedOldCapacityDemands) {
    if( unsatisfiedOldCapacityDemands.size() == 0 ) { 
      //nach Schedulerlauf ist allForeignCapacityDemands leer!
      sb.append("No CapacityDemand from other node\n");
    } else {
      sb.append("CapacityDemand from other node:\n");
      for( CapacityDemandForNode cdfn : unsatisfiedOldCapacityDemands ) {
        appendCapacityDemand( sb, cdfn );
        sb.append("\n");
      }
    }
    int demands = ignoredDemand.size() + lastSentDemand.size();
    if( demands == 0 ) {
      sb.append("No CapacityDemand for own node\n");
    } else {
      sb.append("CapacityDemand for own node:\n");
      for( CapacityDemand cd : lastSentDemand.values() ) {
        appendCapacityDemand( sb, cd );
        sb.append(", maxDemand=").append(cd.getMaxDemand());
        sb.append(", sent last at ").append(TIME_FORMAT.toString(cd.getSentTime())).append("\n");
      }
      for( CapacityDemand cd : ignoredDemand.values() ) {
        appendCapacityDemand( sb, cd );
        sb.append(", maxDemand=").append(cd.getMaxDemand());
        sb.append(", sent recently at ").append(TIME_FORMAT.toString(cd.getSentTime())).append("\n");
      }
    }
    sb.append("Last reserved capacities:\n");
    for( String rc : reservedCaps.values() ) {
      sb.append(rc).append("\n");
    }
    
  }

  private void appendCapacityDemand(StringBuilder sb, CapacityDemand cd) {
    sb.append(" * ").append(cd.getCapName()).append(": ");
    sb.append("urgency=").append(cd.getMaxUrgency()).
      append(", count=").append(cd.getCount());
  }

  public static class TimestampFormat {

    private SimpleDateFormat sdf;

    public TimestampFormat(String format) {
      this.sdf = new SimpleDateFormat(format);
    }

    public synchronized long parse(String timestamp) throws ParseException {
      return sdf.parse(timestamp).getTime();
    }
    
    public synchronized String toString(long timestamp) {
      return sdf.format(new Date(timestamp) );
    }
    
    public synchronized String currentTime() {
      return sdf.format(new Date() );
    }
    
  }

  public void refreshCapacity(String capName) {
    //Achtung: wird von anderem Thread aufgerufen
    //Capacity-Eintrag wird nun aus beiden Maps entfernt, damit CapacityDemand beim 
    //n�chsten Scheduling auf jeden Fall verschickt wird.
    lastSentDemand.remove(capName);
    ignoredDemand.remove(capName);
  }

  
}
