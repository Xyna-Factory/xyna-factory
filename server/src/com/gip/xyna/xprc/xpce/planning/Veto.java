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
package com.gip.xyna.xprc.xpce.planning;



import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;



@XynaObjectAnnotation(fqXmlName = "xprc.Veto")
public class Veto extends XynaObject {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Veto.class);


  @LabelAnnotation(label="Veto Name")
  private String vetoName;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfvetoName;


  public String getVetoName() {
    return vetoName;
  }


  public String versionedGetVetoName(long _version) {
    if (oldVersionsOfvetoName == null) {
      return vetoName;
    }
    String _local = vetoName;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfvetoName.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  public void setVetoName(String vetoName) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfvetoName;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfvetoName;
          if (_vo == null) {
            oldVersionsOfvetoName = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.vetoName);
        this.vetoName = vetoName;
      }
      return;
    }
    this.vetoName = vetoName;
  }


  public void unversionedSetVetoName(String vetoName) {
    this.vetoName = vetoName;
  }


  protected static class InternalBuilder<_GEN_DOM_TYPE extends Veto, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> {

    protected _GEN_DOM_TYPE instance;


    protected InternalBuilder(Veto instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }


    public Veto instance() {
      return (Veto) instance;
    }


    public _GEN_BUILDER_TYPE vetoName(String vetoName) {
      this.instance.unversionedSetVetoName(vetoName);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<Veto, Builder> {

    public Builder() {
      super(new Veto());
    }


    public Builder(Veto instance) {
      super(instance);
    }
  }


  public Builder buildVeto() {
    return new Builder(this);
  }


  public Veto() {
    super();
  }


  /**
  * Creates a new instance using locally defined member variables.
  */
  public Veto(String vetoName) {
    this();
    this.vetoName = vetoName;
  }


  protected void fillVars(Veto source, boolean deep) {
    this.vetoName = source.vetoName;
  }


  public Veto clone() {
    return clone(true);
  }


  public Veto clone(boolean deep) {
    Veto cloned = new Veto();
    cloned.fillVars(this, deep);
    return cloned;
  }


  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version,
                         java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }


    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      Veto xoc = (Veto) xo;
      Veto xoco = (Veto) other.xo;
      if (!equal(xoc.versionedGetVetoName(this.version), xoco.versionedGetVetoName(other.version))) {
        return false;
      }
      return true;
    }


    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      Veto xoc = (Veto) xo;
      String vetoName = xoc.versionedGetVetoName(this.version);
      hash = hash * 31 + (vetoName == null ? 0 : vetoName.hashCode());
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version,
                                           java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }


  public void collectChanges(long start, long end,
                             java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers,
                             java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfvetoName, start, end, datapoints);
  }


  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public String toXml(String varName, boolean onlyContent, long version,
                      com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
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
      XMLHelper.beginType(xml, varName, "Veto", "xprc", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "vetoName", versionedGetVetoName(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {"vetoName"})));


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
    String[] varNames = new String[] {"vetoName"};
    Object[] vars = new Object[] {this.vetoName};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }


  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("vetoName".equals(name)) {
      XOUtils.checkCastability(o, String.class, "vetoName");
      setVetoName((String) o);
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
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = Veto.class.getDeclaredField(target_fieldname);
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


  /*------- not generated -------- */

  @Override
  public String toString() {
    return "Veto(" + (vetoName == null ? "null" : ("\"" + vetoName + "\"")) + ")";
  }
}
