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
package com.gip.xyna.xdev.xlibdev.supp4eclipse;



import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.exceptions.XDEV_ProjectTemplateTemplateCouldNotBeAccessedException;
import com.gip.xyna.xdev.exceptions.XDEV_ProjectTemplateZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ServiceImplementationTemplate;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class Support4Eclipse extends FunctionGroup {

  private static final String PROJECT_FOLDER = "TemplateImplKind" + File.separator + "TemplateImplName";
  public static final String SOURCE_FOLDER = "src";
  public static final String XML_DEFINITION_FOLDER = "xmldefinition";
  public static final String COMMON_FOLDER = "common";
  private static final String SERVER_FOLDER = "server";
  private static final String COMMON_LIB_XYNA_FOLDER = "common" + File.separator + "lib" + File.separator + "xyna";
  public static final String PROJECT_LIB_XYNA_FOLDER = "lib" + File.separator + "xyna";

  private static final File TEMPLATEIMPL_FILE = new File("TemplateImpl.zip");
  private static final File TEMPLATEIMPL_NEW_FILE = new File("TemplateImplNew.zip");
  public static final String FILENAME_MDM_JAR = "mdm.jar";
  public static final String FILENAME_SERVICEDEFINITION_JAR = "serviceDefinition.jar";
  public static final String FILENAME_SERVICEDEFINITION_JAVADOC_JAR = "serviceDefinition-javadoc.jar";
  public static final String TRIGGER_LOCATION = "com.gip.xyna.xact.trigger";
  public static final String FILTER_LOCATION = "com.gip.xyna.xact.filter";

  public static final String IMPL_JAVA = "Impl.java";

  public static final String BUILD_XML = "build.xml";
  public static final String SERVER_PROPERTIES = "server.properties";

  public static final String TEMPLATE_SERVER_MDM_PATH = "TemplateServerMDMPath";
  public static final String TEMPLATE_SERVER_MDM_XML_PATH = "TemplateServerMdmXmlPath";
  public static final String TEMPLATE_SERVER_MDM_XML_FILE_NAME = "TemplateServerMdmXmlFileName";
  public static final String TEMPLATE_FQ_CLASSNAME = "TemplateFQClassname";
  public static final String TEMPLATE_WORKSPACE_NAME = "TemplateWorkspaceName";
  public static final String TEMPLATE_REVISION_DIR = "TemplateRevisionDir";
  private static final String TEMPLATE_SERVER_HOST = "TemplateServerHost";
  private static final String TEMPLATE_SERVER_USER_ID = "TemplateServerUserID";
  private static final String TEMPLATE_SERVER_PATH = "TemplateServerPath";
  private static final String TEMPLATE_BUILD_SCRIPT_NAME = "TemplateBuildScriptName";

  private static Logger logger = CentralFactoryLogging.getLogger(Support4Eclipse.class);

  private static final XynaPropertyBoolean XYNAPROPERTY_INCLUDE_WFS_IN_MDM_JARS =
      new XynaPropertyBoolean("xdev.xlibdev.buildmdmjar.includewfs", false)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "'true' if workflow classes should be included in mdm.jar, 'false' if that is not necessary. It may contain them anyway.");

  private static ReentrantLock newBuildLock = new ReentrantLock();


  public static final String DEFAULT_NAME = "Support 4 Eclipse";


  public Support4Eclipse() throws XynaException {
    super();
  }


  public void init() throws XynaException {
    XynaProperty.BUILDMDJAR_JAVA_VERSION.registerDependency(DEFAULT_NAME);
    XYNAPROPERTY_INCLUDE_WFS_IN_MDM_JARS.registerDependency(DEFAULT_NAME);
  }
  

  public void shutdown() throws XynaException {

  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  private static HashSet<String> includeClasses = new HashSet<String>();
  static {
    String ext = ".class";
    includeClasses.add(XynaObject.class.getSimpleName() + ext);
    includeClasses.add(GeneralXynaObject.class.getSimpleName() + ext);
    includeClasses.add(XynaObjectList.class.getSimpleName() + ext);
    includeClasses.add(Container.class.getSimpleName() + ext);
    includeClasses.add(TriggerConnection.class.getSimpleName() + ext);
    includeClasses.add(StartParameter.class.getSimpleName() + ext);
    includeClasses.add(EventListener.class.getSimpleName() + ext);
    includeClasses.add(ConnectionFilter.class.getSimpleName() + ext);
    includeClasses.add(CentralFactoryLogging.class.getSimpleName() + ext);
    includeClasses.add(XynaOrder.class.getSimpleName() + ext);
    includeClasses.add(XMLUtils.class.getSimpleName() + ext);
    includeClasses.add(DestinationKey.class.getSimpleName() + ext);

    includeClasses.add(ResponseListener.class.getSimpleName() + ext);
    includeClasses.add(OrderContext.class.getSimpleName() + ext);
    includeClasses.add(ConnectionFilterInstance.class.getSimpleName() + ext);
  }


  /**
   * setzt in manifest Build-Date und Created-By
   */
  public static void createJarFile(Manifest mf, File jarFile, File inputDir, boolean compressed) throws Ex_FileAccessException {
    mf.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
    mf.getMainAttributes().putValue("Build-Date", Constants.defaultUTCSimpleDateFormat().format(new Date()));

    String javaInformation = System.getProperty("java.vm.version") + " (" + System.getProperty("java.vm.vendor") + ")";
    mf.getMainAttributes().putValue("Created-By", javaInformation);
    try {
      jarFile.getParentFile().mkdirs();
      JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
      try {
        if (!compressed) {
          jos.setLevel(Deflater.NO_COMPRESSION);
        }
        //manifest erst nach der compressionänderung schreiben
        ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
        jos.putNextEntry(e);
        mf.write(new BufferedOutputStream(jos));
        jos.closeEntry();
        FileUtils.zipDir(inputDir, jos, inputDir);
      } finally {
        jos.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(jarFile.getPath());
    }
  }


  /**
   * erzeugt ein jarfile mit mdmobjekten
   * @param targetDir wird erstellt, falls nicht vorhanden
   */
  public static List<Throwable> buildMDMJarFile(File targetDir, Long revision) throws Ex_FileAccessException {
    logger.debug("Building Xyna jar file: Gathering Manifest information.");
    newBuildLock.lock();

    try {
      // erstelle mdm-jar
      Manifest mf = new Manifest();
      // Manifest-Version: 1.0
      // Ant-Version: Apache Ant 1.7.0
      // Created-By: 1.5.0-b64 (Sun Microsystems Inc.)
      // Vendor: GIP AG
      // Version: 2.3.0.0
      // Build-Date: 20090902_1515

      // it is important to set Manifest-Version, otherwise no MANIFEST.MF is created
      mf.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
      mf.getMainAttributes().putValue("Build-Date", Constants.defaultUTCSimpleDateFormat().format(new Date()));

      String javaInformation =
          System.getProperty("java.vm.version") + " (" + System.getProperty("java.vm.vendor") + ")";
      mf.getMainAttributes().putValue("Created-By", javaInformation);

      try {
        mf.getMainAttributes().putValue(Attributes.Name.IMPLEMENTATION_VERSION.toString(),
                                        Updater.getInstance().getFactoryVersion().getString());
      } catch (XPRC_VERSION_DETECTION_PROBLEM e1) {
        throw new RuntimeException("Error determining " + Constants.FACTORY_NAME + " version.", e1);
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException("Error determining " + Constants.FACTORY_NAME + " version.", e1);
      }

      // TODO make sure what fields have to be in here
      //    mf.getMainAttributes().putValue(Attributes.Name.IMPLEMENTATION_TITLE.toString(), Constants.FACTORY_NAME);
      //    mf.getMainAttributes().putValue(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "GIP AG");

      logger.debug("Adding files to Xyna jar file.");

      List<Throwable> exceptions = new ArrayList<Throwable>();

      File f = new File(targetDir, FILENAME_MDM_JAR);
      try {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
          throw new Ex_FileAccessException(f.getAbsolutePath());
        }

        //das wird in GenerationBase.generateJava als class-outputdir auf die javasources konfiguriert
        String xmomClassDir = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision) + ".tmp";
        File tmpDir = new File(xmomClassDir);

        if (tmpDir.exists()) {
          FileUtils.deleteDirectoryRecursively(tmpDir);
        }

        tmpDir.mkdirs();

        JarOutputStream jos = new JarOutputStream(new FileOutputStream(f), mf);
        try {

          //holt absichtlich nicht den default. wenn property nicht gesetzt ist, soll das feature nicht verwendet werden
          String javaVersion = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.BUILDMDJAR_JAVA_VERSION.getPropertyName());

          if (javaVersion == null) {
            xmomClassDir = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision);
            FileUtils.zipDir(new File(xmomClassDir), jos, new File(xmomClassDir));
          } else if (javaVersion.equals("Java5") || javaVersion.equals("Java6") || javaVersion.equals("Java7") || javaVersion.equals("Java11")) {
            List<GenerationBase> objects = new ArrayList<GenerationBase>();
            GenerationBaseCache cache = new GenerationBaseCache();
            Set<DependencyNode> doms =
                XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
                    .getDependencyNodesByType(DependencySourceType.DATATYPE, revision);

            if (doms != null && doms.size() > 0) {
              for (DependencyNode depNode : doms) {
                try {
                  DOM dom =
                      DOM.getOrCreateInstance(depNode.getUniqueName(), cache,
                                                   revision);
                  objects.add(dom);
                } catch (XynaException e) {
                  exceptions.add(new RuntimeException(e));
                  logger.error(e);
                } catch (RuntimeException e) {
                  exceptions.add(e);
                  logger.error(e);
                }
              }
            }

            doms =
                XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
                    .getDependencyNodesByType(DependencySourceType.XYNAEXCEPTION, revision);

            if (doms != null && doms.size() > 0) {
              for (DependencyNode depNode : doms) {
                try {
                  ExceptionGeneration excep = ExceptionGeneration.getOrCreateInstance(depNode.getUniqueName(), cache, revision);
                  objects.add(excep);
                } catch (XynaException e) {
                  exceptions.add(new RuntimeException(e));
                  logger.error(e);
                } catch (RuntimeException e) {
                  exceptions.add(e);
                  logger.error(e);
                }
              }
            }

            if (XYNAPROPERTY_INCLUDE_WFS_IN_MDM_JARS.get()) {
              doms =
                  XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
                      .getDependencyNodesByType(DependencySourceType.WORKFLOW, revision);

              if (doms != null && doms.size() > 0) {
                for (DependencyNode depNode : doms) {
                  try {
                    WF wf = WF.getOrCreateInstance(depNode.getUniqueName(), cache, revision);
                    objects.add(wf);
                  } catch (XynaException e) {
                    exceptions.add(new RuntimeException(e));
                    logger.error(e);
                  } catch (RuntimeException e) {
                    exceptions.add(e);
                    logger.error(e);
                  }
                }
              }
            }
            
            for (GenerationBase o : objects) {
              o.setDeploymentComment("Build mdm jar");
            }
            try {
              GenerationBase.deploy(objects, DeploymentMode.generateMdmJar, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
            } catch (MDMParallelDeploymentException e) {
              for (GenerationBase gb : e.getFailedObjects()) {
                exceptions.add(gb.getExceptionCause());
              }
              logger.error(e);
            } catch (XynaException e) {
              exceptions.add(new RuntimeException(e));
              logger.error(e);
            } catch (RuntimeException e) {
              exceptions.add(e);
              logger.error(e);
            }
            
            FileUtils.zipDir(tmpDir, jos, tmpDir);
          } else {
            RuntimeException e = new RuntimeException("Unknown mdm target java version : " + javaVersion);
            exceptions.add(e);
            logger.error(e);
          }
          jos.flush();
        } finally {
          try {
            jos.close();
          } finally {
            FileUtils.deleteDirectoryRecursively(tmpDir);
          }
        }
      } catch (IOException e) {
        throw new Ex_FileAccessException(f.getAbsolutePath(), e);
      }

      logger.debug("Xyna jar file successfully built.");
      return exceptions;
    } finally {
      newBuildLock.unlock();
    }
  }
  public static void buildProjectTemplate(File projectLocationDirectory, ImplementationTemplate paras)
      throws XDEV_InvalidProjectTemplateParametersException, Ex_FileAccessException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_XmlParsingException {
    buildProjectTemplate(projectLocationDirectory, paras, true);
  }

  /**
   * erzeugt ein project template für die entwicklung im angegebenen verzeichnis
   * @param projectLocationDirectory
   * @param paras
   * @param legacy false: abgespecktes eclipse projekt ohne deployment-ant kram und ohne mdm jar
   */
  public static void buildProjectTemplate(File projectLocationDirectory, ImplementationTemplate paras, boolean legacy)
      throws XDEV_InvalidProjectTemplateParametersException, Ex_FileAccessException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_XmlParsingException {

    //alle io probleme beim lesen sind wohl fehler mit dem templateimpl-zip, die werden als runtimeexceptions geworfen, weil davon ausgegangen wird,
    //dass die installation das korrekt initialisiert.
    //alle io probleme beim schreiben können durch filesystem probleme verursacht sein (kein platz, keine rechte etc) und werden als checked exception propagiert

    //TODO refactoring: idee wie man die unterschiedlichen io exceptions zuordnen könnte: reader/writer überschreiben, so dass unterschiedliche exceptions geworfen werden,
    //dann benötigt man nur jeweils einen catchblock

    // unzip template
    File templateZip = TEMPLATEIMPL_NEW_FILE;
    if (legacy) {
      templateZip = TEMPLATEIMPL_FILE;
    }

    ZipInputStream in;
    try {
      in = new ZipInputStream(new FileInputStream(templateZip));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(
                                 new XDEV_ProjectTemplateTemplateCouldNotBeAccessedException(templateZip
                                     .getAbsolutePath(), e));
    }
    InputStreamReader isw = new InputStreamReader(in);
    BufferedReader br = new BufferedReader(isw);
    try {
      try {
        ZipEntry entry;
        try {
          entry = in.getNextEntry();
        } catch (IOException e1) {
          throw new RuntimeException(
                                     new XDEV_ProjectTemplateTemplateCouldNotBeAccessedException(templateZip
                                         .getAbsolutePath(), e1));
        }
        while (entry != null) {

          if (entry.isDirectory()) {
            //next entry
            try {
              entry = in.getNextEntry();
            } catch (IOException e1) {
              throw new RuntimeException(
                                         new XDEV_ProjectTemplateTemplateCouldNotBeAccessedException(templateZip
                                             .getAbsolutePath(), e1));
            }
            continue;
          }
          File f = new File(projectLocationDirectory, entry.getName());
          if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
              f.createNewFile();
            } catch (IOException e) {
              throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(f.getAbsolutePath(),
                                                                              paras.getProjectName(), e);
            }
          }
          // special treatment for jar files that will just be copied and wont be changed
          if (f.getName().endsWith(".jar")) {
            int size;
            byte[] buffer = new byte[2048];
            FileOutputStream out;
            try {
              out = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
              //wurde ja gerade erst erstellt!
              throw new RuntimeException(
                                         new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(
                                                                                                   f.getAbsolutePath(),
                                                                                                   paras
                                                                                                       .getProjectName(),
                                                                                                   e));
            }
            BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
            try {
              try {
                while ((size = in.read(buffer, 0, buffer.length)) >= 0) {
                  bufferedOut.write(buffer, 0, size);
                }
                bufferedOut.flush();
              } finally {
                bufferedOut.close();
              }
            } catch (IOException e) {
              throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(f.getAbsolutePath(), paras.getProjectName(), e);
            }
          } else {
            FileOutputStream out;
            try {
              out = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
              //wurde ja gerade erst erstellt!
              throw new RuntimeException(
                                         new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(
                                                                                                   f.getAbsolutePath(),
                                                                                                   paras
                                                                                                       .getProjectName(),
                                                                                                   e));
            }
            OutputStreamWriter osw = new OutputStreamWriter(out); // stream => writer
            BufferedWriter bw = new BufferedWriter(osw);
            try {
              String line = null;
              while ((line = br.readLine()) != null) {
                line = line.replaceAll("TemplateImplName(New)?", paras.getProjectName());
                line = line.replaceAll("TemplateImplKind", paras.getProjectKindFolder());

                // update some lines within the build.xml file; more changes will be performed
                // using XML logic
                if (f.getName().equals(BUILD_XML)) {
                  line = updateBuildXmlDeployTarget(projectLocationDirectory, paras, line);
                }

                if (f.getName().equals(SERVER_PROPERTIES)) {
                  line = updateServerProperties(line);
                }

                if (line.equals("TemplateImplLib")) {
                  if (f.getName().equals(".classpath")) {
                    try {
                      paras.writeToClasspathFile(bw, legacy);
                    } catch (IOException e) {
                      throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(f.getAbsolutePath(),
                                                                                      paras.getProjectName(), e);
                    }
                  } else if (f.getName().endsWith(".jpr")) {
                    // TODO
                  }
                } else {
                  bw.write(line + "\n");
                }
              }
            } finally {
              bw.flush();
              bw.close();
            }
          }
          //next entry
          try {
            entry = in.getNextEntry();
          } catch (IOException e1) {
            throw new RuntimeException(
                                       new XDEV_ProjectTemplateTemplateCouldNotBeAccessedException(templateZip
                                           .getAbsolutePath(), e1));
          }
        }
      } finally {
        br.close();
      }
    } catch (IOException e) {
      //probleme beim lesen von templateimpl
      throw new RuntimeException(
                                 new XDEV_ProjectTemplateTemplateCouldNotBeAccessedException(templateZip
                                     .getAbsolutePath(), e));
    }

    try {
      File projectDir = new File(projectLocationDirectory, "TemplateImplNameNew");
      if (legacy) {
        projectDir = new File(projectLocationDirectory, PROJECT_FOLDER);
      }
      writeTemplateFiles(projectLocationDirectory, paras, legacy, projectDir);

      if (legacy) {
        buildMDMJarFileRecursively(new File(projectDir, PROJECT_LIB_XYNA_FOLDER),
                        paras.getRevision(), true, null);
      }
    } catch (Ex_FileAccessException e) {
      throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(e.getFileName(), paras.getProjectName(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(null, paras.getProjectName(), e);
    }


    // insert user input requests that parse required information from the user
    //paras.updateBuildXml(projectLocationDirectory);

    // rename folder to match impl-kind and project name
    if (legacy) {
      File buildScript =
          new File(projectLocationDirectory + File.separator + PROJECT_FOLDER + File.separator + "buildTemplate.xml");
      buildScript.renameTo(new File(projectLocationDirectory + File.separator + PROJECT_FOLDER + File.separator
          + paras.getBuildScriptName()));
      File projectFolder = new File(projectLocationDirectory + File.separator + PROJECT_FOLDER);
      projectFolder.renameTo(new File(projectLocationDirectory + File.separator + "TemplateImplKind" + File.separator
          + paras.getProjectName()));
      File typeFolder = new File(projectLocationDirectory + File.separator + "TemplateImplKind");
      typeFolder.renameTo(new File(projectLocationDirectory + File.separator + paras.getProjectKindFolder()));
    } else {
      File projectFolder = new File(projectLocationDirectory, "TemplateImplNameNew");
      File targetFolder = new File(projectLocationDirectory, paras.getBaseDirectoryName());
      if (targetFolder.exists()) {
        //dann hat man zuviel gemacht... wir brauchen nur die src-files und bei serviceimpls auch die jars
        FileUtils.copyRecursivelyWithFolderStructure(new File(projectFolder, SOURCE_FOLDER), new File(targetFolder, SOURCE_FOLDER));
        FileUtils.copyRecursivelyWithFolderStructure(new File(projectFolder, PROJECT_LIB_XYNA_FOLDER), new File(targetFolder, PROJECT_LIB_XYNA_FOLDER));
        if (paras instanceof ServiceImplementationTemplate) {
          FileUtils.copyRecursivelyWithFolderStructure(new File(projectFolder, XML_DEFINITION_FOLDER), new File(targetFolder, XML_DEFINITION_FOLDER));
        }
        FileUtils.deleteDirectoryRecursively(projectFolder);
      } else {
        projectFolder.renameTo(targetFolder);
      }
    }
  }


  private static String updateBuildXmlDeployTarget(File projectLocationDirectory, ImplementationTemplate paras,
                                                   String line) throws XDEV_InvalidProjectTemplateParametersException {
    // some generic replacements
    line = line.replaceAll(TEMPLATE_BUILD_SCRIPT_NAME, Matcher.quoteReplacement(paras.getBuildScriptName()));

    // specific replacements
    line = paras.updateBuildXmlDeployTarget(projectLocationDirectory, line);

    return line;

  }


  private static String updateServerProperties(String line) throws XDEV_InvalidProjectTemplateParametersException {
    // some generic replacements
    try {
      line = line.replaceAll(TEMPLATE_SERVER_HOST, Matcher.quoteReplacement(InetAddress.getLocalHost().getHostName()));
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    line = line.replaceAll(TEMPLATE_SERVER_USER_ID, Matcher.quoteReplacement(System.getProperty("user.name")));
    line = line.replaceAll(TEMPLATE_SERVER_PATH, Matcher.quoteReplacement(System.getProperty("user.dir")));

    return line;
  }


  private static void writeTemplateFiles(File projectLocationDirectory, ImplementationTemplate paras,
                                         boolean legacy, File projectDir)
      throws XDEV_InvalidProjectTemplateParametersException, Ex_FileAccessException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_XmlParsingException {
    paras.writeTemplateFiles(projectDir, legacy);

    if (legacy) {
      // copy common libraries
      FileUtils.copyRecursively(new File("lib"), new File(projectLocationDirectory + File.separator
          + COMMON_LIB_XYNA_FOLDER));
      FileUtils.copyRecursively(new File("exceptions"), new File(projectLocationDirectory + File.separator
          + SERVER_FOLDER + File.separator + "exceptions"));
      FileUtils.copyFile(new File("Exceptions.xml"), new File(projectLocationDirectory + File.separator + SERVER_FOLDER
          + File.separator + "Exceptions.xml"));
      File commonSrc = new File(projectLocationDirectory + File.separator + "common" + File.separator + "src");

      if (!commonSrc.exists()) {
        commonSrc.mkdirs();
      }
    }
  }

  private static final AtomicLong cnt = new AtomicLong(0);
  
  public static List<Throwable> buildMDMJarFileRecursively(File targetDir, Long revision, boolean includeRuntimeContextDependencies, OutputStream statusOutputStream) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    File tmpDir;
    while (true) {
      tmpDir = new File("tmpmdmjar_" + cnt.getAndIncrement());
      if (!tmpDir.exists()) {
        break;
      }
    }
    if (!tmpDir.mkdir()) {
      throw new RuntimeException("Could not create directory " + tmpDir.getAbsolutePath());
    }


    List<Throwable> exceptions = new ArrayList<Throwable>();
    try {
      Set<Long> allRevisions = new HashSet<Long>();
      if (includeRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getDependenciesRecursivly(revision, allRevisions);
      }
      allRevisions.add(revision);
      for (long rev : allRevisions) {
        CommandControl.tryLock(CommandControl.Operation.BUILD_MDMJAR, rev);
        RuntimeContext rc = rm.getRuntimeContext(rev);
        try {
          if (statusOutputStream != null) {
            CommandLineWriter.createCommandLineWriter(statusOutputStream).writeLineToCommandLine( "Building MDM jar file for " + rc + "...");
          }
          exceptions.addAll(buildMDMJarFile(tmpDir, rev));
          File mdmjarFile = new File(tmpDir, "mdm.jar");
          FileUtils.unzip(mdmjarFile.getAbsolutePath(), tmpDir.getAbsolutePath(), new FileFilter() {

            public boolean accept(File pathname) {
              return true;
            }

          });
          FileUtils.deleteFileWithRetries(mdmjarFile);
        } finally {
          CommandControl.unlock(CommandControl.Operation.BUILD_MDMJAR, rev);
        }
      }

      FileUtils.zipDirectory(new File(targetDir, "mdm.jar"), tmpDir);
    } finally {
      FileUtils.deleteDirectoryRecursively(tmpDir);
    }
    return exceptions;
  }


}
