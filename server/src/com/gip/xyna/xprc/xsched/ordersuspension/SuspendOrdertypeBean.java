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
 package com.gip.xyna.xprc.xsched.ordersuspension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;


public class SuspendOrdertypeBean extends XynaObject {

  private static final long serialVersionUID = -5607726114948121958L;

  @LabelAnnotation(label="Ordertype To Suspend")
  private String ordertypeToSuspend;

  @LabelAnnotation(label="Revision")
  private Long revision;

  @LabelAnnotation(label="Interrupt Stuck Orders")
  private boolean interruptStuckOrders;

  @LabelAnnotation(label="Resume Targets")
  private List<ResumeTarget> resumeTargets;

  @LabelAnnotation(label="Success")
  private boolean success = true;

  @LabelAnnotation(label="Keep Unresumeable")
  private boolean keepUnresumeable;

  @LabelAnnotation(label="Suspended Root Order Ids")
  private List<Long> suspendedRootOrderIds;

  public SuspendOrdertypeBean(String ordertypeToSuspend, boolean interruptStuckOrders, Long revision) { //this could be renamed to something like interrupt orders headed for ...
                                                                                       //or does it, what do we put into those? OrderTypes, JavaNames from the Dispatchers?
    this.interruptStuckOrders = interruptStuckOrders;
    this.ordertypeToSuspend = ordertypeToSuspend;
    this.revision = revision;
    resumeTargets = new ArrayList<ResumeTarget>();
    suspendedRootOrderIds = new ArrayList<Long>();
  }
  
  @Override
  public XynaObject clone() {
    return new SuspendOrdertypeBean(ordertypeToSuspend, interruptStuckOrders, revision).
        addResumeTargets(this.resumeTargets);
  }

  public Object get(String path) throws InvalidObjectPathException {
    if ("ordertypeToSuspend".equals(path)) {
      return ordertypeToSuspend;
    }
    else if ("resumeTargets".equals(path)) {
      return resumeTargets;
    }
    else if ("interruptStuckOrders".equals(path)) {
      return interruptStuckOrders;
    }
    else if ("success".equals(path)) {
      return success;
    }
    else if ("revision".equals(path)) {
      return revision;
    }
    else if ("keepUnresumeable".equals(path)) {
      return keepUnresumeable;
    }
    else if ("suspendedRootOrderIds".equals(path)) {
      return suspendedRootOrderIds;
    }
    else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
    }
  }

  @SuppressWarnings("unchecked")
  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("ordertypeToSuspend".equals(name)) {
      ordertypeToSuspend = (String) value;
    }
    else if ("resumeTargets".equals(name)) {
      resumeTargets = (List<ResumeTarget>) value;
    }
    else if ("interruptStuckOrders".equals(name)) {
      interruptStuckOrders = (Boolean) value;
    }
    else if ("success".equals(name)) {
      success = (Boolean) value;
    } else if ("revision".equals(name)) {
      revision = (Long) value;
    }
    else if ("keepUnresumeable".equals(name)) {
      keepUnresumeable = (Boolean) value;
    }
    else if ("suspendedRootOrderIds".equals(name)) {
      suspendedRootOrderIds = (List<Long>) value;
    }
    else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }

  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return toXml(varName, onlyContent);
  }

  @Override
  public String toXml(String varName, boolean onlyContent) {
    return null;
  }

  public SuspendOrdertypeBean setOrdertypeToSuspend(String ordertypeToSuspend) {
    this.ordertypeToSuspend = ordertypeToSuspend;
    return this;
  }


  public String getOrdertypeToSuspend() {
    return ordertypeToSuspend;
  }
  
  
  public SuspendOrdertypeBean setInterruptStuckOrders(boolean interruptStuckOrders) {
    this.interruptStuckOrders = interruptStuckOrders;
    return this;
  }

  public boolean isInterruptingStuckOrders() {
    return interruptStuckOrders;
  }
  
  
  public SuspendOrdertypeBean addResumeTarget(ResumeTarget resumeTarget) {
    this.resumeTargets.add(resumeTarget);
    return this;
  }
  
  public SuspendOrdertypeBean addResumeTargets(Collection<ResumeTarget> resumeTargets) {
    this.resumeTargets.addAll(resumeTargets);
    return this;
  }


  public boolean wasSuccessfull() {
    return success;
  }
  
  
  public SuspendOrdertypeBean setSuccess(boolean value) {
    this.success = value;
    return this;
  }

  
  public Long getRevision() {
    return revision;
  }

  public List<ResumeTarget> getResumeTargets() {
    return resumeTargets;
  }
  
  public List<Long> getSuspendedRootOrderIds() {
    return suspendedRootOrderIds;
  }
  
  public void addSuspendedRootOrderIds(Collection<Long> suspendedRootOrderIds) {
    this.suspendedRootOrderIds.addAll(suspendedRootOrderIds);
  }
  
  public boolean isKeepUnresumeable() {
    return keepUnresumeable;
  }
  
  public void setKeepUnresumeable(boolean keepUnresumeable) {
    this.keepUnresumeable = keepUnresumeable;
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
      foundField = SuspendOrdertypeBean.class.getDeclaredField(target_fieldname);
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
