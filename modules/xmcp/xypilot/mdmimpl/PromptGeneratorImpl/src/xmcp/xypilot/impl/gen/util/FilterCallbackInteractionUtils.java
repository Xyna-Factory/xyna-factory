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
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLSourceAbstraction;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xact.http.URLPath;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.ContainerArea;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.ModellingItem;
import xmcp.processmodeller.datatypes.XMOMItem;
import xmcp.processmodeller.datatypes.response.GetXMLResponse;
import xmcp.processmodeller.datatypes.response.GetXMOMItemResponse;
import xmcp.xypilot.Documentation;
import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.Mapping;
import xmcp.xypilot.MappingAssignment;
import xmcp.xypilot.MemberReference;
import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.Parameter;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.locator.UnsavedChangesXmlSource;
import xmcp.xypilot.metrics.Code;



public class FilterCallbackInteractionUtils {

  private static final Logger logger = Logger.getLogger("XyPilot");

  private static final String h5xdevfilterCallbackName = "H5XdevFilter";
  private static final HTTPMethod httpGet = new GET();
  private static final HTTPMethod httpPut = new PUT();
  private static final HTTPMethod httpPost = new POST();
  private static final String getXmlUrlTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/xml";
  private static final String getDatatypeResponseUrlTemplate = "/runtimeContext/%s/xmom/%s/%s/%s";
  private static final String putChangeTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/objects/%s/change";
  private static final String putInsertTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/objects/%s/insert";

  public static DOM getDatatypeDom(XMOMItemReference ref, XynaOrderServerExtension order, String type) throws XynaException {
    Long revision = getRevision(ref);
    XMLSourceAbstraction inputSource = getXml(ref, order, type);
    DOM dom = DOM.getOrCreateInstance(ref.getFqName(), new GenerationBaseCache(), revision, inputSource);
    dom.parse(false);
    return dom;
  }

  public static ExceptionGeneration getException(XMOMItemReference ref, XynaOrderServerExtension order) throws XynaException {
    Long revision = getRevision(ref);
    XMLSourceAbstraction inputSource = getXml(ref, order, "exceptions");
    ExceptionGeneration exg = ExceptionGeneration.getOrCreateInstance(ref.getFqName(), new GenerationBaseCache(), revision, inputSource);
    exg.parse(false);
    return exg;
  }
  
  public static StepMapping getMapping(MemberReference ref, XynaOrderServerExtension order) throws XynaException {
    Long revision = getRevision(ref.getItem());
    XMLSourceAbstraction inputSource = getXml(ref.getItem(), order, "workflows");
    WF wf = (WF)GenerationBase.getOrCreateInstance(ref.getItem().getFqName(),new GenerationBaseCache(), revision, inputSource);
    wf.parse(false);
    StepMapping result = findStepMapping(ref.getMember().substring(4), wf.getWfAsStep());
    return result;
  }
  

  private static StepMapping findStepMapping(String stepId, Step step) {
    if (step instanceof StepMapping && stepId.equals(step.getStepId())) {
      return (StepMapping) step;
    }
    for (Step child : step.getChildSteps()) {
      StepMapping result = findStepMapping(stepId, child);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private static XMLSourceAbstraction getXml(XMOMItemReference ref, XynaOrderServerExtension order, String type) throws XynaException {
    Long revision = getRevision(ref);
    URLPath url = createUrlPath(getXmlUrlTemplate, ref, type);
    String xml = ((GetXMLResponse) order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpGet)).getCurrent();
    return new UnsavedChangesXmlSource(xml, ref.getFqName(), revision);
  }

  public static Item getDatatypeItemByAreaOrItemId(XMOMItemReference ref, XynaOrderServerExtension order, String id, String type) throws XynaException {
    XMOMItem parent = getXmomItem(ref, order, type);
    return findId(parent, id);
  }

  public static Item getExceptionItemByAreaOrItemId(XMOMItemReference ref, XynaOrderServerExtension order, String id) throws XynaException {
    XMOMItem parent = getXmomItem(ref, order, "exceptions");
    return findId(parent, id);
  }
 

  private static XMOMItem getXmomItem(XMOMItemReference ref, XynaOrderServerExtension order, String type) throws XynaException {
    URLPath url = createUrlPath(getDatatypeResponseUrlTemplate, ref, type);
    return ((GetXMOMItemResponse) order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpGet)).getXmomItem();
  }

  private static Item findId(Item item, String id) {
    if (item.getId() != null && item.getId().equals(id)) {
      return item;
    }
    if (item instanceof ModellingItem && ((ModellingItem)item).getAreas() != null) {
      for (Area area: ((ModellingItem) item).getAreas()) {
        Item ret = findId(item, area, id);
        if (ret != null) {
          return ret;
        }
      }
    }
    return null;
  }

