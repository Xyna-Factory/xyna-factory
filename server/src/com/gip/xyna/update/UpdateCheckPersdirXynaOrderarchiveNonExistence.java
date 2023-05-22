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

package com.gip.xyna.update;

import java.io.File;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


public class UpdateCheckPersdirXynaOrderarchiveNonExistence extends Update {

  private final Version afterUpdate;
  private final Version allowedForUpdate;


  public UpdateCheckPersdirXynaOrderarchiveNonExistence(final Version allowedForUpdate, final Version afterUpdate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
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
    return false;
  }


  @Override
  protected void update() throws XynaException {
    File mayAtMostContainAnIndexFile = new File(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator
                    + "xynaorderarchive");
    if (mayAtMostContainAnIndexFile.exists()) {
      File[] files = mayAtMostContainAnIndexFile.listFiles();
      if (files != null && files.length > 1) {
        throwRuntimeException(mayAtMostContainAnIndexFile);
      } else if (files != null && files.length == 1) {
        if (files[0].getAbsolutePath().endsWith(".index")) {
          logger.debug("Removing obsolete folder '" + mayAtMostContainAnIndexFile.getAbsolutePath() + "'");
          files[0].delete();
          mayAtMostContainAnIndexFile.delete();
        } else {
          throwRuntimeException(mayAtMostContainAnIndexFile);
        }
      } else {
        logger.debug("Removing obsolete folder '" + mayAtMostContainAnIndexFile.getAbsolutePath() + "'");
        mayAtMostContainAnIndexFile.delete();
      }
    }
  }


  private void throwRuntimeException(File mayAtMostContainAnIndexFile) {
    throw new RuntimeException("The folder '" + mayAtMostContainAnIndexFile.getPath()
                    + "' might contain obsolete running orders."
                    + " Please use an older version of the server to continue these orders and delete the folder.");
  }

}
