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
package com.gip.xyna.xprc.xpce.dispatcher;



import java.io.IOException;
import java.io.Serializable;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;



public class DestinationKey implements Serializable, StringSerializable<DestinationKey> {

  private static final long serialVersionUID = 1L;

  private String orderType;
  private Boolean compensate = false;
  
  private String applicationName; //nur noch wegen Abw�rtskompatibilit�t drin
  private String versionName; //nur noch wegen Abw�rtskompatibilit�t drin
  private RuntimeContext runtimeContext;

  
  public DestinationKey(String orderType, String applicationName, String versionName) {
    this(orderType, applicationName == null ? RevisionManagement.DEFAULT_WORKSPACE : new Application(applicationName, versionName));
  }
  

  public DestinationKey(String orderType, RuntimeContext runtimeContext) {
    if (orderType == null) {
      throw new IllegalArgumentException("Ordertype may not be null.");
    } else {
      this.orderType = orderType.trim(); //leerzeichen am anfang und ende sind nie zu gebrauchen
    }
    this.runtimeContext = runtimeContext;
  }


  public DestinationKey(String orderType) {
    this(orderType, null);
  }


  /**
   * intern benutzt um zu kennzeichnen, dass der auftrag ein subauftrag ist, der die kompensierung eines vorher
   * erfolgreich ausgef�hrten subauftrags erledigen soll.
   */
  public void setCompensate(boolean b) {
    compensate = b;
  }


  public boolean isCompensate() {
    return compensate;
  }


  public String getOrderType() {
    return orderType;
  }


  public void setOrderType(String orderType) {
    if (orderType == null) {
      throw new RuntimeException("Ordertype may not be null");
    }
    this.orderType = orderType;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((orderType == null) ? 0 : orderType.hashCode());
    result = prime * result + ((getRuntimeContext() == null) ? 0 : getRuntimeContext().hashCode());
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
    DestinationKey other = (DestinationKey) obj;
    if (orderType == null) {
      if (other.orderType != null)
        return false;
    } else if (!orderType.equals(other.orderType))
      return false;
    if (getRuntimeContext() == null) {
      if (other.getRuntimeContext() != null)
        return false;
    } else if (!getRuntimeContext().equals(other.getRuntimeContext()))
      return false;
    return true;
  }


  public String toString() {
    return orderType;
  }


  public String getApplicationName() {
    if (runtimeContext != null &&
        runtimeContext instanceof Application) {
      return runtimeContext.getName();
    }
    return applicationName;
  }

  @Deprecated
  public void setApplicationName(String applicationName) {
    if (applicationName == null) {
      setRuntimeContext(RevisionManagement.DEFAULT_WORKSPACE);
      return;
    }
    
    this.applicationName = applicationName;
    if (runtimeContext != null &&
        runtimeContext instanceof Application) {
      this.versionName = ((Application) runtimeContext).getVersionName();
    }
    this.runtimeContext = null;
  }


  public String getVersionName() {
    if (runtimeContext != null &&
        runtimeContext instanceof Application) {
      return ((Application)runtimeContext).getVersionName();
    }
    return versionName;
  }


  @Deprecated
  public void setVersionName(String versionName) {
    this.versionName = versionName;
    if (runtimeContext != null &&
        runtimeContext instanceof Application) {
      this.applicationName = runtimeContext.getName();
    }
    runtimeContext = null;
  }


  public String getWorkspaceName() {
    if (runtimeContext != null &&
        runtimeContext instanceof Workspace) {
      return runtimeContext.getName();
    }
    
    return null;
  }
  
  public RuntimeContext getRuntimeContext() {
    RuntimeContext value = runtimeContext;
    if (value == null) {
      if (applicationName != null) {
        value = new Application(applicationName, versionName);
      } else {
        value = RevisionManagement.DEFAULT_WORKSPACE;
      }
    }
    
    return value;
  }
  
  
  public void setRuntimeContext(RuntimeContext runtimeContext) {
    this.applicationName = null;
    this.versionName = null;
    this.runtimeContext = runtimeContext;
  }


  public boolean isAllowedForBackup() {
    return true;
  }


  public DestinationKey deserializeFromString(String string) {
    return DestinationKey.valueOf(string);
  }

  public String serializeToString() {
    if (getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
      return StringUtils.mask(orderType,'@');
    } else {
      return StringUtils.mask(orderType,'@')+"@"+getRuntimeContext().serializeToString();
    }
  }
  
  /**
   * Lesen des DestinationKey im Format "orderType@applicationName/versionName" f�r Applications
   * bzw. "orderType@workspaceName" f�r Workspaces
   * Im Ordertype m�ssen '@', im ApplicationName '/' maskiert werden:
   * "or\\der\@Ty/pe@applic\\ati\/on@Name/ver@si/on\Name" f�r "or\der@Ty/pe", "applic\ati/on@Name", "ver@si/on\Name"
   * @param string
   * @return
   */
  public static DestinationKey valueOf(String string) {
    
    int idx = 0;
    StringBuilder orderType = new StringBuilder();
    idx += StringUtils.readMaskedUntil( orderType, string.substring(idx), '@' );
    //System.err.println( orderType + " ### " + string.substring(idx)  );
    String runtimeContext = string.substring(idx);
    if (runtimeContext.length() == 0) {
      return new DestinationKey( orderType.toString() );
    }
    
    return new DestinationKey(orderType.toString(), RuntimeContext.valueOf(runtimeContext));
  }

  
  public boolean isInApplication(ApplicationName application) {
    if (application.getName() == null) {
      return getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE);
    }
    
    return getRuntimeContext().equals(new Application(application.getName(), application.getVersionName()));
  }

  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    getRuntimeContext();
  }


  public RuntimeContext unsafeGetRuntimeContext() {
    return runtimeContext;
  }
}
