/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

public class MailConfigurationParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<String> ORDER_TYPE = 
    StringParameter.typeString("orderType").
    documentation( Documentation.
        de("OrderType (Signatur WF(xact.mail.Mail) -> *, * darf xact.mail.Mail und xact.mail.MailTreatment enthalten").
        en("Order type (signature WF(xact.mail.Mail) -> *, * may contains xact.mail.Mail and xact.mail.MailTreatment").build() ).
    mandatory().build();
  
  public static final StringParameter<Integer> RETRIES_ON_ERROR = 
      StringParameter.typeInteger("retriesOnError").
      documentation( Documentation.
          de("Maximale Anzahl an Retries, nachdem Auftrag fehlgeschlagen ist").
          en("Maximum number of retries after order failed").build() ).
      optional().defaultValue(0).build();
 

  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
    StringParameter.asList( ORDER_TYPE, RETRIES_ON_ERROR );

  private String orderType;
  private int retriesOnError;

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }

   @Override
  public MailConfigurationParameter build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {
    MailConfigurationParameter param = new MailConfigurationParameter();
    param.orderType = ORDER_TYPE.getFromMap(paramMap);
    param.retriesOnError = RETRIES_ON_ERROR.getFromMap(paramMap);
    return param;
  }

  public String getOrderType() {
    return orderType;
  }
  
  public int getRetriesOnError() {
    return retriesOnError;
  }

}
