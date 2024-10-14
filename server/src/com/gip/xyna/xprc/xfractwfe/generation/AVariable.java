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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaGenUtils;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_ExceptionClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Utils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public abstract class AVariable implements XmlAppendable, HasDocumentation, HasMetaTags {

  //TODO xynaexceptino
  public static class UnsupportedJavaTypeException extends Exception {

    private static final long serialVersionUID = 1L;

  }
  
  public static enum PrimitiveType {
    STRING(String.class.getSimpleName(), String.class.getSimpleName(), String.class.getName(), true, "new String()") {

      @Override
      public Object fromString(String v) {
        return v;
      }


      @Override
      public String createFromLong(String varName) {
        return "String.valueOf(" + varName + ")";
      }
      
      @Override
      public String toLiteral(String value) {
        if( value == null ) {
          return "null";
        }
        return "\"" +StringUtils.toLiteral(value) +"\"";
      }

      @Override
      public String getJavaTypeName() {
        return "String";
      }
    },
    BOOLEAN(Boolean.class.getSimpleName(), boolean.class.getSimpleName(), boolean.class.getName(), false, "false") {

      @Override
      public Object fromString(String v) {
        return Boolean.parseBoolean(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }
      
      @Override
      public String toLiteral(String value) {
        if (isEmpty(value)) {
          return getDefaultConstructor();
        }
        if( "true".equals(value) ) {
          return "true";
        }
        if( "false".equals(value) ) {
          return "false";
        }
        throw new IllegalArgumentException("\""+value+"\" is no legal boolean");
      }

      @Override
      public String getJavaTypeName() {
        return "boolean";
      }
    },
    BOOLEAN_OBJ(Boolean.class.getSimpleName(), Boolean.class.getSimpleName(), Boolean.class.getName(), true, "false") {

      @Override
      public Object fromString(String v) {
        return Boolean.parseBoolean(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }
      
      @Override
      public String toLiteral(String value) {
        if( isEmpty(value) ) {
          return "null";
        }
        if( "true".equals(value) ) {
          return "Boolean.TRUE";
        }
        if( "false".equals(value) ) {
          return "Boolean.FALSE";
        }
        throw new IllegalArgumentException("\""+value+"\" is no legal Boolean");
      }

      @Override
      public String getJavaTypeName() {
        return "Boolean";
      }
    },
    INT(Integer.class.getSimpleName(), int.class.getSimpleName(), int.class.getName(), false, "0") {

      @Override
      public Object fromString(String v) {
        return Integer.parseInt(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return "(int) " + varName;
      }

      @Override
      public String getJavaTypeName() {
        return "int";
      }
    },
    INTEGER(Integer.class.getSimpleName(), Integer.class.getSimpleName(), Integer.class.getName(), true, "0") {

      @Override
      public Object fromString(String v) {
        return Integer.parseInt(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return "(int) " + varName;
      }

      @Override
      public String getJavaTypeName() {
        return "Integer";
      }
    },
    LONG(Long.class.getSimpleName(), long.class.getSimpleName(), long.class.getName(), false, "0L") {

      @Override
      public Object fromString(String v) {
        return Long.parseLong(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return varName;
      }
      
      @Override
      public String toLiteral(String value) {
        if( isEmpty(value) ) {
          return getDefaultConstructor();
        }
        return value+"L";
      }

      @Override
      public String getJavaTypeName() {
        return "long";
      }
    },
    LONG_OBJ(Long.class.getSimpleName(), Long.class.getSimpleName(), Long.class.getName(), true, "0L") {

      @Override
      public Object fromString(String v) {
        return Long.parseLong(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return varName;
      }
      
      @Override
      public String toLiteral(String value) {
        if( isEmpty(value) ) {
          return "null";
        }
        return value+"L";
      }

      @Override
      public String getJavaTypeName() {
        return "Long";
      }
    },
    DOUBLE(Double.class.getSimpleName(), double.class.getSimpleName(), double.class.getName(), false, "0d") {

      @Override
      public Object fromString(String v) {
        return Double.parseDouble(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return "(double) " + varName;
      }
      
      @Override
      public String toLiteral(String value) {
        if( isEmpty(value) ) {
          return getDefaultConstructor();
        }
        return value+"d";
      }

      @Override
      public String getJavaTypeName() {
        return "double";
      }
    },
    DOUBLE_OBJ(Double.class.getSimpleName(), Double.class.getSimpleName(), Double.class.getName(), true, "0d") {

      @Override
      public Object fromString(String v) {
        return Double.parseDouble(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        return "(double) " + varName;
      }
      
      @Override
      public String toLiteral(String value) {
        if( isEmpty(value) ) {
          return "null";
        }
        return value+"d";
      }

      @Override
      public String getJavaTypeName() {
        return "Double";
      }
    },
    BYTE(Byte.class.getSimpleName(), byte.class.getSimpleName(), byte.class.getName(), false, "0") {

      @Override
      public Object fromString(String v) {
        return Byte.parseByte(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }


      @Override
      public String getJavaTypeName() {
        return "byte";
      }
    },
    BYTE_OBJ(Byte.class.getSimpleName(), Byte.class.getSimpleName(), Byte.class.getName(), true, "0") {

      @Override
      public Object fromString(String v) {
        return Byte.parseByte(v);
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }


      @Override
      public String getJavaTypeName() {
        return "Byte";
      }
    },
    VOID(Void.class.getSimpleName(), void.class.getSimpleName(), Void.class.getName(), false, "") {

      @Override
      public Object fromString(String v) {
        return null;
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }


      @Override
      public String getJavaTypeName() {
        return "void";
      }
    },
    CONTAINER(Container.class.getSimpleName(), Container.class.getSimpleName(), Container.class.getName(), true, "new "
        + Container.class.getSimpleName()) {

      @Override
      public Object fromString(String v) {
        return null;
      }


      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }


      @Override
      public String getJavaTypeName() {
        return "Container";
      }
    },
    EXCEPTION(Exception.class.getSimpleName(), Exception.class.getSimpleName(), Exception.class.getName(), true, "new " + Exception.class.getSimpleName()) {

      @Override
      public Object fromString(String v) {
        throw new RuntimeException();
      }

      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }

      @Override
      public String getJavaTypeName() {
        return "Exception";
      }
    },
    XYNAEXCEPTION(XynaException.class.getSimpleName(), XynaException.class.getSimpleName(), XynaException.class.getName(), true, "new " + XynaException.class.getSimpleName()) {

      @Override
      public Object fromString(String v) {
        throw new RuntimeException();

      }

      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }

      @Override
      public String getJavaTypeName() {
        return "XynaException";
      }
    },
    //konstruktor nicht, weil abstrakt
    XYNAEXCEPTIONBASE(XynaExceptionBase.class.getSimpleName(), XynaExceptionBase.class.getSimpleName(), XynaExceptionBase.class.getName(), true, "null") {

      @Override
      public Object fromString(String v) {
        throw new RuntimeException();

      }

      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }

      @Override
      public String getJavaTypeName() {
        return "XynaExceptionBase";
      }
    },
    ANYTYPE(GeneralXynaObject.class.getSimpleName(), GeneralXynaObject.class.getSimpleName(), GeneralXynaObject.class.getName(), true, "null") {

      @Override
      public Object fromString(String v) {
        throw new RuntimeException();
      }

      @Override
      public String createFromLong(String varName) throws UnsupportedJavaTypeException {
        throw new UnsupportedJavaTypeException();
      }

      @Override
      public String getJavaTypeName() {
        return "AnyType";
      }
    }
    ;

    private final String objectClass;
    private final String clazz;
    private final String fqName;
    private final boolean isObject;
    private final String defaultConstructor;
    private final static Map<String, PrimitiveType> classNameMap = new HashMap<String, PrimitiveType>();
    static {
      for (PrimitiveType p : PrimitiveType.values()) {
        classNameMap.put(p.clazz, p);
      }
    }

    private PrimitiveType(String objectClass, String clazz, String fqName, boolean isObject, String defaultConstructor) {
      this.objectClass = objectClass;
      this.clazz = clazz;
      this.fqName = fqName;
      this.isObject = isObject;
      this.defaultConstructor = defaultConstructor;
    }

    public String getObjectClassOfType() {
      return objectClass;
    }

    /**
     * simpleclassname
     */
    public String getClassOfType() {
      return clazz;
    }


    public boolean isObject() {
      return isObject;
    }


    public String getDefaultConstructor() {
      return defaultConstructor;
    }

    
    public String getFqName() {
      return fqName;
    }

    /**
     * erstellt eine objektinstanz vom typ des enums aus der stringdarstellung des objekts
     * (also gegen-operation zu tostring()
     * z.b. this==Integer, v = "3" -&gt; return 3;
     */
    public abstract Object fromString(String v);


    /**
     * erstellt ein codesnippet, welches den long mit dem übergebenen namen nach dem entsprechenden typ castet.
     * z.b. falls this==String -&gt; return "String.valueOf(&lt;varName&gt;)" 
     */
    public abstract String createFromLong(String varName) throws UnsupportedJavaTypeException;


    public abstract String getJavaTypeName();


    /**
     * Erzeugt einen validen Literal zu dem übergebenen Wert.
     * z.B für 123 und Type long: 123L,
     *     für \a  und Type String: "\\a"
     * @param value
     * @return
     */
    public String toLiteral(String value) {
      if ( isEmpty(value) ) {
        if (isObject()) {
          return "null";
        } else {
          return getDefaultConstructor();
        }
      }
      return value; 
    }
    
    private static boolean isEmpty(String value) {
      return value == null || value.length() == 0;
    }

    /**
     * @throws UnsupportedJavaTypeException falls kein primitivetype mit diesem klassennamen existiert
     */
    public static PrimitiveType create(String className) throws UnsupportedJavaTypeException {
      PrimitiveType p = classNameMap.get(className);
      if (p == null) {
        throw new UnsupportedJavaTypeException();
      }
      return p;
    }
    
    public static PrimitiveType createOrNull(String className) {
      return classNameMap.get(className);
    }
    
    
    public static PrimitiveType boxType(PrimitiveType type) {
      switch (type) {
        case BOOLEAN :
          return BOOLEAN_OBJ;
        case BYTE :
          return BYTE_OBJ;
        case DOUBLE :
          return DOUBLE_OBJ;
        case INT :
          return INTEGER;
        case LONG :
          return LONG_OBJ;
        default :
          return type;
      }
    }
  }


  private static Logger logger = CentralFactoryLogging.getLogger(AVariable.class);

  private String id;
  private String instanceId;
  private String refInstanceId;
  protected String varName;
  protected String label;
  protected String documentation;
  protected boolean isList = false;
  // if this data element is child of another one
  private AVariable parentVariableInXml;

  private String className;
  protected String fqClassName;
  
  protected String unsupportedTypeName;
  protected boolean isJavaBaseType = false;
  protected PrimitiveType javaType;

  // original namen
  protected String originalClassName;
  protected String originalPath;

  protected DomOrExceptionGenerationBase domOrException;

  // if value-tag is child of this data-tag
  protected String value;
  protected String[] values; // falls liste, kann es mehrere values geben
  protected ArrayList<AVariable> children;

  private boolean hasConstantValue = false;

  protected final GenerationBase creator;
  
  protected Long revision;
  
  protected Set<PersistenceTypeInformation> persistenceTypes;
  private DataModelInformation dataModelInformation;
  
  private boolean missingDataElement = false;
  
  private boolean prototype = false;
 
  protected DomOrExceptionGenerationBase defaultTypeRestriction;
  
  protected RuntimeContext containedRuntimeContextInformation;
  
  private boolean isUserOutput = false;

  private UnknownMetaTagsComponent unknownMetaTagsComponent = new UnknownMetaTagsComponent();
  
  private final Set<String> sourceIds = new HashSet<>();
  private String targetId = null;
  
  private boolean isFunctionResult;
  
  protected AVariable(GenerationBase creator) {
    this.creator = creator;
    this.revision = creator.getRevision();
  }
  
  protected AVariable(GenerationBase creator, Long revision) {
    this.creator = creator;
    this.revision = revision;
  }
  
  protected AVariable(AVariable original) {
    this( original.creator, original);
  }
  
  protected AVariable(GenerationBase creator, AVariable original) {
    this.creator = creator;
    this.varName = original.varName;
    this.label = original.label;
    this.documentation = original.documentation;
    this.isList = original.isList;
    this.parentVariableInXml = original.parentVariableInXml;
    this.className = original.className;
    this.fqClassName = original.fqClassName;
    this.unsupportedTypeName = original.unsupportedTypeName;
    this.isJavaBaseType = original.isJavaBaseType;
    this.javaType = original.javaType;
    this.originalClassName = original.originalClassName;
    this.originalPath = original.originalPath;
    this.domOrException = original.domOrException;
    this.value = original.value;
    this.values = original.values;
    this.hasConstantValue = original.hasConstantValue;
    this.children = original.children;
    this.revision = original.revision;
    this.persistenceTypes = original.persistenceTypes;
    this.dataModelInformation = original.dataModelInformation;
    this.missingDataElement = original.missingDataElement;
    this.prototype = original.prototype;
    this.containedRuntimeContextInformation = original.containedRuntimeContextInformation;
    
    setId(original.id);
  }
  
  public static AVariable createAVariable(String id, DomOrExceptionGenerationBase gb, boolean isList) {
    AVariable result = null;
    
    if (gb instanceof DOM) {
      result = new ServiceVariable(new DatatypeVariable(gb, gb.getRevision()));
    } else if (gb instanceof ExceptionGeneration) {
      result = new ExceptionVariable(gb, gb.getRevision());
    } else {
      throw new RuntimeException();
    }

    //AnyType
    if(gb != null)
      result.replaceDomOrException(gb, gb.getOriginalSimpleName());

    result.setIsList(isList);
    result.setId(String.valueOf(id));
    
    return result;
  }
  
  public static AVariable createAnyType(GenerationBase creator, boolean isList) {
      ServiceVariable sv = new ServiceVariable(new DatatypeVariable(creator));
      sv.setLabel("AnyType");
      sv.setOriginalClassName("AnyType");
      sv.setOriginalPath("base");
      sv.setId(String.valueOf(creator.getNextXmlId()));
      sv.isList = isList;
      sv.isJavaBaseType = true;
      sv.javaType = PrimitiveType.ANYTYPE;
      return sv;
  }

  public DomOrExceptionGenerationBase getDomOrExceptionObject() {
    return domOrException;
  }


  public void setGenerationBaseObject(DomOrExceptionGenerationBase domOrException) {
    this.domOrException = domOrException;
  }


  public void parseXML(Element e) throws XPRC_InvalidPackageNameException {
    parseXML(e, null, false);
  }

  public void parseXML(Element e, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    parseXML(e, null, includeNullEntries);
  }

  protected void parseXML(Element e, AVariable parent) throws XPRC_InvalidPackageNameException {
    parseXML(e, parent, false);
  }

  protected void parseXML(Element e, AVariable parent, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    genericParseXml(e, parent);
    if (!prototype) {
      specialParseXml(e, revision);
      parseChildren(e, includeNullEntries);
    }
  }
  

  protected void parseChildren(Element e) throws XPRC_InvalidPackageNameException {
    parseChildren(e, false);
  }

  protected void parseChildren(Element e, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    List<Element> datatypeChildElements = XMLUtils.getChildElementsByName(e, GenerationBase.EL.DATA);
    List<Element> exceptionChildElements = XMLUtils.getChildElementsByName(e, GenerationBase.EL.EXCEPTION);

    //FIXME zwischen null und leerstring unterscheiden. dafür benötigt man ein zusätzliches attribut
    //am besten wäre es, leerstring auszuzeichnen, weil ansonsten für zahlenwertige values immer eine spezialbehandlung notwendig ist. null ist da bereits behandelt
    if (datatypeChildElements.isEmpty() && exceptionChildElements.isEmpty()) {
      if (isList()) {
        //listenwertige variable
        List<Element> listelements = XMLUtils.getChildElementsByName(e, GenerationBase.EL.VALUE);
        if (!listelements.isEmpty()) {
          hasConstantValue = true;
        }

        if (isJavaBaseType && javaType != PrimitiveType.ANYTYPE) {
          values = new String[listelements.size()];
          for (int i = 0; i<listelements.size(); i++) {
            values[i] = XMLUtils.getTextContentOrNull(listelements.get(i));
          }
        } else {
          //liste komplexer typen: unterhalb von value ist jeweils ein data/exception-element
          for (Element listelement : listelements) {
            List<Element> childElements = XMLUtils.getChildElements(listelement);
            if (!childElements.isEmpty()) {
              parseChildElements(XMLUtils.getChildElements(listelement), includeNullEntries);
            } else if (includeNullEntries) {
              children.add(null);
            }
          }
        }
      } else {
        Element val = XMLUtils.getChildElementByName(e, GenerationBase.EL.VALUE);
        if (val != null) {
          hasConstantValue = true;
          value = XMLUtils.getTextContentOrNull(val);
        }
      }
    } else {
      if (!datatypeChildElements.isEmpty()) {
        for (Element dtChild : datatypeChildElements) {
          DatatypeVariable v = new DatatypeVariable(creator, revision);
          v.parseXML(dtChild, this, includeNullEntries);
          children.add(v);
        }
      }

      if (!exceptionChildElements.isEmpty()) {
        for (Element exChild : exceptionChildElements) {
          ExceptionVariable v = new ExceptionVariable(creator, revision);
          v.parseXML(exChild, this, includeNullEntries);
          children.add(v);
        }
      }
    }
  }

  private void parseChildElements(List<Element> childElements, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    for (Element childElement : childElements) {
      AVariable var = null;
      if (childElement != null && GenerationBase.EL.DATA.equals(childElement.getTagName())) {
        var = new DatatypeVariable(creator, revision);
      } else if (childElement != null && GenerationBase.EL.EXCEPTION.equals(childElement.getTagName())) {
        var = new ExceptionVariable(creator, revision);
      } else {
        missingDataElement = true;
        if (childElement != null || !includeNullEntries) {
          continue;
        }
      }

      if (var != null) {
        var.parseXML(childElement, this, includeNullEntries);
      }

      children.add(var);
    }
  }

  /**
   * gibt nicht nur den typ der variable zurück sondern auch die typen von membervariablen (-instanzen), 
   * wenn diese in dieser variable auch instanziiert wurden.
   * d.h. für variablen, die nur referenziert werden, gehören die membervariablen nicht zu den dependencies
   * dazu, ansonsten schon (insbesondere dann auch ggfs abgeleitete membervariablen).
   * 
   * grund: wenn variablen konstant vorbelegt sind, müssen alle membervariablen im wf instanziiert werden
   * und werden deshalb direkt verwendet.
   */
  public Set<GenerationBase> getDependencies() {
    HashSet<GenerationBase> set = new HashSet<GenerationBase>();
    getDependenciesRecursively(set);
    if (defaultTypeRestriction != null) {
      set.add(defaultTypeRestriction);
    }
    return set;
  }


  private void getDependenciesRecursively(Set<GenerationBase> set) {
    GenerationBase gb = getDomOrExceptionObject();
    if (gb != null) {
      set.add(gb);
    }
    if (children != null) {
      for (AVariable child : children) {
        child.getDependenciesRecursively(set);
      }
    }
  }


  protected abstract void specialParseXml(Element e, Long revision) throws XPRC_InvalidPackageNameException;


  protected void genericParseXml(Element e, AVariable parent) {

    children = new ArrayList<AVariable>();
    setId(e.getAttribute(GenerationBase.ATT.ID));
    instanceId = e.getAttribute(GenerationBase.ATT.OBJECT_ID);
    refInstanceId = e.getAttribute(GenerationBase.ATT.OBJECT_REFERENCE_ID);
    varName = e.getAttribute(GenerationBase.ATT.VARIABLENAME);
    label = e.getAttribute(GenerationBase.ATT.LABEL);
    this.parentVariableInXml = parent;
    isList = XMLUtils.isTrue(e, GenerationBase.ATT.ISLIST);
    prototype = XMLUtils.isTrue(e, GenerationBase.ATT.ABSTRACT);

    parseUnknownMetaTags(e, Arrays.asList(EL.PERSISTENCE, EL.USEROUTPUT, GenerationBase.EL.DOCUMENTATION));
    Element meta = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
    if (meta != null) {
      persistenceTypes = PersistenceTypeInformation.parse(meta);
      dataModelInformation = DataModelInformation.parse(meta);
      Element documentationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.DOCUMENTATION);
      documentation = XMLUtils.getTextContent(documentationElement);
      
      // bei auditdaten und toXml verwendet:

      Element applicationElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.APPLICATION);
      Element applicationVersionElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.APPLICATION_VERSION);
      Element workspaceElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.WORKSPACE);
      if (applicationElement != null && applicationVersionElement != null) {
        containedRuntimeContextInformation =
            new Application(XMLUtils.getTextContent(applicationElement), XMLUtils.getTextContent(applicationVersionElement));
      } else if (workspaceElement != null) {
        containedRuntimeContextInformation = new Workspace(XMLUtils.getTextContent(workspaceElement));
      }

      Element userOutputElemet = XMLUtils.getChildElementByName(meta, GenerationBase.EL.USEROUTPUT);
      isUserOutput = (userOutputElemet != null) && XMLUtils.getTextContent(userOutputElemet).equals("true");
    }

    for (Element sourceTag : XMLUtils.getChildElementsByName(e, GenerationBase.EL.SOURCE)) {
      if (sourceTag != null) {
        sourceIds.add(sourceTag.getAttribute(GenerationBase.ATT.REFID));
      }
    }

    Element targetTag = XMLUtils.getChildElementByName(e, GenerationBase.EL.TARGET);
    if (targetTag != null) {
      targetId = targetTag.getAttribute(GenerationBase.ATT.REFID);
    }
  }


  @Override
  public String getDocumentation() {
    return documentation;
  }

  public String getId() {
    return this.id;
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public String getRefInstanceId() {
    return this.refInstanceId;
  }

  public final String getVarName() {
    return this.varName;
  }

  public final String getLabel() {
    return this.label;
  }

  public final boolean isList() {
    return isList;
  }
  
  public final boolean isPrototype() {
    return prototype;
  }

  @Override
  public void parseUnknownMetaTags(Element element, List<String> knownMetaTags) {
    unknownMetaTagsComponent.parseUnknownMetaTags(element, knownMetaTags);
  }

  @Override
  public List<String> getUnknownMetaTags() {
    return unknownMetaTagsComponent.getUnknownMetaTags();
  }

  @Override
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    unknownMetaTagsComponent.setUnknownMetaTags(unknownMetaTags);
  }

  @Override
  public boolean hasUnknownMetaTags() {
    return unknownMetaTagsComponent.hasUnknownMetaTags();
  }

  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {
    unknownMetaTagsComponent.appendUnknownMetaTags(xml);
  }

  public final String getFQClassName() {
    return fqClassName;
  }

  protected final void setFQClassName(String s) {
    this.fqClassName = s;
  }

  protected final String getClassNameDirectly() {
    return className;
  }

  protected final String getClassName(Set<String> importedClassesFqStrings) {
    return getClassName(true, importedClassesFqStrings);
  }

  protected void setClassName(String s) {
    this.className = s;
  }

  /**
   * gibt simpleclassname zurück, mit listensupport (inkl generics + extends)<p>
   * Beispiele:
   * <ul>
   * <li>List&lt;boolean&gt;</li>
   * <li>List&lt;? extends MyType&gt;</li>
   * <li>boolean</li>
   * <li>MyType</li>
   * </ul>
   */
  protected String getClassName(boolean withGenerics, Set<String> importedClassesFqStrings) {
    if (isList()) {
      if (withGenerics) {
        return List.class.getSimpleName() + "<" + (isJavaBaseType ? getJavaTypeAsObject() : "? extends " + getSimpleOrFQClassName(importedClassesFqStrings))
                        + ">";
      } else {
        return List.class.getSimpleName();
      }
    }
    if (isJavaBaseType) {
      return javaType.getClassOfType();
    }
    return getSimpleOrFQClassName(importedClassesFqStrings);
  }

  private String getSimpleOrFQClassName(Set<String> importedClassesFqStrings) {
    if (importedClassesFqStrings != null && importedClassesFqStrings.contains(fqClassName)) {
      return className;
    }
    return fqClassName;
  }

  /**
   * gibt import-korrigierten classname zurück, mit listensupport (inkl generics und extends)<p>
   * Beispiele:
   * <ul>
   * <li>List&lt;boolean&gt;</li>
   * <li>List&lt;? extends [bla.]MyType&gt;</li>
   * <li>boolean</li>
   * <li>[bla.]MyType</li>
   * </ul>
   */
  protected String getEventuallyQualifiedClassNameWithGenerics(Set<String> importedClassesFqStrings) {
    return getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, true);
  }

  /**
   * gibt auch list (mit generics) zurück, falls liste<p>
   * Beispiele:
   * <ul>
   * <li>List&lt;boolean&gt;</li>
   * <li>List&lt;[? extends ][bla.]MyType&gt;</li>
   * <li>boolean</li>
   * <li>[bla.]MyType</li>
   * </ul>
   * @param withExtendsGenerix sollen die listen generic parameter mit &lt;? extends classname&gt; erstellt werden?
   */
  public String getEventuallyQualifiedClassNameWithGenerics(Set<String> importedClassesFqStrings,
                                                            boolean withExtendsGenerix) {
    if (isList()) {
      String s = getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings);
      if (withExtendsGenerix) {
        return List.class.getSimpleName() + "<" + (isJavaBaseType ? getJavaTypeAsObject() : "? extends " + s) + ">";
      } else {
        return List.class.getSimpleName() + "<" + (isJavaBaseType ? getJavaTypeAsObject() : s) + ">";
      }
    }
    if (isJavaBaseType) {
      return javaType.getClassOfType();
    }
    return getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings);
  }

  /**
   * gibt nicht list zurück, falls liste<p>
   * Beispiele:
   * <ul>
   * <li>List&lt;boolean&gt; =&gt; boolean</li>
   * <li>List&lt;bla.MyType&gt; =&gt; [bla.]MyType</li>
   * <li>boolean</li>
   * <li>[bla.]MyType</li>
   * </ul>
   */
  //FIXME refactor name in "nolist" anstatt "nogenerics". achtung: wer verwendet das? typemapping? projekte sonst?
  public String getEventuallyQualifiedClassNameNoGenerics(Set<String> currentImportsFqClasses) {
    if (isJavaBaseType) {
      return javaType.getClassOfType();
    }
    return getSimpleOrFQClassName(currentImportsFqClasses);
  }

  
  /**
   * gibt nicht list zurück, falls liste. bei primitiven listen wird der objektifizierte type zurück gegeben<p>
   * Beispiele:
   * <ul>
   * <li>List&lt;boolean&gt; =&gt; Boolean</li>
   * <li>List&lt;bla.MyType&gt; =&gt; [bla.]MyType</li>
   * <li>boolean</li>
   * <li>[bla.]MyType</li>
   * </ul>
   */
  //FIXME refactor name in "nolist" anstatt "nogenerics". achtung: wer verwendet das? typemapping? projekte sonst?
  public String getEventuallyQualifiedClassNameNoGenericsAsObject(Set<String> currentImportsFqClasses) {
    if (isJavaBaseType) {
      if (isList()) {
        return getJavaTypeAsObject();
      } else {
        return javaType.getClassOfType();
      }
    }
    return getSimpleOrFQClassName(currentImportsFqClasses);
  }
  

  public String getEventuallyQualifiedClassNameToBeUsedInGenerics(Set<String> importedClasseNames) {
    if (!isList() && isJavaBaseType && !getJavaTypeEnum().isObject()) {
      return getJavaTypeAsObject();
    }
    return getEventuallyQualifiedClassNameWithGenerics(importedClasseNames, false);
  }
  
  protected String getJavaTypeAsObject() {   
    return javaType.getObjectClassOfType();
  }

  protected void getImports(HashSet<String> imports) {
    if (!isJavaBaseType) {
      if (imports.contains(getFQClassName()))
        return;
      imports.add(getFQClassName());

      if (getDomOrExceptionObject() != null) {
        for (AVariable v : getDomOrExceptionObject().getMemberVars()) {
          v.getImports(imports);
        }
      }
    }
  }
  
  public String getValue() {
    return value;
  }
  
  public String[] getValues() {
    return values;
  }
  
  
  public void setValue(String value) {
    this.value = value;
  }
  
  public void setValues(String[] values) {
    this.values = values;
  }

  boolean hasConstantValue() {
    return hasConstantValue;
  }
  

  /**
   * gibt varname.getA().getB() zurück, falls path = "A.B" ist
   */
  public String getGetter(String path) {
    return getGetter(getVarName(), path);
  }
  
  /**
   * varname.getA().setB(value), falls path = "A.B" ist
   */
  public static String getSetter(String varName, String value, String path) {
    StringBuilder sb = new StringBuilder();
    sb.append(varName);
    if (GenerationBase.isEmpty(path)) {
      sb.append(" = " + value);
    } else {
      String[] parts = path.split("\\.");
      for (int i = 0; i < parts.length - 1; i++) {
        sb.append(".").append(GenerationBase.buildGetter(parts[i])).append("()");
      }
      sb.append(".").append(GenerationBase.buildSetter(parts[parts.length - 1])).append("(").append(value).append(")");
    }
    return sb.toString();
  }
  
  /**
   * gibt varname.getA().getB() zurück, falls path = "A.B" ist
   */
  public static String getGetter(String varName, String path) {
    StringBuilder sb = new StringBuilder();
    sb.append(varName);
    if (!GenerationBase.isEmpty(path)) {
      String[] parts = path.split("\\.");
      for (int i = 0; i<parts.length; i++) {
        sb.append(".").append(GenerationBase.buildGetter(parts[i])).append("()");
      }
    }
    return sb.toString();
  }

  /**
   * varname.getA().setB(value), falls path = "A.B" ist
   */
  protected String getSetter(String value, String path) {
    return getSetter(getVarName(), value, path);
  }


  public final void fillVariableContents() throws XPRC_MEMBER_DATA_NOT_IDENTIFIED,
                  XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
                  XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    fillVariableContents(parentVariableInXml);
  }

  private String variableNotFound;

  /**
   * Ordnet kind-data-elemente zugehoerigen doms zu. hierfuer muessen alle doms bereits geparst sein!
   */
  protected final void fillVariableContents(AVariable aparent) throws XPRC_MEMBER_DATA_NOT_IDENTIFIED,
                  XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
                  XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException {
    
    if ((!isJavaBaseType || javaType == null) && getDomOrExceptionObject() == null && parentVariableInXml != null
                    && parentVariableInXml.getDomOrExceptionObject() != null) {
      DomOrExceptionGenerationBase parentDom = parentVariableInXml.getDomOrExceptionObject();
      // anhand von parentDoms membervars bestimmen, zu welchem dom aktueller knoten gehoert.
      // dann javatype setzen
      ArrayList<AVariable> memberVars = new ArrayList<AVariable>(parentDom.getMemberVars());
      DomOrExceptionGenerationBase superClassDOM = parentDom.getSuperClassGenerationObject();
      while (superClassDOM != null) {
        memberVars.addAll(superClassDOM.getMemberVars());
        superClassDOM = superClassDOM.getSuperClassGenerationObject();
      }
      boolean found = false;
      for (AVariable memberVar : memberVars) {
        if (memberVar.getVarName().equals(getVarName())) {
          if (memberVar.isJavaBaseType()) {
            isJavaBaseType = true;
            javaType = memberVar.getJavaTypeEnum();
          } else {
            setGenerationBaseObject(memberVar.getDomOrExceptionObject());
            setClassName(memberVar.getClassNameDirectly());
            setFQClassName(memberVar.getFQClassName());
            isJavaBaseType = false;
          }
          found = true;
          break;
        }
      }
      if (!found) {
        if (variableNotFound == null) {
          variableNotFound = "";
        } else {
          variableNotFound += ", ";
        }
        variableNotFound += getVarName() + " in " + parentDom.getOriginalFqName();
      }
    }

    for (AVariable v : children) {
      v.fillVariableContents(this);
    }
  }


  public PrimitiveType getJavaTypeEnum() {
    return javaType;
  }

  public final boolean isJavaBaseType() {
    return this.isJavaBaseType;
  }


  public String getOriginalName() {
    return originalClassName;
  }


  public String getOriginalPath() {
    return originalPath;
  }

  
  protected final void generateJava(CodeBuffer cb, boolean withDeclaration, Set<String> importedClassNames) {

    if (withDeclaration) {
      cb.add("private ").add(getEventuallyQualifiedClassNameWithGenerics(importedClassNames) + " ");
    }
    cb.add(getVarName());

    // create a constructor for the following cases:
    // a) trivial type => getDomOrExceptionObject = null
    // b) not abstract
    // c) variable is a list
    if (getDomOrExceptionObject() != null && getDomOrExceptionObject().isAbstract() && !isList()) {
      cb.add(" = null");
    } else {
      cb.add(" = ");
      generateConstructor(cb, importedClassNames, false);
    }
    cb.addLB();

  }


  /**
   * cloning dieser variable
   * @param varNamePrefix zb variablen-name von parentobjekt.
   */
  public void generateJavaClone(CodeBuffer cb, String varNamePrefix, Set<String> importedClassNames, boolean useGetterForVarAccess) {
    String _varAccess =
        (varNamePrefix != null ? varNamePrefix + "." : "")
            + (useGetterForVarAccess ? JavaGenUtils.getGetterFor(getVarName()) + "()" : getVarName());

    if (isList()) {
      cb.add(XynaObject.class.getSimpleName(), ".cloneList(", _varAccess, ", ",
             getEventuallyQualifiedClassNameNoGenerics(importedClassNames),
             ".class, ", String.valueOf(isJavaBaseType()), ", ", "deep)");
    } else {
      if (isJavaBaseType()) {
        cb.add(_varAccess);
      } else {
        cb.add("(", getEventuallyQualifiedClassNameNoGenerics(importedClassNames), ") ", XynaObject.class.getSimpleName(), ".clone(", _varAccess, ", deep)");
      }
    }
  }

  public void generateJavaXml(CodeBuffer cb) {
    generateJavaXml(cb, false); //abwärtskompatibel
  }
  
  public abstract void generateJavaXml(CodeBuffer cb, boolean useCache);
  
  protected void generateConstructor(CodeBuffer cb, Set<String> importedClassNames) {
    generateConstructor(cb, importedClassNames, false);
  }
  
  /**
   * falls ignoreList == true und isList == true, wird nur der konstruktor für ein einzelnes listenelement erzeugt.
   * ansonsten wie {@link #generateConstructor(CodeBuffer, Set)}
   */
  protected abstract void generateConstructor(CodeBuffer cb, Set<String> importedClassNames, boolean ignoreList);
  
  /**
   * @deprecated use {@link #getJavaTypeEnum()}
   */
  @Deprecated
  public final String getJavaType() {
    return javaType.getClassOfType();
  }

  public final List<AVariable> getChildren() {
    if(children == null)
      return null;
    return Collections.unmodifiableList(children);
  }
  
  public void removeChildren(Collection<AVariable> children) {
    this.children.removeAll(children);
  }
  

  public void addChild(AVariable child) {
    if (children == null) {
      children = new ArrayList<AVariable>();
    }
    children.add(child);
  }


  public void setHasConstantValue(boolean hasConstantValue) {
    this.hasConstantValue = hasConstantValue;
  }


  protected String getInstantiableClassName(Set<String> importedClassNames) {
    return getInstantiableClassName(importedClassNames, false);
  }

  protected String getInstantiableClassName(Set<String> importedClassNames, boolean ignoreList) {
    if (isList() && !ignoreList) {
      return ArrayList.class.getSimpleName() + "<"
          + (isJavaBaseType ? getJavaTypeAsObject() : getEventuallyQualifiedClassNameNoGenerics(importedClassNames))
          + ">";
    } else {
      return getEventuallyQualifiedClassNameNoGenerics(importedClassNames);
    }
  }

  protected void generateVariableReference(CodeBuffer cb, boolean leadingComma, List<AVariable> entries,
                                           Set<String> importedClassNames) {
    for (int i = 0; i < entries.size(); i++) {
      AVariable v = entries.get(i);
      if (i > 0 || leadingComma) {
        cb.add(", ");
      }
      generateVariableReference(cb, v, importedClassNames);
    }
  }
  
  protected void generateVariableReference(CodeBuffer cb, AVariable v,
                                           Set<String> importedClassNames) {
    boolean foundChildData = false;
    for (AVariable c : children) {
      if (sameVariableName(v.getVarName(), c.getVarName())) {
        foundChildData = true;
        c.generateConstructor(cb, importedClassNames, false);
        break;
      }
    }
    if (!foundChildData) {
      if (logger.isDebugEnabled()) {
        logger.debug("in " + getInstantiableClassName(importedClassNames) + " no childdata found: " + v.getVarName());
        for (AVariable c : children) {
          logger.debug("children: " + c.getVarName());
        }
      }
      if (v.isList()) {
        v.generateConstructor(cb, importedClassNames, false);
      } else { // keine daten => als wert null übergeben.
        // ist nicht immer javabasetype, aber falls null, wird das abgefangen
        if (v.getJavaTypeEnum() == null) {
          cb.add("(" + v.getEventuallyQualifiedClassNameNoGenerics(importedClassNames) + ") null");
        } else {
          cb.add(v.getJavaTypeEnum().toLiteral(null));
        }
      }
    }
  }

  

  private static boolean sameVariableName(String v1, String v2) {
    return !GenerationBase.isEmpty(v1)
        && !GenerationBase.isEmpty(v2)
        && (v1.length() == 1 && v1.equalsIgnoreCase(v2))
        || (v1.substring(1, v1.length()).equals(v2.substring(1, v2.length())) && v1.substring(0, 1)
            .equalsIgnoreCase(v2.substring(0, 1)));
  }


  /**
   * Füllt das übergebene XynaObject mit den werten, die in dem aktuellen variablen-objekt stecken, also 'this' ist eine
   * variable von parent.
   */
  @SuppressWarnings("unchecked")
  public void fillObject(GeneralXynaObject result) throws InvalidObjectPathException, XDEV_PARAMETER_NAME_NOT_FOUND {
    // xo.setBla(value); und dann für alle kinddata-elemente rekursiv
    if (isJavaBaseType) {
      if (isList()) {
        ArrayList<Object> l = (ArrayList<Object>) result.get(getVarName());
        if (l == null) {
          l = new ArrayList<Object>();
          result.set(getVarName(), l);
        }
        for (String value : values) {
          l.add(castToJavaType(value));
        }
      } else {
        result.set(getVarName(), castToJavaType(value));
      }
    } else {
      if (isList()) {
        ArrayList<Object> l = (ArrayList<Object>) result.get(getVarName());
        if (l == null) {
          l = new ArrayList<Object>();
          result.set(getVarName(), l);
        }
        for (AVariable child : children) {
          // kein javabasetype!
          GeneralXynaObject o = XynaObject.instantiate(child.getFQClassName(), child instanceof DatatypeVariable, child.getDomOrExceptionObject().getRevision());
          l.add(o);
          for (AVariable childVar : child.getChildren()) {
            childVar.fillObject(o);
          }
        }
      } else {
        GeneralXynaObject o = XynaObject.instantiate(getFQClassName(), this instanceof DatatypeVariable, getDomOrExceptionObject().getRevision());
        result.set(getVarName(), o);
        for (AVariable child : children) {
          child.fillObject(o);
        }
      }
    }
  }


  public GeneralXynaObject getXoRepresentation() throws InvalidObjectPathException, XDEV_PARAMETER_NAME_NOT_FOUND {
    
    if(!isList) {
      GeneralXynaObject gxo = XynaObject.instantiate(getFQClassName(), this instanceof DatatypeVariable, getDomOrExceptionObject().getRevision());
      for (AVariable child : children) {
        child.fillObject(gxo);
      }
      return gxo;     
    }
    
    // list
    Class<?> clazz = determineClass();
    GeneralXynaObjectList<GeneralXynaObject> result = new GeneralXynaObjectList<GeneralXynaObject>((Class<GeneralXynaObject>) clazz);
    
    for (AVariable child : children) {
      GeneralXynaObject o = XynaObject.instantiate(child.getFQClassName(), child instanceof DatatypeVariable, child.getDomOrExceptionObject().getRevision());
      result.add(o);
      for (AVariable childVar : child.getChildren()) {
        childVar.fillObject(o);
      }
    }
    
    
    
    return result;
  }
  
  
  private Class<?> determineClass(){
    Class<?> result = null;
    
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    
    result = GenerationBase.getReservedClass(domOrException.getOriginalFqName());
    if(result != null) {
      return result;
    }

    try {
      if (this instanceof DatatypeVariable) {
        result = cld.getMDMClassLoader(fqClassName, revision, true).loadClass(domOrException.getOriginalFqName());
      } else {
        result = cld.getExceptionClassLoader(fqClassName, revision, true).loadClass(domOrException.getOriginalFqName());
      }

    } catch (XFMG_MDMObjectClassLoaderNotFoundException | ClassNotFoundException | XFMG_ExceptionClassLoaderNotFoundException e) {
      throw new RuntimeException(e);
    }
    
    return result;
  }
  

  private static String updateXMLForExceptionLists(String xml, GeneralXynaObject xo, GenerationBase creator) {
    if(!(xo instanceof XynaObjectList<?>))
      return xml;
    
    String typeName = ((XynaObjectList<?>)xo).getContainedFQTypeName();
    try {
      ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
      cld.loadClassWithClassLoader(ClassLoaderType.Exception, typeName, typeName, creator.getRevision());
    }
    catch(Exception e) {
      //not an exception
      return xml;
    }
    
    //change Data to Exception
    xml = xml.replaceFirst(GenerationBase.EL.DATA, GenerationBase.EL.EXCEPTION);
    
    //TODO:
    xml = xml.substring(0, xml.length() - (GenerationBase.EL.DATA.length() + 2));
    xml += GenerationBase.EL.EXCEPTION + ">";
    
    return xml;
  }

  public static AVariable createFromXo(GeneralXynaObject xo, GenerationBase creator, boolean isList) {
    String xml = xo.toXml();
    xml = updateXMLForExceptionLists(xml, xo, creator);
    Document doc;
    try {
      doc = XMLUtils.parseString(xml);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
    Element rootEl = doc.getDocumentElement();
    AVariable v;
    if (rootEl.getTagName().equals(GenerationBase.EL.DATA)) {
      v = new ServiceVariable(creator);
    } else if (rootEl.getTagName().equals(GenerationBase.EL.EXCEPTION)) {
      v = new ExceptionVariable(creator);
    } else {
      throw new RuntimeException("unexpected root tag: " + rootEl.getTagName());
    }
    try {
      v.parseXML(rootEl);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    //einfachster weg, die generationbase objekte der members korrekt zu setzen und zu initialisieren:
    try {
      v.domOrException.createObject(v, false);
    } catch (XPRC_MDMObjectCreationException e) {
      throw new RuntimeException(e);
    }
    return v;
  }


  protected Object castToJavaType(String v) {
    if (v == null) {
      return null;
    }
    if (javaType == null) {
      throw new RuntimeException("Variable " + varName + " is incomplete: isSimple=" + isJavaBaseType + ", type=null, missingDataElement="
          + missingDataElement + ", id=" + id + ", label=" + label + ", unsupportedType=" + unsupportedTypeName + ", value=" + v + ", list="
          + isList);
    }
    return javaType.fromString(v);
  }


  public AVariable getParentVariableInXml() {
    return parentVariableInXml;
  }


  public void setId(String id) {
    this.id = id;
    getCreator().addXmlId(id);
    setVarName(createVarName());
  }


  public boolean hasValue() {
    if (value != null) {
      return true;
    }
    if (values != null && values.length > 0) {
      return true;
    }
    if (children != null && children.size() > 0) {
      return true;
    }
    return false;
  }
  
  public boolean hasValueOrIsDOM() {
    return domOrException != null || hasValue();
  }

  
  public Set<PersistenceTypeInformation> getPersistenceTypes() {
    return persistenceTypes;
  }

  public void setPersistenceTypes(Set<PersistenceTypeInformation> persistenceTypes) {
    this.persistenceTypes = persistenceTypes;
  }

  public String toString() {
    return getClass().getSimpleName() + " id = " + id + " @" + System.identityHashCode(this);
  }
  
  public DataModelInformation getDataModelInformation() {
    return dataModelInformation;
  }


  public String getUniqueTypeName() {
    String t = isList() ? "L$" : "";
    t += isJavaBaseType() ? "E$" + getJavaTypeEnum().name() : getFQClassName();
    return t;
  }


  public void validate() throws XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
    if (missingDataElement) {
      throw new XPRC_InvalidXMLMissingListValueException(getFQClassName());
    }
    if (variableNotFound != null) {
      throw new XPRC_MEMBER_DATA_NOT_IDENTIFIED(variableNotFound);
    }
    if (prototype) {
      throw new XPRC_PrototypeDeployment();
    }
    if (unsupportedTypeName != null) {
      throw new XPRC_JAVATYPE_UNSUPPORTED(unsupportedTypeName);
    }
    if (children != null) {
      for (AVariable child : children) {
        child.validate();
      }
    }
  }
  
  public DomOrExceptionGenerationBase getDefaultTypeRestriction() {
    return defaultTypeRestriction;
  }


  protected void substituteParseXmlRef(String originalClassName, String originalPath, String varName, boolean isList)
      throws XPRC_InvalidPackageNameException {
    children = new ArrayList<AVariable>();
    this.varName = varName;
    this.isList = isList;

    this.originalClassName = originalClassName;
    this.originalPath = originalPath;
    StringBuilder originalFqNameBuilder = new StringBuilder();
    originalFqNameBuilder.append(originalPath).append(".").append(originalClassName);
    String originalFqName = originalFqNameBuilder.toString();
    
    long rev = revision;
    if (containedRuntimeContextInformation != null) {
      try {
        rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(containedRuntimeContextInformation) ;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      }
    }
    
    DomOrExceptionGenerationBase d;
    if (this instanceof ExceptionVariable) {
      d = creator.getCachedExceptionInstanceOrCreate(originalFqName, rev);
    } else {
      d = creator.getCachedDOMInstanceOrCreate(originalFqName, rev);
    }
    setFQClassName(d.getFqClassName());
    setClassName(d.getSimpleClassName());
    setGenerationBaseObject(d);
  }


  // kann anstelle des parse verwendet werden
  protected void substituteParseXmlNoRef(String javaType, String varName, boolean isList) {
    children = new ArrayList<AVariable>();
    this.varName = varName;

    isJavaBaseType = true;
    this.javaType = PrimitiveType.createOrNull(javaType);
    if (this.javaType == null) {
      unsupportedTypeName = javaType;
    }
    this.isList = isList;
  }
  
  public void setIsList(boolean isList) {
    this.isList = isList;
  }

  public void setLabel(String label) {
    this.label = label;
  }
  
  public GenerationBase getCreator() {
    return creator;
  }
  
  public void createPrototype(String label) {
    setId(creator.getNextXmlId().toString());
    replaceDomOrException(null,label);
  }

  public void createDOM(String label, DOM dom) {
    createDomOrException(label, dom);
  }

  public void createDomOrException(String label, DomOrExceptionGenerationBase domOrException) {
    setId(creator.getNextXmlId().toString());
    replaceDomOrException(domOrException, label);
  }

  @Override
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }
  
  public void setVarName(String varName) {
    this.varName = varName;
  }

  protected String createVarName() {
    if (isPrototype()) {
      return Utils.labelToJavaName(getLabel(), false) + getId();
    } else {
      return Utils.changeCaseFirstChar(getOriginalName() + getId(), false);
    }
  }

  public void replaceDOM(DOM dom, String label) {
    replaceDomOrException(dom, label);
  }

  public void replaceDomOrException(DomOrExceptionGenerationBase domOrException, String label) {
    this.domOrException = domOrException;
    this.label = label;
    this.prototype = domOrException == null;
    if( this.prototype ) {
      this.originalPath = null;
      this.originalClassName = null;
      this.fqClassName = null;
    } else {
      this.originalPath = domOrException.getOriginalPath();
      this.originalClassName = domOrException.getOriginalSimpleName();
      this.fqClassName = domOrException.getFqClassName();
    }

    setVarName(createVarName());
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    appendXML(xml, true);
  }

  public void appendXML(XmlBuilder xml, boolean includeReferences) {
    xml.startElementWithAttributes(EL.DATA); {
      appendAttributes(xml);
      xml.endAttributes();

      // <Meta>
      if (hasMeta()) {
        xml.startElement(EL.META); {
          appendMeta(xml);
        } xml.endElement(EL.META);
      }

      appendChildren(xml);
    } xml.endElement();
  }

  protected void appendAttributes(XmlBuilder xml) {
    addAttributeIfFilled(xml, ATT.ID, getId());
    addAttributeIfFilled(xml, ATT.OBJECT_ID, instanceId);
    addAttributeIfFilled(xml, ATT.OBJECT_REFERENCE_ID, refInstanceId);
    addAttributeIfFilled(xml, ATT.VARIABLENAME, getVarName());
    addAttributeIfFilled(xml, ATT.LABEL, getLabel());

    if (isList()) {
      xml.addAttribute(ATT.ISLIST, ATT.TRUE);
    }

    if (isPrototype()) {
      xml.addAttribute(ATT.ABSTRACT, ATT.TRUE);
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(Utils.labelToJavaName(getLabel(), true)));
    } else {
      xml.addAttribute(ATT.REFERENCENAME, XMLUtils.escapeXMLValue(getOriginalName()));
      xml.addAttribute(ATT.REFERENCEPATH, XMLUtils.escapeXMLValue(getOriginalPath()));
    }
  }

  protected boolean hasMeta() {
    return ( (persistenceTypes != null) || isUserOutput() || hasUnknownMetaTags() );
  }

  protected void appendMeta(XmlBuilder xml) {
    // <Persistence>
    if (persistenceTypes != null) {
      xml.startElement(GenerationBase.EL.PERSISTENCE); {
        for (PersistenceTypeInformation persistenceType : persistenceTypes) {
          persistenceType.appendXML(xml);
        }
      } xml.endElement(GenerationBase.EL.PERSISTENCE);
    }

    if (isUserOutput) {
      xml.element(EL.USEROUTPUT, Boolean.toString(isUserOutput));
    }

//    if (dataModelInformation != null) {
//      // TODO
//    }
//    if (documentation != null) {
//      // TODO
//    }
//    if (containedRuntimeContextInformation != null) {
//      // TODO
//    }


    appendUnknownMetaTags(xml);
  }

  protected void appendChildren(XmlBuilder xml) {
    if (!hasConstantValue()) {
      if (children == null) {
        return;
      }

      for (AVariable child : children) {
        child.appendXML(xml, false);
      }

      return;
    }

    if (isList()) {
      if (isJavaBaseType && javaType != PrimitiveType.ANYTYPE) {
        for (String value : getValues()) {
          xml.optionalElement(EL.VALUE, value == null ? null : XMLUtils.escapeXMLValueAndInvalidChars(value, false, false));
        }
      } else {
        if (children == null) {
          return;
        }

        for (AVariable child : children) {
          xml.startElement(EL.VALUE); {
            child.appendXML(xml, false);
          } xml.endElement(EL.VALUE);
        }
      }
    } else if (isJavaBaseType && value != null && javaType != PrimitiveType.ANYTYPE) {
      xml.optionalElement(EL.VALUE, value == null ? null : XMLUtils.escapeXMLValueAndInvalidChars(value, false, false));
    }
  }

  private void addAttributeIfFilled(XmlBuilder xml, String name, String value) {
    if ( (value != null) && (value.length() > 0) ) {
      xml.addAttribute(name, XMLUtils.escapeXMLValue(value, true, false));
    }
  }

  public Set<String> getSourceIds() {
    return sourceIds;
  }

  public void setSourceIds(String ... sourceIds) {
    for (String id : sourceIds) {
      this.sourceIds.add(id);
    }
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }
  
  public long getRevision() {
    return revision;
  }

  public boolean isUserOutput() {
    return isUserOutput;
  }

  public void setIsUserOutput(boolean isUserOutput) {
    this.isUserOutput = isUserOutput;
  }

  public boolean isFunctionResult() {
    return isFunctionResult;
  }

  public void markAsFunctionResult() {
    isFunctionResult = true;
  }
}
