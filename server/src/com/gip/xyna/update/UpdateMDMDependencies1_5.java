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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/**
 *  1.3 &lt;Exception&gt; elements get referencename and referencepath if it does not exist 
 */
public class UpdateMDMDependencies1_5 extends MDMUpdate {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateMDMDependencies1_5.class);
  static final Pattern findUnderscoresPattern = Pattern.compile("^(.*)(\\.\\_\\d+)+(.*)", Pattern.CASE_INSENSITIVE);


  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.4");
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.5");
  }


  private final AtomicBoolean updatingTriggers = new AtomicBoolean(false);


  @Override
  protected void update(Document doc) throws XynaException {

    Element root = doc.getDocumentElement();

    // only datatypes
    if (!root.getTagName().equals(GenerationBase.EL.DATATYPE)) {
      return;
    }

    List<Element> serviceElements = XMLUtils.getChildElementsByName(root, GenerationBase.EL.SERVICE);
    if (serviceElements != null && serviceElements.size() > 0) {
      // for all services defined in that datatype
      for (Element serviceElement: serviceElements) {
        Element metaElement = XMLUtils.getChildElementByName(serviceElement, GenerationBase.EL.META);
        if (metaElement != null) {
          // only if its meta element exists
          Element additionalDependenciesElement = XMLUtils
                          .getChildElementByName(metaElement, GenerationBase.EL.ADDITIONALDEPENDENCIES);
          if (additionalDependenciesElement != null) {
            // only if the meta element defines additional dependencies
            List<Element> additionalDatatypeDependencyElements = XMLUtils
                            .getChildElementsByName(additionalDependenciesElement,
                                                    GenerationBase.EL.DEPENDENCY_DATATYPE);
            List<Element> additionalWorkflowDependencyElements = XMLUtils
                            .getChildElementsByName(additionalDependenciesElement,
                                                    GenerationBase.EL.DEPENDENCY_WORKFLOW);
            List<Element> allAdditionalDependencyElements = additionalDatatypeDependencyElements;
            allAdditionalDependencyElements.addAll(additionalWorkflowDependencyElements);
            if (allAdditionalDependencyElements.size() == 0) {
              continue;
            } else {
              // only for the datatype dependencies
              for (Element oneAdditionalDependencyElement : allAdditionalDependencyElements) {
                String oldValue = XMLUtils.getTextContent(oneAdditionalDependencyElement);
                if (logger.isTraceEnabled()) {
                  logger.trace("Modifying additional dependency, old value: " + oldValue);
                }
                String newValue = removeUnderscoresAfterDotAndBeforeNumber(oldValue);
                XMLUtils.setTextContent(oneAdditionalDependencyElement, newValue);
                if (logger.isTraceEnabled()) {
                  logger.trace("Modified additional dependency, new value: " + newValue);
                }
              }
            }
          }
        }
      }
    }

    // if this method is called multiply for different documents by different threads, only one of them
    // is supposed to update the triggers and filters
    if (updatingTriggers.compareAndSet(false, true)) {
      updateTriggersAndFilters();
    }

  }


  private String removeUnderscoresAfterDotAndBeforeNumber(String oldValue) {
    Matcher m = findUnderscoresPattern.matcher(oldValue);
    whileLoop : while (m.matches()) {
      String toBeModified = m.group(2);
      if (toBeModified != null && toBeModified.length() != 0) {
        oldValue = oldValue.replaceAll(toBeModified, toBeModified.replace("_", ""));
        m = findUnderscoresPattern.matcher(oldValue);
      } else {
        break whileLoop;
      }
    }
    return oldValue;
  }


  private void updateTriggersAndFilters() throws XynaException {
    VersionDependentPath versionDependentPath = VersionDependentPath.getCurrent();
    String filterDeploymentDir = versionDependentPath.getPath(PathType.FILTER, true);
    File filtersDir = new File(filterDeploymentDir);
    if (filtersDir.exists() && filtersDir.isDirectory()) {
      for (File oneFilterXmlFile : getRelevantXmlFilesInDirectory(filtersDir)) {
        updateOneTrigger(oneFilterXmlFile);
      }
    }
    String triggerDeploymentDir = versionDependentPath.getPath(PathType.TRIGGER, true);
    File triggerssDir = new File(triggerDeploymentDir);
    if (triggerssDir.exists() && triggerssDir.isDirectory()) {
      for (File oneTriggerXmlFile : getRelevantXmlFilesInDirectory(triggerssDir)) {
        updateOneTrigger(oneTriggerXmlFile);
      }
    }
  }


  private List<File> getRelevantXmlFilesInDirectory(File dir) {
    ArrayList<File> result = new ArrayList<File>();
    for (File f : dir.listFiles(xmlFilter)) {
      if (f.isDirectory()) {
        result.addAll(getRelevantXmlFilesInDirectory(f));
      } else {
        result.add(f);
      }
    }
    return result;
  }


  private void updateOneTrigger(File xmlFile) throws XynaException {
    Document d = XMLUtils.parse(xmlFile.getAbsolutePath());
    String mdmVersionFile = d.getDocumentElement().getAttribute(GenerationBase.ATT.MDM_VERSION);
    if (mdmVersionFile == null || mdmVersionFile.length() == 0) {
      Element additionalDependenciesElement = XMLUtils.getChildElementByName(d.getDocumentElement(),
                                                                             GenerationBase.EL.ADDITIONALDEPENDENCIES);
      if (additionalDependenciesElement != null) {
        List<Element> dependencyElements = XMLUtils.getChildElementsByName(additionalDependenciesElement,
                                                                           GenerationBase.EL.DEPENDENCY_DATATYPE);
        dependencyElements.addAll(XMLUtils.getChildElementsByName(additionalDependenciesElement,
                                                                  GenerationBase.EL.DEPENDENCY_WORKFLOW));
        for (Element dependencyElement : dependencyElements) {
          String oldValue = XMLUtils.getTextContent(dependencyElement);
          if (logger.isTraceEnabled()) {
            logger.trace("Updating dependency for trigger or filter, old value: " + oldValue);
          }
          String newValue = removeUnderscoresAfterDotAndBeforeNumber(oldValue);
          XMLUtils.setTextContent(dependencyElement, newValue);
          if (logger.isTraceEnabled()) {
            logger.trace("Updated dependency for trigger or filter, new value: " + newValue);
          }
        }
      }
      d.getDocumentElement().setAttribute(GenerationBase.ATT.MDM_VERSION, getVersionAfterUpdate().getString());
      XMLUtils.saveDom(xmlFile, d);
    }
  }

}
