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

package com.gip.xyna.xact.filter.actions.orderinputdetails;



import java.util.List;
import java.util.Map;

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
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class OrderinputdetailsAction extends RuntimeContextDependendAction {

  public static final String BASE_PATH = "/" + PathElements.RTCS;


  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/" + PathElements.ORDER_INPUT_DETAILS) && Method.POST == method;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (!checkLoginAndRights(tc, jfai, UserManagement.ScopedRight.ORDER_INPUT_SOURCE.getKey())) {
      return jfai;
    }

    JsonParser jp = new JsonParser();
    StarttestcaseRequestJson srj;

    try {
      srj = jp.parse(tc.getPayload(), StarttestcaseRequestJson.getJsonVisitor());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }

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

    JsonBuilder jb = new JsonBuilder();

    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    jb.startObject();

    if (storable != null) {

      buildOrderInputJson(storable, rm, srj, jb);

    }

    jb.endObject();
    String s = jb.toString();
    jfai.sendJson(tc, s);
    return jfai;
  }


  public JsonBuilder buildOrderInputJson(OrderInputSourceStorable oiss, RevisionManagement rm, StarttestcaseRequestJson srj, JsonBuilder jb) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    jb.addStringAttribute("orderInput", oiss.getName());
    jb.addObjectAttribute("rtc", new RuntimeContextJson(rm.getRuntimeContext(oiss.getRevision())));
    jb.addStringAttribute("workstepsWithSources", "");
    jb.addStringAttribute("orderType", oiss.getOrderType());
    jb.addStringAttribute("inputGenerator", oiss.getParameters().get("orderTypeOfGeneratingWorkflow"));
    jb.addStringAttribute("testCaseName", srj.getTestCaseName());
    jb.addStringAttribute("documentation", oiss.getDocumentation());
    jb.addStringAttribute("state", oiss.getState());
    jb.addStringAttribute("tableName", oiss.getTableName());
    jb.addStringAttribute("id", String.valueOf(oiss.getId()));
    
    return jb;
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
