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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;



public class MinifyApplicationXml {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Expecting one argument: Path to application.xml and an optional argument: backup original file to");
      System.exit(1);
    }
    File f = new File(args[0]);

    System.out.println("Minify application xml at " + f.getAbsolutePath());
    if (args.length == 2) {
      System.out.println("Backup original file to " + new File(args[1]).getAbsolutePath());
    }


    if (!f.exists()) {
      System.out.println("File " + f.getAbsolutePath() + " not found.");
      System.exit(1);
    }

    try {
      if (args.length == 2) {
        Files.copy(f.toPath(), new File(args[1]).toPath());
      }
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
    
    ApplicationXmlEntry app = ApplicationXmlHandler.parseApplicationXml(f.getAbsolutePath());
    app.minify();

    try {
      Document doc = app.buildXmlDocument();
      XMLUtils.saveDomToWriter(new FileWriter(f), doc);
    } catch (IOException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
  

}
