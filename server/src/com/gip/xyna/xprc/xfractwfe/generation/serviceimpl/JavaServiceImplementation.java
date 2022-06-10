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
package com.gip.xyna.xprc.xfractwfe.generation.serviceimpl;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.InterfaceVersion;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch.ExceptionHierarchyComparator;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



/**
 * wie sieht der generierte code aus, der für die eclipse projekte generiert wird
 */
public class JavaServiceImplementation {

  public final static Logger logger = CentralFactoryLogging.getLogger(JavaServiceImplementation.class);
  
  private final static String STATIC_OPERATION_INTERFACE_SUFFIX = "ServiceOperation";
  private final static String NONSTATIC_OPERATION_INTERFACE_SUFFIX = "InstanceOperation";
  public final static String STATIC_OPERATION_IMPL_SUFFIX = "ServiceOperationImpl";
  public final static String NONSTATIC_OPERATION_IMPL_SUFFIX = "InstanceOperationImpl";
  private final static String STATIC_OPERATION_IMPL_INSTANCE = "serviceOperationInstance";
  private final static String NONSTATIC_OPERATION_IMPL_INSTANCE = "instanceOperationInstance";
  private final static String SUPER_PROXY_NAME_SUFFIX = "SuperProxy";
  private final static String SUPER_PROXY_INTERNALSUPERCALL_DELEGATION_VARNAME = "internalSuperCallDelegation";
  private final static String SUPER_PROXY_DELEGATE_JAVA_DIRECTLY_SUFFIX = "_delegateToJavaImplDirectly";

  private final DOM dom;
  private final InterfaceVersion version;


  public JavaServiceImplementation(DOM dom, InterfaceVersion version) {
    this.dom = dom;
    this.version = version;
  }


  private boolean hasStaticOperations(boolean checkVersion) {
    for (Operation op : dom.getOperations()) {
      if (op.isStatic()) {
        if (!checkVersion || isSameVersion(op.getVersion(), version)) {
          return true;
        }
      }
    }
    return false;
  }


  private boolean hasNonStaticOperations(boolean checkVersion) {
    for (Operation op : dom.getOperations()) {
      if (!op.isStatic()) {
        if (!checkVersion || isSameVersion(op.getVersion(), version)) {
          return true;
        }
      }
    }
    return false;
  }


