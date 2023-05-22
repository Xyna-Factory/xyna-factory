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
package com.gip.xyna.xdev.xfractmod.xmdm;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.OrderContext;

public abstract class ConnectionFilter<I extends TriggerConnection> extends ResponseListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(ConnectionFilter.class);
  private static final long serialVersionUID = 7269094453154015058L;
  protected boolean autoCloseTriggerConnection = true;
  private long revision;
  
  public enum FilterResponsibility {
    NOT_RESPONSIBLE,                    // Der Filter ist nicht zust�ndig.
    RESPONSIBLE,                        // Der Filter ist zust�ndig und liefert eine XynaOrder.
    RESPONSIBLE_WITHOUT_XYNAORDER,      // Der Filter ist zust�ndig. Der Request wird allerdings direkt im Filter behandelt und dementsprechend keine
                                        // XynaOrder zur�ckgeliefert.
    RESPONSIBLE_BUT_TOO_NEW             // Der Filter ist zust�ndig, aber die Filterversion ist zu neu. Der Request wird an eine �ltere Version
                                        // des Filters weitergeleitet.
  }
  
  public static class FilterResponse {
    
    private XynaOrder order;
    private FilterResponsibility responsibility;
    
    public static FilterResponse responsible(XynaOrder order) {
      return new FilterResponse(order, FilterResponsibility.RESPONSIBLE);
    }
    
    public static FilterResponse notResponsible() {
      return new FilterResponse(null, FilterResponsibility.NOT_RESPONSIBLE);
    }
    
    public static FilterResponse responsibleWithoutXynaorder() {
      return new FilterResponse(null, FilterResponsibility.RESPONSIBLE_WITHOUT_XYNAORDER);
    }
    public static FilterResponse responsibleButTooNew() {
      return new FilterResponse(null, FilterResponsibility.RESPONSIBLE_BUT_TOO_NEW);
    }
    
    private FilterResponse(XynaOrder order, FilterResponsibility responsibility) {
      this.order = order;
      this.responsibility = responsibility;
    }
    
    public XynaOrder getOrder() {
      return order;
    }
    
    public FilterResponsibility getResponsibility() {
      return responsibility;
    }
  }
  
  
  //TODO: hier k�nnte man per generics den richtigen trigger �bergeben, das w�re aber eine schnittstellen�nderung, 
  //weil dann der connectionfilter die triggerklasse auch �ber generics kennen muss.
  @SuppressWarnings("rawtypes")
  public void onDeployment(EventListener trigger) {
    if (logger.isDebugEnabled()) {
      logger.debug("deploying " + this);
    }
  }


  @SuppressWarnings("rawtypes")
  public void onUndeployment(EventListener trigger) {
    if (logger.isDebugEnabled()) {
      logger.debug("undeploying " + this);
    }
  }
  
  public FilterConfigurationParameter createFilterConfigurationTemplate() {
    return null;
  }

  @Deprecated
  public XynaOrder generateXynaOrder(I tc) throws XynaException, InterruptedException {
    return null;
  }

  
  /**
   * Wird vom Processing in einem eigenen Thread aufgerufen, sobald etwas empfangen wurde und ein Workerthread zur
   * Verarbeitung aufgerufen wurde.
   * Wenn der Filter nicht zust�ndig ist, wird ein FilterResponse-Objekt mit leerer XynaOrder
   * und NOT_RESPONSIBLE oder RESPONSIBLE_BUT_TO_NEW als FilterResponsibility zur�ckgeliefert.
   * 
   */
  public FilterResponse createXynaOrder(I tc, FilterConfigurationParameter config) throws XynaException {
    return createXynaOrder(tc); //Default-Implementierung f�r alte Filter
  }
  
  @Deprecated
  public FilterResponse createXynaOrder(I tc) throws XynaException {
    FilterResponse filterResponse;
    try {
      @SuppressWarnings("deprecation")
      XynaOrder result = generateXynaOrder(tc);
    
      if(result == null) {
        filterResponse =  FilterResponse.notResponsible();
      } else {
        filterResponse =  FilterResponse.responsible(result);
      }
    } catch (InterruptedException e) {
      filterResponse = FilterResponse.responsibleWithoutXynaorder();
    }
    return filterResponse;
  }


  // FIXME make the following method abstract and remove the onResponse method accepting only XynaObject
  // das ist nur aus abw�rtskompatibilit�tsgr�nden so wie es ist (umstellung auf generalxynaobject)
  /**
   * Wird aufgerufen, sobald ein Auftrag, der von diesem Filter gestartet wurde, erfolgreich beendet wurde.
   */
  public void onResponse(GeneralXynaObject response, I tc) {
    if (response instanceof XynaObject) {
      onResponse((XynaObject) response, tc);
    } else {
      XynaException ex = new XDEV_UNSUPPORTED_FEATURE("The used filter does not accept "
                      + GeneralXynaObject.class.getSimpleName() + " as response object");
      onError(new XynaException[] {ex}, tc);
    }
  }


  public void onResponse(XynaObject response, I tc) {
    
  }


  /**
   * Wird aufgerufen, sobald ein Auftrag, der von diesem Filter gestartet wurde, fehlerhaft beendet wurde.
   */
  public abstract void onError(XynaException[] e, I tc);


  /**
   * Liefert Beschreibung der Funktion des Filters
   */
  public abstract String getClassDescription();


  /**
   * Hier kommen Antworten zu Auftr�gen an, die durch diesen Eventlistener getriggert worden sind. Das k�nnen
   * unterschiedliche Typen sein.
   */
  @SuppressWarnings("unchecked")
  public void onResponse(GeneralXynaObject c, OrderContext ctx) {
    I tc = (I) ctx.get(EventListener.KEY_CONNECTION);
    try {
      onResponse(c, tc);
    } finally {
      if( autoCloseTriggerConnection ) {
        tc.close();
      }
    }
  }


  @SuppressWarnings("unchecked")
  public void onError(XynaException[] e, OrderContext ctx) {
    I tc = (I) ctx.get(EventListener.KEY_CONNECTION);
    try {
      onError(e, tc);
    } finally {
      if( autoCloseTriggerConnection ) {
        tc.close();
      }
    }
  }

  /** 
   * Achtung: bei Setzen auf false wird TriggerConnection nicht mehr automatisch geschlossen!
   */
  @Override
  protected void setSingleExecutionOnly(boolean singleExecutionOnly) {
    super.setSingleExecutionOnly(singleExecutionOnly);
    if( !singleExecutionOnly ) {
      autoCloseTriggerConnection = false;
    }
  }
  
  /**
   * F�hrt das Runnable nach dem Commit/Close auf die HISTORY-Connection aus
   * Achtung: wenn ein Runnable gesetzt ist, wird die TriggerConnection nicht mehr automatisch geschlossen!
   * @param runnable
   * @throws IllegalStateException wenn keine HISTORY-Connection vorhanden ist
   */
  protected void executeAfterHistoryClose(Runnable runnable) {
    super.executeAfterHistoryClose(runnable);
    autoCloseTriggerConnection = false;
  }


  public void setRevision(long revision) {
    this.revision = revision;
  }


  public long getRevision() {
    return revision;
  }


}
