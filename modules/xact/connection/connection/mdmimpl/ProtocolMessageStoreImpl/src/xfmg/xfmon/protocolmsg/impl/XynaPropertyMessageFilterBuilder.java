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
package xfmg.xfmon.protocolmsg.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder;

import xfmg.xfmon.protocolmsg.ProtocolMessage;


public class XynaPropertyMessageFilterBuilder implements Builder<XynaPropertyBuildMessageFilter> {

  public XynaPropertyBuildMessageFilter fromString(String value) throws ParsingException {
    if (Boolean.FALSE.toString().equalsIgnoreCase(value)) {
      return new GlobalMessageFilter(false);
    } else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
      return new GlobalMessageFilter(true);
    } else {
      return new ProtocolListMessageFilter(value);
    }
  }

  public String toString(XynaPropertyBuildMessageFilter value) {
    return value.asString();
  }
  
  
  public static final class GlobalMessageFilter implements XynaPropertyBuildMessageFilter {
    
    private final boolean allow;
    
    public GlobalMessageFilter(boolean allow) {
      this.allow = allow;
    }

    public boolean accept(ProtocolMessage msg) {
      return allow;
    }

    public String asString() {
      return String.valueOf(allow);
    }
    
    
  }
  
  
  public static class ProtocolListMessageFilter implements XynaPropertyBuildMessageFilter {
    
    private final Set<String> allowedProtocols;
    
    
    public ProtocolListMessageFilter(String value) {
      allowedProtocols = new HashSet<String>();
      if (value.contains(",")) {
        String[] values = value.split(",");
        for (String valueElement : values) {
          allowedProtocols.add(valueElement.trim());
        }
      } else {
        allowedProtocols.add(value.trim());
      }
    }
    
    public boolean accept(ProtocolMessage msg) {
      return allowedProtocols.contains(msg.getProtocolName());
    }

    public String asString() {
      return StringUtils.joinStringArray(new ArrayList<String>(allowedProtocols).toArray(new String[allowedProtocols.size()]), ", ");
    }
    
  }

}
