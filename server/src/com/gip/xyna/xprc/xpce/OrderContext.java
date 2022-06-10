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
package com.gip.xyna.xprc.xpce;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;



public class OrderContext implements Serializable {

  private static final long serialVersionUID = 1L;

  protected static final Logger logger = CentralFactoryLogging.getLogger(OrderContext.class);

  private static final String KEY_LOGGING_DIAG_CONTEXT = "xyna.loggingDiagnosisContext";
  
  protected final XynaOrderServerExtension xo;
  
  /**
   * einträge vererben sich auf series-nachfolger.
   */
  protected transient HashMap<String, Serializable> mapForSeriesFamily = null;

  protected OrderContext(XynaOrderServerExtension xo) {
    if (xo == null) {
      throw new IllegalArgumentException("Cannot create order context with XynaOrder=null");
    }
    this.xo = xo;
    if( xo.getParentOrder() != null ) {
      OrderContext parentContext = xo.getParentOrder().getOrderContext();
      setLoggingDiagnosisContext( parentContext.getLoggingDiagnosisContext() );
    }
  }

  protected OrderContext(OrderContext ctx, XynaOrderServerExtension xo) {
    this(xo);
    if (ctx == null) {
      throw new IllegalArgumentException("Provided order context is null");
    }
    this.mapForSeriesFamily = ctx.mapForSeriesFamily;
  }

  
  public Serializable get(String key) {
    if (mapForSeriesFamily == null)
      return null;
    synchronized (this) {
      return mapForSeriesFamily.get(key);
    }
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    synchronized (this) {
      if (mapForSeriesFamily == null) {
        s.writeInt(0);
      } else {
        s.writeInt(mapForSeriesFamily.size());
        Iterator<String> it = mapForSeriesFamily.keySet().iterator();
        while (it.hasNext()) {
          String k = it.next();
          Serializable v = mapForSeriesFamily.get(k);
          s.writeObject(k);
          s.writeObject(SerializableClassloadedObject.useRevisionsIfReachable(v, this.xo.getRevision(), this.xo.getParentRevision()));
        }
      }
    }
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    int size = s.readInt();
    if (size > 0) {
      mapForSeriesFamily = new HashMap<String, Serializable>();
      for (int i = 0; i < size; i++) {
        String k = (String) s.readObject();
        Serializable v = (Serializable) ((SerializableClassloadedObject) s.readObject()).getObject();
        mapForSeriesFamily.put(k, v);
      }
    }
  }


  public long getOrderId() {
    return xo.getId();
  }


  public OrderContext getParentOrderContext() {
    return xo.getParentOrder() != null ? xo.getParentOrder().getOrderContext() : null;
  }
  
  
  public OrderContext getRootOrderContext() {
    return xo.getRootOrder().getOrderContext();
  }


  public String getOrderType() {
    return xo.getDestinationKey().getOrderType();
  }


  public int getPriority() {
    return xo.getPriority();
  }


  //TODO klarmachen, dass das nur während planning funktioniert.
  public void setPriority(int prio) {
    synchronized (this) {
      xo.setPriority(prio);
    }
  }


  public int getMonitoringCode() {
    return xo.getMonitoringCode() == null ? 0 : xo.getMonitoringCode();
  }

  public void setCustom0(String value) {
    synchronized (this) {
      xo.setCustom0(value);
    }
  }

  public void setCustom1(String value) {
    synchronized (this) {
      xo.setCustom1(value);
    }
  }


  public void setCustom2(String value) {
    synchronized (this) {
      xo.setCustom2(value);
    }
  }


  public void setCustom3(String value) {
    synchronized (this) {
      xo.setCustom3(value);
    }
  }


  public String getCustom0() {
    synchronized (this) {
      return xo.getCustom0();
    }
  }


  public String getCustom1() {
    synchronized (this) {
      return xo.getCustom1();
    }
  }


  public String getCustom2() {
    synchronized (this) {
      return xo.getCustom2();
    }
  }


  public String getCustom3() {
    synchronized (this) {
      return xo.getCustom3();
    }
  }


  public String getSessionId() {
    synchronized (this) {
      return xo.getSessionId();
    }
  }


  public RunnableForFilterAccess getRunnableForFilterAccess(String key) {
    return xo.getRunnableForFilterAccess(key);
  }


  public GeneralXynaObject getClonedInputParameters() {
    return xo.getInputPayload().clone();
  }


  public void setInputParametersDuringPlanning(GeneralXynaObject modifiedInputPayload) {
    xo.setInputPayload(modifiedInputPayload);
  }


  public boolean isGeneratedFromAFrequencyControlledOrderCreationTask() {
    if (xo.getMiscellaneousDataBean() != null) {
      return xo.getMiscellaneousDataBean().getFrequencyTaskId() != 0;
    } else {
      return false;
    }
  }


  // TODO encapsulate requests like these into another bean?
  public Long getAssociatedFrequencyControlledOrderCreationTaskId() {
    return xo.getMiscellaneousDataBean() != null ? xo.getMiscellaneousDataBean().getFrequencyTaskId() : null;
  }
  
  /**
   * soll vom projekt nicht aufgerufen werden
   */
  protected void set(String key, Serializable val) {
    synchronized (this) {
      if (mapForSeriesFamily == null)
        mapForSeriesFamily = new HashMap<String, Serializable>();
      mapForSeriesFamily.put(key, val);
    }
  }
  
  /**
   * @return
   */
  public String getLoggingDiagnosisContext() {
    return (String) get(KEY_LOGGING_DIAG_CONTEXT);
  }
  
  /**
   * @param log4jDiagContext the log4jDiagContext to set
   */
  public void setLoggingDiagnosisContext(String log4jDiagContext) {
    set(KEY_LOGGING_DIAG_CONTEXT,log4jDiagContext);
  }
  
  
  public boolean hadErrorsDuring(ProcessingStage stage) {
    return xo.hadErrorsDuring(stage);
  }
  
  public Collection<XynaException> getErrorsFrom(ProcessingStage stage) {
    return xo.getErrorsFrom(stage);
  }
  
  
  public Long getRevision() {
    return this.xo.getRevision();
  }
  
}
