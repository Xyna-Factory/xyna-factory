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
package com.gip.xyna.update.outdatedclasses_6_1_2_3;

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


public class Container  extends XynaObject {
  
  private static final long serialVersionUID = 1L;
  private GeneralXynaObject[] params;

  private static final Logger logger = CentralFactoryLogging.getLogger(Container.class);


  public Container(GeneralXynaObject... params) {
    this.params = params;
  }


  /**
   * For old services this constructor needs to be supported, otherwhise the services will throw a NoSuchMethodError at
   * runtime
   */
  public Container(XynaObject... params) {
    this.params = params;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (params != null && params.length > 0) {
      for (int i = 0; i < params.length; i++) {
        sb.append(params[i]);
        if (i < params.length - 1)
          sb.append(", ");
      }
    } else {
      sb.append(Container.class.getSimpleName()).append("[]");
    }
    return sb.toString();
  }


  public GeneralXynaObject get(int i) {
    if (i >= params.length || i < 0) {
      throw new IllegalArgumentException("Tried to access an invalid element (requested index '" + i + "', number of entries: '" + params.length + "'");
    }
    return params[i];
  }

  public GeneralXynaObject set(int i, GeneralXynaObject value) {
    if (i >= params.length || i < 0) {
      throw new IllegalArgumentException("Tried to access an invalid element (requested index '" + i + "', number of entries: '" + params.length + "'");
    }
    GeneralXynaObject old = params[i];
    params[i] = value;
    return old;
  }
  

  public void add(GeneralXynaObject xo) {
    if (params == null) {
      params = new GeneralXynaObject[1];
      params[0] = xo;
    } else {
      GeneralXynaObject[] newParams = new GeneralXynaObject[params.length + 1];
      System.arraycopy(params, 0, newParams, 0, params.length);
      newParams[params.length] = xo;
      params = newParams;
    }
  }


  @Override
  public Container clone() {
    return clone(true);
  }

  @Override
  public Container clone(boolean deep) {
    Container newContainer = new Container();
    for (GeneralXynaObject o : params) {
      if (o == null) {
        newContainer.add(null);
      } else {
        if (deep) {
          newContainer.add(o.clone());
        } else {
          newContainer.add(o);
        }
      }
    }
    return newContainer;
  }


  public Object get(String name) throws InvalidObjectPathException {
    if ("params".equals(name)) {
      return params;
    }
    else {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name)); //wegen abwärtskompatibilität nicht direkt werfen
    }
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("params".equals(name)) {
      params = (GeneralXynaObject[]) value;
    }
    else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


  public int size() {
    if (params != null) {
      return params.length;
    } else {
      return 0;
    }
  }


  @Override
  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    StringBuilder xml = new StringBuilder();
    // TODO we should provide a valid XML by enclosing the sum of the substrings by an XML tag
    for (GeneralXynaObject xo : params) {
      if (xo != null) {
        // TODO this has to be passed because when parsing the XML again we can't not expect the variable name to be
        // set. Nevertheless it is somewhat strange...
        xml.append(xo.toXml("n/a", false, version, cache));
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("Tried to generate XML representation of " + getClass().getSimpleName()
                          + " when one of the contained objects was null, result may be corrupted");
        }
        xml.append("<Data/>\n"); //das ist in vielen fällen richtig
      }
    }
    return xml.toString();
  }


  public boolean supportsObjectVersioning() {
    return false;
  }


  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
  }
  
  /*
  public static final ThreadLocal<Stack<Integer>> stackSize = new ThreadLocal<Stack<Integer>>();

  private void readObject(ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    readObjectNoData();
    stream.defaultReadObject();
  }
  
  private void readObjectNoData() throws ObjectStreamException {
    Stack<Integer> s = stackSize.get();
    if (s == null) {
      s = new Stack<Integer>();
      stackSize.set(s);
    }
    s.push(Thread.currentThread().getStackTrace().length);
  }
  
  //Container könnte ein Array mit componenttyp XynaObject enthalten - das ist dann nicht kompatibel mit (General)XynaObjectList-Elementen, die keine XynaObjects sind.
  private Object readResolve() throws ObjectStreamException {
    Stack<Integer> s = stackSize.get();
    s.pop(); //aufräumen TODO eigtl bräuchte man hier nen finally bei der deserialisierung... 
    
    return readResolvePublic();
  }
*/
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = Container.class.getDeclaredField(target_fieldname);
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
  
  
  
  public Object readResolvePublic() {
    if (params == null) {
      return this;
    }
    GeneralXynaObject[] gxoarr = new GeneralXynaObject[params.length];
    for (int i = 0; i<gxoarr.length; i++) {
      gxoarr[i] = params[i];
      if (gxoarr[i] instanceof XynaObjectList) {
        gxoarr[i] = (GeneralXynaObject) ((XynaObjectList<?>) gxoarr[i]).readResolvePublic();
      }
    }
    return new com.gip.xyna.xdev.xfractmod.xmdm.Container(gxoarr);
  }
}
