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
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;

import com.gip.xyna.xact.filter.actions.auth.ExternalUserLoginAction.ExternalAuthType;

public class H5XdevFilterParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<ExternalAuthType> AUTH_TYPE = StringParameter.typeEnum(ExternalAuthType.class, "externalAuthType", true)
          .documentation(Documentation
                  .de("Art der externen Authentifizierung.")
                  .en("Type of external authentication.")
                  .build())
          .optional().defaultValue(ExternalAuthType.CLIENT_CERT).build();

  public static final StringParameter<String> AUTH_HEADER = StringParameter.typeString("externalAuthHeader")
          .documentation(Documentation
                  .de("Name des HTTP Headers mit den Informationen für die externe Authentifizierung.")
                  .en("Name of the HTTP header containing the data for external authentification.")
                  .build())
          .optional().defaultValue("SSL_CLIENT_CERT").build();

  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
    StringParameter.asList( AUTH_TYPE, AUTH_HEADER );

  private ExternalAuthType authType;
  private String authHeader;

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }

   @Override
  public H5XdevFilterParameter build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {
    H5XdevFilterParameter param = new H5XdevFilterParameter();
    param.authType = AUTH_TYPE.getFromMap(paramMap);
    param.authHeader = AUTH_HEADER.getFromMap(paramMap);
    return param;
  }

  public ExternalAuthType getExternalAuthType() {
    return authType;
  }
  
  public String getExternalAuthHeader() {
    return authHeader;
  }

  @Override
  public String toString() {
    return "H5XdevFilterParameter{" +
            "authType=" + authType +
            ", authHeader='" + authHeader + '\'' +
            '}';
}
}
