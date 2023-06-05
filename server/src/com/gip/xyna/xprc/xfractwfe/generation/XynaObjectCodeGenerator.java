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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ListWithConstantSize;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.UpdateList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.serviceimpl.JavaServiceImplementation;


public class XynaObjectCodeGenerator {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaObjectCodeGenerator.class);

  private DOM dom;
  private List<AVariable> memberVars;
  private boolean isAbstract;
  private Set<String> importedClassNames = new HashSet<String>();
  private Set<ExceptionVariable> transientExceptionVariables = new HashSet<ExceptionVariable>();
  
  private static final String _METHODNAME_CREATE_PATH_ORIG = "createPath";
  private static final String METHODNAME_CREATE_PATH;
  private static final String _METHODNAME_GET_PATH_ORIG = "getPath";
  private static final String METHODNAME_GET_PATH;
  private static final String _METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH_ORIG = "getDataModelInfoForPath";
  private static final String METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH;
  private static final String _METHODNAME_GET_OBJECT_ORIG = "getObject";
  static final String METHODNAME_GET_OBJECT;
  private static final String _METHODNAME_GET_THROWABLE_ORIG = "getThrowable";
  static final String METHODNAME_GET_THROWABLE;
  private static final String _METHODNAME_GET_REVISION_ORIG = "getRevision";
  static final String METHODNAME_GET_REVISION;
  private static final String _METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM_ORIG = "getDeployedJarFilePathInMDM";
  private static final String METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM;
  private static final String _METHODNAME_DATA_MODEL_INFORMATION_GET_ORIG = "get";
  private static final String METHODNAME_DATA_MODEL_INFORMATION_GET;
  
  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_CREATE_PATH = Path.class.getDeclaredMethod(_METHODNAME_CREATE_PATH_ORIG, String.class, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CREATE_PATH_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_PATH = Path.class.getDeclaredMethod(_METHODNAME_GET_PATH_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PATH_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH = Path.class.getDeclaredMethod(_METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH_ORIG, String.class, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_OBJECT = SerializableClassloadedObject.class.getDeclaredMethod(_METHODNAME_GET_OBJECT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_OBJECT_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_THROWABLE = SerializableClassloadedException.class.getDeclaredMethod(_METHODNAME_GET_THROWABLE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_THROWABLE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_REVISION = ClassLoaderBase.class.getDeclaredMethod(_METHODNAME_GET_REVISION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_REVISION_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM = DOM.class.getDeclaredMethod(_METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM_ORIG, String.class, Long.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM_ORIG + " not found", e);
    }
    try {
      METHODNAME_DATA_MODEL_INFORMATION_GET = DataModelInformation.class.getDeclaredMethod(_METHODNAME_DATA_MODEL_INFORMATION_GET_ORIG, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_DATA_MODEL_INFORMATION_GET_ORIG + " not found", e);
    }
  }
  
  public XynaObjectCodeGenerator(DOM dom) {
    this.dom = dom;
    this.memberVars = dom.getMemberVars();
    this.isAbstract = dom.isAbstract();
  }

  public void generateJavaInternally(CodeBuffer cb) {
    generateJavaImports(cb);
    generateJavaClassHeader(cb);
    generateJavaClassMembers(cb);
    generateJavaClassBuilder(cb);
    generateJavaConstructors(cb);
    if( XynaProperty.XYNAOBJECT_HAS_GENERATED_TOSTRING.get() ) {
      generateJavaToString(cb);
    }
    generateJavaClone(cb);
    generateObjectVersionClass(cb);
    generateVersionChangeCollection(cb);
    generateJavaToXML(cb);
    generateJavaGetVariableNames(cb);
    generateJavaGeneralGetter(cb);
    generateJavaGeneralSetter(cb);
    generateJavaFieldCache(cb);
    generateJavaMethods(cb);
  }

  public void generateJavaStub(CodeBuffer cb) {
    Set<String> libs = new HashSet<String>(dom.getAdditionalLibraries());
    dom.getAdditionalLibraries().clear();
    List<Operation> operations = dom.getOperations();
    for (Operation operation : operations) {
      if (operation instanceof JavaOperation) {
        ((JavaOperation) operation).setActive(false);
      }
    }
    generateJavaInternally(cb);
    dom.getAdditionalLibraries().addAll(libs);
    for (Operation operation : operations) {
      if (operation instanceof JavaOperation) {
        ((JavaOperation) operation).setActive(true);
      }
    }
  }
  
  public void generateJavaImports(CodeBuffer cb) {
    // this set is only required for the following import creation. the imported class names set is required below.
    Set<String> importedSimpleClasseNames = new HashSet<String>();

    // bugz9525: the datatype itself is considered "imported"
    importedSimpleClasseNames.add(dom.getSimpleClassName());

    if (!isEmpty(dom.getOriginalPath())) {
      cb.addLine("package " + GenerationBase.getPackageNameFromFQName(dom.getFqClassName())).addLB();
    } else {
      cb.addLine("package " + GenerationBase.DEFAULT_PACKAGE).addLB();
    }

    boolean previousContainedJavaPrefix = false;
    boolean previousContainedXynaPrefix = false;

    for (String i : dom.getImports()) {

      if (i.contains(".")) {
        String currentSimpleClassName = i.substring(i.lastIndexOf(".") + 1);
        if (!importedSimpleClasseNames.contains(currentSimpleClassName)) {

          if (i.startsWith("java.")) {
            if (!previousContainedJavaPrefix) {
              cb.addLB();
            }
            previousContainedJavaPrefix = true;
            previousContainedXynaPrefix = false;
          } else if (i.startsWith("com.gip.xyna.")) {
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

          cb.addLine("import ", i);
          importedSimpleClasseNames.add(currentSimpleClassName);
          importedClassNames.add(i);
        }

      } else {
        cb.addLine("import ", GenerationBase.DEFAULT_PACKAGE + "." + i);
      }
    }
  }
  
  private void generateJavaClassHeader(CodeBuffer cb) {
    cb.addLB().add("@XynaObjectAnnotation(fqXmlName = \"", dom.getOriginalPath(), ".", dom.getOriginalSimpleName(), "\")\n");
    cb.add("public ");
    if (isAbstract) {
      cb.add("abstract ");
    }
    cb.add("class ", dom.getSimpleClassName(), " extends ");
    if (hasSuperClass()) {
      cb.add(dom.getSuperClassGenerationObject().getClassName(importedClassNames));
    } else {
      cb.add(XynaObject.class.getSimpleName());
    }
    cb.add(" ");
    cb.add("{").addLB(2);

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(dom.calculateSerialVersionUID()), "L");
    cb.addLine("private static final ", Logger.class.getSimpleName(), " logger = ",
               CentralFactoryLogging.class.getSimpleName(), ".getLogger(", dom.getSimpleClassName(), ".class)").addLB(2);

  }
  
  public static String getVarNameForOldVersions(AVariable v) {
    return "oldVersionsOf" + v.getVarName();
  }

  private static void generateLazyInitVersionedObject(CodeBuffer cb, AVariable v, Set<String> importedClassNames) {
    cb.addLine(VersionedObject.class.getCanonicalName(), "<",
               v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClassNames), "> _vo = lazyInit_", getVarNameForOldVersions(v), "()");
  }

  private void generateJavaClassMembers(CodeBuffer cb) {
    if (dom.getPathMapInformation() != null) {
      generateJavaPathMapMembers(cb);
    }
    // membervars
    for (AVariable v : memberVars) {
      if (v instanceof DatatypeVariable &&
          ((DatatypeVariable)v).getRestrictions() != null) {
        ((DatatypeVariable)v).getRestrictions().generateJava(cb);
      }

      cb.add("@LabelAnnotation(label=").addString(v.getLabel()).add(")" + System.getProperty("line.separator"));

      // definition
      cb.add("  private ");
      if (v instanceof ExceptionVariable) {
        cb.add("transient ");
        transientExceptionVariables.add((ExceptionVariable) v);
      }
      cb.add(v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, false), " ", v.getVarName()).addLB(2);

      //versioning
      appendVersionedObjectMemberVar(cb, v, importedClassNames);

      // getter
      appendGetter(cb, v, importedClassNames, true);

      // versionedGetter
      appendVersionedGetter(cb, v, importedClassNames);

      // setter
      appendSetter(cb, v, importedClassNames);

      //unversionedSetter: vom Builder aufzurufen, damit dort nicht bereits versionen angelegt werden!
      appendUnversionedSetter(cb, importedClassNames, v);

      if (v.isList()) {
        cb.addLine("public void addTo", GenerationBase.buildGetter(v.getVarName()).substring(3), "(",
                   v.getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " e) {");
        //dieses feature wird in projekten genutzt. bei änderungen also bitte aufpassen

        cb.addLine("if (supportsObjectVersioning()) {");
        generateLazyInitVersionedObject(cb, v, importedClassNames);
        cb.addLine("synchronized (_vo) {");
        cb.addLine(v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, false), "__tmp = this.", v.getVarName());
        cb.addLine("if (__tmp == null) {");
        cb.addLine("__tmp = this.", v.getVarName(), " = new ", ArrayList.class.getSimpleName(), "<",
                   v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">()");
        cb.addLine("_vo.add(null)");
        cb.addLine("} else {");
        cb.addLine("_vo.add(new ", ListWithConstantSize.class.getCanonicalName(), "(__tmp))");
        cb.addLine("}");
        cb.addLine("__tmp.add(e)");
        cb.addLine("}"); //end synchronized

        cb.addLine("return");
        cb.addLine("}"); //endif useVersioning
        cb.addLine("if (this.", v.getVarName(), " == null) {");
        cb.addLine("this.", v.getVarName(), " = new ", ArrayList.class.getSimpleName(), "<",
                   v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">()");
        cb.addLine("}");
        cb.addLine("this.", v.getVarName(), ".add(e)");
        cb.addLine("}").addLB();


        cb.addLine("public void removeFrom", GenerationBase.buildGetter(v.getVarName()).substring(3), "(",
                   v.getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " e) {");
        cb.addLine("if (this.", v.getVarName(), " == null) {");
        cb.addLine("return");
        cb.addLine("}");
        cb.addLine("if (supportsObjectVersioning()) {");
        generateLazyInitVersionedObject(cb, v, importedClassNames);
        cb.addLine("synchronized (_vo) {");
        cb.addLine(v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, false), "__tmp = this.", v.getVarName());
        cb.addLine("_vo.add(__tmp)");
        cb.addLine("this.", v.getVarName(), " = new ", ArrayList.class.getSimpleName(), "<",
                   v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">(__tmp)");
        cb.addLine("this.", v.getVarName(), ".remove(e)");
        cb.addLine("}"); //end synchronized
        cb.addLine("return");
        cb.addLine("}"); //endif useVersioning

        cb.addLine("this.", v.getVarName(), ".remove(e)");
        cb.addLine("}");
        cb.addLB();

      }
    }

    appendUseVersioningMethod(cb);
  }

  public static void appendGetter(CodeBuffer cb, AVariable v, Set<String> importedClassNames, boolean useListExtendedGenerics) {
    cb.addLine("public ", v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, useListExtendedGenerics), " ", GenerationBase.buildGetter(v.getVarName()), "() {");
    if (v.isList()) {
      //wenn listen herausgegeben werden, können diese geändert werden und müssen dann von der versionierung beachtet werden -> also wrappen
      cb.addLine("if (supportsObjectVersioning()) {");
      cb.addLine("if (", v.getVarName(), " == null) {");
      cb.addLine("return null");
      cb.addLine("}"); //v==null
      String genericParameter = "<" + v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames) + ">";        
      cb.addLine("return new ", VersionedList.class.getCanonicalName(), genericParameter, "(", v.getVarName(), ", new ", UpdateList.class.getCanonicalName(), genericParameter, "() {");
      cb.addLB();
      cb.addLine("private static final long serialVersionUID = 1L");
      cb.addLB();
      cb.addLine("public void update(", List.class.getSimpleName(), genericParameter, " _newList) {");
      cb.addLine(v.getVarName(), " = _newList");
      cb.addLine("}").addLB();
      cb.addLine("public ", VersionedObject.class.getCanonicalName(), "<List", genericParameter, "> getOldVersions() {");
      generateLazyInitVersionedObject(cb, v, importedClassNames);
      cb.addLine("return _vo");
      cb.addLine("}").addLB();
      cb.addLine("});"); //return
      cb.addLine("}"); //useVersioning
    }
    cb.addLine("return ", v.getVarName()).addLine("}");
    cb.addLB();

  }
  

  public static void appendUseVersioningMethod(CodeBuffer cb) {
    cb.addLine("public boolean supportsObjectVersioning() {");
    switch (XynaProperty.useVersioningConfig.get()) {
      case 5 :
      case 4 :
      case 3 :
        //ausserhalb der factory kein versioning verwenden, weil keine versionsnummern über idgenerator gefunden werden können.
        cb.addLine("if (!", XynaFactory.class.getName(), ".isFactoryServer()) {");
        cb.addLine("return false");
        cb.addLine("}");
      default :
        break;
    }
    switch (XynaProperty.useVersioningConfig.get()) {
      case 5 :
        cb.addLine("return true");
        break;
      case 4 :
      case 3 :
        cb.addLine("if (", XynaProperty.class.getName(), ".useVersioningConfig.get() == 4) {");
        cb.addLine("return true");
        cb.addLine("} else {");
        cb.addLine("return false");
        cb.addLine("}");
        break;
      default :
        cb.addLine("return false");
        break;
    }
    cb.addLine("}").addLB();
  }

  public static void appendVersionedObjectMemberVar(CodeBuffer cb, AVariable v, Set<String> importedClassNames) {
    cb.add("private volatile ");
    if (v instanceof ExceptionVariable) {
      cb.add("transient ");
    }
    cb.add(XOUtils.VersionedObject.class.getCanonicalName(), "<",
               v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClassNames), "> ", getVarNameForOldVersions(v));
    cb.addLB(2);
    
    cb.addLine("private ", VersionedObject.class.getCanonicalName(), "<",
               v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClassNames), "> lazyInit_", getVarNameForOldVersions(v), "() {");
    cb.addLine(VersionedObject.class.getCanonicalName(), "<",
               v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClassNames), "> _vo = ", getVarNameForOldVersions(v));
    cb.addLine("if (_vo == null) {");
    cb.addLine("synchronized (this) {");
    cb.addLine("_vo = ", getVarNameForOldVersions(v));
    cb.addLine("if (_vo == null) {");
    cb.addLine(getVarNameForOldVersions(v), " = _vo = new ", VersionedObject.class.getCanonicalName(), "<",
               v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClassNames), ">()");
    cb.addLine("}"); //endif
    cb.addLine("}"); //end synchronized
    cb.addLine("}"); //endif vo == null
    cb.addLine("return _vo");
    cb.addLine("}");
    cb.addLB(2);
  }


  public static void appendSetter(CodeBuffer cb, AVariable v, Set<String> importedClassNames) {
    cb.addLine("public void ", GenerationBase.buildSetter(v.getVarName()), "(",
               v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, false), " ", v.getVarName(), ") {");
    if (v.isList()) {
      cb.addLine(v.getVarName(), " = ", XOUtils.class.getSimpleName(), ".substituteList(", v.getVarName(), ")");
    }
    cb.addLine("if (supportsObjectVersioning()) {");
    generateLazyInitVersionedObject(cb, v, importedClassNames);
    cb.addLine("synchronized (_vo) {");
    cb.addLine("_vo.add(this.", v.getVarName(), ")");
    cb.addLine("this.", v.getVarName(), " = ", v.getVarName());
    cb.addLine("}"); //end synchronized
    cb.addLine("return");
    cb.addLine("}"); //endif useVersioning
    cb.addLine("this.", v.getVarName(), " = ", v.getVarName());
    cb.addLine("}");
    cb.addLB();
  }


  public static void appendVersionedGetter(CodeBuffer cb, AVariable v, Set<String> importedClasseNames) {
    cb.addLine("public ", v.getEventuallyQualifiedClassNameWithGenerics(importedClasseNames), " versionedG",
               GenerationBase.buildGetter(v.getVarName()).substring(1), "(long _version) {");
    
    cb.addLine("if (", getVarNameForOldVersions(v), " == null) {");
    cb.addLine("return ", v.getVarName());
    cb.addLine("}");
    cb.addLine(v.getEventuallyQualifiedClassNameWithGenerics(importedClasseNames, false), " _local = ", v.getVarName());
    cb.addLine(XOUtils.Version.class.getCanonicalName(), "<", v.getEventuallyQualifiedClassNameToBeUsedInGenerics(importedClasseNames),
               "> _ret = ", getVarNameForOldVersions(v), ".getVersion(_version)");
    cb.addLine("if (_ret == null) {");
    cb.addLine("return _local");
    cb.addLine("}");
    cb.addLine("return _ret.object").addLine("}");
    cb.addLB();
  }


  private void generateJavaPathMapMembers(CodeBuffer cb) {
    if (memberVars.size() > 0) {
      // membervars
      boolean foundValue = generateJavaPathMapMembersMap(cb);

      if (!foundValue) {
        //es gab keine values, also hat man nur pfade!
        if (memberVars.get(0).isList()) {
          //für Listen ein Set erstellen
          generateJavaPathMapMembersSet(cb);
        } else {
          //sonst nur einen einzelnen Pfad
          generateJavaPathMapMembersSinglePath(cb);
        }
      }
    }
  }


  private void createObjectsInPath(CodeBuffer cb, String pathInChild, AVariable aVariable, String parentVarName) {
    String[] parts = pathInChild.split("\\.");
    StringBuilder sb = new StringBuilder();
    if (parts.length > 1) {
      for (int i = 0; i < parts.length - 1; i++) {
        sb.append(parts[i]);
        for (AVariable childVar : aVariable.getDomOrExceptionObject().getMemberVars()) {
          if (childVar.getVarName().equals(parts[i])) {
            aVariable = childVar;
            break;
          }
        }
        cb.addLine("if (", AVariable.getGetter(parentVarName, sb.toString()), " == null) {");
        cb.addLine(AVariable.getSetter(parentVarName, "new " + aVariable.getFQClassName() + "()", sb.toString()));
        cb.addLine("}");
        sb.append(".");
      }
    }
  }

  private boolean generateJavaPathMapMembersMap(CodeBuffer cb) {
    Triple<AVariable, AVariable, String> triple = GenerationBase.traversePathMapHierarchyToFindValue(dom, dom, "");
    
    if (triple == null) {
      return false;
    }
    
    AVariable v = triple.getFirst();
    AVariable rootMemberVar = triple.getSecond();
    String localPath = triple.getThird();

    // definition
    String childType = rootMemberVar.getEventuallyQualifiedClassNameNoGenerics(importedClassNames);
    String generics = "<" + Path.class.getName() + ", " + childType + ">";
    cb.add("private final ", Map.class.getSimpleName(), generics);

    cb.add(" ", v.getVarName(), "Map = new ", HashMap.class.getSimpleName(), generics, "()");
    cb.addLB(2);
    // getFromMap
    cb.addLine("public ", childType, " ", GenerationBase.buildGetter(v.getVarName()),
               "FromMap(String xfl, String fqDataModelName) throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine(Path.class.getName(), " _path = ", Path.class.getName(), ".", METHODNAME_CREATE_PATH, "(xfl, fqDataModelName)");
    cb.addLine("return ", v.getVarName(), "Map.get(_path)");
    cb.addLine("}");
    cb.addLB();

    // setInMap
    cb.addLine("public ", childType, " ", GenerationBase.buildSetter(v.getVarName()), "InMap(String xfl, String fqDataModelName, ",
               v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, false), " ", v.getVarName(), ") throws ",
               XynaException.class.getSimpleName(), " {");
    cb.addLine(Path.class.getName(), " _path = ", Path.class.getName(), ".", METHODNAME_CREATE_PATH, "(xfl, fqDataModelName)");
    cb.addLine(childType, " _child = new ", childType, "()");
    int len = rootMemberVar.getVarName().length() + 1;
    String pathKeyVarName = dom.getPathMapInformation().getPathKey();
    createObjectsInPath(cb, pathKeyVarName.substring(len), rootMemberVar, "_child");
    cb.addLine(AVariable.getSetter("_child", "_path." + METHODNAME_GET_PATH + "()", pathKeyVarName.substring(len)));
    createObjectsInPath(cb, localPath.substring(len), rootMemberVar, "_child");
    cb.addLine(AVariable.getSetter("_child", v.getVarName(), localPath.substring(len)));

    addAutomaticFilledValues(cb, rootMemberVar, v);

    String varNameStartsUpperCase = GenerationBase.buildGetter(rootMemberVar.getVarName()).substring(3);
    cb.addLine("addTo", varNameStartsUpperCase, "(_child)");
    //TODO hier könnte man auch schöner das objekt nicht hinzufügen, wenn es bereits in der map enthalten ist, und dann nur den value umsetzen
    cb.addLine(childType, " _previousValue = ", v.getVarName(), "Map.put(_path, _child)");
    cb.addLine("if (_previousValue != null) {");
    cb.addLine("removeFrom", varNameStartsUpperCase, "(_previousValue)");
    cb.addLine("}");
    cb.addLine("return _child");
    cb.addLine("}");
    cb.addLB();

    return true;
  }
  
  
  private void generateJavaPathMapMembersSet(CodeBuffer cb) {
    // definition
    cb.addLine("private final ", Set.class.getName(), "<", Path.class.getName(), "> _allPaths = new ", HashSet.class.getSimpleName(),
               "<", Path.class.getName(), ">()").addLB();
    
    // getPaths
    cb.addLine("public ", Set.class.getSimpleName(), "<", Path.class.getName(), "> getPaths() {");
    cb.addLine("return _allPaths");
    cb.addLine("}").addLB();

    // addPath
    cb.addLine("public void addPath(String xfl, String fqDataModelName) throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine(Path.class.getName(), " _path = ", Path.class.getName(), ".", METHODNAME_CREATE_PATH, "(xfl, fqDataModelName)");
    cb.addLine("_allPaths.add(_path)");
    
    if (memberVars.get(0).isJavaBaseType) {
      cb.addLine("addTo", GenerationBase.buildGetter(memberVars.get(0).getVarName()).substring(3), "(_path.getPath())");
    } else {
      cb.addLine(memberVars.get(0).getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " _child = new ", memberVars.get(0)
        .getEventuallyQualifiedClassNameNoGenerics(importedClassNames), "()");
      String pathKeyVarName = dom.getPathMapInformation().getPathKey();
      String pathInChild = pathKeyVarName.substring(memberVars.get(0).getVarName().length() + 1);
      createObjectsInPath(cb, pathInChild, memberVars.get(0), "_child");
      cb.addLine(AVariable.getSetter("_child", "_path." + METHODNAME_GET_PATH + "()", pathInChild));
      
      addAutomaticFilledValues(cb, memberVars.get(0), null);
      
      cb.addLine("addTo", GenerationBase.buildGetter(memberVars.get(0).getVarName()).substring(3), "(_child)");
    }
    cb.addLine("}").addLB();
  }
  
  
  private void generateJavaPathMapMembersSinglePath(CodeBuffer cb) {
    // definition
    cb.addLine("private ", Path.class.getName(), " _path").addLB();
    
    // getPath
    cb.addLine("public ", Path.class.getName(), " getPath() {");
    cb.addLine("return _path");
    cb.addLine("}").addLB();

    // setPath
    cb.addLine("public void setPath(String xfl, String fqDataModelName) throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("this._path = ", Path.class.getName(), ".", METHODNAME_CREATE_PATH, "(xfl, fqDataModelName)");
    if (memberVars.get(0).isJavaBaseType) {
      cb.addLine(memberVars.get(0).getVarName(), " = _path.", METHODNAME_GET_PATH, "()");
    } else {
      cb.addLine(memberVars.get(0).getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " _child = new ", memberVars.get(0)
          .getEventuallyQualifiedClassNameNoGenerics(importedClassNames), "()");
      String pathKeyVarName = dom.getPathMapInformation().getPathKey();
      String pathInChild = pathKeyVarName.substring(memberVars.get(0).getVarName().length() + 1);
      createObjectsInPath(cb, pathInChild, memberVars.get(0), "_child");
      cb.addLine(AVariable.getSetter("_child", "_path." + METHODNAME_GET_PATH + "()", pathInChild));

      addAutomaticFilledValues(cb, memberVars.get(0), null);

      cb.addLine(memberVars.get(0).getVarName(), " = _child");
    }
    cb.addLine("}").addLB();
  }

  //usecase: das datenmodell kennt metainformationen, die im pathmap objekt automatisch eingetragen werden sollen, wenn der pfad entsprechend gewählt ist
  //         beispiel: type-information im datenmodell im datenmodell-spezifischen format. z.B. beim SNMPSET will man nicht im mapping selbst den typ angeben
  //         müssen.
  private void addAutomaticFilledValues(CodeBuffer cb, AVariable rootVar, AVariable v) {
    for (AVariable vv :rootVar.getDomOrExceptionObject().getMemberVars()) {
      if (dom.getPathMapInformation() != null) {
        String childPath = rootVar.getVarName() + "." + vv.getVarName();
        if (dom.getPathMapInformation().getInheritFromDataModel(childPath) != null) {
          if (!vv.isJavaBaseType() && !vv.isList()) {
            //TODO mehr als eine automatisch erstellte membervariable unterstützen

            cb.addLine("if (fqDataModelName != null) {");
            cb.addLine(DataModelInformation.class.getSimpleName(), " _dataModel = ", Path.class.getName(),
                       ".", METHODNAME_GET_DATA_MODEL_INFO_FOR_PATH, "(xfl, fqDataModelName)");
            cb.addLine("if (_dataModel != null) {");
            cb.addLine("try {");
            cb.addLine(vv.getEventuallyQualifiedClassNameNoGenerics(importedClassNames), " _autoCreated = (", vv
                .getEventuallyQualifiedClassNameNoGenerics(importedClassNames), ") Class.forName(_dataModel.", METHODNAME_DATA_MODEL_INFORMATION_GET, "(\"", dom
                .getPathMapInformation().getInheritFromDataModel(childPath), "\")).newInstance()");
            cb.addLine("_child.", GenerationBase.buildSetter(vv.getVarName()), "(_autoCreated)");
            cb.addLine("} catch (Exception _ee) {");
            cb.addLine("throw new RuntimeException(_ee)");
            cb.addLine("}");
            cb.addLine("}");
            cb.addLine("}");
          } else {
            throw new RuntimeException("Simple member variables that are to be filled automatically are not supported yet.");
          }
        }
      } else if (v != null && vv.getVarName().equals(v.getVarName())) {
        //in die map auch den value eintragen
        cb.addLine("_child.", GenerationBase.buildSetter(vv.getVarName()), "(", v.getVarName(), ")");
      }
    }
  }


  private static final String BUILDER_GENERIC_DOM_TYPE = "_GEN_DOM_TYPE";
  private static final String BUILDER_GENERIC_BUILDER_TYPE = "_GEN_BUILDER_TYPE";
  

  public static void generateJavaClassBuilder(CodeBuffer cb, BuilderConfig config) {

    //class InternalBuilder
    cb.add("protected static class InternalBuilder<" + BUILDER_GENERIC_DOM_TYPE + " extends ", config.className, ", "
        + BUILDER_GENERIC_BUILDER_TYPE + " extends InternalBuilder<" + BUILDER_GENERIC_DOM_TYPE + ", " + BUILDER_GENERIC_BUILDER_TYPE + ">>");
    if (config.superClassName != null) {
      cb.add(" extends ", config.superClassName, ".InternalBuilder<" + BUILDER_GENERIC_DOM_TYPE
          + ", " + BUILDER_GENERIC_BUILDER_TYPE + ">");
    }
    cb.addLine("{").addLB();
    //Instanz-Variable
    if (config.superClassName == null) {
      cb.addLine("protected " + BUILDER_GENERIC_DOM_TYPE + " instance").addLB();
    }
    //Konstruktor
    cb.addLine("protected InternalBuilder(", config.className, " instance) {");
    if (config.superClassName != null) {
      cb.addLine("super(instance)");
    } else {
      cb.addLine("this.instance = (" + BUILDER_GENERIC_DOM_TYPE + ") instance");
    }
    
    //additional constructor content - after setting instance
    if(config.additionalConstructorContent != null) {
      cb.add(config.additionalConstructorContent);
    }
    
    cb.addLine("}").addLB();
    //Instance-Getter
    cb.addLine("public ", config.className, " instance() {").addLine("return (", config.className, ") instance").addLine("}").addLB();
    //Setter
    for (AVariable v : config.members) {
      cb.addLine("public " + BUILDER_GENERIC_BUILDER_TYPE + " ", v.getVarName(), "(",
                 v.getEventuallyQualifiedClassNameWithGenerics(config.imports, false), " ", v.getVarName(), ") {")
          .addLine("this.instance.unversionedS", GenerationBase.buildSetter(v.getVarName()).substring(1), "(", v.getVarName(), ")")
          .addLine("return (" + BUILDER_GENERIC_BUILDER_TYPE + ") this").addLine("}").addLB();
    }
    //class InternalBuilder fertig
    cb.addLine("}").addLB();
    
    if (!config.isAbstract) {
      //class Builder
      cb.addLine("public static class Builder extends InternalBuilder<", config.className, ", Builder> {");
      cb.addLine("public Builder() {");
      cb.addLine("super(new ", config.className, "())");
      cb.addLine("}");
      cb.addLine("public Builder(", config.className, " instance) {");
      cb.addLine("super(instance)");
      cb.addLine("}");
      cb.addLine("}").addLB();

      //build-Methode
      cb.addLine("public Builder build", config.className, "() {").addLine("return new Builder(this)").addLine("}").addLB();
    }
  }
  

  private void generateJavaClassBuilder(CodeBuffer cb) {
    BuilderConfig config = new BuilderConfig();
    config.setAbstract(dom.isAbstract());
    config.setClassName(dom.getSimpleClassName());
    config.setImports(importedClassNames);
    config.setMembers(dom.getMemberVars());
    config.setSuperClassName(hasSuperClass() ? dom.getSuperClassGenerationObject().getClassName(importedClassNames) : null);
    generateJavaClassBuilder(cb, config);
  }
  
  private void generateJavaConstructors(CodeBuffer cb) {

    cb.addLine("public ", dom.getSimpleClassName(), "() {");
    cb.addLine("super()");
    cb.addLine("}").addLB();
    
    if (memberVars.size() > 0 && calcLengthOfParameters(memberVars) < 255) {
      cb.addLine("/**");
      cb.addLine("* Creates a new instance using locally defined member variables.");
      //TODO javadoc für alle membervars mit doku aus xml?
      cb.addLine("*/");
      cb.add("public ", dom.getSimpleClassName(), "(");
      for (AVariable v : memberVars) {
        cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames, true) + " "
            + v.getVarName());
      }
      cb.add(") {").addLB();
      cb.addLine("this()");
      for (AVariable v : memberVars) {
        if (v.isList()) {
          cb.addLine("if (", v.getVarName(), " != null) {");
          cb.addLine("this.", v.getVarName(), " = new ", ArrayList.class.getSimpleName(), "<",
                     v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">(", v.getVarName(), ")");
          cb.addLine("} else {");
          cb.addLine("this.", v.getVarName(), " = new " + ArrayList.class.getSimpleName(), "<",
                     v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">()");
          cb.addLine("}");
        } else {
          cb.addLine("this.", v.getVarName(), " = ", v.getVarName());
        }
      }
      cb.addLine("}").addLB();

    }

    if (hasSuperClass()) {

      List<AVariable> inheritedMembers = dom.getSuperClassGenerationObject().getAllMemberVarsIncludingInherited();

      if (inheritedMembers.size() > 0 && calcLengthOfParameters(dom.getAllMemberVarsIncludingInherited()) < 255) {

        cb.addLine("/**");
        cb.addLine("* Creates a new instance expecting all inherited member variables");
        cb.addLine("*/");
        cb.add("public ", dom.getSimpleClassName(), "(");
        for (AVariable v : dom.getAllMemberVarsIncludingInherited()) {
          cb.addListElement(v.getEventuallyQualifiedClassNameWithGenerics(importedClassNames) + " " + v.getVarName());
        }
        cb.add(") {").addLB();
        cb.add("this(");
        for (AVariable v : memberVars) {
          cb.addListElement(v.getVarName());
        }
        cb.add(")").addLB();
        for (AVariable v : inheritedMembers) {
          if (v.isList()) {
            cb.addLine("if (", v.getVarName(), " != null) {");
            cb.addLine("unversionedS", GenerationBase.buildSetter(v.getVarName()).substring(1), "(new ", ArrayList.class.getSimpleName(),
                       "<", v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">(", v.getVarName(), "))");
            cb.addLine("} else {");
            cb.addLine("unversionedS", GenerationBase.buildSetter(v.getVarName()).substring(1), "(new ", ArrayList.class.getSimpleName(),
                       "<", v.getEventuallyQualifiedClassNameNoGenericsAsObject(importedClassNames), ">())");
            cb.addLine("}");
          } else {
            cb.addLine("unversionedS", GenerationBase.buildSetter(v.getVarName()).substring(1), "(", v.getVarName(), ")");
          }
        }

        cb.addLine("}").addLB();

      }

    }
  }
  
  private void generateJavaToString(CodeBuffer cb) {
    cb.addLine("public String toString() {");
    //TODO im generierten code stringbuilder verwenden
    cb.add("return \"",dom.getSimpleClassName(),"(\"");
    boolean first = true;
    if (hasSuperClass()) {
      cb.add("+super.toString()");
      first = false;
    }
    boolean asMap = memberVars.size() > 3;
    for (AVariable v : memberVars) {
      appendMemberVarToString(cb, v, first, asMap);
      first = false;
    }
    cb.add("+\")\";").addLB();
    cb.addLine("}").addLB();
  }  
  
  private void appendMemberVarToString(CodeBuffer cb, AVariable v, boolean first, boolean asMap) {
    PrimitiveType type = v.getJavaTypeEnum();
    String varName = v.getVarName();
    boolean isString = type != null && type == PrimitiveType.STRING;
    boolean isObject = type == null || type.isObject();
    if (logger.isTraceEnabled()) {
      logger.trace(varName+" -> "+type);
    }
    String output;
    if( isString ) {
      output = "(\"\\\"\"+"+varName+"+\"\\\"\")"; // -> ("\""+varName+"\"")
    } else {
      output = v.getVarName();
    }
    cb.add("+\n       ");
    if( asMap ) {
      String mapOutput = null;
      if( first ) {
        mapOutput = "\""+varName+"=\"+"+output; // -> "varName="+output
      } else {
        mapOutput = "\","+varName+"=\"+"+output; // -> ",varName="+output
      }
      if( isObject ) {
        cb.add( nullCheck( varName, "\"\"", mapOutput) );
      } else {
        cb.add(mapOutput);
      }
    } else {
      if( ! first ) {
        cb.add("\",\"+");
      }
      if( isString ) {
        cb.add( nullCheck(varName, "\"null\"", output ) );
      } else {
        cb.add(output);
      }
    }
  }
  private String nullCheck(String varName, String then, String otherwise ) {
    return "("+varName+"==null?"+then+":"+otherwise+")";
  }

  
  private void generateJavaClone(CodeBuffer cb) {
    DOMCloneConfig config = new DOMCloneConfig();
    config.setAbstract(dom.isAbstract());
    config.setHasSuperClass(dom.hasSuperClassGenerationObject());
    config.setImportedClassNames(importedClassNames);
    config.setMembers(memberVars);
    config.setSimpleClassName(dom.getSimpleClassName());
    config.setInstanceOperations(dom.collectOperationsOfDOMHierarchy(false));
    config.setImplClassName(dom.getImplClassName());
    config.setLibraryExists(dom.libraryExists());
    generateJavaClone(cb, config);
  }
  public static void generateJavaClone(CodeBuffer cb, CloneConfig config) {
    cb.addLine("protected void fillVars(", config.getSimpleClassName(), " source, boolean deep) {");
    if (config.hasSuperClass()) {
      cb.addLine("super.fillVars(source, deep)");
    }
    for (AVariable v : config.getMembers()) {
      cb.add("this.", v.getVarName(), " = ");
      v.generateJavaClone(cb, "source", config.getImportedClassNames(), false);
      cb.addLB();
    }

    cb.addLine("}").addLB();

    String cloneMethodName = config instanceof ExceptionCloneConfig ? "cloneWithoutCause" : "clone";
    
    if (!config.isAbstract()) {
      cb.addLine("public ", config.getSimpleClassName(), " ",cloneMethodName, "() {");
      cb.addLine("return ", cloneMethodName, "(true)");
      cb.addLine("}").addLB();

      cb.addLine("public ", config.getSimpleClassName(), " ", cloneMethodName,"(boolean deep) {");

      cb.addLine(config.getSimpleClassName(), " cloned = new ", config.getSimpleClassName(), "()");
      cb.addLine("cloned.fillVars(this, deep)");
      
      if(config instanceof DOMCloneConfig) {
        DOMCloneConfig domConfig = (DOMCloneConfig)config;
        OperationInformation[] instanceOperations = domConfig.getInstanceOperations();
        boolean hasImplInstanceVar =
            instanceOperations.length > 0 && domConfig.getLibraryExists();
        if (hasImplInstanceVar) {
          cb.add("try {").addLB();
          cb.add(Method.class.getName() + " cloneMethod = " + DOM.class.getName() + ".getPublicCloneMethodIfPresent(" + GenerationBase.buildGetter(DOM.INSTANCE_METHODS_IMPL_VAR) + "().getClass())").addLB();
          cb.add("if (cloneMethod != null) {").addLB();
          cb.add("cloned.setImplementationOfInstanceMethods((" + domConfig.getImplClassName() + ")cloneMethod.invoke(" + GenerationBase.buildGetter(DOM.INSTANCE_METHODS_IMPL_VAR), "()))").addLB();
          cb.addLine(JavaServiceImplementation.class.getName(), ".setInstanceVarInImpl(cloned, cloned.", DOM.INSTANCE_METHODS_IMPL_VAR, ")");
          cb.add("}").addLB();
          cb.add("} catch (" + IllegalAccessException.class.getName() + " e) {").addLB();
          cb.add("} catch (" + InvocationTargetException.class.getName() + " e) {").addLB();
          cb.add("}").addLB();
        }
      }
      
      cb.addLine("return cloned");
      cb.addLine("}").addLB();

    } else {
      cb.addLine("public abstract ", config.getSimpleClassName(), " ",cloneMethodName,"()").addLB();
      cb.addLine("public abstract ", config.getSimpleClassName(), " ",cloneMethodName,"(boolean deep)").addLB();
    }
  }


  private void generateVersionChangeCollection(CodeBuffer cb) {
    appendVersionChangeCollection(cb, memberVars, dom.hasSuperClassGenerationObject());
  }


  public static void appendVersionChangeCollection(CodeBuffer cb, List<AVariable> memberVars, boolean hasVersionedSuperType) {
    cb.addLine("public void collectChanges(long start, long end, " + changeSetsOfMembersSignature + ", ", Set.class.getName(),
               "<Long> datapoints) {");
    if (hasVersionedSuperType) {
      cb.addLine("super.collectChanges(start, end, changeSetsOfMembers, datapoints)");
    }
    for (AVariable v : memberVars) {
      if (v.isJavaBaseType()) {
        cb.addLine(XOUtils.class.getSimpleName(), ".addChangesForSimpleMember(", getVarNameForOldVersions(v), ", start, end, datapoints)");
      } else if (v.isList()) {
        cb.addLine(XOUtils.class.getSimpleName(), ".addChangesForComplexListMember(this.", v.getVarName(), ", ", getVarNameForOldVersions(v),
                   ", start, end, changeSetsOfMembers, datapoints)");
      } else {
        cb.addLine(XOUtils.class.getSimpleName(), ".addChangesForComplexMember(this.", v.getVarName(), ", ", getVarNameForOldVersions(v),
                   ", start, end, changeSetsOfMembers, datapoints)");
      }
    }
    cb.addLine("}").addLB();
  }


  private final static String changeSetsOfMembersSignature = IdentityHashMap.class.getName() + "<"
      + GeneralXynaObject.class.getSimpleName() + ", " + DataRangeCollection.class.getName() + "> changeSetsOfMembers";


  public static void appendObjectVersionClassGeneration(CodeBuffer cb, List<AVariable> memberVars, Set<String> imports,
                                                        String simpleClassName, String fqSuperClassName) {
    String superClassName;
    boolean hasSuperObjectVersionClass = fqSuperClassName != null;
    if (hasSuperObjectVersionClass) {
      superClassName = fqSuperClassName + ".ObjectVersion"; //TODO klassenname könnte bei nicht-generiertem code anders sein?!
    } else {
      superClassName = XOUtils.ObjectVersionBase.class.getCanonicalName();
    }
    cb.addLine("public static class ObjectVersion extends ", superClassName, " {").addLB();

    cb.addLine("public ObjectVersion(", GeneralXynaObject.class.getSimpleName(), " xo, long version, ", changeSetsOfMembersSignature, ") {");
    cb.addLine("super(xo, version, changeSetsOfMembers)");
    cb.addLine("}").addLB(); //end construktor

    cb.addLine("protected boolean memberEquals(", ObjectVersionBase.class.getCanonicalName(), " o) {");
    if (hasSuperObjectVersionClass) {
      cb.addLine("if (!super.memberEquals(o)) {");
      cb.addLine("return false");
      cb.addLine("}");
    }
    if (memberVars.size() > 0) {
      cb.addLine("ObjectVersion other = (ObjectVersion) o");
      cb.addLine(simpleClassName, " xoc = (", simpleClassName, ") xo");
      cb.addLine(simpleClassName, " xoco = (", simpleClassName, ") other.xo");

      //compare membervars
      for (AVariable v : memberVars) {
        String var = "xoc.versionedG" + GenerationBase.buildGetter(v.getVarName()).substring(1) + "(this.version)";
        String otherVar = "xoco.versionedG" + GenerationBase.buildGetter(v.getVarName()).substring(1) + "(other.version)";
        if (v.isJavaBaseType()) {
          if (v.isList()) {
            cb.addLine("if (!listEqual(", var, ", ", otherVar, ")) {");
          } else if (v.getJavaTypeEnum().isObject()) {
            cb.addLine("if (!equal(", var, ", ", otherVar, ")) {");
          } else {
            cb.addLine("if (", var, " != ", otherVar, ") {");
          }
        } else if (v.isList()) {
          //FIXME zyklencheck: ähnlich wie bei hashcode berechnung stack von einem der objekte durchreichen
          cb.addLine("if (!listEqual(", var, ", ", otherVar, ", this.version, other.version, changeSetsOfMembers)) {");
        } else {
          //FIXME zyklencheck
          cb.addLine("if (!xoEqual(", var, ", ", otherVar, ", this.version, other.version, changeSetsOfMembers)) {");
        }
        cb.addLine("return false");
        cb.addLine("}");
      }
    }

    cb.addLine("return true");
    cb.addLine("}").addLB(); //end memberEquals

    //hashcode
    cb.addLine("public int calcHashOfMembers(", Stack.class.getName(), "<", GeneralXynaObject.class.getSimpleName(), "> stack) {");
    if (hasSuperObjectVersionClass) {
      cb.addLine("int hash = super.calcHashOfMembers(stack)");
    } else {
      cb.addLine("int hash = 1");
    }

    if (memberVars.size() > 0) {
      cb.addLine(simpleClassName, " xoc = (", simpleClassName, ") xo");
      for (AVariable v : memberVars) {
        String vg = "xoc.versionedG" + GenerationBase.buildGetter(v.getVarName()).substring(1) + "(this.version)";
        cb.addLine(v.getEventuallyQualifiedClassNameWithGenerics(imports), " ", v.getVarName(), " = ", vg);
        if (v.isJavaBaseType()) {
          if (v.isList()) {
            cb.addLine("hash = hash * 31 + hashList(", v.getVarName(), ")");
          } else if (v.getJavaTypeEnum().isObject()) {
            cb.addLine("hash = hash * 31 + (", v.getVarName(), " == null ? 0 : ", v.getVarName(), ".hashCode())");
          } else {
            cb.addLine("hash = hash * 31 + ", v.getJavaTypeEnum().getObjectClassOfType(), ".valueOf(", v.getVarName(), ").hashCode()");
          }
        } else if (v.isList()) {
          //TODO zyklen check: stack beachten. falls stack objekt enthält, dann den abstand im stack zu dem eigenen objekt als für den hash relevante zahl verwenden.
          //also A-B-A -> hash*31+2, A-B-C-A -> hash*31+3 usw
          cb.addLine("hash = hash * 31 + hashList(", v.getVarName(), ", this.version, changeSetsOfMembers, stack)");
        } else {
          //TODO zyklen check                
          cb.addLine("hash = hash * 31 + (", v.getVarName(), " == null ? 0 : ", v.getVarName(),
                     ".createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack))");
        }
      }
    }

    cb.addLine("return hash");
    cb.addLine("}").addLB(); //end calchash

    cb.addLine("}").addLB(2); //end class
    
    //im generierten code kann man nicht mit "new" neue objectversions von membervars erzeugen, weil die laufzeit klasse nicht klar ist (vererbung!)
    cb.addLine("public ObjectVersion createObjectVersion(long version, ", changeSetsOfMembersSignature, ") {");
    cb.addLine("return new ObjectVersion(this, version, changeSetsOfMembers)");
    cb.addLine("}").addLB(2);
  }


  private void generateObjectVersionClass(CodeBuffer cb) {
    String superFqClassName = null;
    if (dom.hasSuperClassGenerationObject()) {
      superFqClassName = dom.getSuperClassGenerationObject().getClassName(importedClassNames);
    }
    appendObjectVersionClassGeneration(cb, memberVars, importedClassNames, dom.getSimpleClassName(), superFqClassName);
  }


  private void generateJavaToXML(CodeBuffer cb) {
    cb.addLine("public String toXml(String varName, boolean onlyContent) {");
    cb.addLine("return toXml(varName, onlyContent, -1, null)");
    cb.addLine("}").addLB();

    cb.addLine("public String toXml(String varName, boolean onlyContent, long version, ",
               GeneralXynaObject.XMLReferenceCache.class.getCanonicalName(), " cache) {");

    cb.addLine(StringBuilder.class.getSimpleName() + " xml = new " + StringBuilder.class.getSimpleName() + "()");


    //TODO codeanteile aus generiertem code auslagern -> nicht so einfach, weil rekursive aufrufe
    cb.addLine("long objectId");
    cb.addLine("if (!onlyContent) {");
    appendToXMLSnippetForObjectReferences(cb);

    cb.addLine(XMLHelper.class.getSimpleName(), ".beginType(xml, varName, \"", dom.getOriginalSimpleName(), "\", \"",
               dom.getOriginalPath(), "\", objectId, refId, ", RevisionManagement.class.getSimpleName(),
               ".getRevisionByClass(", dom.getSimpleClassName(), ".class), cache)");
    cb.addLine("} else {"); //onlyContent
    cb.addLine("objectId = -1");
    cb.addLine("}");

    if (memberVars.size() > 0 || hasSuperClass()) {
      cb.addLine("if (objectId != -2) {"); //objectId == -2 bedeutet, dass referenziert wird
      if (hasSuperClass()) {
        cb.addLine("xml.append(super.toXml(varName, true, version, cache))");
      }
      for (AVariable v : memberVars) {
        v.generateJavaXml(cb, true);
      }
      cb.addLine("}"); //end if objectId != -2
    }
    cb.addLine("if (!onlyContent) {");
    cb.addLine(XMLHelper.class.getSimpleName(), ".endType(xml)");
    cb.addLine("}");
    cb.addLine("return xml.toString()");
    cb.addLine("}").addLB();
  }


  private void generateJavaGetVariableNames(CodeBuffer cb) {
    cb.add("private static Set<String> varNames = " + Collections.class.getSimpleName()
        + ".unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{");
    for (int i = 0; i < memberVars.size(); i++) {
      cb.addListElement("\"" + memberVars.get(i).getVarName() + "\"");
    }
    cb.add("})));").addLB();

    cb.addLine("/**");
    cb.addLine("* @deprecated use {@link #getVariableNames()} instead");
    cb.addLine("*/");
    cb.addLine("@Deprecated");
    cb.addLine("public HashSet<String> getVarNames() {");
    if (hasSuperClass()) {
      // as long as this is not regenerated once the parent class changes, the set has to be calculated dynamically 
      cb.addLine("HashSet<String> ret = new HashSet<String>(varNames)");
      cb.addLine("ret.addAll(super.getVarNames())");
      cb.addLine("return ret");
    } else {
      cb.addLine("return new HashSet<String>(varNames)");
    }
    cb.addLine("}").addLB().addLB();

    // people should use the following method instead
    cb.addLine("public Set<String> getVariableNames() {");
    if (hasSuperClass()) {
      // as long as this is not regenerated once the parent class changes, the set has to be calculated dynamically
      // TODO performance
      cb.addLine("Set<String> ret = new HashSet<String>(varNames)");
      cb.addLine("ret.addAll(super.getVariableNames())");
      cb.addLine("return " + Collections.class.getSimpleName() + ".unmodifiableSet(ret)");
    } else {
      cb.addLine("return varNames");
    }
    cb.addLine("}").addLB();

  }
  
  private void generateJavaGeneralGetter(CodeBuffer cb) {
    appendGeneralGetter(cb, importedClassNames, memberVars, hasSuperClass());
  }
  
  private void generateJavaGeneralSetter(CodeBuffer cb) {
    appendGeneralSetter(cb, importedClassNames, memberVars, hasSuperClass());
  }
  
  private void generateJavaMethods(CodeBuffer cb) {
    // methoden
    for (List<Operation> operations : dom.getServiceNameToOperationMap().values()) {
      for (Operation o : operations) {
        o.generateJava(cb, importedClassNames);
      }
    }

    if (!isAbstract) {
      // non-static onDeployment
      appendDeploymenthandlingMethod(cb, "onDeployment", "onDeployment", "callOndeploymentHandlerFromServiceImpl");
      appendDeploymenthandlingMethod(cb, "onUndeployment", "onUndeployment", "callOnUndeploymentHandlerFromServiceImpl");
    }

    OperationInformation[] instanceOperations = dom.collectOperationsOfDOMHierarchy(false);
    boolean hasImplInstanceVar =
        instanceOperations.length > 0 && dom.isFirstTypeOfHierarchyWithJavaImpl(true, null) && dom.libraryExists();
    if (transientExceptionVariables.size() > 0 || hasImplInstanceVar) {
      generateJavaReadWriteObject(cb, importedClassNames, transientExceptionVariables, hasImplInstanceVar);
    }

    if (instanceOperations.length > 0) {
      //es gibt instanzmethoden

      if (hasImplInstanceVar) {
        //variable existiert nur einmal in der hierarchie, die subklassen verwenden diese mit
        //volatile für die lazy initialisierung
        cb.addLine("protected volatile transient ", dom.getImplClassName(), " ", DOM.INSTANCE_METHODS_IMPL_VAR).addLB();
        cb.addLine("protected void ", GenerationBase.buildSetter(DOM.INSTANCE_METHODS_IMPL_VAR), "(", dom.getImplClassName(),
                   " ", DOM.INSTANCE_METHODS_IMPL_VAR, ") {");
        cb.addLine("this.", DOM.INSTANCE_METHODS_IMPL_VAR, " = ", DOM.INSTANCE_METHODS_IMPL_VAR);
        cb.addLine("}").addLB();       
      }
      boolean hasJavaImpl = dom.hasJavaImpl(true, null);

      if (hasJavaImpl && isAbstract) {
        if (dom.isFirstTypeOfHierarchyWithJavaImpl(true, null)) {
          /*
           * usecase: hierarchy A -> B -> C
           * A hat methode
           * B ist abstrakt
           * C wird instanziiert und ruft methode auf. dann darf in B und C init() nicht überschrieben sein
           */
          cb.addLine("protected abstract void ", DOM.INIT_METHODNAME, "()");
        }
      } else if (hasJavaImpl && dom.libraryExists()) {
        //init methode aufrufen
        cb.addLine("protected void ", DOM.INIT_METHODNAME, "() {");
        cb.addLine("if (", DOM.INSTANCE_METHODS_IMPL_VAR, " == null) {");
        cb.addLine("synchronized(this) {");
        cb.addLine("if (", DOM.INSTANCE_METHODS_IMPL_VAR, " == null) {");
        cb.addLine(GenerationBase.buildSetter(DOM.INSTANCE_METHODS_IMPL_VAR), "(new ", dom.getImplClassName(), "(this))");
        cb.addLine("}"); //if
        cb.addLine("}"); //sync
        cb.addLine("}"); //if
        cb.addLine("}").addLB();

      } else if (hasJavaImpl //äquivalent zu: (hasImpl && !libraryExists()) || (!hasImpl && !abstract && hasOnlyAbstractBla())
          || (!isAbstract && hasOnlyAbstractSuperTypesUntilFirstTypeWithJavaImpl())) {
        //init wird lokal nicht benötigt, aber weil die methode abstrakt ist, muss sie definiert sein

        //kann zustand beim ersten speichern von der gui aus sein -> soll kompilieren. TODO über irgendein flag steuern, ob das
        //hier eine runtimeexception zur laufzeit oder zur deployzeit wirft.
        cb.addLine("protected void ", DOM.INIT_METHODNAME, "() {");
        cb.addLine("throw new ", RuntimeException.class.getName(), "(\"Unexpected call of ", DOM.INIT_METHODNAME,
                   ". Non abstract implementation of library is needed.\")");
        cb.addLine("}");
        cb.addLB();

        //keine impl -> kein getter benötigt
      }
      
      if (hasJavaImpl && dom.libraryExists()) {
        cb.addLine("public ", dom.getImplClassName(), " ", GenerationBase.buildGetter(DOM.INSTANCE_METHODS_IMPL_VAR), "() {");
        cb.addLine(DOM.INIT_METHODNAME, "()");
        cb.addLine("return (", dom.getImplClassName(), ") ", DOM.INSTANCE_METHODS_IMPL_VAR);
        cb.addLine("}").addLB();
      }

      if (dom.hasSuperTypeWithInstanceMethods(null)) {
        for (OperationInformation oi : dom.getSuperClassGenerationObject().collectOperationsOfDOMHierarchy(false)) {
          if (!oi.isAbstract() && !oi.isFinal()) {
            cb.addLine("/**");
            cb.addLine(" * \"Publishing\" of super-call. May be called by reflection only.");
            cb.addLine(" */");
            cb.add("private ");
            oi.getOperation().createMethodSignature(cb,
                                                    true,
                                                    importedClassNames,
                                                    oi.getOperation().getName()
                                                        + Operation.METHOD_INTERNAL_SUPERCALL_PROXY,
                                                        dom.getSuperClassGenerationObject().getClassName(importedClassNames) + " "
                                                        + Operation.VAR_SUPERCALL_DELEGATOR);
            cb.add(" {").addLB();
            if (oi.getOperation().getOutputVars() != null && oi.getOperation().getOutputVars().size() > 0) {
              cb.add("return ");
            }
            oi.getOperation().generateJavaForInvocation(cb,
                                                        "super." + oi.getOperation().getName()
                                                            + Operation.METHOD_INTERNAL_SUPERCALL_DESTINATION_SUFFIX,
                                                        Operation.VAR_SUPERCALL_DELEGATOR);
            cb.addLB();
            cb.addLine("}").addLB();
          }
        }
      }
    }
    // ende
    cb.addLine("}");

  }


  private void generateJavaReadWriteObject(CodeBuffer cb, Set<String> currentImportsFqClasses,
                                           Set<ExceptionVariable> transientExceptionVariables, boolean hasImplInstanceVar) {
    //irgendwie muss man bei der deserialisierung wieder an die classloader der causes von exceptions kommen => dazu serializableclassloadedobject verwenden
    
    //für die instanzen der impls muss man auch serializableclassloaded object verwenden, weil die impl-klassen dem classloader, der die serialisierung
    //gestartet hat (vgl. ContainerClass), nicht bekannt sein müssen.

    //TODO sortierung derart, dass eine umbenennung einer variablen nicht zur inkompatibilität mit dem alten stand führt.
    //sortieren, damit die reihenfolge immer gleich ist:
    List<ExceptionVariable> exceptionVariables = new ArrayList<ExceptionVariable>(transientExceptionVariables);
    Collections.sort(exceptionVariables, ScopeStep.comparatorForExceptionVariableSerialization);
    cb.addLine("private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {");
    cb.addLine("s.defaultReadObject()");
    for (ExceptionVariable transientExceptionVariable : exceptionVariables) {
      if (transientExceptionVariable.isList()) {
        cb.addLine("{");
        cb.addLine("List<", SerializableClassloadedException.class.getSimpleName(), "> tmpList = (List<", SerializableClassloadedException.class.getSimpleName(), ">) s.readObject()" );
        cb.addLine("if (tmpList != null) {");
        cb.addLine("List<", transientExceptionVariable.getClassNameDirectly(), "> dataList = new ArrayList<", transientExceptionVariable.getClassNameDirectly(), ">()" );
        cb.addLine("for (", SerializableClassloadedException.class.getSimpleName(), " scxo : tmpList) {");
        cb.addLine("dataList.add((", transientExceptionVariable.getClassNameDirectly(), ") scxo.", METHODNAME_GET_THROWABLE, "());");
        cb.addLine("}");
        cb.addLine("this.", transientExceptionVariable.getVarName(), " = dataList");
        cb.addLine("}"); //if != null
        cb.addLine("}");
      } else {
        cb.addLine("this.", transientExceptionVariable.getVarName(), " = (",
                   transientExceptionVariable.getEventuallyQualifiedClassNameNoGenerics(currentImportsFqClasses),
                   ") ((", SerializableClassloadedException.class.getSimpleName(), ") s.readObject()).", METHODNAME_GET_THROWABLE, "()");
      }
    }
    if (hasImplInstanceVar) {
      cb.addLine(DOM.INSTANCE_METHODS_IMPL_VAR, " = (", dom.getImplClassName(), ") ((",
                 SerializableClassloadedObject.class.getSimpleName(), ") s.readObject()).", METHODNAME_GET_OBJECT, "()");
    }

    cb.addLine("}").addLB();

    cb.addLine("private void writeObject(ObjectOutputStream s) throws IOException {");
    cb.addLine("s.defaultWriteObject()");
    for (ExceptionVariable transientExceptionVariable : exceptionVariables) {
      if (transientExceptionVariable.isList()) {
        cb.addLine("if (this.", transientExceptionVariable.getVarName(), " != null) {");
        cb.addLine("List<", SerializableClassloadedException.class.getSimpleName(), "> tmpList = new ArrayList<", SerializableClassloadedException.class.getSimpleName(), ">()" );
        cb.addLine("for (", transientExceptionVariable.getClassNameDirectly(), " ex : this.", transientExceptionVariable.getVarName(), ") {");
        cb.addLine("tmpList.add(new ", SerializableClassloadedException.class.getSimpleName(), "(ex))");
        cb.addLine("}");
        cb.addLine("s.writeObject(tmpList)");
        cb.addLine("} else {");
        cb.addLine("s.writeObject(null)");
        cb.addLine("}");
      } else {
        cb.addLine("s.writeObject(new ", SerializableClassloadedException.class.getSimpleName(), "(this.",
                   transientExceptionVariable.getVarName(), "))");
      }
    }
    if (hasImplInstanceVar) {
      cb.addLine("if (", DOM.INSTANCE_METHODS_IMPL_VAR, " == null) {");
      cb.addLine("s.writeObject(new ", SerializableClassloadedObject.class.getSimpleName(), "(null))");
      cb.addLine("} else {");
      cb.addLine("s.writeObject(new ", SerializableClassloadedObject.class.getSimpleName(), "(",
                 DOM.INSTANCE_METHODS_IMPL_VAR, ", ", DOM.INSTANCE_METHODS_IMPL_VAR, ".getClass().getClassLoader()))");
      cb.addLine("}");
    }
    cb.addLine("}");
    cb.addLB();
  }

  private void appendDeploymenthandlingMethod(CodeBuffer cb, String methodName, String methodToBeCalled,
                                              String methodForDeploymentTask) {

    cb.addLine("public void ", methodName, "() throws ", XynaException.class.getSimpleName(), " {");

    cb.addLine("super.", methodToBeCalled, "()");
    if (dom.hasJavaImpl(false, null) && dom.libraryExists()) {
      // the revision has to be determined at compile time because the code is not regenerated when building a new revision
      cb.addLine("long thisRevision = ((", ClassLoaderBase.class.getName(), ") getClass().getClassLoader()).", METHODNAME_GET_REVISION, "()");
      cb.addLine("String path = ", DOM.class.getName(), ".", METHODNAME_GET_DEPLOYED_JAR_FILE_PATH_IN_MDM, "(getClass().getName(), thisRevision)");
      cb.addLine("path += \"", dom.getImplClassName(), ".jar\"");
      cb.addLine("if (new ", File.class.getName(), "(path).exists()) {");
      cb.addLine("Object o = null");
      cb.addLine("try {");
      cb.addLine("Class<?> c = Class.forName(\"", dom.getImplFqClassName(), "\")");
      cb.addLine("if (!", DeploymentTask.class.getSimpleName(), ".class.isAssignableFrom(c)) {");
      cb.addLine("if (logger.isTraceEnabled()) {");
      cb.addLine("logger.trace(\"class ", dom.getImplFqClassName(), " is no deployment task, cannot call ", methodName, "\")");
      cb.addLine("}");
      cb.addLine("return");
      cb.addLine("}");
      cb.addLine("o = c.getDeclaredConstructor().newInstance()");
      cb.addLine("} catch (", Throwable.class.getName(), " t) {");
      cb.addLine("throw new ", XPRC_MDMDeploymentException.class.getSimpleName(), "(\"", dom.getOriginalFqName(), "\", t)");
      cb.addLine("}");
      //nun der eigentliche aufruf der methode
      cb.addLine(methodForDeploymentTask, "((", DeploymentTask.class.getSimpleName(), ")o)");
      cb.addLine("}");
    }

    cb.addLine("}").addLB();
  }
  
  /**
   * true, falls: alle supertypen bis zu dem ersten, der javaimpl hat (ihn inkludierend), sind abstrakt.
   * false, falls es mindestens einen nicht abstrakten supertypen bis dahin gibt oder falls es gar keinen supertyp mit javaimpl gibt
   */
  private boolean hasOnlyAbstractSuperTypesUntilFirstTypeWithJavaImpl() {
    DOM[] hierarchy = dom.getDOMHierarchy();
    boolean first = true;
    for (DOM d : hierarchy) {
      if (first) {
        first = false;
        continue;
      }
      
      if (!d.isAbstract()) {
        return false;
      }
      if (d.isFirstTypeOfHierarchyWithJavaImpl(true, null)) {
        return true;
      }
    }
    return false;
  }

  
  private boolean hasSuperClass() {
    return dom.getSuperClassGenerationObject() != null;
  }

  protected static boolean isEmpty(String s) {
    return s == null || s.trim().length() == 0;
  }
  

  //every long and every double counts twice
  protected int calcLengthOfParameters(List<AVariable> list) {
    long longDoubles = list.stream()
        .filter(x -> x.isJavaBaseType && !x.isList && (x.javaType == PrimitiveType.LONG || x.javaType == PrimitiveType.DOUBLE)).count();
    return (int) (list.size() + longDoubles);
  }


  public static void appendToXMLSnippetForObjectReferences(CodeBuffer cb) {
    cb.addLine("long refId");
    cb.addLine("if (cache != null) {");      
    cb.addLine("ObjectVersion ov = new ObjectVersion(this, version, cache.changeSetsOfMembers)"); 
    cb.addLine("refId = cache.putIfAbsent(ov)"); //equals+hashcode verwendung von objectversion.
    cb.addLine("if (refId > 0) {"); 
    //bereits vorhanden im cache
    cb.addLine("objectId = -2"); //keinen content schreiben
    cb.addLine("} else {"); //keine refid
    cb.addLine("objectId = -refId");
    cb.addLine("refId = -1");
    cb.addLine("}");
    cb.addLine("} else {"); //cache == null
    cb.addLine("objectId = -1");
    cb.addLine("refId = -1");
    cb.addLine("}");
  }

  public static void appendGeneralSetter(CodeBuffer cb, Set<String> imports, List<AVariable> memberVars, boolean hasSuperClassWithGeneralGetter) {
    // allgemeiner setter
    cb.addLine("public void set(String name, Object o) throws ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getSimpleName(), " {");
    for (int i = 0; i < memberVars.size(); i++) {
      AVariable v = memberVars.get(i);
      if (i > 0) {
        cb.add(" else ");
      }
      cb.add("if (\"", v.getVarName(), "\".equals(name)) {").addLB();
      if (v.isJavaBaseType() && !v.isList()) {

        cb.addLine(XOUtils.class.getSimpleName(), ".checkCastability(o, ", v.getJavaTypeAsObject(), ".class, \"", v.getVarName(), "\")");
        if (!v.getJavaTypeEnum().isObject()) {
          //null checken, weil es ansonsten nicht gesetzt werden kann!
          cb.addLine("if (o != null) {");
        }

        cb.addLine(GenerationBase.buildSetter(v.getVarName()), "((", v.getJavaTypeAsObject(), ") o)");
        if (!v.getJavaTypeEnum().isObject()) {
          cb.addLine("}");
        }
      } else {

        boolean nextIsList = v.isList();
        if (nextIsList) {
          cb.addLine("if (o != null) {");
          cb.addLine("if (!(o instanceof ", List.class.getSimpleName(), ")) {");
          cb.addLine("throw new ", IllegalArgumentException.class.getSimpleName(), "(\"Error while setting member variable ",
                     v.getVarName(), ", expected list, got \" + o.getClass().getName())");
          cb.addLine("}");
          cb.addLine("if (((", List.class.getSimpleName(), ") o).size() > 0) {");
          cb.addLine(XOUtils.class.getSimpleName(), ".checkCastability(((", List.class.getSimpleName(), ") o).get(0), ",
                     v.getEventuallyQualifiedClassNameNoGenericsAsObject(imports), ".class, \"", v.getVarName(), "\")");
          cb.addLine("}");
          cb.addLine("}");
        } else {
          cb.addLine(XOUtils.class.getSimpleName(), ".checkCastability(o, ", v.getEventuallyQualifiedClassNameNoGenerics(imports), ".class, \"",
                     v.getVarName(), "\")");
        }
        cb.addLine(GenerationBase.buildSetter(v.getVarName()), "((",
                   v.getEventuallyQualifiedClassNameWithGenerics(imports, false), ") o)");

      }
      cb.add("}");
    }
    if (memberVars.size() > 0) {
      cb.add(" else {").addLB();
    }
    if (hasSuperClassWithGeneralGetter) {
      cb.addLine("super.set(name, o)");
    } else {
      cb.addLine("throw new " + XDEV_PARAMETER_NAME_NOT_FOUND.class.getSimpleName() + "(name)");
    }
    if (memberVars.size() > 0) {
      cb.addLine("}");
    }
    cb.addLine("}").addLB();
  }

  public static void appendGeneralGetter(CodeBuffer cb, Set<String> imports, List<AVariable> memberVars, boolean hasSuperClassWithGeneralGetter) {
    // allgemeiner getter
    cb.addLine("/**");
    cb.addLine(" * gets membervariable by name or path. e.g. get(\"myVar.myChild\") gets");
    cb.addLine(" * the child variable of the membervariable named \"myVar\" and is equivalent");
    cb.addLine(" * to getMyVar().getMyChild()");
    cb.addLine(" * @param name variable name or path separated by \".\".");
    cb.addLine(" */");

    cb.addLine("public Object get(String name) throws ", InvalidObjectPathException.class.getSimpleName(), " {");
    cb.add("String[] varNames = new String[]{");
    for (int i = 0; i < memberVars.size(); i++) {
      cb.addListElement("\"" + memberVars.get(i).getVarName() + "\"");
    }
    cb.add("};").addLB();
    cb.add("Object[] vars = new Object[]{");
    for (AVariable v : memberVars) {
      if (v.isList()) {
        //getter verwenden, um bei listen die versionsbewusste liste zu bekommen
        cb.addListElement(GenerationBase.buildGetter(v.getVarName()) + "()");
      } else {
        cb.addListElement("this." + v.getVarName());
      }
    }
    cb.add("};").addLB();
    cb.addLine("Object o = ", XOUtils.class.getSimpleName(), ".getIfNameIsInVarNames(varNames, vars, name)");
    cb.addLine("if (o == ", XOUtils.class.getSimpleName(), ".VARNAME_NOTFOUND) {");
    if (hasSuperClassWithGeneralGetter) {
      cb.addLine("o = super.get(name)");
      cb.addLine("if (o == ", XOUtils.class.getSimpleName(), ".VARNAME_NOTFOUND) {");
    }
    cb.addLine("throw new ", InvalidObjectPathException.class.getSimpleName(), "(new ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getSimpleName(), "(name))");
    if (hasSuperClassWithGeneralGetter) {
      cb.addLine("}");
    }
    cb.addLine("}");
    cb.addLine("return o");
    cb.addLine("}").addLB();

  }
  
  
  public static void appendUnversionedSetter(CodeBuffer cb, Set<String> imports, AVariable v) {
    cb.addLine("public void unversionedS", GenerationBase.buildSetter(v.getVarName()).substring(1), "(",
               v.getEventuallyQualifiedClassNameWithGenerics(imports, false), " ", v.getVarName(), ") {");
    if (v.isList()) {
      cb.addLine("this.", v.getVarName(), " = ", XOUtils.class.getSimpleName(), ".substituteList(", v.getVarName(), ")");
    } else {
      cb.addLine("this.", v.getVarName(), " = ", v.getVarName());
    }
    cb.addLine("}");
    cb.addLB();
  }


  public static CodeBuffer createExceptionBuilderAdditionalConstructorContent(Set<String> imports, List<AVariable> members) {
    CodeBuffer cb = new CodeBuffer("XynaProcessing");
    CodeBuffer constructor;
    // 1. initialize lists empty, 
    for (AVariable memberVar : members) {
      if (!memberVar.isList) {
        continue;
      }
      //creates: member(new List<type>() {});
      constructor = new CodeBuffer("XynaProcessing");
      memberVar.generateConstructor(constructor, imports);
      cb.addLine(memberVar.getVarName() + "(", constructor.toString(false), ")");
    }

    return cb;
  }
  
  
  private static final String FIELD_MAP_NAME = "fieldMap";
  public static final String FIELD_GETTER_METHOD_NAME = "getField";

  public void generateJavaFieldCache(CodeBuffer cb) {
    final String fieldMapName = FIELD_MAP_NAME;
    final String fieldGetterMethodName = FIELD_GETTER_METHOD_NAME;
    final String superFieldGetterMethodName = FIELD_GETTER_METHOD_NAME;
    final String inputParamName = "target_fieldname";
    final String resultParamName = "foundField";
    cb.addLine("private static ", ConcurrentMap.class.getSimpleName(), "<", String.class.getSimpleName(), ", ", Field.class.getSimpleName(), "> ", fieldMapName, " = new ", ConcurrentHashMap.class.getSimpleName(), "()");
    cb.addLB();
    cb.addLine("public static ", Field.class.getSimpleName(), " ", fieldGetterMethodName, "(", String.class.getSimpleName()," ",inputParamName, ") throws ", InvalidObjectPathException.class.getSimpleName(), " {");
    cb.addLine(Field.class.getSimpleName(), " ", resultParamName, " = null");
    cb.addLine(resultParamName, " = ", fieldMapName, ".get(", inputParamName, ")");
    cb.addLine("if (", resultParamName, " != null) {");
    cb.addLine("return ", resultParamName);
    cb.addLine("}");
    cb.addLine("try {");
    cb.addLine(resultParamName, " = ", dom.getSimpleClassName(), ".class.getDeclaredField(", inputParamName,")");
    cb.addLine("} catch (", NoSuchFieldException.class.getSimpleName(), " e) {");
    cb.addLine("}");
    cb.addLine("if (", resultParamName, " == null) {");
    if (dom.hasSuperClassGenerationObject()) {
      cb.addLine("return ", dom.getSuperClassGenerationObject().getFqClassName(), ".", superFieldGetterMethodName, "(", inputParamName, ")");
    } else {
      cb.addLine("throw new ", InvalidObjectPathException.class.getSimpleName(), "(new ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getSimpleName(), "(", inputParamName, "))");
    }
    cb.addLine("} else {");
    cb.addLine(resultParamName, ".setAccessible(true)");
    cb.addLine(fieldMapName, ".put(", inputParamName, ", ", resultParamName, ")");
    cb.addLine("return ", resultParamName);
    cb.addLine("}");
    cb.addLine("}");
    cb.addLB();
  }
  
  
  public static abstract class CloneConfig{
    
    protected String simpleClassName;
    protected boolean hasSuperClass;
    protected boolean isAbstract;
    protected Set<String> importedClassNames;
    protected List<AVariable> members;
    
    public String getSimpleClassName() {
      return simpleClassName;
    }
    
    public void setSimpleClassName(String simpleClassName) {
      this.simpleClassName = simpleClassName;
    }
    
    public boolean hasSuperClass() {
      return hasSuperClass;
    }
    
    public void setHasSuperClass(boolean hasSuperClass) {
      this.hasSuperClass = hasSuperClass;
    }
    
    public Set<String> getImportedClassNames() {
      return importedClassNames;
    }
    
    public void setImportedClassNames(Set<String> importedClassNames) {
      this.importedClassNames = importedClassNames;
    }
    
    public List<AVariable> getMembers() {
      return members;
    }
    
    public void setMembers(List<AVariable> members) {
      this.members = members;
    }

    
    public boolean isAbstract() {
      return isAbstract;
    }

    
    public void setAbstract(boolean isAbstract) {
      this.isAbstract = isAbstract;
    }
  }
  
  public static class DOMCloneConfig extends CloneConfig{
    protected boolean libraryExists;
    protected OperationInformation[] instanceOperations;
    protected String implClassName;
    
    public boolean getLibraryExists() {
      return libraryExists;
    }
    
    public void setLibraryExists(boolean libraryExists) {
      this.libraryExists = libraryExists;
    }
    
    public OperationInformation[] getInstanceOperations() {
      return instanceOperations;
    }
    
    public void setInstanceOperations(OperationInformation[] instanceOperations) {
      this.instanceOperations = instanceOperations;
    }
    
    public String getImplClassName() {
      return implClassName;
    }
    
    public void setImplClassName(String implClassName) {
      this.implClassName = implClassName;
    }
  }
  
  public static class ExceptionCloneConfig extends CloneConfig{
    
  }
  
  public static class BuilderConfig {

    private String className;
    private String superClassName;
    private Set<String> imports;
    private List<AVariable> members;
    private boolean isAbstract;
    private CodeBuffer additionalConstructorContent;


    public CodeBuffer getAdditionalConstructorContent() {
      return additionalConstructorContent;
    }


    public void setAdditionalConstructorContent(CodeBuffer additionalConstructorContent) {
      this.additionalConstructorContent = additionalConstructorContent;
    }


    public String getClassName() {
      return className;
    }


    public void setClassName(String className) {
      this.className = className;
    }


    public String getSuperClassName() {
      return superClassName;
    }


    public void setSuperClassName(String superClassName) {
      this.superClassName = superClassName;
    }


    public Set<String> getImports() {
      return imports;
    }


    public void setImports(Set<String> imports) {
      this.imports = imports;
    }


    public List<AVariable> getMembers() {
      return members;
    }


    public void setMembers(List<AVariable> members) {
      this.members = members;
    }


    public boolean isAbstract() {
      return isAbstract;
    }


    public void setAbstract(boolean isAbstract) {
      this.isAbstract = isAbstract;
    }
  }

}
