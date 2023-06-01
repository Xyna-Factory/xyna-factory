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

package com.gip.xyna.xact.filter.actions.generateinput;



import java.util.ArrayList;
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
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xprc.XynaOrderCreationParameter;



public class GenerateinputAction extends RuntimeContextDependendAction {

  public static final String BASE_PATH = "/" + PathElements.RTCS;
  public static final String RIGHT_NAME = "xfmg.xfctrl.orderInputSources:generate:*:*:*";


  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/" + PathElements.GENERATEINPUT) && Method.POST == method;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    if (!checkLoginAndRights(tc, jfai, RIGHT_NAME)) {
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
    XynaOrderCreationParameter xocp = management.generateOrderInput(storable.getId());
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();

    jb.addStringAttribute("orderType", xocp.getOrderType());

    try {
      jb.addNumberAttribute("inputSourceId", xocp.getOrderInputSourceId());
    } catch (NullPointerException e) {
      // Do nothing
    }
    try {
      jb.addIntegerAttribute("priority", xocp.getPriority());
    } catch (NullPointerException e) {
      // Do nothing
    }
    List<String> customStringContainerList = new ArrayList<String>();
    customStringContainerList.add(xocp.getCustom0());
    customStringContainerList.add(xocp.getCustom1());
    customStringContainerList.add(xocp.getCustom2());
    customStringContainerList.add(xocp.getCustom3());

    int i = 0;
    for (String s : customStringContainerList) {
      if (s == null) {
        s = "null";
        customStringContainerList.set(i, s);
      }
      i++;
    }

    jb.addStringListAttribute("customStringContainer", customStringContainerList);
    jb.addOptionalIntegerAttribute("monitoringLevel", xocp.getMonitoringLevel());
    XynaObjectJsonBuilder builder = new XynaObjectJsonBuilder(rc, jb);

    jb.addAttribute("output");

    if (!(xocp.getInputPayload() instanceof Container)) {
      jb.startList();
    }

    builder.buildJson(xocp.getInputPayload());

    if (!(xocp.getInputPayload() instanceof Container)) {
      jb.endList();
    }
    jb.endObject();

    String s = jb.toString();
    jfai.sendJson(tc, s);
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
