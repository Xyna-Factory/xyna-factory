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



public class SuspendAllOrdersBean extends XynaObject {

  private static final long serialVersionUID = -5775838373374060396L;

  @LabelAnnotation(label="Request Succeeded")
  private Boolean requestSucceeded = false;

  @LabelAnnotation(label="Suspend for Shutdown")
  private Boolean suspendForShutdown = false;


  public SuspendAllOrdersBean() {
    requestSucceeded = false;
    suspendForShutdown = false;
  }
  
  public SuspendAllOrdersBean(boolean suspendForShutdown) {
    requestSucceeded = false;
    this.suspendForShutdown = Boolean.valueOf(suspendForShutdown);
  }


  @Override
  public XynaObject clone() {
    return new SuspendAllOrdersBean().setRequestSucceeded(requestSucceeded);
  }


  public Object get(String name) throws InvalidObjectPathException {
    if ("requestSucceeded".equals(name)) {
      return requestSucceeded;
    } else if ("suspendForShutdown".equals(name)) {
      return suspendForShutdown;
    }
    else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("requestSucceeded".equals(name)) {
      requestSucceeded = (Boolean) value;
    } else if ("suspendForShutdown".equals(name)) {
      suspendForShutdown = (Boolean) value;
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
    return null; // FIXME is this required somewhere?
  }


  public SuspendAllOrdersBean setRequestSucceeded(boolean requestSucceeded) {
    this.requestSucceeded = requestSucceeded;
    return this;
  }


  public boolean isRequestSucceeded() {
    return requestSucceeded;
  }
  
  
  public SuspendAllOrdersBean setSuspendForShutdown(boolean suspendForShutdown) {
    this.suspendForShutdown = suspendForShutdown;
    return this;
  }
  

  public boolean isSuspendForShutdown() {
    return suspendForShutdown;
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
      foundField = SuspendAllOrdersBean.class.getDeclaredField(target_fieldname);
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