  public String createDelegationImplCode() {
    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    cb.addLine("package " + getPackage()).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    boolean hasStaticOperationImpl = hasStaticOperations(false) || !dom.isAbstract(); //auch ondeployment erlauben, wenn nicht abstrakt + instanzmethoden 
    boolean hasSuperTypeWithJavaImpl = dom.hasSuperTypeWithJavaImpl(true, null);
    if (!hasSuperTypeWithJavaImpl) {
      cb.addLine("import ", Serializable.class.getName());
    }
    if (!importedClassesFullyQualified.contains(HashMap.class.getName())) {
      cb.addLine("import ", HashMap.class.getName());
    }
    if (!importedClassesFullyQualified.contains(Map.class.getName())) {
      cb.addLine("import ", Map.class.getName());
    }
    String delegationSuperClassName = null;

    if (hasSuperTypeWithJavaImpl) {
      delegationSuperClassName = getDelegationImplFQClassName(dom.getNextSuperTypeWithJavaImpl(false, null));
      cb.addLine("import ", delegationSuperClassName);
    }

    cb.addLB(2).add("public ");
    if (dom.isAbstract()) {
      cb.add("abstract ");
    }
    cb.add("class ", getDelegationImplSimpleClassName());

    if (hasSuperTypeWithJavaImpl) {
      cb.add(" extends ", GenerationBase.getSimpleNameFromFQName(delegationSuperClassName));
    }
    boolean implementsExists = false;
    if (hasStaticOperationImpl) {
      implementsExists = true;
      cb.add(" implements ", ExtendedDeploymentTask.class.getSimpleName());
    }
    if (!hasSuperTypeWithJavaImpl) {
      if (!implementsExists) {
        cb.add(" implements ");
      } else {
        cb.add(", ");
      }
      cb.add(Serializable.class.getSimpleName());
    }
    cb.add(" {").addLB(2);
    appendSerialVersionUID(cb);
    
    InterfaceVersion[] versions = getVersionsWithStaticOperations(true);
    if (versions.length <= 0) { // appears to only contain instance services, use current version if not abstract
      if (hasStaticOperationImpl) {
        versions = new InterfaceVersion[1];
        versions[0] = InterfaceVersion.BASE;
      }
    }

    for (InterfaceVersion version : versions) {
      cb.addLine("private static final ", getProjectStaticImplFQClassName(version), " ", getVarNameStaticOperationImplInstance(version),
                 " = new ", getProjectStaticImplFQClassName(version), "()").addLB();
    }

    if (getOperations(false).length > 0 && dom.hasJavaImpl(true, null)) {
      //nonstatic impl-instanzen+getter
      //es gibt in der hierarchie instanzmethoden && (library exists || es gibt instanzmethoden, die in javalib delegieren)
      for (InterfaceVersion version : dom.getVersionsOfOperations(true)) {
        if (!dom.hasSuperTypeWithJavaImpl(true, version)) {
          String v = getVarNameNonstaticOperationImplInstance(version);
          cb.addLine("protected final ", getProjectNonStaticImplFQClassName(version), " ", v).addLB();
          cb.addLine("public ", getProjectNonStaticImplFQClassName(version), " ", GenerationBase.buildGetter(v),
              "() {");
          cb.addLine("return (", getProjectNonStaticImplFQClassName(version), ") ", v);
          cb.addLine("}").addLB();
        }
      }

      //helpermethode für konstruktoren
      cb.addLine("private static Map<String, Object> setImpls(Map<String, Object> map, ", dom.getFqClassName(), " o) {");
      if (!dom.isAbstract()) {
        boolean first = true;
        for (InterfaceVersion version : dom.getVersionsOfOperations(true)) {
          if (first) {
            cb.add("Object ");
            first = false;
          }
          cb.add("versionImpl = map.get(\"" + version.getNameCompatibleWithCurrentVersion() + "\")").addLB();
          cb.addLine("if (versionImpl == null) {");
          cb.addLine("versionImpl = new ", getProjectNonStaticImplFQClassName(version), "(o)");
          cb.addLine("map.put(\"", version.getNameCompatibleWithCurrentVersion(), "\", versionImpl)");
          cb.addLine("}");
        }
      }
      cb.addLine("return map");
      cb.addLine("}").addLB();


      //konstruktor 1: wird von datentyp.init methode aus aufgerufen (oder super-aufrufe von altem (< 5.1.3) generierten code)
      cb.addLine("public ", getDelegationImplSimpleClassName(), "(", dom.getFqClassName(), " o) {");
      cb.addLine("this(o, new HashMap<String, Object>())");
      cb.addLine("}").addLB();

      //konstruktor 2a: wird von abgeleiteter klasse aufgerufen
      cb.addLine("public ", getDelegationImplSimpleClassName(), "(", dom.getFqClassName(), " o, Map<String, Object> implMap) {");
      if (dom.hasSuperTypeWithJavaImpl(true, null)) {
        cb.addLine("super(o, setImpls(implMap, o))");
      } else {
        cb.addLine("setImpls(implMap, o)");
      }
      for (InterfaceVersion version : dom.getVersionsOfOperations(true)) {
        if (!dom.hasSuperTypeWithJavaImpl(true, version)) {
          //impl ist hier definiert, wurde aber evtl in einem sub-typ instanziiert und über die map hierher delegiert
          cb.addLine("this.", getVarNameNonstaticOperationImplInstance(version), " = (", getProjectNonStaticImplFQClassName(version),
                     ") implMap.get(\"", version.getNameCompatibleWithCurrentVersion(), "\")");
        }
      }
      cb.addLine("}").addLB();

      //konstruktor 2b: für abwärtskompatibilität bezüglich generiertem code <= version 5.1.3
      cb.addLine("public ", getDelegationImplSimpleClassName(), "(", getProjectNonStaticImplFQClassName(InterfaceVersion.BASE), " o) {");
      cb.addLine("this(o == null ? null : ((", getSuperProxySimpleClassName(InterfaceVersion.BASE), /*protected methode in gleichem package*/
                 ") o).getInstanceVar(), o == null ? new HashMap<String, Object>() : new HashMap<String, Object>(",
                 Collections.class.getName(), ".singletonMap(\"" + InterfaceVersion.BASE.getNameCompatibleWithCurrentVersion() + "\", o)))");
      cb.addLine("}").addLB();
    }

    //konstruktor 3: wird von deploymenthandling verwendet, um die onDeployment() methoden aufzurufen
    if (!dom.isAbstract()) {
      cb.addLine("public ", getDelegationImplSimpleClassName(), "() {");
      if (dom.hasSuperTypeWithJavaImpl(true, null)) {
        cb.addLine("super(null, new HashMap<String, Object>())");
      }
      //else leerer super-aufruf
      for (InterfaceVersion version : dom.getVersionsOfOperations(true)) {
        if (!dom.hasSuperTypeWithJavaImpl(true, version) && hasNonStaticOperations(false)) {
          cb.addLine("this.", getVarNameNonstaticOperationImplInstance(version), " = null");
        }
      }
      cb.addLine("}").addLB();
    } 

    if (hasStaticOperationImpl) {
      //FIXME falls abstrakt, werden die nie aufgerufen. das ist für den eclipse user verwirrend
      cb.addLine("public void onDeployment() throws ", XynaException.class.getSimpleName(), " {");
      for (InterfaceVersion version : versions) {
        cb.addLine(getVarNameStaticOperationImplInstance(version), ".onDeployment()");
      }
      cb.addLine("}").addLB();

      cb.addLine("public void onUndeployment() throws ", XynaException.class.getSimpleName(), " {");
      for (InterfaceVersion version : versions) {
        cb.addLine(getVarNameStaticOperationImplInstance(version), ".onUndeployment()");
      }
      cb.addLine("}").addLB();

      cb.addLine("public Long getOnUnDeploymentTimeout() {");
      cb.addLine("Long ret = null");
      cb.addLine("Long t");
      for (InterfaceVersion version : versions) {
        cb.addLine("t = ", getVarNameStaticOperationImplInstance(version), ".getOnUnDeploymentTimeout()");
        cb.addLine("if (t != null) {");
        cb.addLine("if (ret == null) {");
        cb.addLine("ret = t");
        cb.addLine("} else {");
        cb.addLine("ret += t");
        cb.addLine("}");
        cb.addLine("}");
      }
      cb.addLine("return ret");
      cb.addLine("}").addLB();

      cb.addLine("public ", BehaviorAfterOnUnDeploymentTimeout.class.getSimpleName(), " getBehaviorAfterOnUnDeploymentTimeout() {");
      cb.addLine(BehaviorAfterOnUnDeploymentTimeout.class.getSimpleName(), " ret = null");
      //TODO versionen sortieren, und immer die neuste statische instanz nehmen?
      for (InterfaceVersion version : versions) {
        cb.addLine("ret = ", getVarNameStaticOperationImplInstance(version), ".getBehaviorAfterOnUnDeploymentTimeout()");
        cb.addLine("if (ret != null) {");
        cb.addLine("return ret");
        cb.addLine("}");
      }
      cb.addLine("return ret");
      cb.addLine("}").addLB();
    }

    for (Operation operation : dom.getOperations()) {
      if (operation.isAbstract()) {
        continue;
      }
      if (!(operation instanceof JavaOperation)) {
        continue;
      }
      cb.add("public ");
      if (operation.isStatic()) {
        cb.add("static ");
      }
      operation.createMethodSignature(cb, false, importedClassesFullyQualified, operation.getName());
      cb.add(" {").addLB();
      if (operation.getOutputVars() != null && operation.getOutputVars().size() > 0) {
        cb.add("return ");
      }
      String instanceName;
      if (operation.isStatic()) {
        instanceName = getVarNameStaticOperationImplInstance(operation.getVersion());
      } else {
        instanceName =
            "((" + getProjectNonStaticImplFQClassName(operation.getVersion()) + ") "
                + getVarNameNonstaticOperationImplInstance(operation.getVersion()) + ")";
      }
      operation.generateJavaForInvocation(cb, instanceName + "." + operation.getNameWithoutVersion());
      cb.addLB().addLine("}").addLB();
    }
    
    /*
     * MyInstanceOperationImpl instanceOperationInstance;
     * 
     * public MyImpl clone() {
     *   Method cloneMethod = DOM.getPublicCloneMethodIfPresent(instanceOperationInstance.getClass());
     *   if (cloneMethod != null) {
     *     try {
     *       return new MyImpl((MyInstanceOperationImpl) cloneMethod.invoke(instanceOperationInstance));
     *     } catch (IllegalAccessException e) {
     *     } catch (InvocationTargetException e) {
     *     }
     *   }
     *   return new MyImpl(((MyInstanceOperationImpl) instanceOperationInstance).getInstanceVar());
     * }
     */
    OperationInformation[] instanceOperations = dom.collectOperationsOfDOMHierarchy(false);
    boolean hasImplInstanceVar =
        instanceOperations.length > 0 && dom.libraryExists();
    if (hasImplInstanceVar && !dom.isAbstract()) {
      cb.addLine("public ", getDelegationImplSimpleClassName(), " clone() {");
      cb.addLine(Method.class.getName(), " cloneMethod = ", DOM.class.getName(), ".getPublicCloneMethodIfPresent(",
                 getVarNameNonstaticOperationImplInstance(InterfaceVersion.BASE), ".getClass())");
      cb.addLine("if (cloneMethod != null) {");
      cb.addLine("try {");
      cb.addLine(getProjectNonStaticImplFQClassName(InterfaceVersion.BASE), " implclone = (",
                 getProjectNonStaticImplFQClassName(InterfaceVersion.BASE), ") cloneMethod.invoke(",
                 getVarNameNonstaticOperationImplInstance(InterfaceVersion.BASE), ")");
      cb.addLine("return new ", getDelegationImplSimpleClassName(), "(implclone)"); //TODO das ist aber unschön, diesen konstruktor aufzurufen, der steht oben als "nur aus abwärtskompatibilität" markiert.
      cb.addLine("} catch (", IllegalAccessException.class.getName(), " e) {");
      cb.addLine("} catch (", InvocationTargetException.class.getName(), " e) {");
      cb.addLine("}"); //end try      
      cb.addLine("}"); //end if
      //null oder fehler beim clone
      cb.addLine("return new ", getDelegationImplSimpleClassName(), "(((", getProjectNonStaticImplFQClassName(InterfaceVersion.BASE), ") ",
                 getVarNameNonstaticOperationImplInstance(InterfaceVersion.BASE), ").getInstanceVar())");
      cb.addLine("}").addLB();
    }
    cb.addLine("}");
    return cb.toString();
  }
  

