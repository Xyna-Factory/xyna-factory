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
package xprc.xpce.datatype.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import base.Text;
import xprc.xpce.datatype.DatatypeInspector;
import xprc.xpce.datatype.DatatypeInspectorInstanceOperation;
import xprc.xpce.datatype.DatatypeInspectorSuperProxy;
import xprc.xpce.datatype.InspectionParameter;
import xprc.xpce.datatype.MetaTag;
import xprc.xpce.datatype.NamedVariableMember;
import xprc.xpce.datatype.NamedXMOMMember;


public class DatatypeInspectorInstanceOperationImpl extends DatatypeInspectorSuperProxy implements DatatypeInspectorInstanceOperation {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DatatypeInspectorInstanceOperationImpl.class);
  
  private final static DatatypeInspectorServiceOperationImpl STATIC_INSTANCE = new DatatypeInspectorServiceOperationImpl();
  
  private transient DOM dom;
  private GeneralXynaObject xynaObject;

  public DatatypeInspectorInstanceOperationImpl(DatatypeInspector instanceVar) {
    super(instanceVar);
  }

  public Text getDocumentation() {
    return new Text(dom.getDocumentation());
  }

  public Text getLabel() {
    return new Text(dom.getLabel() );
  }

  public Text getTypeName() {
    return new Text( dom.getFqClassName() );
  }

  @Deprecated
  public xprc.xpce.datatype.DatatypeInspector inspect(GeneralXynaObject anyType) {
    return STATIC_INSTANCE.inspectDatatype(anyType);
  }

  @Deprecated
  public xprc.xpce.datatype.DatatypeInspector inspectByName(XynaOrderServerExtension correlatedXynaOrder, Text text) {
    return STATIC_INSTANCE.inspectDatatypeByName(correlatedXynaOrder, text);
  }

  public Boolean isReservedObject(Text text) {
    return com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.isReservedServerObjectByFqOriginalName(text.getText());
  }

  public List<? extends NamedVariableMember> listAllVariableMembers() {
    return listVariableMembersRecursivly(xynaObject, "", dom, xynaObject, new InspectionParameter(true, null));
  }

  public List<? extends NamedVariableMember> listAllVariableMembersWithParams(InspectionParameter inspectionParameter) {
    return listVariableMembersRecursivly(xynaObject, "", dom, xynaObject, inspectionParameter);
  }

  
  private static String[] getXMLTagAndValue(String xmlEl) throws XPRC_XmlParsingException {
    Document doc = XMLUtils.parseString(xmlEl);
    return new String[] {doc.getDocumentElement().getNodeName(), XMLUtils.getTextContent(doc.getDocumentElement())};
  }
  

  private List<NamedVariableMember> listVariableMembersRecursivly(GeneralXynaObject rootObject, String varNamePrefix,
                                                                  com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase currentGXOContext,
                                                                  GeneralXynaObject currentGXO,
                                                                  InspectionParameter inspectionParameter) {
    try {
      ArrayList<NamedVariableMember> list = new ArrayList<NamedVariableMember>();
      for (com.gip.xyna.xprc.xfractwfe.generation.AVariable var : currentGXOContext
                      .getAllMemberVarsIncludingInherited()) {
        String label = var.getLabel();
        String varName = var.getVarName();
        String documentation = var.getDocumentation();
        String currentVarName = varNamePrefix == "" ? varName : varNamePrefix + "." + varName;
        if (documentation != null &&
            inspectionParameter.getDocumentationRecursionStopMarker() != null &&
            inspectionParameter.getDocumentationRecursionStopMarker().length() > 0 &&
            documentation.contains(inspectionParameter.getDocumentationRecursionStopMarker())) {
          continue;
        }
        List<MetaTag> metaTags = parseUnknownMetaTags(var.getUnknownMetaTags());
        String type;
        if (var.isJavaBaseType()) {
          type = var.getJavaTypeEnum().getFqName();
        } else {
          type = var.getFQClassName();
        }
        boolean isList = var.isList();
        NamedVariableMember nvm = new NamedVariableMember(label, currentVarName, documentation, type, isList, metaTags);
        if (var.isJavaBaseType() || inspectionParameter.getIncludeComplexMembers()) {
          list.add(nvm);
        }
        nvm.setParentObject(rootObject);
        if (!var.isJavaBaseType()) {
          Object nextO = currentGXO.get(varName);
          if (nextO != null) {
            if (nextO instanceof List) {
              for (int i = 0; i < ((List) nextO).size(); i++) {
                GeneralXynaObject elementGXO = (GeneralXynaObject) ((List) nextO).get(i);
                Long revision = com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement.getRevisionByClass(elementGXO.getClass());
                String fqName = elementGXO.getClass().getName();
                DOM elementDOM = STATIC_INSTANCE.getGeneration(fqName, revision);
                list.addAll(listVariableMembersRecursivly(rootObject, currentVarName + "[" + i + "]", elementDOM,
                                                          elementGXO, inspectionParameter));
              }
            } else {
              GeneralXynaObject nextGXO = (GeneralXynaObject) nextO;
              Long revision = com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement
                              .getRevisionByClass(nextGXO.getClass());
              String fqName = nextGXO.getClass().getName();
              DOM nextDOM = STATIC_INSTANCE.getGeneration(fqName, revision);
              list.addAll(listVariableMembersRecursivly(rootObject, currentVarName, nextDOM, nextGXO,
                                                        inspectionParameter));
            }
          }
        }
      }
      return list;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public List<? extends NamedXMOMMember> listAllXynaObjectMembers() {
    ArrayList<NamedXMOMMember> list = new ArrayList<NamedXMOMMember>();
    for( com.gip.xyna.xprc.xfractwfe.generation.AVariable member : dom.getAllMemberVarsIncludingInherited() ) {
      if( ! member.isJavaBaseType() ) {
        String label = member.getLabel();
        String varName = member.getVarName();
        String documentation = member.getDocumentation();
        String fqXMLName = member.getOriginalPath() +"." + member.getOriginalName();
        NamedXMOMMember nxm = new NamedXMOMMember(label, varName, documentation, fqXMLName);
        nxm.setParentObject(xynaObject);
        list.add( nxm );

        Object o = null;
        try {
          o = xynaObject.get(varName);
        } catch( Exception e ) {
          throw new RuntimeException(e);
        }
        if( o instanceof GeneralXynaObject ) {
          nxm.setAnyType( (GeneralXynaObject)o );
          nxm.setDOM((com.gip.xyna.xprc.xfractwfe.generation.DOM)member.getDomOrExceptionObject());
        } else if( o instanceof com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList ) {
          XynaObjectList xol = new XynaObjectList( (List) o, member.getOriginalName(), member.getOriginalPath() );
          nxm.setAnyType(xol);
          nxm.setDOM((com.gip.xyna.xprc.xfractwfe.generation.DOM)member.getDomOrExceptionObject());
        } else if( o == null ) {
          //nicht setzen
          nxm.setDOM((com.gip.xyna.xprc.xfractwfe.generation.DOM)member.getDomOrExceptionObject());
        } else {
          throw new RuntimeException("Member "+label+" has type "+o.getClass().getName() );
        }
      }
    }
    return list;
  }

  public void setDOM(DOM generation) {
    this.dom = generation;
  }

  public void setXynaObject(GeneralXynaObject xynaObject) {
    this.xynaObject = xynaObject;
  }
  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
    s.writeObject(dom.getOriginalFqName());
    s.writeObject(dom.getRuntimeContext());
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
    String fqName = (String)s.readObject();
    RuntimeContext rtc = (RuntimeContext) s.readObject();
    try {
      dom = new DatatypeInspectorServiceOperationImpl()
          .getGeneration(fqName,
                         XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rtc));
    } catch (Exception e) {
      logger.warn("After deserializing the DatatypeInspector instance for " + fqName + " in " + rtc.getGUIRepresentation() + " the DOM Object could not be recreated", e);
    }
  }

  @Override
  public List<? extends MetaTag> getMetaTags() {
    return parseUnknownMetaTags(dom.getUnknownMetaTags());
  }

  private static List<MetaTag> parseUnknownMetaTags(List<String> unknownMetaTags) {
    List<MetaTag> metaTags = new ArrayList<MetaTag>();
    for (String e : unknownMetaTags) {
      String[] kv;
      try {
        kv = getXMLTagAndValue(e);
      } catch (XPRC_XmlParsingException ex) {
        throw new RuntimeException("Could not parse meta tag: " + e, ex);
      }
      metaTags.add(new MetaTag(kv[0], kv[1]));
    }
    return metaTags;
  }
  
 }
