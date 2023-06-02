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
package com.gip.xyna.update;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public abstract class UpdateXMOMXMLs extends UpdateJustVersion {

  static enum XMLLocation {
    SAVED, DEPLOYED/* includes all applications */, BOTH;
  }
  
  private XMLLocation location;
  private List<XMOMType> affectedTypes;
  
  
  public UpdateXMOMXMLs(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses,
                        boolean mustRewriteWorkflows, boolean mustRewriteDatatypes, boolean mustRewriteExceptions,
                        XMOMType[] affectedXMOMTypes, XMLLocation location) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses, mustRewriteWorkflows, mustRewriteDatatypes, mustRewriteExceptions);
    this.location = location;
    this.affectedTypes = Arrays.asList(affectedXMOMTypes);
  }
  
  
  @Override
  protected void update() throws XynaException {
    Set<File> files = findFiles(location);
    List<Pair<File, Document>> docs = new ArrayList<Pair<File, Document>>();
    for (File file : files) {
      try {
        Document d = XMLUtils.parse(file, false);
        Element rootElement = d.getDocumentElement();
        String roottag = rootElement.getTagName();
        XMOMType type = XMOMType.getXMOMTypeByRootTag(roottag);
        if (type != null && affectedTypes.contains(type)) {
          docs.add(Pair.of(file, d));
        }
      } catch (XPRC_XmlParsingException e) {
        // skip
      }
    }
    
    for (Pair<File, Document> pair : docs) {
      if (pair.getFirst().getPath().contains("bug16445")) {
        System.out.println("da>!");
      }
      adjust(pair.getSecond());
      XMLUtils.saveDom(pair.getFirst(), pair.getSecond());
    }
  }
  
  
  abstract void adjust(Document doc);
  
  protected Set<File> findFiles(XMLLocation location) {
    Set<File> allFiles = new HashSet<File>();
    
    VersionDependentPath versionDependentPath = VersionDependentPath.getCurrent();
    String savedMdmPath = versionDependentPath.getPath(PathType.XMOM, false);
    if (location == XMLLocation.SAVED || location == XMLLocation.BOTH) {
      findFilesRecursivly(allFiles, new File(savedMdmPath));
    }
    if (location == XMLLocation.DEPLOYED || location == XMLLocation.BOTH) {
      findFilesRecursivly(allFiles, new File(Constants.BASEDIR + Constants.fileSeparator + Constants.REVISION_PATH));
    }
    return allFiles;
  }
  
  
  protected void findFilesRecursivly(Set<File> files, File currentDirectory) {
    File[] currentFiles = currentDirectory.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        if (pathname.getName().endsWith(".xml") ||
            pathname.isDirectory()) {
          return true;
        } else {
          return false;
        }
      }
    });
    for (File file : currentFiles) {
      if (file.isDirectory()) {
        findFilesRecursivly(files, file);
      } else {
        files.add(file);
      }
    }
  }
  
}
