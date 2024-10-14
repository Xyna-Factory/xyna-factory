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
package xdev.yang.impl;



import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import xdev.yang.YangAppGenerationInputParameter;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.parser.YangParserException;
import org.yangcentral.yangkit.parser.YangYinParser;
import org.yangcentral.yangkit.writter.YinWriter;

import java.util.zip.ZipOutputStream;

import java.io.FileOutputStream;
import java.io.FileInputStream;



public class YangApplicationGeneration {

  public static YangApplicationGenerationData createModuleCollectionApp(YangAppGenerationInputParameter genParameter) {
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    List<File> files = new ArrayList<>(); // temporary files to remove when finished

    // directory to create the application
    String appName = genParameter.getApplicationName();
    File unzipedApp = new File("/tmp/" + appName);
    files.add(unzipedApp);

    // directory with yang modules
    String yangModulesDir = fileMgmt.retrieve(genParameter.getFileID().getId()).getOriginalFilename();
    if (yangModulesDir.endsWith(".zip")) {
      yangModulesDir = decompressArchive(yangModulesDir);
      files.add(new File(yangModulesDir));
    }
    Document yinDocument = parseYangModules(yangModulesDir);

    createDatatype(genParameter.getDataTypeFQN(), "YangModuleCollection", yinDocument, unzipedApp.getAbsolutePath());
    createApplicationXML(genParameter, unzipedApp.getAbsolutePath());

    // build application
    String appFileId = buildApplication(appName, unzipedApp);

    return new YangApplicationGenerationData(appFileId, files);
  }


  public static YangApplicationGenerationData createDeviceApp(YangAppGenerationInputParameter genParameter) {
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    List<File> files = new ArrayList<>(); // temporary files to remove when finished

    // directory to create the application
    String appName = genParameter.getApplicationName();
    File unzipedApp = new File("/tmp/" + appName);
    files.add(unzipedApp);

    String capabilitiesFile = fileMgmt.retrieve(genParameter.getFileID().getId()).getOriginalFilename();
    Document capabilities;
    try {
      capabilities = XMLUtils.parse(capabilitiesFile);
    } catch (Ex_FileAccessException | XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }

    createDatatype(genParameter.getDataTypeFQN(), "YangDevice", capabilities, unzipedApp.getAbsolutePath());
    createApplicationXML(genParameter, unzipedApp.getAbsolutePath());

    // build application
    String appFileId = buildApplication(appName, unzipedApp);

    return new YangApplicationGenerationData(appFileId, files);
  }


  /*
   * create the only datatype of the application and save to target
   */
  private static void createDatatype(String fqDatatypeName, String baseTypeName, Document metaData, String target) {
    StringBuilder collectionDatatype = new StringBuilder();
    collectionDatatype.append("<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Version=\"1.8\"");
    collectionDatatype.append(" TypeName=\"\" TypePath=\"\" Label=\"\" IsAbstract=\"false\"");
    collectionDatatype.append(" BaseTypeName=\"\" BaseTypePath=\"xmcp.yang\">\n");
    collectionDatatype.append("<Meta>\n<IsServiceGroupOnly>false</IsServiceGroupOnly>\n</Meta>");
    collectionDatatype.append("</DataType>\n");

    String xmomPath = fqDatatypeName.substring(0, fqDatatypeName.lastIndexOf("."));
    String datatypeName = fqDatatypeName.substring(fqDatatypeName.lastIndexOf(".") + 1);
    try {
      Document datatypeDocument = XMLUtils.parseString(collectionDatatype.toString());
      datatypeDocument.getDocumentElement().setAttribute("TypeName", datatypeName);
      datatypeDocument.getDocumentElement().setAttribute("TypePath", xmomPath);
      datatypeDocument.getDocumentElement().setAttribute("Label", datatypeName);
      datatypeDocument.getDocumentElement().setAttribute("BaseTypeName", baseTypeName);

      datatypeDocument.getElementsByTagName("Meta").item(0).appendChild(datatypeDocument.importNode(metaData.getDocumentElement(), true));

      String filePath = "XMOM/" + fqDatatypeName.replace(".", "/") + ".xml";
      XMLUtils.saveDom(new File(target + "/" + filePath), datatypeDocument);
    } catch (XPRC_XmlParsingException | Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }

  }


