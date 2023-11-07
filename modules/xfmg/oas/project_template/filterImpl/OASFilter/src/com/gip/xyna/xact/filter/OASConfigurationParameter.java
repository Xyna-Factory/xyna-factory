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
package com.gip.xyna.xact.filter;

import java.util.List;
import java.util.Map;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;

public class OASConfigurationParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<String> ORDER_TYPE = 
    StringParameter.typeString("orderType").
    documentation( Documentation.
        de("OrderType als Beispiel einer Filter-Konfiguration").
        en("Order type as an example for filter configuration").build() ).
    optional().build();

  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
    StringParameter.asList( ORDER_TYPE );

  private String orderType;

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }

  @Override
  public OASConfigurationParameter build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {
    OASConfigurationParameter param = new OASConfigurationParameter();
    param.orderType = ORDER_TYPE.getFromMap(paramMap);
    return param;
  }

  public String getOrderType() {
    return orderType;
  }

}
