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
package com.gip.xyna.xmcp.xfcli.scriptentry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FileUtils.FileInputStreamCreator;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xmcp.xfcli.scriptentry.util.ApplicationData;

/***
 * 
 * Create a XynaApplication by archiving to contents of input/app, input/application.xml and the relevant XMOM-XMLs in input/XMOM. <br>
 * Input: Path to application.xml and output path (usually directory of application.xml /deploy)<br>
 * Output: Application in folder deploy next to application.xml <br>
 * File: applicationName.versionName.app <br>
 * Does not delete input/app
 */
public class CreateApplicationArchive {

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Input: <Path to application xml> <Output Path>");
      throw new RuntimeException();
    }
    String applicationXmlFilePath = args[0];
    String outputPath = args[1];
    CreateApplicationArchive executor = new CreateApplicationArchive();
    try {
      executor.createApplicationArchive(applicationXmlFilePath, outputPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private void createApplicationArchive(String appXmlPath, String outputPath) throws Exception {
    File applicationXmlFile = new File(appXmlPath);
    String baseDir = applicationXmlFile.getParent();
    ApplicationData appData = ApplicationData.collectXmomEntries(applicationXmlFile);
    String appName = appData.getApplicationXmlEntry().getApplicationName();
    String versionName = appData.getApplicationXmlEntry().getVersionName();
    Stream<XMOMXmlEntry> stream = appData.getApplicationXmlEntry().getXmomEntries().stream();
    Set<String> entries = stream.map(x -> x.getFqName().replace('.', File.separatorChar) + ".xml").collect(Collectors.toSet());
    File deployDir = new File(outputPath);
    File applicationOutputFile = Path.of(deployDir.getAbsolutePath(), appName + "." + versionName + ".app").toFile();
    File tmpDir = new File(deployDir, "app");
    Path xmomPath = Path.of(baseDir, "XMOM");
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(applicationOutputFile));
    int basePathLength = xmomPath.toFile().getAbsolutePath().length() + 1; //+1 is final File.separatorChar
    
    try {
      //add content of input/app to archive
      FileUtils.zipDir(tmpDir, zos, tmpDir, null, null);

      //add XMOM-XMLs to archive
      File xmomDir = xmomPath.toFile();
      File baseDirFile = applicationXmlFile.getParentFile();
      FileUtils.zipDir(xmomDir, zos, baseDirFile, null, null, new FileInputStreamCreator() {
        
        public InputStream create(File f) throws FileNotFoundException {
          //check if file is part of application
          if (entries.contains(f.getAbsolutePath().substring(basePathLength))) {
            return new FileInputStream(f);
          }
          return null;
        }
  
      });
      
      zos.flush();
    } finally {
      zos.close();
    }
  }
}
