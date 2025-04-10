/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package com.gip.xyna.xact.filter.xmom.datatypes.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.HasXoRepresentation;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.xmom.MetaXmomContainers;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils.ExtendedContextBuilder;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import xmcp.processmodeller.datatypes.DataTypeTypeLabelArea;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.MetaTag;
import xmcp.processmodeller.datatypes.MetaTagArea;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberVariable;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberVariableArea;


public abstract class DomOrExceptionXo implements HasXoRepresentation {

  static final String ROOT_XML_NS = "http://www.gip.com/xyna/xdev/xfractmod";
  static final String XML_NS = "xmlns";

  private final DomOrExceptionGenerationBase domOrExceptionGbo;
  private final XMOMType type;
  private final List<DatatypeMemberXo> variables;
  private final ObjectIdentifierJson baseType;
  protected final ExtendedContextBuilder contextBuilder;
  protected final GuiHttpPluginManagement pluginMgmt;
  private static final Logger logger = CentralFactoryLogging.getLogger(DomOrExceptionXo.class);

  public DomOrExceptionXo(GenerationBaseObject gbo) {
    this.domOrExceptionGbo = (DomOrExceptionGenerationBase)gbo.getGenerationBase();
    this.type = gbo.getType();
    this.variables = Utils.createDtMembers(domOrExceptionGbo.getAllMemberVarsIncludingInherited(), gbo, Utils.getParents(gbo.getParent()));
    this.baseType = gbo.getParent() != null ? gbo.getParent().getObjectIdentifierJson() : null;
    contextBuilder = new ExtendedContextBuilder(gbo);
    pluginMgmt = GuiHttpPluginManagement.getInstance();
  }

  protected static MemberVariableArea createEmptyMemberVariableArea() {
    MemberVariableArea area = new MemberVariableArea();
    area.setReadonly(false);
    area.setName(Tags.DATA_TYPE_MEMBER_VARS_AREA);
    area.setId(ObjectId.createId(ObjectType.memberVarArea, null));
    area.setItemTypes(Arrays.asList(MetaXmomContainers.DATA_FQN,
                                    MetaXmomContainers.EXCEPTION_FQN,
                                    MetaXmomContainers.DATA_MEMBER_VARIABLE_FQN));
    return area;
  }

  protected MemberVariableArea createMemberVariableArea(String location) {
    MemberVariableArea area = createEmptyMemberVariableArea();
    variables.stream().filter(vmj -> vmj.getInheritedFrom() == null)
      .forEach(vmj -> {
        MemberVariable mv = (MemberVariable) vmj.getXoRepresentation();
        mv.setReadonly(false);
        mv.setDeletable(true);
        area.addToItems((Item)mv);
      }
    );

    area.unversionedSetPlugin(pluginMgmt.createPlugin(contextBuilder.instantiateContext(location, null)));
    return area;
  }

  protected static MemberVariableArea createEmptyInheritedVariablesArea() {
    MemberVariableArea area = new MemberVariableArea();
    area.setReadonly(true);
    area.setName(Tags.DATA_TYPE_INHERITED_VARS_AREA);
    return area;
  }

  protected MemberVariableArea createInheritedVariablesArea() {
    MemberVariableArea area = createEmptyInheritedVariablesArea();
    variables.stream().filter(vmj -> vmj.getInheritedFrom() != null)
      .forEach(vmj -> {
        MemberVariable mv = (MemberVariable) vmj.getXoRepresentation();
        mv.setReadonly(true);
        area.addToItems((Item)mv);
      }
    );
    return area;
  }

  protected static TextArea createEmptyDocumentationArea() {
    TextArea area = new TextArea();
    area.setName(Tags.DATA_TYPE_DOCUMENTATION_AREA);
    area.setId(ObjectId.createDocumentationAreaId(null));
    return area;
  }

  protected TextArea createDocumentationArea(String text, String pluginPath) {
    TextArea area = createEmptyDocumentationArea();
    area.unversionedSetText(text);
    area.unversionedSetPlugin(pluginMgmt.createPlugin(contextBuilder.instantiateContext(pluginPath, null)));
    return area;
  }

  protected DataTypeTypeLabelArea createDataTypeTypeLabelArea() {
    DataTypeTypeLabelArea area = new DataTypeTypeLabelArea();
    area.setIsAbstract(domOrExceptionGbo.isAbstract());
    area.setFqn(domOrExceptionGbo.getOriginalFqName());
    area.setName(Tags.DATA_TYPE_TYPE_INFO_AREA);
    area.setText(domOrExceptionGbo.getLabel());
    area.setId(ObjectId.createId(ObjectType.typeInfoArea, null));

    DomOrExceptionGenerationBase superDoE = domOrExceptionGbo.getSuperClassGenerationObject();

    if (XMOMType.EXCEPTION == type && superDoE != null && XynaExceptionBase.class.getCanonicalName().equals(superDoE.getFqClassName())) {
      area.setBaseType(com.gip.xyna.xact.filter.util.Utils.EXCEPTION_BASE_TYPE_GUI);
    } else if (superDoE != null) {
      area.setBaseType(superDoE.getOriginalFqName());
    }

    return area;
  }

  public static MetaTagArea createEmptyMetaTagArea(String baseId) {
    MetaTagArea area = new MetaTagArea();
    area.unversionedSetName(Tags.META_TAG_AREA);
    area.setId(ObjectId.createId(ObjectType.metaTagArea, baseId));
    return area;
  }


  public static MetaTagArea createMetaTagArea(List<String> unknownMetaTags, boolean readonly) {
    return createMetaTagArea(unknownMetaTags, readonly, null);
  }

  public static MetaTagArea createMetaTagArea(List<String> unknownMetaTags, boolean readonly, String baseId) {
    MetaTagArea result = createEmptyMetaTagArea(baseId);
    result.unversionedSetItemTypes(List.of(MetaTag.class.getCanonicalName()));
    List<Item> list = new ArrayList<Item>();
    if (unknownMetaTags == null) {
      return result;
    }

    for (int i = 0; i < unknownMetaTags.size(); i++) {
      MetaTag tag = new MetaTag.Builder()
          .id(ObjectId.createMetaTagId(i, baseId))
          .tag(removeXmlNs(unknownMetaTags.get(i), ROOT_XML_NS))
          .instance();
      list.add(tag);
    }
    result.unversionedSetItems(list);
    result.unversionedSetReadonly(readonly);
    return result;
  }


  private static String removeXmlNs(String xml, String rootXmlNs) {
    try {
      Element element = XMLUtils.parseString(xml).getDocumentElement();
      for (int i = 0; i < element.getAttributes().getLength(); i++) {
        Node attribute = element.getAttributes().item(i);
        if (attribute.getNodeName().startsWith(XML_NS) && attribute.getNodeValue().startsWith(rootXmlNs)) {
          element.removeAttribute(attribute.getNodeName());
        }
      }

      return XMLUtils.getXMLString(element, false);
    } catch (XPRC_XmlParsingException e) {
      logger.warn("Failed to remove namespace attribute from XML tag " + xml, e);
      return xml;
    }
  }

  public List<DatatypeMemberXo> getVariables() {
    return variables;
  }

  public ObjectIdentifierJson getBaseType() {
    return baseType;
  }

}
