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

package com.gip.xyna.update;

import java.io.File;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public enum VersionDependentPath {
  
  savedAndRevisions() {
    
    @Override
    public String getPath(PathType pathtype, boolean deployed) {
      StringBuilder builder = new StringBuilder();
      if (deployed) {
        builder.append("..").append(fileSeparator).append(REVISION_PATH).append(fileSeparator).append(PREFIX_REVISION);
        builder.append(SUFFIX_REVISION_WORKINGSET).append(fileSeparator);
      } else {
        if (pathtype != PathType.SERVICE) {
          builder.append("..").append(fileSeparator).append(PREFIX_SAVED).append(fileSeparator);
        }
      }
      switch(pathtype) {
        case XMOM:
          builder.append(SUBDIR_XMOM);
          break;
        case TRIGGER:
          builder.append(SUBDIR_TRIGGER);
          break;
        case FILTER:
          builder.append(SUBDIR_FILTER);
          break;
        case SERVICE:
          builder.append(SUBDIR_SERVICES);
          break;
        case SHAREDLIB:
          builder.append(SUBDIR_SHAREDLIBS).append(fileSeparator);
          break;
        case XMOMCLASSES:
          builder.append(SUBDIR_XMOMCLASSES);
          break;
      }
      return builder.toString();
    }
  },
  
  
  onlyRevisions() {
    
    @Override
    public String getPath(PathType pathType, boolean deployed) {
      return RevisionManagement.getPathForRevision(pathType, RevisionManagement.REVISION_DEFAULT_WORKSPACE, deployed);
    }
  };
  
  
  public static final String fileSeparator = "/";
  
  //Konstanten für Pfade ab Version 4.1.15.0
  public static final String SERVICES_DIR = "." + fileSeparator + "services";
  public static final String REVISION_PATH = "revisions";
  public static final String PREFIX_REVISION = "rev_";
  public static final String PREFIX_SAVED = "saved";
  public static final String SUFFIX_REVISION_WORKINGSET = "workingset";
  public static final String SUBDIR_XMOM = "XMOM";
  public static final String SUBDIR_SERVICES = "services";
  public static final String SUBDIR_XMOMCLASSES = "xmomclasses";
  public static final String SUBDIR_SHAREDLIBS = "sharedLibs";
  public static final String SUBDIR_TRIGGER = "trigger";
  public static final String SUBDIR_FILTER = "filter";


  public static VersionDependentPath getCurrent() {
    //neusten pfad zuerst probieren, dann immer ältere checken.
    for (int i = values().length - 1; i >= 0; i--) {
      VersionDependentPath v = values()[i];
      if (new File(v.getPath(PathType.XMOM, false)).exists()) {
        return v;
      }
    }
    
    return onlyRevisions;
  }

  public abstract String getPath(PathType pathType, boolean deployed);
}
