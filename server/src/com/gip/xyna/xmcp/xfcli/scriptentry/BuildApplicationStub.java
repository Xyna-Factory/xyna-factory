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
package com.gip.xyna.xmcp.xfcli.scriptentry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FileUtils.FileInputStreamCreator;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xmcp.xfcli.scriptentry.util.ApplicationData;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.StubFileCreator;

/***
 * 
 * Without relying on a running XynaFactory-Server, create an application-stub. <br>
 * Input: Path to application-stub.xml <br>
 * Output: Stub-application in folder deploy next to application-stub.xml <br>
 * File: applicationName.versionName_stub.app
 */

public class BuildApplicationStub {
  
  private static final String TMPDIR = "stubapp";

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Input: Path to application stub xml");
      return;
    }
    String applicationStubXmlFilePath = args[0];
    BuildApplicationStub executor = new BuildApplicationStub();
    File tmpDir = new File(new File(applicationStubXmlFilePath).getParent(), TMPDIR);
    try {
      executor.deleteDirectoryRecursive(tmpDir);
      executor.buildApplicationStub(applicationStubXmlFilePath);
    } catch(Exception e) {
      System.out.println("Exception: " + e);
    } finally {
      executor.deleteDirectoryRecursive(tmpDir);
    }
  }
  
  private boolean deleteDirectoryRecursive(File dir) {
    if (!dir.exists()) {
      return false;
    }
    File[] files = dir.listFiles();
    if (files != null) {
        for (File file : files) {
            deleteDirectoryRecursive(file);
        }
    }
    return dir.delete();
}
  
  
  private void buildApplicationStub(String applicationStubXmlFilePath) throws Exception {

    //create temporary directory
    File applicationStubXmlFile = new File(applicationStubXmlFilePath);
    String baseDir = applicationStubXmlFile.getParent();
    File tmpDir = new File(baseDir, TMPDIR);
    tmpDir.mkdirs();
    
    //collect application data
    ApplicationData appData = ApplicationData.collectXmomEntries(applicationStubXmlFile);
    String appName = appData.getApplicationXmlEntry().getApplicationName();
    String versionName = appData.getApplicationXmlEntry().getVersionName();
    Set<String> xmomEntries = appData.getXmomEntries();
    ApplicationXmlEntry appEntry = appData.getApplicationXmlEntry();
    
    //create deploy folder
    new File(baseDir, "deploy").mkdirs();
    
    //for each file in XMOM, create a stub file
    //workflow XMLs are reduced to signature
    //XMLs not in xmomEntries are ignored
    //and create application file
    File applicationOutputFile = Path.of(baseDir, "deploy", appName + "." + versionName + "_stub.app").toFile();
    StubFileCreator creator = new StubFileCreator();
    Path xmomPath = Path.of(baseDir, "XMOM");
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(applicationOutputFile));
    try {
      FileUtils.zipDir(xmomPath.toFile(), zos, new File(baseDir), null, null, new FileInputStreamCreator() {
        
        public InputStream create(File f) throws FileNotFoundException {
            return creator.createStubForFile(f, xmomEntries);
        }
  
      });
  
      ZipEntry ze = new ZipEntry(ApplicationManagementImpl.XML_APPLICATION_FILENAME);
      zos.putNextEntry(ze);
      Document doc = appEntry.buildXmlDocument();
      XMLUtils.saveDomToOutputStream(zos, doc);
      zos.flush();
    } finally {
      zos.close();
    }
  }
}
