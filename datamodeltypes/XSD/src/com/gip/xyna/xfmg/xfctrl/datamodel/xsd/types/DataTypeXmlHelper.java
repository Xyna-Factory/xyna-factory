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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo.Type;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember.TypeInfoMemberBuilder;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DataModelInformation;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.DataModel;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Meta;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable.VariableBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class DataTypeXmlHelper {

  private static Logger logger = CentralFactoryLogging.getLogger(DataTypeXmlHelper.class);
  
  private GenerationParameter generationParameter;

  public DataTypeXmlHelper() {
    this.generationParameter = null;//TODO unschön, Daten werden beim Parsen nicht benötigt
  }
  
  public DataTypeXmlHelper(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
  }
 
  public Datatype toDatatype(TypeInfo typeInfo, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel dm) {
    List<Variable> vs = new ArrayList<Variable>();
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      vs.add( createVariable(typeInfo, tim) );
    }
    return Datatype.create(typeInfo.getXmomType()).
             basetype(getBaseType(typeInfo)).
             meta( generationParameter.isGenerateDataModelInfo() ? meta_dataModel_Type(typeInfo, dm) : null ).
             variables(vs).
             build();
  }

  public TypeInfo toTypeInfo(Datatype dataType) throws XPRC_XmlParsingException, Ex_FileAccessException, ClassNotFoundException {
    return parse(dataType);
  }
  
  public TypeInfo toTypeInfo(DOM dom) throws XPRC_XmlParsingException, Ex_FileAccessException {
    return parse(dom);
  }
  
  private Variable createVariable(TypeInfo parent, TypeInfoMember tim) {
    VariableBuilder vb = Variable.create(tim.getVarName()).
        label(tim.getLabel()).
        isList(tim.isList());
    if( generationParameter.isGenerateDataModelInfo() ) {
      vb.meta(meta_dataModel_Member(parent, tim));
    }
    if( tim.isSimple() ) {
      vb.simpleType(tim.getSimpleType());
    } else {
      vb.complexType(tim.getComplexType().getXmomType());
    }
    return vb.build();
  }

  private XmomType getBaseType(TypeInfo typeInfo) {
    if( typeInfo.getBaseTypeInfo() != null ) {
      return typeInfo.getBaseTypeInfo().getXmomType();
    } else {
      return Constants.getBase_XmomType();
    }
  }
  
  
  private Meta meta_dataModel_Type(TypeInfo typeInfo, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel dm) {
    XSDDataModel_Type xdm = new XSDDataModel_Type(typeInfo);
    DataModel dataModel = DataModel.modelName(dm.getType().getFqName(), xdm);
    return Meta.dataModel(dataModel);
  }

  private Meta meta_dataModel_Member(TypeInfo parent, TypeInfoMember member) {
    XSDDataModel_Member xdm = new XSDDataModel_Member(parent,member);
    DataModel dataModel = DataModel.modelName(null, xdm);
    return Meta.dataModel(dataModel);
  }
  
  
  public static abstract class XSDDataModel implements XmlAppendable {
    protected static final String EL_XSD_INFO = "XSDInfo";
    protected static final String EL_XSD = "XSD";
    
    protected static final String ATTR_NAMESPACE = "Namespace";
    protected static final String ATTR_NAME = "Name";
    
    public void appendXML(XmlBuilder xml) {
      XmlBuilder local = new XmlBuilder();
      appendXsdData(local);
      String xsdInfo = local.toString();
      if( xsdInfo.endsWith(XmlBuilder.LINE_SEPARATOR)) {
        xsdInfo = xsdInfo.substring(0,xsdInfo.length()-XmlBuilder.LINE_SEPARATOR.length());
      }
      xml.element(EL_XSD_INFO, XmlBuilder.encode(xsdInfo) );
    }
    
    abstract protected void appendXsdData(XmlBuilder local);
    
    protected static FQName parseFQName(Element element, String defaultNS) {
      String namespace = element.getAttribute(ATTR_NAMESPACE);
      String name = element.getAttribute(ATTR_NAME);
      if( isEmpty(namespace) ) {
        return new FQName(defaultNS,name);
      } else {
        return new FQName(namespace,name);
      }
    }
    
    protected static boolean isEmpty(String string) {
      return string == null || string.length() == 0;
    }
    
    protected boolean isNSEqual(String ns1, String ns2) {
      if( ns1 == null ) {
        return ns2 == null;
      } else {
        return ns1.equals(ns2);
      }
    }
  }
  
  public static class XSDDataModel_Type extends XSDDataModel {
    private static final String EL_ROOT_ELEMENTS ="RootElements";
    private static final String EL_ROOT ="Root";
    private TypeInfo typeInfo;

    public XSDDataModel_Type(TypeInfo typeInfo) {
      this.typeInfo = typeInfo;
    }

    @Override
    protected void appendXsdData(XmlBuilder xml) {
      String namespace = typeInfo.getName().getNamespace();
      xml.startElementWithAttributes(EL_XSD);
      xml.addAttribute(ATTR_NAMESPACE, namespace );
      xml.addAttribute(ATTR_NAME, typeInfo.getName().getName() );
      xml.endAttributes();
      
      if( typeInfo.getRootElements() != null && ! typeInfo.getRootElements().isEmpty() ) {
        xml.startElement(EL_ROOT_ELEMENTS);
        for( FQName root : typeInfo.getRootElements() ) {
          appendFQName(xml, EL_ROOT, root, namespace);   
        }
        xml.endElement(EL_ROOT_ELEMENTS);
      }
      xml.endElement(EL_XSD);
    }
    
    private void appendFQName(XmlBuilder xml, String tag, FQName name, String defaultNamespace) {
      xml.startElementWithAttributes(tag);
      String namespace = name.getNamespace();
      if( ! isNSEqual( defaultNamespace, namespace ) && namespace != null ) {
        xml.addAttribute(ATTR_NAMESPACE, namespace );
      }
      xml.addAttribute(ATTR_NAME, name.getName() );
      xml.endAttributesAndElement();
    }

    public static TypeInfo parse(String xml) throws XPRC_XmlParsingException, Ex_FileAccessException {
      Document doc = XMLUtils.parseString(xml, true);
      Element element = doc.getDocumentElement();

      FQName name = parseFQName(element, null);
      TypeInfo ti = new TypeInfo(Type.Complex, name); //TODO Complex ist meist falsch
      
      for( Element re : XMLUtils.getChildElementsRecursively(element, EL_ROOT) ) {
        ti.addRootElement(parseFQName(re, name.getNamespace()));
      }
      
      return ti;
    }
   
  }
  
  public static class XSDDataModel_Member extends XSDDataModel {
    private static final String ATTR_TYPE = "Type";
    private static final String ATTR_FORM = "Form";
    private static final String ATTR_USAGE = "Usage";
    private static final String ATTR_POSITION = "Position";
    private static final String EL_CHOICE = "Choice";
    private static final String ATTR_XMOMCLASS = "XmomClass"; //FIXME XmomType 
    private TypeInfo parent;
    private TypeInfoMember member;

    public XSDDataModel_Member(TypeInfo parent, TypeInfoMember member) {
      this.parent = parent;
      this.member = member;
      
    }

    protected void appendXsdData(XmlBuilder xml) {
      String parentNamespace = parent.getName().getNamespace();
      String namespace = member.getQualifiedNamespace();
      
      xml.startElementWithAttributes(EL_XSD);
      if( ! isNSEqual( parentNamespace, namespace) ) {
        xml.addAttribute(ATTR_NAMESPACE, member.getQualifiedNamespace() );
      }
      xml.addAttribute(ATTR_NAME, member.getName().getName() );
      
      MemberType type = member.getMemberType();
      xml.addAttribute(ATTR_TYPE, type.name() );
      xml.addAttribute(ATTR_FORM, type.form(member.isQualified()) );
      xml.addAttribute(ATTR_USAGE, type.usage(member.isOptional()) );
      
      
      xml.addAttribute(ATTR_POSITION, String.valueOf(member.getPosition()) );
      if( type == MemberType.Choice ) {
        xml.endAttributes();
        for( Pair<FQName,TypeInfo> c : member.getChoiceMember() ) {
          xml.startElementWithAttributes(EL_CHOICE);
          xml.addAttribute(ATTR_NAME, c.getFirst().getName() );
          xml.addAttribute(ATTR_XMOMCLASS, c.getSecond().getXmomType().getFQTypeName() ); //FIXME Bug 23174
          xml.endAttributesAndElement();
        }
        xml.endElement(EL_XSD);
      } else {
        xml.endAttributesAndElement();
      }
      
    }

    public static TypeInfoMemberBuilder parse(String xml, String defaultNS) throws XPRC_XmlParsingException, Ex_FileAccessException {
      Document doc = XMLUtils.parseString(xml, true);
      Element element = doc.getDocumentElement();

      FQName name = parseFQName(element, defaultNS);
      
      MemberType type = MemberType.valueOf(element.getAttribute(ATTR_TYPE));
      TypeInfoMemberBuilder builder = TypeInfoMember.create(name, type);
      
       
      builder.qualified(type.isQualified(element.getAttribute(ATTR_FORM)));
      builder.optional(type.isOptional(element.getAttribute(ATTR_USAGE)));
      builder.position( Integer.parseInt(element.getAttribute(ATTR_POSITION)) );
     
      if( type == MemberType.Choice ) {
        List<Pair<FQName, TypeInfo>> choice = new ArrayList<Pair<FQName, TypeInfo>>();
        for( Element c : XMLUtils.getChildElementsByName(element, EL_CHOICE) ) {
          FQName cn = parseFQName(c, defaultNS);
          TypeInfo cti = new TypeInfo(Type.Complex, cn);
          cti.setXmomType( XmomType.ofFQTypeName(c.getAttribute(ATTR_XMOMCLASS)));
          choice.add( Pair.of(cn, cti));
        }
        builder.choice(choice);
      }
      return builder;
    }
    
  }
  
  public TypeInfo parse(Datatype dataType) throws XPRC_XmlParsingException, Ex_FileAccessException, ClassNotFoundException {
    DataModel dm = dataType.getMeta().getDataModel();
    String xml = getXSDDataModelXML( dm); 
    
    TypeInfo ti = XSDDataModel_Type.parse( xml );
    ti.setXmomType(dataType.getType());
    
    if( dataType.getBaseType() != null ) {
      ti.setBaseType( typeInfoFor(dataType.getBaseType()) );
    }
    
    for( Variable v : dataType.getVariables() ) {
      DataModel dmv = v.getMeta().getDataModel();
      String xmlv = getXSDDataModelXML( dmv ); 
      
      TypeInfoMemberBuilder member = XSDDataModel_Member.parse( xmlv, ti.getName().getNamespace() );
      
      member.varName(v.getName()).label( v.getLabel() );
      if( v.getTypeReference() != null ) {
        member.complexType(typeInfoFor(v.getTypeReference()));
      } else {
        member.simpleType(v.getMeta().getType());
      }
      ti.addMemberDontChangePosition(member.build());
    }
    return ti;
  }

  private TypeInfo typeInfoFor(XmomType xmomType) {
    TypeInfo ct = new TypeInfo(TypeInfo.Type.Complex, null);  //FIXME
    ct.setXmomType(xmomType);
    return ct;
  }

  private String getXSDDataModelXML(DataModel dm) {
    if( dm.getDataModelSpecifics() instanceof XSDDataModel ) {
      XSDDataModel xsdDM = (XSDDataModel)dm.getDataModelSpecifics();
      XmlBuilder xml = new XmlBuilder();
      xsdDM.appendXsdData(xml);
      return xml.toString();
    } else {
      return null;
    }
  }


  private TypeInfo parse(DOM dom) throws XPRC_XmlParsingException, Ex_FileAccessException {
    DataModelInformation dataModInfo = dom.getDataModelInformation();
    if (dataModInfo == null) {
      if (!dom.exists()) {
        RuntimeContext runtimeContext;
        try {
          runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(dom.getRevision());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException("Invalid revision found while parsing " + dom.getFqClassName() + ": " + dom.getRevision());
        }
        throw new RuntimeException("Type " + dom.getOriginalFqName() + " not found in " + runtimeContext.getGUIRepresentation());
      } else {
        throw new RuntimeException("Type " + dom.getOriginalFqName() + " does not contain Datamodel Meta information."
          + " Please make sure it has been generated using Xyna Factory Manager.");
      }
    }
    String xml = dataModInfo.get("XSDInfo");
    TypeInfo ti = XSDDataModel_Type.parse( xml );
    ti.setXmomType( XmomType.ofFQTypeName(dom.getFqClassName() ) );
    
    if( dom.getSuperClassGenerationObject() != null ) {
      XmomType baseType = XmomType.ofFQTypeName( dom.getSuperClassGenerationObject().getFqClassName());
      ti.setBaseType( typeInfoFor(baseType) );
    }
    
    for( AVariable v : dom.getMemberVars() ) {
      DataModelInformation dmi = v.getDataModelInformation();
      if (dmi == null) {
        throw new RuntimeException("Type " + dom.getFqClassName() + " does not contain Datamodel Meta information for member variable " + v.getVarName() + "."
            + " Please make sure it has been generated using Xyna Factory Manager.");
      }
      String xmlv = dmi.get("XSDInfo");
      TypeInfoMemberBuilder member = XSDDataModel_Member.parse( xmlv, ti.getName().getNamespace() );
      
      member.varName(v.getVarName()).label( v.getLabel() );
      if (logger.isDebugEnabled()) {
        if( v.getJavaTypeEnum() != null ) {
          logger.debug("parsing variable " + v.getVarName() + " of type " + v.getJavaTypeEnum() );
        } else {
          logger.debug("parsing variable " + v.getVarName() + " of type " + v.getFQClassName() );
        }
      }
      if( v.getJavaTypeEnum() != null ) {
        member.simpleType( v.getJavaTypeEnum() );
      } else if( v.getFQClassName() != null ) {
        member.complexType(typeInfoFor( XmomType.ofFQTypeName(v.getFQClassName()) ) );
      } else {
        member.simpleType( PrimitiveType.STRING );
      }
      member.list(v.isList());
      ti.addMemberDontChangePosition(member.build());
    }
    return ti;
  }
}
