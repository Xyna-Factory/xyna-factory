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
package xmcp.xypilot.impl;

import base.math.IntegerNumber;
import xfmg.xfctrl.appmgmt.RuntimeContextService;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.xypilot.XypilotUserConfig;
import xmcp.xypilot.impl.config.XypilotUserConfigStorage;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.yggdrasil.plugin.Context;
import xprc.xpce.RuntimeContext;
import xmcp.xypilot.CodeSuggestion;
import xmcp.xypilot.NoXyPilotUserConfigException;
import xmcp.xypilot.PromptGeneratorServiceOperation;

public class PromptGeneratorServiceOperationImpl implements ExtendedDeploymentTask, PromptGeneratorServiceOperation {

    private PluginManagement pluginManagement = new PluginManagement();

    public void onDeployment() throws XynaException {
      pluginManagement.registerPlugins(getOwnRtc());
      XypilotUserConfigStorage.init();
    }


    public void onUndeployment() throws XynaException {
      pluginManagement.unregisterPlugins(getOwnRtc());
    }


    public Long getOnUnDeploymentTimeout() {
      return null;
    }


    public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
      return null;
    }


    private RuntimeContext getOwnRtc() {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      Long revision = clb.getRevision();
      return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
    }

    @Override
    public void generate(XynaOrderServerExtension correlatedXynaOrder, Context context) {
      pluginManagement.generate(correlatedXynaOrder, context);
    }





    @Override
    public void storeUserConfig(XynaOrderServerExtension correlatedXynaOrder, XypilotUserConfig config) {
      XypilotUserConfigStorage storage = new XypilotUserConfigStorage(correlatedXynaOrder);
      String sessionId = correlatedXynaOrder.getSessionId();
      String user = XynaFactory.getInstance().resolveSessionToUser(sessionId);
      config.unversionedSetUser(user);
      try {
        storage.storeConfig(config);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }

    
    @Override
    public XypilotUserConfig loadUserConfig(XynaOrderServerExtension arg0) throws NoXyPilotUserConfigException {
      Generation generation = new Generation();
      try {
        return generation.getConfigFromOrder(arg0);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }


    @Override
    public List<? extends CodeSuggestion> generateCodeSuggestions(XynaOrderServerExtension order, Context context) {
      Generation generation = new Generation();
      try {
        return generation.genCodeSuggestions(order, context);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

}
