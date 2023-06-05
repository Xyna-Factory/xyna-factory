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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import org.jsmiparser.smi.SmiModuleIdentity;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;


public enum Versioning implements DocumentedEnum {
  CountRevisions(Documentation.
                 en("count of revisions" ).
                 de("Anzahl der Revisionen").
                 build()) {
    public String createPathVersion(SmiModuleIdentity mi) {
      return "v"+mi.getRevisions().size();
    }
    public String createDataModelVersion(SmiModuleIdentity mi) {
      return String.valueOf(mi.getRevisions().size());
    }
  },
  LastUpdated(Documentation.
              en("timestamp of last revision" ).
              de("Zeitstempel der letzten Revision").
              build()) {
    public String createPathVersion(SmiModuleIdentity mi) {
      String lu = mi.getLastUpdated();
      if( lu != null ) {
        if( lu.endsWith("Z") ) {
          return "v"+lu.substring(0,lu.length()-1);
        } else {
          return "v"+lu;
        }
      } else {
        return "v"+mi.getRevisions().size();
      }
    }
    public String createDataModelVersion(SmiModuleIdentity mi) {
      String lu = mi.getLastUpdated();
      if( lu != null ) {
        if( lu.endsWith("Z") ) {
          return lu.substring(0,lu.length()-1);
        } else {
          return lu;
        }
      }
      return "-";
    }
  };

  private Documentation doc;

  private Versioning( Documentation doc) {
    this.doc = doc;
  }
  
  public Documentation getDocumentation() {
    return doc;
  }
  
  public abstract String createPathVersion(SmiModuleIdentity mi);

  public abstract String createDataModelVersion(SmiModuleIdentity mi);

}