  private InterfaceVersion[] getVersionsWithStaticOperations(boolean onlyOneCurrentVersion) {
    Set<InterfaceVersion> set = new HashSet<InterfaceVersion>();
    boolean containsCurrentVersion = false;
    for (Operation op : dom.getOperations()) {
      if (op.isStatic()) {
        if (onlyOneCurrentVersion && op.getVersion().isCurrentVersion()) {
          if (containsCurrentVersion) {
            continue;
          }
          containsCurrentVersion = true;
        }
        set.add(op.getVersion());
      }
    }
    return set.toArray(new InterfaceVersion[set.size()]);
  }


  private OperationInformation[] getOperations(boolean includeStatic) {
    return dom.collectOperationsOfDOMHierarchy(includeStatic);
  }


  private String getPackage() {
    return GenerationBase.getPackageNameFromFQName(dom.getFqClassName());
  }


  public String createProjectStaticImplCode() {
    boolean hasStaticOperationImpl = hasStaticOperations(true) || !dom.isAbstract();

    if (!hasStaticOperationImpl) {
      return null;
    }
    CodeBuffer cb = new CodeBuffer("Development");
    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    String pkg = GenerationBase.getPackageNameFromFQName(getProjectStaticImplFQClassName(version));
    cb.addLine("package ", pkg).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    if (hasStaticOperations(true)) {
      cb.addLine("import ", getInterfaceStaticFQClassName());
    }

    cb.addLB().addLB().add("public ");
    cb.add("class ");
    cb.add(getProjectStaticImplSimpleClassName(), " implements " + ExtendedDeploymentTask.class.getSimpleName(),
           hasStaticOperations(true) ? (", " + getInterfaceStaticSimpleClassName()) : "");
    cb.add(" {").addLB(2);

    if (hasStaticOperationImpl) {
      cb.addLine("public void onDeployment() throws " + XynaException.class.getSimpleName() + " {");
      cb.addLine("// TODO do something on deployment, if required");
      cb.addLine("// This is executed again on each classloader-reload, that is each");
      cb.addLine("// time a dependent object is redeployed, for example a type of an input parameter.");
      cb.addLine("}").addLB();

      cb.addLine("public void onUndeployment() throws " + XynaException.class.getSimpleName() + " {");
      cb.addLine("// TODO do something on undeployment, if required");
      cb.addLine("// This is executed again on each classloader-unload, that is each");
      cb.addLine("// time a dependent object is redeployed, for example a type of an input parameter.");
      cb.addLine("}").addLB();

      cb.addLine("public Long getOnUnDeploymentTimeout() {");
      cb.addLine("// The (un)deployment runs in its own thread. The service may define a timeout");
      cb.addLine("// in milliseconds, after which Thread.interrupt is called on this thread.");
      cb.addLine("// If null is returned, the default timeout (defined by " + XynaProperty.class.getSimpleName() + " "
          + XynaProperty.DEPLOYMENTHANDLER_TIMEOUT.toString() + ") will be used.");
      cb.addLine("return null;");
      cb.addLine("}").addLB();

      String cn = BehaviorAfterOnUnDeploymentTimeout.class.getSimpleName();
      cb.addLine("public ", cn, " getBehaviorAfterOnUnDeploymentTimeout() {");
      cb.addLine("// Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.");
      cb.addLine("// - " + cn + "." + BehaviorAfterOnUnDeploymentTimeout.EXCEPTION.name()
          + ": Deployment will be aborted, while undeployment will log the exception and NOT abort.");
      cb.addLine("// - " + cn + "." + BehaviorAfterOnUnDeploymentTimeout.IGNORE.name()
          + ": (Un)Deployment will be continued in another thread asynchronously.");
      cb.addLine("// - " + cn + "." + BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD.name()
          + ": (Un)Deployment will be continued after calling Thread.stop on the thread.");
      cb.addLine("//   executing the (Un)Deployment.");
      cb.addLine("// If null is returned, the factory default <IGNORE> will be used.");
      cb.addLine("return null");
      cb.addLine("}").addLB();
    }

    appendOperationsForProjectImpl(cb, true, importedClassesFullyQualified);
    cb.addLine("}");
    return cb.toString();
  }


  private void imports(CodeBuffer cb, HashSet<String> implDontImport, HashSet<String> importedClasses,
                       HashSet<String> importedClassesFullyQualified, HashSet<String> fullQualificationRequiringClasses) {
    implDontImport.add(XynaProcessing.class.getName());
    implDontImport.add(XynaOrder.class.getName());
    implDontImport.add(DestinationKey.class.getName());

    boolean previousContainedJavaPrefix = false;
    boolean previousContainedXynaPrefix = false;
    for (String importAsString : dom.getImports()) {
      if (implDontImport.contains(importAsString)) {
        continue;
      }
      if (importAsString.contains(".")) {
        String currentSimpleClassName = importAsString.substring(importAsString.lastIndexOf(".") + 1);
        if (!importedClasses.contains(currentSimpleClassName)) {

          if (importAsString.startsWith("java.")) {
            if (!previousContainedJavaPrefix) {
              cb.addLB();
            }
            previousContainedJavaPrefix = true;
            previousContainedXynaPrefix = false;
          } else if (importAsString.startsWith("com.gip.xyna.")) {
            if (!previousContainedXynaPrefix) {
              cb.addLB();
            }
            previousContainedXynaPrefix = true;
            previousContainedJavaPrefix = false;
          } else {
            if (previousContainedJavaPrefix || previousContainedXynaPrefix) {
              cb.addLB();
            }
            previousContainedXynaPrefix = false;
            previousContainedJavaPrefix = false;
          }

          cb.addLine("import ", importAsString);
          importedClasses.add(currentSimpleClassName);
          importedClassesFullyQualified.add(importAsString);

        } else {
          fullQualificationRequiringClasses.add(importAsString);
        }
      } else {
        cb.addLine("import ", GenerationBase.DEFAULT_PACKAGE, ".", importAsString);
      }
    }
  }


