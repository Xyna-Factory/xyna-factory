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
package com.gip.xyna.xmcp.xfcli;

import java.util.List;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;


public class PluginDescriptionUtils {

  public static String help(PluginDescription pluginDescription, 
                          List<String> parameters, ParameterUsage usage, String usageString) {
    DocumentationLanguage lang = DocumentationLanguage.EN;
    for( String param : parameters ) {
      if( param.equalsIgnoreCase("DE") ) {
        lang = DocumentationLanguage.DE;
      }
    }
    
    StringBuilder output = new StringBuilder();
    append(output, pluginDescription, lang, usage, usageString);
    return output.toString();
  }

  public static void append(StringBuilder output, PluginDescription pluginDescription, DocumentationLanguage lang, ParameterUsage usage, String usageString) {
    if( pluginDescription.hasParameters(usage) ) {
      output.append( pluginDescription.getDescription() );
      output.append("\n  parameters for ").append(usageString).append(":");
      for( StringParameter<?> sp : pluginDescription.getParameters(usage) ) {
        output.append("\n   * ");
        StringParameterFormatter.appendStringParameter( output, sp, lang, "       " );
      }
    } else {
      output.append("no parameters defined");
    }
  }

}
