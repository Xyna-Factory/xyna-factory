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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.ModuleCollectionGenerationParameter;



public class ModuleCollectionApp {

  public static YangModuleApplicationData createModuleCollectionApp(ModuleCollectionGenerationParameter genParameter) {
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    String yangDir = fileMgmt.retrieve(genParameter.getFileID().getId()).getOriginalFilename();
    if (yangDir.endsWith(".zip")) {
      yangDir = decompressArchive(yangDir);
    }
    String appName = genParameter.getApplicationName();
    File unzipedApp = new File("/tmp/" + appName);

    // temporary files to remove when finished
    List<File> files = new ArrayList<>();
    files.add(unzipedApp);


    // collect yang files
    // parse yang files
    createCollectionDatatype(genParameter.getDataTypeFQN(), unzipedApp.getAbsolutePath());
    createApplicationXML(genParameter, unzipedApp.getAbsolutePath());

    // build application
    File tmpFile;
    try {
      tmpFile = File.createTempFile(appName + "_", ".zip");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile))) {
      FileUtils.zipDir(unzipedApp, zos, unzipedApp);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


    String id;
    try (FileInputStream is = new FileInputStream(tmpFile)) {
      id = fileMgmt.store("yang", tmpFile.getAbsolutePath(), is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new YangModuleApplicationData(id, files);
  }


  private static void createApplicationXML(ModuleCollectionGenerationParameter genParameter, String target) {
    StringBuilder applicationXML = new StringBuilder();
    applicationXML.append("<Application applicationName=\"\" factoryVersion=\"\" versionName=\"\" xmlVersion=\"1.1\">\n");
    applicationXML.append("<ApplicationInfo>\n<RuntimeContextRequirements>\n<RuntimeContextRequirement>\n");
    applicationXML.append("<ApplicationName>YangBase</ApplicationName>\n<VersionName>1.0.0</VersionName>\n");
    applicationXML.append("</RuntimeContextRequirement>\n</RuntimeContextRequirements>\n</ApplicationInfo>\n");
    applicationXML.append("<XMOMEntries>\n<XMOMEntry implicitDependency=\"false\">\n<FqName></FqName>\n");
    applicationXML.append("<Type>DATATYPE</Type>\n</XMOMEntry>\n</XMOMEntries>\n</Application>\n");
    Long revision = ((ClassLoaderBase) ModuleCollectionApp.class.getClass().getClassLoader()).getRevision();

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
      applicationDocument.getDocumentElement().setAttribute("versionName", genParameter.getApplicationVersion());
      applicationDocument.getElementsByTagName("FqName").item(0).setTextContent(genParameter.getDataTypeFQN());
      applicationDocument.getElementsByTagName("VersionName").item(0).setTextContent(yangBaseAppVersion);
      XMLUtils.saveDom(new File(target + "/application.xml"), applicationDocument);

    } catch (XPRC_XmlParsingException | Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
  }


  private static void createCollectionDatatype(String fqDatatypeName, String target) {
    StringBuilder collectionDatatype = new StringBuilder();
    collectionDatatype.append("<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Version=\"1.8\"");
    collectionDatatype.append(" TypeName=\"\" TypePath=\"\" Label=\"\" IsAbstract=\"false\">");
    collectionDatatype.append(" BaseTypeName=\"YangModuleCollection\" BaseTypePath=\"xdev.yang\">\n");
    collectionDatatype.append("<Meta>\n<IsServiceGroupOnly>false</IsServiceGroupOnly>\n</Meta>");
    collectionDatatype.append("</DataType>\n");

    String xmomPath = fqDatatypeName.substring(0, fqDatatypeName.lastIndexOf("."));
    String datatypeName = fqDatatypeName.substring(fqDatatypeName.lastIndexOf(".") + 1);
    try {
      Document datatypeDocument = XMLUtils.parseString(collectionDatatype.toString());
      datatypeDocument.getDocumentElement().setAttribute("TypeName", datatypeName);
      datatypeDocument.getDocumentElement().setAttribute("TypePath", xmomPath);
      datatypeDocument.getDocumentElement().setAttribute("Label", datatypeName);
      String filePath = "XMOM/" + fqDatatypeName.replace(".", "/") + ".xml";
      XMLUtils.saveDom(new File(target + "/" + filePath), datatypeDocument);
    } catch (XPRC_XmlParsingException | Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
  }


  public static String decompressArchive(String zipFile) {
    String unzipDir = "/tmp/" + new File(zipFile).getName() + "_unzipped";
    FileUtils.deleteDirectoryRecursively(new File(unzipDir));
    try {
      FileUtils.unzip(zipFile, unzipDir, (path) -> true);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not unzip " + zipFile, e);
    }
    File[] files = new File(unzipDir).listFiles((path) -> path.isFile());
    if (files.length != 1) {
      throw new RuntimeException("Could not find specification file in zip.");
    }
    return files[0].getAbsolutePath();
  }


  public static class YangModuleApplicationData implements Closeable {

    private final String id;
    private final List<File> files;


    public YangModuleApplicationData(String id, List<File> files) {
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