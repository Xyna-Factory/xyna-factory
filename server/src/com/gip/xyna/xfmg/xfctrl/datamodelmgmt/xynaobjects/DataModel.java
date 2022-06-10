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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects;


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
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xfctrl.datamodel.DataModel")
public class DataModel extends XynaObject {

  private static final long serialVersionUID = -8378170109502714659L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DataModel.class);


  private String dataModelType;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfdataModelType;


  public String getDataModelType() {
    return dataModelType;
  }

  public String versionedGetDataModelType(long _version) {
    if (oldVersionsOfdataModelType == null) {
      return dataModelType;
    }
    String _local = dataModelType;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfdataModelType.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDataModelType(String dataModelType) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfdataModelType;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdataModelType;
          if (_vo == null) {
            oldVersionsOfdataModelType = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.dataModelType);
        this.dataModelType = dataModelType;
      }
      return;
    }
    this.dataModelType = dataModelType;
  }

  public void unversionedSetDataModelType(String dataModelType) {
    this.dataModelType = dataModelType;
  }

  private XmomType type;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType> oldVersionsOftype;


  public XmomType getType() {
    return type;
  }

  public XmomType versionedGetType(long _version) {
    if (oldVersionsOftype == null) {
      return type;
    }
    XmomType _local = type;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<XmomType> _ret = oldVersionsOftype.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setType(XmomType type) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType> _vo = oldVersionsOftype;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOftype;
          if (_vo == null) {
            oldVersionsOftype = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.type);
        this.type = type;
      }
      return;
    }
    this.type = type;
  }

  public void unversionedSetType(XmomType type) {
    this.type = type;
  }

  private XmomType baseType;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType> oldVersionsOfbaseType;


  public XmomType getBaseType() {
    return baseType;
  }

  public XmomType versionedGetBaseType(long _version) {
    if (oldVersionsOfbaseType == null) {
      return baseType;
    }
    XmomType _local = baseType;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<XmomType> _ret = oldVersionsOfbaseType.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setBaseType(XmomType baseType) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType> _vo = oldVersionsOfbaseType;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfbaseType;
          if (_vo == null) {
            oldVersionsOfbaseType = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<XmomType>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.baseType);
        this.baseType = baseType;
      }
      return;
    }
    this.baseType = baseType;
  }

  public void unversionedSetBaseType(XmomType baseType) {
    this.baseType = baseType;
  }

  private String version;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfversion;


  public String getVersion() {
    return version;
  }

  public String versionedGetVersion(long _version) {
    if (oldVersionsOfversion == null) {
      return version;
    }
    String _local = version;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfversion.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setVersion(String version) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfversion;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfversion;
          if (_vo == null) {
            oldVersionsOfversion = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.version);
        this.version = version;
      }
      return;
    }
    this.version = version;
  }

  public void unversionedSetVersion(String version) {
    this.version = version;
  }

  private String documentation;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfdocumentation;


  public String getDocumentation() {
    return documentation;
  }

  public String versionedGetDocumentation(long _version) {
    if (oldVersionsOfdocumentation == null) {
      return documentation;
    }
    String _local = documentation;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfdocumentation.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDocumentation(String documentation) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfdocumentation;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdocumentation;
          if (_vo == null) {
            oldVersionsOfdocumentation = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.documentation);
        this.documentation = documentation;
      }
      return;
    }
    this.documentation = documentation;
  }

  public void unversionedSetDocumentation(String documentation) {
    this.documentation = documentation;
  }

  private List<DataModelSpecific> dataModelSpecifics;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> oldVersionsOfdataModelSpecifics;


  public List<? extends DataModelSpecific> getDataModelSpecifics() {
    if (supportsObjectVersioning()) {
      if (dataModelSpecifics == null) {
        return dataModelSpecifics;
      }
      return new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList<DataModelSpecific>(dataModelSpecifics , new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList<DataModelSpecific>() {

        private static final long serialVersionUID = 1L;

        public void update(List<DataModelSpecific> _newList) {
          dataModelSpecifics = _newList;
        }

        public com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> getOldVersions() {
          return lazyInitOldVersionsOfdataModelSpecifics();
        }

      });
    }
    return dataModelSpecifics;
  }
  
  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> lazyInitOldVersionsOfdataModelSpecifics() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> _vo = oldVersionsOfdataModelSpecifics;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfdataModelSpecifics;
        if (_vo == null) {
          oldVersionsOfdataModelSpecifics = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>>();
        }
      }
    }
    return _vo;
  }

  public List<? extends DataModelSpecific> versionedGetDataModelSpecifics(long _version) {
    if (oldVersionsOfdataModelSpecifics == null) {
      return dataModelSpecifics;
    }
    List<DataModelSpecific> _local = dataModelSpecifics;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<List<DataModelSpecific>> _ret = oldVersionsOfdataModelSpecifics.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDataModelSpecifics(List<DataModelSpecific> dataModelSpecifics) {
    dataModelSpecifics = XOUtils.substituteList(dataModelSpecifics);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> _vo = lazyInitOldVersionsOfdataModelSpecifics();
      synchronized (_vo) {
        _vo.add(this.dataModelSpecifics);
        this.dataModelSpecifics = dataModelSpecifics;
      }
      return;
    }
    this.dataModelSpecifics = dataModelSpecifics;
  }

  public void unversionedSetDataModelSpecifics(List<DataModelSpecific> dataModelSpecifics) {
    this.dataModelSpecifics = XOUtils.substituteList(dataModelSpecifics);
  }

  public void addToDataModelSpecifics(DataModelSpecific e) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> _vo = lazyInitOldVersionsOfdataModelSpecifics();
      synchronized (_vo) {
        List<DataModelSpecific>__tmp = this.dataModelSpecifics;
        _vo.add(__tmp);
        if (__tmp == null) {
          this.dataModelSpecifics = new ArrayList<DataModelSpecific>();
        } else {
          this.dataModelSpecifics = new ArrayList<DataModelSpecific>(__tmp.size() + 1);
          this.dataModelSpecifics.addAll(__tmp);
        }
        this.dataModelSpecifics.add(e);
      }
      return;
    }
    if (this.dataModelSpecifics == null) {
      this.dataModelSpecifics = new ArrayList<DataModelSpecific>();
    }
    this.dataModelSpecifics.add(e);
  }

  public void removeFromDataModelSpecifics(DataModelSpecific e) {
    if (this.dataModelSpecifics == null) {
      return;
    }
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<DataModelSpecific>> _vo = lazyInitOldVersionsOfdataModelSpecifics();
      synchronized (_vo) {
        List<DataModelSpecific>__tmp = this.dataModelSpecifics;
        _vo.add(__tmp);
        this.dataModelSpecifics = new ArrayList<DataModelSpecific>(__tmp);
        this.dataModelSpecifics.remove(e);
      }
      return;
    }
    this.dataModelSpecifics.remove(e);
  }

  private String nodeName;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfnodeName;


  public String getNodeName() {
    return nodeName;
  }

  public String versionedGetNodeName(long _version) {
    if (oldVersionsOfnodeName == null) {
      return nodeName;
    }
    String _local = nodeName;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfnodeName.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setNodeName(String nodeName) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfnodeName;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfnodeName;
          if (_vo == null) {
            oldVersionsOfnodeName = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.nodeName);
        this.nodeName = nodeName;
      }
      return;
    }
    this.nodeName = nodeName;
  }

  public void unversionedSetNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  private boolean local;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> oldVersionsOflocal;


  public boolean getLocal() {
    return local;
  }

  public boolean versionedGetLocal(long _version) {
    if (oldVersionsOflocal == null) {
      return local;
    }
    boolean _local = local;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Boolean> _ret = oldVersionsOflocal.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setLocal(boolean local) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> _vo = oldVersionsOflocal;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOflocal;
          if (_vo == null) {
            oldVersionsOflocal = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.local);
        this.local = local;
      }
      return;
    }
    this.local = local;
  }

  public void unversionedSetLocal(boolean local) {
    this.local = local;
  }

  private List<XmomType> xmomTypes;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> oldVersionsOfxmomTypes;


  public List<? extends XmomType> getXmomTypes() {
    if (supportsObjectVersioning()) {
      if (xmomTypes == null) {
        return xmomTypes;
      }
      return new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList<XmomType>(xmomTypes , new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList<XmomType>() {

        private static final long serialVersionUID = 1L;

        public void update(List<XmomType> _newList) {
          xmomTypes = _newList;
        }

        public com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> getOldVersions() {
          return lazyInitOldVersionsOfxmomTypes();
        }


      });
    }
    return xmomTypes;
  }

  private VersionedObject<List<XmomType>> lazyInitOldVersionsOfxmomTypes() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> _vo = oldVersionsOfxmomTypes;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfxmomTypes;
        if (_vo == null) {
          oldVersionsOfxmomTypes = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>>();
        }
      }
    }
    return _vo;
  }
  
  public List<? extends XmomType> versionedGetXmomTypes(long _version) {
    if (oldVersionsOfxmomTypes == null) {
      return xmomTypes;
    }
    List<XmomType> _local = xmomTypes;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<List<XmomType>> _ret = oldVersionsOfxmomTypes.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setXmomTypes(List<XmomType> xmomTypes) {
    xmomTypes = XOUtils.substituteList(xmomTypes);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> _vo = lazyInitOldVersionsOfxmomTypes();
      synchronized (_vo) {
        _vo.add(this.xmomTypes);
        this.xmomTypes = xmomTypes;
      }
      return;
    }
    this.xmomTypes = xmomTypes;
  }

  public void unversionedSetXmomTypes(List<XmomType> xmomTypes) {
    this.xmomTypes = XOUtils.substituteList(xmomTypes);
  }

  public void addToXmomTypes(XmomType e) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> _vo = lazyInitOldVersionsOfxmomTypes();
      synchronized (_vo) {
        List<XmomType>__tmp = this.xmomTypes;
        _vo.add(__tmp);
        if (__tmp == null) {
          this.xmomTypes = new ArrayList<XmomType>();
        } else {
          this.xmomTypes = new ArrayList<XmomType>(__tmp.size() + 1);
          this.xmomTypes.addAll(__tmp);
        }
        this.xmomTypes.add(e);
      }
      return;
    }
    if (this.xmomTypes == null) {
      this.xmomTypes = new ArrayList<XmomType>();
    }
    this.xmomTypes.add(e);
  }

  public void removeFromXmomTypes(XmomType e) {
    if (this.xmomTypes == null) {
      return;
    }
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<List<XmomType>> _vo = lazyInitOldVersionsOfxmomTypes();
      synchronized (_vo) {
        List<XmomType>__tmp = this.xmomTypes;
        _vo.add(__tmp);
        this.xmomTypes = new ArrayList<XmomType>(__tmp);
        this.xmomTypes.remove(e);
      }
      return;
    }
    this.xmomTypes.remove(e);
  }

  private Integer xmomTypeCount;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfxmomTypeCount;


  public Integer getXmomTypeCount() {
    return xmomTypeCount;
  }

  public Integer versionedGetXmomTypeCount(long _version) {
    if (oldVersionsOfxmomTypeCount == null) {
      return xmomTypeCount;
    }
    Integer _local = xmomTypeCount;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfxmomTypeCount.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setXmomTypeCount(Integer xmomTypeCount) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfxmomTypeCount;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfxmomTypeCount;
          if (_vo == null) {
            oldVersionsOfxmomTypeCount = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.xmomTypeCount);
        this.xmomTypeCount = xmomTypeCount;
      }
      return;
    }
    this.xmomTypeCount = xmomTypeCount;
  }

  public void unversionedSetXmomTypeCount(Integer xmomTypeCount) {
    this.xmomTypeCount = xmomTypeCount;
  }

  private Boolean deployable;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> oldVersionsOfdeployable;


  public Boolean getDeployable() {
    return deployable;
  }

  public Boolean versionedGetDeployable(long _version) {
    if (oldVersionsOfdeployable == null) {
      return deployable;
    }
    Boolean _local = deployable;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Boolean> _ret = oldVersionsOfdeployable.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDeployable(Boolean deployable) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> _vo = oldVersionsOfdeployable;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdeployable;
          if (_vo == null) {
            oldVersionsOfdeployable = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.deployable);
        this.deployable = deployable;
      }
      return;
    }
    this.deployable = deployable;
  }

  public void unversionedSetDeployable(Boolean deployable) {
    this.deployable = deployable;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends DataModel, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(DataModel instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public DataModel instance() {
      return (DataModel) instance;
    }

    public _GEN_BUILDER_TYPE dataModelType(String dataModelType) {
      this.instance.unversionedSetDataModelType(dataModelType);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE type(XmomType type) {
      this.instance.unversionedSetType(type);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE baseType(XmomType baseType) {
      this.instance.unversionedSetBaseType(baseType);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE version(String version) {
      this.instance.unversionedSetVersion(version);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE documentation(String documentation) {
      this.instance.unversionedSetDocumentation(documentation);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE dataModelSpecifics(List<DataModelSpecific> dataModelSpecifics) {
      this.instance.unversionedSetDataModelSpecifics(dataModelSpecifics);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE nodeName(String nodeName) {
      this.instance.unversionedSetNodeName(nodeName);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE local(boolean local) {
      this.instance.unversionedSetLocal(local);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE xmomTypes(List<XmomType> xmomTypes) {
      this.instance.unversionedSetXmomTypes(xmomTypes);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE xmomTypeCount(Integer xmomTypeCount) {
      this.instance.unversionedSetXmomTypeCount(xmomTypeCount);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE deployable(Boolean deployable) {
      this.instance.unversionedSetDeployable(deployable);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<DataModel, Builder> {
    public Builder() {
      super(new DataModel());
    }
    public Builder(DataModel instance) {
      super(instance);
    }
  }

  public Builder buildDataModel() {
    return new Builder(this);
  }

  public DataModel() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public DataModel(String dataModelType, XmomType type, XmomType baseType, String version, String documentation, List<? extends DataModelSpecific> dataModelSpecifics, String nodeName, boolean local, List<? extends XmomType> xmomTypes, Integer xmomTypeCount, Boolean deployable) {
    this();
    this.dataModelType = dataModelType;
    this.type = type;
    this.baseType = baseType;
    this.version = version;
    this.documentation = documentation;
    if (dataModelSpecifics != null) {
      this.dataModelSpecifics = new ArrayList<DataModelSpecific>(dataModelSpecifics);
    } else {
      this.dataModelSpecifics = new ArrayList<DataModelSpecific>();
    }
    this.nodeName = nodeName;
    this.local = local;
    if (xmomTypes != null) {
      this.xmomTypes = new ArrayList<XmomType>(xmomTypes);
    } else {
      this.xmomTypes = new ArrayList<XmomType>();
    }
    this.xmomTypeCount = xmomTypeCount;
    this.deployable = deployable;
  }

  protected void fillVars(DataModel source, boolean deep) {
    this.dataModelType = source.dataModelType;
    this.type = (XmomType)XynaObject.clone(source.type, deep);
    this.baseType = (XmomType)XynaObject.clone(source.baseType, deep);
    this.version = source.version;
    this.documentation = source.documentation;
    this.dataModelSpecifics = XynaObject.cloneList(source.dataModelSpecifics, DataModelSpecific.class, false, deep);
    this.nodeName = source.nodeName;
    this.local = source.local;
    this.xmomTypes = XynaObject.cloneList(source.xmomTypes, XmomType.class, false, deep);
    this.xmomTypeCount = source.xmomTypeCount;
    this.deployable = source.deployable;
  }

  public DataModel clone() {
    return clone(true);
  }

  public DataModel clone(boolean deep) {
    DataModel cloned = new DataModel();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      DataModel xoc = (DataModel) xo;
      DataModel xoco = (DataModel) other.xo;
      if (!equal(xoc.versionedGetDataModelType(this.version), xoco.versionedGetDataModelType(other.version))) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetType(this.version), xoco.versionedGetType(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetBaseType(this.version), xoco.versionedGetBaseType(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetVersion(this.version), xoco.versionedGetVersion(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetDocumentation(this.version), xoco.versionedGetDocumentation(other.version))) {
        return false;
      }
      if (!listEqual(xoc.versionedGetDataModelSpecifics(this.version), xoco.versionedGetDataModelSpecifics(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetNodeName(this.version), xoco.versionedGetNodeName(other.version))) {
        return false;
      }
      if (xoc.versionedGetLocal(this.version) != xoco.versionedGetLocal(other.version)) {
        return false;
      }
      if (!listEqual(xoc.versionedGetXmomTypes(this.version), xoco.versionedGetXmomTypes(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetXmomTypeCount(this.version), xoco.versionedGetXmomTypeCount(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetDeployable(this.version), xoco.versionedGetDeployable(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      DataModel xoc = (DataModel) xo;
      String dataModelType = xoc.versionedGetDataModelType(this.version);
      hash = hash * 31 + (dataModelType == null ? 0 : dataModelType.hashCode());
      XmomType type = xoc.versionedGetType(this.version);
      hash = hash * 31 + (type == null ? 0 : type.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      XmomType baseType = xoc.versionedGetBaseType(this.version);
      hash = hash * 31 + (baseType == null ? 0 : baseType.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      String version = xoc.versionedGetVersion(this.version);
      hash = hash * 31 + (version == null ? 0 : version.hashCode());
      String documentation = xoc.versionedGetDocumentation(this.version);
      hash = hash * 31 + (documentation == null ? 0 : documentation.hashCode());
      List<? extends DataModelSpecific> dataModelSpecifics = xoc.versionedGetDataModelSpecifics(this.version);
      hash = hash * 31 + hashList(dataModelSpecifics, this.version, changeSetsOfMembers, stack);
      String nodeName = xoc.versionedGetNodeName(this.version);
      hash = hash * 31 + (nodeName == null ? 0 : nodeName.hashCode());
      boolean local = xoc.versionedGetLocal(this.version);
      hash = hash * 31 + Boolean.valueOf(local).hashCode();
      List<? extends XmomType> xmomTypes = xoc.versionedGetXmomTypes(this.version);
      hash = hash * 31 + hashList(xmomTypes, this.version, changeSetsOfMembers, stack);
      Integer xmomTypeCount = xoc.versionedGetXmomTypeCount(this.version);
      hash = hash * 31 + (xmomTypeCount == null ? 0 : xmomTypeCount.hashCode());
      Boolean deployable = xoc.versionedGetDeployable(this.version);
      hash = hash * 31 + (deployable == null ? 0 : deployable.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfdataModelType, start, end, datapoints);
    XOUtils.addChangesForComplexMember(type, oldVersionsOftype, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(baseType, oldVersionsOfbaseType, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfversion, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfdocumentation, start, end, datapoints);
    XOUtils.addChangesForComplexListMember(dataModelSpecifics, oldVersionsOfdataModelSpecifics, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfnodeName, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOflocal, start, end, datapoints);
    XOUtils.addChangesForComplexListMember(xmomTypes, oldVersionsOfxmomTypes, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfxmomTypeCount, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfdeployable, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "DataModel", "xfmg.xfctrl.datamodel", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "dataModelType", versionedGetDataModelType(version), version, cache);
      XMLHelper.appendData(xml, "type", versionedGetType(version), version, cache);
      XMLHelper.appendData(xml, "baseType", versionedGetBaseType(version), version, cache);
      XMLHelper.appendData(xml, "version", versionedGetVersion(version), version, cache);
      XMLHelper.appendData(xml, "documentation", versionedGetDocumentation(version), version, cache);
      XMLHelper.appendDataList(xml, "dataModelSpecifics", "DataModelSpecific", "xfmg.xfctrl.datamodel", versionedGetDataModelSpecifics(version), version, cache);
      XMLHelper.appendData(xml, "nodeName", versionedGetNodeName(version), version, cache);
      XMLHelper.appendData(xml, "local", versionedGetLocal(version), version, cache);
      XMLHelper.appendDataList(xml, "xmomTypes", "XmomType", "xfmg.xfctrl.datamodel", versionedGetXmomTypes(version), version, cache);
      XMLHelper.appendData(xml, "xmomTypeCount", versionedGetXmomTypeCount(version), version, cache);
      XMLHelper.appendData(xml, "deployable", versionedGetDeployable(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"dataModelType", "type", "baseType", "version", "documentation", "dataModelSpecifics", "nodeName", "local", "xmomTypes", "xmomTypeCount", "deployable"})));
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
    String[] varNames = new String[]{"dataModelType", "type", "baseType", "version", "documentation", "dataModelSpecifics", "nodeName", "local", "xmomTypes", "xmomTypeCount", "deployable"};
    Object[] vars = new Object[]{this.dataModelType, this.type, this.baseType, this.version, this.documentation, getDataModelSpecifics(), this.nodeName, this.local, this.xmomTypes, this.xmomTypeCount, this.deployable};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("dataModelType".equals(name)) {
      XOUtils.checkCastability(o, String.class, "dataModelType");
      setDataModelType((String) o);
    } else if ("type".equals(name)) {
      XOUtils.checkCastability(o, XmomType.class, "type");
      setType((XmomType) o);
    } else if ("baseType".equals(name)) {
      XOUtils.checkCastability(o, XmomType.class, "baseType");
      setBaseType((XmomType) o);
    } else if ("version".equals(name)) {
      XOUtils.checkCastability(o, String.class, "version");
      setVersion((String) o);
    } else if ("documentation".equals(name)) {
      XOUtils.checkCastability(o, String.class, "documentation");
      setDocumentation((String) o);
    } else if ("dataModelSpecifics".equals(name)) {
      if (o != null) {
        if (!(o instanceof List)) {
          throw new IllegalArgumentException("Error while setting member variable dataModelSpecifics, expected list, got "
              + o.getClass().getName());
        }
        if (((List) o).size() > 0) {
          XOUtils.checkCastability(((List) o).get(0), DataModelSpecific.class, "dataModelSpecifics");
        }
      }
      setDataModelSpecifics((List<DataModelSpecific>) o);
    } else if ("nodeName".equals(name)) {
      XOUtils.checkCastability(o, String.class, "nodeName");
      setNodeName((String) o);
    } else if ("local".equals(name)) {
      XOUtils.checkCastability(o, Boolean.class, "local");
      if (o != null) {
        setLocal((Boolean) o);
      }
    } else if ("xmomTypes".equals(name)) {
      if (o != null) {
        if (!(o instanceof List)) {
          throw new IllegalArgumentException("Error while setting member variable xmomTypes, expected list, got " + o.getClass().getName());
        }
        if (((List) o).size() > 0) {
          XOUtils.checkCastability(((List) o).get(0), XmomType.class, "xmomTypes");
        }
      }
      setXmomTypes((List<XmomType>) o);
    } else if ("xmomTypeCount".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "xmomTypeCount");
      setXmomTypeCount((Integer) o);
    } else if ("deployable".equals(name)) {
      XOUtils.checkCastability(o, Boolean.class, "deployable");
      setDeployable((Boolean) o);
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
      foundField = DataModel.class.getDeclaredField(target_fieldname);
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
  

  /*-------not generated------*/

  public String toString() {
    return "DataModel(" + (dataModelType == null ? "" : "dataModelType=" + ("\"" + dataModelType + "\""))
        + (type == null ? "" : ",type=" + type) + (baseType == null ? "" : ",baseType=" + baseType)
        + (version == null ? "" : ",version=" + ("\"" + version + "\""))
        + (documentation == null ? "" : ",documentation=" + ("\"" + documentation + "\""))
        + (dataModelSpecifics == null ? "" : ",dataModelSpecifics=" + dataModelSpecifics)
        + (nodeName == null ? "" : ",nodeName=" + ("\"" + nodeName + "\"")) + ",local=" + local
        + (xmomTypes == null ? "" : ",xmomTypes=" + xmomTypes) + (xmomTypeCount == null ? "" : ",xmomTypeCount=" + xmomTypeCount)
        + (deployable == null ? "" : ",deployable=" + deployable) + ")";
  }

}
