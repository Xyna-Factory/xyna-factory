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
package xmcp.forms.plugin.impl;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xmcp.forms.plugin.Plugin;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;



/**
 *
 * Interface between outside (PluginManagement) using xmcp.forms.plugin.Plugin
 * and internal data storage using xmcp.forms.plugin.impl.PluginStorable.
 */
public class PluginStorage {

  private static final String product = "zeta";
  private static final String context = "plugin";
  
  private static Logger logger = CentralFactoryLogging.getLogger(PluginStorage.class);
  private static final MessageBusManagementPortal messageBusManagementPortal = XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement();

  public void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(PluginStorable.class);
  }


  public void registerPlugin(Plugin plugin) {
    PluginStorable storable = convertToStorable(plugin);
    try {
      buildExecutor().execute(new RegisterPlugin(storable));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    publishMessageBusEvent("registration", storable.getId());
  }


  public List<Plugin> listPlugins() {
    List<Plugin> result = new ArrayList<Plugin>();
    Collection<PluginStorable> storables;
    try {
      storables = buildExecutor().execute(new ListPlugins());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    Plugin plugin;
    for (PluginStorable storable : storables) {
      try {
        plugin = convertFromStorable(storable);
        result.add(plugin);
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not convert Plugin Storable " + storable.getId(), e);
        }
      }
    }
    return result;
  }


  public void unregisterPlugin(Plugin plugin) {
    PluginStorable storable = convertToStorable(plugin);
    try {
      buildExecutor().execute(new UnregisterPlugin(storable));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    publishMessageBusEvent("removal", storable.getId());
  }


  private void publishMessageBusEvent(String usecase, String plugin) {
    List<SerializablePair<String, String>> payload = new ArrayList<>();
    payload.add(new SerializablePair<String, String>(usecase, plugin));
    MessageInputParameter para = new MessageInputParameter(product, context, usecase, "zeta", payload, false);
    try {
      Long id = messageBusManagementPortal.publish(para);
      if (logger.isDebugEnabled()) {
        logger.debug("published message bus event '" + id + "' for '" + usecase + "' of plugin '" + plugin + "'");
      }
    } catch (XynaException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("could not publish plugin update! " + e);
      }
    }
  }


  private Plugin convertFromStorable(PluginStorable storable) {
    Plugin.Builder builder = new Plugin.Builder();
    builder.navigationEntryLabel(storable.getNavigationentrylabel());
    builder.navigationEntryName(storable.getNavigationentryname());
    builder.navigationIconName(storable.getNavigationiconname());
    builder.definitionWorkflowFQN(storable.getDefinitionworkflowfqn());
    builder.pluginRTC(convertToRtc(storable.getPluginrtc()));
    builder.path(storable.getPath());
    return builder.instance();
  }


  private PluginStorable convertToStorable(Plugin plugin) {
    PluginStorable result = new PluginStorable();
    result.setNavigationentrylabel(plugin.getNavigationEntryLabel());
    result.setNavigationentryname(plugin.getNavigationEntryName());
    result.setNavigationiconname(plugin.getNavigationIconName());
    result.setDefinitionworkflowfqn(plugin.getDefinitionWorkflowFQN());
    result.setPluginrtc(convertRtc(plugin.getPluginRTC()));
    result.setPath(plugin.getPath());
    result.setId(PluginStorable.createId(result));
    return result;
  }


  private String convertRtc(RuntimeContext rtc) {
    if (rtc instanceof Workspace) {
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(((Workspace) rtc).getName()).serializeToString();
    } else if (rtc instanceof Application) {
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application(((Application) rtc).getName(), ((Application) rtc).getVersion())
          .serializeToString();
    }
    throw new RuntimeException("Unsupported RuntimeContext type: " + rtc.getClass());
  }


  private RuntimeContext convertToRtc(String rtc) {
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext r = com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.valueOf(rtc);
    if (r instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) {
      return new Workspace(r.getName());
    } else if (r instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application) {
      return new Application(r.getName(), ((com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application) r).getVersionName());
    }
    throw new RuntimeException("Could not convert '" + rtc + "' to RuntimeContext");
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(PluginStorable.class);
  }


  private static class UnregisterPlugin implements WarehouseRetryExecutableNoResult {

    private PluginStorable toUnregister;


    public UnregisterPlugin(PluginStorable toUnregister) {
      this.toUnregister = toUnregister;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteOneRow(toUnregister);
    }

  }

  private static class RegisterPlugin implements WarehouseRetryExecutableNoResult {

    private PluginStorable toRegister;


    public RegisterPlugin(PluginStorable toRegister) {
      this.toRegister = toRegister;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PluginStorable oldEntry = new PluginStorable();
      oldEntry.setId(toRegister.getId());
      
      try {
        con.queryOneRow(oldEntry);
        if(oldEntry.equals(toRegister)) {
          return; //PluginStorable exists already
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        //register new PluginStorable
      }
      
      con.persistObject(toRegister);
    }

  }

  private static class ListPlugins implements WarehouseRetryExecutableNoException<Collection<PluginStorable>> {

    @Override
    public Collection<PluginStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {

      return con.loadCollection(PluginStorable.class);
    }

  }
}
