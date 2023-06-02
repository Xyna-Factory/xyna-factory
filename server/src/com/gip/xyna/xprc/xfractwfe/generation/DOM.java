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



import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.collections.lists.StringSerializableList.SeparatorSerializeAlgorithm;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibClassLoader;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateVariableNamesException;
import com.gip.xyna.xprc.exceptions.XPRC_InconsistentFileNameAndContentException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMethodAbstractAndStaticException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MayNotOverrideFinalOperationException;
import com.gip.xyna.xprc.exceptions.XPRC_MdmDeploymentCyclicInheritanceException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.serviceimpl.JavaServiceImplementation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class DOM extends DomOrExceptionGenerationBase {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(DOM.class);
  public final static String INIT_METHODNAME = "initImplementationOfInstanceMethods";
  public final static String INSTANCE_METHODS_IMPL_VAR = "implementationOfInstanceMethods";

  public static final String READER_FOR_DATATYPE = "READER_FOR_DATATYPE";
  public static final String TRANSFORMDATATYPE_METHODNAME = "transformDatatype";
  public static final String FILLDIRECTMEMBERS_METHODNAME = "fillDirectMembers";


  
  private ArrayList<AVariable> memberVars = new ArrayList<AVariable>();
  private Map<String, List<Operation>> serviceNameToOperationMap = new HashMap<String, List<Operation>>();
  private Map<String, String> serviceNameToServiceLabel = new HashMap<String, String>();
  private DOM superClassDom;
  static ReentrantLock cacheLockDOM = new ReentrantLock();

  // used for DependencyRegister
  private AdditionalDependencyContainer additionalDependencies = new AdditionalDependencyContainer();

  // used internally for getDependency()
  private Set<GenerationBase> additionalDependenciesSet = new HashSet<GenerationBase>();
  
  private Set<String> additionalLibNames = new TreeSet<String>();
  private Set<String> sharedLibs = new HashSet<String>();
  
  private PersistenceInformation persistenceInformation;
  private DataModelInformation dataModelInformation;
  private PathMapInformation pathMapInformation;

  private Boolean isServiceGroupOnly = null;

  private DOM(String originalName, String domInputNameFQ, Long revision) {
    super(originalName, domInputNameFQ, revision);
  }
  
  DOM(String originalName, String domInputNameFQ, GenerationBaseCache cache, Long revision, String realType, XMLSourceAbstraction inputSource) {
    super(originalName, domInputNameFQ, cache, revision, realType, inputSource);
  }

  public static DOM getInstance(String fqXmlName) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                        XPRC_InvalidPackageNameException {
    return getInstance(fqXmlName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static DOM getInstance(String fqXmlName, Long revision) throws XPRC_InvalidPackageNameException {
    String fqClassName = transformNameForJava(fqXmlName);

    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(fqXmlName, revision);

    GenerationBase o = tryGetGlobalCachedInstance(fqXmlName, revision);
    if (o == null) {
      cacheLockDOM.lock();
      try {
        o = tryGetGlobalCachedInstance(fqXmlName, revision);
        if (o == null) {
          o = new DOM(fqXmlName, fqClassName, revision);
          GenerationBase.cacheGlobal(o);
        }
      } finally {
        cacheLockDOM.unlock();
      }
    }

    if (!(o instanceof DOM)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(fqXmlName, "DOM", o.getClass().getSimpleName()));
    }
    return (DOM) o;
  }
  
  public static DOM getOrCreateInstance(String fqXmlName, GenerationBaseCache cache, Long revision) throws XPRC_InvalidPackageNameException {
    return getOrCreateInstance(fqXmlName, cache, revision, new FactoryManagedRevisionXMLSource());
  }

  public static DOM getOrCreateInstance(String fqXmlName,
                                        GenerationBaseCache cache, Long revision, XMLSourceAbstraction inputSource)
      throws XPRC_InvalidPackageNameException {
    revision = inputSource.getRevisionDefiningXMOMObjectOrParent(fqXmlName, revision);
    String fqClassName = GenerationBase.transformNameForJava(fqXmlName);
    DOM dom = (DOM) cache.getFromCache(fqXmlName, revision);
    if (dom == null) {
      dom = new DOM(fqXmlName, fqClassName, cache, revision, null, inputSource);
      cache.insertIntoCache(dom);
    }
    
    return dom;
  }
  

  public static DOM generateUncachedInstance(String originalInputName, boolean fromDeploymentLocation, Long revision)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
      XPRC_MDMDeploymentException {
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalInputName, revision);
    
    DOM dom = getOrCreateInstance(originalInputName, new GenerationBaseCache(), revision);
    dom.parseGeneration(fromDeploymentLocation, false, true);
    return dom;
  }
  
  
  public static void deploy(List<String> objects, DeploymentMode mode, boolean inheritCodeChange, WorkflowProtectionMode remode, String comment)
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    List<GenerationBase> doms = new ArrayList<GenerationBase>();
    for (String fqXmlName : objects) {
      DOM d = getInstance(fqXmlName);
      d.setDeploymentComment(comment);
      doms.add(d);  
    }
    GenerationBase.deploy(doms, mode, inheritCodeChange, remode);
  }
  

  //false = serialisierte objektinstanzen der alten version können von der neuen version problemlos deserialisiert werden und alle methoden, die es in der alten
  //        version gab, können immer noch aufgerufen werden.
  @Override
  public boolean compareImplementation(GenerationBase oldVersion) {
    DOM oldDOM = (DOM)oldVersion;
    //super class 
    if (superClassDom != null) {
      if (oldDOM.superClassDom != null) {
        if (!superClassDom.getFqClassName().equals(oldDOM.superClassDom.getFqClassName())) {
          return true;
        }
      } else {
        return true;
      }
    } else {
      if (oldDOM.superClassDom != null) {
        return true;
      }
    }
    //memberVars
    if (memberVars.size() < oldDOM.memberVars.size()) {
      return true;
    }
    //jede membervariable von old muss immer noch vorhanden sein, ansonsten true zurückgeben
    Map<String, AVariable> nameToNewVars = new HashMap<>();
    for (AVariable v : memberVars) {
      nameToNewVars.put(v.getVarName(), v);
    }
    for (AVariable oldV : oldDOM.memberVars) {
      AVariable newV = nameToNewVars.get(oldV.getVarName());
      if (newV == null) {
        return true;
      }
      if (newV.isList != oldV.isList) {
        return true;
      }
      if (newV.isJavaBaseType) {
        if (oldV.isJavaBaseType) {
          if (newV.getJavaTypeEnum() != oldV.getJavaTypeEnum()) {
            return true;
          }
        } else {
          return true;
        }
      } else  {
        if (oldV.isJavaBaseType) {
          return true;
        }
        if (!oldV.getFQClassName().equals(newV.getFQClassName())) {
          return true;
        }
      }
    }

    //operations & serviceNames
    if (serviceNameToOperationMap.size() < oldDOM.serviceNameToOperationMap.size()) {
      return true;
    }

    Set<Entry<String, List<Operation>>> oldServiceToOperationMapEntries = oldDOM.serviceNameToOperationMap.entrySet();
    for (Entry<String, List<Operation>> oldServiceToOperationMapEntry : oldServiceToOperationMapEntries) {
      if (!serviceNameToOperationMap.containsKey(oldServiceToOperationMapEntry.getKey())) {
        return true;
      } else {
        if (serviceNameToOperationMap.get(oldServiceToOperationMapEntry.getKey()).size() < oldServiceToOperationMapEntry
            .getValue().size()) {
          return true;
        }
        oldOperationLoop : for (Operation oldOperation : oldServiceToOperationMapEntry.getValue()) {
          for (Operation newOperation : serviceNameToOperationMap.get(oldServiceToOperationMapEntry.getKey())) {
            if (!oldOperation.compareImplementation(newOperation)) {
              continue oldOperationLoop;
            }
          }
          return true;
        }
      }
    }
 
    return false;
  }


  private void addDependenciesOfVariable(AVariable var, Set<GenerationBase> set) {
    DomOrExceptionGenerationBase doe = var.getDomOrExceptionObject();
    if (doe != null) { // javatype?
      set.add(doe);
    }
    if (var.isList()) {
      for (AVariable child : var.getChildren()) {
        //kinder könnten von abgeleitetem typ sein (z.b. konstante vorbelegung oder beim fromXML)
        addDependenciesOfVariable(child, set);
      }
    }
  }


  @Override
  public Set<GenerationBase> getDirectlyDependentObjects() {
    Set<GenerationBase> result = new HashSet<GenerationBase>();
    for (AVariable v : memberVars) {
      addDependenciesOfVariable(v, result);
    }
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      for (Operation op : operations) {
        result.addAll(op.getDependentDoms());
        result.addAll(op.getDependentWFs());
        result.addAll(op.getDependentExceptions());
      }
    }
    if (superClassDom != null) {
      result.add(superClassDom);
    }

    // add additional dependencies 
    result.addAll(additionalDependenciesSet);
    
    return result;
  }


  @Override
  protected String[] generateJavaInternally(CodeBuffer cb, boolean compileSafe) {
    // TODO extract this mechanism since it is used in WF and DOM classes

    XynaObjectCodeGenerator xocg = new XynaObjectCodeGenerator(this);
    
    xocg.generateJavaInternally(cb);
    
    return new String[] {cb.toString()};
  }





  /**
   * getter der form .getX().getY() für den relativen pfad von fullPath relativ zu basePath. 
   */
  public static String createGetterForRelativePath(String fullPath, String basePath) {
    StringBuilder getter = new StringBuilder();
    String relativePath = fullPath.substring(basePath.length());
    if (relativePath.length() > 0 && relativePath.startsWith(".")) {
      relativePath = relativePath.substring(1);
    }
    for (String part : relativePath.split("\\.")) {
      getter.append(".").append(buildGetter(part)).append("()");
    }
    return getter.toString();
  }






  public static boolean foundCycle(DOM d, List<DOM> previousDoms) {
    for (DOM previous : previousDoms) {
      if (previous == d) {
        return true;
      }
      //FIXME was ist mit subtypen?
    }
    return false;
  }



  public  boolean isInheritedFromStorable(boolean directly) {
    if (superClassDom == null) {
      return false;
    }
    if (directly) {
      return superClassDom.getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS);
    } else {
      return isInheritedFromStorable();
    }
  }


  @Deprecated
  public static final XynaPropertyBuilds<StringSerializableList<String>> storableInterfaces =
      new XynaPropertyBuilds<StringSerializableList<String>>("xprc.xfractwfe.generation.storable.xmom.interfaces",
                                                             new StringSerializableList<String>(String.class, new ArrayList<String>(),
                                                                                                new SeparatorSerializeAlgorithm(",")))
                                                                                                    .setDefaultDocumentation(DocumentationLanguage.EN,
                                                                                                                             "Comma separated list of fqnames of xmom storable types, that should only be used as interfaces. They must not have super types outside this list (except xnwh.persistence.Storable).");


  //storable equivalents sind per xynaproperty (s.o.) definierte storable klassen, die nur als marker klassen existieren, und kein eigenes relationales mapping besitzen
  public boolean isStorableEquivalent() {
    if (getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
      return true;
    }

    //immediate parent has to be STORABLE_BASE_CLASS
    if (getSuperClassGenerationObject() == null
        || !getSuperClassGenerationObject().getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
      return false;
    }

    StringSerializableList<String> storableInterfaceList = storableInterfaces.get();
    if (storableInterfaceList != null && storableInterfaceList.contains(getFqClassName())) {
      return true;
    }

    if (persistenceInformation != null) {
      return !persistenceInformation.hasTableRepresentation();
    } else {
      return true;
    }
  }


  public boolean isInheritedFromStorable() {
    if (superClassDom != null) {
      DOM d = this;
      while (d.superClassDom != null) {
        d = d.superClassDom;
      }
      //d ist die zugrundeliegende basisklasse
      if (d.getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
        return true;
      }
    }
    return false;
  }
  
  
  public DOM getBaseStorableType() {
    DOM current = this;
    DOM mostRecentValid = this;
    while (current.superClassDom != null) {
      if (current.superClassDom.isStorableEquivalent()) {
        if (current.superClassDom.persistenceInformation.hasTableRepresentation()) {
          break;
        }
      } else {
        mostRecentValid = current.superClassDom;
      }
      current = current.superClassDom;
    }
    return mostRecentValid;
  }





  @Override
  protected void parseXmlInternally(Element rootElement) throws XPRC_InconsistentFileNameAndContentException,
      XPRC_InvalidPackageNameException {
    // validierung, dass xml in einer sprach-version vorliegt, die der server versteht
    validateClassName(rootElement.getAttribute(GenerationBase.ATT.TYPEPATH),
                      rootElement.getAttribute(GenerationBase.ATT.TYPENAME));
    if (!isEmpty(rootElement.getAttribute(GenerationBase.ATT.BASETYPENAME))) {
      // FIXME hier müsste man eigentlich das super class dom auch parsen, um sicherzugehen, dass das nicht ein Exceptiontyp
      //       ist, den man hier zurückerhält...
      superClassDom =
          getCachedDOMInstanceOrCreate(rootElement.getAttribute(GenerationBase.ATT.BASETYPEPATH) + "."
              + rootElement.getAttribute(GenerationBase.ATT.BASETYPENAME), revision);
    }
    setIsAbstract(XMLUtils.isTrue(rootElement, ATT.ABSTRACT));
    setLabel(rootElement.getAttribute(GenerationBase.ATT.LABEL));

    // parse memberVars
    List<Element> ds = XMLUtils.getChildElementsByName(rootElement, EL.DATA);
    for (Element d : ds) {
      DatatypeVariable v = new DatatypeVariable(this, revision);
      v.parseXML(d);
      memberVars.add(v);
    }
    ds = XMLUtils.getChildElementsByName(rootElement, EL.EXCEPTION);
    for (Element d : ds) {
      ExceptionVariable v = new ExceptionVariable(this, revision);
      v.parseXML(d);
      memberVars.add(v);
    }

    //parse libraries
    List<Element> sharedLibElements = XMLUtils.getChildElementsByName(rootElement, EL.SHAREDLIB);
    Set<String> sharedLibs = new HashSet<String>();
    if (sharedLibElements != null) {
      for (Element element : sharedLibElements) {
        String content = XMLUtils.getTextContent(element);
        if (!GenerationBase.isEmpty(content)) {
          sharedLibs.add(content.trim());
        }
      }
      this.sharedLibs.addAll(sharedLibs);
    }
    List<Element> libElements = XMLUtils.getChildElementsByName(rootElement, EL.LIBRARIES);
    if (libElements != null) {
      for (Element element : libElements) {
        String content = XMLUtils.getTextContent(element);
        if (!GenerationBase.isEmpty(content)) {
          additionalLibNames.add(content.trim());
        }
      }
    }

    // parse operations
    List<Element> ss = XMLUtils.getChildElementsByName(rootElement, GenerationBase.EL.SERVICE);
    if (ss != null && ss.size() > 0) {
      for (Element s : ss) {
        String serviceName = s.getAttribute(ATT.TYPENAME);
        String serviceLabel = s.getAttribute(ATT.LABEL);
        List<Operation> operationsForService = new ArrayList<Operation>();
        List<Element> ops = XMLUtils.getChildElementsByName(s, EL.OPERATION);
        for (Element op : ops) {
          boolean wfCall = XMLUtils.getChildElementsByName(op, EL.WORKFLOW_CALL).size() > 0;
          Operation o;
          if (wfCall) {
            o = new WorkflowCallInService(this);
          } else {
            o = new JavaOperation(this);
          }
          o.parseXML(op);
          operationsForService.add(o);
          if (o.isAbstract()) {
            setIsAbstract(true);
          }
        }
        parseAdditionalDependencies(s, sharedLibs);
        serviceNameToOperationMap.put(serviceName, operationsForService);
        serviceNameToServiceLabel.put(serviceName, serviceLabel);
      }
    }
    // servicereferences
    ss = XMLUtils.getChildElementsByName(rootElement, EL.SERVICEREFERENCE);
    for (Element s : ss) {
      String serviceName = s.getAttribute(ATT.REFERENCENAME);
      String serviceLabel = s.getAttribute(ATT.LABEL);
      List<Operation> operationsForService = new ArrayList<Operation>();
      Operation o = new WorkflowCallServiceReference(this);
      o.parseXML(s);
      operationsForService.add(o);
      serviceNameToOperationMap.put(serviceName, operationsForService);
      serviceNameToServiceLabel.put(serviceName, serviceLabel);
    }

    Element metaElement = XMLUtils.getChildElementByName(rootElement, GenerationBase.EL.META);
    if (metaElement != null) {
      Element documentationElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DOCUMENTATION);
      if (documentationElement != null) {
        setDocumentation(XMLUtils.getTextContent(documentationElement));
      }

      dataModelInformation = DataModelInformation.parse(metaElement);

      pathMapInformation = PathMapInformation.parse(metaElement);

      Element sgOnlyElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.IS_SERVICE_GROUP_ONLY);
      if (sgOnlyElement != null) {
        isServiceGroupOnly = Boolean.parseBoolean(XMLUtils.getTextContent(sgOnlyElement));
      } else {
        isServiceGroupOnly = null;
      }
    }

    //pktype ist ggf aus supertype und wird erst später befüllt (validate)
    persistenceInformation = PersistenceInformation.parse(metaElement);
    
    if (isStorableEquivalent() && 
        persistenceInformation.hasTableRepresentation()) {
      for (AVariable v : memberVars) {
        if (v.persistenceTypes != null) {
          v.persistenceTypes.clear();
        }
      }
    }
  }

  public Set<String> getAdditionalLibraries() {
    return additionalLibNames;
  }

  public void addAdditionalLibrary(int index, String libName) {
    if (index != additionalLibNames.size()) {
      throw new RuntimeException("Only inserting a new lib at the end of the list is supported, right now.");
    }

    additionalLibNames.add(libName);
  }

  public String deleteAdditionalLibrary(int index) {
    int libIdx = 0;
    for (String lib : additionalLibNames) {
      if (libIdx == index) {
        additionalLibNames.remove(lib);
        return lib;
      }

      libIdx++;
    }

    return null;
  }

  public void addSharedLib(String libName) {
    sharedLibs.add(libName);
  }

  public boolean deleteSharedLib(String libName) {
    return sharedLibs.remove(libName);
  }

  public static String getDeployedJarFilePathInDefaultMDM(String fqClassName) {
    return getDeployedJarFilePathInMDM(fqClassName, null);
  }


  public static String getDeployedJarFilePathInMDM(String fqClassName, Long revision) {
    if (revision == null) {
      revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    return RevisionManagement.getPathForRevision(PathType.SERVICE, revision) + Constants.fileSeparator + fqClassName + Constants.fileSeparator;
  }
  

  private void parseAdditionalDependencies(Element s, Set<String> parsedSharedLibs)
      throws XPRC_InvalidPackageNameException {

    Element meta = XMLUtils.getChildElementByName(s, EL.META);
    if (meta != null) {
      getAdditionalDependencies().parseMore(meta);

      // build dependency set with generation base
      for (String domName : getAdditionalDependencies().getAdditionalDependencies(AdditionalDependencyType.DATATYPE)) {
        additionalDependenciesSet.add(getCachedDOMInstanceOrCreate(domName, revision));
      }
      for (String wfName : getAdditionalDependencies().getAdditionalDependencies(AdditionalDependencyType.WORKFLOW)) {
        additionalDependenciesSet.add(getCachedWFInstanceOrCreate(wfName, revision));
      }
      for (String exceptionName : getAdditionalDependencies()
          .getAdditionalDependencies(AdditionalDependencyType.EXCEPTION)) {
        additionalDependenciesSet.add(getCachedExceptionInstanceOrCreate(exceptionName, revision));
      }
    }
    if (parsedSharedLibs != null) {
      getAdditionalDependencies().addAdditionalSharedLibDependencies(parsedSharedLibs);
    }

  }


  /**
   * fügt ohne rekursion abhängige jars dazu
   */
  public void getDependentJarsWithoutRecursion(Set<String> jars, boolean withSharedLibs, boolean tryFromSaved) throws XPRC_JarFileForServiceImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {
    if (XynaFactory.isFactoryServer()) {
      Set<String> ret = new HashSet<String>();
      for (String libName : additionalLibNames) {
        ret.add(getJarFileForServiceLocation(getFqClassName(), revision, libName, tryFromSaved, xmlInputSource).getPath());
      }
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      if (withSharedLibs) {
        for (String s : sharedLibs) {
          // FIXME abhängigkeit von sharedlibclassloader entfernen.
          Long rev = rcdm.getRevisionDefiningSharedLib(s, revision);
          if (rev == null) {
            throw new XFMG_SHARED_LIB_NOT_FOUND(s);
          }
          File[] files = SharedLibClassLoader.getJarsOfSharedLib(s, rev);
          for (File f : files) {
            ret.add(f.getPath());
          }
        }
      }
      jars.addAll(ret);
    }
  }
  
  public static File getJarFileForServiceLocation(String fqClassName, Long revision, String jarName, boolean tryFromSaved, XMLSourceAbstraction source) throws XPRC_JarFileForServiceImplNotFoundException {
  File jarFile;
    if (tryFromSaved && (revision == null || source.isOfRuntimeContextType(revision, RuntimeContextType.Workspace))) {
      jarFile = new File(GenerationBase.getFileLocationOfServiceLibsForSaving(fqClassName, revision) + Constants.fileSeparator + jarName);
      if (jarFile.exists()) {
        return jarFile;
      }
    }
    jarFile = new File(DOM.getDeployedJarFilePathInMDM(fqClassName, revision) + jarName);
    if (!jarFile.exists()) {
      throw new XPRC_JarFileForServiceImplNotFoundException(fqClassName, jarFile.getPath());
    } else {
      return jarFile;
    }
  }


  /**
   * rekursion über andere davon abhängige doms, nicht über WFs
   */
  public void getDependentJarsWithRecursion(Set<String> jars, boolean withSharedLibs, boolean tryFromSaved)
      throws XPRC_JarFileForServiceImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {
    jars.addAll(getAdditionalLibsWithRecursion(withSharedLibs, new ArrayList<DOM>(), tryFromSaved));
  }


  private Set<String> getAdditionalLibsWithRecursion(boolean withSharedLibs, ArrayList<DOM> workedOperations, boolean tryFromSaved)
      throws XPRC_JarFileForServiceImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {
    if (workedOperations.contains(this))
      return new HashSet<String>();
    else
      workedOperations.add(this);

    //HashSet<String> result = getAdditionalLibsWithoutRecursion(withSharedLibs);
    Set<String> result = new HashSet<String>();
    getDependentJarsWithoutRecursion(result, withSharedLibs, tryFromSaved);
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      for (Operation op : operations) {
        if (op instanceof JavaOperation) {
          for (AVariable v : ((JavaOperation) op).getInputVars()) {
            if (!(v.getDomOrExceptionObject() instanceof DOM))
              continue;
            result.addAll(((DOM) v.getDomOrExceptionObject()).getAdditionalLibsWithRecursion(withSharedLibs,
                                                                                             workedOperations,
                                                                                             tryFromSaved));
          }
          for (AVariable v : ((JavaOperation) op).getOutputVars()) {
            if (!(v.getDomOrExceptionObject() instanceof DOM))
              continue;
            result.addAll(((DOM) v.getDomOrExceptionObject()).getAdditionalLibsWithRecursion(withSharedLibs,
                                                                                             workedOperations,
                                                                                             tryFromSaved));
          }
        }
      }
    }

    return result;
  }


  public String[] getSharedLibs() {
    return sharedLibs.toArray(new String[0]);
  }


  @Override
  protected void validateInternally() throws XPRC_InvalidVariableNameException, XPRC_DuplicateVariableNamesException,
      XPRC_MdmDeploymentCyclicInheritanceException, XPRC_MayNotOverrideFinalOperationException, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_InvalidXmlMissingRequiredElementException,
      XPRC_InvalidXmlMethodAbstractAndStaticException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {

    throwExceptionIfNotExists();
    
    DOM nextSuperClassDOM = getSuperClassGenerationObject();
    while (nextSuperClassDOM != null) {
      // due to thread safety of the deployment we can just check the object references, this is faster than a string comparison
      if (this == nextSuperClassDOM) {
        throw new XPRC_MdmDeploymentCyclicInheritanceException(getOriginalFqName());
      }
      nextSuperClassDOM = nextSuperClassDOM.getSuperClassGenerationObject();
    }
    validateVars(memberVars);
    for (List<Operation> ops : serviceNameToOperationMap.values()) {
      for (Operation op : ops) {
        op.validate();
      }
    }
    
    GenerationBase.checkUniqueVarNamesWithInherited(getAllMemberVarsIncludingInherited());

    boolean foundAbstractMethod = false;
    if (getSuperClassGenerationObject() != null) {
      for (Operation o : getOperations()) {
        if (o.isAbstract()) {
          foundAbstractMethod = true;
        }
        if (!o.isStatic()) {
          //finde die superclass, die diese operation definiert. falls überschreiben nicht ok -> fehler.
          nextSuperClassDOM = getSuperClassGenerationObject();
          while (nextSuperClassDOM != null) {
            //finde operation mit gleichem namen und parametern:
            boolean found = false;
            for (Operation oSuper : nextSuperClassDOM.getOperations()) {
              if (oSuper.hasEqualSignature(o)) {
                if (oSuper.isFinal()) {
                  throw new XPRC_MayNotOverrideFinalOperationException(o.getName(),
                                                                       nextSuperClassDOM.getOriginalFqName(),
                                                                       getOriginalFqName());
                }
              }
            }
            if (found) {
              break; //nächste operation checken
            }
            //operation nicht gefunden, vielleicht in oberklasse??
            nextSuperClassDOM = nextSuperClassDOM.getSuperClassGenerationObject();
          }
        }
      }
    }
    
    if (!isAbstract() && !foundAbstractMethod) {
      //nach abstrakten methoden in den oberklassen suchen, die hier nicht überschrieben werden
      OperationInformation[] operations = collectOperationsOfDOMHierarchy(false);
      for (OperationInformation o : operations) {
        if (o.isAbstract()) {
          foundAbstractMethod = true;
          break;
        }
      }
    }
    
    if (!isAbstract() && foundAbstractMethod) {
      //die gui sollte das verhindern
      throw new RuntimeException("Datatype " + getOriginalFqName() + " is not defined as abstract but has or inherits abstract methods.");
    }


    //checken, dass instanzmethoden, die auf workflows zeigen, die korrekten inputparameter haben
    for (Operation o : getOperations()) {
      if (o instanceof WorkflowCallInService) {
        WorkflowCallInService wop = ((WorkflowCallInService) o);
        List<AVariable> inputVars = wop.getWf().getInputVars();

        if (!wop.getWf().exists()) {
          throw new RuntimeException("Workflow " + wop.getWf().getOriginalFqName() + " referenced by operation "
              + o.getName() + " in datatype " + getOriginalFqName() + " is missing.");
        }
        if (inputVars.size() == 0) {
          throw new RuntimeException("Workflow " + wop.getWf().getOriginalFqName() + " referenced by operation "
              + o.getName() + " in datatype " + getOriginalFqName() + " has no input parameters.");
        }

        //ersten input entfernen //TODO validieren, dass der erste input einen passenden typ hat TODO outputvars validieren TODO beim validieren supertypen zulassen
        List<AVariable> inputVarsCopy = new ArrayList<AVariable>();
        boolean first = true;
        for (AVariable av : inputVars) {
          if (first) {
            first = false;
            continue;
          }
          inputVarsCopy.add(av);
        }

        if (!Operation.parametersAreEqual(inputVarsCopy, o.getInputVars())) {
          throw new RuntimeException("Workflow " + wop.getWf().getOriginalFqName() + " referenced by operation "
              + o.getName() + " in datatype " + getOriginalFqName() + " has incompatible input parameters.");
        }
      }
    }
    
    if (isInheritedFromStorable() && 
        !isStorableEquivalent()) {
      //gibt es genau eine unique id?
      List<AVariable> allMemberVars = getAllMemberVarsIncludingInherited();
      int cntUnID = 0;
      for (AVariable var : allMemberVars) {
        if (var.getPersistenceTypes() != null && 
            var.getPersistenceTypes().contains(PersistenceTypeInformation.UNIQUE_IDENTIFIER)) {
          cntUnID ++;
        }
      }
      if (cntUnID != 1) {
        if (cntUnID == 0) {
          throw new RuntimeException("Datatype " + getOriginalFqName() + " has not defined an unique identifier.");
        } else {
          throw new RuntimeException("Datatype " + getOriginalFqName() + " has defined more than one unique identifier.");
        }
      }
      
      //sind historization metamarkierungen sinnvoll gesetzt etc
      persistenceInformation.validate(this);

      checkUniqueLowerCaseVarNamesForStorablesRecursively(this, this, "", new ArrayList<DOM>());
      
      //automatisch flattening aktivieren, falls so konfiguriert TODO das passt eigtl nicht so gut ins validate... aber wohin sonst?
      if (xmomStorableFlatteningMode.get() != FlatteningMode.NONE && (mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew)) {
        List<String> pathsToAutoFlat = new ArrayList<String>();
        List<String> pathsToAutoExclude = new ArrayList<String>();
        List<DOM> previousDoms = new ArrayList<DOM>();
        previousDoms.add(this);
        if (xmomStorableFlatteningMode.get() == FlatteningMode.SINGLE_VAR_ONLY) {
          collectPathToAutoFlatForSingleVarTypes(this, this, "", previousDoms, pathsToAutoFlat, false);
        } else {
          GenerationBaseCache gbc = new GenerationBaseCache();
          collectPathToAutoFlat(this, this, "", previousDoms, pathsToAutoFlat, pathsToAutoExclude, false, gbc);
        }
        if (pathsToAutoFlat.size() > 0 ||
            pathsToAutoExclude.size() > 0) {
          for (String pathToAutoFlat : pathsToAutoFlat) {
            persistenceInformation.getFlattened().add(pathToAutoFlat);
          }
          for (String pathToAutoExclude : pathsToAutoExclude) {
            persistenceInformation.getFlatExclusions().add(pathToAutoExclude);
          }
          
          //im xml verewigen. sowohl im saved als auch im deployed-ordner
          //TODO achtung! bei xsd änderungen sind die xml anpassungen evtl auch anzupassen
          try {
            Document d = XMLUtils.parse(getFileLocationForDeploymentStaticHelper(getOriginalFqName(), getRevision()) + ".xml", true);
            Element docEl = d.getDocumentElement();
            Element metaEl = XMLUtils.getChildElementByName(docEl, GenerationBase.EL.META);
            if (metaEl == null) {
              metaEl = d.createElement(GenerationBase.EL.META);              
              List<Element> childElements = XMLUtils.getChildElements(docEl);
              if (childElements.size() == 0) {
                docEl.appendChild(metaEl);
              } else {
                docEl.insertBefore(metaEl, childElements.get(0));
              }
            }
            Element persistenceEl = XMLUtils.getChildElementByName(metaEl, GenerationBase.EL.PERSISTENCE);
            if (persistenceEl == null) {
              persistenceEl = d.createElement(GenerationBase.EL.PERSISTENCE);
              metaEl.appendChild(persistenceEl);
            }
            for (String pathToAutoFlat : pathsToAutoFlat) {
              if (logger.isDebugEnabled()) {
                logger.debug("autoflattening path " + pathToAutoFlat + " in xmom storable " + getOriginalFqName());
              }
              Element flatEl = d.createElement(PersistenceInformation.FLAT);
              persistenceEl.appendChild(flatEl);
              Text flatTxt = d.createTextNode(pathToAutoFlat);
              flatEl.appendChild(flatTxt);
            }
            for (String pathToAutoExclude : pathsToAutoExclude) {
              if (logger.isDebugEnabled()) {
                logger.debug("autoflatexclusion path " + pathToAutoExclude + " in xmom storable " + getOriginalFqName());
              }
              Element flatEl = d.createElement(PersistenceInformation.FLAT_EXCLUSION);
              persistenceEl.appendChild(flatEl);
              Text flatTxt = d.createTextNode(pathToAutoExclude);
              flatEl.appendChild(flatTxt);
            }
            
            XMLUtils.saveDom(new File(getFileLocationForDeploymentStaticHelper(getOriginalFqName(), getRevision()) + ".xml"), d);
            if (xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) {
              XMLUtils.saveDom(new File(getFileLocationForSavingStaticHelper(getOriginalFqName(), getRevision()) + ".xml"), d);
            }
          } catch (Ex_FileAccessException e) {
            throw new RuntimeException(e);
          } catch (XPRC_XmlParsingException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }


  private void collectPathToAutoFlatForSingleVarTypes(DOM baseType, DOM dom, String path, List<DOM> previousDoms, List<String> pathsToAutoFlat, boolean isList) {
    int cntSimpleTypeMemberVars = 0;
    int cntNonSimpleTypeMemberVars = 0;
    for (AVariable var : dom.getAllMemberVarsIncludingInherited()) {
      String childPath = path;
      if (childPath.length() > 0) {
        childPath += ".";
      }
      childPath += var.getVarName();

      if (XMOMStorableStructureCache.isTransient(baseType, var, childPath)) {
        continue;
      }
      
      if (var.isJavaBaseType()) {
        if (var.isList()) {
          //nichts zu tun -> extra tabelle
          cntNonSimpleTypeMemberVars++;
        } else {
          cntSimpleTypeMemberVars++;
        }
      } else {
        DomOrExceptionGenerationBase d = var.getDomOrExceptionObject();
        if (d instanceof DOM) {
          DOM childDom = (DOM) d;
          if (XMOMStorableStructureCache.isStorableReference(baseType, childPath)) {
            cntNonSimpleTypeMemberVars++;
          } else if (foundCycle(childDom, previousDoms)) {
            cntNonSimpleTypeMemberVars++;
            //als blob speichern, so wie eine exception
          } else if (XMOMStorableStructureCache.isFlat(baseType, childPath)) {
            cntNonSimpleTypeMemberVars++;
          } else {
            //expansiv oder expansivList
            cntNonSimpleTypeMemberVars++;
            previousDoms.add(childDom);
            collectPathToAutoFlatForSingleVarTypes(baseType, childDom, childPath, previousDoms, pathsToAutoFlat, var.isList());
            previousDoms.remove(previousDoms.size() - 1);
          }
        } else {
          cntNonSimpleTypeMemberVars++;
          //exception -> blob
        }
      }
    }

    if (cntNonSimpleTypeMemberVars == 0 && cntSimpleTypeMemberVars == 1 && !isList) {
      pathsToAutoFlat.add(path);
    }
  }
  
  
  private void collectPathToAutoFlat(DOM baseType, DOM dom, String path, List<DOM> previousDoms, List<String> pathsToAutoFlat, List<String> pathsToAutoExclude, boolean parentWasFlat, GenerationBaseCache gbc) {
    for (AVariable var : dom.getAllMemberVarsIncludingInherited()) {
      String childPath = path;
      if (childPath.length() > 0) {
        childPath += ".";
      }
      childPath += var.getVarName();

      if (XMOMStorableStructureCache.isTransient(baseType, var, childPath)) {
        continue;
      }
      
      if (!var.isJavaBaseType()) {
        DomOrExceptionGenerationBase d = var.getDomOrExceptionObject();
        if (d instanceof DOM) {
          DOM childDom = (DOM) d;
          if (XMOMStorableStructureCache.isStorableReference(baseType, childPath)) {
            
          } else if (foundCycle(childDom, previousDoms)) {
            
          } else {
            boolean flat = true;
            if (var.isList()) {
              flat = false;
            } else {
              if (childDom.hasSuperClassGenerationObject() &&
                  !childDom.getSuperClassGenerationObject().isStorableEquivalent()) {
                previousDoms.add(childDom.getSuperClassGenerationObject());
                collectPathToAutoFlat(baseType, childDom.getSuperClassGenerationObject(), childPath, previousDoms, pathsToAutoFlat, pathsToAutoExclude, parentWasFlat, gbc);
                previousDoms.remove(previousDoms.size() - 1);
                if (xmomStorableFlatteningMode.get() == FlatteningMode.EXCLUDE_HIERARCHIES) {
                  flat = false;
                  pathsToAutoExclude.add(childPath); 
                }
              }
              Set<GenerationBase> subTypes = childDom.getSubTypes(gbc,false);
              if (childDom.getSubTypes(gbc, false).size() > 0) {
                flat = false;
                pathsToAutoExclude.add(childPath);
                for (GenerationBase subType : subTypes) {
                  previousDoms.add((DOM)subType);
                  collectPathToAutoFlat(baseType, (DOM)subType, childPath, previousDoms, pathsToAutoFlat, pathsToAutoExclude, parentWasFlat, gbc);
                  previousDoms.remove(previousDoms.size() - 1);
                }
              }
            }
           
            if (flat && !parentWasFlat) {
              if (!XMOMStorableStructureCache.isFlat(baseType, childPath)) {
                pathsToAutoFlat.add(childPath);  
              }
            }
            
            //expansiv oder expansivList
            previousDoms.add(childDom);
            collectPathToAutoFlat(baseType, childDom, childPath, previousDoms, pathsToAutoFlat, pathsToAutoExclude, flat, gbc);
            previousDoms.remove(previousDoms.size() - 1);
          }
        }
      }
    }
  }
  

  private static final XynaPropertyEnum<FlatteningMode> xmomStorableFlatteningMode =
                  new XynaPropertyEnum<FlatteningMode>("xyna.xnwh.persistence.xmom.flattening.auto.mode", FlatteningMode.class, FlatteningMode.NONE);
  static {
    xmomStorableFlatteningMode
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Creates flattening entries for members of xmom storables automatically depending on the mode. Possible modes are:"
                                     + Arrays.stream(FlatteningMode.values())
                                         .map(m -> "\n" + m.name() + ": " + m.getDoc(DocumentationLanguage.EN))
                                         .collect(Collectors.joining(", ")));
  }


  private static enum FlatteningMode {
    NONE("No auto flattening"), 
    SINGLE_VAR_ONLY("Auto flattening of complex member variables (that do not have subtypes) that have only one primitive member variable (including members of super types)"), 
    EXCLUDE_HIERARCHIES("Auto flattening of all members that have neither sub- nor super types"), 
    EXCLUDE_TYPES_WITH_SUBTYPES("Auto flattening of all members that don't have subtypes");

    private final String docEN;


    private FlatteningMode(String docEN) {
      this.docEN = docEN;
    }


    public String getDoc(DocumentationLanguage lang) {
      return docEN;
    }
  }


  /**
   * alle variablennamen müssen auch in lowercase eindeutig sein. auch für die verwendeten datentypen
   */
  private static void checkUniqueLowerCaseVarNamesForStorablesRecursively(DOM rootXMOMStorable, DOM currentDom,
                                                                          String path, List<DOM> previousDoms) {
    if (foundCycle(currentDom, previousDoms)) {
      return;
    }
    previousDoms.add(currentDom);
    List<AVariable> allMemberVars = currentDom.getAllMemberVarsIncludingInherited();
    Set<String> lowerCaseVarNames = new HashSet<String>();
    for (AVariable v : allMemberVars) {      
      String localPath = path;
      if (path.length() > 0) {
        localPath += ".";
      }
      localPath += v.getVarName();

      if (!v.isJavaBaseType() && v.getDomOrExceptionObject() instanceof DOM) {
        //ggf rekursion
        if (XMOMStorableStructureCache.isStorableReference(rootXMOMStorable, localPath)) {
          //ok next
        } else if (XMOMStorableStructureCache.isTransient(rootXMOMStorable, v, localPath)) {
          //ok next
        } else {
          checkUniqueLowerCaseVarNamesForStorablesRecursively(rootXMOMStorable, (DOM) v.getDomOrExceptionObject(),
                                                              localPath, previousDoms);
        }
      } else {
        //membervar die in einer storable-spalte resultiert
        if (XMOMStorableStructureCache.isTransient(rootXMOMStorable, v, localPath)) {
          //ok egal 
        } else {
          if (!lowerCaseVarNames.add(v.getVarName().toLowerCase())) {
            //varName doppelt!
            throw new RuntimeException("Root XMOM Storable " + rootXMOMStorable.getOriginalFqName()
                + " contains a member variable of type " + currentDom.getOriginalFqName()
                + " in its hierarchy. This datatype contains two membervars with the same case insensitive name: "
                + v.getVarName().toLowerCase());
          }
        }
      }
    }
    previousDoms.remove(previousDoms.size() - 1);
  }


  public DOM[] getDOMHierarchy() {
    DOM nextSuperClassDOM = this;
    List<DOM> hierarchyList = new ArrayList<DOM>();
    while (nextSuperClassDOM != null) {
      hierarchyList.add(nextSuperClassDOM);
      nextSuperClassDOM = nextSuperClassDOM.getSuperClassGenerationObject();
    }
    return hierarchyList.toArray(new DOM[hierarchyList.size()]);
  }


  public static class OperationInformation {

    /**
     * domHierarchy[0] = aktuelles dom. domHierarchy[1] ist das super-dom davon usw
     */
    private final DOM[] domHierarchy;
    //die operation kann in dem dom definiert sein, oder in einem seiner super-doms
    private final Operation operation;


    public OperationInformation(DOM dom, Operation operation) {
      this.operation = operation;
      domHierarchy = dom.getDOMHierarchy();
    }


    /**
     * operation des doms, oder eine geerbte operation eines seiner super-doms
     */
    public Operation getOperation() {
      return operation;
    }


    /**
     * hier abstrakt oder geerbt abstrakt
     */
    public boolean isAbstract() {
      return operation.isAbstract();
    }


    /**
     * hier final definiert oder geerbt final
     */
    public boolean isFinal() {
      return operation.isFinal();
    }

    /**
     * @deprecated use {@link #isAbstractInBaseType()} instead
     */
     @Deprecated
    public boolean IsAbstractInBaseType() {
      return isAbstractInBaseType();
    }
    
    /**
     * ~ muss man also überschreiben, wenn man das objekt instanziieren will 
     */
    public boolean isAbstractInBaseType() {
      if (getDefiningType() == domHierarchy[0]) {
        return false;
      }
      boolean first = true;
      for (DOM d : domHierarchy) {
        if (first) {
          //erstes nicht berücksichtigen
          first = false;
          continue;
        }
        try {
          Operation op = d.getOperationByName(operation.getName());
          if (op.isAbstract()) {
            return true;
          } else {
            return false;
          }
        } catch (XPRC_OperationUnknownException e) {
          continue;
        }
      }
      throw new RuntimeException("operation not found");
    }


    /**
     * ist operation hier oder in oberklasse in java implementiert.
     * falls nein -&gt; return nein.
     * 
     * falls ja, return "isAbstract" dort.
     */
    public boolean isNotAbstractInJavaServiceImpl() {
      for (DOM d : domHierarchy) {
        try {
          Operation op = d.getOperationByName(operation.getName());
          if (op.implementedInJavaLib()) {
            return true;
          }
        } catch (XPRC_OperationUnknownException e) {
          continue;
        }
      }
      return false;
    }


    /**
     * dom, in den die operation das erste mal definiert wurde (d.h. sie ist in keinem der super-typen definiert)
     */
    public DOM getDefiningType() {
      for (int i = domHierarchy.length - 1; i >= 0; i--) {
        if (isOperationDefined(domHierarchy[i])) {
          return domHierarchy[i];
        }
      }
      throw new RuntimeException("Operation not found");
    }


    private boolean isOperationDefined(DOM dom) {
      try {
        Operation op = dom.getOperationByName(operation.getName());
        if (op != null) {
          return true;
        }
      } catch (XPRC_OperationUnknownException e) {
      }
      return false;
    }


    public boolean isImplementedHere() {
      return operation.getParent() == domHierarchy[0];
    }


    public DOM getImplementingType() {
      if (isAbstract()) {
        throw new RuntimeException();
      }

      for (DOM d : domHierarchy) {
        try {
          Operation op = d.getOperationByName(operation.getName());
          if (op.isAbstract()) {
            throw new RuntimeException();
          } else {
            return d;
          }
        } catch (XPRC_OperationUnknownException e) {
          continue;
        }
      }
      throw new RuntimeException("Operation not implemented and not abstract");
    }


    public boolean isFinalInSuperType() {
      if (isAbstract()) {
        return false;
      }
      DOM implType = getImplementingType();
      if (implType == domHierarchy[0]) {
        return false;
      }
      try {
        return implType.getOperationByName(operation.getName()).isFinal();
      } catch (XPRC_OperationUnknownException e) {
        throw new RuntimeException(e);
      }
    }

  }


  /**
   * gibt es instance methoden in einem obertyp?
   */
  public boolean hasSuperTypeWithInstanceMethods(InterfaceVersion versionFilter) { 
    DOM[] hierarchy = getDOMHierarchy();
    if (hierarchy.length == 1) {
      return false;
    }
    boolean first = true;
    for (DOM dom : hierarchy) {
      if (first) {
        first = false;
        continue;
      }
      if (dom.hasInheritableInstanceMethods(versionFilter)) {
        return true;
      }
    }
    return false;
  }

  /**
   * sind lokal methoden definiert, die in einem subtyp überschrieben werden können
   * (weder static noch final)
   */
  public boolean hasInheritableInstanceMethods(InterfaceVersion versionFilter) {
    boolean hasInheritableInstanceMethods = false;
    for (Operation op : getOperations()) {
      if (!op.isStatic() && !op.isFinal() && (versionFilter == null || versionFilter.equals(op.getVersion()))) {
        hasInheritableInstanceMethods = true;
        break;
      }
    }
    return hasInheritableInstanceMethods;
  }


  public boolean hasSuperTypeWithJavaImpl(boolean onlyCountInstanceMethods, InterfaceVersion versionFilter) {
    return getNextSuperTypeWithJavaImpl(onlyCountInstanceMethods, versionFilter) != null;
  }


  public boolean isFirstTypeOfHierarchyWithJavaImpl(boolean onlyCountInstanceMethods, InterfaceVersion versionFilter) {
    return !hasSuperTypeWithJavaImpl(onlyCountInstanceMethods, versionFilter) && hasJavaImpl(onlyCountInstanceMethods, versionFilter);    
  }


  public boolean hasJavaImpl(boolean onlyCountInstanceMethods, InterfaceVersion versionFilter) {
    boolean hasInstanceMethods = false;
    boolean hasStaticMethods = false;
    //bei den methoden zu checken ist nicht redundant (später wird auf das impl-jar gecheckt), weil die library unter umständen nicht da ist
    //und trotzdem verwendet werden soll. z.b. beim ersten deployment aus der gui
    for (Operation op : getOperations()) {
      if (onlyCountInstanceMethods && op.isStatic()) {
        continue;
      }
      if (versionFilter != null && !versionFilter.equals(op.getVersion())) {
        continue;
      }
      if (!op.isStatic()) {
        hasInstanceMethods = true;
      } else {
        hasStaticMethods = true;
      }
      if (op.implementedInJavaLib()) {
        return true;
      }
    }
    // => keine methode verwendet das impl-jar

    //unterstützung von javalib an einer instanz, die die lib nicht selbst verwendet (aber für subklassen zur verfügung stellt)
    if (libraryExists()) {
      if (onlyCountInstanceMethods && !hasInstanceMethods && hasStaticMethods) {
        //library bezieht sich auf statische methoden
        return false;
      }
      //library angegeben und bezieht sich entweder auf lokale instanzmethoden oder auf welche einer parentklasse oder man will eine library ohne methoden
      return true;
    }
    return false;
  }

  /**
   * impl lib wird verwendet
   */
  public boolean libraryExists() {
    return getAdditionalLibraries().contains(getImplClassName() + ".jar") && 
           XynaFactory.isFactoryServer(); // ignore libraries for script access
  }

  public DOM getNextSuperTypeWithJavaImpl(boolean onlyCountInstanceMethods, InterfaceVersion versionFilter) {
    DOM[] hierarchy = getDOMHierarchy();
    if (hierarchy.length == 1) {
      return null;
    }
    boolean first = true;
    for (DOM dom : hierarchy) {
      if (first) {
        first = false;
        continue;
      }
      if (dom.hasJavaImpl(onlyCountInstanceMethods, versionFilter)) {
        return dom;
      }
    }
    return null;
  }


  /**
   * für statische operations nur die des aktuellen types, nicht die der hierarchy!<br>
   * nach name sortiert
   */
  public OperationInformation[] collectOperationsOfDOMHierarchy(boolean includeStaticMethods) {
    //TODO nicht nur nach dem operationnamen gehen
    SortedMap<String, OperationInformation> opMap = new TreeMap<String, OperationInformation>();
    for (DOM dom : getDOMHierarchy()) {
      for (Operation op : dom.getOperations()) {
        if (opMap.containsKey(op.getName())) { //überschrieben
          continue;
        } else {
          if (!op.isStatic() || (includeStaticMethods && dom == this)) {
            //nicht-statische immer, statische nur, wenn sie auch inkludiert werden sollen
            opMap.put(op.getName(), new OperationInformation(this, op));
          }
        }
      }
    }
    
    return opMap.values().toArray(new OperationInformation[opMap.size()]);
  }

  public List<Operation> getOperations() {
    List<Operation> allOperations = new ArrayList<Operation>();
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      allOperations.addAll(operations);
    }
    return allOperations;
  }

  public void addOperation(int index, Operation operation) {
    String serviceName = getOriginalSimpleName();
    if (serviceNameToOperationMap.get(serviceName) == null) {
      createInitialService();
    }

    serviceNameToOperationMap.get(serviceName).add(index, operation);
  }


  public boolean removeOperation(int index) {
    String serviceName = getOriginalSimpleName();
    if (serviceNameToOperationMap.get(serviceName) == null || index < 0 || index >= serviceNameToOperationMap.get(serviceName).size()) {
      return false;
    }

    if (!isServiceGroupOnly() && serviceNameToOperationMap.get(serviceName).size() == 1) {
      serviceNameToOperationMap.remove(serviceName);
      serviceNameToServiceLabel.remove(serviceName);
    } else {
      serviceNameToOperationMap.get(serviceName).remove(index);
    }

    return true;
}


  public Map<String, List<Operation>> getServiceNameToOperationMap() {
    return serviceNameToOperationMap;
  }


  public String getServiceForOperation(String operationName) {
    if (serviceNameToOperationMap == null) {
      return null;
    }

    for (Entry<String, List<Operation>> entry : serviceNameToOperationMap.entrySet()) {
      List<Operation> operations = entry.getValue();
      for (Operation operation : operations) {
        if (Objects.equals(operation.getName(), operationName)) {
          return entry.getKey();
        }
      }
    }

    return null;
  }


  public String getLabelOfService(String serviceName) {
    return serviceNameToServiceLabel.get(serviceName);
  }


  public DOM getSuperClassGenerationObject() {
    return superClassDom;
  }

  @Override
  public boolean hasSuperClassGenerationObject() {
    return superClassDom != null;
  }

  public Set<String> getImports() {

    Set<String> imports = getBasicImports();
    
    for (AVariable v : memberVars) {
      if (!v.isJavaBaseType()) {
        if (v.getFQClassName() == null) {
          throw new RuntimeException("variable " + v.getVarName() + " in type " + getFqClassName()
              + " has not specified its type."); //sollte beim parsen auftauchen!
        }
        imports.add(v.getFQClassName());
      }
      if (v instanceof ExceptionVariable) {
        imports.add(XynaExceptionBase.class.getName());
      }
    }
    for (OperationInformation operation : collectOperationsOfDOMHierarchy(true)) {
      if (operation.getOperation().isStatic() && !operation.isImplementedHere()) {
        //benötigt keine imports lokal
        continue;
      }
      if (!operation.getOperation().isStatic() && operation.isAbstractInBaseType() && !operation.isImplementedHere()) {
        //imports von geerbten methoden sind notwendig für die super-aufruf-proxys
        continue;
      }
      operation.getOperation().getImports(imports);
    }
    if (superClassDom != null) {
      if (!getPackageNameFromFQName(superClassDom.getFqClassName()).equals(getPackageNameFromFQName(getFqClassName()))) {
        imports.add(superClassDom.getFqClassName());
      }
    }
    
    if (getPathMapInformation() != null) {
      imports.add(Map.class.getName());
      imports.add(HashMap.class.getName());
      imports.add(DataModelInformation.class.getName());
    }
    
    imports.remove(getFqClassName());
    imports.remove(null); //sollte eigtl nicht enthalten sein, aber dies ist nicht die richtige stelle, deshalb einen fehler zu werfen

    Set<String> sortedImports = new TreeSet<>(imports);
    return sortedImports;

  }
  
  public static Set<String> getBasicImports() {
    HashSet<String> imports = new HashSet<String>();
    imports.add(XynaProcessing.class.getName());
    imports.add(XynaOrderServerExtension.class.getName());
    imports.add(XynaException.class.getName());
    imports.add(XynaExceptionBase.class.getName());
    imports.add(InvalidObjectPathException.class.getName());
    imports.add(Container.class.getName());
    imports.add(XynaObject.class.getName());
    imports.add(XOUtils.class.getName());
    imports.add(GeneralXynaObject.class.getName());
    imports.add(XynaObjectList.class.getName());
    imports.add(GeneralXynaObjectList.class.getName());
    imports.add(DestinationKey.class.getName());
    imports.add(ArrayList.class.getName());
    imports.add(List.class.getName());
    imports.add(HashSet.class.getName());
    imports.add(Collections.class.getName());
    imports.add(Set.class.getName());
    imports.add(Arrays.class.getName());
    imports.add(getNameForImport(DeploymentTask.class));
    imports.add(getNameForImport(ExtendedDeploymentTask.class));
    imports.add(getNameForImport(BehaviorAfterOnUnDeploymentTimeout.class));
    imports.add(XMLHelper.class.getCanonicalName());
    imports.add(CentralFactoryLogging.class.getName());
    imports.add(Logger.class.getName());
    imports.add(XDEV_PARAMETER_NAME_NOT_FOUND.class.getName());
    imports.add(XynaObjectAnnotation.class.getName());
    imports.add(LabelAnnotation.class.getName());
    imports.add(ServiceStepEventSource.class.getName());
    imports.add(ServiceStepEventHandling.class.getName());

    imports.add(InvocationTargetException.class.getName());
    imports.add(SecurityException.class.getName());
    imports.add(NoSuchMethodException.class.getName());
    imports.add(IllegalArgumentException.class.getName());
    imports.add(IllegalAccessException.class.getName());
    imports.add(XPRC_MDMDeploymentException.class.getName());
    
    //serialisierung
    imports.add(IOException.class.getName());
    imports.add(ClassNotFoundException.class.getName());
    imports.add(ObjectInputStream.class.getName());
    imports.add(ObjectOutputStream.class.getName());
    imports.add(SerializableClassloadedException.class.getName());
    imports.add(SerializableClassloadedXynaObject.class.getName());
    imports.add(SerializableClassloadedObject.class.getName());
    
    //reflection field cache
    imports.add(Class.class.getName());
    imports.add(Field.class.getName());
    imports.add(ConcurrentMap.class.getName());
    imports.add(ConcurrentHashMap.class.getName());
    imports.add(NoSuchFieldException.class.getName());
    
    
    //runtimeContext für audits
    imports.add(RevisionManagement.class.getName());
    return imports;
  }
  
  public static String getNameForImport(Class<?> class1) {
    return class1.getCanonicalName();
  }

  public Operation getOperationByName(String operationName) throws XPRC_OperationUnknownException {
    return getOperationByName(operationName, false);
  }

  //FIXME mehrere operations mit dem gleichen namen unterstützen
  public Operation getOperationByName(String operationName, boolean includeParents) throws XPRC_OperationUnknownException {
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      for (Operation o : operations) {
        if (o.getName().equals(operationName)) {
          return o;
        }
      }
    }

    if (includeParents && getSuperClassGenerationObject() != null) {
      return getSuperClassGenerationObject().getOperationByName(operationName, includeParents);
    }

    throw new XPRC_OperationUnknownException(operationName);
  }


  @Override
  public List<AVariable> getMemberVars() {
    return Collections.unmodifiableList(memberVars);
  }


  @Override
  public boolean replaceMemberVar(AVariable oldVar, AVariable newVar) {
    int index = memberVars.indexOf(oldVar);
    if (index < 0) {
      return false;
    }

    memberVars.set(index, newVar);

    return true;
  }


  @Override
  public void addMemberVar(int index, AVariable var) {
    memberVars.add(index, var);
  }


  @Override
  public boolean removeMemberVar(AVariable var) {
    return memberVars.remove(var);
  }


  @Override
  public AVariable removeMemberVar(int index) {
    return memberVars.remove(index);
  }


  public boolean replaceOperation(Operation oldOperation, Operation newOperation) {
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      for (int operationIdx = 0; operationIdx < operations.size(); operationIdx++) {
        Operation operation = operations.get(operationIdx);
        if (operation == oldOperation) {
          operations.set(operationIdx, newOperation);
          return true;
        }
      }
    }

    return false;
  }


  public List<AVariable> getAllMemberVarsIncludingInherited() {
    DOM d = superClassDom;
    List<List<AVariable>> allMemberVars = new ArrayList<List<AVariable>>();
    allMemberVars.add(getMemberVars());
    while (d != null) {
      List<AVariable> dml = d.getMemberVars();
      allMemberVars.add(dml);
      d = d.superClassDom;
    }

    List<AVariable> allMemberVars2 = new ArrayList<AVariable>();
    for (int i = allMemberVars.size() - 1; i >= 0; i--) {
      for (AVariable v : allMemberVars.get(i)) {
        allMemberVars2.add(v);
      }
    }
    return allMemberVars2;
  }


  /**
   * vorher sollte man parse aufgerufen haben
   */
  public TemplateGenerationResult generateServiceImplTemplate() {
    
    /*
     * java-/class-files befinden sich wo?
     * 
     *  MDM         -        Interfaces.jar      -         ProjectImpl         -       Interfaces.jar
     *                                                     
     * --------------------------------------------------------------------------------------------------
     * Usage:                                                    
     * Datatype  --calls-->  DelegationImpl  --calls-->        StaticImpl
     *                                                  and/or InstanceImpl  extends    SuperProxy
     *                                                               \
     *                                                                      \
     *                                                                     implements
     *                                                                              \
     *                                                                                     v
     *                                                                                StaticInterface
     *                                                                             or NonStaticInterface
     */
    List<Pair<String, String>> filesForGeneratedAdditionalLib = new ArrayList<Pair<String,String>>();
    List<Pair<String, String>> dependencies = new ArrayList<Pair<String,String>>();
    List<Pair<String, String>> templateImplementationFiles = new ArrayList<Pair<String,String>>();    

    InterfaceVersion[] versions = getVersionsOfOperations(true);
    JavaServiceImplementation impl = new JavaServiceImplementation(this, null);
    String delegationImplCode = impl.createDelegationImplCode();
    String superDelegationImplCode = impl.createSuperDelegationImplCode();

    filesForGeneratedAdditionalLib.add(new Pair<String, String>(impl.getDelegationImplFQClassName(), delegationImplCode));
    if (superDelegationImplCode != null) {
      dependencies.add(new Pair<String, String>(impl.getSuperDelegationImplFQClassName(), superDelegationImplCode));
    }

    for (InterfaceVersion version : versions) {
      impl = new JavaServiceImplementation(this, version);
      String projectStaticImplCode = impl.createProjectStaticImplCode();
      String projectNonStaticImplCode = impl.createProjectNonStaticImplCode();
      String superProxyCode = impl.createSuperProxyCode();
      String interfaceStaticCode = impl.createInterfaceStaticCode();
      String interfaceNonStaticCode = impl.createInterfaceNonStaticCode();
      String superProjectNonStaticImplCode = impl.createSuperProjectNonStaticImplCode();

      if (superProxyCode != null) {
        filesForGeneratedAdditionalLib.add(new Pair<String, String>(impl.getSuperProxyFQClassName(), superProxyCode));
      }
      if (interfaceStaticCode != null) {
        filesForGeneratedAdditionalLib.add(new Pair<String, String>(impl.getInterfaceStaticFQClassName(), interfaceStaticCode));
      }
      if (interfaceNonStaticCode != null) {
        filesForGeneratedAdditionalLib.add(new Pair<String, String>(impl.getInterfaceNonStaticFQClassName(), interfaceNonStaticCode));
      }
      
      if (projectStaticImplCode != null) {
        templateImplementationFiles.add(new Pair<String, String>(impl.getProjectStaticImplFQClassName(version), projectStaticImplCode));  
      }
      if (projectNonStaticImplCode != null) {
        templateImplementationFiles.add(new Pair<String, String>(impl.getProjectNonStaticImplFQClassName(version), projectNonStaticImplCode));  
      }
      
      if (superProjectNonStaticImplCode != null) {
        dependencies.add(new Pair<String, String>(impl.getSuperProjectNonStaticImplFQClassName(version), superProjectNonStaticImplCode));
      }
    }
    
    return new TemplateGenerationResult(filesForGeneratedAdditionalLib, templateImplementationFiles, dependencies);
  }
  
  public static class InterfaceVersion {

    private static final Pattern p = Pattern.compile("[\\w_]+");
    public static final InterfaceVersion BASE = new InterfaceVersion();
    private static final String BASE_VERSION_NAME = "_internal_base_version";
    
    private final String versionName;
    private final String suffix;
    private final String pkgName;
    private final boolean currentVersion;
    
    private InterfaceVersion() {
      this.versionName = BASE_VERSION_NAME;
      this.suffix = "";
      this.pkgName = BASE_VERSION_NAME;
      this.currentVersion = true;
    }
    
    public InterfaceVersion(String versionName, boolean currentVersion) {
      this.versionName = versionName;
      Matcher m = p.matcher(versionName);
      if (!m.matches()) {
        throw new RuntimeException("Version contains invalid characters: " + versionName);
      }
      this.suffix = "_" + versionName;
      this.pkgName = versionName;
      this.currentVersion = currentVersion;
    }

    public String getSuffix() {
      return suffix;
    }

    public String getPackageName() {
      return pkgName;
    }


    @Override
    public int hashCode() {
      return Objects.hash(versionName);
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      InterfaceVersion other = (InterfaceVersion) obj;
      return Objects.equals(versionName, other.versionName);
    }

    public boolean isCurrentVersion() {
      return currentVersion;
    }

    public String getName() {
      return versionName;
    }

    public String getNameCompatibleWithCurrentVersion() {
      if (currentVersion) {
        return BASE_VERSION_NAME;
      }
      return versionName;
    }

  }


  public InterfaceVersion[] getVersionsOfOperations(boolean onlyOneCurrentVersion) {
    Set<InterfaceVersion> versions = new HashSet<DOM.InterfaceVersion>();
    versions.add(InterfaceVersion.BASE); //base immer, damit es auch ohne modellierte methoden möglich ist, code zu schreiben
    boolean containsCurrentVersion = false;
    for (Operation op : getOperations()) {
      if (onlyOneCurrentVersion && op.getVersion().isCurrentVersion()) {
        if (containsCurrentVersion) {
          continue;
        }
        containsCurrentVersion = true;
      }
      versions.add(op.getVersion());
    }
    return versions.toArray(new InterfaceVersion[versions.size()]);
  }

  public final String getImplClassName() {
    return getSimpleClassName() + "Impl";
  }


  //FIXME soll das wirklich festgelegt werden, dass impl klassen so heissen müssen. abgesehen von der template-generierung ist das die einzige stelle wo man 
  // sich darauf verlässt. dann kann man auch die code-snippets verstecken und sagen, dass man die eh immer generiert.
  public final String getImplFqClassName() {
    return getFqClassName() + "Impl";
  }


  @Override
  protected void fillVarsInternally() throws XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    HashSet<AVariable> vars = new HashSet<AVariable>();
    vars.addAll(memberVars);
    for (List<Operation> operations : serviceNameToOperationMap.values()) {
      for (Operation op : operations) {
        if (op instanceof JavaOperation) {
          JavaOperation jop = (JavaOperation) op;
          vars.addAll(jop.getInputVars());
          vars.addAll(jop.getOutputVars());
        }
      }
    }
    for (AVariable v : vars) {
      v.fillVariableContents();
    }
    
    //persistenceinformation fertig befüllen falls nötig. kann in parsexml nicht gemacht werden, weil noch das geparsten super-objekt fehlt.
    PrimitiveType pktype = PersistenceInformation.detectPrimaryKeyType(getAllMemberVarsIncludingInherited());
    if (pktype != null) {
      if (persistenceInformation == PersistenceInformation.EMPTY) {
        persistenceInformation = PersistenceInformation.forSuperType(pktype);
      } else {
        persistenceInformation.setPkType(pktype);
      }
    }
  }


  protected void setAdditionalDependencies(AdditionalDependencyContainer container) {
    this.additionalDependencies = container;
  }


  public AdditionalDependencyContainer getAdditionalDependencies() {
    return additionalDependencies;
  }


  public Set<GenerationBase> getAdditionalDependenciesSet() {
    return additionalDependenciesSet;
  }

  /**
   * Add/Remove library tags to/from XML and include calls to impl class
   */
  public static void addLibraryTagAndCodeSnippetInXML(Document doc, DOM dom, boolean libraryTagOnly, boolean adjustDomAccordingly)
      throws XPRC_InvalidPackageNameException {
    
    Element root = doc.getDocumentElement();

    List<Element> ss = XMLUtils.getChildElementsByName(root, GenerationBase.EL.SERVICE);
    if (ss.size() == 0) {
      // not expected to happen since that is checked on first deployment when performing an xsd check

      // kann aber passieren, wenn oberklasse methoden hat, und man im impl nur interne methoden überschreiben möchte
      if (!dom.hasSuperTypeWithJavaImpl(true, null)) {
        throw new RuntimeException("invalid xml of datatype " + dom.getOriginalFqName() + ". expected at least one child of "
            + GenerationBase.EL.SERVICE);
      }
    }

    String serviceImplString = dom.getImplClassName() + ".jar";
    if (!dom.getAdditionalLibraries().contains(serviceImplString)) {
      List<Element> libs = XMLUtils.getChildElementsByName(root, GenerationBase.EL.LIBRARIES);
      Element lib = doc.createElement(GenerationBase.EL.LIBRARIES);
      XMLUtils.setTextContent(lib, serviceImplString);
      if (libs == null || libs.size() == 0) {
        if (ss.size() == 0) {
          root.appendChild(lib);
        } else {
          root.insertBefore(lib, ss.get(0));
        }
      } else {
        root.insertBefore(lib, libs.get(0));
      }
      if (adjustDomAccordingly) {
        dom.getAdditionalLibraries().add(serviceImplString);
      }
    }

    if (libraryTagOnly) {
      return;
    }

    for (Element s : ss) {
      List<Element> ops = XMLUtils.getChildElementsByName(s, GenerationBase.EL.OPERATION);
      for (Element op : ops) {
        Element sourceCode = XMLUtils.getChildElementByName(op, GenerationBase.EL.SOURCECODE);
        if (sourceCode != null) {

          Element snippet = XMLUtils.getChildElementByName(sourceCode, GenerationBase.EL.CODESNIPPET);

          JavaOperation jop = null;
          if (dom.getOperations() == null) {
            throw new RuntimeException("Operations of DOM have not been parsed.");
          }
          for (Operation dop : dom.getOperations()) {
            if (dop instanceof JavaOperation) {
              jop = (JavaOperation) dop;
              if (jop.getName().equals(op.getAttribute(GenerationBase.ATT.OPERATION_NAME))) {
                break;
              }
            }
          }

          String implString = jop.createImplCallSnippet(false, adjustDomAccordingly);
          XMLUtils.setTextContent(snippet, implString);
        }
      }
    }
  }
  
  
  public static class TemplateGenerationResult {
    
    private final List<Pair<String, String>> filesForGeneratedAdditionalLib;
    private final List<Pair<String, String>> templateImplementationFiles;
    private final List<Pair<String, String>> dependencies;


    TemplateGenerationResult(List<Pair<String, String>> filesForGeneratedAdditionalLib,
                             List<Pair<String, String>> templateImplementationFiles,
                             List<Pair<String, String>> dependencies) {
      this.filesForGeneratedAdditionalLib = filesForGeneratedAdditionalLib;
      this.templateImplementationFiles = templateImplementationFiles;
      this.dependencies = dependencies;
    }


    /**
     * die files, die im eclipse project im servicedefintion.jar liegen
     */
    public List<Pair<String, String>> getFilesForGeneratedAdditionalLib() {
      return filesForGeneratedAdditionalLib;
    }
    
    /**
     * die files, die im eclipse projekt implementiert werden müssen 
     */
    public List<Pair<String, String>> getTemplateImplementationFiles() {
      return templateImplementationFiles;
    }

    /**
     * fürs compile notwendige oberklasse-files, die man nicht aus den deploy-ten jars verwenden will.
     * problemfall ist z.b., dass in der oberklasse abstrakte methoden in der implklasse sind, die
     * der server nicht kennt
     */
    public  List<Pair<String, String>>  getDependencies() {
      return dependencies;
    }
    
  }

  public PersistenceInformation getPersistenceInformation() {
    return persistenceInformation;
  }


  /**
   * sucht alle xmom storables, die in ihrer membervariablen-hierarchie diesen typ enthalten. <br>
   * diese müssen nämlich auch neu deployed werden, wenn this sich ändert (weil die storable-klassen sich ändern müssen).
   */
  public Set<GenerationBase> getRootXMOMStorablesUsingThis(GenerationBaseCache parseAdditionalCache, Set<GenerationBase> visited) {
    //dependencyregister bemühen. 

    Set<GenerationBase> ret = new HashSet<GenerationBase>();
    
    if (isInheritedFromStorable()) {
      ret.add(this);
    }

    //FIXME performance!!!! eigtl wäre es hier schön, wenn man die xmomdatabase verwenden könnte. man benötigt aber die informationen aus dem deployed ordner
    Set<DependencyNode> dependencies =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencies(getOriginalFqName(), DependencySourceType.DATATYPE, revision, true);

    for (DependencyNode dn : dependencies) {
      if (dn.getType() == DependencySourceType.DATATYPE && !dn.getUniqueName().equals(getOriginalFqName())) {
        DOM dom;
        try {
          //global cached funktioniert hier nicht gut, weil der deploymentmode dann falsch gesetzt wird
          dom = DOM.getOrCreateInstance(dn.getUniqueName(), parseAdditionalCache, dn.getRevision());
          dom.parseGeneration(true, false, false);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        } catch (XPRC_InheritedConcurrentDeploymentException e) {
          throw new RuntimeException(e);
        } catch (AssumedDeadlockException e) {
          throw new RuntimeException(e);
        } catch (XPRC_MDMDeploymentException e) {
          throw new RuntimeException(e);
        }
        
        if (dom.isInheritedFromStorable() &&
            dom.usesInStorableHierarchy(this)) {
          ret.add(dom);
        }
      }
    }
    
    visited.add(this);
    
    Set<GenerationBase> subTypes = getSubTypes(parseAdditionalCache, false);
    for (GenerationBase subType : subTypes) {
      if (visited.add(subType)) {
        if (subType instanceof DOM) {
          ret.addAll(((DOM)subType).getRootXMOMStorablesUsingThis(parseAdditionalCache, visited));
        }
      }
    }
    if (isInheritedFromStorable()) {
      ret.addAll(subTypes);
    }
    
    if (superClassDom != null &&
        !superClassDom.isStorableEquivalent()) {
      if (visited.add(superClassDom)) {
        ret.addAll(superClassDom.getRootXMOMStorablesUsingThis(parseAdditionalCache, visited));
      }
      if (isInheritedFromStorable()) {
        ret.add(superClassDom);
      }
    }

    return ret;
  }
  
  /**
   * wenn man alle membervariablen von this rekursiv betrachtet (bis jeweils zu referenzierten anderen storablen 
   * oder transienten variablen), wird dort das übergebene dom irgendwo verwendet??
   * berücksichtigt nicht, dass z.b. das dom in einer additionaldependency eines verwendeten datentyps steht oder
   * unterhalb einer evtl im blob gespeicherten exception verwendet wird. 
   */
  private boolean usesInStorableHierarchy(DOM dom) {
    return usesInStorableHierarchyRecursively(this, this, dom, "");
  }


  private static boolean usesInStorableHierarchyRecursively(DOM xmomStorableRoot, DOM currentDOM, DOM domToCheck,
                                                            String path) {
    for (AVariable v : currentDOM.getAllMemberVarsIncludingInherited()) {
      if (!v.isJavaBaseType() && v.getDomOrExceptionObject() instanceof DOM) {
        String localPath = path;
        if (path.length() > 0) {
          localPath += ".";
        }
        localPath += v.getVarName();
        if (XMOMStorableStructureCache.isTransient(xmomStorableRoot, v, localPath)) {
          //ok next
        } else if (v.getDomOrExceptionObject().getFqClassName().equals(domToCheck.getFqClassName())) {
          return true;
        /*} else if (isStorableReference(xmomStorableRoot, localPath)) {
          //ok next*/
        } else if (usesInStorableHierarchyRecursively(xmomStorableRoot, (DOM) v.getDomOrExceptionObject(), domToCheck,
                                                      localPath)) {
          return true;
        }
      }
    }
    return false;
  }


  @Override
  protected String getHumanReadableTypeName() {
    return GenerationBase.EL.DATATYPE;
  }

  public DataModelInformation getDataModelInformation() {
    return dataModelInformation;
  }
  
  public PathMapInformation getPathMapInformation() {
    return pathMapInformation;
  }

  public String getClassName(Set<String> importedClassNames) {
    if (importedClassNames.contains(getFqClassName())) {
      return getSimpleClassName();
    }
    return getFqClassName();
  }

  public void replaceMemberVars(List<AVariable> newMembers) {
    this.memberVars.clear();
    this.memberVars.addAll(newMembers);
  }

  public void replaceParent(DOM dom) {
    this.superClassDom = dom;
  }
  
  public static Method getPublicCloneMethodIfPresent(Class<?> clazz) {
    try {
      Method method = clazz.getDeclaredMethod("clone");
      if (method != null &&
          Modifier.isPublic(method.getModifiers())) {
        return method;
      } else {
        return null;
      }
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }

  public String getServiceName(Operation operation) {
    for (String curServiceName : serviceNameToOperationMap.keySet()) {
      for (Operation curOperation : serviceNameToOperationMap.get(curServiceName)) {
        if (curOperation == operation) {
          return curServiceName;
        }
      }
    }
    
    return null;
  }

  @Override
  protected DependencySourceType getDependencySourceType() {
    return DependencySourceType.DATATYPE;
  }

  public void createEmptyDT(String label) {
    setLabel(label);
  }

  public void createEmptySG(String label) {
    setLabel(label);
    createInitialService();
  }

  private void createInitialService() {
    serviceNameToOperationMap.put(getOriginalSimpleName(), new ArrayList<Operation>());
    serviceNameToServiceLabel.put(getOriginalSimpleName(), getLabel());
  }

  public boolean isServiceGroupOnly() {
    if (isServiceGroupOnly != null) {
      // prefer previously stored information (for instance from parsing)
      return isServiceGroupOnly;
    }

    // no previously stored information concerning whether DOM is serviceGroupOnly is present -> determine based on operations and member variables

    // when no service-tag is present the DOM is not serviceGroupOnly
    if (serviceNameToOperationMap.keySet().size() == 0) {
      isServiceGroupOnly = false;
      return isServiceGroupOnly;
    }

    List<Operation> operations = getOperations();
    for (Operation operation : operations) {
      // if there is at least one dynamic method, the DOM is not serviceGroupOnly
      if (!operation.isStatic()) {
        isServiceGroupOnly = false;
        return isServiceGroupOnly;
      }
    }

    // DOM has no dynamic methods. If there are also member vars, it's serviceGroupOnly.
    isServiceGroupOnly = (memberVars.size() == 0);
    return isServiceGroupOnly;
  }

}
