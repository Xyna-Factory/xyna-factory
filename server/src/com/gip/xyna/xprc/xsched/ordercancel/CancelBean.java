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

public class CancelBean extends XynaObject {

  private static final long serialVersionUID = 1L;

  private Long idToBeCanceled;
  private Long relativeTimeout;
  private boolean waitForTimeout;

  private CANCEL_RESULT result;

  public enum CANCEL_RESULT {
    SUCCESS, WORK_IN_PROGRESS, FAILED
  }

  public CancelBean() {
  }

  @Override
  public CancelBean clone() {
    return clone(true);
  }

  @Override
  public CancelBean clone(boolean deep) {
    CancelBean copy = new CancelBean(result, relativeTimeout, idToBeCanceled);
    copy.setWaitForTimeout(waitForTimeout);
    return copy;
  }


  public CancelBean(CANCEL_RESULT result, Long relativeTimeout, Long id) {
    this.result = result;
    this.relativeTimeout = relativeTimeout;
    this.idToBeCanceled = id;
  }


  public Object get(String name) throws InvalidObjectPathException {
    if ("result".equals(name)) {
      return result;
    }
    else if ("idToBeCanceled".equals(name)) {
      return idToBeCanceled;
    }
    else if ("relativeTimeout".equals(name)) {
      return relativeTimeout;
    }
    else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
  }

  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("result".equals(name)) {
      result = (CANCEL_RESULT) value;
    }
    else if ("idToBeCanceled".equals(name)) {
      idToBeCanceled = (Long) value;
    }
    else if ("relativeTimeout".equals(name)) {
      relativeTimeout = (Long) value;
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
    return null; // FIXME is this required anywhere?
  }


  public void setResult(CANCEL_RESULT result) {
    this.result = result;
  }

  public CANCEL_RESULT getResult() {
    return this.result;
  }


  public void setIdToBeCanceled(Long id) {
    this.idToBeCanceled = id;
  }


  public Long getIdToBeCanceled() {
    return this.idToBeCanceled;
  }


  public void setRelativeTimeout(Long relativeTimeout) {
    this.relativeTimeout = relativeTimeout;
  }


  public Long getRelativeTimeout() {
    return this.relativeTimeout;
  }

  public void setWaitForTimeout(boolean waitForTimeout) {
    this.waitForTimeout = waitForTimeout;
  }

  public boolean isWaitForTimeout() {
    return waitForTimeout;
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
      foundField = CancelBean.class.getDeclaredField(target_fieldname);
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
