/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

 * Copyright 2026 Xyna GmbH, Germany
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

public class McpServerConfigurationParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<String> ENDPOINT = 
    StringParameter.typeString("endpoint").
    documentation( Documentation.
        de("MCPServer Endpunkt").
        en("Endpoint of the MCP Server").build() ).
    optional().defaultValue("/mcp").build();

  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
    StringParameter.asList( ENDPOINT );

  private String endpoint;

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }

   @Override
  public McpServerConfigurationParameter build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {
    McpServerConfigurationParameter param = new McpServerConfigurationParameter();
    param.endpoint = ENDPOINT.getFromMap(paramMap);
    return param;
  }

  public String getEndpoint() {
    return endpoint;
  }

}
