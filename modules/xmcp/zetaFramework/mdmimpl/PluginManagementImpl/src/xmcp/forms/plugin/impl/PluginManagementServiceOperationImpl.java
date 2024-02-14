/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package xmcp.forms.plugin.impl;


import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import java.util.List;
import xmcp.forms.plugin.Plugin;
import xmcp.forms.plugin.PluginManagementServiceOperation;


public class PluginManagementServiceOperationImpl implements ExtendedDeploymentTask, PluginManagementServiceOperation {
  
  public void onDeployment() throws XynaException {
    PluginStorage storage = new PluginStorage();
    storage.init();
  }

  public void onUndeployment() throws XynaException {
  }

  public Long getOnUnDeploymentTimeout() {
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }

  public List<? extends Plugin> listPlugins() {
    return new PluginStorage().listPlugins();
  }

  public void registerPlugin(Plugin plugin) {
    new PluginStorage().registerPlugin(plugin);
  }

  public void unregisterPlugin(Plugin plugin) {
    new PluginStorage().unregisterPlugin(plugin);
  }

}
