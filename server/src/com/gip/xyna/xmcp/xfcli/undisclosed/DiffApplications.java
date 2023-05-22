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
package com.gip.xyna.xmcp.xfcli.undisclosed;



import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationFileReader;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;



public class DiffApplications implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    if (allArgs.getArgCount() != 2) {
      clw.writeLineToCommandLine("Syntax: diffapplications <application file 1> <application file 2>");
      clw.writeLineToCommandLine("application files may be whole applications or just the xml descriptions.");
      return;
    }
    File f = new File(allArgs.getArg(0));
    if (!f.exists()) {
      clw.writeLineToCommandLine("File not found: " + allArgs.getArg(0));
      return;
    }
    File f2 = new File(allArgs.getArg(1));
    if (!f2.exists()) {
      clw.writeLineToCommandLine("File not found: " + allArgs.getArg(1));
      return;
    }

    try {
      ApplicationXmlEntry applicationXml1 = parse(f, clw);
      ApplicationXmlEntry applicationXml2 = parse(f2, clw);
      ApplicationXmlEntry.diff(applicationXml1, applicationXml2, clw);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (XFMG_CouldNotImportApplication e) {
      throw new RuntimeException("Invalid file!", e);
    }
  }


  private ApplicationXmlEntry parse(File file, CommandLineWriter clw) throws ParserConfigurationException, SAXException, IOException,
      XFMG_CouldNotImportApplication {
    if (file.getName().endsWith(".xml")) {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      ApplicationXmlHandler handler = new ApplicationXmlHandler();
      saxParser.parse(file, handler);
      return handler.getApplicationXmlEntry();
    } else {
      ApplicationFileReader afr = new ApplicationFileReader(clw.getPrintStream(), "application.xml", false);
      afr.read(file.getAbsolutePath());
      return afr.getApplicationXml();
    }
  }
}
