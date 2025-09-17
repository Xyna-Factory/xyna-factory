/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xfmg.oas.generation.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.validations.oas.OpenApiEvaluator;
import org.openapitools.codegen.validations.oas.RuleConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import xfmg.oas.generation.impl.ApplicationGenerationServiceOperationImpl;


public class OasAppBuilder {

  
  public OASApplicationData createOasApp(String generator, String target, String specFile) {
    return createOasApp(generator, target, specFile, new OasImportStatusHandler());
  }

  
  public OASApplicationData createOasApp(String generator, String target, String specFile,
                                         OasImportStatusHandler statusHandler) {
    List<File> files = new ArrayList<>();
    statusHandler.storeStatusParsing();
    
    callGenerator(generator, target, specFile);
    
    statusHandler.storeStatusAppBinaryGen();
    separateFiles(target);
    compileFilter(target);
    String appName = readApplicationXML(target);
 
    File targetAsFile = new File(target);
    File unzipedApp = new File("/tmp/" + appName);
    files.add(unzipedApp);
    File tmpFile;
    try {
      FileUtils.copyRecursivelyWithFolderStructure(targetAsFile, unzipedApp);
      FileUtils.deleteDirectoryRecursively(targetAsFile);
      tmpFile = File.createTempFile(appName + "_", ".zip");
      files.add(tmpFile);
    } catch (IOException | Ex_FileAccessException e1) {
      throw new RuntimeException(e1);
    }
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(unzipedApp, zos, unzipedApp);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String id = null;
    try (FileInputStream is = new FileInputStream(tmpFile)) {
      id = fileMgmt.store("oas", tmpFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return new OASApplicationData(id, files);
  }
  
  
  public void createOasAppOffline(String generator, String targetDir, String specFile) {
    try {
      Path tmpDir = Files.createTempDirectory("oasmain");
      File tmpDirFile = tmpDir.toFile();
      try {
        String tmpDirAsString = tmpDir.toString();

        callGenerator(generator, tmpDirAsString, specFile);
        separateFiles(tmpDirAsString);
        String appName = readApplicationXML(tmpDirAsString);

        File targetAppFile = new File(targetDir, appName + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetAppFile))) {
          FileUtils.zipDir(tmpDirFile, zos, tmpDirFile);
        }

      } finally {
        FileUtils.deleteDirectoryRecursively(tmpDirFile);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } 
  }

  
  private void callGenerator(String generatorName, String target, String specFile) {
    final CodegenConfigurator configurator = new CodegenConfigurator()
        .setGeneratorName(generatorName)
        .setInputSpec(specFile)
        .addAdditionalProperty("generateAliasAsModel", XynaFactory.isFactoryServer() ?
                               ApplicationGenerationServiceOperationImpl.createListWrappers.get() : true)
        .addAdditionalProperty("x-createListWrappers", XynaFactory.isFactoryServer() ? 
                               ApplicationGenerationServiceOperationImpl.createListWrappers.get() : true)
        .setOutputDir(target);
    
      final ClientOptInput clientOptInput = configurator.toClientOptInput();
      DefaultGenerator generator = new DefaultGenerator();
      generator.opts(clientOptInput).generate();
  }
  
  
  public ValidationResult validate(String specFile) {
    ValidationResult result = new ValidationResult();
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    SwaggerParseResult parserResult = new OpenAPIParser().readLocation(specFile, null, options);
    parserResult.getMessages().forEach(message -> result.addError(message));
    OpenAPI specification = parserResult.getOpenAPI();

    RuleConfiguration ruleConfiguration = new RuleConfiguration();
    ruleConfiguration.setEnableRecommendations(true);

    OpenApiEvaluator evaluator = new OpenApiEvaluator(ruleConfiguration);
    org.openapitools.codegen.validation.ValidationResult evaluatorResult = evaluator.validate(specification);
    evaluatorResult.getErrors().forEach(invalid -> result.addError(invalid.getMessage()));
    evaluatorResult.getWarnings().forEach(invalid -> result.addWarning(invalid.getMessage()));
    return result;
  }
  
  
  private String readApplicationXML(String target) {
    Path applicationXML = Path.of(target, "application.xml");
    String appname = "app";
    if (!applicationXML.toFile().exists()) {
      return appname;
    }
    try {
      Document applicationDocument = XMLUtils.parseString(Files.readString(applicationXML));
      if (applicationDocument.getDocumentElement().hasAttribute("applicationName")) {
        appname = applicationDocument.getDocumentElement().getAttribute("applicationName");
      }
      if (applicationDocument.getDocumentElement().hasAttribute("versionName")) {
        appname = appname + "_" + applicationDocument.getDocumentElement().getAttribute("versionName");
        
      }
    } catch (IOException | XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
    return appname;
  }


  private FilenameFilter toSplitFilter = new FilenameFilter() {

    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith("_toSplit.xml") || Files.isDirectory(Path.of(dir.getAbsolutePath(), name));
    }
  };

  private List<String> xmoms = new ArrayList<String>(Arrays.asList(GenerationBase.EL.SERVICE, GenerationBase.EL.DATATYPE));


  private boolean isRelevant(String nodeName) {
    return nodeName != null && xmoms.contains(nodeName);
  }


  private void separateFiles(String target) {
    Path xmomDir = Path.of(target, "XMOM");
    List<File> filesToSplit = new ArrayList<>();
    FileUtils.findFilesRecursively(new File(target), filesToSplit, toSplitFilter);
    for (File f : filesToSplit) {
      try {
        Document d = XMLUtils.parseString("<a>" + Files.readString(Path.of(f.getAbsolutePath())) + "</a>");
        NodeList nl = d.getDocumentElement().getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
          Node item = nl.item(i);
          if (!isRelevant(item.getNodeName())) {
            continue;
          }
          String typePath = item.getAttributes().getNamedItem("TypePath").getNodeValue().replace('.', File.separatorChar);
          String typeName = item.getAttributes().getNamedItem("TypeName").getNodeValue();
          Path dir = Path.of(xmomDir.toString(), typePath);
          Files.createDirectories(dir);
          try (Writer w = new FileWriter(Path.of(dir.toString(), typeName + ".xml").toFile())) {
            XMLUtils.saveDomToWriter(w, item, true);
          }
        }
        FileUtils.deleteFileWithRetries(f);
      } catch (IOException | XPRC_XmlParsingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  
  private FilenameFilter findOASFilterJava = new FilenameFilter() {

    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith("_oasFilter.java") || Files.isDirectory(Path.of(dir.getAbsolutePath(), name));
    }
  };


  private void compileFilter(String target) {
    List<File> filterJava = new ArrayList<>();
    FileUtils.findFilesRecursively(Path.of(target, "filter").toFile(), filterJava, findOASFilterJava);
    
    for (File javaFile: filterJava) {
     String filterName = javaFile.getName().substring(0, javaFile.getName().lastIndexOf("_"));

      //build mdm.jar
      Path mdmJarPath = Path.of(target, "mdm.jar");
      Long revision = null;
      Long httpRevision = null;
      try {
        ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
        revision = clb.getRevision();
        httpRevision = getHttpRevision(revision);
        Support4Eclipse.buildMDMJarFileRecursively(new File(target), revision, true, null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      String httpBasePath = RevisionManagement.getPathForRevision(PathType.TRIGGER, httpRevision, true);
      Path httpTriggerJar = Path.of(httpBasePath, "HTTPTrigger", "HTTPTrigger.jar");
      Path filterOutputDir = Path.of(target, "filter", filterName, filterName + ".jar");

      InMemoryCompilationSet cs = new InMemoryCompilationSet(false, false, false);
      try {
        cs.addToCompile(new JavaSourceFromString("src.com.gip.xyna.xact.filter." + filterName, Files.readString(javaFile.toPath())));
        cs.addToClassPath(mdmJarPath.toString());
        cs.addToClassPath(httpTriggerJar.toString());
        cs.compileToJar(filterOutputDir.toFile(), false);
        FileUtils.deleteFileWithRetries(javaFile);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  private Long getHttpRevision(Long ourRevision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    XynaFactoryControl xynaFactoryControl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    Set<Long> candidates = xynaFactoryControl.getRuntimeContextDependencyManagement().getDependencies(ourRevision);
    for (Long candidate : candidates) {
      if (xynaFactoryControl.getRevisionManagement().getRuntimeContext(candidate).getName().equals("Http")) {
        return candidate;
      }
    }
    return null;
  }
  
  public static String decompressArchive(String zipFile) {
    String unzipDir =  "/tmp/" + new File(zipFile).getName() + "_unzipped";
    FileUtils.deleteDirectoryRecursively(new File(unzipDir));
    try {
      FileUtils.unzip(zipFile, unzipDir, (path) -> true);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not unzip " + zipFile, e);
    }
    File[] files = new File(unzipDir).listFiles((path) -> path.isFile());
    if(files.length != 1) {
      throw new RuntimeException("Could not find specification file in zip.");
    }
    return files[0].getAbsolutePath();
  }
  
}
