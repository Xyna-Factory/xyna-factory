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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaGenUtils;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry_1_1;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionXmlInvalidBaseReferenceException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class ExceptionVariable extends AVariable {

  private ExceptionGeneration exceptionGeneration;

  private boolean hasInitializied = false;
  private String unknownType;
  
  public ExceptionVariable(GenerationBase creator, Long revision) {
    super(creator, revision);
  }
  

  public ExceptionVariable(GenerationBase creator) {
    super(creator);
  }
  
  public ExceptionVariable(GenerationBase creator, String fqClassName) {
    super(creator);
    setFQClassName(fqClassName);
  }
  
  public ExceptionVariable(ExceptionVariable original) {
    super(original);
    this.exceptionGeneration = original.exceptionGeneration;
    this.hasInitializied = original.hasInitializied;
    this.unknownType = original.unknownType;
  }


  public ExceptionVariable(ExceptionEntry_1_1 exceptionEntry, GenerationBase creator, Long revision)
      throws XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidExceptionXmlInvalidBaseReferenceException, XPRC_InvalidPackageNameException {

    super(creator);
    String basePath = exceptionEntry.getBaseExceptionPath();
    String baseName = exceptionEntry.getBaseExceptionName();
    boolean baseReferenceNameIsEmpty = baseName == null || baseName.trim().length() == 0;
    boolean baseReferencePathIsEmpty = basePath == null || basePath.trim().length() == 0;
    if (baseReferenceNameIsEmpty || baseReferencePathIsEmpty) {
      if (!(baseReferenceNameIsEmpty && baseReferencePathIsEmpty)) {
        throw new XPRC_InvalidExceptionXmlInvalidBaseReferenceException(basePath + "." + baseName);
      }
    }

    init(exceptionEntry.getPath(), exceptionEntry.getName());
  }


  public void init(String path, String name) throws XPRC_InvalidPackageNameException {

    if (hasInitializied) {
      return;
    }
    originalClassName = name;
    originalPath = path;

    long rev = revision;
    if (containedRuntimeContextInformation != null) {
      try {
        rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(containedRuntimeContextInformation);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      }
    }
    
    exceptionGeneration = creator.getCachedExceptionInstanceOrCreate(path + "." + name, rev);
    domOrException = exceptionGeneration;
    setFQClassName(exceptionGeneration.getFqClassName());
    String cn = GenerationBase.getSimpleNameFromFQName(getFQClassName());
    setClassName(cn);

    //stimmt zufällig: die nicht-mdm-, sondern basis-exception-typen sind hiermit gemeint: xynaexception, xynaexceptionbase, exception
    isJavaBaseType = GenerationBase.isReservedServerObjectByFqClassName(getFQClassName());
    if (isJavaBaseType) {
      javaType = PrimitiveType.createOrNull(cn);
      if (javaType == null) {
        // in der Fabrik implementiert aber keine der basis-exception-typen
        isJavaBaseType = false;
      }
    }

    hasInitializied = true;

  }


  @Override
  public void specialParseXml(Element e, Long revision) throws XPRC_InvalidPackageNameException {
    init(e.getAttribute(GenerationBase.ATT.REFERENCEPATH), e.getAttribute(GenerationBase.ATT.REFERENCENAME));
  }


  protected ExceptionGeneration getExceptionGeneration() {
    return exceptionGeneration;
  }
  
  protected void setExceptionGeneration(ExceptionGeneration exception) {
    this.exceptionGeneration = exception;
  }

  
  @Override
  public void generateJavaXml(CodeBuffer cb, boolean usingCache) {
    String cacheString;
    if (isList()) {
      if (usingCache) {
        cacheString =
            ", version, " + RevisionManagement.class.getSimpleName() + ".getRevisionByClass(" + getFQClassName() + ".class), cache";
      } else {
        cacheString = ", -1, 0, null";
      }
    } else {
      if (usingCache) {
        cacheString = ", version, cache";
      } else {
        cacheString = ", -1, null";
      }
    }

    String getter = JavaGenUtils.getGetterFor(getVarName());
    if (usingCache) {
      getter = "versionedG" + getter.substring(1) + "(version)";
    } else {
      getter += "()";
    }
    if (isList()) {
      cb.addLine(XMLHelper.class.getSimpleName(), ".appendExceptionList(xml, \"", getVarName(), "\", \"", originalClassName, "\", \"",
                 originalPath, "\", ", getter, cacheString, ")");
    } else {
      cb.addLine(XMLHelper.class.getSimpleName(), ".appendException(xml, \"", getVarName(), "\", ", getter, cacheString, ")");
    }
  }


  @Override
  protected void generateConstructor(CodeBuffer cb, Set<String> importedClassNames, boolean ignoreList) {

    if (isJavaBaseType()) {
      if (getFQClassName().equals(XynaException.class.getName())) {
        cb.add("new ", getClassName(importedClassNames), "(\"TODO insert code here\")"); // TODO
      } else if (getFQClassName().equals(Exception.class.getName())) {
        cb.add("new ", getClassName(importedClassNames), "()"); // TODO
      } else {
        throw new RuntimeException("exception java base type " + getFQClassName() + " is not supported");
      }
    } else {
      if (isList() && !ignoreList) {
        cb.add("new ", GeneralXynaObjectList.class.getSimpleName(), "(",
               getEventuallyQualifiedClassNameNoGenerics(importedClassNames), ".class");
        for (AVariable var : children) {
          cb.add(", ");
          var.generateConstructor(cb, importedClassNames, ignoreList);
        }
        cb.add(")");
      } else {
        if (!hasValueOrIsDOM()) {
          cb.add("null");
        } else {
          cb.add("new ", getInstantiableClassName(importedClassNames, ignoreList), ".Builder().");
          for (AVariable var : children) { //reihenfolge egal (im gegenteil zu konstruktoren)
            if (var.hasValueOrIsDOM()){
              cb.add(var.getVarName(), "(");
              var.generateConstructor(cb, importedClassNames, false);
              cb.add(").");
            }
          }
          cb.add("instance()");
        }
      }
    }

  }
  
  @Override
  protected void getImports(HashSet<String> imports) {
    if (!imports.contains(GeneralXynaObjectList.class.getName())) {
      imports.add(GeneralXynaObjectList.class.getName());
    }
    
    super.getImports(imports);
  }


  @Override
  public void validate() throws XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
    super.validate();
    boolean referencePathIsEmpty = originalPath == null || originalPath.trim().length() == 0;
    boolean referenceNameIsEmpty = originalClassName == null || originalClassName.trim().length() == 0;
    if (referencePathIsEmpty || referenceNameIsEmpty) {
      throw new XPRC_InvalidExceptionVariableXmlMissingTypeNameException(originalPath + "." + originalClassName);
    }
    if (unknownType != null) {
      throw new RuntimeException("Unsupported type: " + unknownType);
    }
  }

  @Override
  public void appendXML(XmlBuilder xml, boolean includeReferences) {
    xml.startElementWithAttributes(EL.EXCEPTION); {
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
      if (children != null) {
        for (AVariable child : children) {
          child.appendXML(xml, false);
        }
      }
    } xml.endElement(EL.EXCEPTION); // TODO: <Source> and <Target>, like in DatatypeVariable?
  }

}
