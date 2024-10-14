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
package com.gip.xyna.update;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class UpdateDiscoverXMOMDatabase extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;
  private final boolean discoverApplications;

  UpdateDiscoverXMOMDatabase(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate, boolean discoverApplications) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
    this.discoverApplications = discoverApplications;
  }
  
  @Override
  protected void update() throws XynaException {
    
    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      UpdateGeneratedClasses.mockFactory();
      ODS ods = ODSImpl.getInstance();
      XMOMDatabase xmomDatabase = XMOMDatabase.getXMOMDatabasePreInit(ods, "update");
      try {
        xmomDatabase.discovery(discoverApplications);
      } catch (XynaException e) {
        logger.warn("XMOM-Discovery failed, the command 'xmomdiscovery' can be used to manually trigger a discovery.",e);
      }
      GenerationBase.clearGlobalCache();
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustRegenerate;
  }

}
