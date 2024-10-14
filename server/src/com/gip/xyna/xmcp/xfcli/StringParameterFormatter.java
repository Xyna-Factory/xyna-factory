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
package com.gip.xyna.xmcp.xfcli;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterWithEnum;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;


/**
 *
 */
public class StringParameterFormatter {
  

  public static void appendStringParameter(StringBuilder output, StringParameter<?> sp) {
    appendStringParameter(output, sp, DocumentationLanguage.EN, "    ");
  }

  @SuppressWarnings("rawtypes")
  public static void appendStringParameter(StringBuilder output, StringParameter<?> sp, DocumentationLanguage lang, String indentation) {
    String mode = getMode(sp);
    
    String typename = sp.getSimpleTypeString();
    StringParameterWithEnum<?,?> spwe = null;
    if ( sp instanceof StringParameterWithEnum ) {
      spwe = (StringParameterWithEnum)sp;
      if( spwe.hasDocumentedEnum() ) {
        typename = spwe.getSimpleTypeString();
      } else {
        String enums = spwe.getEnumConstantsAsStrings().toString();
        typename = spwe.getSimpleTypeString()+"("+ enums.substring(1,enums.length()-1) +")";
      }
    }
    
    output.append(sp.getName()).append(": ").append(sp.documentation(lang));
    output.append(" (").append(mode).append(", type=").append(typename);
   
    if( sp.getDefaultValueAsString() != null ) {
      if( "String".equals(typename) ) {
        output.append(", default=\"").append(sp.getDefaultValueAsString()).append("\"");
      } else {
        output.append(", default=").append(sp.getDefaultValueAsString());
      }
    }
    
    if( spwe != null && spwe.hasDocumentedEnum() ) {
      for( Pair<String,Documentation> p : spwe.getEnumDocumentation() ) {
        output.append("\n").append(indentation).append(p.getFirst()).append(": ").append(p.getSecond().get(lang));
      }
      output.append("\n").append(indentation);
    }
    
    output.append(")");

    
  }
  
  private static String getMode(StringParameter<?> sp) {
    if( sp.isMandatory() ) {
      return "mandatory";
    } else if( sp.isMandatoryFor() ) {
      StringBuilder sb = new StringBuilder();
      sb.append( "mandatory for [" );
      String sep = "";
      List<SerializablePair<String,Serializable>> mandatoryFor = sp.getMandatoryFor();
      for( SerializablePair<String,Serializable> pair : mandatoryFor ) {
        sb.append(sep).append(pair.getFirst());
        if( pair.getSecond() != null ) {
          if( pair.getSecond() instanceof String ) {
            sb.append("=\"").append(pair.getSecond()).append("\"");
          } else {
            sb.append("=").append(pair.getSecond());
          }
        }
        sep = ", ";
      }
      sb.append("]");
      return sb.toString();
    } else {
      return "optional";
    }
  }

}
