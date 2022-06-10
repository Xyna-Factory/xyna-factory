/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna;



import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



/**
 * This is used to update MDM files while creating a delivery item Set IsXynaComponent="true" for all components
 * that are contained within the delivery item FIXME this should not be part of the delivered factory
 */
public class MdmDeliveryHelper {

  private static FilenameFilter xmlFilter = new FilenameFilter() {

    public boolean accept(File dir, String name) {
      boolean isXmlFile = name.endsWith(".xml");
      if (isXmlFile) {
        return true;
      } else {
        boolean isNonSvnDirectory = new File(dir, name).isDirectory() && !name.equals(".svn");
        if (isNonSvnDirectory) {
          return true;
        }
      }
      return false;
    }

  };


  public static void main(String[] args) {

    if (args == null) {
      throw new IllegalArgumentException("Arguments may not be null");
    } else if (args.length < 1) {
      throw new IllegalArgumentException("At least one parameter is required: target directory");
    }

    String directory = args[0];
    File rootFile = new File(directory);
    try {
      prepareMDMFiles(rootFile, true, isXmomDir(rootFile));
    } catch (XynaException e) {
      throw new RuntimeException("An unexpected error occurred while preparing MDM files for delivery", e);
    }

  }


  private static boolean isXmomDir(File f) {
    return "XMOM".equals(f.getName()) || "MDM".equals(f.getName());
  }


  private static void prepareMDMFiles(File dir, boolean dirMustExist, boolean oneParentWasXMOMDir) throws XynaException {
    if (!dir.isDirectory()) {
      if (dirMustExist) {
        throw new XynaException("directory " + dir.getPath() + " not found");
      } else {
        return;
      }
    }
    File[] files = dir.listFiles(xmlFilter);
    for (File f : files) {
      if (f.isDirectory()) {
        boolean oneParentWasXMOMDirForThisSubdir = oneParentWasXMOMDir || isXmomDir(f);
        prepareMDMFiles(f, dirMustExist, oneParentWasXMOMDirForThisSubdir);
      } else if (oneParentWasXMOMDir) {
        prepareOneMDMFile(f);
      }
    }
  }


  private static void prepareOneMDMFile(File f) throws XynaException {
    // this may be extended
    Document d = XMLUtils.parse(f.getAbsolutePath());
    Element rootElement = d.getDocumentElement();
    List<Element> targetElements;
    if (rootElement.getTagName().equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
      targetElements = XMLUtils.getChildElementsByName(rootElement, GenerationBase.EL.EXCEPTIONTYPE);
    } else if (rootElement.getTagName().equals(GenerationBase.EL.DATATYPE)) {
      targetElements = new ArrayList<Element>();
      targetElements.add(rootElement);
    } else if (rootElement.getTagName().equals(GenerationBase.EL.SERVICE)) {
      targetElements = new ArrayList<Element>();
      targetElements.add(rootElement);
    } else {
      System.out.println("Skipping XML: " + f.getAbsolutePath());
      return;
    }
    for (Element targetElement : targetElements) {
      Element metaElement = XMLUtils.getChildElementByName(targetElement, GenerationBase.EL.META);
      if (metaElement == null) {
        metaElement = d.createElement(GenerationBase.EL.META);
        targetElement.insertBefore(metaElement, targetElement.getFirstChild());
      }
      Element xynaComponentElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.ISXYNACOMPONENT);
      if (xynaComponentElement == null) {
        xynaComponentElement = d.createElement(GenerationBase.EL.ISXYNACOMPONENT);
        metaElement.appendChild(xynaComponentElement);
      }
      XMLUtils.setTextContent(xynaComponentElement, "true");
    }
    XMLUtils.saveDom(f, d);
  }

}
