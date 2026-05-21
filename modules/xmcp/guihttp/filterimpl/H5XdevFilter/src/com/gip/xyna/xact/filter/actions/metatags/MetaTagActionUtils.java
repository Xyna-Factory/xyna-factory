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
package com.gip.xyna.xact.filter.actions.metatags;

import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMGui;

public class MetaTagActionUtils {

  /**
   * data types and service groups:
   * /xmom/{datatypes, servicegroups}/<path>/<name>/meta
   *
   * members and services:
   * /xmom/{datatypes, servicegroups}/<path>/<name>/{members, services}/<memberName or serviceName>/meta
   */
  public static boolean matchUrlRuntimeContextIndependent(URLPath url) {
    int pathLength = url.getPathLength();
    if (pathLength >= 5) {
      boolean startsWithXMOM = url.getPathElement(0).equals(PathElements.XMOM);
      boolean datatypeOrServicegroup = url.getPathElement(1).equals(PathElements.DATA_TYPES) || url.getPathElement(1).equals(PathElements.SERVICES_GROUPS);
      boolean endsWithMeta = url.getPathElement(pathLength-1).equals(PathElements.META);

      if (pathLength == 7) {
        boolean memberOrService = url.getPathElement(4).equals(PathElements.MEMBERS) || url.getPathElement(4).equals(PathElements.SERVICES);
        return startsWithXMOM && datatypeOrServicegroup && memberOrService && endsWithMeta;
      }

      return pathLength == 5 && startsWithXMOM && datatypeOrServicegroup && endsWithMeta;
    }

    return false;
  }

  public static MetaTagProcessingInfoContainer createProcessingInfoContainer(URLPath url, String sessionId, XMOMGui xmomGui, Long revision)  throws Exception {
    String fqn = url.getPathElement(2) + "." + url.getPathElement(3);
    String type = "";
    String elementName = "";
    if (url.getPathLength() == 7) { // url for members and services
      type = url.getPathElement(4);
      elementName = url.getPathElement(5);
    }
    FQName fqName = new FQName(revision, fqn);
    GenerationBaseObject gbo = xmomGui.getSessionBasedData(sessionId).load(fqName);
    return new MetaTagProcessingInfoContainer(type, elementName, gbo);
  }

  public static class MetaTagProcessingInfoContainer {
    private final String type;
    private final String elementName;
    private final GenerationBaseObject gbo;

    public MetaTagProcessingInfoContainer(String type, String elementName, GenerationBaseObject gbo) {
      this.type = type;
      this.elementName = elementName;
      this.gbo = gbo;
    }


    public String getType() {
      return type;
    }


    public String getElementName() {
      return elementName;
    }


    public GenerationBaseObject getGbo() {
      return gbo;
    }
  }
}
