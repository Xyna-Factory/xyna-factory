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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.ToolProvider;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna.XMOM.base.IPv6;
import com.gip.xyna.XMOM.base.net.exception.AddressNoNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv4FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv4ValidationException;
import com.gip.xyna.XMOM.base.net.exception.IPv6FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv6ValidationException;
import com.gip.xyna.XMOM.base.net.exception.IllegalNetmaskLengthException;
import com.gip.xyna.XMOM.base.net.exception.MACAddressValidationException;
import com.gip.xyna.XMOM.base.net.exception.MaxListOfIPsExceededException;
import com.gip.xyna.XMOM.base.net.exception.NetworkNotMatchesNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.NoFreeIPFoundException;
import com.gip.xyna.XMOM.base.net.exception.PortValidationException;
import com.gip.xyna.XMOM.base.net.exception.VLANIDValidationException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.Combinatorics;
import com.gip.xyna.utils.Combinatorics.CombinationHandler;
import com.gip.xyna.utils.FolderCopyWithBackup;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.collections.WeakIdentityHashMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidClassNameException;
import com.gip.xyna.utils.exceptions.xmlstorage.InvalidValuesInXMLException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMDeleteEvent;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentTransition;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.radius.PresharedKey;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSConnectionConfig;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserName;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_BackupFileException;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentCleanupException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateVariableNamesException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE;
import com.gip.xyna.xprc.exceptions.XPRC_InconsistentFileNameAndContentException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionXmlInvalidBaseReferenceException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMethodAbstractAndStaticException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMParallelDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MayNotOverrideFinalOperationException;
import com.gip.xyna.xprc.exceptions.XPRC_MdmDeploymentCyclicInheritanceException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.exceptions.XPRC_SimultanuousUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_UndeploymentDuringDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.exceptions.XPRC_WrongDeploymentTypeException;
import com.gip.xyna.xprc.exceptions.XPRC_XMOMObjectDoesNotExist;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.compile.Compilation;
import com.gip.xyna.xprc.xfractwfe.generation.compile.CompilationResult;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.Veto;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall;


/**
 * oberklasse von WF, DOM und ExceptionGeneration.
 *
 * generationbase objekte werden beim deployment und undeployment von
 * mdm objekten benutzt. die objekte kennen die struktur, die im xml angegeben ist und wissen, wie sie den zugehörigen
 * java code erstellen. ausserdem kennen sie die abhängigkeiten zu anderen generationbase objekten.
 *
 * weil im dependency-tree referenzen mehrfach vorkommen können, die objekte aber nicht mehrfach geparst etc werden müssen,
 * gibt es eine singleton-struktur aller generationbase objekte. man erstellt ein neues objekt zb mit
 * DOM.getInstance(name) und erhält ggfs ein objekt aus dem cache, welches bereits geparst ist. dann muss
 * das parsen nicht erneut ausgeführt werden.
 *
 * damit operationen wie zb "parse" nicht mehrfach ausgeführt werden, gibt es einen state, der beschreibt, welche
 * operationen bereits ausgeführt wurden.
 */
public abstract class GenerationBase {

  private static final Logger logger = CentralFactoryLogging.getLogger(GenerationBase.class);


  public static final String DEFAULT_PACKAGE = "defaultpackage";

  public static final String ANYTYPE_REFERENCE_PATH = "base";
  public static final String ANYTYPE_REFERENCE_NAME = "AnyType";
  public static final String CORE_EXCEPTION_REFERENCE_PATH = "core.exception";
  public static final String CORE_EXCEPTION_REFERENCE_NAME = "Exception";
  public static final String DEFAULT_EXCEPTION_REFERENCE_PATH = "core.exception";
  public static final String DEFAULT_EXCEPTION_REFERENCE_NAME = "XynaException";
  public static final String DEFAULT_EXCEPTION_BASE_REFERENCE_PATH = DEFAULT_EXCEPTION_REFERENCE_PATH;
  public static final String DEFAULT_EXCEPTION_BASE_REFERENCE_NAME = "XynaExceptionBase";

  public static final String CORE_EXCEPTION = "core.exception.Exception";
  public static final String CORE_XYNAEXCEPTION = DEFAULT_EXCEPTION_REFERENCE_PATH + "." + DEFAULT_EXCEPTION_REFERENCE_NAME;
  public static final String CORE_XYNAEXCEPTIONBASE = DEFAULT_EXCEPTION_BASE_REFERENCE_PATH + "." + DEFAULT_EXCEPTION_BASE_REFERENCE_NAME;
  public static final String SCHEDULERBEAN = "xprc.SchedulerBean";
  public static final String CAPACITY = "xprc.Capacity";
  public static final String VETO = "xprc.Veto";

  public static final String PORT = "xfmg.xopctrl.radius.RADIUSServerPort";
  public static final String IP = "base.IP";
  public static final String IPv4 = "base.IPv4";
  public static final String IPv6 = "base.IPv6";
  public static final String AUTHENTICATION_RESULT = "xfmg.xopctrl.AuthenticationResult";
  public static final String USER_NAME = "xfmg.xopctrl.UserName";
  public static final String DOMAIN_NAME = "xfmg.xopctrl.DomainName";
  public static final String RADIUS_CONNECTION_CONFIG = "xfmg.xopctrl.radius.RADIUSConnectionConfig";
  public static final String PRESHARED_KEY = "xfmg.xopctrl.radius.PresharedKey";
  public static final String SSL_PARAMETER = "xact.ldap.SSLParameter";
  public static final String SSL_KEYSTORE_PARAMETER = "xact.ldap.SSLKeystoreParameter";
  public static final String SSL_KEY_AND_TRUSTSTORE_PARAMETER = "xact.ldap.SSLKeyAndTruststoreParameter";
  public static final String SSL_TRUST_EVERYONE = "xact.ldap.TrustEveryone";
  public static final String FORMAT_EXCEPTION = "base.net.exception.FormatException";
  public static final String VALIDATION_EXCEPTION = "base.net.exception.ValidationException";
  public static final String ADDRESS_NO_NETMASK_EXCEPTION = "base.net.exception.AddressNoNetmaskException";
  public static final String ILLEGAL_NETMASK_LENGTH_EXCEPTION = "base.net.exception.IllegalNetmaskLengthException";
  public static final String IPV4_FORMAT_EXCEPTION = "base.net.exception.IPv4FormatException";
  public static final String IPV4_VALIDATION_EXCEPTION = "base.net.exception.IPv4ValidationException";
  public static final String NETWORK_NOT_MATCHES_NETMASK_EXCEPTION = "base.net.exception.NetworkNotMatchesNetmaskException";
  public static final String IPV6_FORMAT_EXCEPTION = "base.net.exception.IPv6FormatException";
  public static final String IPV6_VALIDATION_EXCEPTION = "base.net.exception.IPv6ValidationException";
  public static final String MAX_LIST_OF_IPS_EXCEEDED_EXCEPTION = "base.net.exception.MaxListOfIPsExceededException";
  public static final String NO_FREE_IP_FOUND_EXCEPTION = "base.net.exception.NoFreeIPFoundException";
  public static final String PORT_VALIDATION_EXCEPTION = "base.net.exception.PortValidationException";
  public static final String MAC_ADDRESS_VALIDATION_EXCEPTION = "base.net.exception.MACAddressValidationException";
  public static final String VLAN_ID_VALIDATION_EXCEPTION = "base.net.exception.VLANIDValidationException";

  public static final String COPYRIGHT_HEADER = "<!--\n" + 
      " * - - - - - - - - - - - - - - - - - - - - - - - - - -\n" + 
      " * Copyright 2022 GIP SmartMercial GmbH, Germany\n" + 
      " *\n" +
      " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      " * you may not use this file except in compliance with the License.\n" +
      " * You may obtain a copy of the License at\n" +
      " *\n" +
      " * http://www.apache.org/licenses/LICENSE-2.0\n" +
      " *\n" +
      " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      " * See the License for the specific language governing permissions and\n" +
      " * limitations under the License.\n" +
      " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n" +
      " -->";

  
  private static final String pathSeparator = Constants.PATH_SEPARATOR;
  
  /**
   * bestimmt die reihenfolge, in der dependencies ermittelt werden
   * TODO: bisher muss jede GenerationBase Implementierung das freiwillig verwenden, besser wäre, wenn das automatisch geschehen würde
   *
   * die feste reihenfolge ist für die deadlock vermeidung wichtig.
   * wie die reihenfolge tatsächlich bestimmt ist, ist für die meisten statusübergänge egal. nur beim compile
   * bietet es sich an, mit den workflows zu beginnen.
   * deshalb ist der comparator so gebaut, dass erst alle WFs kommen, dann alle DOMs und dann die Exceptions.
   */
  public static final Comparator<GenerationBase> DEPENDENCIES_COMPARATOR = new Comparator<GenerationBase>() {

    private static final int C_WF = 1;
    private static final int C_DOM = 2;
    private static final int C_EX = 3;

    private int getC(GenerationBase gb) {
      if (gb instanceof WF) {
        return C_WF;
      }
      if (gb instanceof DOM) {
        return C_DOM;
      }
      return C_EX;
    }

    public int compare(GenerationBase o1, GenerationBase o2) {      
      Long r1 = o1.revision;
      Long r2 = o2.revision;
      if (r1 == null) {
        r1 = -4L;
      }
      if (r2 == null) {
        r2 = -4L;
      }
      
      if (r1 < r2) {
        return 1;
      } else if (r1 > r2) {
        return -1;
      }
      //gleiche revision
      int c1 = getC(o1);
      int c2 = getC(o2);
      if (c1 == c2) {
        return o2.getFqClassName().compareTo(o1.getFqClassName());
      } else {
        return c1 - c2;
      }
    }

  };


  /**
   * für diese objekte wird
   *  - kein java code generiert und
   *  - kein compile ausgeführt.
   *  - die XSD-Validierung gegen die Datei im "saved"-Verzeichnis ausgeführt.
   * ausserdem können sie nicht undeployed werden.
   *
   * alle andere deployment schritte werden durchgeführt
   */
  private static final BijectiveMap<String, Class<?>> mdmObjectMappingToJavaClasses = new BijectiveMap<String, Class<?>>();
  static {
    mdmObjectMappingToJavaClasses.put(CORE_EXCEPTION, Exception.class); // Base
    mdmObjectMappingToJavaClasses.put(CORE_XYNAEXCEPTION, XynaException.class); // Base
    mdmObjectMappingToJavaClasses.put(CORE_XYNAEXCEPTIONBASE, XynaExceptionBase.class); // Base
    mdmObjectMappingToJavaClasses.put(DatatypeVariable.ANY_TYPE, GeneralXynaObject.class);
    mdmObjectMappingToJavaClasses.put(SCHEDULERBEAN, SchedulerBean.class); // Processing
    mdmObjectMappingToJavaClasses.put(CAPACITY, Capacity.class); // Processing
    mdmObjectMappingToJavaClasses.put(VETO, Veto.class);  // Processing
    mdmObjectMappingToJavaClasses.put(PORT, RADIUSServerPort.class); // Radius
    mdmObjectMappingToJavaClasses.put(IP, IP.class); // Base
    mdmObjectMappingToJavaClasses.put(IPv4, IPv4.class); // Base
    mdmObjectMappingToJavaClasses.put(IPv6, IPv6.class); // Base
    mdmObjectMappingToJavaClasses.put(AUTHENTICATION_RESULT, AuthenticationResult.class); // User
    mdmObjectMappingToJavaClasses.put(USER_NAME, UserName.class); // User
    mdmObjectMappingToJavaClasses.put(DOMAIN_NAME, DomainName.class); // User
    mdmObjectMappingToJavaClasses.put(PRESHARED_KEY, PresharedKey.class); // Radius
    mdmObjectMappingToJavaClasses.put(RADIUS_CONNECTION_CONFIG, RADIUSConnectionConfig.class); // Radius
    mdmObjectMappingToJavaClasses.put(FORMAT_EXCEPTION, FormatException.class); // Net
    mdmObjectMappingToJavaClasses.put(VALIDATION_EXCEPTION, ValidationException.class); // Net
    mdmObjectMappingToJavaClasses.put(ADDRESS_NO_NETMASK_EXCEPTION, AddressNoNetmaskException.class); // Net
    mdmObjectMappingToJavaClasses.put(ILLEGAL_NETMASK_LENGTH_EXCEPTION, IllegalNetmaskLengthException.class); // Net
    mdmObjectMappingToJavaClasses.put(IPV4_FORMAT_EXCEPTION, IPv4FormatException.class); // Net
    mdmObjectMappingToJavaClasses.put(IPV4_VALIDATION_EXCEPTION, IPv4ValidationException.class); // Net
    mdmObjectMappingToJavaClasses.put(NETWORK_NOT_MATCHES_NETMASK_EXCEPTION, NetworkNotMatchesNetmaskException.class); // Net
    mdmObjectMappingToJavaClasses.put(IPV6_FORMAT_EXCEPTION, IPv6FormatException.class); // Net
    mdmObjectMappingToJavaClasses.put(IPV6_VALIDATION_EXCEPTION, IPv6ValidationException.class); // Net
    mdmObjectMappingToJavaClasses.put(MAX_LIST_OF_IPS_EXCEEDED_EXCEPTION, MaxListOfIPsExceededException.class); // Net
    mdmObjectMappingToJavaClasses.put(NO_FREE_IP_FOUND_EXCEPTION, NoFreeIPFoundException.class); // Net
    mdmObjectMappingToJavaClasses.put(PORT_VALIDATION_EXCEPTION, PortValidationException.class); // Net
    mdmObjectMappingToJavaClasses.put(MAC_ADDRESS_VALIDATION_EXCEPTION, MACAddressValidationException.class); // Net
    mdmObjectMappingToJavaClasses.put(VLAN_ID_VALIDATION_EXCEPTION, VLANIDValidationException.class); // Net
    
    //xfmg.xods.configuration.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.BehaviourIfPropertyNotSet.class ); // XynaProperty
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.CreateProperty.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.DE.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.Documentation.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.DocumentationLanguage.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.EN.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.ThrowException.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.UseValue.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaProperty.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyBoolean.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyCustomizable.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyInteger.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyLong.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyRelativeDate.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyString.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyUnreadableString.class );

