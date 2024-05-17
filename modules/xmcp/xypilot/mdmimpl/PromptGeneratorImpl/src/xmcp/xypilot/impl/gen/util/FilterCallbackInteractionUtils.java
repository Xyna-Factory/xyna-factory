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
package xmcp.xypilot.impl.gen.util;



import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLSourceAbstraction;

import xact.http.URLPath;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.PUT;
import xmcp.processmodeller.datatypes.response.GetXMLResponse;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.locator.UnsavedChangesXmlSource;
import xmcp.xypilot.Documentation;



public class FilterCallbackInteractionUtils {

  private static final Logger logger = Logger.getLogger("XyPilot");

  private static final String h5xdevfilterCallbackName = "H5XdevFilter";
  private static final HTTPMethod httpGet = new GET();
  private static final HTTPMethod httpPut = new PUT();
  private static final String getXmlUrlTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/xml";
  private static final String putObjDocTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/objects/documentationArea/change";


  public static DOM getDom(XMOMItemReference ref, XynaOrderServerExtension order) throws XynaException {
    Long revision = getRevision(ref);
    URLPath url = createUrlPath(getXmlUrlTemplate, ref, "datatypes");
    String xml = ((GetXMLResponse) order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpGet)).getCurrent();
    XMLSourceAbstraction inputSource = new UnsavedChangesXmlSource(xml, ref.getFqName(), revision);
    return DOM.getOrCreateInstance(ref.getFqName(), new GenerationBaseCache(), revision, inputSource);
  }


  public static void updateDomDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    URLPath url = createUrlPath(putObjDocTemplate, ref, "datatypes");
    String payload = " { \"text\": \"" + JsonUtils.escapeString(docu.getText()) + "\"}";
    order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload);
  }


  private static URLPath createUrlPath(String template, XMOMItemReference ref, String type) {
    String fqn = ref.getFqName();
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String name = fqn.substring(fqn.lastIndexOf(".") + 1);
    String ws = URLEncoder.encode(ref.getWorkspace(), Charset.defaultCharset());
    URLPath url = new URLPath.Builder().path(String.format(template, ws, type, path, name)).query(Collections.emptyList()).instance();
    return url;
  }


  /**
   * Gets the revision for the given workspace or -1 if an error occurs
   */
  public static long getRevision(XMOMItemReference item) {
    try {
      long parentRev = XynaFactory.getInstance().getRevision(null, null, item.getWorkspace());
      return XynaFactory.getInstance().getRevisionDefiningXMOMObjectOrParent(item.getFqName(), parentRev);
    } catch (Throwable e) {
      logger.warn("Couldn't generate revision of Workspace " + item.getWorkspace() + ". Return -1.", e);
      return -1L;
    }
  }
}
