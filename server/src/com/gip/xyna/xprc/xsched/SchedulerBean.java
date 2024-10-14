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
package com.gip.xyna.xprc.xsched;



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
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.Veto;



@XynaObjectAnnotation(fqXmlName = "xprc.SchedulerBean")
public class SchedulerBean extends XynaObject {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SchedulerBean.class);


  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> oldVersionsOfcapacities;


  public List<? extends Capacity> versionedGetCapacities(long _version) {
    if (oldVersionsOfcapacities == null) {
      return capacities;
    }
    List<Capacity> _local = capacities;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<List<Capacity>> _ret = oldVersionsOfcapacities.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  public void unversionedSetCapacities(List<Capacity> capacities) {
    this.capacities = XOUtils.substituteList(capacities);
  }


  public void addToCapacities(Capacity e) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> _vo = lazyInitOldVersionsOfcapacities();
      synchronized (_vo) {
        List<Capacity> __tmp = this.capacities;
        _vo.add(__tmp);
        if (__tmp == null) {
          this.capacities = new ArrayList<Capacity>();
        } else {
          this.capacities = new ArrayList<Capacity>(__tmp.size() + 1);
          this.capacities.addAll(__tmp);
        }
        this.capacities.add(e);
      }
      return;
    }
    if (this.capacities == null) {
      this.capacities = new ArrayList<Capacity>();
    }
    this.capacities.add(e);
  }


  public void removeFromCapacities(Capacity e) {
    if (this.capacities == null) {
      return;
    }
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> _vo = lazyInitOldVersionsOfcapacities();
      synchronized (_vo) {
        List<Capacity> __tmp = this.capacities;
        _vo.add(__tmp);
        this.capacities = new ArrayList<Capacity>(__tmp);
        this.capacities.remove(e);
      }
      return;
    }
    this.capacities.remove(e);
  }


  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> oldVersionsOfvetos;


  public List<? extends Veto> versionedGetVetos(long _version) {
    if (oldVersionsOfvetos == null) {
      return vetos;
    }
    List<Veto> _local = vetos;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<List<Veto>> _ret = oldVersionsOfvetos.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  public void unversionedSetVetos(List<Veto> vetos) {
    this.vetos = XOUtils.substituteList(vetos);
  }


  public void addToVetos(Veto e) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> _vo = lazyInitOldVersionsOfvetos();
      synchronized (_vo) {
        List<Veto> __tmp = this.vetos;
        _vo.add(__tmp);
        if (__tmp == null) {
          this.vetos = new ArrayList<Veto>();
        } else {
          this.vetos = new ArrayList<Veto>(__tmp.size() + 1);
          this.vetos.addAll(__tmp);
        }
        this.vetos.add(e);
      }
      return;
    }
    if (this.vetos == null) {
      this.vetos = new ArrayList<Veto>();
    }
    this.vetos.add(e);
  }


  public void removeFromVetos(Veto e) {
    if (this.vetos == null) {
      return;
    }
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> _vo = lazyInitOldVersionsOfvetos();
      synchronized (_vo) {
        List<Veto> __tmp = this.vetos;
        _vo.add(__tmp);
        this.vetos = new ArrayList<Veto>(__tmp);
        this.vetos.remove(e);
      }
      return;
    }
    this.vetos.remove(e);
  }


  protected static class InternalBuilder<_GEN_DOM_TYPE extends SchedulerBean, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> {

    protected _GEN_DOM_TYPE instance;


    protected InternalBuilder(SchedulerBean instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }


    public SchedulerBean instance() {
      return (SchedulerBean) instance;
    }


    public _GEN_BUILDER_TYPE capacities(List<Capacity> capacities) {
      this.instance.unversionedSetCapacities(capacities);
      return (_GEN_BUILDER_TYPE) this;
    }


    public _GEN_BUILDER_TYPE vetos(List<Veto> vetos) {
      this.instance.unversionedSetVetos(vetos);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<SchedulerBean, Builder> {

    public Builder() {
      super(new SchedulerBean());
    }


    public Builder(SchedulerBean instance) {
      super(instance);
    }
  }


  public Builder buildSchedulerBean() {
    return new Builder(this);
  }


  public SchedulerBean() {
    super();
  }


  /**
  * Creates a new instance using locally defined member variables.
  */
  public SchedulerBean(List<? extends Capacity> capacities, List<? extends Veto> vetos) {
    this();
    if (capacities != null) {
      this.capacities = new ArrayList<Capacity>(capacities);
    } else {
      this.capacities = new ArrayList<Capacity>();
    }
    if (vetos != null) {
      this.vetos = new ArrayList<Veto>(vetos);
    } else {
      this.vetos = new ArrayList<Veto>();
    }
  }


  protected void fillVars(SchedulerBean source, boolean deep) {
    this.capacities = XynaObject.cloneList(source.capacities, Capacity.class, false, deep);
    this.vetos = XynaObject.cloneList(source.vetos, Veto.class, false, deep);
  }


  public SchedulerBean clone() {
    return clone(true);
  }


  public SchedulerBean clone(boolean deep) {
    SchedulerBean cloned = new SchedulerBean();
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
      SchedulerBean xoc = (SchedulerBean) xo;
      SchedulerBean xoco = (SchedulerBean) other.xo;
      if (!listEqual(xoc.versionedGetCapacities(this.version), xoco.versionedGetCapacities(other.version), this.version, other.version,
                     changeSetsOfMembers)) {
        return false;
      }
      if (!listEqual(xoc.versionedGetVetos(this.version), xoco.versionedGetVetos(other.version), this.version, other.version,
                     changeSetsOfMembers)) {
        return false;
      }
      return true;
    }


    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      SchedulerBean xoc = (SchedulerBean) xo;
      List<? extends Capacity> capacities = xoc.versionedGetCapacities(this.version);
      hash = hash * 31 + hashList(capacities, this.version, changeSetsOfMembers, stack);
      List<? extends Veto> vetos = xoc.versionedGetVetos(this.version);
      hash = hash * 31 + hashList(vetos, this.version, changeSetsOfMembers, stack);
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
    XOUtils.addChangesForComplexListMember(capacities, oldVersionsOfcapacities, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexListMember(vetos, oldVersionsOfvetos, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "SchedulerBean", "xprc", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendDataList(xml, "capacities", "Capacity", "xprc", versionedGetCapacities(version), version, cache);
      XMLHelper.appendDataList(xml, "vetos", "Veto", "xprc", versionedGetVetos(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections
      .unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {"capacities", "vetos"})));


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



  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("capacities".equals(name)) {
      if (o != null) {
        if (!(o instanceof List)) {
          throw new IllegalArgumentException("Error while setting member variable capacities, expected list, got "
              + (o == null ? "null" : o.getClass().getName()));
        }
        if (((List) o).size() > 0) {
          XOUtils.checkCastability(((List) o).get(0), Capacity.class, "capacities");
        }
      }
      setCapacities((List<Capacity>) o);
    } else if ("vetos".equals(name)) {
      if (o != null) {
        if (!(o instanceof List)) {
          throw new IllegalArgumentException("Error while setting member variable vetos, expected list, got "
              + (o == null ? "null" : o.getClass().getName()));
        }
        if (((List) o).size() > 0) {
          XOUtils.checkCastability(((List) o).get(0), Veto.class, "vetos");
        }
      }
      setVetos((List<Veto>) o);
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


  /*------- not generated -------- */

  @LabelAnnotation(label="Capacities")
  protected List<Capacity> capacities;

  @LabelAnnotation(label="Vetos")
  protected List<Veto> vetos;


  public SchedulerBean(List<Capacity> caps) {
    capacities = caps;
  }

  public List<? extends Capacity> getCapacitiesNoLazyCreate() {
    return capacities;
  }


  List<? extends Veto> getVetosNoLazyCreate() {
    return vetos;
  }


  private void vetosLazyCreate() {
    if (vetos == null) {
      vetos = new ArrayList<Veto>();
    }
  }


  private void capacitiesLazyCreate() {
    if (capacities == null) {
      capacities = new ArrayList<Capacity>();
    }
  }


  public List<? extends Capacity> getCapacities() {    
    capacitiesLazyCreate();
    if (supportsObjectVersioning()) {
      return new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList<Capacity>(capacities , new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList<Capacity>() {

        private static final long serialVersionUID = 1L;

        public void update(List<Capacity> _newList) {
          capacities = _newList;
        }

        public com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> getOldVersions() {
          return lazyInitOldVersionsOfcapacities();
        }

      });
    }
    return capacities;
  }

  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> lazyInitOldVersionsOfcapacities() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> _vo = oldVersionsOfcapacities;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfcapacities;
        if (_vo == null) {
          oldVersionsOfcapacities = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>>();
        }
      }
    }
    return _vo;
  }
  
  public List<? extends Veto> getVetos() {
    vetosLazyCreate();
    if (supportsObjectVersioning()) {
      return new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList<Veto>(vetos , new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList<Veto>() {

        private static final long serialVersionUID = 1L;

        public void update(List<Veto> _newList) {
          vetos = _newList;
        }

        public com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> getOldVersions() {
          return lazyInitOldVersionsOfvetos();
        }

      });
    }
    return vetos;
  }

  private VersionedObject<List<Veto>> lazyInitOldVersionsOfvetos() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> _vo = oldVersionsOfvetos;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfvetos;
        if (_vo == null) {
          oldVersionsOfvetos = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>>();
        }
      }
    }
    return _vo;
  }
  
  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[] {"capacities", "vetos"};
    Object[] vars = new Object[] {this.capacities, this.vetos};
    capacitiesLazyCreate();
    vetosLazyCreate();
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }


  //PERFORMANCE Capacitymanagement
  private Capacity[] capArray;
  private static Capacity[] EMPTY_ARRAY = new Capacity[0];


  public Capacity[] getCapsAsArray() {
    if (capArray == null) {
      int size = capacities != null ? capacities.size() : 0;
      if (size == 0) {
        capArray = EMPTY_ARRAY;
      } else {
        capArray = new Capacity[size];
        capArray = capacities.toArray(capArray);
      }
    }
    return capArray;
  }


  public String toString() {
    return "SchedulerBean(" + capacities + "," + vetos + ")";
  }


  public List<? extends Capacity> unversionedGetCapacities() {
    capacitiesLazyCreate();
    return capacities;
  }


  public List<? extends Veto> unversionedGetVetos() {
    vetosLazyCreate();
    return vetos;
  }

  public void setVetos(List<? extends Veto> vetos) {
    List<Veto> list = vetos != null
      ? new ArrayList<Veto>(vetos)
      : new ArrayList<Veto>();
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Veto>> _vo = lazyInitOldVersionsOfvetos();
      synchronized (_vo) {
        _vo.add(this.vetos);
        this.vetos = list;
      }
      return;
    }
    this.vetos = list;
  }


  public void setCapacities(List<? extends Capacity> capacities) {
    List<Capacity> list = capacities != null
      ? new ArrayList<Capacity>(capacities)
      : new ArrayList<Capacity>();
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<Capacity>> _vo = lazyInitOldVersionsOfcapacities();
      synchronized (_vo) {
        _vo.add(this.capacities);
        this.capacities = list;
      }
      return;
    }
    this.capacities = list;
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = SchedulerBean.class.getDeclaredField(target_fieldname);
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
