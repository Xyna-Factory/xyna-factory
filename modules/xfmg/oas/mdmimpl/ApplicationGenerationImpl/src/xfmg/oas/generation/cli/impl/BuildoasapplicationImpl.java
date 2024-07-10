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
package xfmg.oas.generation.cli.impl;



import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

import xfmg.oas.generation.cli.generated.Buildoasapplication;

import org.apache.log4j.Logger;


public class BuildoasapplicationImpl extends XynaCommandImplementation<Buildoasapplication> {

  private static Logger logger = CentralFactoryLogging.getLogger(BuildoasapplicationImpl.class);
  
  public void execute(OutputStream statusOutputStream, Buildoasapplication payload) throws XynaException {
    String specFile = payload.getPath();
    String target = "/tmp/" + payload.getApplicationName();
    
    if(specFile.endsWith(".zip")) {
      specFile = decompressArchive(specFile);
    }
    
    ValidationResult result = validate(specFile);
    StringBuilder errors = new StringBuilder("Validation found errors:");
    if (!result.getErrors().isEmpty()) {
      logger.error("Spec: " + specFile + " contains errors.");
      result.getErrors().forEach(error -> {
        logger.error(error);
        errors.append(" ");
        errors.append(error);
      });
    }
    if (!result.getWarnings().isEmpty()) {
      logger.warn("Spec: " + specFile + " contains warnings.");
      result.getWarnings().forEach(warning -> logger.warn(warning));
    }
    if (!result.getErrors().isEmpty()) {
      throw new RuntimeException(errors.toString());
    }

    createAppAndPrintId(statusOutputStream, "xmom-data-model", target + "_datatypes", specFile, "datamodel");
    if (payload.getBuildProvider()) {
      createAppAndPrintId(statusOutputStream, "xmom-server", target + "_provider", specFile, "provider");
    }
    if (payload.getBuildClient()) {
      createAppAndPrintId(statusOutputStream, "xmom-client", target + "_client", specFile, "client");
    }

    writeToCommandLine(statusOutputStream, "Done.");
  }
  
  private void createAppAndPrintId(OutputStream statusOutputStream, String generator, String target, String specFile, String type) {
    try (OASApplicationData appData = createOasApp(generator, target, specFile)) {
      writeToCommandLine(statusOutputStream, type + " ManagedFileId: " + appData.getId() + " ");
    } catch (IOException e) {
      writeToCommandLine(statusOutputStream, "Could not clean up temporary files for " + type);
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + type, e);
      }
    }
  }

  
  public OASApplicationData createOasApp(String generator, String target, String specFile) {
    List<File> files = new ArrayList<>();
    callGenerator(generator, target, specFile);

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
  

  private void callGenerator(String generatorName, String target, String specFile) {
    final CodegenConfigurator configurator = new CodegenConfigurator()
        .setGeneratorName(generatorName)
        .setInputSpec(specFile)
        .setOutputDir(target);

      final ClientOptInput clientOptInput = configurator.toClientOptInput();
      DefaultGenerator generator = new DefaultGenerator();
      generator.opts(clientOptInput).generate();
  }
  
  
  public class ValidationResult {
    private List<String> errors = new LinkedList<String>();
    private List<String> warnings = new LinkedList<String>();
    
     public List<String> getErrors() {
       return errors;
     }
     
     public List<String> getWarnings() {
       return warnings;
     }
     
     public void addError(String error) {
       errors.add(error);
     }
     
     public void addWarning(String warning) {
       warnings.add(warning);
     }
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


  private void compileFilter(String target) {
    Path filterJavaFile = Path.of(target, "filter", "OASFilter", "OASFilter.java");
    if (!filterJavaFile.toFile().exists()) {
      return;
    }

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
    Path filterOutputDir = Path.of(target, "filter", "OASFilter", "OASFilter.jar");

    InMemoryCompilationSet cs = new InMemoryCompilationSet(false, false, false);
    try {
      cs.addToCompile(new JavaSourceFromString("src.com.gip.xyna.xact.filter.OASFilter", Files.readString(filterJavaFile)));
      cs.addToClassPath(mdmJarPath.toString());
      cs.addToClassPath(httpTriggerJar.toString());
      cs.compileToJar(filterOutputDir.toFile(), false);
    } catch (Exception e) {
      throw new RuntimeException(e);
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
  
  public static class OASApplicationData implements Closeable {
    private final String id;
    private final List<File> files;
    
    public OASApplicationData(String id, List<File> files) {
      this.id = id;
      this.files = files;
    }
    
    public String getId() {
      return id;
    }
    
    public List<File> getFiles() {
      return files;
    }

    @Override
    public void close() throws IOException {
      for(File f : files) {
        if(f.isFile()) {
          f.delete();
        } else if(f.isDirectory()) {
          FileUtils.deleteDirectory(f);
        }
      }
    }
  }
}
