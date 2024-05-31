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
package xmcp.xypilot.impl;



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.xypilot.Documentation;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.XypilotUserConfig;
import xmcp.xypilot.impl.config.XypilotUserConfigStorage;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.pipeline.Pipeline;
import xmcp.xypilot.impl.gen.util.FilterCallbackInteractionUtils;
import xmcp.xypilot.impl.locator.DataModelLocator;
import xmcp.xypilot.impl.locator.PipelineLocator;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.Workspace;



public class Generation {

  private XMOMItemReference buildItemFromContext(Context context) {
    XMOMItemReference.Builder builder = new XMOMItemReference.Builder();
    builder.fqName(context.getFQN());
    builder.workspace(((Workspace)context.getRuntimeContext()).getName());
    return builder.instance();
  }

  private String createCorrelation(XMOMItemReference ref, String type) {
    return String.format("%s-%s-WS:\"%s\"", type, ref.getFqName(), ref.getWorkspace());
  }


  private void publishUpdateMessage(XMOMItemReference xmomItemReference, String type) throws XynaException {
    String correlation = createCorrelation(xmomItemReference, type);
    MessageInputParameter message = new MessageInputParameter("Xyna", "Process Modeller Autosaves", correlation, "", null, false);
    XynaFactory.getInstance().Publish(message);
  }


  public void genDatatypeDocu(XynaOrderServerExtension order, Context context) throws Exception {
    XypilotUserConfig config = getConfigFromOrder(order);
    XMOMItemReference xmomItemReference = buildItemFromContext(context);
    DomModel model = DataModelLocator.getDomModel(xmomItemReference, order);
    Pipeline<Documentation, DomModel> pipeline = PipelineLocator.getPipeline(config, "dom-documentation");
    Documentation doc = pipeline.run(model, config.getUri()).firstChoice();
    FilterCallbackInteractionUtils.updateDomDocu(doc, order, xmomItemReference);
    publishUpdateMessage(xmomItemReference, "DataType");
  }

  public XypilotUserConfig getConfigFromOrder(XynaOrderServerExtension order) throws PersistenceLayerException {
    String sessionId = order.getSessionId();
    String user = XynaFactory.getInstance().resolveSessionToUser(sessionId);
    XypilotUserConfigStorage storage = new XypilotUserConfigStorage();
    XypilotUserConfig config = storage.loadConfig(user);
    if(config == null) {
      throw new RuntimeException("No XyPilot Config for user " + user);
    }
    return config;
  }

  @FunctionalInterface
  public interface GenerationInterface {

    void apply(XynaOrderServerExtension order, Context context) throws Exception;
  }
}
