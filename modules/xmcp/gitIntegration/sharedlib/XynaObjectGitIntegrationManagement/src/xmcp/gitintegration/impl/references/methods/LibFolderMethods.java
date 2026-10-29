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
package xmcp.gitintegration.impl.references.methods;



import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;

import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceMethods;



public class LibFolderMethods implements ReferenceMethods {

  private static final Logger logger = CentralFactoryLogging.getLogger(LibFolderMethods.class);


  public List<File> execute(InternalReference reference) {
    Path libFolder = Path.of(reference.getPathToRepo(), reference.getPath());
    List<File> files = new ArrayList<>();
    FileUtils.findFilesRecursively(libFolder.toFile(), files, (x, y) -> true);

    if (logger.isDebugEnabled()) {
      logger.debug("searched for files in lib_folder " + libFolder.toString() + ". Found " + files.size() + " files.");
    }

    return files;
  }
}
