/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;



public class RedirectionBean extends XynaObject {

  private static final Logger logger = CentralFactoryLogging.getLogger(RedirectionBean.class);

  private static final long serialVersionUID = -7270258773099785417L;

  @LabelAnnotation(label="Failed Step")
  private int failedStep;

  @LabelAnnotation(label="Failed Process")
  private String failedProcess;

  @LabelAnnotation(label="Reason")
  private String reason;

  @LabelAnnotation(label="Redirection Order")
  private XynaOrderServerExtension redirectionOrder; //umleitungsauftrag

  @LabelAnnotation(label="Redirected Order")
  private XynaOrderServerExtension redirectedOrder; //auftrag, der umgeleitet wird


  public RedirectionBean(XynaOrderServerExtension redirectedOrder, int step, String process, String theReason) {
    this.redirectedOrder = redirectedOrder;
    failedStep = step;
    failedProcess = process;
    reason = theReason;
  }


  public Object get(String name) throws InvalidObjectPathException {
    if ("failedStep".equals(name)) {
      return failedStep;
    } else if ("reason".equals(name)) {
      return reason;
    } else if ("failedProcess".equals(name)) {
      return failedProcess;
    } else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("failedStep".equals(name)) {
      failedStep = (Integer) value;
    } else if ("failedProcess".equals(name)) {
      failedProcess = (String) value;
    } else if ("reason".equals(name)) {
      reason = (String) value;
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


  public int getFailedStep() {
    return failedStep;
  }


  public void setFailedStep(int failedStep) {
    this.failedStep = failedStep;
  }


  public String getReason() {
    return reason;
  }


  public void setReason(String reason) {
    this.reason = reason;
  }


  @Override
  public XynaObject clone() {
    return new RedirectionBean(redirectedOrder, failedStep, failedProcess, reason);
  }

  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return toXml(varName, onlyContent);
  }

  @Override
  public String toXml(String varName, boolean onlyContent) {
    return null;
  }


  public void setRedirectionOrder(XynaOrderServerExtension xo) {
    redirectionOrder = xo;
  }


  public XynaOrderServerExtension getRedirectionOrder() {
    return redirectionOrder;
  }


  public XynaOrderServerExtension getRedirectedOrder() {
    return redirectedOrder;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (logger.isTraceEnabled()) {
      logger.trace("Tracing redirected order");
      traceResponseListeners(redirectedOrder);
      logger.trace("Tracing redirection order");
      traceResponseListeners(redirectionOrder);
    }
    out.defaultWriteObject();
  }

  private void traceResponseListeners(XynaOrderServerExtension xo) {
    if (xo == null)
      return;
    for (XynaOrderServerExtension order : xo.getOrderAndChildrenRecursively()) {
      logger.trace("writing responselistener for " + getClass().getSimpleName() + ": " + order.getResponseListener());
    }
  }


  public boolean supportsObjectVersioning() {
    return false;
  }


  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
  }

  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();


  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = RedirectionBean.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
}
