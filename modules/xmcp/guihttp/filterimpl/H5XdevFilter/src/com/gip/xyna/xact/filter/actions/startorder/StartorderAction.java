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

package com.gip.xyna.xact.filter.actions.startorder;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.actions.startorder.json.StartorderRequestJson;
import com.gip.xyna.xact.filter.session.XMOMGuiReply.Status;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;



public class StartorderAction extends RuntimeContextDependendAction {
  private static final Logger logger = CentralFactoryLogging.getLogger(StartorderAction.class);
  public static final String BASE_PATH = "/" + PathElements.RTCS;
  private static final XynaFactoryControl xfctrl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
  private static final String documentFqn = "xact.templates.Document";
  private static final String ordertypeFqn = "xprc.xpce.OrderType";

  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/" + PathElements.START_ORDER) && Method.POST == method;
  }

  
  private GeneralXynaObject convertPayloadToWFInput(RuntimeContext rtc, HTTPTriggerConnection tc, RuntimeContext startOrderRTC) {
    MDMClassLoader classloader = null;
    try {
      Long revision = xfctrl.getRevisionManagement().getRevision(rtc);
      Long startOrderRevision = xfctrl.getRevisionManagement().getRevision(startOrderRTC);
      classloader = xfctrl.getClassLoaderDispatcher().getMDMClassLoader(ordertypeFqn, revision, true);
      StartorderRequestJson srj = new JsonParser().parse(tc.getPayload(), StartorderRequestJson.getJsonVisitor(startOrderRevision));

      GeneralXynaObject doc = prepareObject(documentFqn, classloader, "text", tc.getPayload());
      GeneralXynaObject ot = prepareObject(ordertypeFqn, classloader, "orderType", srj.getOrderType());

      return new Container(doc, ot);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private GeneralXynaObject prepareObject(String fqn, ClassLoader cl, String member, String value) throws Exception {
    Class<?> fqnClass = cl.loadClass(fqn);
    Object fqnObject = fqnClass.getDeclaredConstructor().newInstance();
    GeneralXynaObject casted = (GeneralXynaObject) fqnObject;
    casted.set(member, value);
    return casted;
  }


  private String validate(HTTPTriggerConnection tc, RuntimeContext startOrderRTC) throws XynaException {
    String validationProperty = H5XdevFilter.VALIDATION_WORKFLOW.get();
    if (validationProperty == null || validationProperty.isEmpty() || startOrderRTC.getName().equals(Utils.APP_NAME)) {
      return tc.getPayload();
    }
    int orderTypeSeparator = validationProperty.indexOf('@');
    String rtcAsString = validationProperty.substring(orderTypeSeparator + 1);
    RuntimeContext validationRTC = RuntimeContext.valueOf(rtcAsString);
    String validationOrderType = validationProperty.substring(0, orderTypeSeparator);
    GeneralXynaObject gxo = convertPayloadToWFInput(validationRTC, tc, startOrderRTC);
    DestinationKey dk = new DestinationKey(validationOrderType, validationRTC);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, gxo);
    xocp.setTransientCreationRole(getRole(tc));
    GeneralXynaObject result = XynaFactory.getInstance().getProcessing().startOrderSynchronously(xocp);
    return (String) result.get("text");
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc)
      throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    String payload = null; 

    try {
      payload = validate(tc, rc);
    } catch (XynaException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }


    XynaPlainSessionCredentials xpsc = AuthUtils.readCredentialsFromRequest(tc);
    Role role = getRole(tc);
    if(role == null) {
      AuthUtils.replyLoginRequiredError(tc, jfai);
      return jfai;
    }

    JsonParser jp = new JsonParser();
    StartorderRequestJson srj;

    try {
      srj = jp.parse(payload, StartorderRequestJson.getJsonVisitor(revision));
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }


    XynaOrderCreationParameter xocp = getXynaOrderCreationParameter(srj, rc);
    xocp.setPriority(srj.getPriority());

    xocp.setCustom0(srj.getCustomString(0));
    xocp.setCustom1(srj.getCustomString(1));
    xocp.setCustom2(srj.getCustomString(2));
    xocp.setCustom3(srj.getCustomString(3));

    xocp.setOrderInputSourceId(srj.getInputSourceId());
    xocp.setMonitoringLevel(srj.getMonitoringLevel());
    xocp.setSessionId(xpsc.getSessionId());
    xocp.setTransientCreationRole(role);

    XynaMultiChannelPortalSecurityLayer xynaMultiChannelPortalSecurityLayer =
        XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer();

    try {
      if (!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(Rights.START_ORDER.name(), role)) {
        String startOrderRight = ScopedRightUtils.getStartOrderRight(xocp.getDestinationKey());
        if (!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(startOrderRight, role)) {
          AuthUtils.replyError(tc, jfai, Status.forbidden, new XFMG_ACCESS_VIOLATION(startOrderRight, role.getName()));
          return jfai;
        }
      }
    } catch (XynaException e) {
      logger.debug("error during start order authorization", e);
      AuthUtils.replyError(tc, jfai, Status.forbidden, new RuntimeException("Authorization failed"));
      return jfai;
    }

    //check order entry
    if (rc instanceof Application) {
      Long myRevision = -1L;
      try {
        ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
        myRevision = clb.getRevision();
      } catch (Exception e) {
        AuthUtils.replyError(tc, jfai, Status.forbidden, new RuntimeException("Could not determine revision of Filter"));
        return jfai;
      }
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Application app = (Application) rc;
      Long calledRevision = rm.getRevision(app.getName(), app.getVersionName(), null);


      if (RevisionOrderControl.checkCustomOrderEntryClosed(calledRevision, myRevision, H5XdevFilter.ORDERENTRYNAME)) {
        AuthUtils.replyError(tc, jfai, Status.forbidden, new RuntimeException("Order entry disabled for application '" + app.getName() + "/" + app.getVersionName() + "'."));
        return jfai;
      }
    }


    if (srj.isAsync()) {
      JsonBuilder jb = new JsonBuilder();
      jb.startObject();
      Long l = xynaMultiChannelPortalSecurityLayer.startOrder(xocp, role);
      jb.addStringAttribute("orderID", Long.toString(l));
      jb.endObject();
      String s = jb.toString();
      jfai.sendJson(tc, s);
      return jfai;
    } else {
      //nicht synchron auftrag starten, der kann lange laufen und soll die filter-infrastruktur nicht blockieren. vgl bug 26386
      XynaOrder order = new XynaOrder(xocp);
      return new StartOrderActionInstance(order, rc);
    }
  }


  protected XynaOrderCreationParameter getXynaOrderCreationParameter(StartorderRequestJson srj, RuntimeContext rc) {
    GeneralXynaObject gxo;
    if (srj.getInput().size() == 1) {
      gxo = srj.getInput().get(0);
    } else {
      gxo = srj.getInput();
    }


    DestinationKey dk = new DestinationKey(srj.getOrderType(), rc);
    return new XynaOrderCreationParameter(dk, gxo);
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