  private void appendOperationsForProjectImpl(CodeBuffer cb, boolean staticOps, Set<String> importedClassesFullyQualified) {
    for (OperationInformation operationInformation : getOperations(staticOps)) {
      Operation operation = operationInformation.getOperation();
      if (staticOps && !operation.isStatic()) {
        continue;
      }
      if (!staticOps && operation.isStatic()) {
        continue;
      }
      if (!staticOps && operationInformation.isFinalInSuperType()) {
        continue;
      }
      if (!staticOps && !operationInformation.isImplementedHere()) {
        continue;
      }
      if (!isSameVersion(operation.getVersion(), version)) {
        continue;
      }
      cb.add("public ");
      if (operation.isAbstract()) {
        cb.add("abstract ");
      }
      operation.createMethodSignature(cb, false, importedClassesFullyQualified, operation.getNameWithoutVersion());
      if (operation.isAbstract()) {
        cb.addLB(2);
        continue;
      }
      cb.add(" {").addLB();
      //methode kann in den fällen, wo sie als workflow oder codesnippet implementiert ist, nicht weggelassen oder abstrakt gemacht werden
      //weil die impl-instanz noch instanziierbar sein muss
      if (operation instanceof WorkflowCall) {
        cb.addLine("// Implemented as workflow!");
        returnNothing(cb, operation.getOutputVars());
      } else if (!operation.implementedInJavaLib()) {
        cb.addLine("// Implemented as code snippet!");
        returnNothing(cb, operation.getOutputVars());
      } else {
        ((JavaOperation) operation).createProjectServiceImpl(cb, importedClassesFullyQualified);
      }
      cb.addLine("}").addLB();
    }

  }

  /**
   * gibt zurück, ob die beiden versionen gleich behandelt werden sollen. typischerfall der nicht-identität ist, dass
   * die eine operation keine versionsangabe hat, und die andere die currentversion ist. 
   */
  private boolean isSameVersion(InterfaceVersion v1, InterfaceVersion v2) {
    if (v1.equals(v2)) {
      return true;
    }
    if (v1.isCurrentVersion() && v2.isCurrentVersion()) {
      //bei geerbten methoden kann es nun sein, dass die versionen echt verschieden sind
      //also in Type A ist v_x als currentVersion markiert, in Type B extends A ist v_y als currentVersion markiert
      //das ist dann aber ein bug -> das kann man nicht sinnvoll reparieren. TODO oder?
      if (v1 == InterfaceVersion.BASE || v2 == InterfaceVersion.BASE) {
        return true;
      }
    }
    return false;
  }


  public String createProjectNonStaticImplCode() {
    //auch erzeugen, wenn es keine operations gibt! z.b. leitet sshnetconfconnection von sshconnection nur deshalb ab, um nicht modellierte operations zu überschreiben

    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    String pkg = GenerationBase.getPackageNameFromFQName(getProjectNonStaticImplFQClassName(version));
    cb.addLine("package ", pkg).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    cb.addLine("import ", getSuperProxyFQClassName());
    if (hasNonStaticOperations(true)) {
      cb.addLine("import ", getInterfaceNonStaticFQClassName());
    }
    if (!pkg.equals(getPackage())) {
      cb.addLine("import ", dom.getFqClassName());
    }

    cb.addLB(2);
    cb.add("public ");
    if (dom.isAbstract()) {
      cb.add("abstract ");
    }
    cb.add("class ");
    cb.add(getProjectNonStaticImplSimpleClassName(), " extends ", getSuperProxySimpleClassName());
    if (hasNonStaticOperations(true)) {
      cb.add(" implements ", getInterfaceNonStaticSimpleClassName());
    }
    cb.add(" {").addLB(2);
    appendSerialVersionUID(cb);

    //konstruktor
    cb.addLine("public ", getProjectNonStaticImplSimpleClassName(), "(", dom.getSimpleClassName(), " instanceVar) {");
    cb.addLine("super(instanceVar)");
    cb.addLine("}").addLB();

    appendOperationsForProjectImpl(cb, false, importedClassesFullyQualified);

    cb.addLine("private void writeObject(", ObjectOutputStream.class.getName(), " s) throws ", IOException.class.getName(), " {");
    cb.addLine("//change if needed to store instance context");
    cb.addLine("s.defaultWriteObject()");
    cb.addLine("}").addLB();

    cb.addLine("private void readObject(", ObjectInputStream.class.getName(), " s) throws ", IOException.class.getName(), ", ",
               ClassNotFoundException.class.getSimpleName(), " {");
    cb.addLine("//change if needed to restore instance-context during deserialization of order");
    cb.addLine("s.defaultReadObject()");
    cb.addLine("}").addLB();

    cb.addLine("}");
    return cb.toString();
  }


  private void appendSerialVersionUID(CodeBuffer cb) {
    cb.addLine("private static final long serialVersionUID = 1L").addLB();
  }