  private static Item findId(Item parent, Area area, String id) {
    if (area.getId() != null && area.getId().equals(id)) {
      return parent;
    }
    if (area instanceof ContainerArea && ((ContainerArea)area).getItems() != null) {
      for (Item item: ((ContainerArea) area).getItems()) {
        Item ret = findId(item, id);
        if (ret != null) {
          return ret;
        }
      }
    }
    return null;
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

  public static void updateDomMethodImpl(Code code, XynaOrderServerExtension order, XMOMItemReference ref, String objectId) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, "datatypes", objectId);
    JsonBuilder payload = new JsonBuilder();
    payload.startObject();
    payload.addStringAttribute("implementation", code.getText());
    payload.endObject();
    order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
  }

  public static void updateExceptionVarDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref, String objectId) throws XynaException {
    updateDocu(docu, order, ref, "exceptions", objectId);
  }
  
  public static void updateMappingLabel(String label, XynaOrderServerExtension order, XMOMItemReference ref, String objectId) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, "workflows", objectId);
    JsonBuilder payload = new JsonBuilder();
    payload.startObject();
    payload.addStringAttribute("text", label);
    payload.endObject();
    order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
  }
  
  public static void updateMappingAssignments(Mapping mapping, XynaOrderServerExtension order, XMOMItemReference ref, String stepObjectId, int idx, String exp) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, "workflows", String.format("formula%s-%d_input", stepObjectId.substring(4), idx));
    JsonBuilder payload = new JsonBuilder();
    if(mapping.getExpressionCompletion() != null && !mapping.getExpressionCompletion().isEmpty()) {
      payload = new JsonBuilder();
      payload.startObject();
      payload.addObjectAttribute("content");
      payload.addStringAttribute("expression", exp + mapping.getExpressionCompletion());
      payload.endObject();
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPut, payload.toString());
    }
    
    url = createUrlPath(putInsertTemplate, ref, "workflows", String.format("formulaArea%s", stepObjectId.substring(4)));
    for(MappingAssignment assignment : mapping.getAssignments()) {
      payload = new JsonBuilder();
      payload.startObject();
      payload.addIntegerAttribute("index", -1);
      payload.addObjectAttribute("content");
      payload.addStringAttribute("expression", assignment.getExpression());
      payload.addStringAttribute("type", "formula");
      payload.addListAttribute("variables");
      payload.endList();
      payload.endObject();
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPost, payload.toString());
    }
  }

  private static void updateDocu(Documentation docu, XynaOrderServerExtension order, XMOMItemReference ref, String type, String objectId) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, type, objectId);
    JsonBuilder payload = new JsonBuilder();
    payload.startObject();
    payload.addStringAttribute("text", docu.getText());
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
      payload.addStringAttribute("label", var.getName());
      payload.addStringAttribute("documentation", var.getDocumentation());
      payload.addStringAttribute("primitiveType", var.getPrimitiveType());
      payload.addBooleanAttribute("isList", var.getIsList());
      payload.endObject();
      payload.endObject();
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPost, payload.toString());
    }
  }

  public static void addDomMethods(List<? extends MethodDefinition> methods, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    URLPath url = createUrlPath(putInsertTemplate, ref, "datatypes", "memberMethodsArea");
    for (MethodDefinition method: methods) {
      JsonBuilder payload = new JsonBuilder();
      payload.startObject();
      payload.addIntegerAttribute("index", -1);
      payload.addObjectAttribute("content");
      payload.addStringAttribute("type", "memberMethod");
      payload.addStringAttribute("label", method.getName());
      payload.addStringAttribute("documentation", method.getDocumentation());
      payload.addListAttribute("input");
      for (int index = 0; index < method.getInputParams().size(); index++) {
        Parameter para = method.getInputParams().get(index);
        payload.startObject();
        payload.addStringAttribute("label", para.getName());
        payload.addStringAttribute("fqn", para.getType());
        payload.addBooleanAttribute("isList", para.getIsList());
        payload.endObject();
      }
      payload.endList();
      payload.addListAttribute("output");
      for (Parameter para: method.getOutputParams()) {
        payload.startObject();
        payload.addStringAttribute("fqn", para.getType());
        payload.addBooleanAttribute("isList", para.getIsList());
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
      order.getRunnableForFilterAccess(h5xdevfilterCallbackName).execute(url, httpPost, payload.toString());
    }
  }

  public static void updateExceptionMessages(List<ExceptionMessage> exceptionMessages, XynaOrderServerExtension order, XMOMItemReference ref) throws XynaException {
    URLPath url = createUrlPath(putChangeTemplate, ref, "exceptions", "exceptionMessageArea");
    for (ExceptionMessage message: exceptionMessages) {
      JsonBuilder payload = new JsonBuilder();
      payload.startObject();
      payload.addStringAttribute("messageLanguage", message.getLanguage());
      payload.addStringAttribute("messageText", message.getMessage());
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
   * Gets the revision for the given workspace
   */
  public static long getRevision(XMOMItemReference item) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY{
    try {
      long parentRev = XynaFactory.getInstance().getRevision(null, null, item.getWorkspace());
      return XynaFactory.getInstance().getRevisionDefiningXMOMObjectOrParent(item.getFqName(), parentRev);
    } catch (Throwable e) {
      logger.warn("Couldn't generate revision of Workspace " + item.getWorkspace(), e);
      throw e;
    }
  }
}
