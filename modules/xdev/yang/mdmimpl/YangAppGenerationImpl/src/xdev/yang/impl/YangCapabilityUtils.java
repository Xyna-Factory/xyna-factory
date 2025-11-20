/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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



import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.operation.ModuleGroup;
import xmcp.yang.YangDevice;



public class YangCapabilityUtils {

  private static final Logger logger = CentralFactoryLogging.getLogger(YangCapabilityUtils.class);

  public static boolean isModuleInCapabilities(List<YangDeviceCapability> capabilities, Module module) {
    if (module.getMainModule() == null) {
      return false;
    }
    String moduleNamespace = module.getMainModule().getNamespace().getUri().toString();
    for (YangDeviceCapability capability : capabilities) {
      String capabilityNameSpace = capability.getNameSpace();
      String capabilityRawInfo = capability.getRawInfo();

      // try direct match
      if (capabilityRawInfo.equals(moduleNamespace)) {
        return true;
      }

      // try to match namespace and revision dierectly
      int index = capabilityRawInfo.indexOf("?");
      if (index > 0 && capabilityRawInfo.subSequence(0, index).equals(moduleNamespace)) {
        return true;
      }

      // comparison for capabilities from a yang-library
      if (capabilityNameSpace != null && capabilityNameSpace.equals(moduleNamespace)) {
        String capabilityRevision = capability.getRevision();
        List<String> revisions = new ArrayList<String>();
        module.getRevisions().forEach(e -> {
          revisions.add(e.getArgStr());
        });
        if (capabilityRevision == null) {
          return true;
        }
        if (revisions.contains(capabilityRevision)) {
          return true;
        }
        return false;
      }

      // default capability
      if (capabilityRawInfo.startsWith(Constants.NETCONF_BASE_CAPABILITY_NO_VERSION) && moduleNamespace.equals(Constants.NETCONF_NS)) {
        return true;
      }
    }
    return false;
  }


  public static List<String> getSupportedFeatureNames(List<YangDeviceCapability> capabilities) {
    List<String> allFeatures = new ArrayList<String>();
    for (YangDeviceCapability c : capabilities) {
      List<String> capabilityFeatures = c.getFeatures();
      if (capabilityFeatures != null && !capabilityFeatures.isEmpty()) {
        allFeatures.addAll(capabilityFeatures);
      }
    }
    return allFeatures;
  }


  public static List<YangDeviceCapability> loadCapabilities(String deviceFqn, String workspaceName) {
    DOM deviceDatatype = loadDeviceDatatype(deviceFqn, workspaceName);
    List<String> unknownMetaTags = deviceDatatype.getUnknownMetaTags();
    return loadCapabilitiesImpl(unknownMetaTags);
  }
  
  public static List<YangDeviceCapability> loadCapabilitiesImpl(List<String> unknownMetaTags) {
    Document deviceMeta = loadDeviceMeta(unknownMetaTags);

    Element helloElement = XMLUtils.getChildElementByName(deviceMeta.getDocumentElement(), Constants.TAG_HELLO,
                                                          Constants.NETCONF_NS);
    if (helloElement != null) {
      return loadCapabilitiesFromHelloMessage(helloElement);
    } else {
      Element libElem = XMLUtils.getChildElementByName(deviceMeta.getDocumentElement(), Constants.TAG_YANG_LIBRARY,
                                                       Constants.YANG_LIB_NS);
      if (libElem == null) {
        throw new IllegalArgumentException("Could not find capabilities in xml string");
      }
      return loadCapabilitiesFromYangLibrary(libElem);
    }
  }


