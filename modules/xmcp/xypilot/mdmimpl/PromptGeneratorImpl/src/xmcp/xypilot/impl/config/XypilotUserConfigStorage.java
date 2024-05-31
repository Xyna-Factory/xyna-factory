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
package xmcp.xypilot.impl.config;



import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xmcp.xypilot.XypilotUserConfig;



public class XypilotUserConfigStorage {


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(XypilotUserConfigStorable.class);
  }


  public XypilotUserConfig loadConfig(String user) throws PersistenceLayerException {
    return convert(buildExecutor().execute(new LoadUserConfig(user)));
  }


  public void storeConfig(XypilotUserConfig config) throws PersistenceLayerException {
    buildExecutor().execute(new StoreUserConfig(convert(config)));
  }


  private XypilotUserConfigStorable convert(XypilotUserConfig config) {
    return new XypilotUserConfigStorable(config.getUser(), config.getUri(), config.getModel(), config.getMaxSuggestions());
  }


  private XypilotUserConfig convert(XypilotUserConfigStorable storable) {
    if (storable == null) {
      return null;
    }
    XypilotUserConfig.Builder builder = new XypilotUserConfig.Builder();
    builder.user(storable.getUser()).uri(storable.getXypiloturi()).model(storable.getModel()).maxSuggestions(storable.getMaxsuggestions());
    return builder.instance();
  }


  private static class LoadUserConfig implements WarehouseRetryExecutableNoException<XypilotUserConfigStorable> {

    private String user;


    public LoadUserConfig(String user) {
      this.user = user;
    }


    @Override
    public XypilotUserConfigStorable executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      XypilotUserConfigStorable storable = new XypilotUserConfigStorable(user);
      try {
        con.queryOneRow(storable);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return null;
      }

      return storable;
    }

  }

  private static class StoreUserConfig implements WarehouseRetryExecutableNoResult {

    private XypilotUserConfigStorable storable;


    public StoreUserConfig(XypilotUserConfigStorable storable) {
      this.storable = storable;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.persistObject(storable);
    }

  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(XypilotUserConfigStorable.class);
  }
}
