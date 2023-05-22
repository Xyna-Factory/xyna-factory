/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.Arrays;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaGenUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.DefaultTypeRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.MaxLengthRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.RestrictionType;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restrictions;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

public class DatatypeVariable extends AVariable {
  
  private Restrictions restrictions;

  public DatatypeVariable(GenerationBase creator, Long revision) {
    super(creator, revision);
  }
  
  public DatatypeVariable(GenerationBase creator) {
    super(creator);
  }
  
  public DatatypeVariable(Long revision) {
    super(null, revision);
  }
  
  public DatatypeVariable(DatatypeVariable original) {
    super(original);
    this.restrictions = original.restrictions;
  }

  public DatatypeVariable(GenerationBase creator, AVariable original) {
    super(creator, original);
  }

  public static final String ANY_TYPE = "base.AnyType";

  protected void specialParseXml(Element e, Long revision) throws XPRC_InvalidPackageNameException {

    parseUnknownMetaTags(e, Arrays.asList(EL.PERSISTENCE, EL.METATYPE, EL.USEROUTPUT));

    // parse data
    boolean isRef = !GenerationBase.isEmpty(e.getAttribute(GenerationBase.ATT.REFERENCENAME));
    if (isRef) {
      createReference( e.getAttribute(GenerationBase.ATT.REFERENCEPATH), 
                       e.getAttribute(GenerationBase.ATT.REFERENCENAME) );
    } else {
      Element meta = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
      if (meta != null) {
        Element metaTypeElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.METATYPE);
        if (metaTypeElement != null) {
          String typeValue = XMLUtils.getTextContent(metaTypeElement);
          javaType = PrimitiveType.createOrNull(typeValue);
          if (javaType == null) {
            unsupportedTypeName = typeValue;
          } else {
            isJavaBaseType = true;
          }
        }
      } else if (getParentVariableInXml() != null) {
        isJavaBaseType = true;
        //wird bei fillvariablecontents noch weiter gef�llt
      } else {
        // any type
        setFQClassName(XynaObject.class.getName());
        setClassName(XynaObject.class.getSimpleName());
      }
    }
    Element restriction = XMLUtils.getChildElementByName(e, GenerationBase.EL.RESTRICTION);
    if (restriction != null) {
      this.restrictions = new Restrictions();
      this.restrictions.parseXml(restriction);
      DefaultTypeRestriction dtr = this.restrictions.<DefaultTypeRestriction>getRestriction(RestrictionType.DEFAULT_TYPE, null);
      if (dtr != null &&
          dtr.getDefaultType() != null) {
        if (creator != null) {
          this.defaultTypeRestriction = creator.getCachedDOMInstanceOrCreate(dtr.getDefaultType(), revision);
        } else {
          this.defaultTypeRestriction = DOM.getInstance(dtr.getDefaultType(), revision);
        }
      }
    }
  }


  private DOM createReference(String originalPath, String originalClassName) throws XPRC_InvalidPackageNameException {
    this.originalClassName = originalClassName;
    this.originalPath = originalPath;
    String originalFqName = originalPath + "." + originalClassName;
    if (originalFqName.equals(ANY_TYPE)) {
      javaType = PrimitiveType.ANYTYPE;
      isJavaBaseType = true;
      return null;
    } else {
      DOM d = null;
      
      if (containedRuntimeContextInformation != null) {
        try {
          revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(containedRuntimeContextInformation);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        }
      }
      
      if (creator != null) {
        d = creator.getCachedDOMInstanceOrCreate(originalFqName, revision);
      } else {
        d = DOM.getInstance(originalFqName, revision);
      }
      setFQClassName(d.getFqClassName());
      setClassName(d.getSimpleClassName());
      setGenerationBaseObject(d);
      return d;
    }
  }

  private boolean varNameMustExist() {
    // fallunterscheidung:
    //1. membervariable in dom => ja
    //2. variable in wf => ja
    //  konstantenbelegung in wfs:
    //3. verschachtelte membervariable in wf => ja
    //4. bei listen gibt es ein kapselndes data-element f�r jedes listenelement => nein
    //
    if (getParentVariableInXml() == null || getParentVariableInXml().isList()) {
      return false;
    }
    return true;
  }


  protected void generateConstructor(CodeBuffer cb, Set<String> importedClassNames, boolean ignoreList) {
    try {
      if (isList() && !ignoreList) {
        if (isJavaBaseType) {
          cb.add("new ", getInstantiableClassName(importedClassNames), "(");
          cb.add("Arrays.asList(new ", getJavaTypeAsObject(), "[] {");
          if (values != null) {
            for (int i = 0; i < values.length; i++) {
              if (i > 0) {
                cb.add(", ");
              }
              cb.add(getJavaTypeEnum().toLiteral(values[i]));
            }
          }
          cb.add("})");
          cb.add(")");
        } else {
          cb.add("new ", XynaObjectList.class.getSimpleName(), "(",
                 getEventuallyQualifiedClassNameNoGenerics(importedClassNames), ".class");
          for (AVariable c : children) {
            cb.add(", ");
            c.generateConstructor(cb, importedClassNames);
          }
          cb.add(")");
        }
      } else if (isJavaBaseType) {
        cb.add(javaType.toLiteral(value));
      } else {
        if (hasValueOrIsDOM()) {
          cb.add("new ", getInstantiableClassName(importedClassNames, ignoreList), ".Builder().");
          for (AVariable var : children) { //reihenfolge egal (im gegenteil zu konstruktoren)
            if (var.hasValueOrIsDOM()) {
              cb.add(var.getVarName(), "(");
              var.generateConstructor(cb, importedClassNames, false);
              cb.add(").");
            }
          }
          cb.add("instance()");
        } else {
          cb.add("null");
        }
      }
    } catch (RuntimeException e) {
      throw new RuntimeException("Could not create constructor for variable " + getOriginalPath() + "."
          + getOriginalName() + ": " + e.getMessage(), e);
    }
  }


  protected void setOriginalClassName(String s) {
    this.originalClassName = s;
  }


  protected void setOriginalPath(String s) {
    this.originalPath = s;
  }

  
  /**
   * erzeugt code, der &lt;data&gt;&lt;/data&gt; element f�r diese variable erzeugt
   */
  public void generateJavaXml(CodeBuffer cb, boolean usingCache) {
    String cacheString;
    if (usingCache) {
      if (isList() && (!isJavaBaseType || getJavaTypeEnum() == PrimitiveType.ANYTYPE)) {
        //FIXME das ist ziemlich dreckig 
        String cn = isJavaBaseType ? "xprc.xpce.AnyInputPayload" : getFQClassName();
        cacheString =
            ", version, " + RevisionManagement.class.getSimpleName() + ".getRevisionByClass(" + cn + ".class), cache";
      } else {
        cacheString = ", version, cache";
      }
    } else {
      cacheString = "";
    }
    String getter = JavaGenUtils.getGetterFor(getVarName());
    if (usingCache) {
      getter = "versionedG" + getter.substring(1) + "(version)";
    } else {
      getter += "()";
    }
    // falls komplexer typ => .toXML von dem.
    // falls javatype => <data><value>...
    // falls liste => <data islist=true> um obiges drumrum
    if (isList()) {
      if (isJavaBaseType) {
        if (getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          cb.addLine(XMLHelper.class.getSimpleName(), ".appendDataList(xml, ", "\"", getVarName(), "\", ", "\"AnyType\", ", "\"base\", ",
                     getter, cacheString, ")");
        } else {
        //braucht den cache nicht �bergeben, weil hier nichts referenziert wird, und keine rekursion stattfinden kann
        cb.addLine(XMLHelper.class.getSimpleName(), ".appendDataList(xml, ",
                   "\"", getVarName(), "\", ", getter, ", ",
                   javaType.getClassOfType(),".class)");
        }
      } else {
        cb.addLine(XMLHelper.class.getSimpleName(), ".appendDataList(xml, ",
                   "\"", getVarName(), "\", ",
                   "\"",originalClassName,"\", ",
                   "\"",originalPath,"\", ",
                   getter, cacheString, ")");
      }
    } else {
      cb.addLine(XMLHelper.class.getSimpleName(), ".appendData(xml, \"", getVarName(), "\", ", getter, cacheString, ")");
    }
  }

  
  public String toString() {
    if (getFQClassName() != null) {
      return super.toString() + " - " + getFQClassName();
    } else if (getParentVariableInXml() != null) {
      return super.toString() + " - " + getVarName() + " parent=" + getParentVariableInXml().getFQClassName();
    } else {
      return super.toString();
    }
  }


  @Override
  public void validate() throws XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
    super.validate();

    if (varNameMustExist() && (getVarName() == null || getVarName().length() == 0)) {
      throw new XPRC_MISSING_ATTRIBUTE(GenerationBase.EL.DATA, getFQClassName(), GenerationBase.ATT.VARIABLENAME);
    }
  }
  
  
  public int getMaxLength() {
    if (restrictions == null) {
      return -1;
    } else {
      MaxLengthRestriction maxLength = restrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH, null);
      if (maxLength != null) {
        return maxLength.getLimit();
      } else {
        return -1;
      }
    }
  }
  
  public Restrictions getRestrictions() {
    return restrictions;
  }

  public void create(String fqName) throws XPRC_InvalidPackageNameException {
    Pair<String,String> name = splitFqName(fqName);
    DOM dom = createReference( name.getFirst(), name.getSecond() );
    setVarName(createVarName());
    if( dom == null ) {
      this.label = name.getSecond();
    } else {
      this.label = dom.getLabel();
      this.revision = dom.getRevision();
    }
  }

  private Pair<String, String> splitFqName(String fqName) {
    int idx = fqName.lastIndexOf('.');
    return Pair.of( fqName.substring(0, idx), fqName.substring(idx+1) );
  }

  public void create(PrimitiveType type) {
    this.isJavaBaseType = true;
    this.javaType = type;
  }

  public void create(String path, String name) throws XPRC_InvalidPackageNameException {
    DOM dom = createReference( path, name );
    setVarName(createVarName());
    if( dom == null ) {
      this.label = name;
    } else {
      this.label = dom.getLabel();
    }
  }

  /*
   *
    <Data ID="28" Label="SomeType" ReferenceName="SomeType" ReferencePath="cl.modeller" VariableName="const_SomeType">
      <Target RefID="16"/>
      <Data Label="Double Number" ReferenceName="DoubleNumber" ReferencePath="base.math" VariableName="doubleNumber">
        <Data Label="value" VariableName="value">
          <Meta>
            <Type>double</Type>
          </Meta>
          <Value>6</Value>
        </Data>
      </Data>
      <Data Label="intvar" VariableName="intvar">
        <Meta>
          <Type>int</Type>
        </Meta>
        <Value>22</Value>
      </Data>
      <Data Label="stringvar" VariableName="stringvar">
        <Meta>
          <Type>String</Type>
        </Meta>
        <Value>ff</Value>
      </Data>
    </Data>
   */
  @Override
  public void appendXML(XmlBuilder xml, boolean includeReferences) { // TODO: Fall !isRef und Restrictions
    xml.startElementWithAttributes(EL.DATA); {
      appendAttributes(xml);
      xml.endAttributes();

      if (hasMeta()) {
        xml.startElement(EL.META); {
          appendMeta(xml);
        } xml.endElement(EL.META);
      }

      if (includeReferences) {
        // <Source>
        for (String sourceId :getSourceIds()) {
          xml.startElementWithAttributes(EL.SOURCE); {
            xml.addAttribute(ATT.REFID, sourceId);
          } xml.endAttributesAndElement();
        }

        // <Target>
        if (getTargetId() != null) {
          xml.startElementWithAttributes(EL.TARGET); {
            xml.addAttribute(ATT.REFID, getTargetId());
          } xml.endAttributesAndElement();
        }
      }

      appendChildren(xml);
    } xml.endElement(EL.DATA);
  }

  @Override
  protected boolean hasMeta() {
    if ( (super.hasMeta()) || (hasUnknownMetaTags()) ) {
      return true;
    }

    return ( (isJavaBaseType) || (unsupportedTypeName != null) );
  }

  @Override
  protected void appendMeta(XmlBuilder xml) {
    super.appendMeta(xml);

    if (isJavaBaseType && javaType != PrimitiveType.ANYTYPE) {
       xml.element(EL.METATYPE, javaType.getClassOfType());
    }
  }

}
