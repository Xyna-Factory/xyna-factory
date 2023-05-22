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

package com.gip.xyna.xprc.xsched.ordercancel;



import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;



public class KillStuckProcessBean extends XynaObject {
  private static final long serialVersionUID = 1L;

  @LabelAnnotation(label="Order Id To Be Killed")
  private final long orderIdToBeKilled;

  @LabelAnnotation(label="Force Kill")
  private final boolean forceKill;

  @LabelAnnotation(label="Abort Reason")
  private final AbortionCause abortionReason;

  @LabelAnnotation(label="Ignore Resources When Resuming")
  private final boolean ignoreResourcesWhenResuming;


  @LabelAnnotation(label="Success Free Capacities")
  private boolean successFreeCapacities = false;

  @LabelAnnotation(label="Success Remove Xyna Order")
  private boolean successRemoveXynaOrder = false;

  @LabelAnnotation(label="Success Kill Active Thread")
  private boolean successKillActiveThread = false;

  @LabelAnnotation(label="Result Message")
  private StringBuilder resultMessage = new StringBuilder();


  public KillStuckProcessBean(long orderIdToBeKilled, boolean forceKill, AbortionCause terminationReason) {
    this(orderIdToBeKilled, forceKill, terminationReason, false);
  }


  public KillStuckProcessBean(long orderIdToBeKilled, boolean forceKill, AbortionCause terminationReason,
                              boolean ignoreResourcesWhenResuming) {
    this.orderIdToBeKilled = orderIdToBeKilled;
    this.forceKill = forceKill;
    this.abortionReason = terminationReason;
    this.ignoreResourcesWhenResuming = ignoreResourcesWhenResuming;
  }


  @Override
  public XynaObject clone() {
    KillStuckProcessBean clone = new KillStuckProcessBean(this.orderIdToBeKilled, this.forceKill, this.abortionReason, ignoreResourcesWhenResuming);
    clone.successFreeCapacities = this.successFreeCapacities;
    clone.successKillActiveThread = this.successKillActiveThread;
    clone.successRemoveXynaOrder = this.successRemoveXynaOrder;
    clone.resultMessage = this.resultMessage;
    return clone;
  }


  public Object get(String path) throws InvalidObjectPathException {
    return null; // is this required somewhere?
  }


  public void set(String name, Object value) {
    // is this required somewhere?
  }

  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return toXml(varName, onlyContent);
  }

  @Override
  public String toXml(String varName, boolean onlyContent) {
    return null;
  }

  public boolean isIgnoreResourcesWhenResuming() {
    return ignoreResourcesWhenResuming;
  }

  public long getOrderIdToBeKilled() {
    return this.orderIdToBeKilled;
  }
  
  
  public AbortionCause getTerminationReason() { //TODO - fix inconsistency
    return this.abortionReason;
  }


  public void setHasFreedCapacities() {
    successFreeCapacities = true;
  }


  public boolean hasFreedCapacities() {
    return successFreeCapacities;
  }


  public void setHasRemovedXynaOrder() {
    successRemoveXynaOrder = true;
  }


  public boolean hasRemovedXynaOrder() {
    return successRemoveXynaOrder;
  }


  public void setHasKilledActiveThread() {
    successKillActiveThread = true;
  }


  public boolean hasKilledActiveThread() {
    return successKillActiveThread;
  }


  public boolean forceKill() {
    return forceKill;
  }

  public StringBuilder getResultMessageStringBuilder() {
    return resultMessage;
  }


  public String getResultMessage() {
    return resultMessage.toString();
  }


  public String toString() {
    return "forceKill=" + forceKill + ", reason=" + abortionReason + ", ignoreResourcesWhenResuming="
        + ignoreResourcesWhenResuming;
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
      foundField = KillStuckProcessBean.class.getDeclaredField(target_fieldname);
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