  /*
   * create application xml and save to target
   */
  public static void createApplicationXML(YangAppGenerationInputParameter genParameter, String target) {
    StringBuilder applicationXML = new StringBuilder();
    applicationXML.append("<Application applicationName=\"\" factoryVersion=\"\" versionName=\"\" xmlVersion=\"1.1\">\n");
    applicationXML.append("<ApplicationInfo>\n<RuntimeContextRequirements>\n<RuntimeContextRequirement>\n");
    applicationXML.append("<ApplicationName>YangBase</ApplicationName>\n<VersionName></VersionName>\n");
    applicationXML.append("</RuntimeContextRequirement>\n</RuntimeContextRequirements>\n</ApplicationInfo>\n");
    applicationXML.append("<XMOMEntries>\n<XMOMEntry implicitDependency=\"false\">\n<FqName></FqName>\n");
    applicationXML.append("<Type>DATATYPE</Type>\n</XMOMEntry>\n</XMOMEntries>\n</Application>\n");
    Long revision = ((ClassLoaderBase) YangApplicationGeneration.class.getClassLoader()).getRevision();

    // set version of YangBase application
    Set<Long> dependencyRevisions = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getRuntimeContextDependencyManagement().getDependencies(revision);
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    String yangBaseAppVersion = null;
    for (Long i : dependencyRevisions) {
      try {
        if (rm.getApplication(i).getName().equals("YangBase")) {
          yangBaseAppVersion = rm.getApplication(i).getVersionName();
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("Could not find required application with revision " + i, e);
      }
    }

    try {
      Document applicationDocument = XMLUtils.parseString(applicationXML.toString());
      applicationDocument.getDocumentElement().setAttribute("applicationName", genParameter.getApplicationName());
      applicationDocument.getDocumentElement().setAttribute("versionName", genParameter.getVersionName());
      applicationDocument.getElementsByTagName("FqName").item(0).setTextContent(genParameter.getDataTypeFQN());
      applicationDocument.getElementsByTagName("VersionName").item(0).setTextContent(yangBaseAppVersion);

      XMLUtils.saveDom(new File(target + "/application.xml"), applicationDocument);

    } catch (XPRC_XmlParsingException | Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
  }


  /*
   * create application zip file
   */
  public static String buildApplication(String appName, File unzipedAppDir) {
    File tmpFile;
    String id;
    try {
      tmpFile = File.createTempFile(appName + "_", ".zip");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(unzipedAppDir, zos, unzipedAppDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try (FileInputStream is = new FileInputStream(tmpFile)) {
      id = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement()
          .store("yang", tmpFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return id;
  }


  private static Document parseYangModules(String yangModulesDir) {
    try {
      Document yinDocument = XMLUtils.parseString("<Yang type=\"ModuleCollection\"></Yang>");
      Element rootElement = yinDocument.getDocumentElement();
      YangSchemaContext schemaContext = YangYinParser.parse(yangModulesDir);
      for (var entry : schemaContext.getParseResult().entrySet()) {
        Document doc = XMLUtils.parseString(YinWriter.serialize(entry.getValue()).asXML());
        rootElement.appendChild(yinDocument.importNode(doc.getDocumentElement(), true));
      }

      return yinDocument;

    } catch (IOException | YangParserException | DocumentException | XPRC_XmlParsingException e) {
      throw new RuntimeException("Could not parse Yang modules", e);
    }
  }


  public static String decompressArchive(String zipFile) {
    String unzipDir = "/tmp/" + new File(zipFile).getName().replace(".zip", "_unzipped");
    try {
      FileUtils.unzip(zipFile, unzipDir, (path) -> true);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not unzip " + zipFile, e);
    }
    return unzipDir;
  }


  public static class YangApplicationGenerationData implements Closeable {

    private final String id;
    private final List<File> files;


    public YangApplicationGenerationData(String id, List<File> files) {
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
      for (File f : files) {
        if (f.isFile()) {
          f.delete();
        } else if (f.isDirectory()) {
          FileUtils.deleteDirectory(f);
        }
      }
    }
  }
}
