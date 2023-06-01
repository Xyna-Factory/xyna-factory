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
package com.gip.xyna.xdev.xlibdev.codeaccess;



import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ComponentType;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.FileUpdate;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess.ModificationType;
import com.gip.xyna.xdev.xlibdev.codeaccess.ComponentCodeChange.ComponentNotRegistered;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification.RepositoryModificationType;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ServiceImplementationTemplate;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;



/**
 * compile+jar erstellung falls notwendig (bei eingecheckten jars nicht notwendig).
 * und dann copy ins saved-dir der komponente (bzw ins deployed-dir, falls kein saved existiert)
 */
public class XynaComponentBuilder {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaComponentBuilder.class);
  private static final String SERVER_LIB = "lib";
  private static final String USER_LIB = "userlib";
  private static String factoryVersion;
  static {
    try {
      factoryVersion = Updater.getInstance().getFactoryVersion().getString();
    } catch (XPRC_VERSION_DETECTION_PROBLEM e) {
      logger.warn(null, e);
      factoryVersion = "unknown";
    } catch (PersistenceLayerException e) {
      logger.warn(null, e);
      factoryVersion = "unknown";
    }
  }

  public static final FilenameFilter jarFilter = new FilenameFilter() {

    public boolean accept(File dir, String fileName) {
      return fileName.endsWith(".jar");
    }
  };
  
  public static final FilenameFilter javaFilter = new FilenameFilter() {

    public boolean accept(File dir, String fileName) {
      return fileName.endsWith(".java") || new File(dir, fileName).isDirectory(); // isDirectory check only needed for findFilesRecursively
    }
  };

  private final CodeAccess codeAccess;
  private final String compileTargetVersion;


  XynaComponentBuilder(CodeAccess codeAccess, String compileTargetVersion) {
    this.codeAccess = codeAccess;
    this.compileTargetVersion = compileTargetVersion;
  }


  public void build(ComponentCodeChange modifiedComponent, String version) throws Ex_FileAccessException,
      XPRC_CompileError {
    prepare(modifiedComponent);
    compile(modifiedComponent);    
    FileUpdate[] modifiedfiles = createFilesForDeployment(modifiedComponent, version);
    if (modifiedfiles != null) {
      copyModifiedFilesToSaved(modifiedfiles, modifiedComponent);
    }
  }


  private void prepare(ComponentCodeChange modifiedComponent) throws Ex_FileAccessException {
    // TODO exception klassen generieren? erstmal nicht so wichtig, kann der benutzer einchecken? -> müsste er eigtl nicht!
    
    // nicht mehr benötigte classfiles entfernen
    for (RepositoryItemModification mod : modifiedComponent.getModifiedJavaFiles()) {
      if (mod.getModification() == RepositoryModificationType.Deleted) {
        //TODO
      }
    }
    
    //falls serviceimpl -> interfaces jar entpacken.
    if (modifiedComponent.getComponentType() == ComponentType.CODED_SERVICE) {
      try {
        if (modifiedComponent.getParsedDOMFromSaved(codeAccess.getRevision()).hasJavaImpl(false, null)) {
          //servicedefinition jar zu den classfiles hinzufügen
          FileUtils.unzip(codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath() + Constants.fileSeparator
              + ServiceImplementationTemplate.SERVICE_DEFINITION_JAR, getClassDir(modifiedComponent), new FileFilter() {

                public boolean accept(File f) {
                  if (f.isDirectory()) {
                    return true;
                  }
                  //METAINF nicht mit kopieren
                  if (f.getName().endsWith(".class")) {
                    return true;
                  }
                  return false;
                }
              });
        }
      } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void copyModifiedFilesToSaved(FileUpdate[] files, ComponentCodeChange modifiedComponent)
      throws Ex_FileAccessException {
    File targetDir = getSavedDir(modifiedComponent);
    for (int i = 0; i < files.length; i++) {
      File f = files[i].getFile();
      File target = new File(targetDir, f.getName());
      if (files[i].getUpdateType() == ModificationType.Deleted) {
        FileUtils.deleteFileWithRetries(target);
      } else {
        if (!target.exists()) {
          target.getParentFile().mkdirs();
          try {
            target.createNewFile();
          } catch (IOException e) {
            throw new Ex_FileAccessException(target.getAbsolutePath(), e);
          }
        }
        FileUtils.copyFile(f, target);
      }
    }
  }


  /**
   * @return verzeichnis, in dem die files liegen
   */
  public File getSavedDir(ComponentCodeChange modifiedComponent) {
    return getDir(modifiedComponent, codeAccess.getRevision(), false);
  }
  
  public File getDir(ComponentCodeChange modifiedComponent, long revision, boolean fromDeployed) {
    if (modifiedComponent.getComponentType() == ComponentType.GLOBAL_LIB) {
      return new File(SERVER_LIB);
    } else if (modifiedComponent.getComponentType() == ComponentType.USER_LIB) {
      return new File(USER_LIB);
    }
    String path =
        RevisionManagement.getPathForRevision(transformModificationTypeToPathType(modifiedComponent.getComponentType()),
                                              revision, fromDeployed);
    path +=
        (modifiedComponent.getComponentType() != ComponentType.SHARED_LIB ? Constants.fileSeparator : "")
            + modifiedComponent.getComponentPathName();
    
    return new File(path);
  }


  private PathType transformModificationTypeToPathType(ComponentType componentType) {
    switch (componentType) {
      case CODED_SERVICE :
        return PathType.SERVICE;
      case FILTER :
        return PathType.FILTER;
      case TRIGGER :
        return PathType.TRIGGER;
      case SHARED_LIB :
        return PathType.SHAREDLIB;
      default :
        throw new RuntimeException("unsupported componenttype: " + componentType);
    }
  }


  /**
   * erstellt und gibt die liste aller artefakte (jars, xmls, etc) zurück, die kopiert werden müssen, weil sie sich geändert haben
   */
  private FileUpdate[] createFilesForDeployment(ComponentCodeChange modifiedComponent, String version)
      throws Ex_FileAccessException {
    List<FileUpdate> updates = new ArrayList<FileUpdate>();
    if (modifiedComponent.getComponentType() == ComponentType.SHARED_LIB) {
      updates.addAll(modifiedComponent.getModifiedJars());
      for (ComponentCodeChange modifiedSharedLibJar : modifiedComponent.changedSubComponent) {
        if (modifiedSharedLibJar.getModType() == ModificationType.Modified) {
          String fileName = modifiedSharedLibJar.getComponentOriginalName() + ".jar";
          File jarFile =
              new File(codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath(), fileName);
          File inputDir = new File(getSubComponentClassDir(modifiedComponent, modifiedSharedLibJar.getComponentOriginalName()));
          
          //Exceptions.xml ins classdir kopieren, weil das als resource mit ins jar soll
          copyExceptionsXml(modifiedComponent, inputDir);
          
          Support4Eclipse.createJarFile(createManifest(version), jarFile, inputDir, true);
          updates.add(new FileUpdate(jarFile, ModificationType.Modified));
        } else {
          //TODO delete
          throw new RuntimeException("unsupported modification type: " + modifiedSharedLibJar.getModificationType());
        }
      }
    } else if (modifiedComponent.getComponentType() == ComponentType.GLOBAL_LIB) {
      return null; //FIXME
    } else if (modifiedComponent.getComponentType() == ComponentType.USER_LIB) {
      return null; //FIXME
    } else {
      String fileName;
      if (modifiedComponent.getComponentType() == ComponentType.CODED_SERVICE) {
        fileName = GenerationBase.getSimpleNameFromFQName(modifiedComponent.getComponentPathName()) + "Impl";
      } else {
        fileName = modifiedComponent.getComponentOriginalName();
      }
      fileName += ".jar";
      File jarFile =
          new File(codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
              + Constants.fileSeparator + "deploy", fileName);
      File inputDir = new File(getClassDir(modifiedComponent));
      
      //Exceptions.xml ins classdir kopieren, weil das als resource mit ins jar soll
      copyExceptionsXml(modifiedComponent, inputDir);
      
      Support4Eclipse.createJarFile(createManifest(version), jarFile, inputDir, true);
      updates.add(new FileUpdate(jarFile, ModificationType.Modified));

      //wurden noch andere verwendete jars geändert?
      for (FileUpdate fu : modifiedComponent.getModifiedJars()) {
        updates.add(fu);
      }
    }
    return updates.toArray(new FileUpdate[updates.size()]);
  }


  private void copyExceptionsXml(ComponentCodeChange modifiedComponent, File targetDir) throws Ex_FileAccessException {
    File exceptionsFile = new File(codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
                        + Constants.fileSeparator, "Exceptions.xml");
    if (exceptionsFile.exists()) {
      FileUtils.copyRecursively(exceptionsFile, targetDir);
    }
  }
  
  public Manifest createManifest(String version) {
    Manifest manifest = new Manifest();
    if (version != null) {
      manifest.getMainAttributes().putValue("SVN-Revision", version);
    }
    manifest.getMainAttributes().putValue("Xyna", factoryVersion);
    //TODO projekt-name? factory-identifikation?
    return manifest;
  }


  private void compile(ComponentCodeChange modifiedComponent) throws XPRC_CompileError {
    if (modifiedComponent.getComponentType() == ComponentType.SHARED_LIB) {
      for (ComponentCodeChange modifiedSharedLibJar : modifiedComponent.changedSubComponent) {
        if (modifiedSharedLibJar.getModType() == ModificationType.Modified) {
          String[] javaFilesToCompile = getFilesToCompile(modifiedSharedLibJar);
          if (javaFilesToCompile.length == 0) {
            continue;
          }
          
          String classPath = getClassPathForCompile(modifiedSharedLibJar);
          String classDir = getSubComponentClassDir(modifiedComponent, modifiedSharedLibJar.getComponentOriginalName());
          String sourcePath = getSubComponentSourcePath(modifiedSharedLibJar, modifiedSharedLibJar.getComponentOriginalName());
          File classDirFile = new File(classDir);
          if (!classDirFile.exists()) {
            classDirFile.mkdir();
          }
          compile(modifiedSharedLibJar.getComponentOriginalName(),
                  javaFilesToCompile, classPath, classDir, sourcePath);
        } else {
          //TODO delete
          throw new RuntimeException("unsupported modification type: " + modifiedSharedLibJar.getModificationType());
        }
      }
    } else if (modifiedComponent.getComponentType() == ComponentType.GLOBAL_LIB) {
      return;
    } else if (modifiedComponent.getComponentType() == ComponentType.USER_LIB) {
      return;
    } else {
      //filter, trigger, service  
      String[] javaFilesToCompile = getFilesToCompile(modifiedComponent);
      if (javaFilesToCompile.length == 0) {
        return;
      }
      String classPath = getClassPathForCompile(modifiedComponent);
      String classDir = getClassDir(modifiedComponent);
      String sourcePath = getSourcePath(modifiedComponent);
      File classDirFile = new File(classDir);
      if (!classDirFile.exists()) {
        classDirFile.mkdir();
      }
      compile(modifiedComponent.getComponentOriginalName(), javaFilesToCompile, classPath, classDir, sourcePath);
    }
  }
  
  
  protected void compile(String identifier, String[] javaFiles, String classPath, String classDir, String sourcePath) throws XPRC_CompileError {
    GenerationBase.compileJavaFiles(identifier, javaFiles, classPath, classDir, sourcePath, compileTargetVersion != null, compileTargetVersion, true);
  }


  private String[] getFilesToCompile(ComponentCodeChange modifiedComponent) {
    List<String> result = new ArrayList<String>();
    for (RepositoryItemModification mod : modifiedComponent.getModifiedJavaFiles()) {
      if (mod.getModification() != RepositoryModificationType.Deleted) {
        result.add(mod.getFile());
      }
    }
    return result.toArray(new String[result.size()]);
  }


  private String getSourcePath(ComponentCodeChange modifiedComponent) {
    return codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
        + Constants.fileSeparator + "src";
  }

  private String getSubComponentSourcePath(ComponentCodeChange modifiedComponent, String subComponentName) {
    return codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
        + Constants.fileSeparator + subComponentName + Constants.fileSeparator + "src";
  }


  private String getClassDir(ComponentCodeChange modifiedComponent) {
    return codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
        + Constants.fileSeparator + "bin";
  }

  private String getSubComponentClassDir(ComponentCodeChange modifiedComponent, String subComponentName) {
    return codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath()
        + Constants.fileSeparator + subComponentName + Constants.fileSeparator + "bin";
  }


  private String getClassPathForCompile(ComponentCodeChange modifiedComponent) {
    // abhängige jars ermitteln (shared libs, etc)
    // jars verwenden, die evtl vorher gebaut wurden, aber noch nicht deployed sind -> d.h. sie liegen im saved-verzeichnis.

    StringBuilder classPath = new StringBuilder();
    appendJarsServerLib(classPath);
    appendJarsUserLib(classPath);

    switch (modifiedComponent.getComponentType()) {
      case FILTER :
        try {
          Filter f = modifiedComponent.getFilter(codeAccess.getRevision());
          appendClassPathForSharedLibs(classPath, f.getSharedLibs());
          //jars, die der filter verwendet
          //TODO jars aus repository nehmen?
          appendClassPathForFilterJars(classPath, f, modifiedComponent);
          Trigger t = f.getTrigger();
          //triggerimpl.jar
          appendClassPathForTriggerImplJar(classPath, t);
          //shared libs vom trigger
          appendClassPathForSharedLibs(classPath, t.getSharedLibs());
          //weitere trigger.jars
          appendClassPathForTriggerJars(classPath, t);
          appendClassPathForMdmClasses(classPath);
        } catch (ComponentNotRegistered e) {
          // try to discover trigger-jar
          String libPath = codeAccess.getProjectDir() + File.separatorChar + modifiedComponent.getBasePath() + File.separatorChar + "lib" + File.separatorChar + "xyna";
          File libFolder = new File(libPath);
          for (File file : libFolder.listFiles(jarFilter)) {
            classPath.append(Constants.PATH_SEPARATOR);
            classPath.append(file.getPath());
          }
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
        break;
      case TRIGGER :
        try {
          Trigger t = modifiedComponent.getTrigger(codeAccess.getRevision());
  
          //shared libs vom trigger
          appendClassPathForSharedLibs(classPath, t.getSharedLibs());
          //weitere trigger.jars
          appendClassPathForTriggerJars(classPath, t);
        } catch (ComponentNotRegistered e) {
          // ntbd
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
        break;
      case CODED_SERVICE :
        try {
          DOM dom = modifiedComponent.getParsedDOMFromSaved(codeAccess.getRevision());
          appendClassPathForSharedLibs(classPath, dom.getSharedLibs());
          //additionalLibs wurden noch nicht ins saved-Verzeichnis kopiert,
          //daher aus repository nehmen
          appendClassPathForAdditionalLibs(classPath, dom.getAdditionalLibraries(), dom.getOriginalSimpleName(),
                                           modifiedComponent, true);

          if (dom.hasSuperTypeWithInstanceMethods(null)) {
            //evtl leitet der datentyp von anderen datentypen ab, die benötigt man auch als jars
            boolean first = true;
            for (DOM superType : dom.getDOMHierarchy()) {
              if (first) {
                first = false;
                continue;
              }
              appendClassPathForOtherCodedService(classPath, superType);
            }
          }
          
          //interfaces.jar
          classPath.append(Constants.PATH_SEPARATOR).append(getClassDir(modifiedComponent));
          appendClassPathForMdmClasses(classPath);
          //FIXME fehlerbehandlung
        } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException
            | XPRC_MDMDeploymentException | XPRC_JarFileForServiceImplNotFoundException | XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);
        }
        break;
      case SHARED_LIB :
        String libPath = codeAccess.getProjectDir() + File.separatorChar + modifiedComponent.getBasePath();
        File libFolder = new File(libPath);
        for (File file : libFolder.listFiles(jarFilter)) {
          if (file.getName().equals(modifiedComponent.getComponentOriginalName() + ".jar")) {
            continue;
          }
          classPath.append(Constants.PATH_SEPARATOR);
          classPath.append(file.getPath());
        }
        break;
      default :
        throw new RuntimeException("unsupported type: " + modifiedComponent.getComponentType());
    }
  
    return classPath.toString();
  }


  private void appendClassPathForOtherCodedService(StringBuilder classPath, DOM dom)
      throws XPRC_JarFileForServiceImplNotFoundException, XFMG_SHARED_LIB_NOT_FOUND {
    //TODO: falls in der gleichen revision und auch im repository vorhanden (beides checken!), dann von dort nehmen? 
    //was hätte das für vorteile/nachteile gegenüber aus saved/deployed nehmen? wenn es compilefähig ist, sollte es identisch sein
    //ansonsten?
    Set<String> jars = new HashSet<>();
    boolean fromSaved = false;
    dom.getDependentJarsWithoutRecursion(jars, true, fromSaved);
    for (String jar : jars) {
      classPath.append(Constants.PATH_SEPARATOR);
      classPath.append(jar);
    }
  }


  private void appendClassPathForMdmClasses(StringBuilder classPath) {
    classPath.append(Constants.PATH_SEPARATOR);
    classPath.append(codeAccess.getProjectDir() + Constants.fileSeparator + codeAccess.getRelativePathOfMdmJar());
  }


  /**
   * exklusive dem serviceimpl jar
   */
  private void appendClassPathForAdditionalLibs(StringBuilder classPath, Set<String> additionalLibraries,
                                                String serviceName, ComponentCodeChange modifiedComponent,
                                                boolean fromRepository) {
    if (additionalLibraries.size() > 1) {
      File savedDir;
      if (fromRepository) {
        String libPath = codeAccess.getProjectDir() + Constants.fileSeparator + modifiedComponent.getBasePath() + Constants.fileSeparator + "lib";
        savedDir = new File(libPath);
      } else {
        savedDir = getSavedDir(modifiedComponent);
      }
      
      for (String jar : additionalLibraries) {
        if (jar.equals(serviceName + "Impl.jar")) {
          continue;
        }
        classPath.append(Constants.PATH_SEPARATOR);
        classPath.append(savedDir.getPath()).append(Constants.fileSeparator).append(jar);
      }
    }
    //else nur das impl jar verwendet
  }


  protected void appendJarsUserLib(StringBuilder classPath) {
    //TODO caching?
    String[] jarNames = GenerationBase.getJarFileNamesFromFolder(USER_LIB);
    for (String jar : jarNames) {
      classPath.append(Constants.PATH_SEPARATOR);
      classPath.append(USER_LIB).append(Constants.fileSeparator).append(jar);
    }
  }


  protected void appendJarsServerLib(StringBuilder classPath) {
    //TODO caching?
    String[] jarNames = GenerationBase.getJarFileNamesFromFolder(SERVER_LIB);
    for (String jar : jarNames) {
      classPath.append(Constants.PATH_SEPARATOR);
      classPath.append(SERVER_LIB).append(Constants.fileSeparator).append(jar);
    }
    if (Constants.RUNS_FROM_SOURCE) {
      classPath.append(Constants.PATH_SEPARATOR);
      classPath.append(Constants.SERVER_CLASS_DIR);
    }
  }


  /**
   * exklusive filterimpl.jar
   */
  private void appendClassPathForFilterJars(StringBuilder classPath, Filter f, ComponentCodeChange filterComponent) {
    if (f.getJarFiles().length > 1) {
      File savedDir = getSavedDir(filterComponent);
      for (File jar : savedDir.listFiles(jarFilter)) {
        if (jar.getName().equals(f.getName() + ".jar")) {
          continue;
        }
        classPath.append(Constants.PATH_SEPARATOR);
        classPath.append(jar.getPath());
      }
    }
  }


  /**
   * exklusive triggerimpl.jar
   */
  private void appendClassPathForTriggerJars(StringBuilder classPath, Trigger t) {
    if (t.getJarFiles().length > 1) {
      File savedDir = getSavedDir(createTriggerComponent(t.getTriggerName()));
      for (File jar : savedDir.listFiles(jarFilter)) {
        if (jar.getName().equals(t.getTriggerName() + ".jar")) {
          continue;
        }
        classPath.append(Constants.PATH_SEPARATOR);
        classPath.append(jar.getPath());
      }
    }
  }


  private void appendClassPathForTriggerImplJar(StringBuilder classPath, Trigger t) {
    File savedDir = getSavedDir(createTriggerComponent(t.getTriggerName()));
    classPath.append(Constants.PATH_SEPARATOR);
    classPath.append(savedDir.getPath()).append(Constants.fileSeparator).append(t.getTriggerName()).append(".jar");
  }


  private void appendClassPathForSharedLibs(StringBuilder classPath, String[] sharedLibs) {
    for (String sharedLibName : sharedLibs) {
      File sharedLibFolder = resolveSharedLibFolder(sharedLibName);
      if (sharedLibFolder != null) {
        for (File jar : sharedLibFolder.listFiles(jarFilter)) {
          classPath.append(Constants.PATH_SEPARATOR);
          classPath.append(jar.getPath());
        }
      }
    }
  }

  private File resolveSharedLibFolder(String sharedLibName) {
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<Long> allRelevantRevisions = new HashSet<Long>();
    rcdm.getDependenciesRecursivly(codeAccess.getRevision(), allRelevantRevisions);
    
    ComponentCodeChange sharedLibComponent = createSharedLibComponent(sharedLibName);
    
    for (Long revision : allRelevantRevisions) {
      File folder = getDir(sharedLibComponent, revision, false);
      if (folder.exists()) {
        return folder;
      }
      folder = getDir(sharedLibComponent, revision, true);
      if (folder.exists()) {
        return folder;
      }
    }
    
    return null;
  }

  private ComponentCodeChange createTriggerComponent(String triggerName) {
    return new ComponentCodeChange(triggerName, ComponentType.TRIGGER);
  }


  private ComponentCodeChange createSharedLibComponent(String sharedLibName) {
    return new ComponentCodeChange(sharedLibName, ComponentType.SHARED_LIB);
  }
  
}
