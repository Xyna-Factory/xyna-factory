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
package xdev.yang.impl;



import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;

import base.math.IntegerNumber;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xmcp.forms.plugin.Plugin;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;



public class PluginManagement {

  private static Plugin createPlugin(Class<?> creator) {
    Plugin.Builder result = new Plugin.Builder();
    result.definitionWorkflowFQN("xmcp.yang.fman.YangImportDefinition");
    RuntimeContext rtc = getOwnRtc(creator);
    String entryName = "Yang";
    if (rtc == null) {
      return null;
    }
    if (rtc instanceof Application) {
      entryName = entryName + " " + ((Application) rtc).getVersion();
    }
    result.navigationEntryLabel(entryName);
    result.navigationEntryName(entryName);
    result.path("manager");
    result.pluginRTC(rtc);
    return result.instance();
  }


  private static RuntimeContext getOwnRtc(Class<?> creator) {
    ClassLoaderBase clb = (ClassLoaderBase) creator.getClassLoader();
    Long revision = clb.getRevision();
    return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
  }


  public static void registerPlugin(Class<?> creator) {
    Plugin plugin = createPlugin(creator);
    xmcp.forms.plugin.PluginManagement.registerPlugin(plugin);
  }


  public static void unregisterPlugin(Class<?> creator) {
    Plugin plugin = createPlugin(creator);
    xmcp.forms.plugin.PluginManagement.unregisterPlugin(plugin);
  }
}
