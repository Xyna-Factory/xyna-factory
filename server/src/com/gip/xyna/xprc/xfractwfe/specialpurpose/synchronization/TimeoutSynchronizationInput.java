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

package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


public class TimeoutSynchronizationInput extends XynaObject {

  private static final long serialVersionUID = -5122158513163996382L;


  @LabelAnnotation(label="Target Correlation Id")
  private String targetCorrelationId;


  public TimeoutSynchronizationInput(String correlationId) {
    this.targetCorrelationId = correlationId;
  }


  @Override
  public XynaObject clone() {
    return new TimeoutSynchronizationInput(targetCorrelationId);
  }
  
  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return toXml(varName, onlyContent);
  }

  @Override
  public String toXml(String varName, boolean onlyContent) {
    // TODO Auto-generated method stub
    return null;
  }


  public Object get(String path) throws InvalidObjectPathException {
    if ("targetCorrelationId".equals(path)) {
      return targetCorrelationId;
    }
    throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("targetCorrelationId".equals(name)) {
      if (value instanceof String) {
        this.targetCorrelationId = (String) value;
      } else {
        this.targetCorrelationId = value.toString();
      }
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


  public String getTargetCorrelationId() {
    return this.targetCorrelationId;
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
      foundField = TimeoutSynchronizationInput.class.getDeclaredField(target_fieldname);
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
