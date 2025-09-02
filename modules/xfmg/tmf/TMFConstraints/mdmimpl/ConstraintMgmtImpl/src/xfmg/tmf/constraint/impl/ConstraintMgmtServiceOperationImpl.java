/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.constraint.impl;



import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLSourceAbstraction;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import base.math.IntegerNumber;
import xfmg.tmf.constraint.ConstraintMgmtServiceOperation;
import xfmg.tmf.constraint.ContextData;
import xfmg.tmf.constraint.data_model.TMFConstraint;
import xfmg.tmf.constraint.TMFConstraintTableData;
import xact.http.URLPath;
import xact.http.URLPathQuery;
import xact.http.enums.httpmethods.DELETE;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.PUT;
import xact.templates.JSON;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xfmg.xfctrl.datamodel.json.JSONDatamodelServices;
import xmcp.forms.plugin.Plugin;
import xmcp.processmodeller.datatypes.response.GetXMLResponse;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;



public class ConstraintMgmtServiceOperationImpl implements ExtendedDeploymentTask, ConstraintMgmtServiceOperation {

  private static Logger logger = CentralFactoryLogging.getLogger(ConstraintMgmtServiceOperationImpl.class);

  private static final String filterName = "H5XdevFilter";
  private static final String getXmlUrlTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/xml";
  private static final String metaTagEndpointTemplate = "/runtimeContext/%s/xmom/%s/%s/%s/meta";


  public void onDeployment() throws XynaException {
    mangagePlugins(xmcp.forms.plugin.PluginManagement::registerPlugin);
  }


  public void onUndeployment() throws XynaException {
    mangagePlugins(xmcp.forms.plugin.PluginManagement::unregisterPlugin);
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }


  public List<? extends TMFConstraint> getConstraints(XynaOrderServerExtension order, Context context) {
    Pair<Integer, Document> constraintsMeta = loadConstraintsMeta(order, context);
    if (constraintsMeta == null) {
      return new ArrayList<TMFConstraint>();
    }
    return getConstraints(constraintsMeta.getSecond());
  }


  @SuppressWarnings("unchecked")
  private List<? extends TMFConstraint> getConstraints(Document constraintsDocument) {
    if (constraintsDocument == null) {
      return new ArrayList<TMFConstraint>();
    }
    String content = constraintsDocument.getDocumentElement().getTextContent();
    List<?> tmp = (List<?>) JSONDatamodelServices.parseListFromJSON(new xact.templates.Document(new JSON(), content), new TMFConstraint());
    return (List<TMFConstraint>) tmp;
  }


  @SuppressWarnings("unchecked")
  public void setConstraint(XynaOrderServerExtension order, TMFConstraintTableData tMFConstraintTableData) {
    ContextData contextData = tMFConstraintTableData.getContextData();

    Pair<Integer, Document> constraintsMeta = loadConstraintsMeta(order, contextData);
    List<TMFConstraint> constraintsList = (List<TMFConstraint>) getConstraints(constraintsMeta.getSecond());
    // change selected table entry
    constraintsList.set(tMFConstraintTableData.getContextData().getDataIndex(), tMFConstraintTableData.getConstraint());

    replaceMetaTag(order, contextData, constraintsMeta.getFirst(), constraintsList);
  }


  @SuppressWarnings("unchecked")
  public void addConstraint(XynaOrderServerExtension order, TMFConstraintTableData tMFConstraintTableData) {
    ContextData contextData = tMFConstraintTableData.getContextData();

    Pair<Integer, Document> constraintsMeta = loadConstraintsMeta(order, contextData);
    List<TMFConstraint> constraintsList = (List<TMFConstraint>) getConstraints(constraintsMeta.getSecond());
    // append entry
    constraintsList.add(tMFConstraintTableData.getConstraint());

    replaceMetaTag(order, contextData, constraintsMeta.getFirst(), constraintsList);
  }


  @SuppressWarnings("unchecked")
  public void deleteConstraint(XynaOrderServerExtension order, TMFConstraintTableData tMFConstraintTableData) {
    ContextData contextData = tMFConstraintTableData.getContextData();

    Pair<Integer, Document> constraintsMeta = loadConstraintsMeta(order, contextData);
    List<TMFConstraint> constraintsList = (List<TMFConstraint>) getConstraints(constraintsMeta.getSecond());
    // append entry
    constraintsList.remove(tMFConstraintTableData.getContextData().getDataIndex());

    replaceMetaTag(order, contextData, constraintsMeta.getFirst(), constraintsList);
  }


