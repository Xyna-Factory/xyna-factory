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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class UpdateXMOMDatabaseWithRevisionAndApplication extends UpdateJustVersion {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateXMOMDatabaseWithRevisionAndApplication.class);
  
  public UpdateXMOMDatabaseWithRevisionAndApplication(Version oldVersion, Version newVersion, boolean needsRegenerate) {
    super(oldVersion, newVersion, needsRegenerate);
  }

 
  @Override
  protected void update() throws XynaException {
    
    // Factory muss gemockt werdern, weil discovery auf VersionManagement zugreift ... wie ich das hasse :(
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
      ODS ods = ODSImpl.getInstance();
      XMOMDatabase xmomdb = XMOMDatabase.getXMOMDatabasePreInit(ods, "update");
      xmomdb.discovery();    
      GenerationBase.clearGlobalCache();   
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }

}
