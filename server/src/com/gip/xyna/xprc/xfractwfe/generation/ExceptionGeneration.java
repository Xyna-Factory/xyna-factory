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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidClassNameException;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaClass;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntryProvider;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry_1_1;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionParameter;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance_1_1;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParser;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParserFactory;
import com.gip.xyna.utils.exceptions.xmlstorage.InvalidValuesInXMLException;
import com.gip.xyna.utils.exceptions.xmlstorage.XSDNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateVariableNamesException;
import com.gip.xyna.xprc.exceptions.XPRC_InconsistentFileNameAndContentException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionXmlInvalidBaseReferenceException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectCodeGenerator.BuilderConfig;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectCodeGenerator.ExceptionCloneConfig;



public class ExceptionGeneration extends DomOrExceptionGenerationBase {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExceptionGeneration.class);
  static ReentrantLock cacheLockExceptionGeneration = new ReentrantLock();

  public static final String EXCEPTION_MESSAGE_VARNAME = "message";
  public static final String EXCEPTION_STACKTRACE_VARNAME = "stacktrace";
  public static final String EXCEPTION_CODE_VARNAME = "code";

  // this contains 'stackTrace' and 'stacktrace' since Throwable defines a getter "getStackTrace()"
  private static final String forbiddenMemberVariables[] = new String[] {"cause", EXCEPTION_MESSAGE_VARNAME,
                  "localizedMessage", "stackTrace", EXCEPTION_STACKTRACE_VARNAME, EXCEPTION_CODE_VARNAME};


  private ExceptionStorageInstance exceptionStorage;
  private ExceptionEntry_1_1 exceptionEntry;

  private ExceptionGeneration superClassExceptionGen;

  private ArrayList<AVariable> memberVariables = new ArrayList<AVariable>();


  protected ExceptionGeneration(String originalName, String fqClassName, Long revision) {
    super(originalName, fqClassName, revision);
  }
  
  
  ExceptionGeneration(String originalName, String fqClassName, GenerationBaseCache cache, Long revision, String realType, XMLSourceAbstraction xmlInputSource) {
    super(originalName, fqClassName, cache, revision, realType, xmlInputSource);
  }

  public static ExceptionGeneration getInstance(String originalFQExceptionName)throws XPRC_InvalidPackageNameException {
    return getInstance(originalFQExceptionName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  public static ExceptionGeneration getInstance(String originalFQExceptionName, Long revision) throws XPRC_InvalidPackageNameException {

    String fqExceptionClassName = GenerationBase.transformNameForJava(originalFQExceptionName);
    
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalFQExceptionName, revision);
    
    GenerationBase o = GenerationBase.tryGetGlobalCachedInstance(originalFQExceptionName, revision);
    
    if (o == null) {
      cacheLockExceptionGeneration.lock();
      try {
        o = GenerationBase.tryGetGlobalCachedInstance(originalFQExceptionName, revision);
        if (o == null) {
          o = new ExceptionGeneration(originalFQExceptionName, fqExceptionClassName, revision);
          GenerationBase.cacheGlobal(o);
        }
      } finally {
        cacheLockExceptionGeneration.unlock();
      }
    }

    if (!(o instanceof ExceptionGeneration)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(fqExceptionClassName, "ExceptionGeneration", o.getClass().getSimpleName()));
    }

    return (ExceptionGeneration) o;

  }
  
  public static ExceptionGeneration getOrCreateInstance(String originalInputName, GenerationBaseCache cache, Long revision) throws XPRC_InvalidPackageNameException {
    return getOrCreateInstance(originalInputName, cache, revision, new FactoryManagedRevisionXMLSource());
  }
  
  public static ExceptionGeneration getOrCreateInstance(String originalInputName, GenerationBaseCache cache, Long revision, XMLSourceAbstraction inputSource) throws XPRC_InvalidPackageNameException {
    revision = inputSource.getRevisionDefiningXMOMObjectOrParent(originalInputName, revision);
    String fqClassName = GenerationBase.transformNameForJava(originalInputName);
    ExceptionGeneration exception = (ExceptionGeneration) cache.getFromCache(originalInputName, revision);
    if (exception == null) {
      exception = new ExceptionGeneration(originalInputName, fqClassName, cache, revision, null, inputSource);
      cache.insertIntoCache(exception);
    }
    return exception;
  }
  

  public static ExceptionGeneration generateUncachedInstance(String originalWFInputName,
                                                             boolean fromDeploymentLocation, Long revision)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
      XPRC_MDMDeploymentException {
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalWFInputName, revision);
    ExceptionGeneration excep = getOrCreateInstance(originalWFInputName, new GenerationBaseCache(), revision);
    excep.parseGeneration(fromDeploymentLocation, false, true);
    return excep;
  }


  @Override
  protected void fillVarsInternally() {
  }


  @Override
  protected String[] generateJavaInternally(CodeBuffer cb, boolean compileSafe) throws InvalidClassNameException,
                  InvalidValuesInXMLException {

    ExceptionEntryProvider provider = new ExceptionEntryProvider() {

      public ExceptionEntry get(String path, String name) {
        String fqName = path + "." + name;

        try {
          return getCachedExceptionInstanceOrCreate(fqName, revision).exceptionEntry;
        } catch (XynaException e) {
          throw new RuntimeException("Unexpected error while instantiating '" + fqName + "'", e);
        }
      }

    };
    if (exceptionStorage == null) {
      throw new NullPointerException("exceptionstorage not set for " + getOriginalFqName());
    }

    exceptionStorage.skipGettersAndSetters(); //setter und getter wegen objektversionierung neu generieren
    
    JavaClass[] classes = exceptionStorage.generateJavaClasses(false, provider, exceptionStorage.getXmlFile());
    if (classes == null || classes.length < 1) {
      throw new RuntimeException("ExceptionStorage did not return any java classes.");
    } else if (classes.length > 1) {
      throw new RuntimeException("Cannot handle more than one exception class.");
    }


    boolean hasSuperClassForVersioning =
        hasSuperClassGenerationObject() && !GenerationBase.CORE_XYNAEXCEPTIONBASE.equals(superClassExceptionGen.getOriginalFqName());
    String fqSuperClassName = null;
    if (hasSuperClassForVersioning) {
      fqSuperClassName = superClassExceptionGen.getFqClassName();
    }

    if (hasSuperClassForVersioning && GenerationBase.isReservedServerObjectByFqClassName(fqSuperClassName)) {
      String xmlSuperFqClassName = superClassExceptionGen.getOriginalFqName(); //fqName in XML - invalid for reservedServerObjects
      classes[0].removeImport(xmlSuperFqClassName); //getImports() includes correct import for super class
    }

    
    classes[0].addMemberVar("private static final long serialVersionUID = " + calculateSerialVersionUID() + "L");
    HashSet<String> imports = getImports();
    for (String s : imports) {
      classes[0].addImport(s);
    }
    //imports wurden evtl von den exception utils geaddet
    if (classes[0].removeImport(GenerationBase.CORE_EXCEPTION)) {
      //ntbd
    }
    if (classes[0].removeImport(GenerationBase.CORE_XYNAEXCEPTION)) {
      classes[0].addImport(XynaException.class.getName());
    }
    if (classes[0].removeImport(GenerationBase.CORE_XYNAEXCEPTIONBASE)) {
      classes[0].addImport(XynaExceptionBase.class.getName());
    }


    if (logger.isDebugEnabled()) {
      logger.debug("Got " + classes.length + " class" + (classes.length > 1 ? "es" : "")
                      + " from the exception storage, modifying content...");
    }        

    for (JavaClass clazz : classes) {
      clazz.addInterface(GeneralXynaObject.class.getName());
      CodeBuffer codeBufferForClazz;

      for (AVariable v : memberVariables) {
        //versionedObject memberVar
        codeBufferForClazz = new CodeBuffer("XynaProcessing");
        XynaObjectCodeGenerator.appendVersionedObjectMemberVar(codeBufferForClazz, v, imports);
        clazz.addMethod(codeBufferForClazz);

        //Getter
        codeBufferForClazz = new CodeBuffer("XynaProcessing");
        XynaObjectCodeGenerator.appendGetter(codeBufferForClazz, v, imports, false);
        clazz.addMethod(codeBufferForClazz);
        
        //Setter
        codeBufferForClazz = new CodeBuffer("XynaProcessing");
        XynaObjectCodeGenerator.appendSetter(codeBufferForClazz, v, imports);
        clazz.addMethod(codeBufferForClazz);
        
        //versionedGetter
        codeBufferForClazz = new CodeBuffer("XynaProcessing");
        XynaObjectCodeGenerator.appendVersionedGetter(codeBufferForClazz, v, imports);
        clazz.addMethod(codeBufferForClazz);
        
        //unversionedSetter for Builder
        codeBufferForClazz = new CodeBuffer("XynaProcessing");
        XynaObjectCodeGenerator.appendUnversionedSetter(codeBufferForClazz, imports, v);
        clazz.addMethod(codeBufferForClazz);
      }
      
      //useVersioning methode
      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      XynaObjectCodeGenerator.appendUseVersioningMethod(codeBufferForClazz);
      clazz.addMethod(codeBufferForClazz);
      
      //objectversion class
      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      XynaObjectCodeGenerator.appendObjectVersionClassGeneration(codeBufferForClazz, memberVariables, imports, getSimpleClassName(), fqSuperClassName);
      clazz.addMethod(codeBufferForClazz);     
      
      //collectChanges methode
      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      XynaObjectCodeGenerator.appendVersionChangeCollection(codeBufferForClazz, memberVariables, hasSuperClassForVersioning);
      clazz.addMethod(codeBufferForClazz);     
      
      //toXml signaturen:      
      codeBufferForClazz = new CodeBuffer("Xyna Processing");
      codeBufferForClazz.addLine("public String toXml() {");
      codeBufferForClazz.addLine("return toXml(null)");
      codeBufferForClazz.addLine("}");
      clazz.addMethod(codeBufferForClazz);

      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      codeBufferForClazz.addLine("public String toXml(String varName) {");
      codeBufferForClazz.addLine("return toXml(varName, false, -1, null)");
      codeBufferForClazz.addLine("}");
      clazz.addMethod(codeBufferForClazz);

      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      codeBufferForClazz.addLine("public String toXml(String varName, boolean onlyContent) {");
      codeBufferForClazz.addLine("return toXml(varName, onlyContent, -1, null)");
      codeBufferForClazz.addLine("}").addLB();
      clazz.addMethod(codeBufferForClazz);

      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      codeBufferForClazz.addLine("public String toXml(String varName, boolean onlyContent, long version, ",
                                 XMLReferenceCache.class.getCanonicalName(), " cache) {");
      //TODO unterstützung für onlyContent (wird für super-aufrufe verwendet, also hier gerade nicht notwendig)
      codeBufferForClazz.addLine("long objectId");
      XynaObjectCodeGenerator.appendToXMLSnippetForObjectReferences(codeBufferForClazz);
      codeBufferForClazz.addLine("StringBuilder xml = new StringBuilder()");
      codeBufferForClazz.addLine(XynaObject.XMLHelper.class.getCanonicalName(), ".beginExceptionType(xml, varName, \"",
                                 getOriginalSimpleName(), "\", \"", getOriginalPath(), "\", objectId, refId, ",
                                 RevisionManagement.class.getSimpleName(), ".getRevisionByClass(getClass()), cache)");

      if (getAllMemberVarsIncludingInherited().size() > 0) {
        codeBufferForClazz.addLine("if (objectId != -2) {");
        // member variables
        for (AVariable v : getAllMemberVarsIncludingInherited()) {
          v.generateJavaXml(codeBufferForClazz, true);
        }
        codeBufferForClazz.addLine("}");
      }

      codeBufferForClazz.addLine(XynaObject.XMLHelper.class.getCanonicalName(), ".endExceptionType(xml)");
      codeBufferForClazz.addLine("return xml.toString()");
      codeBufferForClazz.addLine("}").addLB();
      clazz.addMethod(codeBufferForClazz);
      
      String superClass = superClassExceptionGen != null ? superClassExceptionGen.getSimpleClassName() : null;
      if("XynaExceptionBase".equals(superClass)) {
        superClass = null; //do not consider XynaExceptionBase as parent here 
      }
      
      //cloneWithoutCause, fillVars
      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      ExceptionCloneConfig config = new ExceptionCloneConfig();
      config.setAbstract(isAbstract());
      config.setHasSuperClass(superClass != null);
      config.setImportedClassNames(imports);
      config.setMembers(memberVariables);
      config.setSimpleClassName(clazz.getSimpleClassName());
      XynaObjectCodeGenerator.generateJavaClone(codeBufferForClazz, config);
      clazz.addMethod(codeBufferForClazz);

      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      XynaObjectCodeGenerator.appendGeneralSetter(codeBufferForClazz, imports, getMemberVars(), hasSuperClassForVersioning);
      clazz.addMethod(codeBufferForClazz);

      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      XynaObjectCodeGenerator.appendGeneralGetter(codeBufferForClazz, imports, getMemberVars(), hasSuperClassForVersioning);
      clazz.addMethod(codeBufferForClazz);
      
      //builder
      codeBufferForClazz = new CodeBuffer("XynaProcessing");
      BuilderConfig builderConfig = new BuilderConfig();
      builderConfig.setAbstract(isAbstract());
      builderConfig.setClassName(getSimpleClassName());
      builderConfig.setImports(imports);
      builderConfig.setMembers(getMemberVars());
      builderConfig.setSuperClassName(superClass);
      CodeBuffer constructor = XynaObjectCodeGenerator.createExceptionBuilderAdditionalConstructorContent(imports, getMemberVars());
      builderConfig.setAdditionalConstructorContent(constructor);
      XynaObjectCodeGenerator.generateJavaClassBuilder(codeBufferForClazz, builderConfig);
      clazz.addNestedClass(codeBufferForClazz);
    }
    

    if (logger.isDebugEnabled()) {
      logger.debug("Successfully modified class definitions, getting source");
    }

    String[] result = new String[classes.length];
    for (int i = 0; i < classes.length; i++) {
      result[i] = classes[i].getSourceCode("XynaProcessing");
    }

    return result;

  }


  @Override
  protected void parseXmlInternally(Element rootElement) throws XPRC_InconsistentFileNameAndContentException,
      XPRC_InvalidPackageNameException, InvalidXMLException {

    ExceptionStorageParser parser;
    try {
      parser = ExceptionStorageParserFactory.getParser(rootElement.getOwnerDocument());
      exceptionStorage = parser.parse(false, 0);
    } catch (XSDNotFoundException e) {
      throw new RuntimeException(e);
    }
    if (exceptionStorage.getEntries().size() != 1) {
      throw new RuntimeException("Invalid number of contained exceptions: " + exceptionStorage.getEntries().size());
    }

    ExceptionEntry entryUncasted = exceptionStorage.getEntries().get(0);
    if (!(entryUncasted instanceof ExceptionEntry_1_1)) {
      throw new RuntimeException("Invalid entry in provided exception file: outdated exception format "
          + entryUncasted.getClass().getName());
    }

    exceptionEntry = (ExceptionEntry_1_1) entryUncasted;

    validateClassName(exceptionEntry.getPath(), exceptionEntry.getName());

    Element exceptionTypeElement = XMLUtils.getChildElementsByName(rootElement, EL.EXCEPTIONTYPE).get(0);
    setLabel(exceptionTypeElement.getAttribute(GenerationBase.ATT.LABEL));

    setIsAbstract(exceptionEntry.isAbstract());


    if (exceptionEntry.getBaseExceptionName() != null && exceptionEntry.getBaseExceptionPath() != null) {
      String superClassFQ = exceptionEntry.getBaseExceptionPath() + "." + exceptionEntry.getBaseExceptionName();
      superClassExceptionGen = getCachedExceptionInstanceOrCreate(superClassFQ, revision);
    }

    memberVariables = new ArrayList<AVariable>();
    for (ExceptionParameter exParameter : exceptionEntry.getParameter()) {
      AVariable v = createVariableFromExceptionParameter(exParameter);
      memberVariables.add(v);
    }

    // documentation
    Element metaElement = XMLUtils.getChildElementByName(exceptionTypeElement, GenerationBase.EL.META);
    if (metaElement != null) {
      Element documentationElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        setDocumentation(XMLUtils.getTextContent(documentationElement));
      }
    }
  }


  private AVariable createVariableFromExceptionParameter(ExceptionParameter parameter) throws XPRC_InvalidPackageNameException {

    AVariable result;
    if (parameter.getType().equals("Data")) {
      result = new DatatypeVariable(this, revision);
    } else {
      result = new ExceptionVariable(this, revision);
    }
    if (parameter.isReference()) {
      result.substituteParseXmlRef(parameter.getTypeName(), parameter.getTypePath(), parameter.getVarName(), parameter.isList());
    } else {
      result.substituteParseXmlNoRef(parameter.getJavaType(), parameter.getVarName(), parameter.isList());
    }

    result.setLabel(parameter.getLabel());
//    result.setDocumentation(parameter.getDocumentation()); TODO PMOD-259

    return result;
  }


  @Override
  protected void validateInternally() throws XPRC_DuplicateVariableNamesException, XPRC_InvalidVariableNameException,
      XPRC_InvalidExceptionXmlInvalidBaseReferenceException {
    if (exceptionEntry.getBaseExceptionName() == null || exceptionEntry.getBaseExceptionPath() == null) {
      if (!(exceptionEntry.getBaseExceptionName() == null && exceptionEntry.getBaseExceptionPath() == null)) {
        throw new XPRC_InvalidExceptionXmlInvalidBaseReferenceException(exceptionEntry.getPath() + "." + exceptionEntry.getName());
      }
    }
    
    // make sure variable names are unique
    GenerationBase.checkUniqueVarNamesWithInherited(getAllMemberVarsIncludingInherited());

    // make sure that no illegal variable names like "cause" are used
    for (AVariable variable : getAllMemberVarsIncludingInherited()) {
      for (String forbiddenName : forbiddenMemberVariables) {
        // use ignoreCase just to be sure
        if (forbiddenName.equalsIgnoreCase(variable.getVarName())) {
          throw new XPRC_InvalidVariableNameException(getOriginalFqName(), variable.getVarName());
        }
      }
    }

  }


  private HashSet<String> getImports() {

    HashSet<String> imports = new HashSet<String>();
    imports.add(XynaException.class.getName());
    imports.add(XynaExceptionBase.class.getName());
    imports.add(GeneralXynaObject.class.getName());
    imports.add(XynaObject.class.getName());
    imports.add(XOUtils.class.getName());
    imports.add(XMLHelper.class.getCanonicalName());
    imports.add(InvalidObjectPathException.class.getName());
    imports.add(XDEV_PARAMETER_NAME_NOT_FOUND.class.getName());
    
    //runtimeContext für audits
    imports.add(RevisionManagement.class.getName());

    boolean simpleListRequired = false;
    boolean xynaObjectListRequired = false;
    
    for (AVariable v : memberVariables) {
      if (!v.isJavaBaseType()) {
        imports.add(v.getFQClassName());
        if(v.isList) {
          xynaObjectListRequired = true;
        }
      }
      if(v.isList) {
        simpleListRequired = true;
      }
    }

    if (simpleListRequired) {
      imports.add(ArrayList.class.getCanonicalName());
      imports.add(Arrays.class.getCanonicalName());
    }
    
    if (xynaObjectListRequired) {
      imports.add(XynaObjectList.class.getCanonicalName());
    }

    
    if (superClassExceptionGen != null) {
      imports.add(superClassExceptionGen.getFqClassName());
    }

    return imports;

  }


  @Override
  public List<AVariable> getMemberVars() {
    return Collections.unmodifiableList(this.memberVariables);
  }


  @Override
  public boolean replaceMemberVar(AVariable oldVar, AVariable newVar) {
    int index = memberVariables.indexOf(oldVar);
    if (index < 0) {
      return false;
    }

    memberVariables.set(index, newVar);

    return true;
  }


  public void replaceParent(ExceptionGeneration exceptionGeneration) {
    this.superClassExceptionGen = exceptionGeneration;
  }


  @Override
  public void addMemberVar(int index, AVariable var) {
    memberVariables.add(index, var);
  }


  @Override
  public boolean removeMemberVar(AVariable var) {
    return memberVariables.remove(var);
  }


  @Override
  public AVariable removeMemberVar(int index) {
    return memberVariables.remove(index);
  }


  public List<AVariable> getAllMemberVarsIncludingInherited() {
    ExceptionGeneration d = superClassExceptionGen;
    List<List<AVariable>> allMemberVars = new ArrayList<List<AVariable>>();
    allMemberVars.add(getMemberVars());
    while (d != null) {
      List<AVariable> dml = d.getMemberVars();
      allMemberVars.add(dml);
      d = d.superClassExceptionGen;
    }

    List<AVariable> allMemberVars2 = new ArrayList<AVariable>();
    for (int i = allMemberVars.size() - 1; i >= 0; i--) {
      for (AVariable v : allMemberVars.get(i)) {
        allMemberVars2.add(v);
      }
    }
    return allMemberVars2;
  }


  @Override
  public Set<GenerationBase> getDirectlyDependentObjects() {
    Set<GenerationBase> result = new HashSet<GenerationBase>();
    for (AVariable v : getMemberVars()) {
      if (v.getDomOrExceptionObject() != null) { // javatype?
        result.add(v.getDomOrExceptionObject());
      }
    }
    if (superClassExceptionGen != null) {
      result.add(superClassExceptionGen);
    }
    return result;
  }


  public DomOrExceptionGenerationBase getSuperClassGenerationObject() {
    return superClassExceptionGen;
  }
  
  
  @Override
  public boolean hasSuperClassGenerationObject() {
    return superClassExceptionGen != null;
  }


  public ExceptionEntry getExceptionEntry() {
    return exceptionEntry;
  }


  protected ExceptionGeneration getSuperClassExceptionGeneration() {
    return superClassExceptionGen;
  }


  @Override
  public boolean compareImplementation(GenerationBase oldVersion) {
    ExceptionGeneration oldExcep = (ExceptionGeneration) oldVersion;
    //superclass
    if (superClassExceptionGen != null) {
      if (oldExcep.superClassExceptionGen != null) {
        if (!superClassExceptionGen.getFqClassName().equals(oldExcep.superClassExceptionGen.getFqClassName())) {
          return true;
        }
      } else {
        return true;
      }
    } else {
      if (oldExcep.superClassExceptionGen != null) {
        return true;
      }
    }
    //memberVars
    if (memberVariables.size() != oldExcep.memberVariables.size()) {
      return true;
    }
    for (int i = 0; i < memberVariables.size(); i++) {
      if (memberVariables.get(i).isJavaBaseType) {
        if (oldExcep.memberVariables.get(i).isJavaBaseType) {
          if (memberVariables.get(i).javaType != oldExcep.memberVariables.get(i).javaType) {
            return true;
          }
        } else {
          return true;
        }
      } else {
        if (!oldExcep.memberVariables.get(i).isJavaBaseType) {
          if (!memberVariables.get(i).getFQClassName().equals(oldExcep.memberVariables.get(i).getFQClassName())) {
            return true;
          }
        } else {
          if (!memberVariables.get(i).getFQClassName().equals(oldExcep.memberVariables.get(i).getFQClassName())) {
            return true;
          }
        }
      }

      if (!memberVariables.get(i).getVarName().equals(oldExcep.memberVariables.get(i).getVarName())) {
        return true;
      }
    }
    return false;
  }


  @Override
  protected String getHumanReadableTypeName() {
    return "Exception";
  }

  @Override
  protected DependencySourceType getDependencySourceType() {
    return DependencySourceType.XYNAEXCEPTION;
  }


  public void createEmpty(String label) {
    setLabel(label);

    exceptionStorage = new ExceptionStorageInstance_1_1();
    exceptionEntry = new ExceptionEntry_1_1(new HashMap<String, String>(), getOriginalSimpleName(), getOriginalPath(), null);
    exceptionStorage.addEntry(exceptionEntry);

    try {
      superClassExceptionGen = getCachedExceptionInstanceOrCreate(XynaExceptionBase.class.getCanonicalName(), revision);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException("Could not create new Exception", e);
    }
  }

}