  private Pair<Integer, Document> loadConstraintsMeta(XynaOrderServerExtension order, ContextData contextData) {
    Workspace workspace = (Workspace) contextData.getRuntimeContext();
    return loadConstraintsMeta(order, new Context(contextData.getFqn(), "", "", workspace));
  }


  private Pair<Integer, Document> loadConstraintsMeta(XynaOrderServerExtension order, Context context) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      RuntimeContext rtc = context.getRuntimeContext();
      Long revision;
      if (rtc instanceof Workspace) {
        revision = revMgmt.getRevision(null, null, ((Workspace) rtc).getName());
      } else {
        Application rtcApp = (Application) rtc;
        revision = revMgmt.getRevision(rtcApp.getName(), rtcApp.getVersion(), null);
      }

      XMLSourceAbstraction xmlSource = getXml(revision, context, order, "datatypes");
      DOM dom = DOM.getOrCreateInstance(context.getFQN(), new GenerationBaseCache(), revision, xmlSource);
      dom.parse(false);

      List<String> unknownMetaTags = dom.getUnknownMetaTags();
      for (int i = 0; i < unknownMetaTags.size(); i++) {
        String unknownMetaTag = unknownMetaTags.get(i);
        Document d = XMLUtils.parseString(unknownMetaTag);
        if (d.getDocumentElement().getTagName().equals("TMFConstraints")) {
          return new Pair<Integer, Document>(i, d);
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }


  /*
   * get xml in the current, unsaved state
   */
  private static XMLSourceAbstraction getXml(Long revision, Context context, XynaOrderServerExtension order, String type)
      throws XynaException {
    URLPath url = createUrlPath(getXmlUrlTemplate, context.getFQN(), ((Workspace) context.getRuntimeContext()).getName(), type);
    String xml = ((GetXMLResponse) order.getRunnableForFilterAccess(filterName).execute(url, new GET())).getCurrent();
    return new UnsavedChangesXmlSource(xml, context.getFQN(), revision);
  }


  private void replaceMetaTag(XynaOrderServerExtension order, ContextData contextData, Integer tagNumber,
                              List<TMFConstraint> constraintsList) {
    Workspace workspace = (Workspace) contextData.getRuntimeContext();
    URLPath url = createUrlPath(metaTagEndpointTemplate, contextData.getFqn(), workspace.getName(), "datatypes");
    String payload = buildConstraintsPayload(constraintsList);

    try {
      RunnableForFilterAccess runnable = order.getRunnableForFilterAccess(filterName);

      // remove old meta tag
      List<URLPathQuery> query = new ArrayList<>();
      query.add(new URLPathQuery.Builder().attribute("metaTagId").value("metaTag" + tagNumber).instance());
      url.setQuery(query);
      runnable.execute(url, new DELETE(), "");

      // add new meta tag
      url.setQuery(Collections.emptyList());
      runnable.execute(url, new PUT(), payload);
    } catch (Exception e) {
      throw new RuntimeException("Could not update tmf constraints");
    }
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private String buildConstraintsPayload(List<TMFConstraint> constraintsList) {
    String constraintsJson = JSONDatamodelServices.writeJSONList((List) constraintsList).getText();
    // remove new lines, change already existing \ to \\ and properly escape double quotes for the payload
    constraintsJson = constraintsJson.replaceAll("\n", "").replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
    constraintsJson = constraintsJson.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    String payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},"
        + "\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\"" + "<TMFConstraints>"
        + constraintsJson + "</TMFConstraints>\"}}";
    return payload;
  }


  private static URLPath createUrlPath(String template, String fqn, String workspaceName, String type) {
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String name = fqn.substring(fqn.lastIndexOf(".") + 1);
    String workspaceNameEscaped = URLEncoder.encode(workspaceName, Charset.defaultCharset());
    URLPath url = new URLPath.Builder().path(String.format(template, workspaceNameEscaped, type, path, name, ""))
        .query(Collections.emptyList()).instance();
    return url;
  }


  private void mangagePlugins(Consumer<Plugin> consumer) {
    Plugin.Builder builder = new Plugin.Builder();
    builder.pluginRTC(getOwnRtc());
    builder.definitionWorkflowFQN("xfmg.tmf.constraint.uidefinition.ConstraintsTableDefinition");
    builder.navigationEntryName("Constraints Table");
    builder.navigationEntryLabel("Constraints Table");
    builder.path("modeller/datatype");
    consumer.accept(builder.instance());
  }


  private RuntimeContext getOwnRtc() {
    ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
    Long revision = clb.getRevision();
    return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
  }
}
