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
package xfmg.oas.generation.cli.impl;



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
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.openapitools.codegen.OpenAPIGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

import xfmg.oas.generation.cli.generated.Buildoasapplication;



public class BuildoasapplicationImpl extends XynaCommandImplementation<Buildoasapplication> {

  public void execute(OutputStream statusOutputStream, Buildoasapplication payload) throws XynaException {
    String swagger = payload.getPath();
    String target = "/tmp/" + payload.getApplicationName();

    try {
      OpenAPIGenerator.main(new String[] {"validate", "-i", swagger, "--recommend"});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String id = createOasApp("xmom-data-model", target + "_datatypes", swagger);
    writeToCommandLine(statusOutputStream, "Datamodel ManagedFileId: " + id);
    if (payload.getBuildProvider()) {
      id = createOasApp("xmom-server", target + "_provider", swagger);
      writeToCommandLine(statusOutputStream, "provider ManagedFileId: " + id);
    }
    if (payload.getBuildClient()) {
      id = createOasApp("xmom-client", target + "_client", swagger);
      writeToCommandLine(statusOutputStream, "client ManagedFileId: " + id);
    }

    writeToCommandLine(statusOutputStream, "Done.");
  }


  private String createOasApp(String generator, String target, String swagger) {
    OpenAPIGenerator.main(new String[] {"generate", "-g", generator, "-i", swagger, "-o", target});

    separateFiles(target);
    compileFilter(target);

    File targetAsFile = new File(target);
    File tmpFile;
    try {
      tmpFile = File.createTempFile("app_", ".zip");
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }

    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(targetAsFile, zos, targetAsFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String id = null;
    try (FileInputStream is = new FileInputStream(tmpFile)) {
      id = fileMgmt.store("oas", tmpFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return id;
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
}
