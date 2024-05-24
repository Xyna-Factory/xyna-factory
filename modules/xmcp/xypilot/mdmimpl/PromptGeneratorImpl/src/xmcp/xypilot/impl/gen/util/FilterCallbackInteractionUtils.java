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
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
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
import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.Parameter;



public class FilterCallbackInteractionUtils {

  private static final Logger logger = Logger.getLogger("XyPilot");

  private static final String h5xdevfilterCallbackName = "H5XdevFilter";
  private static final HTTPMethod httpGet = new GET();
  private static final HTTPMethod httpPut = new PUT();
  private static final String getXmlUrlTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/xml";
  private static final String putChangeTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/objects/%s/change";
  private static final String putInsertTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/objects/%s/insert";

  public static DOM getDatatypeDom(XMOMItemReference ref, XynaOrderServerExtension order) throws XynaException {
    Long revision = getRevision(ref);
    XMLSourceAbstraction inputSource = getXml(ref, order, "datatypes");
    return DOM.getOrCreateInstance(ref.getFqName(), new GenerationBaseCache(), revision, inputSource);
  }
  
  public static ExceptionGeneration getException(XMOMItemReference ref, XynaOrderServerExtension order) throws XynaException {
    Long revision = getRevision(ref);
    XMLSourceAbstraction inputSource = getXml(ref, order, "exceptions");
    return ExceptionGeneration.getOrCreateInstance(ref.getFqName(), new GenerationBaseCache(), revision, inputSource);
  }

  private static XMLSourceAbstraction getXml(XMOMItemReference ref, XynaOrderServerExtension order, String type) throws XynaException {
    Long revision = getRevision(ref);
    URLPath url = createUrlPath(getXmlUrlTemplate, ref, type);
    String xml = ((GetXMLResponse) order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpGet)).getCurrent();
    return new UnsavedChangesXmlSource(xml, ref.getFqName(), revision);
  }

  public static void updateDomDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    updateDocu(docu, order, ref, "datatypes", "documentationArea");
  }
  
  public static void updateExceptionDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    updateDocu(docu, order, ref, "exceptions", "documentationArea");
  }
  
  public static void updateDomVarDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref, String objectId) throws XynaException {
    updateDocu(docu, order, ref, "datatypes", objectId);
  }
  
  public static void updateExceptionVarDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref, String objectId) throws XynaException {
    updateDocu(docu, order, ref, "exceptions", objectId);
  }
    
  private static void updateDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref, String type, String objectId) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, type, objectId);
    JsonBuilder payload = new JsonBuilder();
    payload.startObject();
    payload.addStringAttribute("text", JsonUtils.escapeString(docu.getText()));
    payload.endObject();
    order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
  }
  
  public static void addDomVars(List<? extends MemberVariable> vars, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    addVars(vars, order, ref, "datatypes");
  }
  
  public static void addExceptionVars(List<? extends MemberVariable> vars, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    addVars(vars, order, ref, "exceptions");
  }
  
  private static void addVars(List<? extends MemberVariable> vars, XynaOrderServerExtension order, XMOMItemReference ref, String type) throws XynaException {
    URLPath url = createUrlPath(putInsertTemplate, ref, type, "memberVarArea");
    for (MemberVariable var: vars) {
      JsonBuilder payload = new JsonBuilder();
      payload.startObject();
      payload.addIntegerAttribute("index", -1);
      payload.addObjectAttribute("content");
      payload.addStringAttribute("type", "memberVar");
      payload.addStringAttribute("label", JsonUtils.escapeString(var.getName()));
      payload.addStringAttribute("documentation", JsonUtils.escapeString(var.getDocumentation()));
      payload.addStringAttribute("primitiveType", JsonUtils.escapeString(var.getPrimitiveType()));
      payload.addBooleanAttribute("isList", var.getIsList());
      payload.endObject();
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
    }
  }
  
  public static void addDomMethods(List<? extends MethodDefinition> methods, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    URLPath url = createUrlPath(putInsertTemplate, ref, "datatypes", "memberVarArea");
    for (MethodDefinition method: methods) {
      JsonBuilder payload = new JsonBuilder();
      payload.startObject();
      payload.addIntegerAttribute("index", -1);
      payload.addObjectAttribute("content");
      payload.addStringAttribute("type", "memberMethod");
      payload.addStringAttribute("label", JsonUtils.escapeString(method.getName()));
      payload.addStringAttribute("documentation", JsonUtils.escapeString(method.getDocumentation()));
      payload.addListAttribute("input");
      for (int index = 0; index < method.getInputParams().size(); index++) {
        Parameter para = method.getInputParams().get(index);
        payload.startObject();
        payload.addObjectAttribute("content");
        payload.addStringAttribute("label", para.getName());
        payload.addStringAttribute("fqn", para.getType());
        payload.addStringAttribute("name", para.getName() + index);
        payload.addBooleanAttribute("isList", para.getIsList());
        payload.endObject();
        payload.endObject();
      }
      payload.endList();
      payload.addListAttribute("output");
      for (Parameter para: method.getOutputParams()) {
        payload.startObject();
        payload.addObjectAttribute("content");
        payload.addStringAttribute("fqn", para.getType());
        payload.addBooleanAttribute("isList", para.getIsList());
        payload.endObject();
        payload.endObject();
      }
      payload.endList();
      payload.addListAttribute("throws");
      for (Parameter para: method.getThrowParams()) {
        payload.startObject();
        payload.addStringAttribute("fqn", para.getType());
        payload.endObject();
      }
      payload.endList();
      payload.addStringAttribute("implementation", method.getOutputParams().size() > 0 ? "return null" : "");
      payload.endObject();
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
    }
  }
  
  public static void updateExceptionMessages(List<ExceptionMessage> exceptionMessages, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, "exceptions", "exceptionMessageArea");
    for (ExceptionMessage message: exceptionMessages) {
      JsonBuilder payload = new JsonBuilder();
      payload.startObject();
      payload.addStringAttribute("messageLanguage", JsonUtils.escapeString(message.getLanguage()));
      payload.addStringAttribute("messageText", JsonUtils.escapeString(message.getMessage()));
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
    }
  }

  private static URLPath createUrlPath(String template, XMOMItemReference ref, String type) {
    return createUrlPath(template, ref, type, "");
  }
  
  private static URLPath createUrlPath(String template, XMOMItemReference ref, String type, String objectid) {
    String fqn = ref.getFqName();
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String name = fqn.substring(fqn.lastIndexOf(".") + 1);
    String ws = URLEncoder.encode(ref.getWorkspace(), Charset.defaultCharset());
    URLPath url = new URLPath.Builder().path(String.format(template, ws, type, path, name, objectid)).query(Collections.emptyList()).instance();
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
