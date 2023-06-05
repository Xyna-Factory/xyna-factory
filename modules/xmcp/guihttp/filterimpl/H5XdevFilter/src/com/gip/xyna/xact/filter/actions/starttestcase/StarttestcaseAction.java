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

package com.gip.xyna.xact.filter.actions.starttestcase;



import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.actions.starttestcase.json.StarttestcaseRequestJson;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xprc.XynaOrderCreationParameter;



public class StarttestcaseAction extends RuntimeContextDependendAction {

  public static final String BASE_PATH = "/" + PathElements.RTCS;


  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/" + PathElements.START_TEST_CASE) && Method.POST == method;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    Role role = getRole(tc);
    if(role == null) {
      AuthUtils.replyLoginRequiredError(tc, jfai);
    }

    JsonParser jp = new JsonParser();
    StarttestcaseRequestJson srj;

    try {
      srj = jp.parse(tc.getPayload(), StarttestcaseRequestJson.getJsonVisitor());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }

    JsonBuilder jb = new JsonBuilder();

    OrderInputSourceManagement management =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

    List<OrderInputSourceStorable> list = management.getOrderInputSourcesForRevision(revision);
    OrderInputSourceStorable storable = null;
    Map<String, String> parameters;

    for (OrderInputSourceStorable oiss : list) {
      parameters = oiss.getParameters();

      if (parameters.containsValue(srj.getTestCaseName())) {
        storable = oiss;
        break;
      }
    }

    XynaOrderCreationParameter xocp;
    try {
      OptionalOISGenerateMetaInformation inputGeneratorParams = new OptionalOISGenerateMetaInformation();
      inputGeneratorParams.setValue("executingUser", srj.getUser());
      xocp = management.generateOrderInput(storable.getId(), inputGeneratorParams);
    } catch (NullPointerException ex) {
      return sendError(tc, jfai, jb, "Order Type for this Test Case is not set!", ex);
    } catch (NoSuchElementException ex) {
      return sendError(tc, jfai, jb, "EC-xfm.xtf.tcase.05", ex);
    }
    XynaMultiChannelPortalSecurityLayer xynaMultiChannelPortalSecurityLayer =
        XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer();

    xocp.setCustom0(srj.getTestCaseId().toString());
    xocp.setCustom1(srj.getTestCaseName());
    xocp.setCustom2(srj.getUser());

    jb.startObject();

    Long l = xynaMultiChannelPortalSecurityLayer.startOrder(xocp, role);
    jb.addStringAttribute("orderID", Long.toString(l));
    jb.endObject();
    String s = jb.toString();
    jfai.sendJson(tc, s);
    return jfai;
  }
  
  private JsonFilterActionInstance sendError(HTTPTriggerConnection tc, JsonFilterActionInstance jfai, JsonBuilder jb, String message, Exception ex) throws SocketNotAvailableException {
    jb.startObject();
    jb.addStringAttribute("errorMessage", message);
    if(ex != null) {
      StackTraceElement[] ste = ex.getStackTrace();
      jb.addAttribute("stackTrace");
      jb.startList();
      for (StackTraceElement s : ste) {
        jb.addStringListElement(s.toString());
      }
      jb.endList();
    }
    jb.endObject();
    jfai.sendJson(tc, HTTPTriggerConnection.HTTP_INTERNALERROR, jb.toString());
    return jfai;
  }


  @Override
  public String getTitle() {
    // Wird nie aufgerufen
    return null;
  }


  @Override
  public void appendIndexPage(HTMLPart body) {
    // Wird nie aufgerufen
  }


  @Override
  public boolean hasIndexPageChanged() {
    // Wird nie aufgerufen
    return false;
  }

}
