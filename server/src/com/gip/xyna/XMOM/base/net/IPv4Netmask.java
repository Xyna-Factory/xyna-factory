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
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv4Address;
import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "base.net.IPv4Netmask")
public class IPv4Netmask extends XynaObject {

  private static final long serialVersionUID = -1633625149L;
  private static final Logger logger = CentralFactoryLogging.getLogger(IPv4Netmask.class);

  private IPv4NetmaskData _mask;

  // fake-variable that can be found by XOUtils.getLabelFor(...) when determining labels for fields in getVariableNames()
  @LabelAnnotation(label="Mask")
  private transient IPv4NetmaskData mask;

  /**
   * required for getField
   * when using IPv4Netmask in a mapping and assigning a value to "mask", the type adjustment mechanism
   * calls getField("mask") to determine which type it should adjust to. It should adjust to String
   * therefore a Member of type String is required.
   * Can be removed once XBE-462 is implemented.
   */
  @SuppressWarnings("unused")
  private volatile String _maskString; 

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4NetmaskData> oldVersionsOfmask;

  public String versionedGetMask(long _version) {
    if (oldVersionsOfmask == null) {
      return transformMaskToString(_mask);
    }
    IPv4NetmaskData _local = _mask;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv4NetmaskData> _ret = oldVersionsOfmask.getVersion(_version);
    if (_ret == null) {
      return transformMaskToString(_local);
    }
    return transformMaskToString(_ret.object);
  }
  
  public String getMask() {
    return transformMaskToString(_mask);
  }

  public void setMask(String mask) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4NetmaskData> _vo = oldVersionsOfmask;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmask;
          if (_vo == null) {
            oldVersionsOfmask = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4NetmaskData>();
          }
        }
      }
      IPv4NetmaskData local = transformStringToMask(mask);
      synchronized (_vo) {
        _vo.add(this._mask);
        this._mask = local;
      }
      return;
    }
    this._mask = transformStringToMask(mask);
  }

  public void unversionedSetMask(String mask) {
    this._mask = transformStringToMask(mask);
  }

  public static String transformMaskToString(IPv4NetmaskData m) {
    if (m == null) { 
      return null; 
    }
    return m.getIPv4Address().toDotDecimalString();
  }

  public static IPv4NetmaskData transformStringToMask(String mask) {
    if (mask == null) {
      return null;
    }
    try {
      return new IPv4NetmaskData(mask);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected IPv4NetmaskData getIPv4NetmaskData() {
    return _mask;
  }
  
  protected static class InternalBuilder<_GEN_DOM_TYPE extends IPv4Netmask, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(IPv4Netmask instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public IPv4Netmask instance() {
      return (IPv4Netmask)instance;
    }

    public _GEN_BUILDER_TYPE mask(String mask) {
      this.instance.unversionedSetMask(mask);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<IPv4Netmask, Builder> {
    public Builder() {
      super(new IPv4Netmask());
    }
    public Builder(IPv4Netmask instance) {
      super(instance);
    }
  }

  public Builder buildIPv4Netmask() {
    return new Builder(this);
  }

  public IPv4Netmask() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public IPv4Netmask(String mask) {
    this();
    unversionedSetMask(mask);
  }

  protected void fillVars(IPv4Netmask source, boolean deep) {
    unversionedSetMask(source.getMask());
  }

  public IPv4Netmask clone() {
    return clone(true);
  }

  public IPv4Netmask clone(boolean deep) {
    IPv4Netmask cloned = new IPv4Netmask();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      IPv4Netmask xoc = (IPv4Netmask) xo;
      IPv4Netmask xoco = (IPv4Netmask) other.xo;
      if (!equal(xoc.versionedGetMask(this.version), xoco.versionedGetMask(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      IPv4Netmask xoc = (IPv4Netmask) xo;
      String mask = xoc.versionedGetMask(this.version);
      hash = hash * 31 + (mask == null ? 0 : mask.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfmask, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "IPv4Netmask", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "mask", versionedGetMask(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"mask"})));
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
    String[] varNames = new String[]{"mask"};
    Object[] vars = new Object[]{this.getMask()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("mask".equals(name)) {
      XOUtils.checkCastability(o, String.class, "mask");
      if(o != null) {
        this.setMask((String) o);
      }
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    
    if("mask".equals(target_fieldname)){
      target_fieldname = "_maskString";
    }
    
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = IPv4Netmask.class.getDeclaredField(target_fieldname);
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


  public String toDecimalRepresentation() throws FormatException, ValidationException {
    if (_mask == null) { 
      return null; 
    }
    return _mask.getIPv4Address().toDotDecimalString();
  }

  
  public IPv4 asIPv4() throws FormatException, ValidationException {
    if (_mask == null) { 
      return null; 
    }
    return new IPv4(_mask.getIPv4Address());
  }
  
  public IPv4 toWildcardMask() throws FormatException, ValidationException {
     return new IPv4(new IPv4Address(_mask.getIPInvertedAsLong()));
  }
  
  
  public int prefixLength() throws FormatException, ValidationException {
    if (_mask == null) { 
      return -1; 
    }
    return _mask.getLength();
  }
  
}
