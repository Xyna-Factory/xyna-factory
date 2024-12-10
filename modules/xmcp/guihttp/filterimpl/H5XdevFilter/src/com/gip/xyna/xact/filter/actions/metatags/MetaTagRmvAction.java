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



import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.URLPath.URLPathQuery;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.actions.metatags.MetaTagActionUtils.MetaTagProcessingInfoContainer;
import com.gip.xyna.xact.filter.actions.startorder.Endpoint;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.gb.ObjectId.ObjectIdPrefix;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;



public class MetaTagRmvAction extends RuntimeContextDependendAction implements Endpoint {

  private XMOMGui xmomGui;


  private static final Map<String, MetaTagRmvFunction> metaTagRmvFunctions = setupMetaTagRmvFunctions();

  private static final Map<String, MetaTagRmvFunction> setupMetaTagRmvFunctions() {
    Map<String, MetaTagRmvFunction> result = new HashMap<>();
    result.put(PathElements.MEMBERS, MetaTagRmvAction::rmvMemberMetaTag);
    result.put(PathElements.SERVICES, MetaTagRmvAction::rmvMethodMetaTag);
    result.put("", MetaTagRmvAction::rmvDocumentMetaTag);
    return result;
  }

  public MetaTagRmvAction(XMOMGui xmomGui) {
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
    return MetaTagActionUtils.matchUrlRuntimeContextIndependent(url) && method == Method.DELETE;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {

    JsonFilterActionInstance actionInstance = new JsonFilterActionInstance();
    if (!checkLoginAndRights(tc, actionInstance, GuiRight.PROCESS_MODELLER.getKey(), Rights.EDIT_MDM.toString())) {
      return actionInstance;
    }

    try {
      rmvMetaTag(getSession(tc).getId(), revision, url);
    } catch (Exception e) {
      AuthUtils.replyError(tc, actionInstance, e);
    }
    GenerationBaseObject gbo = xmomGui.getGbo(getSession(tc), rc, revision, url.getPathElement(2), url.getPathElement(3));
    actionInstance.sendJson(tc, gbo.buildXMOMGuiReply().getJson());
    return actionInstance;
  }


  @Override
  public GeneralXynaObject execute(XynaPlainSessionCredentials creds, URLPath url, Method method, String payload) {
    try {
      RTCInfo info = extractRTCInfo(url);
      URLPath urlNoRtc = url.subURL(2);
      rmvMetaTag(creds.getSessionId(), info.revision, urlNoRtc);
    } catch (Exception e) {
    }
    return null;
  }


  private void rmvMetaTag(String sessionId, Long revision, URLPath url) throws Exception {
    URLPathQuery id = url.getQuery("metaTagId");
    MetaTagProcessingInfoContainer data = MetaTagActionUtils.createProcessingInfoContainer(url, sessionId, xmomGui, revision);
    int index = Integer.valueOf(ObjectIdPrefix.metaTag.getBaseId(id.getValue()));
    MetaTagRmvFunction func = metaTagRmvFunctions.get(data.getType());
    func.rmvMetaTag(data.getGbo(), data.getElementName(), index);
  }


  private static void rmvMemberMetaTag(GenerationBaseObject gbo, String objectName, int index) {
    AVariable member = gbo.getDOM().getMemberVars().stream().filter(x -> x.getVarName().equals(objectName)).findFirst().get();
    member.getUnknownMetaTags().remove(index);
  }


  private static void rmvMethodMetaTag(GenerationBaseObject gbo, String objectName, int index) throws XynaException {
    Operation method = gbo.getDOM().getOperationByName(objectName);
    method.getUnknownMetaTags().remove(index);
  }


  private static void rmvDocumentMetaTag(GenerationBaseObject gbo, String objectName, int index) throws XynaException {
    DOM document = gbo.getDOM();
    document.getUnknownMetaTags().remove(index);
  }


  private interface MetaTagRmvFunction {

    void rmvMetaTag(GenerationBaseObject gbo, String objectName, int index) throws XynaException;
  }
}
