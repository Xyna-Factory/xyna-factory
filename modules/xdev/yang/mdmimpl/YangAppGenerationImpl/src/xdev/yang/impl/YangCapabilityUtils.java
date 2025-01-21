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



import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xmcp.yang.YangDevice;



public class YangCapabilityUtils {


  public static List<Module> filterModules(List<Module> modules, List<String> capabilities) {
    List<Module> result = new ArrayList<>(modules);
    result.removeIf(x -> !isModuleInCapabilities(capabilities, x));
    return result;
  }


  private static boolean isModuleInCapabilities(List<String> capabilities, Module module) {
    if(module.getMainModule() == null) {
      return false;
    }
    String moduleNamespace = module.getMainModule().getNamespace().getUri().toString();
    for (String capability : capabilities) {

      //direct match
      if (capability.equals(moduleNamespace)) {
        return true;
      }

      //match name
      int index = capability.indexOf("?");
      if(index > 0 && capability.subSequence(0, index).equals(moduleNamespace)) {
        return true;
      }

      //default capability
      if(capability.startsWith(Constants.NETCONF_BASE_CAPABILITY_NO_VERSION) && moduleNamespace.equals(Constants.NETCONF_NS)) {
        return true;
      }

    }
    return false;
  }


  public static List<String> loadCapabilities(String deviceFqn, String workspaceName) {
    DOM deviceDatatype = loadDeviceDatatype(deviceFqn, workspaceName);
    List<String> unknownMetaTags = deviceDatatype.getUnknownMetaTags();
    Document deviceMeta = loadDeviceMeta(unknownMetaTags);

    Element ele = XMLUtils.getChildElementByName(deviceMeta.getDocumentElement(), Constants.TAG_HELLO);
    if (ele != null) {
      return loadCapabilitiesFromHelloMessage(ele);
    }
    else {
      return loadCapabilitiesFromYangLibrary(XMLUtils.getChildElementByName(deviceMeta.getDocumentElement(), Constants.TAG_YANG_LIBRARY));
    }
  }

  private static List<String> loadCapabilitiesFromHelloMessage(Element ele) {
    ele = XMLUtils.getChildElementByName(ele, Constants.TAG_CAPABILITIES);
    List<Element> capabilities = XMLUtils.getChildElementsByName(ele, Constants.TAG_CAPABILITY);
    List<String> result = new ArrayList<String>();
    for (Element capability : capabilities) {
      result.add(capability.getTextContent());
    }
    return result;
  }

  private static List<String> loadCapabilitiesFromYangLibrary(Element ele) {
    List<String> result = new ArrayList<String>();
    List<Element> moduleSets = XMLUtils.getChildElementsByName(ele, Constants.TAG_MODULE_SET);
    for (Element moduleSet: moduleSets) {
      List<Element> modules = XMLUtils.getChildElementsByName(moduleSet, Constants.TAG_MODULE);
      for (Element module : modules) {
        result.add(XMLUtils.getChildElementByName(module, Constants.TAG_MODULE_NAME).getTextContent());
      }
    }
    return result;
  }


  private static Document loadDeviceMeta(List<String> unknownMetaTags) {
    try {
      for (String unknownMetaTag : unknownMetaTags) {
        Document d = XMLUtils.parseString(unknownMetaTag);
        boolean isYang = d.getDocumentElement().getTagName().equals(Constants.TAG_YANG);
        boolean isDevice = Constants.VAL_DEVICE.equals(d.getDocumentElement().getAttribute(Constants.ATT_YANG_TYPE));
        if (isYang && isDevice) {
          return d;
        }
      }
      throw new RuntimeException("No Device Meta Tag found");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static DOM loadDeviceDatatype(String deviceFqn, String workspace) {
    XynaFactoryControl factoryControl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    XmomDbInteraction interaction = new XmomDbInteraction();
    RevisionManagement rm = factoryControl.getRevisionManagement();
    Long revision;
    try {
      revision = rm.getRevision(null, null, workspace);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    RuntimeContextDependencyManagement rcdm = factoryControl.getRuntimeContextDependencyManagement();
    List<Long> revisions = new ArrayList<>(rcdm.getDependencies(revision));
    List<XMOMDatabaseSearchResultEntry> candidates = interaction.searchYangDTs(YangDevice.class.getCanonicalName(), revisions);
    for (XMOMDatabaseSearchResultEntry candidate : candidates) {
      if (candidate.getFqName().equals(deviceFqn)) {
        Long deviceRevision;
        try {
          deviceRevision = rm.getRevision(candidate.getRuntimeContext());
          DOM result = DOM.getOrCreateInstance(deviceFqn, new GenerationBaseCache(), deviceRevision);
          result.parseGeneration(true, false);
          return result;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    throw new RuntimeException("Could not find device datatype " + deviceFqn + " in " + workspace + " or dependencies");
  }

}
