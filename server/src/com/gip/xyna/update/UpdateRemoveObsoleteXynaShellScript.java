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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;



public class UpdateRemoveObsoleteXynaShellScript extends Update {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateRemoveObsoleteXynaShellScript.class);

  private final Version allowedForUpdate;
  private final Version versionAfterUpdate;
  private final boolean mustUpdateGeneratedClasses;


  public UpdateRemoveObsoleteXynaShellScript(Version allowedForUpdate, Version versionAfterUpdate,
                                             boolean mustUpdateGeneratedClasses) {
    this.allowedForUpdate = allowedForUpdate;
    this.versionAfterUpdate = versionAfterUpdate;
    this.mustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return versionAfterUpdate;
  }


  @Override
  protected void update() throws XynaException {

    File existingObsoleteShellScript = new File("xynaserver.sh");
    if (existingObsoleteShellScript.exists()) {
      logger.debug("Removing obsolete shell script file.");
      existingObsoleteShellScript.delete();
    } else {
      logger.debug("No obsolete shell script file to be removed.");
    }

  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustUpdateGeneratedClasses;
  }

}
