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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;


public class ApplicationFileReader {

private static Logger logger = CentralFactoryLogging.getLogger(ApplicationFileReader.class);
  
  private PrintStream statusOutputStream;
  private String xmlApplicationFilename;
  private boolean checkBinaries;
  private ApplicationXmlEntry applicationXml = null;
  private int predictedAdditionalOpenFiles = 0;
  private int cumulatedSizeOfXMOMClasses = 0;
  private int cumulatedSizeOfJars = 0;

  public ApplicationFileReader(PrintStream statusOutputStream, String xmlApplicationFilename, boolean checkBinaries ) {
    this.statusOutputStream = statusOutputStream;
    this.xmlApplicationFilename = xmlApplicationFilename;
    this.checkBinaries = checkBinaries;
  }

  public void read(String fileName) throws XFMG_CouldNotImportApplication {
    File appFile = new File(fileName);
    if (!appFile.exists()) {
      output(statusOutputStream, "Target file <" + appFile.getAbsolutePath() + "> not found.");
      throw new XFMG_CouldNotImportApplication(fileName);
    }
    
    ZipInputStream zis = null;
    try {
      // search application.xml
      zis = new ZipInputStream(new FileInputStream(appFile));
      ZipEntry zipEntry;
      boolean found = false;
      while ((zipEntry = zis.getNextEntry()) != null) {
        if (zipEntry.getName().equals(xmlApplicationFilename)) {
          
          found = true;
          
          //direkt das xml aus dem zipinputstream lesen macht probleme, weil der stream geschlossen wird, deshalb erstmal das xml in eine bytearray transferieren
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          int length = 0;
          byte[] data = new byte[2048];
          while ((length = zis.read(data)) != -1) {
            bos.write(data, 0, length);
          }

          logger.debug("Parsing file " + xmlApplicationFilename);
          SAXParserFactory factory = SAXParserFactory.newInstance();
          SAXParser saxParser = factory.newSAXParser();
          ApplicationXmlHandler handler = new ApplicationXmlHandler();
          saxParser.parse(new ByteArrayInputStream(bos.toByteArray()), handler);
          applicationXml = handler.getApplicationXmlEntry();

          if (! checkBinaries ) {
            //jars m�ssen nicht �berpr�ft werden
            break;
          }
        } else if (zipEntry.getName().endsWith(".jar")) {
          predictedAdditionalOpenFiles ++;
          cumulatedSizeOfJars += getZipEntrySize(zipEntry, zis);
        } else if (zipEntry.getName().endsWith(".class")) {
          cumulatedSizeOfXMOMClasses += getZipEntrySize(zipEntry, zis);
        }
      }

      if (!found) {
        if (logger.isDebugEnabled()) {
          logger.debug("No " + xmlApplicationFilename + " was found in file " + appFile.getAbsolutePath());
        }
        output(statusOutputStream, "Could not find " + xmlApplicationFilename + " in file (corrupt file?).");
        throw new XFMG_CouldNotImportApplication(fileName, new RuntimeException("Could not find " + xmlApplicationFilename + " in file (corrupt file?)."));
      }


    } catch (XFMG_CouldNotImportApplication e) {
      throw e;
    } catch (Exception e) {
      throw new XFMG_CouldNotImportApplication(fileName, e);
    } finally {
      if (zis != null) {
        try {
          zis.close();
        } catch (IOException e) {
          logger.warn("Could not close file stream.", e);
        }
      }
    }

    
  }
  
  public ApplicationXmlEntry getApplicationXml() {
    return applicationXml;
  }

  public int getPredictedAdditionalOpenFiles() {
    return predictedAdditionalOpenFiles;
  }

  public int getCumulatedSizeOfXMOMClasses() {
    return cumulatedSizeOfXMOMClasses;
  }

  public int getCumulatedSizeOfJars() {
    return cumulatedSizeOfJars;
  }

  private void output(PrintStream statusOutputStream, String text) {
    if (statusOutputStream != null) {
      try {
        statusOutputStream.println(text);
      } catch (Exception e) {
        logger.warn("Failed to print status information to CLI: <" + text + ">", e);
      }
    }
  }
  
  private long getZipEntrySize(ZipEntry ze, ZipInputStream zis) throws IOException {
    if (ze.getSize() > 0) {
      return ze.getSize();
    }
    
    long size = 0;
    int length = 0;
    byte[] data = new byte[2048];
    while ((length = zis.read(data)) != -1) {
      size += length;
    }
    return size;
  }

  public ApplicationInformation getApplicationInformation() {
    ApplicationInformation ai =
        new ApplicationInformation(applicationXml.getApplicationName(), applicationXml.getVersionName(),
                                   ApplicationState.FILE, applicationXml.getComment());
    
    List<RuntimeContextRequirementXmlEntry> rcrs = applicationXml.getApplicationInfo().getRuntimeContextRequirements();
    if( rcrs != null && ! rcrs.isEmpty() ) {
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();      
      List<RuntimeDependencyContext> reqs = new ArrayList<RuntimeDependencyContext>();
      List<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
      for( RuntimeContextRequirementXmlEntry rcr : applicationXml.getApplicationInfo().getRuntimeContextRequirements() ) {
        RuntimeDependencyContext rc = rcr.getRuntimeContext();
        if (!rm.runtimeContextExists(rc)) {
          problems.add( RuntimeContextProblem.unresolvableRequirement(rc));
        }
        reqs.add( rc );
      }
      ai.setRequirements(reqs);
      if( ! problems.isEmpty() ) {
        ai.setProblems(problems);
      }
      
    }
    ai.setDescription(applicationXml.applicationInfo.getDescription());
    ai.setBuildDate(applicationXml.applicationInfo.getBuildDate());
    
    return ai;
  }

  
}
