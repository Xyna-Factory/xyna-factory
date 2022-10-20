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
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;


public class ResumeOrderBean extends XynaObject {

  private static final long serialVersionUID = -5763635438773974343L;

  @LabelAnnotation(label="Request Succeeded")
  private Boolean requestSucceeded = false;

  @LabelAnnotation(label="Resumed")
  private Boolean resumed = false;

  @LabelAnnotation(label="Target Id")
  private Long targetId; //nur zum deserialisieren existierender ResumeOrderBeans

  @LabelAnnotation(label="Target Labe Id")
  private String targetLaneId; //nur zum deserialisieren existierender ResumeOrderBeans

  @LabelAnnotation(label="Target")
  private ResumeTarget target;

  @LabelAnnotation(label="Retry Count")
  private int retryCount;

  @LabelAnnotation(label="May Not Delegate to Other Node if Order Is Not Found")
  private boolean mayNotDelegateToOtherNodeIfOrderIsNotFound; //Not ist zwar unschön, aber der default soll aus Abwärtskompatiblitätsgründen false sein.

  public ResumeOrderBean(ResumeTarget target) {
    this.target = target;
    this.retryCount = 0;
  }
  
  public ResumeOrderBean(ResumeTarget target, int retryCount) {
    this.target = target;
    this.retryCount = retryCount;
  }
 
  
  @Override
  public XynaObject clone() {
    ResumeOrderBean bobba = new ResumeOrderBean(target,retryCount);
    bobba.setRequestSucceeded(requestSucceeded);
    bobba.setResumed(resumed);
    return bobba;
  }


  public Object get(String name) throws InvalidObjectPathException {
    if ("requestSucceeded".equals(name)) {
      return requestSucceeded;
    }
    else if ("resumed".equals(name)) {
      return resumed;
    }
    else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("requestSucceeded".equals(name)) {
      requestSucceeded = (Boolean) value;
    }
    else if ("resumed".equals(name)) {
      resumed = (Boolean) value;
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
    return null; // TODO is this required somewhere?
  }


  public ResumeOrderBean setRequestSucceeded(boolean requestSucceeded) {
    this.requestSucceeded = requestSucceeded;
    return this;
  }

  
  public boolean isRequestSucceeded() {
    return requestSucceeded;
  }

  
  public ResumeOrderBean setResumed(boolean resumed) {
    this.resumed = resumed;
    return this;
  }


  public boolean isResumed() {
    return resumed;
  }
  
  public ResumeTarget getTarget() {
    return target;
  }

  private Object readResolve() {
    if( target == null ) {
      target = new ResumeTarget(null,targetId,targetLaneId);
    }
    return this; 
   }

  public int getRetryCount() {
    return retryCount;
  }

  public boolean supportsObjectVersioning() {
    return false;
  }

  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
  }

  /**
   * flag zur verhinderung von endlos-delegation an den jeweils anderen clusterknoten in einem fall, wo der orderbackup-eintrag verloren gegangen ist.
   */
  public boolean mayDelegateToOtherNodeIfOrderIsNotFound() {
    return !mayNotDelegateToOtherNodeIfOrderIsNotFound;
  }

  
  public void setMayDelegateToOtherNodeIfOrderIsNotFound(boolean mayDelegateToOtherNodeIfOrderIsNotFound) {
    this.mayNotDelegateToOtherNodeIfOrderIsNotFound = !mayDelegateToOtherNodeIfOrderIsNotFound;
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();


  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = ResumeOrderBean.class.getDeclaredField(target_fieldname);
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
