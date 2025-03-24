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

package com.gip.xyna.xact.filter.session.messagebus.events;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xguisupport.messagebus.PredefinedMessagePath;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageOutputParameter;


public class XmomTypeEvent extends MessageOutputParameterEvent {
  
  public static class XmomType {
    private String type;
    private String name;
    RuntimeContext rtc;
    
    public XmomType(String type, String name, RuntimeContext rtc) {
      this.type = type;
      this.name = name;
      this.rtc = rtc;
    }

    
    public String getType() {
      return type;
    }

    
    public String getName() {
      return name;
    }

    
    public RuntimeContext getRtc() {
      return rtc;
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((rtc == null) ? 0 : rtc.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      XmomType other = (XmomType) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (!Objects.equals(rtc, other.rtc))
        return false;
      return true;
    }
  }

  private static final long serialVersionUID = 1L;

  private XmomType xmomType;


  public XmomTypeEvent(MessageOutputParameter source) {
    super(source);

    String type = null;
    String path = null;
    RuntimeContext rtc = null;

    if (PredefinedMessagePath.XYNA_MODELLER_UPDATE.getContext().equals(source.getContext())) {
      rtc = new Workspace(source.getCorrelation());
    } else if (PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_CREATE.getContext().equals(source.getContext()) ||
               PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_UPDATE.getContext().equals(source.getContext()) ||
               PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_DELETE.getContext().equals(source.getContext())) {
      rtc = Workspace.getFromGUIRepresentation(source.getCorrelation());
      if (rtc == null) {
        rtc = Application.getFromGUIRepresentation(source.getCorrelation());
      }
    } else {
      Pattern pattern = Pattern.compile("(.*)-(.*)-WS:\"(.*)\"");
      Matcher m = pattern.matcher(source.getCorrelation());
      if (m.matches()) {
        type = m.group(1);
        path = m.group(2);
        rtc = new Workspace(m.group(3));
      }
    }
    xmomType = new XmomType(type, path, rtc);
  }


  public XmomType getXmomType() {
    return xmomType;
  }
  
}