    //base.date.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate.class ); // Base
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Date.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.DateFormat.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Forever.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Now.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.InvalidDateStringException.class );

    //base.net.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv4.class ); // Net
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv4Netmask.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv4Subnet.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv6.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv6Netmask.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.IPv6Subnet.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.MACAddress.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.Port.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.XMOM.base.net.VLANID.class );

    //xprc.xsched.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.TimeConstraint.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.SimpleTimeConstraint.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.WindowTimeConstraint.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Capacity.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Veto.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.Priority.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.SchedulerInformation.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.TimeConfiguration.class ); // Base
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteTimeConfiguration.class ); // Base
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.RelativeTimeConfiguration.class ); // Base

    //xfmg.xfctrl.datamodel.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType.class ); // DataModel
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific.class );

    //xact.ldap.*
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPServer.class ); // LDAP
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLParameter.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeystoreParameter.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeyAndTruststoreParameter.class );
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.TrustEveryone.class );
    
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall.class ); // Processing
    appendMapping( mdmObjectMappingToJavaClasses, com.gip.xyna.xprc.xsched.xynaobjects.RemoteCallInput.class ); // Processing
  }
  
  private static String[] fqNamesOfTypesUsedByRemoteCall = new String[] {"xprc.xpce.RuntimeContext", "xprc.xpce.AnyInputPayload"};
  
  public static String[] getFqNamesOfTypesUsedByRemoteCall() {
    return fqNamesOfTypesUsedByRemoteCall;
  }
  
  static void appendMapping( Map<String, Class<?>> map, Class<? extends GeneralXynaObject> clazz)  {
    XynaObjectAnnotation sxo = clazz.getAnnotation(XynaObjectAnnotation.class);
    map.put( sxo.fqXmlName(), clazz);
  }


  /**
   * true =&gt; beim cleanup wird objekt aus cache entfernt
   * false =&gt; beim cleanup bleibt objekt in cache
   *
   * wird zb beim serverstart benutzt, damit objekte nicht redundant erzeugt/geparst werden müssen
   * 
   * ist heute nicht mehr so wichtig wie früher, weil es das gleichzeitige deployment gibt, wo die objekte eh nicht zwischendurch wieder aus dem cache entfernt weden.
   */
  public static boolean removeFromCache = true;

  /*
   * verwendung, falls man den generierten code der reserved objects neu generieren will -> true
   * siehe auch unten die auskommentierte main-methode
   */
  private static final boolean overrideReservedServerObjectsForCodeGenUpdates = false;


  //xml file existiert nicht
  private boolean doesntExist;
  
  //objekt wird auch als WF erstellt, wenn man es so will, auch wenn es in wirklichkeit ein datentyp ist - dann steht aber hier der richtige typ
  //allerdings nur, wenn das objekt mit dem richtigen typ im cache war
  private final String realType;

  private static Random random = new Random();
  private static AtomicLong backupCounter = new AtomicLong(0);

  //während kompiliert wird, sollte kein anderer thread gerade classloading betreiben, weil sonst classfiles unvollständig sein
  //könnten. genauso sind zwei gleichzeitig stattfindende compiles auch nicht gut.
  //gleichzeitige classloading geschichten sind ok.
  // => readlock = classloading. writelock = compile (macht aus file-sicht auch sinn)
  private static final ReentrantReadWriteLock classLock = new ReentrantReadWriteLock();

  //löschen von verzeichnissen, compile und erstellen von java-dateien muss synchronisiert sein
  // readlock = compile und datei-erstellung, writelock = löschen
  private static final ReentrantReadWriteLock javaFileLock = new ReentrantReadWriteLock();

  private static GenerationBaseCache globalCache = new GenerationBaseCache();

  public static interface ATT {

    public static final String XMLNS = "xmlns";
    public static final String LABEL = "Label";
    public static final String TYPENAME = "TypeName";
    public static final String TYPEPATH = "TypePath";
    public static final String BASETYPENAME = "BaseTypeName";
    public static final String BASETYPEPATH = "BaseTypePath";
    public static final String VARIABLENAME = "VariableName";
    public static final String OPERATION_NAME = "Name";
    public static final String INVOKE_OPERATION = "Operation";
    public static final String REFERENCENAME = "ReferenceName";
    public static final String REFERENCEPATH = "ReferencePath";
    public static final String ID = "ID";
    public static final String REFID = "RefID";
    public static final String PATH = "Path";
    public static final String SNIPPETTYPE = "Type";
    public static final String SERVICE = "Service";
    public static final String SERVICEID = "ServiceID";
    public static final String ABSTRACT = "IsAbstract";
    public static final String ISSTATIC = "IsStatic";
    public static final String ISLIST = "IsList";
    public static final String ISCANCELABLE = "IsCancelable";
    public static final String ISCASEENABLED = "IsEnabled";
    public static final String ISDEFAULTCASE = "IsDefault";
    public static final String CASENAME = "Label";
    public static final String CASECOMPLEXNAME = "Premise";
    public static final String MDM_VERSION = "Version";
    public static final String DATE = "Date";
    public static final String REQUIRES_XYNA_ORDER = "RequiresXynaOrder";
    public static final String INSTANCE_ID = "InstanceId";
    public static final String ERROR_TYPE = "ErrorType";
    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String STATUS = "Status";
    public static final String EXCEPTION_ID = "ExceptionID";
    public static final String EXCEPTION_NAME = "ExceptionName";
    public static final String EXCEPTION_PATH = "ExceptionPath";
    public static final String INCLUDE_CAUSE = "IncludeCause";
    public static final String CASEALIAS = "Alias";
    public static final String EXCEPTION_CODE = "Code";
    public static final String CODESNIPPET_ACTIVE = "Active";
    public static final String LANGUAGE = "Language";

    public static final String FOREACH_PARALLEL = "IsParallel";
    public static final String FOREACH_LIMITTYPE = "LimitType";
    public static final String FOREACH_LIMIT = "Limit";
    public static final String FOREACH_INDICES = "ForeachIndices";
    public static final String RETRY_COUNTER = "RetryCounter";
    public static final String RETRY_PARAMETER_ID = "RetryParameterID";
    public static final String ISFINAL = "IsFinal";
    
    public static final String OBJECT_ID = "InstanceID";
    public static final String PARENTORDER_ID = "ParentOrderID";
    public static final String OBJECT_REFERENCE_ID = "RefInstanceID";
    
    //RemoteDispatching
    public static final String REMOTE_DESTINATION = "RemoteDestination";

    //für Audit-Daten
    public static final String AUDIT_VERSION = "Version";
    public static final String APPLICATION = "Application";
    public static final String APPLICATION_VERSION = "ApplicationVersion";
    public static final String WORKSPACE = "Workspace";
    public static final String REPOSITORY_REVISION = "RepositoryRevision";
    public static final String AUDIT_FQNAME = "FqName";
    public static final String AUDIT_RC_IS_DEPENDENCY = "IsDependency";
    
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    
    //Restrictions
    public static final String RESTRICTION_UTILIZATION_POLICY = "UtilizationPolicy";
  }


  public static interface SPECIAL_PURPOSES {

    public static final String WAIT = "Wait";
    public static final String SUSPEND = "Suspend";
    public static final String SYNC_AWAIT = "SynchronizationAwait";
    public static final String SYNC_LONG_AWAIT = "SynchronizationLongRunningAwait";
    public static final String SYNC_NOTIFY = "SynchronizationNotify";
    public static final String STARTDOCUMENTCONTEXT = "StartDocumentContext";
    public static final String STOPDOCUMENTCONTEXT = "StopDocumentContext";
    public static final String STARTGENERICCONTEXT = "StartGenericContext";
    public static final String STOPGENERICCONTEXT = "StopGenericContext";
    public static final String GENERICCONTEXT_IDENTIFICATION_ATT = "ContextIdentifier";
    public static final String RETRIEVEDOCUMENT = "RetrieveDocument";
    public static final String QUERY_STORABLE = "QueryStorable";

  }

  public static enum SpecialPurposeIdentifier {
    WAIT(SPECIAL_PURPOSES.WAIT),
    SUSPEND(SPECIAL_PURPOSES.SUSPEND),
    SYNC_AWAIT(SPECIAL_PURPOSES.SYNC_AWAIT),
    SYNC_LONG_AWAIT(SPECIAL_PURPOSES.SYNC_LONG_AWAIT),
    SYNC_NOTIFY(SPECIAL_PURPOSES.SYNC_NOTIFY),
    STARTDOCUMENTCONTEXT(SPECIAL_PURPOSES.STARTDOCUMENTCONTEXT),
    STOPDOCUMENTCONTEXT(SPECIAL_PURPOSES.STOPDOCUMENTCONTEXT),
    RETRIEVEDOCUMENT(SPECIAL_PURPOSES.RETRIEVEDOCUMENT),
    STARTGENERICCONTEXT(SPECIAL_PURPOSES.STARTGENERICCONTEXT),
    STOPGENERICCONTEXT(SPECIAL_PURPOSES.STOPGENERICCONTEXT),
    QUERY_STORABLE(SPECIAL_PURPOSES.QUERY_STORABLE);


    private final String xmlIdentifier;

    private SpecialPurposeIdentifier(String xmlIdentifier) {
      this.xmlIdentifier = xmlIdentifier;
    }

    public String getXmlIdentifier() {
      return xmlIdentifier;
    }

    public static SpecialPurposeIdentifier getSpecialPurposeElementByXmlIdentifier(String xmlIdentifier) {
      if (xmlIdentifier != null) {
        for (SpecialPurposeIdentifier element : values()) {
          if (element.getXmlIdentifier().equals(xmlIdentifier)) {
            return element;
          }
        }
      }
      return null;
    }

  }


  public static interface EL {

    public static final String META = "Meta";
    public static final String ADDITIONALDEPENDENCIES = "AdditionalDependencies";
    public static final String DEPENDENCY_XYNA_PROPERTY = "XynaProperty";
    public static final String DEPENDENCY_DATATYPE = "Datatype";
    public static final String DEPENDENCY_WORKFLOW = "Workflow";
    public static final String DEPENDENCY_TRIGGER = "Trigger";
    public static final String DEPENDENCY_FILTER = "Filter";
    public static final String DEPENDENCY_EXCEPTION = "ExceptionType";
    public static final String DEPENDENCY_SHARED_LIB = "SharedLib";
    public static final String DEPENDENCY_ORDERTYPE = "Ordertype";
    public static final String IS_SERVICE_GROUP_ONLY = "IsServiceGroupOnly";

    public static final String METATYPE = "Type";
    public static final String XYNABEAN = "Data";
    public static final String SOURCE = "Source";
    public static final String TARGET = "Target";
    public static final String LINKTYPE = "LinkType";
    public static final String USEROUTPUT = "UserOutput";
    public static final String LINKTYPE_USER_CONNECTED = "UserConnected";
    public static final String LINKTYPE_CONSTANT_CONNECTED = "Constant";
    public static final String INVOKE = "Invoke";
    public static final String RECEIVE = "Receive";
    public static final String SOURCECODE = "SourceCode";
    public static final String CODESNIPPET = "CodeSnippet";
    public static final String INPUT = "Input";
    public static final String OUTPUT = "Output";
    public static final String OPERATION = "Operation";
    public static final String THROWS = "Throws";
    public static final String DATA = "Data";
    public static final String DATATYPE = "DataType";
    public static final String FUNCTION = "Function";
    public static final String SERIAL = "Serial";
    public static final String PARALLEL = "Parallel";
    public static final String CHOICE = "Choice";
    public static final String CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT = "OuterConditionPart";
    public static final String SERVICE = "Service";
    public static final String SERVICEREFERENCE = "ServiceReference";
    public static final String ROLE = "Role";
    public static final String CHOICEINPUT = "Input";
    public static final String CASE = "Case";
    public static final String ASSIGN = "Assign";
    public static final String COMPENSATE = "Compensate";
    public static final String CATCH = "Catch";
    public static final String THROW = "Throw";
    public static final String PREFERED_EXCEPTION_TYPE = "PreferedExceptionType";
    public static final String EXCEPTION = "Exception";
    public static final String LIBRARIES = "Libraries";
    public static final String VALUE = "Value";
    public static final String SHAREDLIB = "SharedLibraries";
    public static final String PARAMETER = "Parameter";
    public static final String PARAMETER_INPUT = "Input";
    public static final String PARAMETER_OUTPUT = "Output";
    public static final String PARAMETER_ERROR = "Error";
    public static final String COPY = "Copy";
    public static final String EXPECTED_TYPE = "ExpectedType";
    public static final String ORDER_INPUT_SOURCE = "OrderInputSource";
    public static final String ABSTRACT_UID = "Abstract.UID";
    public static final String QUERY_FILTER = "QueryFilter";
    public static final String QUERY_FILTER_CONDITION = "Condition";
    public static final String LinkType = "LinkType";

    //für auditdaten
    public static final String COMPENSATIONS = "Compensations";
    public static final String COMPENSATEFUNCTION = "CompensateFunction";
    public static final String STACKTRACE = "StackTrace";
    public static final String ERRORMESSAGE = "ErrorMessage";
    public static final String EXCEPTIONSTORAGE = "ExceptionStore";
    public static final String EXCEPTIONTYPE = "ExceptionType";
    public static final String DETACHED = "Detached";
    public static final String FREE_CAPACITIES = "FreeCapacities";

    public static final String ISXYNACOMPONENT = "IsXynaComponent";
    public static final String SPECIAL_PURPOSE = "SpecialPurpose";

    public static final String FOREACH = "Foreach";
    public static final String INPUT_LIST = "InputList";
    public static final String OUTPUT_LIST = "OutputList";

    public static final String RETRY = "Retry";
    public static final String MAPPINGS = "Mappings";
    public static final String MAPPINGSINPUT = "Input";
    public static final String MAPPINGSOUTPUT = "Output";
    public static final String MAPPING = "Mapping";
    public static final String MAPPINGSLOCAL = "Local";
    public static final String ISTEMPLATE = "IsTemplate";
    public static final String ISCONDITION = "IsCondition";
    public static final String WORKFLOW_CALL = "Call";
    public static final String DOCUMENTATION = "Documentation";
    public static final String DESCRIPTION = "Description";
    
    public static final String ENHANCED_AUDIT = "EnhancedAudit";
    public static final String AUDIT = "Audit";
    public static final String IMPORT = "Import";
    public static final String DOCUMENT = "Document";
    
    public static final String ORDER_ITEM = "OrderItem";

    //Persistence
    public static final String PERSISTENCE = "Persistence";
    public static final String PERSISTENCE_TYPE = "Type";

    //Restrictions
    public static final String RESTRICTION = "Restriction";
    public static final String RESTRICTION_MAX_LENGTH = "MaxLength";
    public static final String RESTRICTION_DEFAULT_TYPE = "DefaultType";
    public static final String RESTRICTION_MANDATORY = "Mandatory";

    //Datamodel
    public static final String DATAMODEL = "DataModel";
    public static final String PATHMAP = "PathMap";
    public static final String PATHKEY = "PathKey";
    public static final String PATHVALUE = "PathValue";
    public static final String INHERIT_FROM_DATAMODEL = "InheritFromDataModel";
    public static final String INHERIT_FROM_DATAMODEL_PATH = "Path";
    public static final String INHERIT_FROM_DATAMODEL_VALUE = "Value";
    public static final String MODELNAME = "ModelName";
    public static final String MODELOID = "Oid";
    public static final String MODELROOTOID = "RootOid";
    public static final String MODELTYPE = "SnmpType";
    public static final String BASEMODEL = "BaseModel";

    //Versionierung
    public static final String VERSION = "Version";
    public static final String CURRENTVERSION = "CurrentVersion";

    //Forms
    public static final String FORMDEFINITION = "Form";
    public static final String LABEL = "Label";
    public static final String FORM_ITEM = "FormItem";
    public static final String DATALINK = "DataLink";

    //RuntimeContext
    public static final String APPLICATION = "Application";
    public static final String APPLICATION_VERSION = "Version";
    public static final String WORKSPACE = "Workspace";
    
    //RemoteDispatching
    public static final String REMOTE_DISPATCHING = "RemoteDispatching";
    
    public static final String HAS_BEEN_PERSISTED = "HasBeenPersisted";
  }

  private static class StateIsErrorException extends Exception {

    private static final long serialVersionUID = -921495437271115888L;

    public StateIsErrorException(String s) {
      super(s);
    }

  }

  static enum DeploymentState {

    init(0),
    initializeDeploymentModeRunning(1), initializeDeploymentMode(2),
    copyXmlRunning(11), copyXml(12),
    parseXmlRunning(21), parseXml(22),
    completeDependenciesRunning(23), completeDependencies(24),
    collectSpecialDependenciesRunning(25), collectSpecialDependencies(26),
    fillVarsRunning(31), fillVars(32),
    validateRunning(41), validate(42),
    generateJavaRunning(51), generateJava(52),
    bulkCompileRunning(61), bulkCompile(62),
    compileRunning(65), compile(66),
    onDeploymentHandler1Running(71), onDeploymentHandler1(72),
    onDeploymentHandler2Running(73), onDeploymentHandler2(74),
    onDeploymentHandler3Running(75), onDeploymentHandler3(76),
    onDeploymentHandler4Running(77), onDeploymentHandler4(78),
    onDeploymentHandler5Running(79), onDeploymentHandler5(80),
    onDeploymentHandler6Running(81), onDeploymentHandler6(82),
    onDeploymentHandler24Running(121), onDeploymentHandler24(122),
    onDeploymentHandler25Running(123), onDeploymentHandler25(124),
    onDeploymentHandler26Running(125), onDeploymentHandler26(126),
    onDeploymentHandler27Running(127), onDeploymentHandler27(128),
    onDeploymentHandler28Running(129), onDeploymentHandler28(130),
    cleanupRunning(141), cleanup(142),
    errorRunning(201), error(202);


    private int state; //ungerade = running
    private DeploymentState(int state) {
      this.state = state;
    }

    public static DeploymentState getTargetStateForDeploymentHandler(int priority) {
      switch (priority) {
        case 1 : return onDeploymentHandler1;
        case 2 : return onDeploymentHandler2;
        case 3 : return onDeploymentHandler3;
        case 4 : return onDeploymentHandler4;
        case 5 : return onDeploymentHandler5;
        case 6 : return onDeploymentHandler6;
        case 24 : return onDeploymentHandler24;
        case 25 : return onDeploymentHandler25;
        case 26 : return onDeploymentHandler26;
        case 27 : return onDeploymentHandler27;
        case 28 : return onDeploymentHandler28;
        default : throw new RuntimeException("deploymenthandler priority " + priority + " is not defined properly.");
      }
    }

    /**
     * gibt zurück, ob targetstate bereits erreicht ist (von zb anderem thread).
     *
     *  wirft fehler, falls der derzeitige state einen fehler signalisiert und targetstate
     *  nicht auch error ist.
     */
    public boolean hasRunAndHasNoError(DeploymentState targetState) throws StateIsErrorException {
      if (targetState != DeploymentState.error
                      && (this == DeploymentState.errorRunning || this == DeploymentState.error)) {
        throw new StateIsErrorException("could not complete state transition to state " + targetState.toString() + " because the state is already erroneous.");
      }
      return hasRun(targetState);
    }

    public boolean hasRun(DeploymentState targetState) {
      return state >= targetState.state;
    }

    /**
     * also <= targetState
     */
    public boolean isNotFurtherThan(DeploymentState targetState) {
      return state <= targetState.state;
    }

    public boolean isRunning() {
      return state % 2 == 1;
    }

    public boolean mayRun(DeploymentState targetState, DeploymentMode mode) {
      if ((mode == DeploymentMode.reload || mode == DeploymentMode.reloadWithXMOMDatabaseUpdate)
                      && targetState == cleanup) {
        //beim serverstart ist cleanup immer erlaubt, weil das dort der fehlerfall ist
        return true;
      }

      if (this == error) {
        return false;
      }
      if (targetState == error || targetState == cleanup) {
        return true;
      }
      switch (this) {
        case init :
          if (targetState == initializeDeploymentMode) {
            return true;
          }
          break;
        case initializeDeploymentMode :
          if (targetState == copyXml || targetState == parseXml) { //parsexml bei mode = regenerate
            return true;
          }
          break;
        case copyXml :
          if (targetState == parseXml) {
            return true;
          }
          break;
        case parseXml :
          if (targetState == completeDependencies) {
            return true;
          }
          break;
        case completeDependencies :
          if (targetState == collectSpecialDependencies ||
              targetState == fillVars) {
            return true;
          }
          break;
        case collectSpecialDependencies:
          if (targetState == fillVars) {
            return true;
          }
          break;
        case fillVars :
          if (targetState == validate
             || targetState == onDeploymentHandler1 //bei reload/codeUnchanged
             ) {
            return true;
          }
          break;
        case validate :
          if (targetState == generateJava) {
            return true;
          }
          break;
        case generateJava :
          if (targetState == bulkCompile
              || targetState == onDeploymentHandler1 //bei implizitem deployment wird compile übersprungen
              ) {
            return true;
          }
          break;
        case bulkCompile :
          if (targetState == onDeploymentHandler1
              || targetState == compile) { //bei XPRC_CompileError beim compile
            return true;
          }
          break;
        case compile :
          if (targetState == onDeploymentHandler1) {
            return true;
          }
          break;
        case onDeploymentHandler1 :
          if (targetState == onDeploymentHandler2) {
            return true;
          }
          break;
        case onDeploymentHandler2 :
          if (targetState == onDeploymentHandler3) {
            return true;
          }
          break;
        case onDeploymentHandler3 :
          if (targetState == onDeploymentHandler4) {
            return true;
          }
          break;
        case onDeploymentHandler4 :
          if (targetState == onDeploymentHandler5) {
            return true;
          }
          break;
        case onDeploymentHandler5 :
          if (targetState == onDeploymentHandler6) {
            return true;
          }
          break;
        case onDeploymentHandler6 :
          if (targetState == onDeploymentHandler24) {
            return true;
          }
          break;
        case onDeploymentHandler24 :
          if (targetState == onDeploymentHandler25) {
            return true;
          }
          break;
        case onDeploymentHandler25 :
          if (targetState == onDeploymentHandler26) {
            return true;
          }
          break;
        case onDeploymentHandler26 :
          if (targetState == onDeploymentHandler27) {
            return true;
          }
          break;
        case onDeploymentHandler27 :
          if (targetState == onDeploymentHandler28) {
            return true;
          }
          break;
        case onDeploymentHandler28 :
          break;
        default :
          if (logger.isTraceEnabled()) {
            logger.trace("state " + toString() + " is questioned by separate thread.");
          }
      }
      return false;
    }

    public static DeploymentState beginRun(DeploymentState targetState) {
      switch (targetState) {
        case initializeDeploymentMode :
          return initializeDeploymentModeRunning;
        case copyXml :
          return copyXmlRunning;
        case parseXml :
          return parseXmlRunning;
        case completeDependencies :
          return completeDependenciesRunning;
        case collectSpecialDependencies :
          return collectSpecialDependenciesRunning;
        case fillVars :
          return fillVarsRunning;
        case validate :
          return validateRunning;
        case generateJava :
          return generateJavaRunning;
        case bulkCompile :
          return bulkCompileRunning;
        case compile :
          return compileRunning;
        case onDeploymentHandler1 :
          return onDeploymentHandler1Running;
        case onDeploymentHandler2 :
          return onDeploymentHandler2Running;
        case onDeploymentHandler3 :
          return onDeploymentHandler3Running;
        case onDeploymentHandler4 :
          return onDeploymentHandler4Running;
        case onDeploymentHandler5 :
          return onDeploymentHandler5Running;
        case onDeploymentHandler6 :
          return onDeploymentHandler6Running;
        case onDeploymentHandler24 :
          return onDeploymentHandler24Running;
        case onDeploymentHandler25 :
          return onDeploymentHandler25Running;
        case onDeploymentHandler26 :
          return onDeploymentHandler26Running;
        case onDeploymentHandler27 :
          return onDeploymentHandler27Running;
        case onDeploymentHandler28 :
          return onDeploymentHandler28Running;
        case cleanup:
          return cleanupRunning;
        case error:
          return errorRunning;
        default : //fallthrough
      }
      throw new RuntimeException("invalid targetstate: " + targetState.toString());
    }

    public DeploymentState finishRun() {
      switch (this) {
        case initializeDeploymentModeRunning :
          return initializeDeploymentMode;
        case copyXmlRunning :
          return copyXml;
        case parseXmlRunning :
          return parseXml;
        case completeDependenciesRunning :
          return completeDependencies;
        case collectSpecialDependenciesRunning :
          return collectSpecialDependencies;
        case fillVarsRunning :
          return fillVars;
        case validateRunning :
          return validate;
        case generateJavaRunning :
          return generateJava;
        case bulkCompileRunning:
          return bulkCompile;
        case compileRunning:
          return compile;
        case onDeploymentHandler1Running :
          return onDeploymentHandler1;
        case onDeploymentHandler2Running :
          return onDeploymentHandler2;
        case onDeploymentHandler3Running :
          return onDeploymentHandler3;
        case onDeploymentHandler4Running :
          return onDeploymentHandler4;
        case onDeploymentHandler5Running :
          return onDeploymentHandler5;
        case onDeploymentHandler6Running :
          return onDeploymentHandler6;
        case onDeploymentHandler24Running :
          return onDeploymentHandler24;
        case onDeploymentHandler25Running :
          return onDeploymentHandler25;
        case onDeploymentHandler26Running :
          return onDeploymentHandler26;
        case onDeploymentHandler27Running :
          return onDeploymentHandler27;
        case onDeploymentHandler28Running:
          return onDeploymentHandler28;
        case cleanupRunning:
          return cleanup;
        case errorRunning :
          return error;
        default : //fallthrough
      }
      throw new RuntimeException("invalid state: " + toString());
    }

    public DeploymentState next() {
      int n = ordinal() + 2;
      if (n >= values().length) {
        n = values().length - 1;
      }
      return values()[n];
    }

    public boolean supportsDeadLockDetection() {
      return this == parseXml || this == completeDependencies || this == collectSpecialDependencies;
    }

  }


  /**
   * das deployment verläuft in mehreren phasen. je nach deploymentmode werden manche der phasen
   * nicht ausgeführt und mit anderen parametern.
   */
  public enum DeploymentMode {

    /**
     * ähnlich wie codechanged, wird automatisch gesetzt, falls xml nicht im deployment ordner vorhanden ist
     */
    codeNew(true, true, true, true, true, true, true, true, true),
    /**
     * wenn von aussen deploy aufgerufen wird. alle schritte werden ausgeführt
     */
    codeChanged(true, true, true, true, true, true, true, true, true),
    /**
     * sollte nur intern aufgerufen werden (falls ein abhängiges objekt deployed wird). führt die schritte "parse"+"ondeploy" aus
     */
    codeUnchanged(false, false, false, false, false, true, true, true, false),
    /**
     * wie codeUnchanged, wird aber im cleanup aus dem cache entfernt (für specialDependencies). führt die schritte "parse"+"ondeploy" aus
     * entfernt sich auf jeden fall aus dem cache, auch wenn RemoveFromCache=false
     */
    codeUnchangedClearFromCache(false, false, false, false, false, true, true, true, false),
    /**
     * beim serverstart. führt schritte "parse" + "ondeploy" aus
     */
    reload(true, false, false, false, false, false, false, true, false),
    /**
     * beim buildApplication und importApplication. führt schritte "parse" + "ondeploy" aus
     */
    reloadWithXMOMDatabaseUpdate(true, false, false, false, false, false, false, true, false),
    /**
     * im fehlerfall redeploy. soll nicht im fehlerfall wieder aufgerufen werden. verhält sich ansonsten wie "codeNew"
     */
    deployBackup(true, false, true, true, true, false, false, true, false),
    /**
     * alle abhängigen objekte neu generieren (aus deployed-dir)
     */
    regenerateDeployed(false, false, true, true, true, false, false, true, true),
    /**
     * generate and compile object again (from deployed-dir) with the designated Java version
     */
    generateMdmJar(false, false, true, true, true, false, false, true, false),
    /**
     * reservierte objekte (zb objekte, wo klassen auf dem server bereits existieren)
     */
    doNothing(false, false, false, false, false, false, false, false, false),
    /**
     * used to distinguish loading from xml from other modes
     */
    fromXML(false, false, false, false, false, false, false, true, false),
    /**
     * wie fromXML, nur wird auch fillVariables und validate durchgeführt
     */
    fromXMLWithFillVariables(false, false, false, false, false, false, false, true, false),
    /**
     * deployed aus dem deployed-verzeichnis neu, führt im gegensatz zum regenerateDeployed auch deploymenthandler aus etc
     * entfernt sich auf jeden fall aus dem cache, auch wenn RemoveFromCache=false
     * spezialabhängigkeit stört bei mehrfachen deployment nacheinander im cache
     */
    regenerateDeployedAllFeatures(true, false, true, true, true, true, true, true, true),
    /**
     * wie regenerateDeployedAllFeatures, aber da das xml sich geändert hat, wird das xmomrepositorymanagement benachrichtigt
     */
    regenerateDeployedAllFeaturesXmlChanged(true, false, true, true, true, true, true, true, true),
    /**
     * wie regenerateDeployedAllFeatures, aber mit ermittlung von special deps, obwohl sich kein xml geändert hat. z.b. weil sich runtimecontext-deps geändert haben in richtung parents
     * TODO eigtl ist verhalten damit gleich wie regenerateDeployedAllFeaturesXmlChanged
     */
    regenerateDeployedAllFeaturesCollectSpecialDeps(true, false, true, true, true, true, true, true, true);


    private boolean mustExecuteDeploymentHandler;
    private boolean shouldCopyXMLFromSavedToDeployed;
    private boolean shouldGenerateJava;
    private boolean shouldDoCompile;
    private boolean shouldValidate;
    private boolean shouldUseDeploymentManagement;
    private boolean mustPauseMigrationLoadingAtStartup;
    private boolean parseFromDeploymentLocation;
    private boolean notifyCodeAccess;

    private DeploymentMode(boolean mustExecuteDeploymentHandler, boolean shouldCopyXMLFromSavedToDeployed,
                           boolean shouldGenerateJava, boolean shouldDoCompile, boolean shouldValidate,
                           boolean shouldUseDeploymentManagement, boolean mustPauseMigrationLoadingAtStartup, boolean parseFromDeploymentLocation, boolean notifyCodeAccess) {
      this.mustExecuteDeploymentHandler = mustExecuteDeploymentHandler;
      this.shouldCopyXMLFromSavedToDeployed = shouldCopyXMLFromSavedToDeployed;
      this.shouldGenerateJava = shouldGenerateJava;
      this.shouldDoCompile = shouldDoCompile;
      this.shouldValidate = shouldValidate;
      this.shouldUseDeploymentManagement = shouldUseDeploymentManagement;
      this.mustPauseMigrationLoadingAtStartup = mustPauseMigrationLoadingAtStartup;
      this.parseFromDeploymentLocation = parseFromDeploymentLocation;
      this.notifyCodeAccess = notifyCodeAccess;
    }

    public boolean mustExecuteDeploymentHandler() {
      return mustExecuteDeploymentHandler;
    }

    public boolean shouldCopyXMLFromSavedToDeployed() {
      return shouldCopyXMLFromSavedToDeployed;
    }

    public boolean shouldDoCompile() {
      return shouldDoCompile;
    }
    
    public boolean fillVars(boolean doesntExist) {
      if (this == DeploymentMode.doNothing || this == DeploymentMode.fromXML || doesntExist) {
        return false;
      }
      return true;
    }

    /**
     * @return this muss mehr machen als der übergebene mode
     */
    public boolean moreToDoThan(DeploymentMode mode) {
      if (mode == null) {
        return true;
      }
      if (shouldDoCompile && !mode.shouldDoCompile) {
        return true;
      }
      if (shouldCopyXMLFromSavedToDeployed && !mode.shouldCopyXMLFromSavedToDeployed) {
        return true;
      }
      if (mustExecuteDeploymentHandler && !mode.mustExecuteDeploymentHandler) {
        return true;
      }
      if (fillVars(false) && !mode.fillVars(false)) {
        return true;
      }
      return false;
    }

    public boolean mustPauseMigrationLoadingAtStartup() {
      return mustPauseMigrationLoadingAtStartup;
    }

    public boolean parseFromDeploymentLocation() {
      return parseFromDeploymentLocation;
    }

    public boolean notifyCodeAccess() {
      return notifyCodeAccess;
    }

    public boolean mayBeUpgradedTo(DeploymentMode mode, DeploymentState state, Long revision, boolean usedByServerReservedObject) {
      switch (this) {
        case codeUnchangedClearFromCache :
        case fromXMLWithFillVariables :
        case codeUnchanged :
        case doNothing :
          switch (mode) {
            case codeChanged :            
              //für applications ist es ok, da muss nichts kopiert werden.
              try {
                if (XynaFactory.isFactoryServer()) {
                  RuntimeContext rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
                  if (rc instanceof Workspace) {
                    //copy noch nicht begonnen
                    return state.isNotFurtherThan(DeploymentState.initializeDeploymentMode);
                  }
                }
                //keine special dependencies ermittelt
                return state.isNotFurtherThan(DeploymentState.completeDependencies);
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                logger.warn(null, e);
                return false;
              }
            case regenerateDeployedAllFeatures : 
            case regenerateDeployed :
            case codeUnchangedClearFromCache :
              //special dependencies sind die gleichen
              return state.isNotFurtherThan(DeploymentState.collectSpecialDependencies);
            case regenerateDeployedAllFeaturesXmlChanged :
            case regenerateDeployedAllFeaturesCollectSpecialDeps :
              //collect special dependencies muss nun durchgeführt werden, darf also noch nicht gemacht sein!
              return state.isNotFurtherThan(DeploymentState.completeDependencies);
            case fromXMLWithFillVariables :
              //fill variables ist ja evtl anders. ausnahme: usedByServerReservedObject -> da gibt fillVars keine zusatzinfos
              return state.isNotFurtherThan(DeploymentState.collectSpecialDependencies) || usedByServerReservedObject;
            default :
              break;
          }
          break;
        case regenerateDeployedAllFeatures :
        case regenerateDeployedAllFeaturesXmlChanged :
        case regenerateDeployedAllFeaturesCollectSpecialDeps : 
        case reload :
        case reloadWithXMOMDatabaseUpdate :
        case codeChanged :
        case codeNew :
        case deployBackup :
        case fromXML :
        case generateMdmJar :
        case regenerateDeployed :
      }
      return false;
    }


    public DeploymentState getLastParsingStep() {
      switch (this) {
        case reload :
        case regenerateDeployed :
          return DeploymentState.completeDependencies;
        default :
          return DeploymentState.collectSpecialDependencies;
      }
    }

    public boolean updateXMOMRepository() {
      return shouldCopyXMLFromSavedToDeployed() || this == regenerateDeployedAllFeaturesXmlChanged || this == reloadWithXMOMDatabaseUpdate 
          || this == DeploymentMode.regenerateDeployedAllFeatures || this == DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps;
      //letzteres notwendig, weil vorher war das objekt evtl fehlerhaft deployed und deshalb noch gar nicht im xmomrepository drin
    }

    public boolean collectSpecialDependencies() {
      return shouldCopyXMLFromSavedToDeployed() || this == regenerateDeployedAllFeaturesXmlChanged 
          || this == reloadWithXMOMDatabaseUpdate || this == DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps;
    }
  }


  private final static String BREAK_ON_USAGE_IDENTIFIER = "DEFAULT";
  private final static String BREAK_ON_INTERFACE_CHANGES_IDENTIFIER = "TRY";
  private final static String FORCE_DEPLOYMENT_IDENTIFIER = "FORCE";
  private final static String FORCE_KILL_DEPLOYMENT_IDENTIFIER = "FORCEKILL";

  /*
   * Modi und ihre Verwendung (siehe Bug 10186):
   */
  public static enum WorkflowProtectionMode {


    /**
     * Abbruch bei Verwendung - Wird festgestellt das ein Workflow in der Ausführung ist welcher von dem Deployment wie
     * in (1) betroffen ist wird das Deployment abgebrochen.
     */
    BREAK_ON_USAGE(BREAK_ON_USAGE_IDENTIFIER),
    /**
     * Abbruch bei Erkennung einer Schnittstellenänderung - Wird festgestellt, dass das deployte Objekt eine
     * Schnittstellenänderunge einführt und ein Workflow der wie in (2) betroffen ist läuft wird das Deployment
     * abgebrochen. Auch wird das Deployment abgebrochen wenn ein wie in (1) betroffener laufender Auftrag sich nicht
     * suspendieren lässt.
     */
    BREAK_ON_INTERFACE_CHANGES(BREAK_ON_INTERFACE_CHANGES_IDENTIFIER),
    /**
     * Abbruch bei fehlgeschlagener Suspendierung - Fehler aufgrund der Schnittstellenänderungen werden in Kauf
     * genommen, das Deployment wird allerdings abgebrochen wenn ein wie in (1) betroffener laufender Auftrag sich nicht
     * suspendieren lässt.
     */
    FORCE_DEPLOYMENT(FORCE_DEPLOYMENT_IDENTIFIER),
    /**
     * Kein Abbruch - Fehler bei Schnittstellenänderungen werden auch hier in Kauf genommen, auch werden laufende
     * Aufträge die sich nicht suspendieren lassen zwangsweise (interrupt) beendet.
     */
    FORCE_KILL_DEPLOYMENT(FORCE_KILL_DEPLOYMENT_IDENTIFIER);


    private final String identifier;

    private WorkflowProtectionMode(String identifier) {
      this.identifier = identifier;
    }


    public String getIdentifier() {
      return this.identifier;
    }


    public static WorkflowProtectionMode getByIdentifier(String identifier) {
      for (WorkflowProtectionMode mode : values()) {
        if (mode.getIdentifier().equals(identifier)) {
          return mode;
        }
      }
      throw new IllegalArgumentException("Unknown identifier for " + WorkflowProtectionMode.class.getSimpleName());
    }


    public static WorkflowProtectionMode getByIdentifierIgnoreCase(String identifier) {
      for (WorkflowProtectionMode mode : values()) {
        if (mode.getIdentifier().equalsIgnoreCase(identifier)) {
          return mode;
        }
      }
      throw new IllegalArgumentException("Unknown identifier for " + WorkflowProtectionMode.class.getSimpleName());
    }

    public static List<WorkflowProtectionMode> getWorkflowProtectionModesOrderdByForce(boolean ascending) {
      if (ascending) {
        return Arrays.asList(WorkflowProtectionMode.BREAK_ON_USAGE,
                             WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES,
                             WorkflowProtectionMode.FORCE_DEPLOYMENT,
                             WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
      } else {
        return Arrays.asList(WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT,
                             WorkflowProtectionMode.FORCE_DEPLOYMENT,
                             WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES,
                             WorkflowProtectionMode.BREAK_ON_USAGE);
      }
    }

  }

  public static enum DependentObjectMode {
    PROTECT, //beim Undeployment Fehler werfen, falls Abhängigkeiten existieren
    INVALIDATE, //Undeployment trotz Abhängikeiten durchführen, die abhängigen Objekte werden dadurch invalide
    @Deprecated
    UNDEPLOY, //abhängige Objekte rekursiv undeployen
    @Deprecated
    DELETE; //abhängige Objekte rekursiv undeployen und löschen
  }


  private volatile static boolean hasRegisteredDeploymentHandler = false; // ugly!

  static class CopyAndBackupLibrariesAtAppropriateDeploymentHandlingPriority implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
      try {
        object.backupAndCopyServiceLibraries();
      } catch (Ex_FileAccessException e) {
        throw new XPRC_DeploymentHandlerException(object.getFqClassName(), "EXCHANGE_ADDITIONAL_LIBS", e);
      }
    }

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    }

    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }

  }

  private ReentrantLock stateLock = new ReentrantLock();

  //für xmlgenerierung muss man sich name+pfad unverändert merken
  private final String originalPath;
  private final String originalName;
  private final String originalFqName; // keep this as well for performance reasons

  private volatile boolean isXynaFactoryComponent = false;
  private boolean internalParsingDone = false; //zweimal parsen kann vorkommen wegen deadlocks. dann sollte aber das interne parsing nicht erneut aufgerufen werden.
  private long parsingTimestamp = 0;
  private final String simpleClassName;
  private final String fqClassName;
  private String userGeneratedLabel;
  private String xmlRootTagMetadata;
  private DataModelInformation dataModelInformation;
  protected DeploymentMode mode;
  private File backupXml;
  private volatile DeploymentState state = DeploymentState.init;
  //hilfsflag, um die information zu transportieren, dass das objekt als special dependency neu generiert werden muss (oder nicht!).
  //gerade bei wegen deploymentstatus betrachtung gefundenen objekten gibt es viele, wo NICHTS gemacht werden muss
  private boolean regenerateBecauseSpecialDependency;

  private volatile boolean invalidated = false;
  private final List<Throwable> exceptions = new ArrayList<Throwable>();
  private final List<Throwable> exceptionsWhileOnError = new ArrayList<Throwable>();
  private DeploymentState stateBeforeError = DeploymentState.init;
  private JavaSourceFromString generatedJava;

  protected final Long revision;

  /**
   * enthält alle dependencies, inkl sich selbst
   */
  private Dependencies dependencies;

  private boolean inheritCodeChange = false;
  private volatile boolean isBeingUndeployed = false;
  protected final GenerationBaseCache cacheReference;

  public GenerationBaseCache getCacheReference() {
    return cacheReference;
  }

  private static class MapWrapper extends ObjectWithRemovalSupport {
    private final GenerationBaseCache map = new GenerationBaseCache();

    @Override
    protected boolean shouldBeDeleted() {
      return map.size() == 0;
    }

  }
  final static private ConcurrentMapWithObjectRemovalSupport<Long, MapWrapper> parseAdditionalCache = new ConcurrentMapWithObjectRemovalSupport<Long, GenerationBase.MapWrapper>() {

    private static final long serialVersionUID = 9104683742705655083L;

    @Override
    public MapWrapper createValue(Long key) {
      return new MapWrapper();
    }
  };

  protected GenerationBase(String originalClassName, String fqClassName, Long revision) {
    this(originalClassName, fqClassName, globalCache, revision, null, new FactoryManagedRevisionXMLSource());
  }
  
  protected GenerationBase(String originalClassName, String fqClassName, Long revision, XMLSourceAbstraction inputSource) {
    this(originalClassName, fqClassName, globalCache, revision, null, inputSource);
  }

  private static final XynaPropertyBoolean extendedDebugInfo = new XynaPropertyBoolean("xprc.xfractwfe.generation.xmom.generation.debug.creation", true);
  private long _debugCreationTime;
  private StackTraceElement[] _debugCreationCause;

  protected GenerationBase(String fqXmlName, String fqClassName, GenerationBaseCache cache, Long revision,
                           String realType) {
    this(fqXmlName, fqClassName, cache, revision, realType, new FactoryManagedRevisionXMLSource());
  }

  protected GenerationBase(String fqXmlName, String fqClassName, GenerationBaseCache cache, Long revision,
                           String realType, XMLSourceAbstraction inputSource) {
    if (extendedDebugInfo.get()) {
      _debugCreationTime = System.currentTimeMillis();
      _debugCreationCause = Thread.currentThread().getStackTrace();
    }
    this.fqClassName = fqClassName;
    simpleClassName = getSimpleNameFromFQName(fqClassName);
    this.originalName = getSimpleNameFromFQName(fqXmlName);
    this.originalPath = getPackageNameFromFQName(fqXmlName);
    this.originalFqName = originalPath + "." + originalName;
    this.realType = realType;
    if (revision != null) {
      this.revision = revision;
    } else {
      this.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    
    cacheReference = cache;
    xmlInputSource = inputSource;
  }

  public static GenerationBase getInstance(XMOMType type, String originalFqName, Long revision) throws XPRC_InvalidPackageNameException {
    switch (type) {
      case DATATYPE :
        return DOM.getInstance(originalFqName, revision);
      case EXCEPTION :
        return ExceptionGeneration.getInstance(originalFqName, revision);
      case WORKFLOW :
        return WF.getInstance(originalFqName, revision);
      case FORM :
      default :
        throw new RuntimeException("unsupported type " + type);
    }
  }
  
  public static GenerationBase getOrCreateInstance(String fqName, GenerationBaseCache cache, Long usedInRevision) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    return getOrCreateInstance(fqName, cache, usedInRevision, new FactoryManagedRevisionXMLSource());
  }

  public static GenerationBase getOrCreateInstance(String fqName, GenerationBaseCache cache, Long usedInRevision, XMLSourceAbstraction inputSource) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    Long originalRevision = inputSource.getRevisionDefiningXMOMObjectOrParent(fqName, usedInRevision);
    String fqClassName = GenerationBase.transformNameForJava(fqName);
    
    GenerationBase gb = cache.getFromCache( fqName, originalRevision);
    if( gb != null ) {
      return gb;
    }
    
    XMOMType type = inputSource.determineXMOMTypeOf(fqName, originalRevision);
    switch (type) {
    case DATATYPE :
      gb = new DOM(fqName, fqClassName, cache, originalRevision, null, inputSource);
      break;
    case EXCEPTION :
      gb = new ExceptionGeneration(fqName, fqClassName, cache, originalRevision, null, inputSource);
      break;
    case WORKFLOW :
      gb = new WF(fqName, fqClassName, cache, originalRevision, null, inputSource);
      break;
    case FORM :
    default :
      throw new RuntimeException("unsupported type " + type);
    }
    cache.insertIntoCache(gb);
    return gb;
  }
  

  public static class Dependencies {

    private final SortedSet<GenerationBase> allDependencies; //vereinigung aller deps

    private final SortedSet<GenerationBase> normalDependenciesOnly;
    private SortedSet<GenerationBase> subTypeDeps;
    private SortedSet<GenerationBase> additionalObjectsForCodeRegeneration;

    private final DependencyCompletion complete;


    protected Dependencies(SortedSet<GenerationBase> deps, DependencyCompletion complete) {
      this.normalDependenciesOnly = deps;
      this.allDependencies = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      allDependencies.addAll(deps);
      this.complete = complete;
    }


    public SortedSet<GenerationBase> getDependencies(boolean includingSeparableDependencies) {
      if (includingSeparableDependencies) {
        return allDependencies;
      } else {
        return normalDependenciesOnly;
      }
    }


    public void addSubTypes(Set<GenerationBase> s) {
      if (s == null) {
        return;
      }
      if (subTypeDeps == null) {
        subTypeDeps = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      }
      subTypeDeps.addAll(s);
      allDependencies.addAll(s);
    }


    public boolean addSubType(GenerationBase s) {
      if (subTypeDeps == null) {
        subTypeDeps = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      }
      subTypeDeps.add(s);
      return allDependencies.add(s);
    }


    public void addAdditionalObjectsForCodeRegeneration(Set<GenerationBase> s) {
      if (s == null) {
        return;
      }
      if (additionalObjectsForCodeRegeneration == null) {
        additionalObjectsForCodeRegeneration = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      }
      additionalObjectsForCodeRegeneration.addAll(s);
      allDependencies.addAll(s);
    }


    public boolean addAdditionalObjectsForCodeRegeneration(GenerationBase s) {
      if (additionalObjectsForCodeRegeneration == null) {
        additionalObjectsForCodeRegeneration = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      }
      additionalObjectsForCodeRegeneration.add(s);
      return allDependencies.add(s);
    }


    public Set<GenerationBase> getDepsNotIncludedInNormalDeps() {
      if (allDependencies.size() == normalDependenciesOnly.size()) {
        return Collections.emptySet();
      }
      Set<GenerationBase> copy = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      copy.addAll(allDependencies);
      copy.removeAll(normalDependenciesOnly);
      return copy;
    }


    /**
     * fügt element zu normalen deps und allen deps hinzu
     */
    public void addToBoth(GenerationBase gb) {
      normalDependenciesOnly.add(gb);
      allDependencies.add(gb);
    }


  }
  
  
  private boolean mustIncludeOtherDeps() {
    return includeSubTypesInDependencies() || includeAdditionalObjectsForCodeRegeneration();
  }


  private boolean includeSubTypesInDependencies() {
    if (!(mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew)) {
      return false;
    }
    boolean hasInheritableInstanceMethods = false;
    if (this instanceof DOM) {
      DOM dom = (DOM) this;
      hasInheritableInstanceMethods = dom.hasInheritableInstanceMethods(null);
    }
    return hasInheritableInstanceMethods;
  }

  private boolean includeAdditionalObjectsForCodeRegeneration() {
    return mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew;
  }

  /**
   * sammelt alle wfs und datentypen, die von dem objekt direkt oder indirekt benutzt werden. direkt schliesst die
   * objekte aus, auf die nur über einen subauftrag referenziert wird. zb. ist this ein workflow, dann sammelt das
   * objekt alle beteiligten datentypen, services, und die in diesen benutzten typen (membervariablen, methoden
   * signaturen, oberklassen etc) das objekt selbst ist in dem set nicht enthalten.
   */
  public Dependencies getDependenciesRecursively() {
    if (dependencies != null) {
      SortedSet<GenerationBase> dependenciesWithoutSelf = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
      dependenciesWithoutSelf.addAll(dependencies.getDependencies(false));
      dependenciesWithoutSelf.remove(this);
      Dependencies deps = new Dependencies(dependenciesWithoutSelf, dependencies.complete);
      return deps;
    }

    return recalcDependencies();
  }


  private Dependencies recalcDependencies() {
    SortedSet<GenerationBase> list = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
    DependencyCompletion complete = getDependenciesWithCycleDetection(list);
    list.remove(this);
    Dependencies ret = new Dependencies(list, complete);
    return ret;
  }


  private final static XynaPropertyBoolean checkSpecialDependenciesOfSubtypes = new XynaPropertyBoolean("xprc.xfractwfe.deployment.specialdependencies.subtypes.use", true);
  private final static XynaPropertyBoolean checkSpecialDependenciesOfDeploymentItemManagement = new XynaPropertyBoolean("xprc.xfractwfe.deployment.specialdependencies.deploymentstate.use", true);


  private void addOtherDeps(Dependencies ret) {
    if (XynaFactory.isInstanceMocked()) {
      return;
    }
    if (this instanceof DOM) {
      final DOM d = ((DOM) this);
      if (checkSpecialDependenciesOfSubtypes.get() && d.hasInheritableInstanceMethods(null)) {
        ret.addSubTypes(parseAdditionalCache.process(revision, new ValueProcessor<MapWrapper, Set<GenerationBase>>() {

          public Set<GenerationBase> exec(MapWrapper v) {
            Set<GenerationBase> s = d.getSubTypes(v.map);
            for (GenerationBase g : s) {
              g.regenerateBecauseSpecialDependency = true;
            }
            return s;
          }
        }));
      }
    }

    if (checkSpecialDependenciesOfDeploymentItemManagement.get()) {
      ret.addAdditionalObjectsForCodeRegeneration(getAdditionalObjectsForCodeRegeneration());
    }
  }
  
  private static boolean additionalObjectForCodeGenShouldCopyAndNotHasError(GenerationBase gb) {
    if (gb == null) {
      return false;
    }
    DeploymentMode dm = gb.getDeploymentMode();
    if (dm == null) {
       return false;
    }
    if (dm.shouldCopyXMLFromSavedToDeployed) {
      if (gb.hasError()) {
        return false;
      }
      return true;
    }
    return false;
  }

  private Set<GenerationBase> getAdditionalObjectsForCodeRegeneration() {
    Set<GenerationBase> ret = new HashSet<GenerationBase>();
    DeploymentContext deploymentContext = new DeploymentContext(cacheReference);

    DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
    if (dism != null) {
      dism.collectUsingObjectsInContext(originalFqName, deploymentContext, revision);
      Map<Long, Map<XMOMType, Map<String, DeploymentMode>>> additionalObjects = deploymentContext.getAdditionalObjectsForCodeRegeneration();
      
      for (Entry<Long, Map<XMOMType, Map<String, DeploymentMode>>> entry : additionalObjects.entrySet()) {
        Map<XMOMType, Map<String, DeploymentMode>> objectsInRevision = entry.getValue();
        long entryRevision = entry.getKey();
        Map<String, DeploymentMode> additionalWorkflows = objectsInRevision.get(XMOMType.WORKFLOW);
        if (additionalWorkflows != null) {
          for (String fqName : additionalWorkflows.keySet()) {
            try {
              ret.add(getCachedWFInstanceOrCreate(fqName, entryRevision));
            } catch (XPRC_InvalidPackageNameException e) {
              throw new RuntimeException(e);
            }
          }
        }

        Map<String, DeploymentMode> additionalDoms = objectsInRevision.get(XMOMType.DATATYPE);
        if (additionalDoms != null) {
          for (String fqName : additionalDoms.keySet()) {
            try {
              ret.add(getCachedDOMInstanceOrCreate(fqName, entryRevision));
            } catch (XPRC_InvalidPackageNameException e) {
              throw new RuntimeException(e);
            }
          }
        }

        Map<String, DeploymentMode> additionalExceptions = objectsInRevision.get(XMOMType.EXCEPTION);
        if (additionalExceptions != null) {
          for (String fqName : additionalExceptions.keySet()) {
            try {
              ret.add(getCachedExceptionInstanceOrCreate(fqName, entryRevision));
            } catch (XPRC_InvalidPackageNameException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }
    
    boolean invalid;
    if (dism != null) {
      for (GenerationBase generationBase : ret) {
        DeploymentItemState dis = dism.get(generationBase.getOriginalFqName(), generationBase.getRevision());
        //von SAVED aus schauen. wenn das objekt kopiert wird. es ist zwar schon nach deployed kopiert, aber das deploymentitemstatemanagement noch nicht angepasst
        //das passiert erst im cleanup/onerror
        DeploymentLocation source =
            getDeploymentMode().shouldCopyXMLFromSavedToDeployed() ? DeploymentLocation.SAVED : DeploymentLocation.DEPLOYED;
        Set<DeploymentItemInterface> invalid_sd = dis.getInconsistencies(source, DeploymentLocation.DEPLOYED, false);
        Set<DeploymentItemInterface> invalid_ss;
        if (generationBase.xmlInputSource.isOfRuntimeContextType(generationBase.getRevision(), RuntimeContextType.Application)) {
          // es kann in Applications keine Inkonsistenzen zu Saved-Zuständen geben
          invalid_ss = Collections.emptySet();
        } else {
          invalid_ss = dis.getInconsistencies(source, DeploymentLocation.SAVED, false);
        }
        if (invalid_sd.size() == 0) {
          //checken, ob es probleme gibt mit anderen objekten, die auch nach deployed kopiert werden
          invalid = false;
          for (DeploymentItemInterface diii : invalid_ss) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb = cacheReference.getFromCacheInCorrectRevision(prov.getName(), generationBase.getRevision()); //ss_invalid: muss gleiche revision sein
                if (additionalObjectForCodeGenShouldCopyAndNotHasError(gb)) {
                  invalid = true;
                  break;
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        } else if (invalid_ss.size() <= 0) {
          //checken, dass nicht alle deployed/deployed inkonsistenzen verschwinden, weil sie durch saved ersetzt werden
          //d.h. es muss mindestens eine deployed-deployed inkonsistenz geben, die nicht nach deployed kopiert wird
          invalid = false;
          for (DeploymentItemInterface diii : invalid_sd) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb;
                try {
                  gb = cacheReference.getFromCacheInCorrectRevision(GenerationBase.transformNameForJava(prov.getName()), generationBase.getRevision());
                  if (!additionalObjectForCodeGenShouldCopyAndNotHasError(gb)) {
                    invalid = true;
                    break;
                  }
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new RuntimeException(e);
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        } else {
          //es gibt saved-saved inkonsistenzen und saved-deployed ebenso. es kann trotzdem sein, dass die mengen disjunkt sind, und deshalb
          //die saved-deployed inkonsistenzen verschwinden
          //es muss also überprüft werden, dass jede s-d inkonsistenz verschwindet
          invalid = false;
          for (DeploymentItemInterface diii : invalid_sd) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb;
                try {
                  gb = cacheReference.getFromCacheInCorrectRevision(GenerationBase.transformNameForJava(prov.getName()), generationBase.getRevision());
                  if (additionalObjectForCodeGenShouldCopyAndNotHasError(gb)) {
                    //ok, objekt wird nach deployed kopiert - hat es die gleiche inkonsistenz in ss?
                    String descr = diii.getDescription();
                    boolean found = false;
                    for (DeploymentItemInterface diiiss : invalid_ss) {
                      //FIXME performance: alle deploymentiteminterfaces sollten matchableinterfaces sein oder equals implementieren etc 
                      if (!descr.equals(diiiss.getDescription())) {
                        found = true;
                        break;
                      }
                    }
                    if (!found) {
                      invalid = true;
                      break;
                    }
                  } else {
                    //objekt wird nicht repariert
                    invalid = true;
                    break;
                  }
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new RuntimeException(e);
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        }
        boolean currentlyInvalid =  dis.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false).size() > 0;
        if (currentlyInvalid != invalid) {
          generationBase.regenerateBecauseSpecialDependency = true;
        }
        //TODO die anderen objekte aus ret entfernen (und aus dem cache) (falls sie nicht vorher im cache waren - erkennbar am mode/state?)
      }
    }
    return ret;
  }


  private static enum DependencyCompletion {

    complete,
    notComplete,
    completeButUnresolvedCyclicDependencies;

    public DependencyCompletion join(DependencyCompletion d) {
      if (this == notComplete || d == notComplete) {
        return notComplete;
      }
      if (this == completeButUnresolvedCyclicDependencies || d == completeButUnresolvedCyclicDependencies) {
        return completeButUnresolvedCyclicDependencies;
      }
      return complete;
    }
  }

  /**
   * fügt abhängigkeiten zum set dazu.
   * gibt zurück, ob alle dependencies vollständig ermittelt werden konnten, indem für entsprechende objekte "parsing finished" aufgerufen wird.
   */
  private DependencyCompletion getDependenciesWithCycleDetection(SortedSet<GenerationBase> alreadyAdded) {
    DependencyCompletion result = DependencyCompletion.complete;
    alreadyAdded.add(this); //damit bei direkten zyklischen abhängigkeiten kein stackoverflow passiert
    for (GenerationBase gb : getDirectlyDependentObjects()) {
      if (alreadyAdded.add(gb)) {
        if (gb.parsingFinished()) {
          //es ist hier nicht wichtig, was der completionstatus der dependencies von gb ist. z.b. wenn wir oder  ein anderer thread das gb
          //objekt in completeButUnresolvedCyclicDependencies gesetzt hat. da parsingfinished ist, können wir hier die dependencies korrekt berechnen
          result = result.join(gb.getDependenciesWithCycleDetection(alreadyAdded));
        } else if (gb.invalidated || gb.state == DeploymentState.error) {
          //fehler vor dem parsing, als complete zählen
        } else if (gb.stateLock.isHeldByCurrentThread()) {
          //dann ist normal, dass parsing noch nicht finished ist. ok!
          result = result.join(DependencyCompletion.completeButUnresolvedCyclicDependencies);
        } else {
          if (logger.isTraceEnabled()) {
            logger.trace("incomplete: " + gb.getFqClassName() + " in state " + gb.state + ", stateLock: " + gb.stateLock + ". ");
          }
          result = result.join(DependencyCompletion.notComplete);
        }
      }
    }
    return result;
  }

  /**
   * sammelt alle direkt abhängigen objekte. zb membervars, etc
   */
  public abstract Set<GenerationBase> getDirectlyDependentObjects();


  /**
   * nur das xml dieses objekts parsen. rekursion passiert automatisch. abhängige objekte müssen nur instanziiert
   * (getInstance) werden, so dass man sie per getDependencies ermitteln kann
   */
  protected abstract void parseXmlInternally(Element rootElement) throws XPRC_InvalidPackageNameException,
      XPRC_InconsistentFileNameAndContentException, XPRC_InvalidXmlMissingRequiredElementException, InvalidXMLException;


  /**
   * ruft fillVariableContents aller abhängigen Variablen auf. dies passiert nach dem eigentlichen parsen, weil
   * fremdreferenzen erst aufgelöst werden müssen, bevor zb membervariablen zugeordnet werden können
   */
  protected abstract void fillVarsInternally() throws XPRC_MEMBER_DATA_NOT_IDENTIFIED,
      XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidPackageNameException;


  // TODO the generateJavaInternally method should return JavaClass[]
  protected abstract String[] generateJavaInternally(CodeBuffer cb, boolean compileSafe) throws InvalidClassNameException,
      InvalidValuesInXMLException, XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException;


  protected abstract void validateInternally() throws XPRC_InvalidVariableNameException, XPRC_DuplicateVariableNamesException,
      XPRC_MdmDeploymentCyclicInheritanceException, XPRC_MayNotOverrideFinalOperationException, XPRC_EmptyVariableIdException,
      XPRC_InvalidXmlChoiceHasNoInputException, XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException,
      XPRC_InvalidVariableIdException, XPRC_ParsingModelledExpressionException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_InvalidXmlMethodAbstractAndStaticException,
      XPRC_InvalidExceptionXmlInvalidBaseReferenceException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment;


  /**
   * @return the originalPath
   */
  public String getOriginalPath() {
    return originalPath;
  }

  public String getOriginalFqName() {
    return originalFqName;
  }


  /**
   * @return the originalClassName
   */
  public String getOriginalSimpleName() {
    return originalName;
  }


  /**
   * @return the fqClassName
   */
  public String getFqClassName() {
    return fqClassName;
  }



  public String getLabel() {
    return userGeneratedLabel;
  }


  public void setLabel(String label) {
    userGeneratedLabel = label;
  }


  public String getXmlRootTagMetdata() {
    return xmlRootTagMetadata;
  }


  protected void setXmlRootTagMetadata(String xmlMetadata) {
    this.xmlRootTagMetadata = xmlMetadata;
  }

  public DataModelInformation getDataModelInformation() {
    return dataModelInformation;
  }


  public String getSimpleClassName() {
    return simpleClassName;
  }

  public DeploymentMode getDeploymentMode() {
    return mode;
  }


  public long getParsingTimestamp() {
    return parsingTimestamp;
  }

  private void showDependencyTree(String indentation, HashSet<GenerationBase> alreadyDone, Level level)  {
    if (!alreadyDone.add(this)) {
      return;
    }
    logger.log(level, indentation + " " + getOriginalFqName() + " [" + revision + "] " + mode + " - " + state);
    indentation += "  ";
    if (dependencies == null) {
      //TODO workaround eigtl nur wegen bugz 12285
      logger.log(level, "dependencies not set for " + getFqClassName());
    } else {
      for (GenerationBase gb : dependencies.getDependencies(true)) {
        gb.showDependencyTree(indentation, alreadyDone, level);
      }
    }
    indentation = indentation.substring(2);
  }

  private GenerationBase oldInstance = null;


  /**
   * @param mode
   * <ul> 
   *  <li>codeNew wird intern erkannt, falls man codeChanged angibt.
   *  <li>codeUnchanged führt dazu, dass das xml nicht aus dem saved verzeichnis geholt wird, und, dass weder codegenerierung noch validierung +
   *          compile passiert (ausser es ist noch nicht deployed, dann wird alles wie bei codeNew ausgeführt)</li>
   *  <li>Wenn inheritCodeChange true ist, werden codeChanged und codeNew für die abhängigen Objekte übernommen</li>
   * </ul>
   * <br>
   */
  public void deploy(DeploymentMode mode, boolean inheritCodeChange, WorkflowProtectionMode remode)
                  throws XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException {
    if (removeFromCache) {
      parseAdditionalCache.clear();
    }

    synchronized (this) {
      if (isBeingUndeployed) {
        throw new XPRC_DeploymentDuringUndeploymentException(getOriginalFqName(), getDebugInfoException());
      }
    }

    try {
      this.inheritCodeChange = inheritCodeChange;
      initializeDeploymentMode(mode, mode.parseFromDeploymentLocation(), true);

      generateUncachedDeployedInstance(remode);

      copyXml();

      //wegen deadlock gefahr werden hier nicht die methoden der statusübergänge dieses objekts aufgerufen,
      //sondern vorher über die dependencies iteriert.
      parseXmlWithDeadlockDetection(mode.parseFromDeploymentLocation());
      collectSpecialDependenciesWithDeadlockDetection(mode.parseFromDeploymentLocation());

      boolean pauseLoading = this.mode.mustPauseMigrationLoadingAtStartup() && !XynaFactory.getInstance().isStartingUp();
      if (pauseLoading) {
        // pausieren vom Laden des OrderBackups ... wenn das Laden schon fertig, tut das auch nicht weh - die Methode kommt dann sofort zurück
        OrderStartupAndMigrationManagement.getInstance().pauseLoadingAtStartup();
      }
      try {

        // we do need to wait until the parse so that dependendent gbs and their deployment mode are available
        checkForDependentWorkflowsInUse(remode);
        try {

          fillVarsInCorrectOrder();
          validateInCorrectOrder();

          generateJavaInCorrectOrder();
          InMemoryCompilationSet cs = new InMemoryCompilationSet(mode == DeploymentMode.generateMdmJar, true, false);
          compileInCorrectOrder(cs, true);
          
          String classFileName = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, this.revision) + Constants.fileSeparator + fqClassName.replace(".", "/") + ".class";
          long timeStampBeforeCompile = determineLastModified(classFileName);
          boolean compileError = false;
          try {
            try {
              cs.compile();
            } finally {
              compileError = cs.getUnsuccessfullyCompiled() == null || cs.getUnsuccessfullyCompiled().containsKey(fqClassName);
              cs.clear();
            }
          } catch (XPRC_CompileError e) {
            compileInCorrectOrder(cs, false);
            compileError = true;
          }
          
          long timeStampAfterCompile = determineLastModified(classFileName);
          if(generatedJava != null && timeStampAfterCompile == timeStampBeforeCompile && !compileError) {
            if(logger.isDebugEnabled()) {
              logger.debug("successful compilation did not update classfile '" + classFileName + "'.");
            }
            if(XynaProperty.EXCEPTION_ON_DEPLOY_NO_CLASSFILE_UPDATE.get()) {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), new RuntimeException("successful compilation did not update classfile '" + classFileName + "'."));
            }
          }
          

          Integer[] priorities = DeploymentHandling.allPriorities;
          for (int i = 0; i < priorities.length; i++) {
            boolean success = false;
            try {
              try {
                XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling().notifyDeploymentHandlerBegin(priorities[i]);
              } catch (XPRC_DeploymentHandlerException e) {
                throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
              }
              onDeploymentHandlerInCorrectOrder(priorities[i]);
              success = true;
            } finally {
              try {
                XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .notifyDeploymentHandlerFinish(priorities[i], success);
              } catch (XPRC_DeploymentHandlerException e) {
                throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
              }
            }
          }
          cleanupInCorrectOrder();

          throwExceptionCause(null);
        } finally {
          cleanUpDeploymentProcess();
        }

        //fehlerhafte objekte aufräumen, deren exception nicht an rootobjekte propagiert wurde
        onErrorInCorrectOrderAndBackupDeployment();
      } finally {
        if (pauseLoading) {
          // resumen vom Laden des OrderBackups
          // wenn das Laden schon fertig, tut das auch nicht weh - die Methode kommt dann sofort zurück
          OrderStartupAndMigrationManagement.getInstance().resumeLoadingAtStartup();
        }
      }

    } catch (XPRC_WorkflowProtectionModeViolationException e) {
      try {
        errorHandling(e);
      } catch (XPRC_WorkflowProtectionModeViolationException e1) {
        throw new XPRC_MDMDeploymentException(originalFqName, e1);
      }
    } catch (RuntimeException e) {
      this.<RuntimeException> errorHandling(e);
    } catch (Error e) {
      this.<Error> errorHandling(e);
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      this.<XPRC_InheritedConcurrentDeploymentException> errorHandling(e);
    } catch (AssumedDeadlockException e) {
      try {
        this.<AssumedDeadlockException> errorHandling(e);
      } catch (AssumedDeadlockException f) {
        throw new RuntimeException(f);
      }
    } catch (XPRC_MDMDeploymentException e) {
      this.<XPRC_MDMDeploymentException> errorHandling(e);
    } finally {
      cleanupGlobalCaches(revision);
      if (printCacheContent) {
        cacheReference.printCacheContent();
      }
    }
  }
  
  
  private long determineLastModified(String filePath) {
    try {
      return Files.readAttributes(new File(filePath).toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
    } catch (Exception e) {
      return -1l; //method may be called for a file that does not exist yet
    }
  }

  private static void cleanupGlobalCaches(long revision) {
    Path.clearCache();
    if (!XynaFactory.isInstanceMocked()) {
      XynaObject.clearGenerationCache(revision);
    }
  }

                                       // should be Set or Collection 
  public static void deploy(Map<XMOMType, List<String>> deploymentItems, DeploymentMode deploymentMode,
                            boolean inheritCodeChange, WorkflowProtectionMode workflowProtectionMode,
                            Long revision, String comment)
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                         XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    for (XMOMType xmomType : Arrays.asList(XMOMType.DATATYPE, XMOMType.WORKFLOW, XMOMType.EXCEPTION)) {
      if (deploymentItems.get(xmomType) != null) {
        for (String name : deploymentItems.get(xmomType)) {
          objects.add(GenerationBase.getInstance(xmomType, name, revision));
        }
      }
    }

    for (GenerationBase gb : objects) {
      gb.setDeploymentComment(comment);
    }
    deploy(objects, deploymentMode, false, workflowProtectionMode);
  }
  
  
  public static void deploy(Map<Long, Map<XMOMType, Collection<String>>> deploymentItems, DeploymentMode deploymentMode,
                            boolean inheritCodeChange, WorkflowProtectionMode workflowProtectionMode, String comment)
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                         XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    for (Entry<Long, Map<XMOMType, Collection<String>>> deploymentItem : deploymentItems.entrySet()) {
      for (XMOMType xmomType : Arrays.asList(XMOMType.DATATYPE, XMOMType.WORKFLOW, XMOMType.EXCEPTION)) {
        if (deploymentItem.getValue().get(xmomType) != null) {
          for (String name : deploymentItem.getValue().get(xmomType)) {
            objects.add(GenerationBase.getInstance(xmomType, name, deploymentItem.getKey()));
          }
        }
      }
    }

    for (GenerationBase gb : objects) {
      gb.setDeploymentComment(comment);
    }
    deploy(objects, deploymentMode, false, workflowProtectionMode);
  }


  

  /**
   * Deployt mehrere Objekte parallel. D.h. es wird erst für alle Objekte der erste Schritte des Deployments
   * durchgeführt, dann für alle der zweite usw.
   * @param objects
   * @param mode
   * @param inheritCodeChange
   * @param remode
   * @throws MDMParallelDeploymentException
   * @throws XPRC_DeploymentDuringUndeploymentException
   */
  public static void deploy(List<GenerationBase> objects, DeploymentMode mode, boolean inheritCodeChange, WorkflowProtectionMode remode)
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException {
    if (objects.size() == 0) {
      return;
    }

    if (removeFromCache) {
      parseAdditionalCache.clear();
    }

    for (GenerationBase gb : objects) {
      synchronized (gb) {
        if (gb.isBeingUndeployed) {
          throw new XPRC_DeploymentDuringUndeploymentException(gb.getOriginalFqName(), gb.getDebugInfoException());
        }
      }
    }

    SortedSet<GenerationBase> objectsWithDependencies = new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR);
    try {
      for (GenerationBase gb : objects) {
        gb.inheritCodeChange = inheritCodeChange;
      }

      initializeDeploymentMode(objects, mode, mode.parseFromDeploymentLocation(), true);
      generateUncachedDeployedInstance(objects, remode);
      copyXml(objects);

      //wegen deadlock gefahr werden hier nicht die methoden der statusübergänge dieses objekts aufgerufen,
      //sondern vorher über die dependencies iteriert
      parseXmlWithDeadlockDetection(objects, mode.parseFromDeploymentLocation());
      collectSpecialDependenciesInCorrectOrder(objects, mode.parseFromDeploymentLocation());

      for (GenerationBase gb : objects) {
        objectsWithDependencies.addAll(gb.dependencies.getDependencies(true));
      }

      boolean pauseLoading = false;
      if (!XynaFactory.getInstance().isStartingUp()) {
        for (GenerationBase gb : objects) {
          if(gb.mode.mustPauseMigrationLoadingAtStartup()) {
            // pausieren vom Laden des OrderBackups ... wenn das Laden schon fertig, tut das auch nicht weh - die Methode kommt dann sofort zurück
            OrderStartupAndMigrationManagement.getInstance().pauseLoadingAtStartup();
            pauseLoading = true;
            break;
          }
        }
      }

      try {
        // we do need to wait until the parse so that dependendent gbs and their deployment mode are available
        if (XynaFactory.isFactoryServer()) {
          checkForDependentWorkflowsInUse(objects, remode);
        }
        try {

          fillVarsInCorrectOrder(objectsWithDependencies);
          validateInCorrectOrder(objectsWithDependencies);

          generateJavaInCorrectOrder(objectsWithDependencies);
          
          boolean proceedOnError = Compilation.proceedOnErrorPossible();
          InMemoryCompilationSet cs = new InMemoryCompilationSet(mode == DeploymentMode.generateMdmJar, true, proceedOnError);
          if (!XynaFactory.isFactoryServer()) { // TODO encapsulate this better
            cs.setClassDir(objects.get(0).xmlInputSource.getClassOutputFolder().getPath());
          }
          
          compileInCorrectOrder(objectsWithDependencies, cs, true);
          DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
          try {
            cs.compile();
          } catch (XPRC_CompileError e) {
            if (!proceedOnError) { // if the compile does not proceed on error, we have to
              cs.clear();
              compileInCorrectOrder(objectsWithDependencies, cs, false);
            } else {
              for (Entry<String, XPRC_CompileError> error : cs.getUnsuccessfullyCompiled().entrySet()) {
                for (GenerationBase object : objectsWithDependencies) {
                  if (error.getKey().equals(object.getFqClassName()) && dism != null) {
                    DeploymentItemState dis = dism.get(object.getFqClassName(), object.getRevision());
                    if (dis != null) {
                      dis.setBuildError(Optional.of(error.getValue()));
                    }
                  }
                }
              }
            }
          } finally {
            for (String success : cs.getSuccessfullyCompiled()) {
              for (GenerationBase object : objectsWithDependencies) {
                if (success.equals(object.getFqClassName()) && dism != null) {
                  // TODO is it too early to set the error in DeploymentIOtemState?
                  DeploymentItemState dis = dism.get(object.getFqClassName(), object.getRevision());
                  if (dis != null) {
                    dis.setBuildError(Optional.empty());  
                  }
                }
              }
            }
            cs.clear();
          }

          Integer[] priorities = DeploymentHandling.allPriorities;
          for (int i = 0; i < priorities.length; i++) {
            boolean success = false;
            try {
              if (XynaFactory.isFactoryServer()) {
                XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling().notifyDeploymentHandlerBegin(priorities[i]);
                onDeploymentHandlerInCorrectOrder(objectsWithDependencies, priorities[i]);
              }
              success = true;
            } finally {
              if (XynaFactory.isFactoryServer()) {
                XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling().notifyDeploymentHandlerFinish(priorities[i], success);
              }
            }
          }
          cleanupInCorrectOrder(objectsWithDependencies);

        } finally {
          if (XynaFactory.isFactoryServer()) {
            cleanUpDeploymentProcess();
          }
        }

        //falls mind. ein Objekt nicht deployed werden konnte, muss eine Fehlerbehandlung
        //durchgeführt werden
        for (GenerationBase gb : objectsWithDependencies) {
          if (gb.invalidated || gb.state == DeploymentState.error) {
            throw new EmptyException();
          }
        }

        //fehlerhafte objekte aufräumen, deren exception nicht an rootobjekte propagiert wurde
        onErrorInCorrectOrder(objectsWithDependencies);
      } finally {
        if (pauseLoading) {
          // resumen vom Laden des OrderBackups
          // wenn das Laden schon fertig, tut das auch nicht weh - die Methode kommt dann sofort zurück
          OrderStartupAndMigrationManagement.getInstance().resumeLoadingAtStartup();
        }
      }
    } catch (Throwable t) {
      errorHandling(objects, objectsWithDependencies, t);
    } finally {
      if (printCacheContent) {
        IdentityHashMap<GenerationBaseCache, Boolean> ihm =
            new IdentityHashMap<GenerationBaseCache, Boolean>();
        for (GenerationBase gb : objectsWithDependencies) {
          if (!ihm.containsKey(gb.cacheReference)) {
            ihm.put(gb.cacheReference, Boolean.TRUE);
            gb.cacheReference.printCacheContent();
          }
        }
      }
      cleanupGlobalCaches(objects.get(0).revision);
    }
  }
  
  
  public static void undeployAndDelete(Map<XMOMType,List<String>> deploymentItems, boolean disableChecksForRunningOrders,
                     DependentObjectMode dependentObjectMode, boolean checkDeploymentLock, Long revision, RepositoryEvent event, boolean finishUndeploymentHandler) throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    for (XMOMType xmomType : Arrays.asList(XMOMType.DATATYPE, XMOMType.WORKFLOW, XMOMType.EXCEPTION)) {
      if (deploymentItems.get(xmomType) != null) {
        for (String name : deploymentItems.get(xmomType)) {
          try {
            GenerationBase gb = GenerationBase.getInstance(xmomType, name, revision);
            if (gb.getRevision().equals(revision)) {
              objects.add(gb);
            } else {
              logger.info(name + " will not be deleted, because it was found to be part of different runtimecontext: " + gb.getRevision() + " (expected=" + revision + ").");
            }
          } catch (XPRC_InvalidPackageNameException e) {
            logger.warn("Error while trying to delete " + name + ", skipping", e);
          }
        }
      }
    }

    undeployAndDelete(objects, disableChecksForRunningOrders, dependentObjectMode, event, checkDeploymentLock, finishUndeploymentHandler);
  }
  
  
  public static void undeployAndDelete(List<GenerationBase> objects, boolean disableChecksForRunningOrdersUsingThis, DependentObjectMode undeployMode,
                                       RepositoryEvent event, boolean checkDeploymentLock, boolean finishUndeploymentHandler) throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DeploymentContext ctx = new DeploymentContext(globalCache);
    
    try {
      //undeployment
      for (GenerationBase gb : objects) {
        if (logger.isDebugEnabled()) {
          logger.debug("undeploying " + gb.getOriginalFqName() + " in revision " + gb.getRevision());
        }
        try {
          gb.undeploy(undeployMode, disableChecksForRunningOrdersUsingThis, false, checkDeploymentLock, ctx);
        } catch (XPRC_ExclusiveDeploymentInProgress ex) {
          throw new RuntimeException(ex);
        } catch (XPRC_MDMUndeploymentException ex) {
          //dann halt nicht undeployen. aber trotzdem löschen
          logger.warn("could not undeploy object " + gb.getOriginalFqName(), ex);
        }
      }      


      XynaFactoryControl xfc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
      DeploymentItemStateManagement dism = xfc.getDeploymentItemStateManagement();
      DeploymentMarkerManagement dmm = xfc.getDeploymentMarkerManagement();
      XMOMDatabase xmomDatabase = xfc.getXMOMDatabase();
      ApplicationManagement appMgmt = xfc.getApplicationManagement();
      WorkflowDatabase wfdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
      OrdertypeManagement otm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();


      for (GenerationBase gb : objects) {
        try {
          xmomDatabase.unregisterGenerationBaseObject(gb);
        } catch (XynaException ex) {
          logger.warn("Error while trying to unregister " + gb.getOriginalFqName(), ex);
        } catch (RuntimeException ex) {
          logger.warn("Error while trying to unregister " + gb.getOriginalFqName(), ex);
        }

        //aus DeploymentItemStateManagement löschen
        dism.delete(gb.getOriginalFqName(), ctx, gb.getRevision());

        //DeploymentMarker löschen
        try {
          dmm.deleteDeploymentMarkerForDeploymentItem(new DeploymentItemIdentificationBase(XMOMType.getXMOMTypeByGenerationInstance(gb), gb.getOriginalFqName()), gb.getRevision());
        } catch (PersistenceLayerException e1) {
          logger.warn("Error while trying to unregister datatype " + gb.getOriginalFqName(), e1);
        }
      }

      for (GenerationBase gb : objects) {

        //aus saved löschen. vorher nicht löschen, weil das unregister noch ein parse macht und evtl andere objekte mit parsen muss...
        //und Workflows aus saved-Liste in WorkflowDatabase entfernen
        deleteMDMObjectFromSavedFolder(gb.getOriginalFqName(), gb.getRevision());
        if (gb instanceof WF) {
          
          
          wfdb.removeSaved(gb.getOriginalFqName(), gb.getRevision());

          OrdertypeParameter otp = new OrdertypeParameter();
          otp.setRuntimeContext( gb.getRuntimeContext() );
          try {
            otp.setOrdertypeName(gb.getFqClassName());
            otm.deleteOrdertype(otp);
          } catch (PersistenceLayerException e1) {
            logger.warn("Could not remove ordertype info for workflow " + gb.getOriginalFqName());
          }
        }
        event.addEvent(new XMOMDeleteEvent(gb.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(gb), gb.getRevision()));

        //Workflow aus den Application-Definitionen entfernen
        appMgmt.removeObjectFromAllApplications(gb.getOriginalFqName(), ApplicationEntryType.getByXMOMType(XMOMType.getXMOMTypeByGenerationInstance(gb)), gb.getRevision());
      }
    } finally {
      if (finishUndeploymentHandler) {
        GenerationBase.finishUndeploymentHandler();
      }
      Set<String> deleted = new HashSet<>();
      for (GenerationBase gb : objects) {
        deleted.add(gb.getOriginalFqName());
      }
      regenerateDependencies(ctx, deleted);
    }
  }
  

  private static void regenerateDependencies(DeploymentContext ctx, Set<String> ignoreObjects) {
    Map<Long, Map<XMOMType, Map<String, DeploymentMode>>>  additionalObjects = ctx.getAdditionalObjectsForCodeRegeneration();
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    /*
     * Datentypen und Exceptions sind evtl doppelt gewesen und werden nun mit der richtigen Classloaderhierarchie repariert
     * oder
     * Sie funktionieren nun nicht mehr und müssen undeployed werden.
     * 
     * Da im zweiten Fall das Deployment typischerweise fehlschlägt, sollte das so passen.
     */
    for (Entry<Long, Map<XMOMType, Map<String, DeploymentMode>>> entry : additionalObjects.entrySet()) {
      Map<XMOMType, Map<String, DeploymentMode>> revObjects = entry.getValue();
      long revision = entry.getKey();
      for (XMOMType xmomType : Arrays.asList(XMOMType.DATATYPE, XMOMType.WORKFLOW, XMOMType.EXCEPTION)) {
        if (revObjects.get(xmomType) != null) {
          for (String name : revObjects.get(xmomType).keySet()) {
            if (!ignoreObjects.contains(name)) {
              try {
                objects.add(GenerationBase.getInstance(xmomType, name, revision));
              } catch (XPRC_InvalidPackageNameException e) {
                // skip it
                logger.debug("Invalid name '" + name + "' for additional object for code regeneration", e);
              }
            }
          }
        }
      }
    }
    try {
      deploy(objects, DeploymentMode.regenerateDeployedAllFeatures, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
    } catch (MDMParallelDeploymentException e) {
      logger.warn("Error during regenerate",e);
    } catch (XPRC_DeploymentDuringUndeploymentException e) {
      logger.warn("Error during regenerate",e);
    }
  }


  private static final boolean printCacheContent = false;


  private Throwable thrownExceptionCause;
  private XPRC_DeploymentCleanupException thrownExceptionWhileOnError;

  public Throwable getExceptionCause(){
    return thrownExceptionCause;
  }

  public XPRC_DeploymentCleanupException getExceptionWhileOnError(){
    return thrownExceptionWhileOnError;
  }

  private void throwExceptionCause(Set<GenerationBase> additionalObjects) throws AssumedDeadlockException, XPRC_MDMDeploymentException,
      XPRC_InheritedConcurrentDeploymentException {
    if (invalidated || state == DeploymentState.error) {
      Set<Throwable> exceptionCauses = collectExceptionsFromDependencies(additionalObjects);
      if (exceptionCauses.size() == 0) {
        if (logger.isInfoEnabled()) {
          logger.info(getOriginalFqName() + " invalidated but no dependent object has exception. Dependencies are recursively:");
          showDependencyTree("", new HashSet<GenerationBase>(), Level.INFO);
          logger.info("Flat dependencies:");
          for (GenerationBase gb : getDepsLazyCreateSafely(true).getDependencies(true)) {
            logger.info(" "  + gb.toString());
          }
        }

        XPRC_MDMDeploymentException m = new XPRC_MDMDeploymentException(getOriginalFqName(),
                                        new RuntimeException("invalidated but no dependent object has exception."));
        thrownExceptionCause = m;
        throw m;
      } else if (exceptionCauses.size() == 1 && exceptions.size() == 1) {
        Throwable t = exceptions.iterator().next();
        if (t instanceof AssumedDeadlockException) {
          thrownExceptionCause = t;
          throw (AssumedDeadlockException) t;
        } else if (t instanceof RuntimeException) {
          thrownExceptionCause = t;
          throw (RuntimeException) t;
        } else if (t instanceof Error) {
          thrownExceptionCause = t;
          throw (Error) t;
        } else if (t instanceof XPRC_InheritedConcurrentDeploymentException) {
          thrownExceptionCause = t;
          throw (XPRC_InheritedConcurrentDeploymentException) t;
        } else if (t instanceof XPRC_WrongDeploymentTypeException) {
          thrownExceptionCause = t;
          throw (XPRC_WrongDeploymentTypeException) t;
        } else if (t instanceof XPRC_MDMDeploymentException) {
          thrownExceptionCause = t;
          throw (XPRC_MDMDeploymentException) t;
        } else {
          RuntimeException r = new RuntimeException(t);
          thrownExceptionCause = r;
          throw r;
        }
      } else {
        XPRC_MDMDeploymentException m = (XPRC_MDMDeploymentException) (new XPRC_MDMDeploymentException(getOriginalFqName()).initCauses(exceptionCauses
            .toArray(new Throwable[exceptionCauses.size()])));
        thrownExceptionCause = m;
        throw m;
      }
    }

  }

  private static void staticThrowExceptionCause(Collection<GenerationBase> objects) throws MDMParallelDeploymentException {
    Set<GenerationBase> failedObjects = new HashSet<GenerationBase>();

    //alle fehlerhaften Objekte mit ihren Exceptions einsammeln
    for (GenerationBase gb : objects) {
      if (gb.invalidated || gb.state == DeploymentState.error) {
        try {
          gb.throwExceptionCause(null);
        } catch (Throwable t) {
          failedObjects.add(gb);
        }

        //unerwartete Fehler die bei onError aufgetreten sind, in eine eigene Exception schreiben
        if (gb.exceptionsWhileOnError.size() == 1) {
          gb.thrownExceptionWhileOnError = new XPRC_DeploymentCleanupException(gb.getOriginalFqName(), gb.exceptionsWhileOnError.iterator().next());
        } else if (gb.exceptionsWhileOnError.size() > 1){
          gb.thrownExceptionWhileOnError = (XPRC_DeploymentCleanupException) new XPRC_DeploymentCleanupException(gb.getOriginalFqName())
                        .initCauses(gb.exceptionsWhileOnError.toArray(new Throwable[gb.exceptionsWhileOnError.size()]));
        }
      }
    }

    if (failedObjects.size() == 0) {
      //nur fehler in nicht deployten objekten -> TODO hier fehlt eine benachrichtigung für den client. exception != benachrichtigung
      return;
    }
    throw new MDMParallelDeploymentException(failedObjects);
  }


  private Set<Throwable> collectExceptionsFromDependencies(Set<GenerationBase> additionalObjects, Set<GenerationBase> gbsFinished) {
    SortedSet<GenerationBase> gbs;
    if (additionalObjects != null) {
      gbs = new TreeSet<GenerationBase>(DEPENDENCIES_COMPARATOR);
      gbs.addAll(getDepsLazyCreateSafely(true).getDependencies(false));
      gbs.addAll(additionalObjects);
    } else {
      gbs = getDepsLazyCreateSafely(true).getDependencies(false);
    }

    Set<Throwable> s = new HashSet<Throwable>();
    for (GenerationBase gb : gbs) {
      if (!gbsFinished.add(gb)) {
        continue;
      }
      if (gb.invalidated || gb.state == DeploymentState.error) {
        if (gb == this || gb.shouldExceptionBePropagated()) {
          s.addAll(gb.exceptions);
          s.addAll(gb.collectExceptionsFromDependencies(null, gbsFinished));
        }
      }
    }
    return s;
  }


  private Set<Throwable> collectExceptionsFromDependencies(Set<GenerationBase> additionalObjects) {
    return collectExceptionsFromDependencies(additionalObjects, new HashSet<GenerationBase>());
  }

  /**
   * ruft mit inheritCodeChange = false auf
   */
  public void deploy(DeploymentMode mode, WorkflowProtectionMode remode)
      throws XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
      XPRC_MDMDeploymentException {
    deploy(mode, false, remode);
  }

  private static void generateUncachedDeployedInstance(List<GenerationBase> objects, final WorkflowProtectionMode remode) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.generateUncachedDeployedInstance(remode);
      }
    });
  }

  private void generateUncachedDeployedInstance(WorkflowProtectionMode remode) throws XPRC_InheritedConcurrentDeploymentException,
                  AssumedDeadlockException, XPRC_MDMDeploymentException {
    //FIXME cache oder deploymentitemstate management für schnittstellenvergleiche verwenden
    if (this.mode == DeploymentMode.codeChanged &&
        xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace) && 
        remode != WorkflowProtectionMode.BREAK_ON_USAGE ) {
      try {
        if (this instanceof DOM) {
          oldInstance = DOM.generateUncachedInstance(this.originalFqName, true, revision);
        } else if (this instanceof WF) {
          oldInstance = WF.generateUncachedInstance(this.originalFqName, true, revision);
        } else if (this instanceof ExceptionGeneration) {
          oldInstance = ExceptionGeneration.generateUncachedInstance(this.originalFqName, true, revision);
        }
      } catch (XPRC_InvalidPackageNameException e) {
        throw new XPRC_MDMDeploymentException(getOriginalFqName());
      }
    }
  }

  private static void initializeDeploymentMode(List<GenerationBase> objects, final DeploymentMode mode, final boolean fileFromDeploymentLocation, final boolean tryUpgradeMode) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException {
        gb.initializeDeploymentMode(mode, fileFromDeploymentLocation, tryUpgradeMode);
      }
    });
  }


  private void initializeDeploymentMode(DeploymentMode mode1, final boolean fileFromDeploymentLocation, boolean tryUpgradeMode)
                  throws AssumedDeadlockException, XPRC_MDMDeploymentException {
    final DeploymentMode mode;
    if (isReservedServerObject() && this.mode != null) {
      //falls der mode initial explizit anders sein soll, so übernehmen! 
      //er kann aber nicht zu einem späteren zeitpunkt versucht werden auf einen anderen mode zu upgraden außer doNothing
      //alternativ, und evtl sauberer: weitere deploymenthandler speziell für interne objekte ausführen - vgl onDeploymentHandler()
      mode = DeploymentMode.doNothing;
    } else {
      mode = mode1;
    }
    //mode auf codeNew ändern, falls file in deploymentverzeichnis nicht existiert. ansonsten mode so belassen wie er war
    final DeploymentState oldState = state;
    executeJobAndIncrementDeploymentState(DeploymentState.initializeDeploymentMode, new StateTransition() {

      public void exec() {
        synchronized (GenerationBase.this) {
          if (mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables) {
            GenerationBase.this.mode = mode;
            return;
          }
          if (mode == GenerationBase.this.mode) {
            return;
          }

          if (oldState == DeploymentState.init) {
            GenerationBase.this.mode = mode;
            if (mode == DeploymentMode.codeUnchanged || mode == DeploymentMode.codeChanged) {
              if (fileFromDeploymentLocation) {
                // check, ob xml existiert
                File f = GenerationBase.this.xmlInputSource.getFileLocation(GenerationBase.this.getOriginalFqName(), GenerationBase.this.getRevision(), true);
                if (!f.exists()) {
                  GenerationBase.this.mode = DeploymentMode.codeNew;
                  if (logger.isInfoEnabled()) {
                    logger.info("did not find file " + f.getAbsolutePath() + ". overriding mode to "
                                    + GenerationBase.this.mode.toString());
                  }
                } else {
                  // there needs to be a corresponding class file for a deployed object
                  String classFileLocation = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + Constants.fileSeparator + getFqClassName()
                                  .replaceAll("\\.", Constants.fileSeparator);
                  File classFile = new File(classFileLocation + ".class");
                  if (!classFile.exists()) {
                    //inkonsistenter zustand: benutzer will aber offenbar das objekt deployen - also bleibt nichts besseres als codeNew.
                    //passiert aber immer dann, wenn man eine application importiert, die keine classfiles enthält.
                    if (logger.isDebugEnabled()) {
                      logger.debug(new StringBuilder().append("XML for ").append(fqClassName)
                                               .append(" found at deployment location but a corresponding class file ").append(classFile.getAbsolutePath())
                                               .append(" did not exist.").toString());
                    }
                    GenerationBase.this.mode = DeploymentMode.codeNew; //nicht codechanged, weil es fragwürdig ist, in einem solchen zustand ein backup anzulegen (und später zu deployen)
                  }
                }
              }
            }
          } // else bereits von anderem thread in bearbeitung
          else {
            if (GenerationBase.this.mode != null && GenerationBase.this.mode != mode && !isReservedServerObject()) {
              //durch das synchronized und dadurch, dass statetransitions immer nur einmal ausgeführt werden, sollte das nicht passieren
              throw new RuntimeException("Another process is trying to deploy the same or a dependent object ("
                              + getOriginalFqName() + ") and to initialize its deployment mode to " + mode.toString()
                              + ". it is already in state=" + state.toString() + "/mode="
                              + GenerationBase.this.mode.toString() + ". Please try again.", getDebugInfoException());
            }
            GenerationBase.this.mode = mode;
          }
        }
      }

    });

    if (tryUpgradeMode && this.mode != mode) {
      //das ist schlecht, falls ein anderer thread z.b. mit nothing deployed und nun mit codechanged deployed werden soll.
      //dann passt der deploymentmode nicht.
      //der state kann aber schon fortgeschritten sein, deshalb merkt man das oben nicht.
      if (mode.moreToDoThan(this.mode)) {
        if (this.mode != null && this.mode.mayBeUpgradedTo(mode, state, revision, usedByServerReservedObject())) {

          if (logger.isDebugEnabled()) {
            logger.debug("changing mode of " + getOriginalFqName() + " from " + this.mode + " to " + mode + " in state " + state);
          }
          //FIXME was ist, wenn ein anderer thread auf dem objekt arbeitet?
          //      was ist mit den ganzen dependencies? die müssten dann doch jetzt evtl auch nen anderen mode bekommen?
          this.mode = mode;
        } else {
          throw new XPRC_MDMDeploymentException(fqClassName, new RuntimeException("Trying to initialize deployment mode of "
              + getOriginalFqName() + " in state " + state + " to " + mode + " but object is already being deployed with deployment mode "
              + this.mode + ".", getDebugInfoException()));
        }
      }
    }
  }


  private boolean usedByServerReservedObject() {
    return originalFqName.equals(RemoteCall.ANY_INPUT_PAYLOAD_FQ_XML_NAME);
  }

  public void undeploy(DependentObjectMode dependentObjectMode, boolean disableChecks, DeploymentContext context) throws 
      XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    undeploy(dependentObjectMode, disableChecks, true, true, context);
  }
  
  
  public void undeploy(DependentObjectMode dependentObjectMode, boolean disableChecks) throws 
      XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DeploymentContext ctx = new DeploymentContext(cacheReference);
    try {
      undeploy(dependentObjectMode, disableChecks, ctx);
    } finally {
      Set<String> undeployed = new HashSet<>();
      undeployed.add(originalFqName);
      regenerateDependencies(ctx, undeployed);
    }
  }


  public void undeploy(DependentObjectMode dependentObjectMode, boolean disableChecks, boolean finishUndeploymentHandler, boolean checkDeploymentLock, DeploymentContext context)
      throws XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress,
      XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    try {
      undeploy(dependentObjectMode, disableChecks, checkDeploymentLock, context);
      
    } finally {
      if (finishUndeploymentHandler) {
        finishUndeploymentHandler();
      }
    }
  }


  /**
   * - falls workspace, files in deployed löschen (xml, class, jars)
   * - undeploymenthandler aufrufen, zb classloading-dependencies pflegen oder workflowdatabase eintrag entfernen etc
   *
   */
  private void undeploy(DependentObjectMode dependentObjectMode, boolean disableChecks, boolean checkDeploymentLock, DeploymentContext context)
      throws XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress,
      XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    
    if (checkDeploymentLock) {
      DeploymentLocks.writeLock(getOriginalFqName(), getDependencyType(), "Undeployment", revision);
    }
    
    try {
      synchronized (this) {
        if (state != DeploymentState.init) {
          throw new XPRC_UndeploymentDuringDeploymentException(getOriginalFqName(), getDebugInfoException());
        }
        if (isBeingUndeployed) {
          throw new XPRC_SimultanuousUndeploymentException(getOriginalFqName(), getDebugInfoException());
        }
        isBeingUndeployed = true;
      }
      
      DependencySourceType dst = getDependencySourceType();
      if (dst == null) {
        //nicht ordentlich deployed -> aufräumen
        parseCallUndeployHandlerAndRemoveFiles();
      } else {
        DependencyRegister dependencyRegister =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();

        Set<DependencyNode> allDeps = new HashSet<DependencyNode>(dependencyRegister.getDependencies(getOriginalFqName(), dst, revision));
        // remove self
        allDeps.remove(dependencyRegister.getDependencyNode(getOriginalFqName(), dst, revision));
        Set<DependencyNode> deps = new HashSet<DependencyNode>();
        for (DependencyNode dep : allDeps) {
          if (dep.getType() != DependencySourceType.ORDERTYPE) {
            deps.add(dep);
          }
        }

        if (dependentObjectMode == DependentObjectMode.PROTECT) {
          if (deps.size() > 0) {
            throwErrorForExistingDependencies(getOriginalFqName(), deps);
          } else {
            if (!disableChecks && dst == DependencySourceType.WORKFLOW) {
              DeploymentManagement.getInstance().isInUse(new WorkflowRevision(fqClassName, revision)).throwExceptionIfInUse(fqClassName);
            }
            parseCallUndeployHandlerAndRemoveFiles();
          }
        } else {
          if (!disableChecks) {
            checkForRunningDependentWFs(fqClassName);
          }
          parseCallUndeployHandlerAndRemoveFiles();
        }
      }
      DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
      if (dism != null) {
        dism.undeploy(originalFqName, context == null ? DeploymentContext.dummy() : context, revision);
      }
    } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
      throw e;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      throw new XPRC_MDMUndeploymentException(getOriginalFqName(), t);
    } finally {
      Dependencies deps = getDepsLazyCreateSafely(false); //beim undeploy werden objekte nicht geparst -> keine warnung loggen
      for (GenerationBase dep : deps.allDependencies) {
        dep.clearFromCache();
      }

      if (checkDeploymentLock) {
        DeploymentLocks.writeUnlock(getOriginalFqName(), getDependencyType(), revision);
      }
    }
  }


  private RuntimeException getDebugInfoException() {
    StringBuilder sb = new StringBuilder();
    /*
     * <fqname>, <runtimecontext>, <state>, <mode>, <identityhashcode>, <deployed wann>, <deployed durch?>, <cacheid>
     */
    String rcinfo = "";
    try {
      rcinfo = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision).toString();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      rcinfo = "" +revision;
    }
    sb.append("Object in Cache ").append(getCacheId()).append(": ");
    sb.append(originalFqName).append(" [").append(rcinfo).append("] ").append(System.identityHashCode(this)).append(" ").append(state.toString()).append(" ").append(mode.toString());    
    Throwable cause = null;
    if (_debugCreationTime > 0) {
      sb.append(" created: ").append(Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(_debugCreationTime)));
      sb.append(" by cause");
      cause = new RuntimeException("creation cause");
      cause.setStackTrace(_debugCreationCause);
    }
    if (cause == null) {
      return new RuntimeException(sb.toString());
    } else {
      return new RuntimeException(sb.toString(), cause);
    }
  }


  private DependencySourceType getDependencySourceType() {
    DependencyRegister dependencyRegister =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
    DependencyNode dn = dependencyRegister.getDependencyNode(originalFqName, DependencySourceType.DATATYPE, revision);
    if (dn == null) {
      dn = dependencyRegister.getDependencyNode(originalFqName, DependencySourceType.WORKFLOW, revision);
      if (dn == null) {
        dn = dependencyRegister.getDependencyNode(originalFqName, DependencySourceType.XYNAEXCEPTION, revision);
      }
    }
    if (dn == null) {
      return null;
    }
    return dn.getType();
  }


  /**
   * Entfernt das Objekt aus dem Cache und ruft Undeploymenthandler auf.
   * Methode wird in Kontext von Applikationen verwendet, um jene zu entfernen.
   *
   * wenn man alle gewünschten objekte undeployed hat, sollte man {@link #finishUndeploymentHandler()} aufrufen.
   */
  public void undeployRudimentarily(boolean removeFiles) {
    if (logger.isInfoEnabled()) {
      logger.info("undeploying " + getOriginalFqName() + " in revision " + revision);
    }
    try {
      Integer[] priorities = DeploymentHandling.allPriorities;
      for (int i = priorities.length - 1; i >= 0; i--) {
        try {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                          .executeUndeploymentHandler(priorities[i], this);
        } catch (XPRC_UnDeploymentHandlerException e) {
          logger.warn("Call of undeployment handler failed.", e);
        }
      }

      if (removeFiles) {
        deleteXMLAtDeploymentLocation();
        deleteClassFiles();
        deleteJars();
      }
    } finally {
      clearFromCache();
    }
  }

  /**
   * undeploymenthandler batch-verarbeitung ermöglichen, indem man sicher nach dem aufruf für alle objekte nochmal finish() aufruft.
   */
  public static void finishUndeploymentHandler() {
    Integer[] priorities = DeploymentHandling.allPriorities;
    for (int i = priorities.length - 1; i >= 0; i--) {
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                        .finishUndeploymentHandler(priorities[i]);
      } catch (XPRC_UnDeploymentHandlerException e) {
        logger.warn("Call of undeployment handler finish failed.", e);
      }
    }
  }


  private void parseCallUndeployHandlerAndRemoveFiles() {
    File relevantFile = xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), true);
    if (!relevantFile.exists()) {
      if (isReservedServerObject()) {
        relevantFile = xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), false);
        if (!relevantFile.exists()) {
          relevantFile = null;
        }
      } else {
        relevantFile = null;
      }
    }
    if (relevantFile != null) {
      try {
        Document doc = xmlInputSource.getOrParseXML(this, xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), true).exists());
        parseXmlInternally(doc.getDocumentElement());
      } catch (Throwable t) {
        logger.warn("Exception parsing xml of " + originalFqName + " during undeployment.", t);
        //trotzdem weitermachen
      }
    }
    try {
      Integer[] priorities = DeploymentHandling.allPriorities;
      for (int i = priorities.length - 1; i >= 0; i--) {
        try {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
              .executeUndeploymentHandler(priorities[i], this);
        } catch (XPRC_UnDeploymentHandlerException e) {
          logger.warn("Call of undeployment handler failed.", e);
        }
      }
    } finally {
      // put this into the functions?
      if (xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) {
        // Dateien aus Revisionen/Applikationen lieber nicht löschen ... XML ist sonst verloren, weil XML aus SAVED
        // kann schon wieder verändert sein/nicht vorhanden sein
        deleteXMLAtDeploymentLocation();
        deleteClassFiles();
        deleteJars();
      }
    }
  }


  private static void throwErrorForExistingDependencies(String className, Set<DependencyNode> deps)
                  throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    StringBuilder sb = new StringBuilder(" [Dependencies: ");
    for (DependencyNode dep : deps) {
      sb.append(dep.getUniqueName() + " ");
    }
    sb.append("]");
    throw new XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT(className + sb.toString());
  }


  private void checkForRunningDependentWFs(String myFqName)
      throws XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE, XPRC_InvalidPackageNameException, XPRC_DESTINATION_NOT_FOUND {
    DeploymentManagement.getInstance().isInUse(new WorkflowRevision(transformNameForJava(myFqName), revision)).throwExceptionIfInUse(myFqName);

    Set<WorkflowRevision> relevantWFs = getAllRelevantWorkflowsForInterfaceChanges(); //these are all wfs using this object
    for (WorkflowRevision workflowIdentifier : relevantWFs) {
      DeploymentManagement.getInstance().isInUse(workflowIdentifier).throwExceptionIfInUse(workflowIdentifier.wfFqClassName);
    }
  }


  private DependencySourceType retrieveDependencySourceType() {
    if (this instanceof DOM) {
      return DependencySourceType.DATATYPE;
    } else if (this instanceof ExceptionGeneration) {
      return DependencySourceType.XYNAEXCEPTION;
    } else if (this instanceof WF) {
      return DependencySourceType.WORKFLOW;
    } else {
      return null;
    }
  }


  private void deleteXMLAtDeploymentLocation() {
    if (xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) {
      File f = xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), true);
      if (f.exists()) {
        if (logger.isDebugEnabled()) {
          logger.debug("removing xml file " + f.getAbsolutePath());
        }
        f.delete();
        //leere Verzeichnisse löschen
        //String deployedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision);
        //FileUtils.deleteEmptyDirectoryRecursively(f.getParentFile(), new File(deployedMdmDir));
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("didnt find xml file for deletion " + f.getAbsolutePath());
        }
      }
    }
  }


  private void deleteJars() {
    FileUtils.deleteDirectoryRecursively(new File(getFileLocationOfServiceLibsForDeployment(fqClassName, revision)));
  }


  private void deleteClassFiles() {
    String xmomClassesPath = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision);
    String fileLocation = xmomClassesPath + Constants.fileSeparator
                    + this.fqClassName.replaceAll("\\.", Constants.fileSeparator);
    File baseFileLocation = new File(fileLocation + ".class");
    if (baseFileLocation.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("removing " + fileLocation + ".class");
      }
      baseFileLocation.delete();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("could not remove " + fileLocation + ".class" + ", did not exist");
      }
    }

    final String innerclassStart = getSimpleClassName() + "$";
    FilenameFilter fileFilter = new FilenameFilter() {

      public boolean accept(File dir, String name) {
        if (name.endsWith(".class")) {
          if (name.startsWith(innerclassStart)) { //die klasse selbst wurde oben bereits gelöscht.
            return true;
          }
        }
        return false;
      }
    };

    baseFileLocation = baseFileLocation.getParentFile();
    String[] filesToBeRemoved = baseFileLocation.list(fileFilter);
    if (filesToBeRemoved != null && filesToBeRemoved.length > 0) {
      for (String filePath: filesToBeRemoved) {
        File next = new File(baseFileLocation, filePath);
        if (next.exists()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Deleting <" + next.getAbsolutePath() + ">");
          }
          next.delete();
        } else if (logger.isDebugEnabled()) {
          logger.debug("Could not delete <" + next.getAbsolutePath() + ">, did not exist");
        }
      }
    }

    //leere Verzeichnisse löschen (baseFileLocation ist hier bereits das übergeordnete Verzeichnis der
    //gelöschten Datei)
    FileUtils.deleteEmptyDirectoryRecursively(baseFileLocation, new File(xmomClassesPath));
  }



  @Deprecated
  public void parse(boolean cleanup, boolean generateUncachedInstance)
      throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    parseGeneration(true, cleanup, true);
  }


  /**
   * parst das deployte xml und validiert es. abhängige xmls werden auch aus dem deployed-verzeichnis gelesen
   */
  public void parse(boolean cleanup) throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    parseGeneration(true, cleanup, true);
  }


  public void parseGeneration(boolean fromDeploymentLocation, boolean cleanup) throws XPRC_InheritedConcurrentDeploymentException,
      AssumedDeadlockException, XPRC_MDMDeploymentException {
    parseGeneration(fromDeploymentLocation, cleanup, true);
  }

  public void parseGeneration(boolean fromDeploymentLocation, boolean cleanup, boolean validate)
      throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    try {
      initializeDeploymentMode(DeploymentMode.fromXMLWithFillVariables, fromDeploymentLocation, true);
      parseXmlWithDeadlockDetection(fromDeploymentLocation);
      collectSpecialDependenciesWithDeadlockDetection(fromDeploymentLocation);
      fillVarsInCorrectOrder();
      if (validate) {
        validateInCorrectOrder();
      }
      if (cleanup) {
        cleanupInCorrectOrder();
      }
      throwExceptionCause(null);

      //fehlerhafte objekte aufräumen, deren exception nicht an rootobjekte propagiert wurde
      onErrorInCorrectOrderAndBackupDeployment();
    } catch (RuntimeException t) {
      this.<RuntimeException> errorHandling(t);
    } catch (Error t) {
      this.<Error> errorHandling(t);
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      this.<XPRC_InheritedConcurrentDeploymentException> errorHandling(e);
    } catch (AssumedDeadlockException e) {
      this.<AssumedDeadlockException> errorHandling(e);
    } catch (XPRC_MDMDeploymentException e) {
      this.<XPRC_MDMDeploymentException> errorHandling(e);
    }
  }
  
  public JavaSourceFromString generateCode(boolean fromDeploymentLocation)
                  throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    try {
      initializeDeploymentMode(DeploymentMode.regenerateDeployed, fromDeploymentLocation, true);
      parseXmlWithDeadlockDetection(fromDeploymentLocation);
      collectSpecialDependenciesWithDeadlockDetection(fromDeploymentLocation);
      fillVarsInCorrectOrder();
      validateInCorrectOrder();
      generateJava();
      
      cleanupInCorrectOrder();
      throwExceptionCause(null);

      //fehlerhafte objekte aufräumen, deren exception nicht an rootobjekte propagiert wurde
      onErrorInCorrectOrderAndBackupDeployment();
      
      return generatedJava;
    } catch (RuntimeException t) {
      this.<RuntimeException> errorHandling(t);
    } catch (Error t) {
      this.<Error> errorHandling(t);
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      this.<XPRC_InheritedConcurrentDeploymentException> errorHandling(e);
    } catch (AssumedDeadlockException e) {
      this.<AssumedDeadlockException> errorHandling(e);
    } catch (XPRC_MDMDeploymentException e) {
      this.<XPRC_MDMDeploymentException> errorHandling(e);
    }
    return null;
  }
  
  public GeneralXynaObject createObject(AVariable content) throws XPRC_MDMObjectCreationException {
    return createObject(content, true);
  }
  
  /*
   * this ist der typ des rootelements vom content.
   * 
   * membervariablen im content können abgeleitete typen enthalten, deren revision nur über die rootrevision erreichbar ist
   * 
   * jedes parseXML auf den GenerationBase-Objekten braucht aber keine Informationen über die rootrevision, weil das unabhängig von der
   * content-Struktur ist.
   * 
   * Bei der Rekursion über den content muss dann aber die rootrevision durchgereicht werden
   * 
   */
  public GeneralXynaObject createObject(AVariable content, boolean cleanup) throws XPRC_MDMObjectCreationException {
    try {
      try {
        initializeDeploymentMode(DeploymentMode.fromXML, true, false);
        parseXmlWithDeadlockDetection(true);
        collectSpecialDependenciesWithDeadlockDetection(true);
        fillVarsInCorrectOrder();
        validateInCorrectOrder();
        for (GenerationBase gb : content.getDependencies()) {
          //abhängige objekte sind evtl oben noch nicht drin. beispielsweise wenn der content eine liste von abgeleiteten objekten enthält.
          gb.initializeDeploymentMode(DeploymentMode.fromXML, true, false);
          gb.parseXmlWithDeadlockDetection(true);
          gb.collectSpecialDependenciesWithDeadlockDetection(true);
          gb.fillVarsInCorrectOrder();
          gb.validateInCorrectOrder();
        }
        GeneralXynaObject result;
        try {
          content.fillVariableContents();

          if (content.isList()) {
            if (content instanceof DatatypeVariable) {
              XynaObjectList<XynaObject> xol = new XynaObjectList<XynaObject>(null, content.getOriginalName(), content.getOriginalPath());
              for (AVariable child : content.getChildren()) {
                XynaObject xo = (XynaObject) XynaObject.instantiate(child.getFQClassName(), true, child.getDomOrExceptionObject().getRevision());
                xol.add(xo);
                for (AVariable gchild : child.getChildren()) {
                  gchild.fillObject(xo);
                }
              }
              result = xol;
            } else {
              GeneralXynaObjectList<GeneralXynaObject> xol =
                  new GeneralXynaObjectList<GeneralXynaObject>(null, content.getOriginalName(), content.getOriginalPath());
              for (AVariable child : content.getChildren()) {
                GeneralXynaObject xo = XynaObject.instantiate(child.getFQClassName(), false, child.getDomOrExceptionObject().getRevision());
                xol.add(xo);
                for (AVariable gchild : child.getChildren()) {
                  gchild.fillObject(xo);
                }
              }
              result = xol;
            }
          } else if (content instanceof DatatypeVariable) {
            result = XynaObject.instantiate(content.getFQClassName(), true, content.getDomOrExceptionObject().getRevision());
            // v selbst kann kein javatype sein, in xo müssen die kinder gefüllt werden
            for (AVariable child : content.getChildren()) {
              child.fillObject(result);
            }
          } else {
            result = XynaObject.instantiate(content.getFQClassName(), false, content.getDomOrExceptionObject().getRevision());
            // v selbst kann kein javatype sein, in xo müssen die kinder gefüllt werden
            for (AVariable child : content.getChildren()) {
              child.fillObject(result);
            }
          }
          if (cleanup) {
            cleanupInCorrectOrder();
          }


        } finally {
          for (GenerationBase gb : content.getDependencies()) {
            //nur aufräumen, was den richtigen mode hat, evtl ist eines der objekte vor dem parsen bereits im cache gewesen mit einem anderen deploymentmode?
            if (gb.mode == DeploymentMode.fromXML) {
              if (cleanup) {
                gb.cleanupInCorrectOrder();
              }
            }
          }
        }

        throwExceptionCause(content.getDependencies());

        if (cleanup) {
          //fehlerhafte objekte aufräumen, deren exception nicht an rootobjekte propagiert wurde
          onErrorInCorrectOrderAndBackupDeployment();
        }

        return result;

      } catch (RuntimeException t) {
        this.<RuntimeException> errorHandling(t);
      } catch (Error t) {
        this.<Error> errorHandling(t);
      } catch (XynaException e) {
        this.<XynaException> errorHandling(e);
      }
    } catch (XynaException e) {
      logger.debug(null, e);
      throw new XPRC_MDMObjectCreationException(getOriginalFqName(), e);
    }
    return null;
  }


  private static void errorHandling(List<GenerationBase> objects, SortedSet<GenerationBase> objectsWithDependencies, Throwable t) throws MDMParallelDeploymentException {
    Department.handleThrowable(t);

    //falls eine unerwartete Exception aufgetreten ist, diese zu allen noch nicht
    //fertig deployten Objekten hinzufügen (state = cleanup oder ein Schritt vorher,
    //da Cleanup nochmal durchgeführt wird)
    if (!(t instanceof EmptyException)) {
      if (logger.isDebugEnabled()) {
        logger.debug("exception occurred: " + t.getMessage());
        if (logger.isTraceEnabled()) {
          logger.trace(null, t);
        }
      }
      for (GenerationBase gb : objects) {
        if (gb.state != DeploymentState.cleanup && gb.state.next() != DeploymentState.cleanup) {
          DeploymentState s = gb.state;
          gb.exceptions.add(t);

          //falls der fehler nicht aus einer statetransition kam, muss der state umgesetzt werden, damit onerror korrekt durchgeführt wird
          //das sollte für die dependencies nicht notwendig sein
          if (!gb.invalidated && s != DeploymentState.error) {
            gb.invalidate(); //FIXME threadsafety
            if (logger.isDebugEnabled()) {
              logger.debug("invalidated " + gb.originalFqName + ".");
            }
          }
        }
      }
    }

    //die nicht invalidierten objekte aufräumen, und dann auf jeden fall noch onerror aufrufen
    cleanupInCorrectOrder(objectsWithDependencies);

    onErrorInCorrectOrder(objectsWithDependencies);

    staticThrowExceptionCause(objects);
  }

  private <T extends Throwable> void errorHandling(T t) throws T, XPRC_DeploymentCleanupException {
    Department.handleThrowable(t);

    //muss throwable in exceptions liste?
    if (thrownExceptionCause == t) {
      //ok
    } else if (exceptions.size() == 0) {
      exceptions.add(t);
    } else if (exceptions.contains(t)) {
      //ok
    } else if (collectExceptionsFromDependencies(null).contains(t)) {
      //ok
    } else {
      //exception kommt woanders her
      exceptions.add(t);
    }
    DeploymentState s = state;
    if (logger.isDebugEnabled()) {
      logger.debug("got error (" + t.getClass().getSimpleName() + ") " + t.getMessage() + " between state " + s + " and " + s.next());
      if (logger.isTraceEnabled()) {
        logger.trace(null, t);
      }
    }

    //falls der fehler nicht aus einer statetransition kam, muss der state umgesetzt werden, damit onerror korrekt durchgeführt wird
    //das sollte für die dependencies nicht notwendig sein
    if (!invalidated && s != DeploymentState.error) {
      invalidate(); //FIXME threadsafety
    }
    try {
      //die nicht invalidierten objekte aufräumen, und dann auf jeden fall noch onerror aufrufen
      cleanupInCorrectOrder();
    } catch (XPRC_InheritedConcurrentDeploymentException e) {
      logger.warn(null, e);
    } catch (AssumedDeadlockException e) {
      logger.warn(null, e);
    } catch (XPRC_MDMDeploymentException e) {
      logger.warn(null, e);
    }
    try {
      onErrorInCorrectOrderAndBackupDeployment();
    } catch (Throwable t2) {
      Department.handleThrowable(t2);
      throw (XPRC_DeploymentCleanupException) new XPRC_DeploymentCleanupException(getOriginalFqName())
                      .initCauses(new Throwable[] {t, t2});
    }
    throw t;
  }

  private static void copyXml(List<GenerationBase> objects) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.copyXml();
      }
    });
  }

  private void copyXml() throws AssumedDeadlockException {
    //backup erstellen, falls mode=codeChanged.
    //xml kopieren, falls codenew oder codechanged
    executeJobAndIncrementDeploymentState(DeploymentState.copyXml, new StateTransition() {

      public void exec() throws XPRC_MDMDeploymentException {
        if (!mode.shouldCopyXMLFromSavedToDeployed() || isReservedServerObject()) {
          return;
        }

        if (!xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) {
          // wir verzichten auf ein Kopieren, da XML aus SAVED nicht korrekt sein muss
          return;
        }

        try {
          DeploymentLocks.writeLock(getOriginalFqName(), getDependencyType(), "Deployment", revision);

          //backup erstellen
          if (mode == DeploymentMode.codeChanged) {
            File f = xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), true);

            if (f.exists()) {
              backupXml = new File(f.getAbsolutePath() + ".old" + (backupCounter.incrementAndGet()));
              FileUtils.copyFile(f, backupXml);

              if (logger.isDebugEnabled()) {
                logger.debug("created backup " + backupXml.getAbsolutePath());
              }
            } else {
              //müsste codeNew sein!
              throw new Ex_FileAccessException(f.getAbsolutePath());
            }
          }

          if (mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew) {
            serviceLibsBackup = new FolderCopyWithBackup(getFileLocationOfServiceLibsForSaving(fqClassName, revision), getFileLocationOfServiceLibsForDeployment(fqClassName, revision));
          }

          if (logger.isDebugEnabled()) {
            logger.debug("Copying " + getOriginalFqName() + " to deployment folder");
          }

          copyXmlToDeploymentFolder(getOriginalFqName(), getRevision());
        } catch (Ex_FileAccessException e) {
          throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
        }
      }

    });
  }


  private void validateXMLExistenceAndXSD(String targetXmlFile) throws Ex_FileAccessException, XPRC_XmlParsingException {
    File f = new File(targetXmlFile);
    if (!f.exists()) {
      doesntExist = true;
      if (logger.isDebugEnabled()) {
        logger.debug(targetXmlFile + " does not exist (xml for " + getOriginalFqName() + " not found) in revision " + revision + ".");
      }
      return;
    }

    // TODO this is nasty.
    //validierung nicht für exceptions, weil die werden von den exceptionutils mit einem anderen xsd validiert
    if (GenerationBase.this instanceof ExceptionGeneration) {
      return;
    }

    if (!GenerationBasePropertyChangeListener.getInstance().getValidateXsdDisabled()) {
      XMLUtils.validateXMLvsXSD(f.getAbsolutePath());
    }

  }


  protected void validateClassName(String originalPath, String originalName)
                  throws XPRC_InconsistentFileNameAndContentException {
    if (!(originalPath.equals(this.originalPath) && originalName.equals(this.originalName))) {
      throw new XPRC_InconsistentFileNameAndContentException(this.originalPath + "." + this.originalName);
    }
  }

  private static void parseXmlWithDeadlockDetection(List<GenerationBase> objects, final boolean fileFromDeploymentLocation) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.parseXmlWithDeadlockDetection(fileFromDeploymentLocation);
      }
    });
  }

  private void parseXmlWithCyclicDependencyHandling(boolean fileFromDeploymentLocation) throws AssumedDeadlockException {
    parseXml(fileFromDeploymentLocation);
    if (dependencies == null) {
      //fehler bei parsexml...
      dependencies = new Dependencies(new TreeSet<GenerationBase>(DEPENDENCIES_COMPARATOR), DependencyCompletion.complete);
      dependencies.addToBoth(this);
    }
    completeDependencies();
  }


  private void parseXmlWithDeadlockDetection(final boolean fileFromDeploymentLocation) {
    boolean gotDeadlock = true;
    while (gotDeadlock) {
      try {
        parseXmlWithCyclicDependencyHandling(fileFromDeploymentLocation);
        gotDeadlock = false;
      } catch (AssumedDeadlockException e) {
        logger.debug("assumed deadlock during xml-parsing, retrying " + getFqClassName() + " ...");
        logger.trace("", e);
        try {
          Thread.sleep(random.nextInt(200));
        } catch (InterruptedException e1) {
          throw new RuntimeException("thread was interrupted waiting for retry of deadlock", e1);
        }
      }
    }
  }


  public static Element getRootMetaElement(Element rootElement) {
    Element rootMetaElement = null;
    if (rootElement.getTagName().equals(EL.EXCEPTIONSTORAGE)) {
      Element exceptionType = XMLUtils.getChildElementByName(rootElement, GenerationBase.EL.EXCEPTIONTYPE);
      rootMetaElement = XMLUtils.getChildElementByName(exceptionType, GenerationBase.EL.META);
    } else {
      rootMetaElement = XMLUtils.getChildElementByName(rootElement, GenerationBase.EL.META);
    }
    return rootMetaElement;
  }
 
  
  protected XMLSourceAbstraction xmlInputSource;

  // sollte nicht mit false aufgerufen werden wenn der globale cache verwendete wird
  private void parseXml(final boolean fileFromDeploymentLocation) throws AssumedDeadlockException {
    // parse into object.
    // rekursiv für alle abhängigen objekte aufrufen: copyXml + parseXml

    final GenerationBase parent = this;

    executeJobAndIncrementDeploymentState(DeploymentState.parseXml, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (!internalParsingDone) {
          Document d;
          try {

            parsingTimestamp = System.currentTimeMillis();
            d = xmlInputSource.getOrParseXML(GenerationBase.this, fileFromDeploymentLocation);

            if (doesntExist) {
              //erst bei validate fehler werfen
              internalParsingDone = true;
              dependencies = new Dependencies(new TreeSet<GenerationBase>(GenerationBase.DEPENDENCIES_COMPARATOR), DependencyCompletion.complete);
              dependencies.addToBoth(GenerationBase.this);
            }
            
            if (d == null) {
              // input doesn't exist
              return;
            }
          } catch (Ex_FileAccessException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_XmlParsingException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          }
          Element rootElement = d.getDocumentElement();

          if (rootElement.getTagName().equals(EL.DATATYPE) && !(parent instanceof DOM)) {
            throw new XPRC_WrongDeploymentTypeException(getOriginalFqName(), getHumanReadableTypeName(),
                                                        EL.DATATYPE);
          } else if (rootElement.getTagName().equals(EL.SERVICE) && !(parent instanceof WF)) {
            throw new XPRC_WrongDeploymentTypeException(getOriginalFqName(), getHumanReadableTypeName(),
                                                        "Workflow");
          } else if (rootElement.getTagName().equals(EL.EXCEPTIONSTORAGE) && !(parent instanceof ExceptionGeneration)) {
            throw new XPRC_WrongDeploymentTypeException(getOriginalFqName(), getHumanReadableTypeName(),
                                                        "Exception");
          }

          Element rootMetaElement = getRootMetaElement(rootElement);

          if (rootMetaElement != null) {
            Element isFactoryComponentElement = XMLUtils.getChildElementByName(rootMetaElement,
                                                                               GenerationBase.EL.ISXYNACOMPONENT);
            if (isFactoryComponentElement != null) {
              isXynaFactoryComponent = XMLUtils.getTextContent(isFactoryComponentElement).equals("true");
            }
            setXmlRootTagMetadata(XMLUtils.getXMLString(rootMetaElement, false));
            dataModelInformation = DataModelInformation.parse(rootMetaElement);

            if (XMLUtils.getChildElementByName(rootMetaElement, GenerationBase.EL.DATAMODEL) != null) {
              //dieser Type gehört zu einem DatenModell. Ob deployt werden darf oder nicht muss genauer untersucht werden
              if (RevisionManagement.REVISION_DATAMODEL.equals(revision)) {
                //darf nicht deployt werden

                //deploymentlock freigeben, falls es bereits geholt wurde, weil der deploymentmode geändert wird
                // und damit das cleanup nicht mehr das lock freigibt
                if (mode.shouldCopyXMLFromSavedToDeployed()) {
                  if (xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) {
                    DeploymentLocks.writeUnlock(getOriginalFqName(), getDependencyType(), revision);
                  }
                }
                mode = DeploymentMode.fromXML;
                //xml wurde bereits kopiert -> wieder entfernen
                deleteXMLAtDeploymentLocation();
              }
            }
          }

          try {
            // validierung, dass xml in einer sprach-version vorliegt, die der server versteht
            if (XynaFactory.isFactoryServer()) {
              Updater.getInstance().validateMDMVersion(rootElement.getAttribute(ATT.MDM_VERSION));
            }
            parseXmlInternally(rootElement);
          } catch (XynaException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          }
          internalParsingDone = true;
        }

        //rekursion über die abhängigen objekte und versuchen von copy und parseXml:
        Dependencies dependenciesLocal;
        while (true) {
          //liefert anfangs nicht unbedingt vollständige menge, weil das parse von den deps noch nicht durchgeführt wurde.
          dependenciesLocal = recalcDependencies();
          for (GenerationBase gb : dependenciesLocal.getDependencies(false)) {
            gb.xmlInputSource = xmlInputSource;
            if (isReservedServerObjectByFqOriginalName(gb.getOriginalFqName())) {
              gb.initializeDeploymentMode(DeploymentMode.doNothing, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.fromXML) {
              gb.initializeDeploymentMode(DeploymentMode.fromXML, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.fromXMLWithFillVariables) {
              gb.initializeDeploymentMode(DeploymentMode.fromXMLWithFillVariables, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.doNothing) {
              gb.initializeDeploymentMode(DeploymentMode.doNothing, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.regenerateDeployed) {
              gb.initializeDeploymentMode(DeploymentMode.regenerateDeployed, fileFromDeploymentLocation, true);
            } else  if (!gb.getRevision().equals(getRevision())) {
              gb.initializeDeploymentMode(DeploymentMode.fromXMLWithFillVariables, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.reload) {
              gb.initializeDeploymentMode(DeploymentMode.reload, fileFromDeploymentLocation, true);
            } else if (mode == DeploymentMode.reloadWithXMOMDatabaseUpdate) {
              gb.initializeDeploymentMode(DeploymentMode.reloadWithXMOMDatabaseUpdate, fileFromDeploymentLocation, true);
            } else if (mode == DeploymentMode.generateMdmJar) {
              gb.initializeDeploymentMode(DeploymentMode.generateMdmJar, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.deployBackup) {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchanged, fileFromDeploymentLocation, false);
            } else if (mode == DeploymentMode.regenerateDeployedAllFeatures) {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchangedClearFromCache, fileFromDeploymentLocation, optionalModeUpgrade(gb));
            } else if (mode == DeploymentMode.regenerateDeployedAllFeaturesXmlChanged) {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchangedClearFromCache, fileFromDeploymentLocation, optionalModeUpgrade(gb));
            } else if (mode == DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps) {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchangedClearFromCache, fileFromDeploymentLocation, optionalModeUpgrade(gb));
            } else if (mode == DeploymentMode.codeUnchangedClearFromCache) {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchangedClearFromCache, fileFromDeploymentLocation, optionalModeUpgrade(gb));
            } else if (inheritCodeChange) {
              gb.inheritCodeChange = true;
              gb.initializeDeploymentMode(DeploymentMode.codeChanged, fileFromDeploymentLocation, true);
            } else {
              gb.initializeDeploymentMode(DeploymentMode.codeUnchanged, fileFromDeploymentLocation, false);
            }
          }

          //rekursion über die normalen dependencies
          for (GenerationBase gb : dependenciesLocal.getDependencies(false)) {
            if (fileFromDeploymentLocation && !gb.internalParsingDone) {
              gb.copyXml();
            }
            if (gb.comment == null && comment != null) {
              gb.comment = "Implicit Deployment. " + comment;
            }
            gb.parseXml(fileFromDeploymentLocation);
          }

          if (dependenciesLocal.complete == DependencyCompletion.notComplete) {
            /*
             * die erste berechnung der abhängigkeiten ist im normalfall nicht komplett, erst durch die rekursion gibt es die
             * möglichkeit, die abhängigkeiten komplett zu finden (bis auf zyklische).
             * hier ist jetzt die stelle, wo die rekursion geschehen ist, also jetzt nochmal die kompletten dependencies
             * bestimmen.
             *
             * es kann nicht sein, dass eines der dependency objekte von einem anderen thread bearbeitet wird, weil der parsexml
             * aufruf von diesem thread auf das andere objekt dann auf den zustandsübergang warten würde (schlimmstenfalls
             * assumeddeadlockexception)
             *
             * deshalb macht es hier auch keinen sinn, zu warten. man kann direkt erneut das recalcDependencies versuchen
             */
            continue;
          } else if (dependenciesLocal.complete == DependencyCompletion.completeButUnresolvedCyclicDependencies) {
            /*
             * dependencies sind komplett (bis auf zyklische abhängigkeiten)
             *
             * usecase:
             * A -> B -> A
             * A -> C
             *
             * beim parsen von B wird als dependencies nur A ermittelt, A kann nicht rekursiv weiter durchsucht werden, weil noch geblockt vom eigenen thread
             * -> dependencies von B enthalten C nicht, weil C erst nach B geparst wird
             * -> B's dependencies werden als incomplete bzgl cyclic dependencies markiert.
             *
             * wenn später A fertig wird, werden seine abhängigkeiten in B integriert.
             * vgl parseXmlWithDeadlockDetection
             *
             */
            //ok -> zurück zum parentobjekt
            break;
          } else {
            //1. selbst complete, aber kind nicht -> d.h. der thread hat selbst das kind am fertigwerden gehindert.
            //es sollte erneut versucht werden, die kind dependencies fertigzustellen -> passiert im nächsten zustandsübergang
            //2. selbst complete und kinder auch
            break;
          }
        }

        dependenciesLocal.addToBoth(GenerationBase.this);
        if (logger.isDebugEnabled()) {
          logger.debug("normal dependency calculation completed for " + originalFqName);
        }
        dependencies = dependenciesLocal;
      }

      private boolean optionalModeUpgrade(GenerationBase gb) {
        if (gb.mode == null || gb.mode == DeploymentMode.codeUnchangedClearFromCache) {
          return true;
        }
        return gb.mode.mayBeUpgradedTo(DeploymentMode.codeUnchangedClearFromCache, gb.state, gb.revision, usedByServerReservedObject());
      }

    });

  }


  private void completeDependencies() throws AssumedDeadlockException {
    executeJobAndIncrementDeploymentState(DeploymentState.completeDependencies, new StateTransition() {

      @Override
      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        Dependencies dependenciesLocal = dependencies;
        if (dependencies.complete == DependencyCompletion.completeButUnresolvedCyclicDependencies) {
          while (true) {
            dependenciesLocal = recalcDependencies();
            if (dependenciesLocal.complete == DependencyCompletion.notComplete) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted waiting for other thread to finish parseXml on dependent object of " + getOriginalFqName());
              }
            } else if (dependenciesLocal.complete == DependencyCompletion.completeButUnresolvedCyclicDependencies) {
              throw new RuntimeException();
            } else {
              break;
            }
          }
        }
        //rekursion, um auch bei den kindern die completeButUnresolvedCyclicDependencies zu finden
        for (GenerationBase gb : dependenciesLocal.getDependencies(false)) {
          gb.completeDependencies();
        }
        if (dependencies.complete == DependencyCompletion.completeButUnresolvedCyclicDependencies) {
          dependenciesLocal.addToBoth(GenerationBase.this);
          dependencies = dependenciesLocal;
        }
      }

    });
  }

  protected abstract String getHumanReadableTypeName();


  private static void collectSpecialDependenciesInCorrectOrder(List<GenerationBase> objects, final boolean fileFromDeploymentLocation) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.collectSpecialDependenciesWithDeadlockDetection(fileFromDeploymentLocation);
      }
    });
  }


  private void collectSpecialDependenciesWithDeadlockDetection(boolean fileFromDeploymentLocation) throws AssumedDeadlockException {
    if (mode.getLastParsingStep() != DeploymentState.collectSpecialDependencies) {
      return;
    }
    boolean gotDeadlock = true;
    while (gotDeadlock) {
      try {
        collectSpecialDependencies(fileFromDeploymentLocation);
        gotDeadlock = false;
      } catch (AssumedDeadlockException e) {
        //retry
        logger.debug("assumed deadlock during special dependency collection. retrying " + getFqClassName() + " ...");
        logger.trace("", e);
        try {
          Thread.sleep(random.nextInt(200));
        } catch (InterruptedException e1) {
          throw new RuntimeException("thread was interrupted waiting for retry of deadlock", e1);
        }
      }
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("dependencies (" + originalFqName + "): ");
      showDependencyTree("", new HashSet<GenerationBase>(), Level.TRACE);
    }
  }


  private void collectSpecialDependencies(final boolean fileFromDeploymentLocation) throws AssumedDeadlockException {
    /*
     * spezialdependencies haben eigenen deploymentstate, weil sichergestellt werden soll, dass alle objekte in den
     * normalen dependencies bereits ermittelt sind, bevor die spezialdependencies hinzugefügt werden.
     *
     * wenn das nicht der fall ist, müsste der deploymentmode evtl von regenerateDeployedAllFeatures auf einen höheren
     * (z.b. codechanged) geupgraded werden. das geht nicht.
     */
    executeJobAndIncrementDeploymentState(DeploymentState.collectSpecialDependencies, new StateTransition() {

      @Override
      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        if (doesntExist) {
          return;
        }

        SortedSet<GenerationBase> oldDeps = new TreeSet<GenerationBase>(DEPENDENCIES_COMPARATOR);
        oldDeps.addAll(dependencies.getDependencies(false));
        Dependencies newDeps = new Dependencies(oldDeps, dependencies.complete);

        if (mode.collectSpecialDependencies() && mustIncludeOtherDeps()) {
          addOtherDeps(newDeps);

          //alle spezialdependencies behandeln, die nicht in den normalen dependencies enthalten sind
          for (GenerationBase gb : newDeps.getDepsNotIncludedInNormalDeps()) {
            if (isReservedServerObjectByFqOriginalName(gb.getOriginalFqName())) {
              //sollte eigtl nicht vorkommen? sicher ist sicher.
              gb.initializeDeploymentMode(DeploymentMode.doNothing, fileFromDeploymentLocation, false);
            } else if (gb.regenerateBecauseSpecialDependency) {
              //bei instanzmethoden müssen auch abgeleitete doms neu generiert werden
              //bei parentstorables müssen diese auch neu generiert werden
              gb.initializeDeploymentMode(DeploymentMode.regenerateDeployedAllFeatures, fileFromDeploymentLocation, true);
            } else {
              gb.initializeDeploymentMode(DeploymentMode.doNothing, fileFromDeploymentLocation, false);
            }
          }

          for (GenerationBase gb : newDeps.getDependencies(true)) {
            //für die meisten dependencies sind copyXml und parseXml bereis geschehen. macht aber nichts.

            if (fileFromDeploymentLocation && !gb.internalParsingDone) {
              gb.copyXml();
            }
            gb.parseXmlWithCyclicDependencyHandling(fileFromDeploymentLocation); // falls selbst-rekursion, wird das durch state erkannt
            //falls hier eine deadlock exception passiert, weiterwerfen und das lock von this freigeben, damit der andere thread weiterkommt.

            gb.collectSpecialDependencies(fileFromDeploymentLocation);
          }

        } else {
          //rekursion trotzdem durchführen
          for (GenerationBase gb : dependencies.getDependencies(false)) {
            gb.collectSpecialDependencies(fileFromDeploymentLocation);
          }
        }

        /*
         * auch wenn es lokal keine special dependencies gibt, müssen in die gesamt-dependencies von this die neuen dependencies von kindern mit aufgenommen werden!
         * oben ist eine rekursion passiert über this.dependencies, und die haben ggf special dependencies hinzu gefügt bekommen
         * 
         * wenn das nicht passiert, kann es sowohl zu reihenfolgenproblemen kommen (was bei mehreren threads deadlocks nach sich ziehen kann), als auch zu
         * relikten im cache, weil das cleanup nicht rekursiv über den gesamten baum geht, sondern bei do-nothing abbricht.
         */
        for (GenerationBase gb : new TreeSet<GenerationBase>(newDeps.getDependencies(true))) {
          if (gb.dependencies != null) {
            newDeps.addAdditionalObjectsForCodeRegeneration(gb.dependencies.additionalObjectsForCodeRegeneration);
            newDeps.addSubTypes(gb.dependencies.subTypeDeps);
            //normale dependencies können auch dazu gekommen sein (vorwärts-deps von special deps)
            for (GenerationBase ngb : gb.dependencies.normalDependenciesOnly) {
              newDeps.addToBoth(ngb);
            }
          }
        }

        dependencies = newDeps;
      }

    });

  }


  static void traceParsingNotFinished(GenerationBase gb) {
    if (logger.isTraceEnabled()) {
      logger.trace("dependencies of " + gb.getOriginalFqName() + " not added, because its state is not high enough.");
    }
  }


  public boolean parsingFinished() {
    try {
      if (invalidated) {
        return dependencies != null;
      }
      return state.hasRunAndHasNoError(DeploymentState.parseXml);
    } catch (StateIsErrorException e) {
      return dependencies != null;
    }
  }


  private static void checkForDependentWorkflowsInUse(List<GenerationBase> objects, final WorkflowProtectionMode remode)
      throws XPRC_WorkflowProtectionModeViolationException {
    if (XynaFactory.getInstance().isStartingUp()) {
      return;
    }

    Set<WorkflowRevision> relevantSetForClassloadingChanges = new HashSet<WorkflowRevision>();
    Set<WorkflowRevision>relevantSetForInterfaceChanges = new HashSet<WorkflowRevision>();

    //ermitteln der betroffenen Workflows
    for (GenerationBase gb : objects) {

      relevantSetForClassloadingChanges.addAll(gb.getAllRelevantWorkflowsForClassLoadingChanges());
      if (remode == WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES) {
        if (gb.oldInstance != null) {
          try {
            if (gb.detectInterfaceChangesBetweenThisAndOldVersion(gb.oldInstance)) {
              relevantSetForInterfaceChanges.addAll(gb.getAllRelevantWorkflowsForInterfaceChanges());
            }
          } finally {
            gb.oldInstance = null;
          }
        }
      }
    }

    if (relevantSetForInterfaceChanges != null &&
        relevantSetForInterfaceChanges.size() > 0) {
      DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode, relevantSetForInterfaceChanges);
    } else {
      DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode);
    }
  }


  private void checkForDependentWorkflowsInUse(final WorkflowProtectionMode remode) throws XPRC_WorkflowProtectionModeViolationException {
    if (!isDeploymentManagementNecessary()) {
      return;
    }
    try {
      Set<WorkflowRevision> relevantSetForClassloadingChanges = getAllRelevantWorkflowsForClassLoadingChanges();
      switch (remode) {
        case BREAK_ON_INTERFACE_CHANGES :
        case FORCE_DEPLOYMENT :
        case FORCE_KILL_DEPLOYMENT :
          if (oldInstance != null) {
            //TODO eigtl müsste man das für alle objekte sammeln, die das xml von saved nach deployed kopieren
            if (detectInterfaceChangesBetweenThisAndOldVersion(oldInstance)) {
              Set<WorkflowRevision> relevantSetForInterfaceChanges = getAllRelevantWorkflowsForInterfaceChanges();
              DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode, relevantSetForInterfaceChanges);
            } else {
              DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode);
            }
          } else {
            DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode);
          }
          break;
        case BREAK_ON_USAGE : //we don't care for interface changes, we gonna fail anyway if there is a workflow running
          DeploymentManagement.getInstance().addDeployment(relevantSetForClassloadingChanges, remode);
          break;
      }
    } finally {
      oldInstance = null;
    }
  }


  private boolean isDeploymentManagementNecessary() {
    return !XynaFactory.getInstance().isStartingUp();
  }



  private static boolean cleanUpDeploymentProcess() {
    DeploymentManagement.getInstance().cleanupIfLast();
    return true;
  }


  // ermittelt alle WFs die dieses Objekt verwenden, diese (bzw. Objekte welche Sie verwenden) sind betroffen von Interface-Änderungen
  private Set<WorkflowRevision> getAllRelevantWorkflowsForInterfaceChanges() {

    DependencyRegister dr = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getDependencyRegister();
    DependencyNode node = dr.getDependencyNode(originalFqName, retrieveDependencySourceType(), revision);
    if (node == null) { //not present in DependencyRegister = no Dependencies
      return Collections.emptySet();
    }

    Set<DependencyNode> deps = dr.getDependencies(node.getUniqueName(), node.getType(), revision, true);
    deps.add(node);
    return transformDependenySetToAffectedWorkflowIdentifiers(deps);
  }


  //we need to get the oldInstance at the start of the deployment
  private boolean detectInterfaceChangesBetweenThisAndOldVersion(GenerationBase oldInstance) {
    //TODO: would love to generate the oldInstance from backUp here
    return this.compareImplementation(oldInstance);
  }


  public abstract boolean compareImplementation(GenerationBase oldVersion);


  private Set<WorkflowRevision> transformDependenySetToAffectedWorkflowIdentifiers(Set<DependencyNode> dependencyNodes) {
    Set<WorkflowRevision> allAffectedWorkflows = new HashSet<WorkflowRevision>();
    for (DependencyNode node : dependencyNodes) {
      if (node.getType() == DependencySourceType.WORKFLOW) {
        try {
          allAffectedWorkflows.add(new WorkflowRevision(transformNameForJava(node.getUniqueName()), node.getRevision()));
        } catch (XPRC_InvalidPackageNameException e) {
          // nothing to protect if not valid package name
        }
      }
    }
    return allAffectedWorkflows;
  }


  // get everyone using from DepReg
  // for every generationBase dependency (used stuff) if mode.shouldReloadClassloader get using objects from DepReg
  private Set<WorkflowRevision> getAllRelevantWorkflowsForClassLoadingChanges() {
    Set<WorkflowRevision> deps = new HashSet<WorkflowRevision>();
    traverseGenerationBaseAndGetDependencies(deps);
    return deps;
  }


  private void traverseGenerationBaseAndGetDependencies(Set<WorkflowRevision> dependencies) {
    //suche die workflows, die deployed werden und ihre parent-workflows
    DependencyRegister dr = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
    for (GenerationBase gb : this.dependencies.getDependencies(true)) {
      if (gb.mode != null && gb.mode.mustExecuteDeploymentHandler) { //wenn mustExecuteDeploymentHandler false ist, wurde nichts an classloading geändert
        DependencyNode node = dr.getDependencyNode(gb.originalFqName, gb.retrieveDependencySourceType(), gb.revision);
        if (node != null) {
          Set<DependencyNode> deps = dr.getDependencies(node.getUniqueName(), node.getType(), gb.revision, true);
          for (DependencyNode dep : deps) {
            if (dep.getType() == DependencySourceType.WORKFLOW) {
              try {
                dependencies.add(new WorkflowRevision(transformNameForJava(dep.getUniqueName()), dep.getRevision()));
              } catch (XPRC_InvalidPackageNameException e) {
                // if not a valid package name there should be nothing to protect
              }
            }
          }
        }
        if (gb instanceof WF) {
          try {
            dependencies.add(new WorkflowRevision(transformNameForJava(gb.getOriginalFqName()), gb.getRevision()));
          } catch (XPRC_InvalidPackageNameException e) {
            // if not a valid package name there should be nothing to protect
          }
        }
      }
    }
  }


  private static void fillVarsInCorrectOrder(SortedSet<GenerationBase> objects) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.fillVars();
      }
    });
  }

  private void fillVarsInCorrectOrder() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                  XPRC_MDMDeploymentException {
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      gb.fillVars();
    }
  }


  private void fillVars() throws AssumedDeadlockException {
    //rekursiv für alle abhängigen objekte aufrufen: fillVars
    executeJobAndIncrementDeploymentState(DeploymentState.fillVars, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (!mode.fillVars(doesntExist)) {
          return;
        }

        fillVarsInCorrectOrder();
        if (dependentObjectHadError()) {
          return;
        }

        try {
          fillVarsInternally();
        } catch (XynaException e) {
          throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
        }
      }
    });
  }


  private static void validateInCorrectOrder(SortedSet<GenerationBase> objects) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.validate();
      }
    });
  }

  private void validateInCorrectOrder() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                  XPRC_MDMDeploymentException {
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      gb.validate();
    }
  }


  private void validate() throws AssumedDeadlockException {
    executeJobAndIncrementDeploymentState(DeploymentState.validate, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (doesntExist) {
          throw new XPRC_XMOMObjectDoesNotExist(getOriginalFqName());
        }
        if (mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML) {
          return;
        }

        if (mode.shouldValidate) {
          try {
            validateInternally();
          } catch (XPRC_DuplicateVariableNamesException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_MdmDeploymentCyclicInheritanceException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_MayNotOverrideFinalOperationException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_EmptyVariableIdException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidXmlChoiceHasNoInputException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_MissingServiceIdException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidVariableIdException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_ParsingModelledExpressionException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidXMLMissingListValueException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_MISSING_ATTRIBUTE e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_JAVATYPE_UNSUPPORTED e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidExceptionVariableXmlMissingTypeNameException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidXmlMethodAbstractAndStaticException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_InvalidExceptionXmlInvalidBaseReferenceException e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_MEMBER_DATA_NOT_IDENTIFIED e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          } catch (XPRC_PrototypeDeployment e) {
            throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
          }
        }
        validateInCorrectOrder();
      }
    });
  }



  private static void cleanupInCorrectOrder(SortedSet<GenerationBase> objects) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.cleanup();
      }
    });
  }

  /**
   * in dieser reihenfolge werden deadlocks durch unterschiedliche reihenfolgen in der rekursion ausgeschlossen
   */
  private void cleanupInCorrectOrder() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                  XPRC_MDMDeploymentException {
    for (GenerationBase gb : getDepsLazyCreateSafely(true).getDependencies(true)) {
      gb.cleanup();
    }
  }


  private void cleanup() throws AssumedDeadlockException {
    //instance-hashmap aufräumen
    //ggfs backup löschen
    executeJobAndIncrementDeploymentState(DeploymentState.cleanup, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
          XPRC_MDMDeploymentException {
        if (mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables) {
          clearFromCache();
          return;
        }

        if (!invalidated) {
          Optional<Throwable> exception = Optional.empty();
          DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
          if (dism != null) {
            DeploymentTransition transition = DeploymentTransition.SUCCESS;
            if (mode == DeploymentMode.deployBackup) {
              transition = DeploymentTransition.SUCCESSFULL_ROLLBACK;
            }
            dism.deployFinished(GenerationBase.this.originalFqName, transition, mode.shouldCopyXMLFromSavedToDeployed, exception, revision);
          }
        }

        cleanupInCorrectOrder();

        if (invalidated) {
          //onError macht den rest
          return;
        }

        //erst machen, wenn alles andere gutgegangen ist, damit man ggfs wieder zurück kann.
        if (backupXml != null && backupXml.exists()) {
          if (!FileUtils.deleteFileWithRetries(backupXml)) {
            logger.warn("backup could not be deleted " + backupXml.getAbsolutePath());
          }
          backupXml = null;
        }

        if (serviceLibsBackup != null) {
          serviceLibsBackup.remove();
          serviceLibsBackup = null;
        }
    
        // erst am ende, damit nicht ein anderes objekt auf dateien zugreift, die hier noch bearbeitet werden
        clearFromCache();

        //nicht im finally machen, damit bei fehlern onerror nicht schiefgeht.
        if (mode.shouldCopyXMLFromSavedToDeployed && 
            xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace) &&
            !isReservedServerObject()) {
          DeploymentLocks.writeUnlock(getOriginalFqName(), getDependencyType(), revision);
        }
      }
    });
  }


  private static void generateJavaInCorrectOrder(SortedSet<GenerationBase> objects) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.generateJava();
      }
    });
  }

  private void generateJavaInCorrectOrder() throws XPRC_InheritedConcurrentDeploymentException,
                  AssumedDeadlockException, XPRC_MDMDeploymentException {
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      gb.generateJava();
    }
  }


  private void generateJava() throws AssumedDeadlockException {
    // nur falls mode == direct
    // falls javagen im speicher, dann auch bei indirect.
    // rekursiv für alle abhängigen objekte aufrufen: generateJava (indirect)
    executeJobAndIncrementDeploymentState(DeploymentState.generateJava, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables) {
          return;
        }

        generateJavaInCorrectOrder();
        if (dependentObjectHadError()) {
          return;
        }

        if (!isReservedServerObject()) {
          if (mode.shouldGenerateJava) {
            CodeBuffer cb = new CodeBuffer("Processing");
            try {
              String[] classes = generateJavaInternally(cb, false);
              generatedJava = new JavaSourceFromString(fqClassName, fqClassName, classes[0], revision);
              String outputlocation = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision);
              if (mode == DeploymentMode.generateMdmJar) {
                outputlocation += ".tmp";
              }
              generatedJava.setClassOutputLocation(outputlocation);
            } catch (XynaException e) {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
            }
          }
        }
      }
    });
  }
  
  

  private static void compileInCorrectOrder(SortedSet<GenerationBase> objects, final InMemoryCompilationSet cs, final boolean collectCompileTargetsOnly) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.compile(cs, collectCompileTargetsOnly);
      }
    });
  }

  private void compileInCorrectOrder(InMemoryCompilationSet cs, boolean collectCompileTargetsOnly) throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                  XPRC_MDMDeploymentException {
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      gb.compile(cs, collectCompileTargetsOnly);
    }
  }


  private void compile(final InMemoryCompilationSet cs, final boolean collectOnly) throws AssumedDeadlockException {
    // nicht rekursiv aufrufen ausser für abhaengige workflows, weil das java-compile automatisch
    // abhängige objekte mit kompiliert.
    executeJobAndIncrementDeploymentState(collectOnly ? DeploymentState.bulkCompile : DeploymentState.compile, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables) {
          return;
        }

        compileInCorrectOrder(cs, collectOnly); //PERFORMANCE: hier muss man eigtl nicht alle überprüfen
        if (dependentObjectHadError()) {
          return;
        }

        if (mdmObjectMappingToJavaClasses.containsKey(getOriginalFqName())) {
          return;
        }

          if (!(GenerationBase.this instanceof WF)) {
            //evtl additional dependency oder sowas, was noch nicht kompiliert wurde
            String fileLocation = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + Constants.fileSeparator
                            + getFqClassName().replaceAll("\\.", Constants.fileSeparator);
            File f = new File(fileLocation + ".class");
            if (f.exists() && !mode.shouldDoCompile()) {
              //nichts zu tun
            } else if (mode.shouldDoCompile()) {
              //wird gleich kompiliert
            } else {
              // class existiert nicht und soll auch nicht erstellt werden.
              if (logger.isDebugEnabled()) {
                  logger.debug("class file for object " + getOriginalFqName()
                               + " doesn't exist and should not be compiled because of mode (=" + mode.toString()
                               + ").");
              }
            }
          }

          if (mode.shouldDoCompile()) {
            // won't work for scripting but is not needed
            String classFileName = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + Constants.fileSeparator + originalFqName.replace(".", "/") + ".class").getPath();
            boolean compileError = false;
            long timeStampBeforeCompile = determineLastModified(classFileName);
            try {
              HashSet<String> jars = new HashSet<String>();
              if (GenerationBase.this instanceof DOM) { // wfs haben keine abhängigen jars
                // tryFromSaved = mode.shouldCopyXMLFromSavedToDeployed: impls are copied during deploymentHandler execution
                //                                                       saved does currently contain the jar to compile against
                DOM dom = ((DOM) GenerationBase.this);
                while (dom != null) {
                  dom.getDependentJarsWithoutRecursion(jars, true, dom.mode.shouldCopyXMLFromSavedToDeployed);                  
                  dom = dom.getSuperClassGenerationObject();
                }
              }
              
              Set<Long> revisions = xmlInputSource.getDependenciesRecursivly(revision);
              revisions.add(revision);
              
              if (XynaFactory.isFactoryServer()) { // no XMOMCLASSES present on script access
                for (Long rev : revisions) {
                  File mdmclasses = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, rev));
                  if (!mdmclasses.exists()) {
                    mdmclasses.mkdir();
                  }
                  jars.add(mdmclasses.getPath());
                }
              }

              for (String s : jars) {
                cs.addToClassPath(s);
              }
              
              cs.addToCompile(generatedJava);
              if (!collectOnly) {
                try {
                  cs.compile();
                } finally {
                  compileError = cs.getUnsuccessfullyCompiled() == null || cs.getUnsuccessfullyCompiled().containsKey(fqClassName);
                  cs.clear();
                }
              }
            } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
            } catch (XPRC_JarFileForServiceImplNotFoundException e) {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
            }
            
            long timeStampAfterCompile = determineLastModified(classFileName);
            if(!collectOnly && generatedJava != null && timeStampAfterCompile == timeStampBeforeCompile && !compileError) {
              if(logger.isDebugEnabled()) {
                logger.debug("successful compilation did not update classfile '" + classFileName + "'.");
              }
              if(XynaProperty.EXCEPTION_ON_DEPLOY_NO_CLASSFILE_UPDATE.get()) {
                throw new XPRC_MDMDeploymentException(getOriginalFqName(), new RuntimeException("successful compilation did not update classfile '" + classFileName + "'."));
              }
            }
            
          }
      }

    });
  }

  public Set<Long> collectDependentRevisions() {
    Set<Long> revisions = new HashSet<Long>();
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      revisions.add(gb.revision);
    }
    return revisions;
  }

  private static void onDeploymentHandlerInCorrectOrder(SortedSet<GenerationBase> objects, final int prio) {
    executeStep(objects, new DeploymentStep() {
      public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException {
        gb.onDeploymentHandler(prio);
      }
    });
  }

  private void onDeploymentHandlerInCorrectOrder(final int prio) throws XPRC_InheritedConcurrentDeploymentException,
                  AssumedDeadlockException, XPRC_MDMDeploymentException {
    for (GenerationBase gb : dependencies.getDependencies(true)) {
      gb.onDeploymentHandler(prio);
    }
  }


  private void onDeploymentHandler(final int prio) throws AssumedDeadlockException {

    // immer aufrufen, rekursiv für alle abhängigen objekte: onDeploy(indirect)
    DeploymentState targetState = DeploymentState.getTargetStateForDeploymentHandler(prio);
    executeJobAndIncrementDeploymentState(targetState, new StateTransition() {

      public void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                      XPRC_MDMDeploymentException {
        if (mode == DeploymentMode.doNothing) {
          //serverinterne objekte bei deploymenthandlern registrieren. TODO schöner wäre, wenn man den deploymenthandler fragen könnte, ob er serverreservierte objekte behandeln will.
          if (prio == DeploymentHandling.PRIORITY_DEPENDENCY_CREATION && isReservedServerObject() /*&& !reservedObjectIsInitialized()*/) { // TODO reservedObjectIsInitialized requires a fix for the current calling pattern
            onDeploymentHandlerInCorrectOrder(prio);
            try {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                  .executeDeploymentHandler(prio, GenerationBase.this, mode);
            } catch (XPRC_DeploymentHandlerException e) {
              logger.warn("Couldn't register internal object in dependency register: " + originalFqName);
            }
          }
          if (prio == DeploymentHandling.PRIORITY_EXCEPTION_DATABASE && isReservedServerObject() /*&& !reservedObjectIsInitialized()*/) {
            onDeploymentHandlerInCorrectOrder(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE);
            try {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                  .executeDeploymentHandler(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE, GenerationBase.this, mode);
            } catch (XPRC_DeploymentHandlerException e) {
              logger.warn("Couldn't register internal object in exception database: " + originalFqName);
            }
          }
          return;
        }
        if (mode == null || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables) {         
          return;
        }
        generatedJava = null;

        if (mode.mustExecuteDeploymentHandler()) { //falls einmal false, sind auch alle abhängigen objekte false! das gilt zumindest für "mustExecuteDeploymentHandler"

          if (!hasRegisteredDeploymentHandler) {
            synchronized (GenerationBase.class) {
              if (!hasRegisteredDeploymentHandler) {
                XynaFactoryBase factory = XynaFactory.getInstance();
                if (!XynaFactory.isInstanceMocked()) {
                  factory.getProcessing().getWorkflowEngine().getDeploymentHandling().addDeploymentHandler(
                                                                                                    DeploymentHandling.PRIORITY_EXCHANGE_ADDITIONAL_LIBS,
                                                                                                    new CopyAndBackupLibrariesAtAppropriateDeploymentHandlingPriority());
                  hasRegisteredDeploymentHandler = true;
                } else {
                  try {
                    // execute copy directly if mocked, ExecutionTime.update might override mode to new and wouldn't copy impls
                    if (prio == DeploymentHandling.PRIORITY_EXCHANGE_ADDITIONAL_LIBS) {
                      new CopyAndBackupLibrariesAtAppropriateDeploymentHandlingPriority().exec(GenerationBase.this, mode);
                    }
                  } catch (XPRC_DeploymentHandlerException e) {
                    throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
                  }
                }
              }
            }
          }

          onDeploymentHandlerInCorrectOrder(prio);
          if (dependentObjectHadError()) {
            return;
          }

          List<XPRC_DeploymentHandlerException> exceptions = null;
          final ReadLock lock = classLock.readLock();
          lock.lock(); //classloaderdeploymenthandler greift auf classfiles zu.
          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                            .executeDeploymentHandler(prio, GenerationBase.this, mode);
          } catch (XPRC_DeploymentHandlerException e) {
            if(mode == DeploymentMode.deployBackup) {
              if(exceptions == null) {
                exceptions = new ArrayList<XPRC_DeploymentHandlerException>();
              }
              exceptions.add(e);
            } else {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), e);
            }
          } finally {
            lock.unlock();
          }
          if (exceptions != null) {
            if (exceptions.size() == 1) {
              throw new XPRC_MDMDeploymentException(getOriginalFqName(), exceptions.get(0));
            }
            throw (XPRC_MDMDeploymentException) new XPRC_MDMDeploymentException(getOriginalFqName()).initCauses(exceptions
                .toArray(new XPRC_DeploymentHandlerException[exceptions.size()]));
          }
        }
      }


    });
  }

  private boolean shouldExceptionBePropagated() {
    return false; //ist das so ok? vgl bug 19020, 19003
  }

  private boolean dependentObjectHadError() {
    for (GenerationBase gb : getDepsLazyCreateSafely(true).getDependencies(false)) {
      if (gb == this) {
        continue;
      }
      if (gb.invalidated || gb.state == DeploymentState.error) {
        if (gb.shouldExceptionBePropagated()) {
          return true;
        }
      }
    }
    return false;
  }


  private static void onErrorInCorrectOrder(SortedSet<GenerationBase> objects) {
    List<GenerationBase> backupDeploymentSet = new ArrayList<GenerationBase>();
    for (GenerationBase gb : objects) {
      try {
        gb.onError(backupDeploymentSet);
      } catch (Throwable t) {
        //sollte eigentlich nicht auftreten, aber auf jeden Fall onError für alle Objekte durchführen
        Department.handleThrowable(t);
        gb.exceptionsWhileOnError.add(t);
      }
    }
    deployBackup(backupDeploymentSet);
  }


  private static void deployBackup(List<GenerationBase> backupDeploymentSet) {
    try {
      deploy(backupDeploymentSet, DeploymentMode.deployBackup, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
    } catch (MDMParallelDeploymentException e) {
      for (GenerationBase failedObject : e.failedObjects) {
        File fileWithinDeploymentDirectory = failedObject.xmlInputSource.getFileLocation(failedObject.getOriginalFqName(), failedObject.getRevision(), true);
        File errorFile = new File(fileWithinDeploymentDirectory.getAbsolutePath() + ".error" + System.currentTimeMillis());
        try {
          FileUtils.moveFile(fileWithinDeploymentDirectory, errorFile);
        } catch (Ex_FileAccessException f) {
          logger.error("error trying to copy file " + fileWithinDeploymentDirectory.getAbsolutePath(), f);
        }
        if (!fileWithinDeploymentDirectory.exists()) { //only delete classFiles if moveFile was successfull
          failedObject.deleteClassFiles();
        }
        failedObject.exceptionsWhileOnError.add(e);
      }
    } catch (XPRC_DeploymentDuringUndeploymentException e) {
      throw new RuntimeException(e); //unexpected
    }
  }
  
  private void onErrorInCorrectOrderAndBackupDeployment() throws XPRC_DeploymentCleanupException {
    List<GenerationBase> backupDeploymentSet = new ArrayList<GenerationBase>();
    onErrorInCorrectOrder(backupDeploymentSet);
    deployBackup(backupDeploymentSet);
  }
  
  private void onErrorInCorrectOrder(List<GenerationBase> backupDeploymentSet) throws XPRC_DeploymentCleanupException {
    Set<Throwable> errorsWhileOnError = new HashSet<Throwable>();
    for (GenerationBase gb : getDepsLazyCreateSafely(true).getDependencies(true)) {
      try {
        gb.onError(backupDeploymentSet);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        errorsWhileOnError.add(t);
      }
    }
    if (errorsWhileOnError.size() > 0) {
      if (errorsWhileOnError.size() == 1) {
        throw new XPRC_DeploymentCleanupException(getOriginalFqName(), errorsWhileOnError.iterator().next());
      }
      throw (XPRC_DeploymentCleanupException) new XPRC_DeploymentCleanupException(getOriginalFqName())
                      .initCauses(errorsWhileOnError.toArray(new Throwable[errorsWhileOnError.size()]));
    }
  }

  private void onError(final List<GenerationBase> backupDeploymentSet) throws AssumedDeadlockException {
    // ggfs backup einspielen
    // rekursiv für alle abhängigen objekte aufrufen: onError (indirect)
    StateTransition st = new StateTransition() {

      public void exec() throws XPRC_BackupFileException, XPRC_DeploymentCleanupException {
        if (mode == null || mode == DeploymentMode.doNothing || mode == DeploymentMode.fromXML || mode == DeploymentMode.fromXMLWithFillVariables || doesntExist) {
          clearFromCache();
          return;
        }
        try {

          DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
          if (dism != null) {
            Optional<Throwable> exception;
            if (exceptions != null && exceptions.size() > 0) {
              exception = Optional.of(exceptions.get(exceptions.size() - 1));
            } else {
              exception = Optional.<Throwable>of(new XPRC_MDMDeploymentException(GenerationBase.this.fqClassName));
            }
            if (mode == DeploymentMode.deployBackup) {
              dism.deployFinished(originalFqName, DeploymentTransition.ERROR_DURING_ROLLBACK, mode.shouldCopyXMLFromSavedToDeployed, exception, revision);
            } else {
              dism.deployFinished(originalFqName, DeploymentTransition.ROLLBACK, mode.shouldCopyXMLFromSavedToDeployed, exception, revision);
            }
          }

          boolean rollbackXmlOnly = !stateBeforeError.hasRun(DeploymentState.bulkCompile);

          if (mode.mustExecuteDeploymentHandler && !rollbackXmlOnly) {
            //falls es bereits objekte gibt, die dieses objekt verwenden, gibt es classloading-dependencies darauf. die sollen nicht verschwinden
            //zumindest dann nicht, wenn man davon ausgeht, dass ein weiteres deploybackup passiert, was erfolgschancen hat.

            //TODO vielleicht sollte man besser die undeploymenthandler entsprechend klassifizieren (technisch notwendig, wenn ein objekt nicht deployed werden kann
            //     vs benutzer will es undeployen)
            if (mode == DeploymentMode.deployBackup || mode == DeploymentMode.reload || mode == DeploymentMode.reloadWithXMOMDatabaseUpdate
                || mode == DeploymentMode.regenerateDeployedAllFeatures || mode == DeploymentMode.regenerateDeployedAllFeaturesXmlChanged 
                || mode == DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps 
                || backupXml != null || backupExistedBefore) {
              //objekt war ehemals vorhanden, nun ist ein inkonsistenter zustand entstanden. andere objekte benötigen this ja noch!

              //bei einem fixenden redeployment von this gibt es das objekt zwar wieder, aber die classloading-deps können nicht mehr
              //korrekt (für die anderen verwendenden objekte) hergestellt werden!
              // -> classloading-deps jetzt merken
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                  .backupClassloadingDeps(GenerationBase.this);
            }
            //classloading inkonsistenzen aufräumen. andere undeploymenthandler nicht aufrufen, weil der benutzer das objekt nicht undeployen wollte
            try {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                  .executeUndeploymentHandler(DeploymentHandling.PRIORITY_CLASS_LOADER_UNDEPLOY_OLD, GenerationBase.this);
            } catch (Throwable e) {
              //runtimeexceptions und userdefined exceptions nur loggen, backup deployment soll trotzdem versucht werden
              logger.warn("Could not cleanup classloading related stuff for " + getOriginalFqName(), e);
            }
          }

          boolean deployBackup = false;
          if (backupXml != null) {
            deployBackup = true;
            //backup restore
            File f = xmlInputSource.getFileLocation(getOriginalFqName(), getRevision(), true);
            try {
              if (logger.isDebugEnabled()) {
                logger.debug("restoring backup for " + getOriginalFqName());
              }
              FileUtils.moveFile(backupXml, f);
              backupXml = null;
              if (serviceLibsBackup != null) {
                serviceLibsBackup.rollback();
                serviceLibsBackup = null;
              }
            } catch (Ex_FileAccessException e) {
              throw new XPRC_BackupFileException(getOriginalFqName(), e);
            }

            if (rollbackXmlOnly) {
              //es genügt einfach das xml wieder zu rollbacken, sonst wurde noch nichts geändert, was man rollbacken müsste
              deployBackup = false;
            }
          } else if (mode == DeploymentMode.codeNew) {
            //TODO undeploymenthandler durchführen, falls deployment passiert ist...
            deleteXMLAtDeploymentLocation();
          }

          GenerationBase instanceForBackupDeployment = null;
          if (deployBackup) {
            instanceForBackupDeployment = GenerationBase.this.replaceMeInCacheWithNewInstance();
            instanceForBackupDeployment.setDeploymentComment("Backup deployment of " + getOriginalFqName() + " after deployment error."
                + (comment == null ? "" : " Original comment: " + comment));
          } else {
            clearFromCache(); //spezial dependencies rauswerfen
          }
          onErrorInCorrectOrder(backupDeploymentSet); //wenn die methode beendet ist, haben auch alle abhängigen objekte ihr backup-xml restored.

          if (deployBackup) {
            backupDeploymentSet.add(instanceForBackupDeployment);
          }
        } finally {
          if (previousState.hasRun(DeploymentState.copyXml) &&
              mode != null && 
              mode.shouldCopyXMLFromSavedToDeployed() && 
              xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace) && 
              !isReservedServerObject()) {
            DeploymentLocks.writeUnlock(getOriginalFqName(), getDependencyType(), revision);
          }
        }
      }

    };

    executeJobAndIncrementDeploymentState(DeploymentState.error, st);
  }


  private abstract class StateTransition {

    public DeploymentState previousState;

    public abstract void exec() throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
                    XPRC_MDMDeploymentException;
  }

  private interface DeploymentStep {

    public void exec(GenerationBase gb) throws AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InheritedConcurrentDeploymentException;
  }

  public static class AssumedDeadlockException extends XynaException {

    private static final long serialVersionUID = -6209917902553533017L;


    public AssumedDeadlockException(String s) {
      super(s);
    }

  }

  public static class EmptyException extends Exception {

    private static final long serialVersionUID = 1L;
  }


  public static class MDMParallelDeploymentException extends XPRC_MDMParallelDeploymentException {

    private static final long serialVersionUID = 1L;

    private transient List<GenerationBase> failedObjects;
    private List<Pair<String, SerializableExceptionInformation>> serializableFailedObjects;
    private transient Map<String,SerializableExceptionInformation> serializableFailedObjectsAsMap;
    
    public MDMParallelDeploymentException(Set<GenerationBase> failedObjects) {
      super(failedObjects.size());
      this.failedObjects = new ArrayList<GenerationBase>(failedObjects);
      
      List<Throwable> ts = new ArrayList<Throwable>();
      for( GenerationBase gb : this.failedObjects ) {
        ts.add( gb.getExceptionCause() );
      }
      
      initCauses(ts.toArray( new Throwable[ts.size()]));
    }

    public List<GenerationBase> getFailedObjects() {
      return failedObjects;
    }
    
    public Map<String,SerializableExceptionInformation> getExceptionInformationAsMap() {
      if( serializableFailedObjectsAsMap == null ) {
        serializableFailedObjectsAsMap = new HashMap<String,SerializableExceptionInformation>();
        if( serializableFailedObjects != null ) {
          for (Pair<String, SerializableExceptionInformation> pair : serializableFailedObjects) {
            serializableFailedObjectsAsMap.put( pair.getFirst(), pair.getSecond() );
          }
        }
      }
      return serializableFailedObjectsAsMap;
    }
    
    public List<Pair<String, SerializableExceptionInformation>> getSerializableFailedObjects() {
      return serializableFailedObjects;
    }

    public void generateSerializableFailedObjects() {
      serializableFailedObjects = new ArrayList<Pair<String, SerializableExceptionInformation>>();
      for (GenerationBase failed : failedObjects) {
        SerializableExceptionInformation ex = null;
        Throwable exceptionCause = failed.getExceptionCause();
        if (exceptionCause != null) {
          if ((exceptionCause instanceof XPRC_MDMDeploymentException || exceptionCause instanceof XPRC_InheritedConcurrentDeploymentException) && exceptionCause.getCause() != null) {
            exceptionCause = exceptionCause.getCause();
          }
          ex = new SerializableExceptionInformation(exceptionCause);
          serializableFailedObjects.add(Pair.of(failed.getOriginalFqName(), ex));
          while (true) {
            exceptionCause = exceptionCause.getCause();
            if (exceptionCause == null) {
              break;
            }
            SerializableExceptionInformation parent = ex;
            ex = new SerializableExceptionInformation(exceptionCause);
            parent.setCause(ex);
          }
        } else {
          serializableFailedObjects.add(Pair.of(failed.getOriginalFqName(), ex));
        }
      }
    }

    public String getMessage() {
      StringBuilder sb = new StringBuilder(super.getMessage());
      /*
       * During deployment, an error has occurred in 3 objects.
       * <objektname>: fehler
       *   Caused by: fehler2
       *   Caused by: fehler3
       * <objektname2>: fehler
       * ...
       */
      if (serializableFailedObjects != null) {
        int i=1;
        for (Pair<String, SerializableExceptionInformation> pair : serializableFailedObjects) {
          appendFailed(sb, i, pair.getFirst(), pair.getSecond() );
          ++i;
        }
      } else if (failedObjects != null) {
        int i=1;
        for (GenerationBase failed : failedObjects) {
          appendFailed( sb, i, failed);
          ++i;
        }
      }
      return sb.toString();
    }

    private void appendFailed(StringBuilder sb, int idx, String name, SerializableExceptionInformation sei) {
      sb.append("\n").append(idx).append(". ").append(name).append(": \n   ");
      SerializableExceptionInformation e = sei;
      String sep = "";
      while (e != null ) {
        String msg;
        if (e.getMessage() == null) {
          msg = "null";
        } else {
          msg = e.getMessage().replace("\n", "\n   ");
        }
        sb.append(sep).append(e.getClassName()).append(" ").append(msg);
        e = e.getCause();
        sep = "\n  Caused by: ";
      }
    }
    
    private void appendFailed(StringBuilder sb, int idx, GenerationBase failed) {
      sb.append("\n").append(idx).append(". ").append(failed.getOriginalFqName()).append(": \n   ");
      Throwable t = failed.getExceptionCause();
      if (t == null) {
        sb.append("unknown");
      }
      String sep = "";
      while (t != null) {
        sb.append(sep).append(t.getClass().getName()).append(" ");
        if( t.getMessage() == null ) {
          sb.append("no message");
        } else {
          sb.append( t.getMessage().replace("\n", "\n   ") );
        }
        t = t.getCause();
        sep = "\n  Caused by: ";
      }
    }
    
    
  }

  /**
   * Führt einen Schritt des Deplyoments für alle übergebenen GenerationBase-Objekte durch.
   * Tritt bei einem Objekt ein Fehler auf, so wird dieser gefangen und das Objekt invalidiert,
   * so dass der Schritt für die anderen Objekte auch noch durchgeführt werden kann.
   */
  private static void executeStep(Collection<GenerationBase> objects, DeploymentStep step) {
    for (GenerationBase gb : objects) {
      try {
        step.exec(gb);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        gb.invalidate();
        if (logger.isTraceEnabled()) {
          logger.trace("invalidated " + gb.getOriginalFqName(), t);
        }
        gb.exceptions.add(t);
      }
    }
  }


  private static final WeakIdentityHashMap<GenerationBaseCache, Long> cacheIds =
      new WeakIdentityHashMap<GenerationBaseCache, Long>();


  /**
   * führt den job aus, führt den statusübergang bei erfolgreicher job ausführung durch und verhindert,
   * dass andere threads in die quere kommen. falls der statusübergang bereits (von einem anderen thread)
   * durchgeführt wurde, wird nichts gemacht. falls ein anderer thread dieses objekt in errorstatus
   * überführt hat, wird ein entsprechender fehler geworfen.
   *
   * threadsafe
   */
  private void executeJobAndIncrementDeploymentState(DeploymentState targetState, StateTransition stateTransitionJob)
      throws AssumedDeadlockException {

    if (alreadyInvalidated_NoExecutionNecessary(targetState))  {
      //bereits ungültig. später cleanup/errorhandling durchführen
      return;
    }
    if (targetState == DeploymentState.error && !invalidated) {
      //vorher wurde bereits cleanup ausgeführt...
      return;
    }

    try {
      if (state.hasRunAndHasNoError(targetState)) {
        return;
      }
    } catch (StateIsErrorException e) {
      return;
    }

    try {
      boolean gotLock = false;
      int retryCount = 0;
      while (!gotLock && retryCount < 20) {
        retryCount++;
        //hier könnte ein deadlock passieren.
        //beispiel: zyklische abhängigkeiten A -> B -> A. deploy(A) und deploy(B) simultan
        //          deploy(A) lockt A und wartet auf B, deploy(B) lockt B und wartet auf A.

        //handling von deadlocks:
        //falls bei parsexml ein deadlock passiert, wird es einfach nochmal versucht.
        //nachdem parsexml erfolgreich war, werden alle folgenden state-transitions in der
        //gleichen reihenfolge über die menge aller dependencies (inkl dem objekt selbst) durchgeführt.
        //(bei parsexml ist das noch nicht möglich, weil die dependencies dort erst ermittelt werden)
        //in dem obigen beispiel heisst das, dass deploy(A) und deploy(B) in beiden threads in der gleichen
        //reihenfolge durchgeführt werden, und deshalb keine deadlocks passieren können.
        //(unter last kann es aber sehr wohl passieren, dass die threads mal warten müssen. dann sollen
        //sie aber keinen deadlock fehler werfen, sondern weiter warten)
        long lockTimeout;
        if (targetState.supportsDeadLockDetection()) {
          lockTimeout = 1000 + random.nextInt(3000);
        } else {
          lockTimeout = 30 * 1000 + random.nextInt(3000); //10 min, weil 20 mal das ganze
        }
        gotLock = stateLock.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
        if (!gotLock && targetState.supportsDeadLockDetection()) { //handlebar: oben gibt es eine whileschleife bei parsexmlwithdeadlockdetection
          throw new AssumedDeadlockException("dead lock occurred, please try again. [" + originalFqName + "]");
        }
      }
      if (!gotLock) { //nicht handlebar, sollte nicht passieren
        //TODO kann auch passieren, wenn ein anderer thread zb wegen I/O problemen nicht fertig wird. passiert
        //in der entwicklung zb gelegentlich, wenn afs hängt.
        throw new AssumedDeadlockException("dead lock occurred, please try again. [" + originalFqName + "]");
      }
    } catch (InterruptedException e1) {
      throw new RuntimeException("could not get lock", e1);
    }
    try {
      // nochmal überprüfen, für den fall, dass thread gerade warten musste. dann ist das bereits geschehen
      if (alreadyInvalidated_NoExecutionNecessary(targetState)) {
        //bereits ungültig. später errorhandling durchführen
        return;
      }

      try {
        if (state.hasRunAndHasNoError(targetState)) {
          return;
        }
      } catch (StateIsErrorException e) {
        return;
      }
      // state == running => der gleiche thread bearbeitet das objekt bereits und ist durch rekursion erneut hier angekommen.
      // ein anderer thread kann nicht auf running stehen, weil dann das synchronized diesen thread nicht durchgelassen hätte.
      if (state.isRunning()) {
        return;
      }

      DeploymentState oldState = state;
      if (state.mayRun(targetState, mode)) {
        state = DeploymentState.beginRun(targetState);
        boolean gotException = true;
        try {
          if (mode != null) {
            if (logger.isDebugEnabled()) {
              Long cId = getCacheId();
              logger.debug("deploying " + this.toString() + " revision: " + revision + " state: "
                  + oldState.toString() + " => " + state.toString() + " mode=" + mode.toString() + " cache=" + cId);
            }
          } else if (oldState == DeploymentState.init) {
            //vor der initialisierung ist der mode noch nicht gesetzt
          } else {
            throw new RuntimeException("deployment mode is unexpectedly null for " + getOriginalFqName() + " in state=" + state.toString());
          }
          stateTransitionJob.previousState = oldState;
          stateTransitionJob.exec();
          gotException = false;
          state = state.finishRun();
          if (state.isNotFurtherThan(DeploymentState.cleanup) && !invalidated) { //cleanup wird auch für fehlerhafte objekte aufgerufen, nachdem der fehler passiert war
            stateBeforeError = state;
          }
        } finally {
          if (gotException) {
            state = oldState;
          }
        }
      } else {
        throw new RuntimeException("targetState " + targetState.toString() + " not allowed in state " + state.toString() + " for "
            + getOriginalFqName());
      }

      if (state.hasRun(mode.getLastParsingStep())) {
        //vorher passiert noch parsing - da ist es blöd, wenn der fehler sich vererbt, weil die folge-parsing schritte dann nicht ausgeführt werden
        if (dependentObjectHadError()) {
          if (state != DeploymentState.error) {
            invalidate();
            if (logger.isTraceEnabled()) {
              logger.trace("invalidated " + getOriginalFqName() + " because a dependent object is invalidated.");
            }
          }
        }
      }
    } catch (AssumedDeadlockException e) {
      if (targetState.supportsDeadLockDetection()) {
        //deadlock retries
        throw e;
      } else {
        if (targetState != DeploymentState.error) {
          invalidate();
        } else {
          state = DeploymentState.error;
        }
        //enthält maximal 2 objekte: erste exception in der "hinrichtung" beim deployment. zweite exception maximal wenn beim
        // deploymentstate-job nach error nochmal ein fehler auftritt
        if (logger.isTraceEnabled()) {
          logger.trace("invalidated " + getOriginalFqName(), e);
        }
        exceptions.add(e);
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      if (targetState != DeploymentState.error) {
        invalidate();
      } else {
        state = DeploymentState.error;
      }
      if (logger.isTraceEnabled()) {
        logger.trace("invalidated " + getOriginalFqName(), t);
      }
      if (t instanceof StackOverflowError) {
        logger.error(null, t);
      }
      exceptions.add(t);
    } finally {
      stateLock.unlock();
    }
  }


  private boolean alreadyInvalidated_NoExecutionNecessary(DeploymentState targetState) {
    return invalidated && (targetState != DeploymentState.error && targetState != DeploymentState.cleanup);
  }


  private Long getCacheId() {
    Long cId = -1L;
    if (cacheReference != null) {
      synchronized (cacheIds) {
        cId = cacheIds.get(cacheReference);
        if (cId == null) {
          while (true) {
            if (cacheIds.size() == 0) {
              cId = 0L;
              break;
            } else {
              try {
                cId = Collections.max(cacheIds.values()) + 1;
                break;
              } catch (Exception e) { //NoSuchElementException, wegen GC, ConcurrentModificationException ?, NullPointerException ?
                logger.trace(null, e);
              }
            }
          }
          cacheIds.put(cacheReference, cId);
        }
      }
    }
    return cId;
  }


  private void invalidate() {
    invalidated = true;
  }



  public static String getXmlNameForReservedClass(Class<?> c) {
    String name = mdmObjectMappingToJavaClasses.getInverse(c);
    if (name == null) {
      throw new RuntimeException("class " + c.getName() + " not found as reserved server class.");
    }
    return name;
  }

  protected static GenerationBase tryGetGlobalCachedInstance(String fqXmlName, Long revision) {
    return globalCache.getFromCache(fqXmlName, revision);
  }


  protected static void cacheGlobal(GenerationBase o) {
    globalCache.insertIntoCache(o);
  }


  public static void clearGlobalCache() {
    if (logger.isTraceEnabled()) {
      logger.trace("generationbase.cache contained " + globalCache.getNumberOfRevisions() + " revisions and " + globalCache.size() + " elements and will now be cleared.");
    }
    globalCache.clear();
    parseAdditionalCache.clear();
  }

  private boolean clearFromCache() {
    boolean forceRemove = mode == DeploymentMode.regenerateDeployedAllFeatures
        || mode == DeploymentMode.codeUnchangedClearFromCache 
        || mode == DeploymentMode.regenerateDeployedAllFeaturesXmlChanged 
        || mode == DeploymentMode.regenerateDeployedAllFeaturesCollectSpecialDeps;    
    /*
     * mache nichts, wenn (forceremove = false && removefromcache = false) == (!forceremove && !removefromcache) == !(forceremove || removefromcache)
     */
    if (!(forceRemove || removeFromCache)) {
      return true;
    }
    boolean ret = cacheReference.remove(this);
    if (logger.isTraceEnabled()) {
      logger.trace("generationbase.cache contains " + cacheReference.size() + " elements");
    }
    return ret;
  }


  protected GenerationBase replaceMeInCacheWithNewInstance() {
    GenerationBase newInstance;
    if (this instanceof DOM) {
      newInstance = new DOM(originalFqName, fqClassName, cacheReference, revision, null, xmlInputSource);
    } else if (this instanceof WF) {
      newInstance = new WF(originalFqName, fqClassName, cacheReference, revision, null, xmlInputSource);
    } else if (this instanceof ExceptionGeneration) {
      newInstance = new ExceptionGeneration(originalFqName, fqClassName, cacheReference, revision, null, xmlInputSource);
    } else {
      throw new RuntimeException();
    }
    cacheReference.replaceInCache(this, newInstance);
    return cacheReference.getFromCache(originalFqName, revision);
  }


  public void clearCache() {
    if (logger.isTraceEnabled()) {
      logger.trace("generationbase.cache contained " + cacheReference.size() + " elements and will now be cleared.");
    }
    cacheReference.clear();
  }


  GenerationBase getFromCache(String originalXmlName) {
    GenerationBase gb = cacheReference.getFromCache(originalXmlName, revision);
    if (gb != null) {
      return gb;
    }

    long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getRevisionDefiningXMOMObjectOrParent(originalXmlName, revision);
    return cacheReference.getFromCache(originalXmlName, rev);
  }


  DOM getCachedDOMInstanceOrCreate(String originalDomInputName, long useRevision) throws XPRC_InvalidPackageNameException {
    String fqClassName = GenerationBase.transformNameForJava(originalDomInputName);

    long rev = xmlInputSource.getRevisionDefiningXMOMObjectOrParent(originalDomInputName, useRevision);
    
    GenerationBase o;
    DOM.cacheLockDOM.lock();
    try {
      o = cacheReference.getFromCache(originalDomInputName, rev);
      if (o == null) {
        o = new DOM(originalDomInputName, fqClassName, cacheReference, rev, null, new FactoryManagedRevisionXMLSource());
        cacheReference.insertIntoCache(o);
      }
    } finally {
      DOM.cacheLockDOM.unlock();
    }

    if (!(o instanceof DOM)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(originalDomInputName, "DOM", o.getClass().getSimpleName()));
    }
    return (DOM) o;
  }


  WF getCachedWFInstanceOrCreate(String originalWFInputName, long useRevision) throws XPRC_InvalidPackageNameException {
    String fqClassName = GenerationBase.transformNameForJava(originalWFInputName);

    long rev =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalWFInputName, useRevision);
    
    GenerationBase o;
    WF.cacheLockWF.lock();
    try {
      o = cacheReference.getFromCache(originalWFInputName, rev);
      if (o == null) {
        o = new WF(originalWFInputName, fqClassName, cacheReference, rev, null);
        cacheReference.insertIntoCache(o);
      }
    } finally {
      WF.cacheLockWF.unlock();
    }

    if (!(o instanceof WF)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(originalWFInputName, "WF", o.getClass().getSimpleName()));
    }
    return (WF) o;
  }


  ExceptionGeneration getCachedExceptionInstanceOrCreate(String originalFQExceptionName, long useRevision)
      throws XPRC_InvalidPackageNameException {
    String fqExceptionClassName = GenerationBase.transformNameForJava(originalFQExceptionName);

    long rev = xmlInputSource.getRevisionDefiningXMOMObjectOrParent(originalFQExceptionName, useRevision);
    
    GenerationBase o;
    ExceptionGeneration.cacheLockExceptionGeneration.lock();
    try {
      o = cacheReference.getFromCache(originalFQExceptionName, rev);
      if (o == null) {
        o = new ExceptionGeneration(originalFQExceptionName, fqExceptionClassName, cacheReference, rev, null, new FactoryManagedRevisionXMLSource());
        cacheReference.insertIntoCache(o);
      }
    } finally {
      ExceptionGeneration.cacheLockExceptionGeneration.unlock();
    }

    if (!(o instanceof ExceptionGeneration)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(originalFQExceptionName, "ExceptionGeneration", o.getClass().getSimpleName()));
    }
    return (ExceptionGeneration) o;
  }


  private static final Pattern pathAllowedCharsPattern = Pattern.compile("[\\w_]+");
  private static final Pattern beginsWithNumberPattern = Pattern.compile("^\\d.*");
  private static final Pattern dotPattern = Pattern.compile("\\.");
  private static final LruCache<String, String> cacheGoodNames = new LruCache<>(100000);

  public static String transformNameForJava(String fqXmlName) throws XPRC_InvalidPackageNameException {
    synchronized (cacheGoodNames) {
      String cached = cacheGoodNames.get(fqXmlName);
      if (cached != null) {
        return cached;
      }
    }
    
    Class<?> c = mdmObjectMappingToJavaClasses.get(fqXmlName);
    if (c != null) {
      synchronized (cacheGoodNames) {
        cacheGoodNames.put(fqXmlName, c.getName());
      }
      return c.getName();
    }

    String[] parts = dotPattern.split(fqXmlName);
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      // keine sonderzeichen. nur buchstaben und zahlen!
      if (!pathAllowedCharsPattern.matcher(parts[i]).matches()) {
        throw new XPRC_InvalidPackageNameException(fqXmlName);
      }
      if (beginsWithNumberPattern.matcher(parts[i]).matches()) {
        parts[i] = "_" + parts[i];
      }
      if (i > 0) {
        ret.append(".");
      }
      ret.append(parts[i]);
    }
    String s = ret.toString();
    synchronized (cacheGoodNames) {
      cacheGoodNames.put(fqXmlName, s);
    }
    return s;
  }


  public static String transformNameForJava(String path, String name) throws XPRC_InvalidPackageNameException {
    String s;
    if (isEmpty(path)) {
      s = name;
    } else {
      s = path + "." + name;
    }
    return transformNameForJava(s);
  }

  public static Pair<String, String> getPathAndNameFromJavaName(String javaName) {
    int posLastPeriod = javaName.lastIndexOf(".");
    String path = (posLastPeriod >= 0) ? javaName.substring(0, posLastPeriod) : "";
    String name = javaName.substring(posLastPeriod + 1);
    
    return new Pair<String, String>(path, name);
  }
  
  public static void save(String[] classes, String fqClassName) throws Ex_FileAccessException {
    save(classes, fqClassName, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static void save(String[] classes, String fqClassName, boolean fromDeployed, Long revision) throws Ex_FileAccessException {
    String fileLocation = getRelativeJavaFileLocation(fqClassName, fromDeployed, revision);
    File f = new File(fileLocation);
    save(classes, f);
  }

  public static void save(String[] classes, File saveTo) throws Ex_FileAccessException {

    if (logger.isDebugEnabled()) {
      logger.debug("saving " + saveTo.getPath());
    }

    for (String javaCode : classes) { //FIXME mehrfach ins gleiche file schreiben macht keinen sinn.
      final ReadLock lock = javaFileLock.readLock();
      lock.lock();
      try {
        int cnt = 0;
        while (!saveTo.getParentFile().exists() && !saveTo.getParentFile().mkdirs() && cnt < 20) {
          if (logger.isTraceEnabled()) {
            logger.trace("could not create parent directory " + saveTo.getParentFile().getAbsolutePath()
                + ". will retry in 50ms.");
          }
          cnt++;
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            throw new RuntimeException("interrupted while waiting for parent directory to be created.", e);
          }
        }
        try {
          saveTo.createNewFile();
        } catch (IOException e) {
          throw new Ex_FileAccessException(saveTo.getAbsolutePath());
        }
      } finally {
        lock.unlock();
      }
      FileUtils.writeStringToFile(javaCode, saveTo);
    }
  }

  public static void deleteGeneratedJava(String fqClassName) {
    deleteGeneratedJava(fqClassName, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  /**
   * löscht javafile aus mdmclasses verzeichnis, sofern nicht {@link XynaProperty#REMOVE_GENERATED_FILES} auf false
   * gesetzt ist
   */
  public static void deleteGeneratedJava(String fqClassName, boolean fromDeployed, Long revision) {
    if (!XynaProperty.REMOVE_GENERATED_FILES.get()) {
      return;
    }

    final WriteLock lock = javaFileLock.writeLock();
    lock.lock();
    try {
      deleteGeneratedJavaUnlocked(fqClassName, fromDeployed, revision);
    } finally {
      lock.unlock();
    }
  }

  public static void deleteGeneratedJavaUnlocked(String fqClassName) {
    deleteGeneratedJava(fqClassName, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static void deleteGeneratedJavaUnlocked(String fqClassName, boolean fromDeployed, Long revision) {
    if (!XynaProperty.REMOVE_GENERATED_FILES.get()) {
      return;
    }

    String fileLocation = getRelativeJavaFileLocation(fqClassName, fromDeployed, revision);
    File f = new File(fileLocation);

    if (f.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("removing " + fileLocation);
      }
      f.delete();
    } else {
      //kann zb passieren, weil java noch nicht generiert wurde
      //oder weil die methode in onerror aufgerufen wird, nach einem fehler in cleanup, nachdem das file dort bereits gelöscht wurde.
      if (logger.isDebugEnabled()) {
        logger.debug("could not remove " + fileLocation + ", did not exist");
      }
    }

    //leere Verzeichnisse löschen
    FileUtils.deleteEmptyDirectoryRecursively(f.getParentFile(), new File(Constants.GENERATION_DIR));
  }

  public static String getSimpleNameFromFQName(String fqClassName) {
    return fqClassName.substring(fqClassName.lastIndexOf('.') + 1);
  }

  public static String getPackageNameFromFQName(String fqName) {
    return fqName.substring(0, fqName.lastIndexOf('.'));
  }



  public static String getFileLocationOfServiceLibsForSaving(String fqClassName, Long revision) {
    return RevisionManagement.getPathForRevision(PathType.SERVICE, revision, false)
                    + Constants.fileSeparator + fqClassName;
  }


  /**
   * vom workingset
   */
  public static String getFileLocationOfServiceLibsForDeployment(String fqClassName) {
    return getFileLocationOfServiceLibsForDeployment(fqClassName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public static String getFileLocationOfServiceLibsForDeployment(String fqClassName, Long revision) {
    return RevisionManagement.getPathForRevision(PathType.SERVICE, revision, true) + Constants.fileSeparator
        + fqClassName;
  }


  /**
   * benötigt validen packagename
   */
  public static String getFileLocationForSavingStaticHelper(String fqXMLName, Long revision) {
    return new StringBuilder(RevisionManagement.getPathForRevision(PathType.XMOM, revision, false)).append(Constants.fileSeparator)
                    .append(fqXMLName.replaceAll("\\.", Constants.fileSeparator)).toString();
  }


  public static String getFileLocationForDeploymentStaticHelper(String fqXMLName, Long revision) {
    if (isReservedServerObjectByFqOriginalName(fqXMLName)) {
      RevisionManagement rm = getRevisionManagement();
      if (rm.isWorkspaceRevision(revision)) {
        return getFileLocationForSavingStaticHelper(fqXMLName, revision);
      }
    }
    return new StringBuilder(RevisionManagement.getPathForRevision(PathType.XMOM, revision)).append(Constants.fileSeparator)
                    .append(fqXMLName.replaceAll("\\.", Constants.fileSeparator)).toString();
  }
  
  @Deprecated
  public static String getFileLocationOfXmlNameForSaving(String originalName) {
    return getFileLocationForSavingStaticHelper(originalName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static String getFileLocationOfXmlNameForSaving(String originalName, Long revision) {
    return getFileLocationForSavingStaticHelper(originalName, revision);
  }

  @Deprecated
  public static String getFileLocationOfXmlNameForDeployment(String originalName) {
    return getFileLocationForDeploymentStaticHelper(originalName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static String getFileLocationOfXmlNameForDeployment(String originalName, Long revision) {
    return getFileLocationForDeploymentStaticHelper(originalName, revision);
  }

  public static String getFileLocationOfXmlName(String originalName, Long revision) {
    RevisionManagement revisionManagement = getRevisionManagement();
    if (revisionManagement.isWorkspaceRevision(revision)) {
      return getFileLocationForSavingStaticHelper(originalName, revision);
    } else {
      return getFileLocationForDeploymentStaticHelper(originalName, revision);
    }
  }


  public static String getRelativeJavaFileLocation(String fqClassName) {
    return getRelativeJavaFileLocation(fqClassName, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static String getRelativeJavaFileLocation(String fqClassName, boolean fromDeployed, Long revision) {
    if (!fqClassName.contains(".")) {
      fqClassName = DEFAULT_PACKAGE + "." + fqClassName;
    }
    if (fromDeployed) {
      return new StringBuilder(Constants.GENERATION_DIR).append(Constants.fileSeparator)
          .append(fqClassName.replaceAll("\\.", Constants.fileSeparator)).append(".java").toString();
    } else {
      String savedDir = RevisionManagement.getPathForRevision(PathType.ROOT, revision, false);
      return new StringBuilder(savedDir)
          .append(Constants.GENERATION_DIR).append(Constants.fileSeparator)
          .append(fqClassName.replaceAll("\\.", Constants.fileSeparator)).append(".java").toString();
    }
  }


  protected static boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }


  public static String buildGetter(String name) {
    return new StringBuilder("get").append(name.substring(0, 1).toUpperCase()).append(name.substring(1, name.length()))
                    .toString();
  }


  public static String buildSetter(String name) {
    return "s" + buildGetter(name).substring(1, name.length() + 3);
  }


  public static Triple<AVariable, AVariable, String> traversePathMapHierarchyToFindValue(DOM dom, DomOrExceptionGenerationBase var, String path) {
    if (var == null) {
      return null;
    }

    PathMapInformation pmi = dom.getPathMapInformation();
    String pathKeyPath = pmi.getPathKey();
    String pathValuePath = pmi.getPathValue();

    for (AVariable v : var.getMemberVars()) {
      String localPath;
      if (path.length() == 0) {
        localPath = v.getVarName();
      } else {
        localPath = path + "." + v.getVarName();
      }
      if (pathKeyPath.equals(localPath)) {
        //pfad - varname
        continue;
      }
      if (pmi.getInheritFromDataModel(localPath) != null) {
        //benötigt keine map-setter/getter, weil nicht in mappings verwendet
        continue;
      }
      if (v.getDomOrExceptionObject() != null) {
        //rekursion
        Triple<AVariable, AVariable, String> variableWithPath = traversePathMapHierarchyToFindValue(dom, v.getDomOrExceptionObject(), localPath);
        if (variableWithPath != null) {
          variableWithPath.setSecond(v); //rootvar
          return variableWithPath;
        }
        continue;
      }

      //hat simple type!
      if (pathValuePath == null || pathValuePath.equals(localPath)) {
        //-> das ist der value!
        //null ist für abwärtskompatibilität der alte fall: es darf nur genau ein solcher pfad existieren. wird hier aber nicht validiert
        return Triple.of(v, v, localPath);
      }
      //weitersuchen
    }

    return null;
  }

  /**
   * liste mit doppelpunkten bzw. 'path separator' getrennt
   */
  public static String getJarFiles() {

    String[] libs = getJarFileNamesFromFolder(Constants.LIB_DIR);
    String[] userlib = getJarFileNamesFromFolder(Constants.USERLIB_DIR);

    if (libs == null) {
      return Constants.SERVER_CLASS_DIR;
    } else {
      StringBuilder result = new StringBuilder();
      if (Constants.RUNS_FROM_SOURCE) {
        result.append(Constants.SERVER_CLASS_DIR);
      }
      for (String s : libs) {
        if (result.length() > 0) {
          result.append(pathSeparator);
        }
        result.append(Constants.LIB_DIR).append(Constants.fileSeparator).append(s);
      }
      if (userlib != null) {
        for (String s : userlib) {
          if (result.length() > 0) {
            result.append(pathSeparator);
          }
          result.append(Constants.USERLIB_DIR).append(Constants.fileSeparator).append(s);
        }
      }
      return result.toString();
    }
  }


  public static String[] getJarFileNamesFromFolder(String folderName) {
    File f = new File(folderName);
    if (f.exists()) {
      FilenameFilter ff = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          if (name.endsWith(".jar")) {
            return true;
          } else {
            return false;
          }
        }

      };
      return f.list(ff);
    } else {
      return new String[0];
    }
  }


  private static final Pattern jarFileSuffixPattern = Pattern.compile(".*\\.jar$");
  private static final Pattern classFileSuffixPattern = Pattern.compile(".*\\.class$");


  public static String flattenClassPathSet(Set<String> classPathEntries) {
    String additionalLibsAsString = "";
    if (classPathEntries != null) {
      Iterator<String> classPathIterator = classPathEntries.iterator();
      while (classPathIterator.hasNext()) {
        String s = classPathIterator.next();
        File f = new File(s);
        if (f.isDirectory() || jarFileSuffixPattern.matcher(f.getName()).matches()) {
          additionalLibsAsString += s;
          if (classPathIterator.hasNext()) {
            additionalLibsAsString += pathSeparator;
          }
        } else {
          if (logger.isTraceEnabled()) {
            logger.trace("unrecognized file type for library used by compile: " + s);
          }
        }
      }
    }
    return additionalLibsAsString;
  }


  public static void compile(String fqClassName, HashSet<String> additionalLibs, String classDir) throws XPRC_CompileError {
    compile(fqClassName, additionalLibs, classDir, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false, true);
  }


  public static void compile(String fqClassName, HashSet<String> additionalLibs, String classDir, boolean crossCompile) throws XPRC_CompileError {
    compile(fqClassName, additionalLibs, classDir, RevisionManagement.REVISION_DEFAULT_WORKSPACE, crossCompile, true);
  }


  public static void compile(String fqClassName, HashSet<String> additionalLibs, String classDir, Long revision) throws XPRC_CompileError {
    compile(fqClassName, additionalLibs, classDir, revision, false, true);
  }


  public static void compile(String fqClassName, HashSet<String> additionalLibs, String classDir, Long revision,
                             boolean crossCompile, boolean fileLocationFromDeployed) throws XPRC_CompileError {
    String additionalLibsAsString = flattenClassPathSet(additionalLibs);
    File mdmclasses = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision));
    if (!mdmclasses.exists()) {
      mdmclasses.mkdir();
    }
    String classPath =
        getJarFiles() + pathSeparator + mdmclasses.getPath() + additionalLibsAsString + pathSeparator + "../server";
    final ReadLock readLock = javaFileLock.readLock();
    readLock.lock();
    try {
      final Lock writeLock = classLock.writeLock();
      writeLock.lock();
      try {
        compile(fqClassName, classPath, classDir, crossCompile, fileLocationFromDeployed, revision);
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }


  public static void compile(String fqClassName, String classPath, String classDir) throws XPRC_CompileError {
    compile(fqClassName, classPath, classDir, false);
  }

  public static void compile(String fqClassName, String classPath, String classDir, boolean crossCompile) throws XPRC_CompileError {
    compile(fqClassName, classPath, classDir, crossCompile, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public static void compile(String fqClassName, String classPath, String classDir, boolean crossCompile, boolean fileLocationFromDeployed, Long revision)
      throws XPRC_CompileError {
    if (crossCompile) {
      String javaVersion = XynaProperty.BUILDMDJAR_JAVA_VERSION.get();
      //FIXME wieso ist die javaversion in der property nicht einfach gleich der targetversion?
      if (javaVersion.equals("Java5")) {
        compileJavaFiles(fqClassName, new String[] {getRelativeJavaFileLocation(fqClassName, fileLocationFromDeployed, revision)}, classPath, classDir,
                         "gen", true, "1.5");
      } else if (javaVersion.equals("Java6")) {
        compileJavaFiles(fqClassName, new String[] {getRelativeJavaFileLocation(fqClassName, fileLocationFromDeployed, revision)}, classPath, classDir,
                         "gen", true, "1.6");
      } else {
        compileJavaFiles(fqClassName, new String[] {getRelativeJavaFileLocation(fqClassName, fileLocationFromDeployed, revision)}, classPath, classDir,
                         "gen", false, null);
      }
    } else {
      compileJavaFiles(fqClassName, new String[] {getRelativeJavaFileLocation(fqClassName, fileLocationFromDeployed, revision)}, classPath, classDir,
                       "gen", false, null);
    }
  }

  /**
   * @param packagesOrFiles packages müssen "a.b.c" angegeben werden, files als relativ oder absolute fileaddresse (a/b/c) zum aktuellen verzeichnis (nicht sourcepath!)
   * @param targetDir wohin wird javadoc erzeugt
   * @param sourcePath optional. nur für packages relevant
   * @param classPath auflösen von verwendeten klassen
   */
  public static void createJavaDoc(String[] packagesOrFiles, String targetDir, String sourcePath, String classPath) {
    String[] args;
    if (sourcePath != null) {
      args = new String[] {"-d", targetDir, "-sourcepath", sourcePath, "-classpath", classPath};
    } else {
      args = new String[] {"-d", targetDir, "-classpath", classPath};
    }
    String[] argsNew = new String[args.length + packagesOrFiles.length];
    System.arraycopy(args, 0, argsNew, 0, args.length);
    System.arraycopy(packagesOrFiles, 0, argsNew, args.length, packagesOrFiles.length);

    ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
    ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
    int errCode = ToolProvider.getSystemDocumentationTool().run(null, baosOut, baosErr, argsNew);
    String err;
    try {
      err = baosErr.toString(Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    if (err != null && err.trim().length() > 0) {
      logger.warn("error generating javadoc: " + err);
    }
    if (logger.isDebugEnabled()) {
      String out;
      try {
        out = baosOut.toString(Constants.DEFAULT_ENCODING);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      if (out != null && out.trim().length() > 0) {
        logger.debug("output generating javadoc: " + out);
      }
    }
    if (errCode == 0) {
      //ok
    } else {
      logger.warn("javadoc generation not successful for packages: " + Arrays.toString(packagesOrFiles));
    }
  }


  public static void compileJavaFiles(String fqClassName, String[] javaFilesToCompile, String classPath,
                                      String classDir, String sourcepath, boolean useTargetVersion, String targetVersion)
      throws XPRC_CompileError {
    compileJavaFiles(fqClassName, javaFilesToCompile, classPath, classDir, sourcepath, useTargetVersion, targetVersion,
                     false);
  }


  /**
   * kompiliert alle files die in javaFilesToCompile angegeben werden (und alle weiteren, die davon abhängen, falls sie
   * im sourcepath vorgefunden werden).
   */
  public static void compileJavaFiles(String fqClassName, String[] javaFilesToCompile, String classPath,
                                      String classDir, String sourcepath, boolean useTargetVersion, String targetVersion, boolean compileWithDebug)
      throws XPRC_CompileError {

    //TODO hässlich, dass hier der fqclassname übergeben werden muss für die exceptions
    StringWriter sw = new StringWriter();

    int errorCode;
    synchronized (GenerationBase.class) {
      InMemoryCompilationSet cs = new InMemoryCompilationSet(false, false, false);
      for (String path : classPath.split(Constants.PATH_SEPARATOR)) {
        cs.addToClassPath(path);
      }
      cs.setClassDir(classDir);
      
      for (int i = 0; i < javaFilesToCompile.length; i++) {
        String code;
        try {
          code = FileUtils.readFileAsString(new File(javaFilesToCompile[i]));
        } catch (Ex_FileWriteException e) {
          throw new RuntimeException(e);
        }
        String fqClassNameForSource = deriveClassNameFromCode(code);
        if (fqClassNameForSource == null) {
          fqClassNameForSource = fqClassName;
        }
        JavaSourceFromString jsfs = new JavaSourceFromString(fqClassNameForSource, code);
        jsfs.setClassOutputLocation(classDir);
        cs.addToCompile(jsfs);
      }
      CompilationResult result = cs.compile();
      // TODO check result
      errorCode = 0;
    }
    
    StringBuilder sb = new StringBuilder();
    if (logger.isDebugEnabled()) {
      for (String f : javaFilesToCompile) {
        sb.append(f).append(" ");
      }
      String javaFiles = sb.toString().trim();
      logger.debug("--------------- compiling " + javaFiles + " with classpath: "
                      + classPath + " ...");
      logger.debug(sw.toString());
    }
    switch (errorCode) {
      case 0 :
        logger.debug("success");
        break;
      case 1 :
        for (String f : javaFilesToCompile) {
          sb.append(f).append(" ");
        }
        String javaFiles = sb.toString().trim();
        throw new XPRC_CompileError(fqClassName, javaFiles, "compile status: ERROR: "
                        + sw.toString());
      case 2 :
        for (String f : javaFilesToCompile) {
          sb.append(f).append(" ");
        }
        javaFiles = sb.toString().trim();
        throw new XPRC_CompileError(fqClassName, javaFiles, "compile status: CMDERR: "
                        + sw.toString());
      case 3 :
        for (String f : javaFilesToCompile) {
          sb.append(f).append(" ");
        }
        javaFiles = sb.toString().trim();
        throw new XPRC_CompileError(fqClassName, javaFiles, "compile status: SYSERR: "
                        + sw.toString());
      case 4 :
        for (String f : javaFilesToCompile) {
          sb.append(f).append(" ");
        }
        javaFiles = sb.toString().trim();
        throw new XPRC_CompileError(fqClassName, javaFiles, "compile status: ABNORMAL: "
                        + sw.toString());
      default :
        for (String f : javaFilesToCompile) {
          sb.append(f).append(" ");
        }
        javaFiles = sb.toString().trim();
        throw new XPRC_CompileError(fqClassName, javaFiles,
                                    "Compiler returned unexpected code: " + errorCode + ": " + sw.toString());
    }
  }

  
  private static final Pattern PACKAGE_PATTERN = Pattern.compile("[\\n\\s]package\\s+([^;]+);");
  private static final Pattern CODEIDENTIFIER_PATTERN = Pattern.compile("[\\n\\s](class|enum|interface)\\s+([^\\s{]+)[\\s{]");
  
  private static String deriveClassNameFromCode(String code) {
    Matcher codeMatcher = PACKAGE_PATTERN.matcher(code);
    if (codeMatcher.find()) {
      StringBuilder fqNameBuilder = new StringBuilder();
      fqNameBuilder.append(codeMatcher.group(1))
                   .append(".");
      codeMatcher = CODEIDENTIFIER_PATTERN.matcher(code);
      if (codeMatcher.find()) {
        fqNameBuilder.append(codeMatcher.group(2));
        return fqNameBuilder.toString();
      }
    }
    return null;
  }


  private static final Pattern reservedVarNamePattern = Pattern.compile("^(step\\d+|steps|allSteps|preHandlers|postHandlers|l|notdeployed|e|xo)$");
  private static final Pattern varNamePattern = Pattern.compile("^\\w[\\w\\d]*$");
  private static final Set<String> javaReservedWords = new HashSet<String>();
  static {
    /*
     * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.9
        abstract    continue    for           new          switch
        assert      default     if            package      synchronized
        boolean     do          goto          private      this
        break       double      implements    protected    throw
        byte        else        import        public       throws
        case        enum        instanceof    return       transient
        catch       extends     int           short        try
        char        final       interface     static       void
        class       finally     long          strictfp     volatile
        const       float       native        super        while
     */
    javaReservedWords.add("abstract");
    javaReservedWords.add("continue");
    javaReservedWords.add("for");
    javaReservedWords.add("new");
    javaReservedWords.add("switch");
    javaReservedWords.add("assert");
    javaReservedWords.add("default");
    javaReservedWords.add("if");
    javaReservedWords.add("package");
    javaReservedWords.add("synchronized");
    javaReservedWords.add("boolean");
    javaReservedWords.add("do");
    javaReservedWords.add("goto");
    javaReservedWords.add("private");
    javaReservedWords.add("this");
    javaReservedWords.add("break");
    javaReservedWords.add("double");
    javaReservedWords.add("implements");
    javaReservedWords.add("protected");
    javaReservedWords.add("throw");
    javaReservedWords.add("byte");
    javaReservedWords.add("else");
    javaReservedWords.add("import");
    javaReservedWords.add("public");
    javaReservedWords.add("throws");
    javaReservedWords.add("case");
    javaReservedWords.add("enum");
    javaReservedWords.add("instanceof");
    javaReservedWords.add("return");
    javaReservedWords.add("transient");
    javaReservedWords.add("catch");
    javaReservedWords.add("extends");
    javaReservedWords.add("int");
    javaReservedWords.add("short");
    javaReservedWords.add("try");
    javaReservedWords.add("char");
    javaReservedWords.add("final");
    javaReservedWords.add("interface");
    javaReservedWords.add("static");
    javaReservedWords.add("void");
    javaReservedWords.add("class");
    javaReservedWords.add("finally");
    javaReservedWords.add("long");
    javaReservedWords.add("strictfp");
    javaReservedWords.add("volatile");
    javaReservedWords.add("const");
    javaReservedWords.add("float");
    javaReservedWords.add("native");
    javaReservedWords.add("super");
    javaReservedWords.add("while");
  }


  protected void validateVars(Collection<AVariable> vars) throws XPRC_InvalidVariableNameException,
      XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_PrototypeDeployment,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED {
    HashSet<String> varNames = new HashSet<String>();
    for (AVariable v : vars) {
      //reservierte variablen
      if (varNames.contains(v.getVarName()) || reservedVarNamePattern.matcher(v.getVarName()).matches()
          || javaReservedWords.contains(v.getVarName())) {
        throw new XPRC_InvalidVariableNameException(getOriginalFqName(), v.getVarName());
      }
      if (!varNamePattern.matcher(v.getVarName()).matches()) {
        throw new XPRC_InvalidVariableNameException(getOriginalFqName(), v.getVarName());
      }
      varNames.add(v.getVarName());

      v.validate();
    }
  }
  
  public static boolean isReservedName(String name) {
    return reservedVarNamePattern.matcher(name).matches() || javaReservedWords.contains(name);
  }


  public static boolean isReservedServerObjectByFqOriginalName(String originalFqName) {
    if (overrideReservedServerObjectsForCodeGenUpdates) {
      return false;
    }
    return mdmObjectMappingToJavaClasses.containsKey(originalFqName);
  }

  public final boolean isReservedServerObject() {
    return isReservedServerObjectByFqOriginalName(getOriginalFqName());
  }
  
  private static final ConcurrentHashMap<String, Boolean> reservedObjectInitialized = new ConcurrentHashMap<String, Boolean>(64, 0.75f, 3);

  private boolean reservedObjectIsInitialized() {
    if (null == reservedObjectInitialized.putIfAbsent(originalFqName, true)) {
      return false;
    }
    return true;
  }

  public static boolean isReservedServerObjectByFqClassName(String className) {
    if (overrideReservedServerObjectsForCodeGenUpdates) {
      return false;
    }
    Iterator<Class<?>> iter = mdmObjectMappingToJavaClasses.values().iterator();
    while (iter.hasNext()) {
      if (iter.next().getName().equals(className)) {
        return true;
      }
    }
    return false;
  }


  public static Set<String> getReservedServerObjectXmlNames() {
    return mdmObjectMappingToJavaClasses.keySet();
  }

  public static Class<?> getReservedClass(String fqXmlName) {
    return mdmObjectMappingToJavaClasses.get(fqXmlName);
  }

  protected static void checkUniqueVarNamesWithInherited(List<AVariable> allMemberVarsIncludingInherited)
                  throws XPRC_DuplicateVariableNamesException {
    List<AVariable> allVars = allMemberVarsIncludingInherited;
    for (int i = 0; i < allVars.size(); i++) {
      AVariable vi = allVars.get(i);

      for (int j = 0; j < i; j++) {
        AVariable vj = allVars.get(j);
        if (vi.getVarName().equals(vj.getVarName())) {
          throw new XPRC_DuplicateVariableNamesException(vi.getVarName());
        }
      }
    }
  }


  public final boolean isXynaFactoryComponent() {
    return isXynaFactoryComponent;
  }


  void setStateForTestingPurposes(DeploymentState state) {
    this.state = state;
  }


  public void copyXmlToDeploymentFolder(String fqXmlName, Long revision) throws Ex_FileAccessException {
    File f = xmlInputSource.getFileLocation(fqXmlName, revision, false);
    if (!f.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug(getOriginalFqName() + " does not exist (xml not found) in revision " + revision + ".");
      }
      doesntExist = true;
      return;
    }

    File fDeploy = xmlInputSource.getFileLocation(fqXmlName, revision, true);
    if (!fDeploy.exists()) {
      fDeploy.getParentFile().mkdirs();
      try {
        fDeploy.createNewFile();
      } catch (IOException e) {
        throw new Ex_FileWriteException(fDeploy.getAbsolutePath(), e);
      }
    }
    FileUtils.copyFile(f, fDeploy);
  }

 
  private static final Pattern PATTERN_UNDERSCORE_NUMBER = Pattern.compile("\\._(\\d+)");
  private static final Pattern PATTERN_UNDERSCORE_BEFORE_NUMBER = Pattern.compile("\\._(?=\\d)");


  /**
   * falls ein xmom objekt existiert, welches diesen fqclassname hat, wird der fqxmlname des xmom objekts zurückgegeben.
   * 
   * falls kein derartiges xmom objekt gefunden wird, wird der ursprüngliche fqclassname zurückgegeben.
   */
  public static String lookupXMLNameByJavaClassName(String fqClassName, final long revision,
                                                    final boolean followRuntimeContextDependencies) {
    int idx = fqClassName.indexOf("._");
    if (idx > -1) {
      final DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
      final RuntimeContextDependencyManagement rcdm =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

      String guessedName = PATTERN_UNDERSCORE_NUMBER.matcher(fqClassName).replaceAll(".$1");
      if (followRuntimeContextDependencies) {
        Long rev = rcdm.getRevisionDefiningXMOMObject(guessedName, revision);
        if (rev != null) {
          return guessedName;
        }
      } else {
        if (dism.get(guessedName, revision) != null) {
          return guessedName;
        }
      }

      //guess war vielleicht falsch.
      //guess kann nur falsch sein, wenn mehr als einmal "._" im namen enthalten ist.
      int idx2 = fqClassName.indexOf("._", idx + 1);
      if (idx2 > -1) {
        final String[] parts = PATTERN_UNDERSCORE_BEFORE_NUMBER.split(fqClassName);
        //probiere alle kombinationen von weglassen des underscores aus
        int[] propertyCounts = new int[parts.length - 1];
        Arrays.fill(propertyCounts, 2);
        final AtomicReference<String> foundName = new AtomicReference<>(null);
        Combinatorics.iterateOverCombinations(new CombinationHandler() {

          @Override
          public boolean accept(int[] properties) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
              sb.append(parts[i]);
              if (i < parts.length - 1) {
                if (properties[i] == 0) {
                  sb.append("._");
                } else {
                  sb.append(".");
                }
              }
            }
            if (followRuntimeContextDependencies) {
              Long rev = rcdm.getRevisionDefiningXMOMObject(sb.toString(), revision);
              if (rev != null) {
                foundName.set(sb.toString());
                return false;
              }
            } else {
              if (dism.get(sb.toString(), revision) != null) {
                foundName.set(sb.toString());
                return false;
              }
            }
            return true; //nächste kombination
          }

        }, propertyCounts);
        if (foundName.get() != null) {
          return foundName.get();
        }
      }
    }
    return fqClassName;
  }


  public String toString() {
    return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "-" + getOriginalFqName();
  }
  
  
  /**
   * xmom objekt aus saved ordner löschen und evtl vorhandenes verzeichnis
   * aus revisions/rev_workingset/saved/services löschen
   */
  public static void deleteMDMObjectFromSavedFolder(String originalFqName, Long revision) {
    String fileLocation = getFileLocationForSavingStaticHelper(originalFqName, revision) + ".xml";
    File object = new File(fileLocation);

    if (object.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Removing " + fileLocation);
      }
      object.delete();
      //leere Verzeichnisse löschen
      String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
      FileUtils.deleteEmptyDirectoryRecursively(object.getParentFile(),  new File(savedMdmDir));
    }

    File serviceLibsDir = new File(getFileLocationOfServiceLibsForSaving(originalFqName, revision));
    if (serviceLibsDir.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Removing " + serviceLibsDir);
      }
      FileUtils.deleteDirectoryRecursively(serviceLibsDir);
    }
  }


  public static String retrieveRootTag(String originalFqName, Long revision) throws Ex_FileAccessException, XPRC_XmlParsingException {
    return retrieveRootTag(originalFqName, revision, false, false);
  }
  
  private static RevisionManagement revisionManagement;
  private static RevisionManagement getRevisionManagement() {
    if (revisionManagement == null) {
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();  
      if (XynaFactory.getInstance().isStartingUp()) {
        return rm;
      }
      revisionManagement = rm;
    } 
    return revisionManagement;
  }

  public static String getStorageLocation(String originalFqName, RuntimeContext runtimeContext, 
      boolean fallBackToDeployed, boolean allowDelegation) throws XFMG_NoSuchRevision {
    Long revision;
    try {
      revision = getRevisionManagement().getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(runtimeContext.getGUIRepresentation());
    }
    return getStorageLocation(originalFqName, revision, fallBackToDeployed, allowDelegation);
  }

  
  public static String getStorageLocation(String originalFqName, Long revision, 
      boolean fallBackToDeployed, boolean allowDelegation) {
    Long definingRev = revision;
    if (allowDelegation) {
      RuntimeContextDependencyManagement rcdm =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      definingRev = rcdm.getRevisionDefiningXMOMObjectOrParent(originalFqName, revision);
    }
    String location;

    if (getRevisionManagement().isWorkspaceRevision(definingRev)) {
      location = getFileLocationForSavingStaticHelper(originalFqName, definingRev) + ".xml";
      File f = new File(location);
      if (!f.exists() && fallBackToDeployed) {
        location = getFileLocationForDeploymentStaticHelper(originalFqName, definingRev) + ".xml";
      }
    } else {
      location = getFileLocationForDeploymentStaticHelper(originalFqName, definingRev) + ".xml";
    }
    return location;
  }


  public static String retrieveRootTag(String originalFqName, Long revision, boolean fallBackToDeployed, boolean allowDelegation)
      throws Ex_FileAccessException, XPRC_XmlParsingException {
    String loc = getStorageLocation(originalFqName, revision, fallBackToDeployed, allowDelegation);
    try (FileInputStream fis =
        new FileInputStream(new File(loc))) {
      return XMLUtils.getRootElementName(fis);
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(loc, e);
    } catch (IOException e) {
      throw new XPRC_XmlParsingException(loc, e);
    } catch (XMLStreamException e) {
      throw new XPRC_XmlParsingException(loc, e);
    }
  }


  public Long getRevision() {
    return revision;
  }


  private FolderCopyWithBackup serviceLibsBackup;

  private void backupAndCopyServiceLibraries() throws Ex_FileAccessException {
    if ((revision ==  null || xmlInputSource.isOfRuntimeContextType(getRevision(), RuntimeContextType.Workspace)) &&
        (mode == DeploymentMode.codeChanged || mode == DeploymentMode.codeNew)) {
      if (serviceLibsBackup != null) {
        //TODO nur sinnvoll, wenn es ein deployment ist, bei dem unter server/services keine jars liegen, die alten jars aber erhalten bleiben sollen.
        //ansonsten gibt es eigtl noch den usecase: man will jetzt weniger jars verwenden als vorher.
        serviceLibsBackup.copy(false);
      }
    }
  }


  private DependencySourceType getDependencyType() {
    DependencySourceType type = null;

    if (GenerationBase.this instanceof DOM) {
      type = DependencySourceType.DATATYPE;
    } else if (GenerationBase.this instanceof ExceptionGeneration) {
      type = DependencySourceType.XYNAEXCEPTION;
    } else if (GenerationBase.this instanceof WF) {
      type = DependencySourceType.WORKFLOW;
    }

    return type;
  }


  public static String getDefaultOrdertype(GenerationBase gb) {
    return gb.getFqClassName();
  }



  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((originalFqName == null) ? 0 : originalFqName.hashCode());
    result = prime * result + ((revision == null) ? 0 : revision.hashCode());
    return result;
  }



  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GenerationBase other = (GenerationBase) obj;
    if (originalFqName == null) {
      if (other.originalFqName != null)
        return false;
    } else if (!originalFqName.equals(other.originalFqName))
      return false;
    if (revision == null) {
      if (other.revision != null)
        return false;
    } else if (!revision.equals(other.revision))
      return false;
    return true;
  }


  /**
   * methode ist dafür gedacht auch zu funktionieren, wenn das objekt noch nciht geparst wurde oder einen fehler dabei hatte
   **/
  private Dependencies getDepsLazyCreateSafely(boolean warnIfStateIsUnexpectedLow) {
    Dependencies localDependencies;
    if (dependencies != null) {
      localDependencies = dependencies;
    } else {
      if (warnIfStateIsUnexpectedLow) {
        if (state.isNotFurtherThan(DeploymentState.copyXml)) {
          logger.warn("Dependencies found as not complete in unexpected state " + state + " for object " + this + ", mode=" + mode);
        } else {
          logger.debug("Dependencies found as not complete in state " + state + " for object " + this + ", mode=" + mode);
        }
      }
      localDependencies = getDependenciesRecursively();
      localDependencies.addToBoth(this);
      if (localDependencies.complete != DependencyCompletion.notComplete) {
        dependencies = localDependencies;
      }
    }
    return localDependencies;
  }

  public static String escapeForCodeGenUsageInString(String s) {
    return s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", Matcher.quoteReplacement("\\\"")).replaceAll("[\n\r]", "");
  }

  public static String escapeForCodeGenUsageInComment(String s) {
    s = s.replaceAll(Pattern.quote("*/"), Matcher.quoteReplacement("* /")).replaceAll("[\n\r]", "");
    return s;
  }

  private boolean backupExistedBefore = false;

  public void setBackupExisted(boolean b) {
    backupExistedBefore = b;
  }

  public static String getFqXMLName(Document doc) {
    String name;
    Element root = doc.getDocumentElement();
    String rootTagName = root.getTagName();
    if (rootTagName.equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
      Element exceptionTypeElement = XMLUtils.getChildElementByName(root, GenerationBase.EL.EXCEPTIONTYPE);
      name =
          exceptionTypeElement.getAttribute(GenerationBase.ATT.TYPEPATH) + "."
              + exceptionTypeElement.getAttribute(GenerationBase.ATT.TYPENAME);
    } else {
      name = root.getAttribute(GenerationBase.ATT.TYPEPATH) + "." + root.getAttribute(GenerationBase.ATT.TYPENAME);
    }
    return name;
  }


  public static enum JavaVersion {
    JAVA5, JAVA6, JAVA7;
  }


  public static long calcSerialVersionUID(List<Pair<String, String>> types) {
    long ret = -1;
    for (Pair<String, String> type : types) {
      ret = ret * 31 + hash(type.getFirst());
      ret = ret * 31 + hash(type.getSecond());
    }
    return ret;
  }


  private static long hash(String s) {
    //von string.hashcode kopiert, soll aber unabhängig von jvm impl funktionieren
    int h = 1;
    int len = s.length();
    for (int i = 0; i < len; i++) {
      h = 31 * h + s.charAt(i);
    }
    return h;
  }

  public void throwExceptionIfNotExists() throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH {
    if (doesntExist) {
      throw new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(getOriginalFqName(), "TODO", "TODO"); //FIXME
    } else {
      if (realType != null) {
        throw new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(getOriginalFqName(), getTypeAsString(), realType);
      }
    }
  }
  
  
  public boolean exists() {
    return !doesntExist;
  }


  public String getTypeAsString() {
    String currentType;
    if (this instanceof DOM) {
      currentType = "Datatype";
    } else if (this instanceof WF) {
      currentType = "Workflow";
    } else {
      currentType = "Exception";
    }
    return currentType;
  }


  static DeploymentItemStateManagement getDeploymentItemStateManagement() {
    if (XynaFactory.isFactoryServer()) {
      DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      if (dism != null && dism.isInitialized()) {
        return dism;
      }
    }
    return null;
  }


  public boolean hasError() {
    return invalidated || exceptions.size() > 0 || state == DeploymentState.error;
  }

  private String comment;
  private Integer maxXmlId;
  
  public void setDeploymentComment(String comment) {
    this.comment = comment;
  }

  public String getDeploymentComment() {
    if (comment == null) {
      return "";
    }
    return comment;
  }


  public void addXmlId(String xmlIdStr) {
    try {
      if (xmlIdStr != null && xmlIdStr.matches("[0-9]+")) {
        addXmlId(Integer.parseInt(xmlIdStr));
      }
    } catch (NumberFormatException e) {} // nothing to do, since id is a string and therefore can't be in conflict with automatic id generator
  }


  public void addXmlId(Integer xmlId) {
    if( maxXmlId == null ) {
      maxXmlId = xmlId;
    } else {
      if( maxXmlId.intValue() < xmlId.intValue() ) {
        maxXmlId = xmlId;
      }
    }
  }
  
  public Integer getMaxXmlId() {
    return maxXmlId;
  }

  public Integer getNextXmlId() {
    if( maxXmlId == null ) {
      maxXmlId = 1;
    } else {
      maxXmlId = maxXmlId.intValue()+1;
    }
    return maxXmlId;
  }
  

  public RuntimeContext getRuntimeContext() {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return rm.getRuntimeContext(getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }

  public void setDoesntExist() {
    doesntExist = true;
  }
  
  public void resetState() {
    if (!state.isNotFurtherThan(DeploymentState.initializeDeploymentMode)) {
      state = DeploymentState.initializeDeploymentMode;
      internalParsingDone = false;
      doesntExist = false;
    }
  }
  
  public void setXMLInputSource(XMLSourceAbstraction input) {
    this.xmlInputSource = input;
  }

  /*
   * falls man code für die reservierten objekte neu generieren will, kann man sich hiermit für das deploymultiple die namen ausgeben lassen 
  public static void main(String[] args) {
    List<String> datatypes = new ArrayList<String>();
    List<String> exceptions = new ArrayList<String>();
    List<String> all = new ArrayList<String>();
    for (Entry<String, Class<?>> e : mdmObjectMappingToJavaClasses.entrySet()) {
      if (Exception.class.isAssignableFrom(e.getValue())) {
        exceptions.add(e.getKey());
      } else {
        datatypes.add(e.getKey());
      }
      all.add(e.getValue().getName());
    }
    StringBuilder sb = new StringBuilder();
    for (String dt : datatypes) {
      sb.append(dt).append(" ");
    }
    System.out.println("datatypes: " + sb.toString());
    StringBuilder sbEx = new StringBuilder();
    for (String dt : exceptions) {
      sbEx.append(dt).append(" ");
    }
    System.out.println("exceptions: " + sbEx.toString());
    Collections.sort(all);
    System.out.println("\nalle:");
    for (String s : all) {
      System.out.println(s);
    }
  }*/

  public boolean isAbstract() {
    return false;
  }
  
  
  public static interface XMLSourceAbstraction {
    
    Set<Long> getDependenciesRecursivly(Long revision);
    
    XMOMType determineXMOMTypeOf(String fqName, Long originalRevision) throws Ex_FileAccessException, XPRC_XmlParsingException;

    Long getRevisionDefiningXMOMObjectOrParent(String fqName, Long revision);
    
    RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
    
    boolean isOfRuntimeContextType(Long revision, RuntimeContextType type);
    
    Long getRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
    
    File getFileLocation(String originalFqName, Long revision, boolean fileFromDeploymentLocation);
    
    Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException;
    
    default File getClassOutputFolder() {
      return null;
    }
    
  }
  
  
  public static class FactoryManagedRevisionXMLSource implements XMLSourceAbstraction {
    
    public FactoryManagedRevisionXMLSource() {
    }

    public Set<Long> getDependenciesRecursivly(Long revision) {
      Set<Long> revisions = new HashSet<Long>();
      revisions.add(revision);
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, revisions);
      return revisions;
    }

    public Long getRevisionDefiningXMOMObjectOrParent(String fqName, Long revision) {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(fqName, revision);
    }

    public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    }

    public Long getRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rtc);
    }

    public Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
      File xmlFileLocation = getFileLocation(generator, fileFromDeploymentLocation);
      generator.validateXMLExistenceAndXSD(xmlFileLocation.getAbsolutePath());
      
      if (generator.doesntExist) {
        return null;
      }
      
      return XMLUtils.parse(xmlFileLocation, true);
    }

    public XMOMType determineXMOMTypeOf(String fqName, Long originalRevision) throws Ex_FileAccessException, XPRC_XmlParsingException {
      String rootTag = retrieveRootTag(fqName, originalRevision, true, false);
      return XMOMType.getXMOMTypeByRootTag(rootTag);
    }

    public File getFileLocation(GenerationBase generator, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
      String xmlFileLocation;
      if (fileFromDeploymentLocation || isOfRuntimeContextType(generator.getRevision(), RuntimeContextType.Application)) {
        if (overrideReservedServerObjectsForCodeGenUpdates && mdmObjectMappingToJavaClasses.containsKey(generator.originalFqName)) {
          xmlFileLocation = getFileLocationForSaving(generator.getOriginalFqName(), generator.getRevision());
        } else {
          xmlFileLocation = getFileLocationForDeployment(generator.getOriginalFqName(), generator.getRevision());
        }
       } else {
        xmlFileLocation = getFileLocationForSaving(generator.getOriginalFqName(), generator.getRevision());
      }
      xmlFileLocation += ".xml";
      return new File(xmlFileLocation);
    }

    public File getFileLocation(String originalFqName, Long revision, boolean fileFromDeploymentLocation) {
      if (fileFromDeploymentLocation) {
        return new File(getFileLocationForDeployment(originalFqName, revision));
      } else {
        return new File(getFileLocationForSaving(originalFqName, revision));
      }
    }
    
    private String getFileLocationForSaving(String fqXMLName, Long revision) {
      return new StringBuilder(RevisionManagement.getPathForRevision(PathType.XMOM, revision, false)).append(Constants.fileSeparator)
                      .append(fqXMLName.replaceAll("\\.", Constants.fileSeparator)).toString();
    }
    
    private String getFileLocationForDeployment(String fqXMLName, Long revision) {
      if (isReservedServerObjectByFqOriginalName(fqXMLName)) {
        if (isOfRuntimeContextType(revision, RuntimeContextType.Workspace)) {
          return getFileLocationForSaving(fqXMLName, revision);
        }
      }
      return new StringBuilder(RevisionManagement.getPathForRevision(PathType.XMOM, revision))
                    .append(Constants.fileSeparator)
                    .append(fqXMLName.replaceAll("\\.", Constants.fileSeparator)).toString();
    }

    public boolean isOfRuntimeContextType(Long revision, RuntimeContextType type) {
      try {
        return type == getRuntimeContext(revision).getType();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.debug("Failed to resolve revision.",e);
        return false;
      }
    }

  }
  
  
  public static class FileSystemXMLSource implements XMLSourceAbstraction {
    
    private final File targetClassFolder;
    private final Map<RuntimeContext, Set<RuntimeContext>> rtcDependencies;
    private final Map<RuntimeContext, File> rtcXMOMPaths;
    private final BijectiveMap<RuntimeContext, Long> revisions;
    
    public FileSystemXMLSource(Map<RuntimeContext, Set<RuntimeContext>> rtcDependencies, Map<RuntimeContext, File> rtcXMOMPaths, File targetClassFolder) {
      this.rtcDependencies = rtcDependencies;
      this.rtcXMOMPaths = rtcXMOMPaths;
      this.targetClassFolder = targetClassFolder;
      revisions = new BijectiveMap<>();
      long revision = 1;
      for (RuntimeContext rtx : rtcXMOMPaths.keySet()) {
        revisions.put(rtx, revision++);
      }
    }

    public Set<Long> getDependenciesRecursivly(Long revision) {
      Set<Long> dependencies = new HashSet<>();
      getDependenciesRecursivlyInternally(revision, dependencies);
      dependencies.add(revision);
      return dependencies;
    }
    
    
    private void getDependenciesRecursivlyInternally(Long revision, Set<Long> depenedencies) {
      RuntimeContext rtc = revisions.getInverse(revision);
      if (rtcDependencies.containsKey(rtc)) {
        Set<RuntimeContext> deps = rtcDependencies.get(rtc);
        for (RuntimeContext dep : deps) {
          Long depRev = revisions.get(dep);
          if (depenedencies.add(depRev)) {
            getDependenciesRecursivlyInternally(depRev, depenedencies);
          }
        }
      }
    }

    public Long getRevisionDefiningXMOMObjectOrParent(String fqName, Long revision) {
      String fqPath = fqName.replaceAll("\\.", Constants.fileSeparator) + ".xml";
      Set<Long> allRevs = getDependenciesRecursivly(revision);
      for (Long aRev : allRevs) {
        RuntimeContext rtc = revisions.getInverse(aRev);
        File rtcRootPath = rtcXMOMPaths.get(rtc);
        try {
          if (Files.walk(java.nio.file.Path.of(rtcRootPath.getAbsolutePath()))
                   .anyMatch(path -> path.endsWith(fqPath))) {
            return aRev;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return revision;
    }

    public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return revisions.getInverse(revision);
    }

    public Long getRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return revisions.get(rtc);
    }

    public Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
      // TODO let's assume the generator revision is already determined by getRevisionDefiningXMOMObjectOrParent
      File file = getFileLocation(generator, fileFromDeploymentLocation);
      if (!file.exists()) {
        throw new Ex_FileAccessException(file.getAbsolutePath());
      }
      return XMLUtils.parse(file, true);
    }

    public XMOMType determineXMOMTypeOf(String fqName, Long revision)
        throws Ex_FileAccessException, XPRC_XmlParsingException {
      File file = getFileLocation(fqName, revision, true);
      try (FileInputStream fis =
          new FileInputStream(file)) {
        return XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(fis));
      } catch (FileNotFoundException e) {
        throw new Ex_FileAccessException(file.getAbsolutePath(), e);
      } catch (IOException e) {
        throw new XPRC_XmlParsingException(file.getAbsolutePath(), e);
      } catch (XMLStreamException e) {
        throw new XPRC_XmlParsingException(file.getAbsolutePath(), e);
      }
    }

    private File getFileLocation(GenerationBase generator, boolean fileFromDeploymentLocation) {
      return getFileLocation(generator.getOriginalFqName(), generator.getRevision(), fileFromDeploymentLocation);
    }
    
    public File getFileLocation(String fqName, Long revision, boolean fileFromDeploymentLocatio) {
      String fqPath = fqName.replaceAll("\\.", Constants.fileSeparator) + ".xml";
      return new File(rtcXMOMPaths.get(revisions.getInverse(revision)), fqPath);
    }

    public boolean isOfRuntimeContextType(Long revision, RuntimeContextType type) {
      return type == revisions.getInverse(revision).getType();
    }
    
    // used from BuildDatatypeJarFromSource
    public File getXMOMPath(RuntimeContext rtc) {
      return rtcXMOMPaths.get(rtc);
    }

    public File getClassOutputFolder() {
      return targetClassFolder;
    }
    
  }
  
  public static class StringXMLSource implements XMLSourceAbstraction {
    
    private final Map<String, String> xmlsWfAndImports; // maps from fqn to XML-string
    
    public StringXMLSource(Map<String, String> xmlsWfAndImports) {
      this.xmlsWfAndImports = xmlsWfAndImports;
    }

    public Set<Long> getDependenciesRecursivly(Long revision) {
      throw new UnsupportedOperationException("StringXMLSource.getDependenciesRecursivly");
    }

    public Long getRevisionDefiningXMOMObjectOrParent(String fqName, Long revision) {
      throw new UnsupportedOperationException("StringXMLSource.getRevisionDefiningXMOMObjectOrParent");
    }

    public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      throw new UnsupportedOperationException("StringXMLSource.getRuntimeContext");
    }

    public Long getRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      throw new UnsupportedOperationException("StringXMLSource.getRevision");
    }
    
    public Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation) throws Ex_FileAccessException, XPRC_XmlParsingException {
      String xml = xmlsWfAndImports.get(generator.originalFqName);
      return XMLUtils.parseString(xml, true);
    }

    public XMOMType determineXMOMTypeOf(String fqName, Long originalRevision) throws Ex_FileAccessException, XPRC_XmlParsingException {
      String xml = xmlsWfAndImports.get(fqName);
      try (ByteArrayInputStream bais =
          new ByteArrayInputStream(xml.getBytes())) {
        return XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(bais));
      } catch (XMLStreamException e) {
        throw new XPRC_XmlParsingException(fqName, e);
      } catch (IOException e) {
        throw new XPRC_XmlParsingException(fqName, e);
      }
    }

    public File getFileLocation(String originalFqName, Long revision, boolean fileFromDeploymentLocation) {
      throw new UnsupportedOperationException("StringXMLSource.getFileLocation");
    }

    public boolean isOfRuntimeContextType(Long revision, RuntimeContextType type) {
      throw new UnsupportedOperationException("StringXMLSource.isOfRuntimeContextType");
    }

  }

}
