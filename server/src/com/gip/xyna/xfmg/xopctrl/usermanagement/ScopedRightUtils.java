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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xfmg.exceptions.XFMG_AccessVerificationException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class ScopedRightUtils {
  
  public static final String SCOPE_SEPERATOR = ":";
  
  /**
   * Baut den Rechte-String zusammen
   * @param right
   * @param action
   * @param parts
   * @return
   */
  public static String getScopedRight(ScopedRight right, Action action, String... parts) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    scopeParts.addAll(Arrays.asList(parts));
    
    return getScopedRight(right.getKey(), scopeParts);
  }
  
  /**
   * Baut aus dem key und den scopeParts den Rechte-String zusammen.
   * Dabei werden ':', '*', '!' und ',' mit '\' escaped.
   * @param key
   * @param scopeParts
   * @return
   */
  public static String getScopedRight(String key, List<String> scopeParts) {
    StringBuilder sb = new StringBuilder();
    sb.append(key);
    for (String part : scopeParts) {
      if (part == null) {
        part = "";
      } else {
        part = part.replaceAll(SCOPE_SEPERATOR, "\\\\" + SCOPE_SEPERATOR);
        part = part.replaceAll("\\*", "\\\\*");
        part = part.replaceAll("\\!", "\\\\!");
        part = part.replaceAll(",", "\\\\,");
      }
      sb.append(SCOPE_SEPERATOR).append(part);
    }
    return sb.toString();
  }

  public static List<String> getRuntimeContextParts(RuntimeContext runtimeContext) {
    List<String> scopeParts = new ArrayList<String>();
    if (runtimeContext.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
      //wegen Abwaertskompatibilitaet fuer Default-Workspace Leerstring verwenden
      scopeParts.add("");
    } else {
      scopeParts.add(runtimeContext.getName());
    }
    
    if (runtimeContext instanceof Application) {
      //fuer Applications den VersionName anhaengen
      scopeParts.add(((Application)runtimeContext).getVersionName());
    } else {
      //fuer Workspaces Leerstring anhaengen
      scopeParts.add("");
    }
    
    return scopeParts;
  }

  
  public static String getStartOrderRight(DestinationKey dk) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(dk.getOrderType());
    scopeParts.addAll(getRuntimeContextParts(dk.getRuntimeContext()));
    
    return getScopedRight(ScopedRight.START_ORDER.getKey(), scopeParts);
  }
  
  public static String getManageTCORight(Action action, BatchProcessInput input) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    scopeParts.add(input.getSlaveOrderType());
    scopeParts.addAll(getRuntimeContextParts(input.getMasterOrder().getDestinationKey().getRuntimeContext()));
    
    return getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER.getKey(), scopeParts);
  }

  public static String getReadTCORight(BatchProcessInformation batchProcessInfo) throws XFMG_AccessVerificationException {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(Action.read.toString());
    scopeParts.add(batchProcessInfo.getSlaveOrdertype());
    scopeParts.addAll(getRuntimeContextParts(batchProcessInfo.getArchive().getRuntimeContext()));

    return getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER.getKey(), scopeParts);
  }


  
  
  public static String getXynaPropertyRight(String propertyKey, Action action) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    if (propertyKey != null) {
      scopeParts.add(propertyKey);
    }
    
    return getScopedRight(ScopedRight.XYNA_PROPERTY.getKey(), scopeParts);
  }

  public static String getApplicationRight(String applicationName, String versionName, Action action) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    if (applicationName != null) {
      scopeParts.add(applicationName);
    }
    if (versionName != null) {
      scopeParts.add(versionName);
    }
    
    return getScopedRight(ScopedRight.APPLICATION.getKey(), scopeParts);
  }

  public static String getApplicationDefinitionRight(String applicationName, String workspacename, Action action) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    if (workspacename != null) {
      scopeParts.add(workspacename);
    }
    if (applicationName != null) {
      scopeParts.add(applicationName);
    }
    
    return getScopedRight(ScopedRight.APPLICATION_DEFINITION.getKey(), scopeParts);
  }

  public static String getWorkspaceRight(String workspacename, Action action) {
    List<String> scopeParts = new ArrayList<String>();
    scopeParts.add(action.toString());
    if (workspacename != null) {
      scopeParts.add(workspacename);
    }
    
    return getScopedRight(ScopedRight.WORKSPACE.getKey(), scopeParts);
  }

  
}
