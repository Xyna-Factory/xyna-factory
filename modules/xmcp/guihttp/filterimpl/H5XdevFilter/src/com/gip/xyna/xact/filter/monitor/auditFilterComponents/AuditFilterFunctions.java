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

package com.gip.xyna.xact.filter.monitor.auditFilterComponents;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import org.xml.sax.Attributes;

public class AuditFilterFunctions {

  public static RuntimeContext getRtcFromAttributes(Attributes atts) {
    RuntimeContext rc = null;
    
    String rcName = atts.getValue(ATT.WORKSPACE);
    if(rcName != null) {
      rc = new Workspace(rcName);
    } else {
      //TODO: ApplicationDefinition?
      rcName = atts.getValue(ATT.APPLICATION);
      String versionName = atts.getValue(ATT.APPLICATION_VERSION);
      
      //attributes may not contain a RuntimeContext, if the object lives in the same RuntimeContext as the workflow
      if(rcName == null) {
        return null;
      }
      
      rc = new Application(rcName, versionName);
    }
    return rc;
  }
}
