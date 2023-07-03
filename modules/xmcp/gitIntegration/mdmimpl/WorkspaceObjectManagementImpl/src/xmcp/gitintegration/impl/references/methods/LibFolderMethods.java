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
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.FileUtils;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.impl.references.ReferenceMethods;

public class LibFolderMethods implements ReferenceMethods {

  
  public Optional<File> findJar(Reference reference, String jarName, Long revision) {
    FilenameFilter fileNameFilter = createFileNameFilter(jarName);
    //TODO: call to workspace connection management to find location of repository connected to revision
    String pathToRepository = "";
    Path libFolder = Path.of(pathToRepository, reference.getPath());
    List<File> files = new ArrayList<>();
    //check if there is a matching file in libFolder
    FileUtils.findFilesRecursively(libFolder.toFile(), files, fileNameFilter);
    if(!files.isEmpty()) {
      return Optional.of(Path.of(pathToRepository, reference.getPath()).toFile());
    }
    
    return Optional.empty();
  }

  private FilenameFilter createFileNameFilter(String fileName) {
    return new FilenameFilter() {

      public boolean accept(File dir, String name) {
        return name.equals(fileName);
      }
    };
  }
}