  public static List<YangDeviceCapability> loadCapabilitiesFromHelloMessage(Element ele) {
    ele = XMLUtils.getChildElementByName(ele, Constants.TAG_CAPABILITIES, ele.getNamespaceURI());
    List<Element> capabilityElements = XMLUtils.getChildElementsByName(ele, Constants.TAG_CAPABILITY, ele.getNamespaceURI());
    List<YangDeviceCapability> result = new ArrayList<YangDeviceCapability>();

    for (Element ce : capabilityElements) {
      YangDeviceCapability devCapability = new YangDeviceCapability();
      String textContent = ce.getTextContent().trim();
      devCapability.rawInfo = textContent;
      List<Pair<String, String>> queryList = createQueryList(textContent);
      result.add(devCapability);

      if (queryList.isEmpty()) {
        if(textContent.startsWith(Constants.NETCONF_CAPABILITY_URL)) {
          String featureName = textContent.substring(Constants.NETCONF_CAPABILITY_URL.length(), textContent.lastIndexOf(":"));
          devCapability.moduleName = "ietf-netconf";
          devCapability.nameSpace = Constants.NETCONF_NS;
          devCapability.features = Arrays.asList(featureName);
        }
       continue;
      }
      
      devCapability.nameSpace = textContent.split("\\?")[0];
      for (Pair<String, String> q : queryList) {
        String queryAttribute = q.getFirst();
        if (queryAttribute.equals(Constants.TAG_MODULE_REVISION)) {
          devCapability.revision = q.getSecond();
        }
        if (queryAttribute.equals(Constants.TAG_MODULE)) {
          devCapability.moduleName = q.getSecond();
        }
        if (queryAttribute.equals(Constants.TAG_MODULE_FEATURES)) {
          devCapability.features = Arrays.asList(q.getSecond().split(","));
        }
      }
    }

    return result;
  }
  
  private static List<Pair<String, String>> createQueryList(String textContent) {
    List<Pair<String, String>> queryList = new ArrayList<Pair<String, String>>();
    try {
      URI uri = new URI(textContent.replace(":", "/"));
      if (uri.getQuery() != null) {
        for (String kvp : uri.getQuery().split("&|;")) {
          String attribute = null;
          String value = null;
          int idx = kvp.indexOf('=');
          if (idx > 0) {
            attribute = kvp.substring(0, idx);
            value = kvp.substring(idx + 1);
          } else {
            attribute = kvp;
          }
          queryList.add(new Pair<String, String>(URLDecoder.decode(attribute, "UTF-8"), URLDecoder.decode(value, "UTF-8")));
        }
      }
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      logger.warn("Invalid capability format: " + textContent);
    }
    return queryList;
  }


  private static List<YangDeviceCapability> loadCapabilitiesFromYangLibrary(Element ele) {
    List<YangDeviceCapability> result = new ArrayList<YangDeviceCapability>();
    List<Element> moduleSets = XMLUtils.getChildElementsByName(ele, Constants.TAG_MODULE_SET, ele.getNamespaceURI());

    for (Element mset : moduleSets) {
      List<Element> modules = XMLUtils.getChildElementsByName(mset, Constants.TAG_MODULE, mset.getNamespaceURI());
      for (Element module : modules) {
        YangDeviceCapability devCapability = new YangDeviceCapability();
        devCapability.moduleName = XMLUtils.getChildElementByName(module, Constants.TAG_MODULE_NAME, module.getNamespaceURI())
                                           .getTextContent().trim();
        devCapability.nameSpace = XMLUtils.getChildElementByName(module, Constants.TAG_MODULE_NAMESPACE, module.getNamespaceURI())
                                          .getTextContent().trim();
        Element revisionElement = XMLUtils.getChildElementByName(module, Constants.TAG_MODULE_REVISION, module.getNamespaceURI());
        if (revisionElement != null) {
          devCapability.revision = revisionElement.getTextContent().trim();
        }

        List<String> features = new ArrayList<String>();
        XMLUtils.getChildElementsByName(module, Constants.TAG_MODULE_FEATURES, module.getNamespaceURI()).forEach(e -> {
          features.add(e.getTextContent().trim());
        });
        devCapability.features = features;

        result.add(devCapability);
      }
    }
    return result;
  }


  private static Document loadDeviceMeta(List<String> unknownMetaTags) {
    try {
      for (String unknownMetaTag : unknownMetaTags) {
        Document d = XMLUtils.parseString(unknownMetaTag, true);
        boolean isYang = d.getDocumentElement().getLocalName().equals(Constants.TAG_YANG);
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


  public static class YangDeviceCapability {

    private String moduleName;
    private String nameSpace;
    private String revision;
    private String rawInfo;
    private List<String> features;


    public String getModuleName() {
      return moduleName;
    }


    public String getNameSpace() {
      return nameSpace;
    }


    public String getRevision() {
      return revision;
    }


    public String getRawInfo() {
      if (rawInfo != null) { // for capabilities read from hello messages
        return rawInfo;
      } else { // for capabilities read from yang-library
        if (revision != null) {
          return nameSpace + ":" + revision;
        } else {
          return nameSpace;
        }
      }
    }


    public List<String> getFeatures() {
      return features;
    }

  }

}
