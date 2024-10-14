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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.gip.xyna.FileUtils;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class MaxifyApplicationXml {
  private final static String XMOM_FOLDER = "XMOM";

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Expecting one argument: Path to application.xml and an optional argument: backup original file to");
      System.exit(1);
    }
    File f = new File(args[0]);

    System.out.println("Maxify application xml at " + f.getAbsolutePath());
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

    ApplicationXmlMaxifier maxifier = new ApplicationXmlMaxifier();
    ApplicationXmlEntry app = ApplicationXmlHandler.parseApplicationXml(f.getAbsolutePath());
    Path xmomFolder = new File(f.getParent(), XMOM_FOLDER).toPath();
    maxifier.maxifyApplicationXmlEntry(app, xmomFolder);

    try {
      Document doc = app.buildXmlDocument();
      XMLUtils.saveDomToWriter(new FileWriter(f), doc);
    } catch (IOException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }


  public static class ApplicationXmlMaxifier {

    public void maxifyApplicationXmlEntry(ApplicationXmlEntry app, Path xmomFolder) {
      //all explicit dependencies (already in the ApplicationXmlEntry
      Set<String> ignoreSet = app.getXmomEntries().stream().map(x -> x.getFqName()).collect(Collectors.toSet());

      //XMOMXmlEntries to add (implicit dependencies)
      Collection<XMOMXmlEntry> newEntries = null;
      try {
        File f = xmomFolder.toFile();
        Predicate<Path> filter = createFilter(f, ignoreSet);
        newEntries = Files.walk(xmomFolder).filter(filter).map(p -> createXmomElement(f, p)).collect(Collectors.toSet());
      } catch (IOException e) {
        System.out.println(e);
        throw new RuntimeException(e);
      }

      app.getXmomEntries().addAll(newEntries);
    }


    private Predicate<Path> createFilter(File root, Set<String> ignoreSet) {
      return p -> isXmom(p) && !ignoreSet.contains(FileUtils.deriveFqNameFromPath(root, p));
    }


    private boolean isXmom(Path path) {
      if (!path.toFile().isFile()) {
        return false;
      }
      try {
        XMOMType.getXMOMTypeByFile(path.toFile());
      } catch (Exception e) {
        return false;
      }

      return true;
    }


    private XMOMXmlEntry createXmomElement(File xmomFolder, Path path) {
      String fqn = FileUtils.deriveFqNameFromPath(xmomFolder, path);
      XMOMType type = XMOMType.getXMOMTypeByFile(path.toFile());
      return new XMOMXmlEntry(true, fqn, type.toString());
    }

  }
}
