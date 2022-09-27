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
package com.gip.xyna.XMOM.base.net;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XMOM.base.net.internal.PortData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "base.net.Port")
public class Port extends XynaObject {

  private static final long serialVersionUID = -63859887293L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Port.class);

  private PortData _portData;

  // fake-variable that can be found by XOUtils.getLabelFor(...) when determining labels for fields in getVariableNames()
  @LabelAnnotation(label="Port Number")
  private transient int portNumber;

  /**
   * required for getField
   * when using Port in a mapping and assigning a value to "portNumber", the type adjustment mechanism
   * calls getField("portNumber") to determine which type it should adjust to. It should adjust to int
   * therefore a Member of type int is required.
   * Can be removed once XBE-462 is implemented.
   */
  @SuppressWarnings("unused")
  private volatile int _portDataInt; 
  

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PortData> oldVersionsOfportNumber;


  public int getPortNumber() {
    return transformDataToInt(_portData);
  }

  public int versionedGetPortNumber(long _version) {
    if (oldVersionsOfportNumber == null) {
      return transformDataToInt(_portData);
    }
    PortData _local = _portData;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<PortData> _ret = oldVersionsOfportNumber.getVersion(_version);
    if (_ret == null) {
      return transformDataToInt(_local);
    }
    return transformDataToInt(_ret.object);
  }

  public void setPortNumber(int portNumber) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PortData> _vo = oldVersionsOfportNumber;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfportNumber;
          if (_vo == null) {
            oldVersionsOfportNumber = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PortData>();
          }
        }
      }
      PortData local = transformIntToPortData(portNumber);
      synchronized (_vo) {
        _vo.add(this._portData);
        this._portData = local;
      }
      return;
    }
    unversionedSetPortNumber(portNumber);
  }

  public void unversionedSetPortNumber(int portNumber) {
    this._portData = transformIntToPortData(portNumber);
  }

  private static int transformDataToInt(PortData pd) {
    if (pd == null) {
      return -1;
    }
    return pd.getPortNumber();
  }

  private static PortData transformIntToPortData(int portNumber) {
    try {
      return new PortData(portNumber);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }      
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends Port, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(Port instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public Port instance() {
      return (Port)instance;
    }

    public _GEN_BUILDER_TYPE portNumber(int portNumber) {
      this.instance.unversionedSetPortNumber(portNumber);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<Port, Builder> {
    public Builder() {
      super(new Port());
    }
    public Builder(Port instance) {
      super(instance);
    }
  }

  public Builder buildPort() {
    return new Builder(this);
  }

  public Port() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public Port(int portNumber) {
    this();
    this.unversionedSetPortNumber(portNumber);
  }

  protected void fillVars(Port source, boolean deep) {
    this.setPortNumber(source.getPortNumber());
  }

  public Port clone() {
    return clone(true);
  }

  public Port clone(boolean deep) {
    Port cloned = new Port();
    cloned.fillVars(this, deep);
    return cloned;
  }
  
  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      Port xoc = (Port) xo;
      Port xoco = (Port) other.xo;
      if (xoc.versionedGetPortNumber(this.version) != xoco.versionedGetPortNumber(other.version)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      Port xoc = (Port) xo;
      int portNumber = xoc.versionedGetPortNumber(this.version);
      hash = hash * 31 + Integer.valueOf(portNumber).hashCode();
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }


  public void collectChanges(long start, long end, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers, java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfportNumber, start, end, datapoints);
  }

  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }

  public String toXml(String varName, boolean onlyContent, long version, com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
    StringBuilder xml = new StringBuilder();
    long objectId;
    if (!onlyContent) {
      long refId;
      if (cache != null) {
        ObjectVersion ov = new ObjectVersion(this, version, cache.changeSetsOfMembers);
        refId = cache.putIfAbsent(ov);
        if (refId > 0) {
          objectId = -2;
        } else {
          objectId = -refId;
          refId = -1;
        }
      } else {
        objectId = -1;
        refId = -1;
      }
      XMLHelper.beginType(xml, varName, "Port", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "portNumber", versionedGetPortNumber(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"portNumber"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    return new HashSet<String>(varNames);
  }


  public Set<String> getVariableNames() {
    return varNames;
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"portNumber"};
    Object[] vars = new Object[]{this.getPortNumber()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("portNumber".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "portNumber");
      if(o != null) {
        this.setPortNumber((Integer) o);
      }
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    
    if ("portNumber".equals(target_fieldname)) {
      target_fieldname = "_portDataInt";
    }
    
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = Port.class.getDeclaredField(target_fieldname);
      if (foundField == null) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
      } else {
        foundField.setAccessible(true);
        fieldMap.put(target_fieldname, foundField);
        return foundField;
      }
    } catch (NoSuchFieldException e) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname, e));
    }
  }

}
