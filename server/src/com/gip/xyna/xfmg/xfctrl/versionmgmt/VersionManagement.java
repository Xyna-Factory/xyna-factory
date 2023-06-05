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
package com.gip.xyna.xfmg.xfctrl.versionmgmt;

import java.util.List;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class VersionManagement extends FunctionGroup {
  
  
  public static final long REVISION_WORKINGSET = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  
  public static final String DEFAULT_NAME = "VersionManagement";
  
  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  

  public enum PathType {
    XMOM, TRIGGER, FILTER, SERVICE, SHAREDLIB, XMOMCLASSES, ROOT, THIRD_PARTIES
  }
  
  public static class ApplicationName {
    private String name;
    private String versionName;
    
    public ApplicationName(String name, String versionName) {
      this.name = name;
      this.versionName = versionName;      
    }
    public String getName() {
      return name;
    }
    public String getVersionName() {
      return versionName;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((versionName == null) ? 0 : versionName.hashCode());
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
      ApplicationName other = (ApplicationName) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (versionName == null) {
        if (other.versionName != null)
          return false;
      } else if (!versionName.equals(other.versionName))
        return false;
      return true;
    }
    
    public boolean equals(String applicationName, String versionName) {
      return nullEquals( this.name, applicationName ) && nullEquals( this.versionName, versionName );
    }
    private boolean nullEquals(String str1, String str2) {
      if( str1 == null ) {
        return str2 == null;
      } else {
        return str1.equals(str2);
      }
    }
  }
  
  
  public VersionManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(FUTUREEXECUTION_ID, "VersionManagement.initAll").
      after(RevisionManagement.class).execAsync();
  } 


  @Override
  protected void shutdown() throws XynaException {
  }
  

  /**
   * Ermittelt die Revision für eine Version einer Application. Falls nur der Applicationsname gegeben ist, wird die höchste Revision (== neuste Version)
   * zurückgeliefert.
   * 
   */
  @Deprecated
  public long getRevision(String applicationName, String versionName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (applicationName == null) {
      return REVISION_WORKINGSET;
    }
    RevisionManagement revisionMgmt =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    return revisionMgmt.getRevision(new Application(applicationName, versionName));
  }

  @Deprecated
  public ApplicationName getApplicationName(long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ApplicationName result;
    if(revision == REVISION_WORKINGSET) {
      result = new ApplicationName(null, null);
    } else {
      RevisionManagement revisionMgmt =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      RuntimeContext rev = revisionMgmt.getRuntimeContext(revision);
      if (!(rev instanceof Application)) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY("No application object found for revision " + revision,
                                                        XMOMVersionStorable.TABLE_NAME);
      }
      result = new ApplicationName(((Application)rev).getName(), ((Application)rev).getVersionName());
    }
    return result;
  }


  /**
   * Liefert alle Application-Revisions
   * @return
   */
  @Deprecated
  public List<Long> getAllRevisions() {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    return revisionManagement.getAllApplicationRevisions();
  }

  @Deprecated
  public static String getPathForRevision(PathType pathtype, long revision) {
    return getPathForRevision(pathtype, revision, true);
  }
    
  /**
   * hat am ende keinen fileseparator, ausser bei shared libs.
   */
  @Deprecated
  public static String getPathForRevision(PathType pathtype, long revision, boolean deployed) {
    return RevisionManagement.getPathForRevision(pathtype, revision, deployed);
  }

}
