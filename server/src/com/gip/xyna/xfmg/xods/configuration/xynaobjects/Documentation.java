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
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xods.configuration.parameter.Documentation")
public class Documentation extends XynaObject {

  private static final long serialVersionUID = -33789212262300L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Documentation.class);


  private DocumentationLanguage documentationLanguage;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DocumentationLanguage> oldVersionsOfdocumentationLanguage;


  public DocumentationLanguage getDocumentationLanguage() {
    return documentationLanguage;
  }

  public DocumentationLanguage versionedGetDocumentationLanguage(long _version) {
    if (oldVersionsOfdocumentationLanguage == null) {
      return documentationLanguage;
    }
    DocumentationLanguage _local = documentationLanguage;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<DocumentationLanguage> _ret = oldVersionsOfdocumentationLanguage.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDocumentationLanguage(DocumentationLanguage documentationLanguage) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DocumentationLanguage> _vo = oldVersionsOfdocumentationLanguage;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdocumentationLanguage;
          if (_vo == null) {
            oldVersionsOfdocumentationLanguage = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DocumentationLanguage>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.documentationLanguage);
        this.documentationLanguage = documentationLanguage;
      }
      return;
    }
    this.documentationLanguage = documentationLanguage;
  }

  public void unversionedSetDocumentationLanguage(DocumentationLanguage documentationLanguage) {
    this.documentationLanguage = documentationLanguage;
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

  protected static class InternalBuilder<_GEN_DOM_TYPE extends Documentation, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(Documentation instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public Documentation instance() {
      return (Documentation) instance;
    }

    public _GEN_BUILDER_TYPE documentationLanguage(DocumentationLanguage documentationLanguage) {
      this.instance.unversionedSetDocumentationLanguage(documentationLanguage);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE documentation(String documentation) {
      this.instance.unversionedSetDocumentation(documentation);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<Documentation, Builder> {
    public Builder() {
      super(new Documentation());
    }
    public Builder(Documentation instance) {
      super(instance);
    }
  }

  public Builder buildDocumentation() {
    return new Builder(this);
  }

  public Documentation() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public Documentation(DocumentationLanguage documentationLanguage, String documentation) {
    this();
    this.documentationLanguage = documentationLanguage;
    this.documentation = documentation;
  }

  protected void fillVars(Documentation source, boolean deep) {
    this.documentationLanguage = (DocumentationLanguage)XynaObject.clone(source.documentationLanguage, deep);
    this.documentation = source.documentation;
  }

  public Documentation clone() {
    return clone(true);
  }

  public Documentation clone(boolean deep) {
    Documentation cloned = new Documentation();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      Documentation xoc = (Documentation) xo;
      Documentation xoco = (Documentation) other.xo;
      if (!xoEqual(xoc.versionedGetDocumentationLanguage(this.version), xoco.versionedGetDocumentationLanguage(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetDocumentation(this.version), xoco.versionedGetDocumentation(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      Documentation xoc = (Documentation) xo;
      DocumentationLanguage documentationLanguage = xoc.versionedGetDocumentationLanguage(this.version);
      hash = hash * 31 + (documentationLanguage == null ? 0 : documentationLanguage.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      String documentation = xoc.versionedGetDocumentation(this.version);
      hash = hash * 31 + (documentation == null ? 0 : documentation.hashCode());
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
    XOUtils.addChangesForComplexMember(documentationLanguage, oldVersionsOfdocumentationLanguage, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfdocumentation, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "Documentation", "xfmg.xods.configuration.parameter", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "documentationLanguage", versionedGetDocumentationLanguage(version), version, cache);
      XMLHelper.appendData(xml, "documentation", versionedGetDocumentation(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"documentationLanguage", "documentation"})));
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
    String[] varNames = new String[]{"documentationLanguage", "documentation"};
    Object[] vars = new Object[]{this.documentationLanguage, this.documentation};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("documentationLanguage".equals(name)) {
      XOUtils.checkCastability(o, DocumentationLanguage.class, "documentationLanguage");
      setDocumentationLanguage((DocumentationLanguage) o);
    } else if ("documentation".equals(name)) {
      XOUtils.checkCastability(o, String.class, "documentation");
      setDocumentation((String) o);
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


  public String toString() {
    return "Documentation(" + documentationLanguage + "," + (documentation == null ? "null" : ("\"" + documentation + "\"")) + ")";
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = Documentation.class.getDeclaredField(target_fieldname);
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