  public String createSuperProxyCode() {
    //auch erzeugen, wenn es keine operations gibt! z.b. leitet sshnetconfconnection von sshconnection nur deshalb ab, um nicht modellierte operations zu überschreiben

    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    String pkg = getPackage();
    cb.addLine("package ", pkg).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    boolean hasSuperTypeWithJavaImpl = dom.hasSuperTypeWithJavaImpl(true, version);
    if (hasSuperTypeWithJavaImpl) {
      cb.addLine("import ", getProjectNonStaticImplFQClassName(dom.getNextSuperTypeWithJavaImpl(true, version), version));
    }

    cb.addLB(2);

    //klasse kann immer abstrakt sein. ist insbesondere dann notwendig, wenn die oberklasse abstrakt ist. sonst schadet es aber nichts
    cb.add("public abstract class ", getSuperProxySimpleClassName());
    if (hasSuperTypeWithJavaImpl) {
      cb.add(" extends ",
             //TODO muss in der oberklasse das package die gleiche versions-zuordnung haben? oder kann es sein, dass
             //die version hier "currentVersion" ist, und in der oberklasse nicht oder andersherum?
             GenerationBase.getSimpleNameFromFQName(getProjectNonStaticImplFQClassName(dom.getNextSuperTypeWithJavaImpl(true, version), version)));
    } else {
      cb.add(" implements ", Serializable.class.getName());
    }
    cb.add(" {").addLB(2);
    appendSerialVersionUID(cb);

    if (!hasSuperTypeWithJavaImpl) {
      cb.addLine("protected ", dom.getSimpleClassName(), " instanceVar").addLB();
    }

    cb.addLine("protected ", dom.getSimpleClassName(), " getInstanceVar() {");
    cb.addLine("return (", dom.getSimpleClassName(), ") instanceVar");
    cb.addLine("}").addLB();

    if (dom.hasSuperTypeWithInstanceMethods(version)) {
      //TODO variable nicht generieren, wenn sie nicht benötigt werden (also es gibt nur final und abstract methoden im supertype)
      cb.addLine("private final ", dom.getSuperClassGenerationObject().getSimpleClassName(), " ",
                 SUPER_PROXY_INTERNALSUPERCALL_DELEGATION_VARNAME, " = new ", dom.getSuperClassGenerationObject().getSimpleClassName(),
                 "() {");
      cb.addLB();
      appendSerialVersionUID(cb);

      for (OperationInformation operationInformation : dom.getSuperClassGenerationObject().collectOperationsOfDOMHierarchy(false)) {
        //alle abstrakten methoden
        //alle nicht final methoden
        if (operationInformation.isFinal()) {
          continue;
        }
        if (!isSameVersion(operationInformation.getOperation().getVersion(), version)) {
          continue;
        }
        cb.add("public ");
        operationInformation.getOperation().createMethodSignature(cb, false, importedClassesFullyQualified,
                                                                  operationInformation.getOperation().getName());
        cb.add(" {").addLB();
        if (operationInformation.isAbstract()) {
          //abstrakte methode ist nur deshalb vorhanden, weil sonst das delegationobjekt nicht instanziiert werden kann
          returnNothing(cb, operationInformation.getOperation().getOutputVars() );
        } else {
          if (operationInformation.isNotAbstractInJavaServiceImpl()) {
            //super delegation. nur, wenn es auch eine javaimplementierung obendrüber gibt
            if (operationInformation.getOperation().getOutputVars() != null
                && operationInformation.getOperation().getOutputVars().size() > 0) {
              cb.add("return ");
            }
            cb.add(getSuperProxySimpleClassName(), ".super.");
            operationInformation.getOperation().generateJavaForInvocation(cb, operationInformation.getOperation().getNameWithoutVersion());
            cb.addLB();
          } else {
            if (operationInformation.getOperation().getOutputVars() != null
                && operationInformation.getOperation().getOutputVars().size() > 0) {
              cb.addLine("return null");
            }
          }
        }
        cb.addLine("}").addLB();
      }

      //clone methode überschreiben, falls abstrakt in supertype
      if (dom.getSuperClassGenerationObject().isAbstract()) {
        cb.addLine("public ", dom.getSuperClassGenerationObject().getSimpleClassName(), " clone(boolean deep) {");
        cb.addLine("return null");
        cb.addLine("}").addLB();
        cb.addLine("public ", dom.getSuperClassGenerationObject().getSimpleClassName(), " clone() {");
        cb.addLine("return null");
        cb.addLine("}").addLB();
        cb.addLine("protected void ", DOM.INIT_METHODNAME, "() {");
        cb.addLine("}").addLB();
      }

      cb.addLine("};").addLB();
    }

    //konstruktor
    cb.addLine("protected ", getSuperProxySimpleClassName(), "(", dom.getSimpleClassName(), " instanceVar) {");
    if (hasSuperTypeWithJavaImpl) {
      cb.addLine("super(instanceVar)");
    } else {
      cb.addLine("this.instanceVar = instanceVar");
    }
    cb.addLine("}").addLB();

    if (dom.hasSuperTypeWithInstanceMethods(version)) {
      for (OperationInformation operationInformation : dom.getSuperClassGenerationObject().collectOperationsOfDOMHierarchy(false)) {
        if (!operationInformation.getOperation().getVersion().equals(version)) {
          continue;
        }
        //java-delegation direkt
        if (operationInformation.isNotAbstractInJavaServiceImpl()) {
          //achtung: evtl erwartet man die methode, wenn die oberklasse ein jarfile definiert, welches die methode in java implementiert
          //die existenz des jarfiles muss aber nicht bedeuten, dass die methode auch existiert.
          //kann man das irgendwie besser abfragen??
          cb.add("protected ");
          operationInformation.getOperation().createMethodSignature(cb,
                                                                    false,
                                                                    importedClassesFullyQualified,
                                                                    operationInformation.getOperation().getNameWithoutVersion()
                                                                        + SUPER_PROXY_DELEGATE_JAVA_DIRECTLY_SUFFIX);
          cb.add(" {").addLB();
          if (operationInformation.getOperation().getOutputVars() != null && operationInformation.getOperation().getOutputVars().size() > 0) {
            cb.add("return ");
          }
          operationInformation.getOperation().generateJavaForInvocation(cb, "super." + operationInformation.getOperation().getNameWithoutVersion());
          cb.addLB();
          cb.addLine("}").addLB();
        }

        //delegation für finale methoden einfach an die instanz-Variable, da muss man nichts beachten, weil sie lokal nicht überschrieben sein können
        if (operationInformation.isFinal()) {
          cb.add("public final ");
          operationInformation.getOperation().createMethodSignature(cb, false, importedClassesFullyQualified,
                                                                    operationInformation.getOperation().getNameWithoutVersion());
          cb.add(" {").addLB();
          if (operationInformation.getOperation().getOutputVars() != null && operationInformation.getOperation().getOutputVars().size() > 0) {
            cb.add("return ");
          }

          cb.add("((", dom.getSimpleClassName(), ") instanceVar).");
          operationInformation.getOperation().generateJavaForInvocation(cb, operationInformation.getOperation().getName());
          cb.addLB();
          cb.addLine("}").addLB();
        } else if (!operationInformation.isAbstract()) {
          //weder abstrakt noch final 
          //-> methode muss über spezielle delegation den super-aufruf machen, damit evtl eine workflow implementierung aufgerufen werden kann
          String methodReferenceVarName = "method_" + operationInformation.getOperation().getNameWithoutVersion() + "_InternalSuperCallProxy";
          cb.addLine("private transient ", Method.class.getName(), " ", methodReferenceVarName).addLB();

          cb.addLine("/**");
          cb.addLine(" * @throws ", XynaExceptionResultingFromWorkflowCall.class.getName(),
                     " if method is implemented as workflow which throws XynaException not defined in interface");
          cb.addLine(" */");
          cb.add("public ");
          operationInformation.getOperation().createMethodSignature(cb, false, importedClassesFullyQualified,
                                                                    operationInformation.getOperation().getNameWithoutVersion());
          cb.add(" {").addLB();
          //reflection, weil methode im datentyp private ist (sichtbar wäre blöd, weil verwirrend für den benutzer). 
          //Außerdem ist eine höhere Sichtbarkeit schlecht, weil die Methoden dann nicht gezielt aufgerufen werden können (wegen dynamic dispatch)
          cb.addLine("if (", methodReferenceVarName, " == null) {");
          cb.addLine(Method.class.getName(), " temp;");
          cb.addLine("try {");
          cb.add("temp = ", dom.getSimpleClassName(), ".class.getDeclaredMethod(\"", operationInformation.getOperation().getName(),
                 Operation.METHOD_INTERNAL_SUPERCALL_PROXY, "\", ");
          //erster Parameter oberklassen typ
          cb.addListElement(dom.getSuperClassGenerationObject().getSimpleClassName() + ".class");

          //TODO mehr unterstützung von special purpose inputvars, falls man die mit dieser technologie erweitern möchte
          //     vgl auch unten die parameter übergabe ans invoke
          //spezial parameter:
          if (operationInformation.getOperation() instanceof JavaOperation) {
            JavaOperation jo = (JavaOperation) operationInformation.getOperation();
            if (jo.requiresXynaOrder()) {
              cb.addListElement(XynaOrderServerExtension.class.getSimpleName() + ".class");
            }
          }

          for (AVariable inputVar : operationInformation.getOperation().getInputVars()) {
            if (inputVar.isList()) {
              cb.addListElement(List.class.getSimpleName() + ".class");
            } else {
              cb.addListElement(inputVar.getEventuallyQualifiedClassNameNoGenerics(importedClassesFullyQualified) + ".class");
            }
          }
          cb.add(")").addLB();
          //TODO mehr informationen bei den runtimeexceptions übergeben
          cb.addLine("} catch (", SecurityException.class.getSimpleName(), " e) {");
          cb.addLine("throw new ", RuntimeException.class.getSimpleName(), "(e)");
          cb.addLine("} catch (", NoSuchMethodException.class.getSimpleName(), " e) {");
          cb.addLine("throw new ", RuntimeException.class.getSimpleName(), "(e)");
          cb.addLine("}");
          cb.addLine("temp.setAccessible(true)");
          cb.addLine(methodReferenceVarName, " = temp");
          cb.addLine("}"); //end if

          cb.addLine("try {");
          if (operationInformation.getOperation().getOutputVars() != null && operationInformation.getOperation().getOutputVars().size() > 0) {
            cb.add("return (", operationInformation.getOperation().getOutputParameterOfMethodSignature(importedClassesFullyQualified), ") ");
          }
          cb.add(methodReferenceVarName, ".invoke(instanceVar, ");
          //parameterobjekte
          cb.addListElement(SUPER_PROXY_INTERNALSUPERCALL_DELEGATION_VARNAME);
          if (operationInformation.getOperation() instanceof JavaOperation) {
            JavaOperation jo = (JavaOperation) operationInformation.getOperation();
            if (jo.requiresXynaOrder()) {
              cb.addListElement(JavaOperation.CORRELATED_XYNA_ORDER_VAR_NAME);
            }
          }
          for (AVariable inputVar : operationInformation.getOperation().getInputVars()) {
            cb.addListElement(inputVar.getVarName());
          }
          cb.add(")").addLB();

          cb.addLine("} catch (", IllegalArgumentException.class.getSimpleName(), " e) {");
          cb.addLine("throw new ", RuntimeException.class.getSimpleName(), "(e)");
          cb.addLine("} catch (", IllegalAccessException.class.getSimpleName(), " e) {");
          cb.addLine("throw new ", RuntimeException.class.getSimpleName(), "(e)");
          cb.addLine("} catch (", InvocationTargetException.class.getName(), " e) {");
          List<ExceptionVariable> sortedExceptions =
              new ArrayList<ExceptionVariable>(operationInformation.getOperation().getThrownExceptions());
          Collections.sort(sortedExceptions, new ExceptionHierarchyComparator());
          cb.addLine("Throwable targetEx = e.getTargetException()");
          for (ExceptionVariable exception : sortedExceptions) {
            cb.addLine("if (targetEx instanceof ", exception.getFQClassName(), ") {");
            cb.addLine("throw (", exception.getFQClassName(), ") targetEx");
            cb.addLine("}");
          }
          cb.addLine("if (targetEx instanceof ", RuntimeException.class.getSimpleName(), ") {");
          cb.addLine("throw (", RuntimeException.class.getSimpleName(), ") targetEx");
          cb.addLine("}");
          cb.addLine("if (targetEx instanceof ", Error.class.getSimpleName(), ") {");
          cb.addLine("throw (", Error.class.getSimpleName(), ") targetEx");
          cb.addLine("}");
          cb.addLine("throw new RuntimeException(e)");

          cb.addLine("}"); //end try-catchblock

          cb.addLine("}").addLB(); //end methode
        }

      }
    }

    cb.addLine("}");
    return cb.toString();
  }


