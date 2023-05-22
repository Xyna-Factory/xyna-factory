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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.base;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.exceptions.XDEV_ProjectTemplateZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.xfcli.generated.Deploydatatype;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_JarFileForServiceImplNotFoundException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.TemplateGenerationResult;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectCodeGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;



public class ServiceImplementationTemplate extends ImplementationTemplate {

  public static final String IMPL_KIND = "mdmimpl";
  public static final String IMPL_KIND_BUILD_SCRIPT = "buildService.xml";
  public static final String SERVICE_DEFINITION_JAR = Support4Eclipse.PROJECT_LIB_XYNA_FOLDER + File.separator + Support4Eclipse.FILENAME_SERVICEDEFINITION_JAR;
  public static final String SERVICE_DEFINITION_JAVADOC_JAR = Support4Eclipse.PROJECT_LIB_XYNA_FOLDER + File.separator + Support4Eclipse.FILENAME_SERVICEDEFINITION_JAVADOC_JAR;

  private final String fqXmlName;
  private final String mdmClassesPath;

  
  public ServiceImplementationTemplate(String fqXmlName, String mdmClassesPath, Long revision) {
    super(revision);
    this.fqXmlName = fqXmlName;
    this.mdmClassesPath = mdmClassesPath;
  }

  
  public ServiceImplementationTemplate(String fqXmlName, Long revision) {
    super(revision);
    this.fqXmlName = fqXmlName;
    mdmClassesPath = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revision)).getPath();
  }


  public String getFQXmlName() {
    return fqXmlName;
  }


  public String getProjectName() {
    return GenerationBase.getSimpleNameFromFQName(fqXmlName) + "Impl";
  }


  @Override
  public String getBaseDirectoryName() {
    return fqXmlName;
  }
  
  public String getProjectKindFolder() {
    return IMPL_KIND;
  }


  public String getBuildScriptName() {
    return IMPL_KIND_BUILD_SCRIPT;
  }


  public String updateBuildXmlDeployTarget(File projectLocationDirectory, String line)
      throws XDEV_InvalidProjectTemplateParametersException {
    File f;
    try {
      f = new File(GenerationBase.getFileLocationOfServiceLibsForSaving(GenerationBase.transformNameForJava(fqXmlName), revision));
    } catch (XynaException e) {
      throw new XDEV_InvalidProjectTemplateParametersException("fqServiceName", fqXmlName, e);
    }
    line = line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_STATEMENT),
                           Matcher.quoteReplacement(Deploydatatype.COMMAND_Deploydatatype
                              + " -fqDatatypeName ${fqclassname} -workspaceName '${workspacename}' -protectionMode TRY -registerWithXMOM true"));

    String revisionDir = getRevisionDir();
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_PATH),
                           Matcher.quoteReplacement("${server.path}" + File.separator + "${revision.dir}" + f.getPath().substring(revisionDir.length())));

    String pathToSavedXml = GenerationBase.getFileLocationOfXmlNameForSaving(getFQXmlName(), revision) + ".xml";
    String pathOnly = pathToSavedXml.substring(0, pathToSavedXml.lastIndexOf(File.separator));
    String xmlNameOnly = pathToSavedXml.substring(pathToSavedXml.lastIndexOf(File.separator) + 1);
    String deployTargetPath = "${revision.dir}" + pathOnly.substring(revisionDir.length());
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_PATH),
                           Matcher.quoteReplacement("${server.path}" + File.separator + "${revision.dir}" + pathOnly.substring(revisionDir.length())));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_FILE_NAME),
                           Matcher.quoteReplacement(xmlNameOnly));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_FQ_CLASSNAME), Matcher.quoteReplacement(getFQXmlName()));
    line = line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_TARGET_PATH), Matcher.quoteReplacement(deployTargetPath));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_WORKSPACE_NAME), Matcher.quoteReplacement(getWorkspaceName()));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_REVISION_DIR), Matcher.quoteReplacement(revisionDir));
    return line;
  }


  public void writeTemplateFiles(File projectLocationDirectory, boolean legacy) throws Ex_FileAccessException,
      XDEV_InvalidProjectTemplateParametersException {

    DOM d;
    try {
      d = DOM.generateUncachedInstance(getFQXmlName(), legacy, revision);
      d.parseGeneration(legacy, true);
    } catch (XPRC_InvalidPackageNameException e2) {
      throw new XDEV_InvalidProjectTemplateParametersException("fqServiceName", fqXmlName, e2);
    } catch (XPRC_InheritedConcurrentDeploymentException e2) {
      throw new RuntimeException(e2);
    } catch (AssumedDeadlockException e2) {
      throw new RuntimeException(e2);
    } catch (XPRC_MDMDeploymentException e2) {
      throw new RuntimeException(e2);
    }
    
    // create serviceimpl template string
    TemplateGenerationResult templates = d.generateServiceImplTemplate();
    
    File serviceDefinitionLib = generateServiceDefinitionLib(d, templates, projectLocationDirectory);
    File target = new File(projectLocationDirectory, Support4Eclipse.PROJECT_LIB_XYNA_FOLDER + File.separator);
    try {
      target.mkdirs();
      target = new File(projectLocationDirectory, SERVICE_DEFINITION_JAR);
      target.createNewFile();
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }
    FileUtils.copyFile(serviceDefinitionLib, target);
    serviceDefinitionLib.delete();
    
    for (Pair<String, String> pair : templates.getTemplateImplementationFiles()) {
      String javaFilename = pair.getFirst().replaceAll("\\.", File.separator) + ".java";
      File f = new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER + File.separator + javaFilename);
      FileUtils.writeStringToFile(pair.getSecond(), f);
    }
    
    File xmlFile = new File(GenerationBase.getFileLocationOfXmlNameForSaving(getFQXmlName(), revision) + ".xml");
    File xmlFileTarget =
        new File(projectLocationDirectory, Support4Eclipse.XML_DEFINITION_FOLDER + File.separator + xmlFile.getName());

    if (!new File(projectLocationDirectory, Support4Eclipse.XML_DEFINITION_FOLDER).exists()) {
      new File(projectLocationDirectory, Support4Eclipse.XML_DEFINITION_FOLDER).mkdir();
    }

    if (!xmlFileTarget.exists()) {
      try {
        if (!xmlFileTarget.createNewFile()) {
          throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(xmlFileTarget.getAbsolutePath(),
                                                                          getProjectName());
        }
      } catch (IOException e) {
        throw new XDEV_ProjectTemplateZipFileCouldNotBeCreatedException(xmlFileTarget.getAbsolutePath(),
                                                                        getProjectName());
      }
    }
    
    FileUtils.copyFile(xmlFile, xmlFileTarget);
  }


  public String getXMLDefinitionFolder() {
    return getProjectKindFolder() + File.separator + getProjectName() + File.separator + "xmldefinition";
  }


  private File generateServiceDefinitionLib(DOM dom, final TemplateGenerationResult result, File targetDir)
      throws Ex_FileAccessException {
      // compile
      HashSet<String> jars = new HashSet<String>();
      try {
        for (DOM domInHierarchy : dom.getDOMHierarchy()) {
          domInHierarchy.getDependentJarsWithoutRecursion(jars, true, true);
        }
      } catch (XPRC_JarFileForServiceImplNotFoundException e) {
        throw new RuntimeException(e);
      } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
      
      // generate datatype-stub TODO checks to only generate in new cases
      XynaObjectCodeGenerator xocg = new XynaObjectCodeGenerator(dom);
      CodeBuffer stubCb = new CodeBuffer("xdev.xlibdev.supp4eclipse");
      xocg.generateJavaStub(stubCb);

      File tempClassFolder = new File(targetDir, "tmp_classes");
      tempClassFolder.mkdirs();
      File serviceDefinitionLib = new File(targetDir, Support4Eclipse.FILENAME_SERVICEDEFINITION_JAR);
      InMemoryCompilationSet c = new InMemoryCompilationSet(true, true, false);
      for (long rev : dom.collectDependentRevisions()) {
        File mdmclasses = new File(RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, rev));
        c.addToClassPath(mdmclasses.getPath());
      }
      for (String jar : jars) {
        c.addToClassPath(jar);
      }
      String classPath = c.getClassPath();
      try {
        c.setClassDir(tempClassFolder.getAbsolutePath());
        for (Pair<String, String> filenameFilecontentPair : result.getFilesForGeneratedAdditionalLib()) {
          c.addToCompile(new JavaSourceFromString(filenameFilecontentPair.getFirst(), filenameFilecontentPair.getSecond()));
        }
        for (Pair<String, String> filenameFilecontentPair : result.getTemplateImplementationFiles()) {
          c.addToCompile(new JavaSourceFromString(filenameFilecontentPair.getFirst(), filenameFilecontentPair.getSecond()));
        }
        for (Pair<String, String> filenameFilecontentPair : result.getDependencies()) {
          c.addToCompile(new JavaSourceFromString(filenameFilecontentPair.getFirst(), filenameFilecontentPair.getSecond()));
        }
        c.addToCompile(new JavaSourceFromString(dom.getFqClassName(), dom.getFqClassName(), stubCb.toString(), dom.getRevision()));
        try {
          c.compile();
        } catch (Throwable t) {
          Department.handleThrowable(t);
          throw new RuntimeException(t);
        }

        FileUtils.deleteAllBut(tempClassFolder, new FileFilter() {

          public boolean accept(File pathname) {
            //alles ausser den additionalLibs l�schen
            if (pathname.isFile()) {
              for (Pair<String, String> filenameFilecontentPair : result.getFilesForGeneratedAdditionalLib()) {
                String simpleName = filenameFilecontentPair.getFirst();
                if (simpleName.endsWith(".java")) {
                  simpleName = simpleName.substring(0, simpleName.length() - ".java".length());
                }
                simpleName = GenerationBase.getSimpleNameFromFQName(simpleName);
                //innere klassen ber�cksichtigen, aber aufpassen, dass man nicht auch projekt-impl-klassen beh�lt
                //    in dieser form gibt es keine doppeltdeutigkeiten
                if (pathname.getName().matches("^" + Pattern.quote(simpleName) + "(\\$.*)?\\.class$")) {
                  return true;
                }
              }
              return false;
            } else {
              return false;
            }
          }
        });
        FileUtils.zipDirectory(serviceDefinitionLib, tempClassFolder);
      } finally {
        FileUtils.deleteDirectoryRecursively(tempClassFolder);
      }
      
      //javadoc
      Set<String> files = new HashSet<String>();
      for (Pair<String, String> filenameFilecontentPair : result.getFilesForGeneratedAdditionalLib()) {
        String filename = GenerationBase.getRelativeJavaFileLocation(filenameFilecontentPair.getFirst(), false, revision);
        files.add(filename);
      }
      File javadocDir = new File(targetDir, "javadoc");
      String sourcePath =  GenerationBase.getRelativeJavaFileLocation("a.B", false, revision);
      sourcePath = sourcePath.substring(0, sourcePath.length() - "a.B.java".length());
      GenerationBase.createJavaDoc(files.toArray(new String[files.size()]), javadocDir.getAbsolutePath(), sourcePath, classPath);
      Manifest manifest = new Manifest();
      Support4Eclipse.createJarFile(manifest, new File(targetDir, SERVICE_DEFINITION_JAVADOC_JAR), javadocDir, true);
      FileUtils.deleteDirectoryRecursively(javadocDir);
      
      return serviceDefinitionLib;
  }


  public File buildServiceDefinitionJarFile(File targetDir) throws Ex_FileAccessException,
                                           XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                           XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    DOM d = DOM.getInstance(fqXmlName, revision);
    d.parseGeneration(false, true);
    TemplateGenerationResult result = d.generateServiceImplTemplate();
    return generateServiceDefinitionLib(d, result, targetDir);
  }
  

  @Override
  public void writeToClasspathFile(BufferedWriter bw, boolean legacy) throws IOException {
    super.writeToClasspathFile(bw, legacy);
    bw.write("  <classpathentry kind=\"lib\" path=\"lib/xyna/" + Support4Eclipse.FILENAME_SERVICEDEFINITION_JAR + "\"/>\n");
    bw.write("      <attributes>\n");
    bw.write("          <attribute name=\"javadoc_location\" value=\"jar:platform:/resource/" + getProjectName()
        + "/lib/xyna/" + Support4Eclipse.FILENAME_SERVICEDEFINITION_JAVADOC_JAR + "!/\"/>\n");
    bw.write("      </attributes>\n");
  }

}
