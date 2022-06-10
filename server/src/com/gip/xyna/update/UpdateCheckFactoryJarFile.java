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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;


public class UpdateCheckFactoryJarFile extends Update {

  private final Version allowedForUpdate;
  private final Version afterUpdate;

  private final boolean needsRegeneration;


  public UpdateCheckFactoryJarFile(Version allowedForUpdate, Version afterUpdate, boolean needsRegeneration) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.needsRegeneration = needsRegeneration;
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
    return needsRegeneration;
  }


  @Override
  protected void update() throws XynaException {
    File possiblyExistingOldJarFile = new File(Constants.LIB_DIR + Constants.fileSeparator + "xynaserver.jar");
    if (possiblyExistingOldJarFile.exists()) {
      throw new RuntimeException("Found obsolete jar file " + possiblyExistingOldJarFile.getAbsolutePath()
                      + ", please remove before starting Xyna Factory.");
    }
  }

}