  private void returnNothing(CodeBuffer cb, List<AVariable> outputVars) {
    if (outputVars == null || outputVars.size() == 0 ) {
      return; //keine Rückgabe
    }
    if( outputVars.size() > 1 ) {
      cb.addLine("return null");  //Container
      return;
    }
    AVariable out = outputVars.get(0);
    if( out.isJavaBaseType() && ! out.getJavaTypeEnum().isObject() ) {
      //kein Object, daher kein null möglich
      cb.addLine("return "+out.getJavaTypeEnum().getDefaultConstructor());
    } else {
      //Object, daher null erlaubt
      cb.addLine("return null");
    }
  }


  public String createInterfaceStaticCode() {
    return createInterfaceCodeInternally(true);
  }


  public String createInterfaceNonStaticCode() {
    return createInterfaceCodeInternally(false);
  }


  private String createInterfaceCodeInternally(boolean staticOps) {
    if (staticOps) {//static
      if (!hasStaticOperations(true)) {
        return null;
      }
    } else if (!hasNonStaticOperations(true)) { //!static
      return null;
    }

    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    cb.addLine("package ", getPackage()).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    cb.addLB(2);
    cb.addLine("/**");
    //FIXME kommentare müssen als ein ganzer string zum codebuffer geaddet werden, damit keine semikolons generiert werden
    cb.addLine(" * " + GenerationBase.escapeForCodeGenUsageInComment(dom.getDocumentation()));
    cb.addLine(" */");
    cb.addLine("public interface ", staticOps ? getInterfaceStaticSimpleClassName() : getInterfaceNonStaticSimpleClassName(), " {");
    for (OperationInformation operationInformation : getOperations(true)) {
      if (staticOps && !operationInformation.getOperation().isStatic()) {
        continue;
      }
      if (!staticOps && operationInformation.getOperation().isStatic()) {
        continue;
      }
      if (!staticOps && operationInformation.isFinalInSuperType()) {
        //hier nicht überschreibbar
        continue;
      }
      if (!operationInformation.isImplementedHere()) {
        continue;
      }
      Operation operation = operationInformation.getOperation();
      if (!isSameVersion(operation.getVersion(), version)) {
        continue;
      }
      //auch abstrakte und als workflow modellierte operations hier anzeigen und dokumentieren

      String documentation = getDocumentation(operation);
      cb.addLine("/**");
      //FIXME kommentare müssen als ein ganzer string zum codebuffer geaddet werden, damit keine semikolons generiert werden
      cb.addLine(" * " + GenerationBase.escapeForCodeGenUsageInComment(documentation) + "<p>");
      cb.addLine(" * Defined in {@link " + operationInformation.getDefiningType().getFqClassName() + "}<br>");
      if (operation.isFinal()) {
        cb.addLine(" Final");
      }
      if (operation.isAbstract()) {
        cb.addLine(" * Abstract");
      } else {
        String implJavaDoc = " * Implemented in {@link " + operationInformation.getImplementingType().getFqClassName() + "} ";
        if (operation instanceof WorkflowCall) {
          implJavaDoc += "as Workflow";
        } else if (operation.implementedInJavaLib()) {
          implJavaDoc += "in JavaLibrary";
        } else {
          implJavaDoc += "as Codesnippet.";
        }
        cb.addLine(implJavaDoc);
      }
      if (operation.isStepEventListener()) {
        cb.addLine(" * Method marked as abortable. Implement support for abort as:");
        cb.addLine(" * <pre>");
        cb.addLine(" * ServiceStepEventSource eventSource = ServiceStepEventHandling.getEventSource();");
        cb.addLine(" * // register handler for abort event");
        cb.addLine(" * eventSource.listenOnAbortEvents(myServiceStepEventHandler);");
        cb.addLine(" * </pre>");
      }
      //TODO javadoc für die variablen
      cb.addLine(" */");
      cb.add("public ");
      operation.createMethodSignature(cb, false, importedClassesFullyQualified, operation.getNameWithoutVersion());
      cb.addLB(2);
    }
    cb.addLine("}");
    return cb.toString();
  }


