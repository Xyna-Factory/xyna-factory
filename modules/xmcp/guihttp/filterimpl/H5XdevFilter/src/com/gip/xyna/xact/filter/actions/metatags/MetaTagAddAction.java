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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.actions.metatags.MetaTagActionUtils.MetaTagProcessingInfoContainer;
import com.gip.xyna.xact.filter.actions.startorder.Endpoint;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;

import xmcp.processmodeller.datatypes.MetaTag;
import xmcp.processmodeller.datatypes.request.MetaTagRequest;



public class MetaTagAddAction extends RuntimeContextDependendAction implements Endpoint {

  private XMOMGui xmomGui;

  
  private static final Map<String, MetaTagAddFunction> metaTagAddFunctions = setupMetaTagAddFunctions();
  
  private static final Map<String, MetaTagAddFunction> setupMetaTagAddFunctions() {
    Map<String, MetaTagAddFunction> result = new HashMap<>();
    result.put(PathElements.MEMBERS, MetaTagAddAction::addMemberMetaTag);
    result.put(PathElements.SERVICES, MetaTagAddAction::addMethodMetaTag);
    return result;
  }

  public MetaTagAddAction(XMOMGui xmomGui) {
    this.xmomGui = xmomGui;
  }


  @Override
  public void appendIndexPage(HTMLPart arg0) {
  }


  @Override
  public String getTitle() {
    return null;
  }


  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }


  @Override
  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return MetaTagActionUtils.matchUrlRuntimeContextIndependent(url) && method == Method.PUT;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {

    JsonFilterActionInstance actionInstance = new JsonFilterActionInstance();
    if (!checkLoginAndRights(tc, actionInstance, GuiRight.PROCESS_MODELLER.getKey(), Rights.EDIT_MDM.toString())) {
      return actionInstance;
    }

    try {
      addMetaTag(getSession(tc).getId(), revision, url, tc.getPayload());
    } catch (Exception e) {
      AuthUtils.replyError(tc, actionInstance, e);
    }
    return actionInstance;
  }


  @Override
  public GeneralXynaObject execute(XynaPlainSessionCredentials creds, URLPath url, Method method, String payload) {
    try {
      RTCInfo info = extractRTCInfo(url);
      URLPath urlNoRtc = url.subURL(2);
      addMetaTag(creds.getSessionId(), info.revision, urlNoRtc, payload);
    } catch (Exception e) {
    }
    return null;
  }


  private void addMetaTag(String sessionId, Long revision, URLPath url, String payload) throws Exception {
    Long guiHttpRevision = Utils.getGuiHttpApplicationRevision();
    MetaTag metaTag = ((MetaTagRequest) Utils.convertJsonToGeneralXynaObject(payload, guiHttpRevision)).getMetaTag();
    MetaTagProcessingInfoContainer data = MetaTagActionUtils.createProcessingInfoContainer(url, sessionId, xmomGui, revision);
    String tag = metaTag.getTag();
    MetaTagAddFunction func = metaTagAddFunctions.get(data.getType());
    func.addMetaTag(data.getGbo(), data.getElementName(), tag);
  }


  private static void addMemberMetaTag(GenerationBaseObject gbo, String objectName, String content) {
    AVariable member = gbo.getDOM().getMemberVars().stream().filter(x -> x.getVarName().equals(objectName)).findFirst().get();
    List<String> unknownMetaTags = member.getUnknownMetaTags() != null ? member.getUnknownMetaTags() : new ArrayList<String>();
    unknownMetaTags.add(content);
    member.setUnknownMetaTags(unknownMetaTags);
  }


  private static void addMethodMetaTag(GenerationBaseObject gbo, String objectName, String content) throws XynaException {
    Operation method = gbo.getDOM().getOperationByName(objectName);
    List<String> unknownMetaTags = method.getUnknownMetaTags() != null ? method.getUnknownMetaTags() : new ArrayList<String>();
    unknownMetaTags.add(content);
    method.setUnknownMetaTags(unknownMetaTags);
  }


  private interface MetaTagAddFunction {

    void addMetaTag(GenerationBaseObject gbo, String objectName, String content) throws XynaException;
  }
}
