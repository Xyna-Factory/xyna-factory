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

package com.gip.xyna.xact.filter;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;


public class NetConfNotificationReceiverConfigurationParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<String> ORDER_TYPE =
      StringParameter.typeString("order-type").documentation(Documentation.de("Vom Filter zu startender Order-Type")
          .en("Order type the filter will try to start").build()).mandatory().build();
  public static final StringParameter<String> WORKSPACE =
      StringParameter.typeString("workspace").documentation(Documentation.de("Workspace des Order-Types")
          .en("Workspace of the order type").build()).optional().build();
  public static final StringParameter<String> APPLICATION_NAME =
      StringParameter.typeString("application-name").documentation(Documentation.de("Application-Name des Order-Types")
          .en("Application name of the order type").build()).optional().build();
  public static final StringParameter<String> APPLICATION_VERSION =
      StringParameter.typeString("application-version").documentation(Documentation.de("Application-Version des Order-Types")
          .en("Application version of the order type").build()).optional().build();

  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
      StringParameter.asList(ORDER_TYPE, WORKSPACE, APPLICATION_NAME, APPLICATION_VERSION);

  private String orderType;
  private Optional<String> workspace;
  private Optional<String> applicationName;
  private Optional<String> applicationVersion;


  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }


  @Override
  public NetConfNotificationReceiverConfigurationParameter build(Map<String, Object> map)
      throws XACT_InvalidFilterConfigurationParameterValueException {
    NetConfNotificationReceiverConfigurationParameter param = new NetConfNotificationReceiverConfigurationParameter();
    param.orderType = ORDER_TYPE.getFromMap(map);
    param.workspace = Optional.ofNullable(WORKSPACE.getFromMap(map));
    param.applicationName = Optional.ofNullable(APPLICATION_NAME.getFromMap(map));
    param.applicationVersion = Optional.ofNullable(APPLICATION_VERSION.getFromMap(map));
    return param;
  }


  public String getOrderType() {
    return orderType;
  }

  
  public Optional<String> getWorkspace() {
    return workspace;
  }

  
  public Optional<String> getApplicationName() {
    return applicationName;
  }

  
  public Optional<String> getApplicationVersion() {
    return applicationVersion;
  }

}