  /**
   * falls lokal keine dokumentation definiert ist, die aus oberklasse übernehmen
   */
  private String getDocumentation(Operation operation) {
    String documentation = operation.getDocumentation();
    DOM currentDOM = dom;
    while (documentation == null || documentation.trim().length() == 0) {
      //in oberklasse suchen
      currentDOM = currentDOM.getSuperClassGenerationObject();
      if (currentDOM == null) {
        break;
      }
      for (Operation op : currentDOM.getOperations()) {
        if (op.getName().equals(operation.getName())) {
          documentation = op.getDocumentation();
          break;
        }
      }
    }
    return documentation;
  }


  /**
   * erstellt temporären superklassen code falls vorhanden, um nicht abhängig von kram im deployten superklassen code zu sein
   * (z.b. abstract methoden)
   * 
   * code ist anders aufgebaut, als der echte superklassen code, er enthält z.b. keine informationen über weitere superklassen.
   * benötigt werden nur konstruktor und korrekte methodensignaturen
   */
  public String createSuperProjectNonStaticImplCode() {
    if (!dom.hasSuperTypeWithInstanceMethods(null)) {
      return null;
    }

    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    String pkg = GenerationBase.getPackageNameFromFQName(getSuperProjectNonStaticImplFQClassName(version));
    cb.addLine("package ", pkg).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);

    if (!pkg.equals(GenerationBase.getPackageNameFromFQName(dom.getSuperClassGenerationObject().getFqClassName()))) {
      cb.addLine("import ", dom.getSuperClassGenerationObject().getFqClassName());
    }

    cb.addLB(2).add("public ");
    if (dom.getSuperClassGenerationObject().isAbstract()) {
      cb.add("abstract ");
    }
    cb.add("class ", getSuperProjectNonStaticImplSimpleClassName(version));
    cb.add(" {").addLB(2);
    
    cb.addLine("protected ", dom.getSuperClassGenerationObject().getSimpleClassName(), " instanceVar").addLB();

    cb.addLine("public ", getSuperProjectNonStaticImplSimpleClassName(version), "(", dom.getSuperClassGenerationObject()
        .getSimpleClassName(), " instanceVar) {");
    cb.addLine("}");


    for (OperationInformation operationInformation : dom.getSuperClassGenerationObject().collectOperationsOfDOMHierarchy(false)) {
      Operation operation = operationInformation.getOperation();
      if (operationInformation.isFinalInSuperType()) {
        continue;
      }
      if (!isSameVersion(operation.getVersion(), version)) {
        continue;
      }
      cb.add("public ");
      if (operation.isAbstract()) {
        cb.add("abstract ");
      }
      operation.createMethodSignature(cb, false, importedClassesFullyQualified, operation.getNameWithoutVersion());
      if (operation.isAbstract()) {
        cb.addLB(2);
        continue;
      }
      cb.add(" {").addLB();
      if (operation.getOutputVars() != null && operation.getOutputVars().size() > 0) {
        cb.addLine("return null");
      }
      cb.addLine("}").addLB();
    }

    cb.addLine("}");

