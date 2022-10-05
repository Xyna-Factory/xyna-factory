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
package com.gip.xyna.xfmg.xods.configuration.xynaobjects;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xods.configuration.parameter.CreateProperty")
public class CreateProperty extends BehaviourIfPropertyNotSet {

  private static final long serialVersionUID = 34301441380L;
  private static final Logger logger = CentralFactoryLogging.getLogger(CreateProperty.class);


  @LabelAnnotation(label="Documentation")
  private List<Documentation> documentation;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> oldVersionsOfdocumentation;


  public List<? extends Documentation> getDocumentation() {
    if (supportsObjectVersioning()) {
      if (documentation == null) {
        return documentation;
      }
      return new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList<Documentation>(documentation , new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList<Documentation>() {

        private static final long serialVersionUID = 1L;

        public void update(List<Documentation> _newList) {
          documentation = _newList;
        }

        public com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> getOldVersions() {
          return lazyInitOldVersionsOfdocumentation();
        }

      });
    }
    return documentation;
  }

  private VersionedObject<List<Documentation>> lazyInitOldVersionsOfdocumentation() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> _vo = oldVersionsOfdocumentation;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfdocumentation;
        if (_vo == null) {
          oldVersionsOfdocumentation = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>>();
        }
      }
    }
    return _vo;
  }
  
  public List<? extends Documentation> versionedGetDocumentation(long _version) {
    if (oldVersionsOfdocumentation == null) {
      return documentation;
    }
    List<Documentation> _local = documentation;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<List<Documentation>> _ret = oldVersionsOfdocumentation.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDocumentation(List<Documentation> documentation) {
    documentation = XOUtils.substituteList(documentation);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> _vo = lazyInitOldVersionsOfdocumentation();
      synchronized (_vo) {
        _vo.add(this.documentation);
        this.documentation = documentation;
      }
      return;
    }
    this.documentation = documentation;
  }

  public void unversionedSetDocumentation(List<Documentation> documentation) {
    this.documentation = XOUtils.substituteList(documentation);
  }

  public void addToDocumentation(Documentation e) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> _vo = lazyInitOldVersionsOfdocumentation();
      synchronized (_vo) {
        List<Documentation>__tmp = this.documentation;
        _vo.add(__tmp);
        if (__tmp == null) {
          this.documentation = new ArrayList<Documentation>();
        } else {
          this.documentation = new ArrayList<Documentation>(__tmp.size() + 1);
          this.documentation.addAll(__tmp);
        }
        this.documentation.add(e);
      }
      return;
    }
    if (this.documentation == null) {
      this.documentation = new ArrayList<Documentation>();
    }
    this.documentation.add(e);
  }

  public void removeFromDocumentation(Documentation e) {
    if (this.documentation == null) {
      return;
    }
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Documentation>> _vo = lazyInitOldVersionsOfdocumentation();
      synchronized (_vo) {
        List<Documentation>__tmp = this.documentation;
        _vo.add(__tmp);
        this.documentation = new ArrayList<Documentation>(__tmp);
        this.documentation.remove(e);
      }
      return;
    }
    this.documentation.remove(e);
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends CreateProperty, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends BehaviourIfPropertyNotSet.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(CreateProperty instance) {
      super(instance);
    }

    public CreateProperty instance() {
      return (CreateProperty) instance;
    }

    public _GEN_BUILDER_TYPE documentation(List<Documentation> documentation) {
      this.instance.unversionedSetDocumentation(documentation);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<CreateProperty, Builder> {
    public Builder() {
      super(new CreateProperty());
    }
    public Builder(CreateProperty instance) {
      super(instance);
    }
  }

  public Builder buildCreateProperty() {
    return new Builder(this);
  }

  public CreateProperty() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public CreateProperty(List<? extends Documentation> documentation) {
    this();
    if (documentation != null) {
      this.documentation = new ArrayList<Documentation>(documentation);
    } else {
      this.documentation = new ArrayList<Documentation>();
    }
  }

  protected void fillVars(CreateProperty source, boolean deep) {
    super.fillVars(source, deep);
    this.documentation = XynaObject.cloneList(source.documentation, Documentation.class, false, deep);
  }

  public CreateProperty clone() {
    return clone(true);
  }

  public CreateProperty clone(boolean deep) {
    CreateProperty cloned = new CreateProperty();
    cloned.fillVars(this, deep);
    return cloned;
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = CreateProperty.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return BehaviourIfPropertyNotSet.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }

  public static class ObjectVersion extends com.gip.xyna.xfmg.xods.configuration.xynaobjects.BehaviourIfPropertyNotSet.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      CreateProperty xoc = (CreateProperty) xo;
      CreateProperty xoco = (CreateProperty) other.xo;
      if (!listEqual(xoc.versionedGetDocumentation(this.version), xoco.versionedGetDocumentation(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      CreateProperty xoc = (CreateProperty) xo;
      List<? extends Documentation> documentation = xoc.versionedGetDocumentation(this.version);
      hash = hash * 31 + hashList(documentation, this.version, changeSetsOfMembers, stack);
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
    super.collectChanges(start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexListMember(documentation, oldVersionsOfdocumentation, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "CreateProperty", "xfmg.xods.configuration.parameter", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendDataList(xml, "documentation", "Documentation", "xfmg.xods.configuration.parameter", versionedGetDocumentation(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"documentation"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    HashSet<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVarNames());
    return ret;
  }


  public Set<String> getVariableNames() {
    Set<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVariableNames());
    return Collections.unmodifiableSet(ret);
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"documentation"};
    Object[] vars = new Object[]{getDocumentation()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      o = super.get(name);
      if (o == XOUtils.VARNAME_NOTFOUND) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
      }
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("documentation".equals(name)) {
      if (o != null) {
        if (!(o instanceof List)) {
          throw new IllegalArgumentException("Error while setting member variable documentation, expected list, got " + (o == null ? "null" : o.getClass().getName()) );
        }
        if (((List) o).size() > 0) {
          XOUtils.checkCastability(((List) o).get(0), Documentation.class, "documentation");
        }
      }
      setDocumentation((List<Documentation>) o);
    } else {
      super.set(name, o);
    }
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

}
