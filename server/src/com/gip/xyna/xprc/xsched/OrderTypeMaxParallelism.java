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
package com.gip.xyna.xprc.xsched;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingChangeListener;
import com.gip.xyna.xprc.xsched.CapacityManagement.CapacityProblemReaction;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.capacities.CapacityChangeListener;


/**
 * OrderTypeMaxParallelism pr�ft, wieviele Auftr�ge eines bestimmten OrderTypes derzeit gestartet 
 * werden k�nnen. Diese Zahl kann sich �ndern durch �nderungen am Mapping OrderType-&gt;Capacities oder 
 * �nderungen an der Capacity selbst.
 * Im Fall einer solchen �nderungen werden alle LimitChangeListener dar�ber informiert.
 */
public class OrderTypeMaxParallelism implements CapacityChangeListener, CapacityMappingChangeListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(OrderTypeMaxParallelism.class);
  private volatile int currentMaxParallelism;
  private final String orderType;
  private final long revision;
  private volatile boolean capacityChanged;
  private Map<MaxParallelismChangeListener, Boolean> listeners; //die changelistener sollen gc-ed werden k�nnen.
  private String limitingCap;
  
  public interface MaxParallelismChangeListener {
    public void maxParallelismChanged();
  }
  
  public OrderTypeMaxParallelism(String orderType, long revision) {
    //achtung: wird beim serverstart beim deployment aufgerufen, wenn als konstante in einem workflow definiert.
    this.orderType = orderType;
    this.revision = revision;
    capacityChanged = true;
    listeners = new WeakHashMap<MaxParallelismChangeListener, Boolean>();
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
    .registerCapacityChangedListener(this);
    XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
    .registerCapacityMappingChangedListener(this);
  }
  
  public OrderTypeMaxParallelism(String orderType) {
    this(orderType, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  private void setCurrentLimit() {
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      //nichts zu tun
      return;
    }
    List<Capacity> caps =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
        .getCapacities(new DestinationKey(orderType, rc));
    int lowest = Integer.MAX_VALUE;
    for (Capacity c : caps) {
      try {
        CapacityInformation ci =
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
            .getCapacityInformation(c.getCapName());
        if (ci.getState() == State.DISABLED) {
          lowest = 0;
          limitingCap = ci.getName();
          break;
        } else {
          int curr = ci.getCardinality() / c.getCardinality() + (ci.getCardinality() % c.getCardinality() == 0 ? 0 : 1); //aufrunden, wenn rest vorhanden
          //int curr = (ci.getCardinality()+c.getCardinality()-1)/c.getCardinality();  //aufrunden, wenn rest vorhanden       w�re besser
          if( curr < lowest ) {
            lowest = curr;
            limitingCap = ci.getName();
          }
        }
      } catch (IllegalArgumentException e) { //FIXME expliziten fehlertyp werfen!
        //capacity existiert nicht. ok (vgl bugz 14399)
        CapacityProblemReaction cpr = XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.get();
        switch (cpr) {
          case Schedule :
            logger.warn("Capacity limit does not include capacity " + c.getCapName()
                        + ". It is configured for ordertype " + orderType
                        + " but does not exist. This setting is consistent with xyna property "
                        + XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.getPropertyName() + ".", e);
            break;
          case Fail :
          case Wait :
            logger.warn("Capacity limit includes capacity " + c.getCapName()
                        + " as if set to zero. It is configured for ordertype " + orderType
                        + " but does not exist. This setting is consistent with xyna property "
                        + XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.getPropertyName() + ".", e);
            lowest = 0;
            break;
        }
      } catch (Exception e) {
        logger.warn("Capacity parallelism limit could not be set properly.", e);
      }
    }
    currentMaxParallelism = lowest;
  }


  public int maxParallelism() {
    if (capacityChanged) {
      setCurrentLimit();
      capacityChanged = false;
    }
    return currentMaxParallelism;
  }


  public void addMaxParallelismChangeListener(MaxParallelismChangeListener changeListener) {
    synchronized (listeners) {
      listeners.put(changeListener, Boolean.TRUE);
    }
  }


  public void capacityChanged(String capacityName) {
    if (!capacityChanged) {
      capacityChanged = true;
      callMaxParallelismChangeListeners();
    }
  }

  public void capacityMappingChanged(DestinationKey key) {
    if (!capacityChanged && key.getOrderType().equals(orderType)) {
      capacityChanged = true;
      callMaxParallelismChangeListeners();
    }
  }

  private void callMaxParallelismChangeListeners() {
    Set<MaxParallelismChangeListener> ls;
    synchronized (listeners) {
      ls = new HashSet<MaxParallelismChangeListener>(listeners.keySet());
    }
    for (MaxParallelismChangeListener lcl : ls) {
      lcl.maxParallelismChanged();
    }
  }

  /**
   * Ausgabe des Namens der limitierenden Capacity
   * @return
   */
  public String getLimitingCapName() {
    return limitingCap;
  }

}