    return cb.toString();
  }


  /**
   * erstellt temporären superklassen code falls vorhanden, um nicht abhängig von kram im deployten superklassen code zu sein
   * (z.b. abstract methoden)
   * 
   * code ist anders aufgebaut, als der echte superklassen code, er enthält z.b. keine informationen über weitere superklassen.
   * benötigt werden nur konstruktoren
   */
  public String createSuperDelegationImplCode() {
    if (!dom.hasSuperTypeWithInstanceMethods(null)) {
      return null;
    }

    HashSet<String> implDontImport = new HashSet<String>();
    HashSet<String> importedClasses = new HashSet<String>();
    HashSet<String> importedClassesFullyQualified = new HashSet<String>();
    HashSet<String> fullQualificationRequiringClasses = new HashSet<String>();

    CodeBuffer cb = new CodeBuffer("Development");

    cb.addLine("package " + GenerationBase.getPackageNameFromFQName(getSuperDelegationImplFQClassName())).addLB();
    imports(cb, implDontImport, importedClasses, importedClassesFullyQualified, fullQualificationRequiringClasses);
    if (!importedClassesFullyQualified.contains(HashMap.class.getName())) {
      cb.addLine("import ", HashMap.class.getName());
    }
    if (!importedClassesFullyQualified.contains(Map.class.getName())) {
      cb.addLine("import ", Map.class.getName());
    }
    
    InterfaceVersion[] versions = dom.getVersionsOfOperations(true);

    cb.addLB(2).add("public ");
    if (dom.getSuperClassGenerationObject().isAbstract()) {
      cb.add("abstract ");
    }
    cb.add("class ", getSuperDelegationImplSimpleClassName());
    cb.add(" {").addLB(2);

    for (InterfaceVersion version : versions) {
      //evtl zuviele, macht aber nichts
      String v = getVarNameNonstaticOperationImplInstance(version);
      cb.addLine("protected final ", getSuperProjectNonStaticImplFQClassName(version), " ", v).addLB();
    }

    //konstruktor 1
    cb.addLine("public ", getSuperDelegationImplSimpleClassName(), "(", dom.getSuperClassGenerationObject().getFqClassName(), " o) {");
    for (InterfaceVersion version : versions) {
      String v = getVarNameNonstaticOperationImplInstance(version);
      cb.addLine(v, "= null");
    }
    cb.addLine("}").addLB();

    //konstruktor 2
    cb.addLine("public ", getSuperDelegationImplSimpleClassName(), "(", dom.getSuperClassGenerationObject().getFqClassName(),
               " o, Map<String, Object> implMap) {");
    for (InterfaceVersion version : versions) {
      String v = getVarNameNonstaticOperationImplInstance(version);
      cb.addLine(v, "= null");
    }
    cb.addLine("}").addLB();

    cb.addLine("public ", getSuperDelegationImplSimpleClassName(), "() {");
    for (InterfaceVersion version : versions) {
      String v = getVarNameNonstaticOperationImplInstance(version);
      cb.addLine(v, "= null");
    }
    cb.addLine("}").addLB();
    //methoden werden nicht benötigt, weil es keine super-aufrufe in der delegationimpl klasse gibt
    cb.addLine("}");

    return cb.toString();
  }

  public static void setInstanceVarInImpl(XynaObject newInstanceVar, Object implOfInstanceMethodsDelegationLayer) {
    Method m = getMethodForGettingNonStaticProjectImpl(implOfInstanceMethodsDelegationLayer);
    if (m != null) {
      Object impl;
      try {
        impl = m.invoke(implOfInstanceMethodsDelegationLayer);
        Field instanceVarField = getInstanceVarField(impl);
        if (instanceVarField != null) {
          instanceVarField.set(impl, newInstanceVar);
        }
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        logger.debug("Could not substitute instanceVar in project impl of " + newInstanceVar.getClass().getName(), e);
      }
    }
  }

  private static Method getMethodForGettingNonStaticProjectImpl(Object implOfInstanceMethodsDelegationLayer) {
    String getInstanceVarInDelegationLayer = GenerationBase.buildGetter(NONSTATIC_OPERATION_IMPL_INSTANCE);
    try {
      return implOfInstanceMethodsDelegationLayer.getClass().getMethod(getInstanceVarInDelegationLayer);
    } catch (NoSuchMethodException | SecurityException e) {
      return null;
    }
  }


  private static Field getInstanceVarField(Object impl) {
    Class<?> clazz = impl.getClass();
    while (clazz != null) {
      try {
        for (Field f : clazz.getDeclaredFields()) {
          if (f.getName().equals("instanceVar")) {
            f.setAccessible(true);
            return f;
          }
        }
      } catch (SecurityException e) {
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }


  private String getVarNameNonstaticOperationImplInstance(InterfaceVersion version) {
    if (version.isCurrentVersion()) {
      return NONSTATIC_OPERATION_IMPL_INSTANCE;
    } else {
      return NONSTATIC_OPERATION_IMPL_INSTANCE + version.getSuffix();
    }
  }


  private String getVarNameStaticOperationImplInstance(InterfaceVersion version) {
    if (version.isCurrentVersion()) {
      return STATIC_OPERATION_IMPL_INSTANCE;
    } else {
      return STATIC_OPERATION_IMPL_INSTANCE + version.getSuffix();
    }
  }


  private String getInterfaceStaticSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getInterfaceStaticFQClassName());
  }


  private String getInterfaceNonStaticSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getInterfaceNonStaticFQClassName());
  }


  private String getSuperProxySimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getSuperProxyFQClassName());
  }
  
  
  private String getSuperProxySimpleClassName(InterfaceVersion version) {
    return GenerationBase.getSimpleNameFromFQName(getSuperProxyFQClassName(version));
  }


  private String getProjectNonStaticImplSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getProjectNonStaticImplFQClassName(version));
  }


  private String getProjectStaticImplSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getProjectStaticImplFQClassName(version));
  }


  private String getDelegationImplSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getDelegationImplFQClassName());
  }


  private String getSuperDelegationImplSimpleClassName() {
    return GenerationBase.getSimpleNameFromFQName(getSuperDelegationImplFQClassName());
  }


  private String getSuperProjectNonStaticImplSimpleClassName(InterfaceVersion version) {
    return GenerationBase.getSimpleNameFromFQName(getSuperProjectNonStaticImplFQClassName(version));
  }

  
  private String getSuperProxyFQClassName(InterfaceVersion version) {
    if (version.isCurrentVersion()) {
      return dom.getFqClassName() + SUPER_PROXY_NAME_SUFFIX;
    } else {
      return dom.getFqClassName() + SUPER_PROXY_NAME_SUFFIX + version.getSuffix();
    }
  }
  
  
  public String getSuperProxyFQClassName() {
    return getSuperProxyFQClassName(version);
  }


  public String getInterfaceStaticFQClassName() {
    if (version.isCurrentVersion()) {
      return dom.getFqClassName() + STATIC_OPERATION_INTERFACE_SUFFIX;
    } else {
      return dom.getFqClassName() + STATIC_OPERATION_INTERFACE_SUFFIX + version.getSuffix();
    }
  }


  public String getInterfaceNonStaticFQClassName() {
    if (version.isCurrentVersion()) {
      return dom.getFqClassName() + NONSTATIC_OPERATION_INTERFACE_SUFFIX;
    } else {
      return dom.getFqClassName() + NONSTATIC_OPERATION_INTERFACE_SUFFIX + version.getSuffix();
    }
  }


  public String getDelegationImplFQClassName() {
    return getDelegationImplFQClassName(dom);
  }


  private static String getDelegationImplFQClassName(DOM dom) {
    return dom.getFqClassName() + "Impl";
  }


  //subpackage, damit man so ohne weiteres zugriff auf die protected methoden des datentyps hat
  public String getProjectStaticImplFQClassName(InterfaceVersion version) {
    if (version.isCurrentVersion()) {
      return GenerationBase.getPackageNameFromFQName(dom.getFqClassName()) + ".impl." + dom.getSimpleClassName()
          + STATIC_OPERATION_IMPL_SUFFIX;
    } else {
      return GenerationBase.getPackageNameFromFQName(dom.getFqClassName()) + ".impl." + version.getPackageName() + "."
          + dom.getSimpleClassName() + STATIC_OPERATION_IMPL_SUFFIX;
    }
  }


  private static String getProjectNonStaticImplFQClassName(DOM dom, InterfaceVersion version) {
    if (version.isCurrentVersion()) {
      return GenerationBase.getPackageNameFromFQName(dom.getFqClassName()) + ".impl." + dom.getSimpleClassName()
          + NONSTATIC_OPERATION_IMPL_SUFFIX;
    } else {
      return GenerationBase.getPackageNameFromFQName(dom.getFqClassName()) + ".impl." + version.getPackageName() + "."
          + dom.getSimpleClassName() + NONSTATIC_OPERATION_IMPL_SUFFIX;
    }
  }


  public String getProjectNonStaticImplFQClassName(InterfaceVersion version) {
    return getProjectNonStaticImplFQClassName(dom, version);
  }


  public String getSuperProjectNonStaticImplFQClassName(InterfaceVersion version) {
    if (dom.getSuperClassGenerationObject() != null) {
      return getProjectNonStaticImplFQClassName(dom.getSuperClassGenerationObject(), version);
    }
    return null;
  }


  public String getSuperDelegationImplFQClassName() {
    if (dom.getSuperClassGenerationObject() != null) {
      return getDelegationImplFQClassName(dom.getSuperClassGenerationObject());
    }
    return null;
  }

}
