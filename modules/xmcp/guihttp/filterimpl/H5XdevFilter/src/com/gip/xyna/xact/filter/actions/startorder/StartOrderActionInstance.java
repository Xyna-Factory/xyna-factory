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
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrder;
import xmcp.xact.startorder.StartOrderExceptionResponse;
import xmcp.xact.startorder.StartOrderSuccessResponse;



class StartOrderActionInstance extends JsonFilterActionInstance {


  //TODO: remove after updating ZetaFramework
  public static final XynaPropertyBoolean selectRefCountProperty = new XynaPropertyBoolean("zeta.compatibility.startorder.withmeta", false)
      .setDefaultDocumentation(DocumentationLanguage.DE,
                               "Wenn gesetzt, wird die StartOrderResponse mit $meta-Informationen geliefert. *Achtung:* Erst Zeta > v0.6.4 kann eine StartOrderResponse mit $meta-Infos parsen.");


  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(StartOrderActionInstance.class);

  private transient JsonBuilder jb;
  private final RuntimeContext rtc;
  private final long orderId;
  private final XynaOrder order;


  public StartOrderActionInstance(XynaOrder order, RuntimeContext rtc) {
    this.orderId = order.getId();
    this.order = order;
    this.rtc = rtc;
  }


  private void startObject() {
    jb = new JsonBuilder();
    jb.startObject();
    jb.addStringAttribute("orderId", Long.toString(order.getId()));
  }


  @Override
  public void onResponse(GeneralXynaObject xo, HTTPTriggerConnection tc) {
    String s;
    if (selectRefCountProperty.get()) {
      s = modelledOnResponse(xo, tc);
    } else {
      s = unmodelledOnResponse(xo, tc);
      if (s == null) { //exception during unmodelledOnResponse
        return;
      }
    }
    try {
      sendJson(tc, s);
    } catch (SocketNotAvailableException e) {
      logger.trace(null, e);
    }
  }


  private String modelledOnResponse(GeneralXynaObject xo, HTTPTriggerConnection tc) {

    StartOrderSuccessResponse response = new StartOrderSuccessResponse();
    if (xo instanceof Container) {
      Container c = ((Container) xo);
      for (int i = 0; i < c.size(); i++) {
        response.addToOutput(c.get(i));
      }
    } else {
      response.addToOutput(xo);
    }

    response.setOrderId(Long.toString(orderId));
    long revision;
    long backupRevision = -1L;
    try {
      revision = Utils.getGuiHttpRevision();
      backupRevision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(rtc);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    long[] backupRevisions = {backupRevision};
    return Utils.xoToJson(response, revision, backupRevisions);

  }


  private String unmodelledOnResponse(GeneralXynaObject xo, HTTPTriggerConnection tc) {
    startObject();
    jb.addAttribute("output");

    if (!(xo instanceof Container)) {
      jb.startList(); //wenn es kein container ist, muss die listenwertigkeit manuell hergestellt werden
    }
    XynaObjectJsonBuilder builder;
    try {
      builder = new XynaObjectJsonBuilder(rtc, jb);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      sendError(tc, new RuntimeException("Could not serialize output of order " + orderId, e));
      return null;
    }
    builder.buildJson(xo);
    if (!(xo instanceof Container)) {
      jb.endList();
    }
    jb.endObject();
    return jb.toString();
  }


  @Override
  public void onError(XynaException[] xynaExceptions, HTTPTriggerConnection tc) {

    String s;
    if (selectRefCountProperty.get()) {
      s = modelledErrorResponse(xynaExceptions, tc);
    } else {
      s = unmodelledErrorResponse(xynaExceptions, tc);
    }

    try {
      sendJson(tc, s);
    } catch (SocketNotAvailableException e) {
      logger.trace(null, e);
    }
  }


  private String modelledErrorResponse(XynaException[] xynaExceptions, HTTPTriggerConnection tc) {
    StartOrderExceptionResponse response = new StartOrderExceptionResponse();
    response.setOrderId(Long.toString(orderId));
    long orderRtc = -1;
    long guiHttpRevision = -1;
    try {
      orderRtc = Utils.getRtcRevision(rtc);
      guiHttpRevision = Utils.getGuiHttpRevision();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
    }

    if(xynaExceptions == null) {
      response.setErrorMessage("Exception information missing.");
      return Utils.xoToJson(response, orderRtc, new long[] {guiHttpRevision});
    }
    
    Throwable t;
    boolean first;
    boolean multipleStackTraces = xynaExceptions.length > 1;
    if (xynaExceptions.length == 1 && xynaExceptions[0] != null) {
      response.setErrorMessage(xynaExceptions[0].getMessage());
    } else if (xynaExceptions.length > 1) {
      response.setErrorMessage("Multiple errors occurred.");
    }
    
    for (int i = 0; i < xynaExceptions.length; i++) {     
      XynaException xe = xynaExceptions[i];
      if(xe == null) {
        if(logger.isWarnEnabled()) {
          logger.warn("An Exception was null in order " + orderId);
        }
        continue;
      }
      t = xe;
      first = true;

      if (multipleStackTraces) {
        response.addToStackTrace("--- This is Stacktrace " + i + " of " + xynaExceptions.length + " ---");
      }

      while (t != null) {
        if (first) {
          first = false;
        } else {
          response.addToStackTrace("Caused by: " + t.getMessage());
        }
        StackTraceElement[] ste = t.getStackTrace();
        if(ste == null) {
          response.addToStackTrace("NO STACKTRACE INFORMATION AVAILABLE!");
          continue;
        }
        for (StackTraceElement s : ste) {
          response.addToStackTrace(s == null ? "NULL" : s.toString());
        }
        t = t.getCause();
      }

      if (xe instanceof XynaExceptionBase) {
        response.addToExceptions((XynaExceptionBase) xe);
      }
    }

    return Utils.xoToJson(response, orderRtc, new long[] {guiHttpRevision});
  }


  private String unmodelledErrorResponse(XynaException[] xynaExceptions, HTTPTriggerConnection tc) {
    startObject();
    Throwable t;
    if (xynaExceptions.length == 0) {
      t = new RuntimeException("no specific error");
    } else {
      t = xynaExceptions[0]; //TODO multiple fehler weitergeben?
    }
    jb.addStringAttribute("errorMessage", t.getMessage());
    jb.addAttribute("stackTrace");
    jb.startList();

    boolean first = true;
    while (t != null) {
      if (first) {
        first = false;
      } else {
        jb.addStringListElement("Caused by: " + t.getMessage());
      }
      StackTraceElement[] ste = t.getStackTrace();
      for (StackTraceElement s : ste) {
        jb.addStringListElement(s.toString());
      }

      t = t.getCause();
    }

    jb.endList();
    jb.endObject();
    return jb.toString();
  }


  @Override
  public FilterResponse filterResponse() throws XynaException {
    return FilterResponse.responsible(order);
  }


}