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
package com.gip.xyna.xdev.xlibdev.repositoryaccess;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.gip.xyna.FileUtils;
import com.gip.xyna.xfmg.Constants;


public class RepositoryItemModification implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private static String revisionsPath;
  static {
    try {
      revisionsPath = new File(".." + Constants.fileSeparator + Constants.REVISION_PATH).getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private String file; // ../revisions/rev_x/saved/svn/bla....java
  private RepositoryModificationType modification;
  
  public RepositoryItemModification(String file, RepositoryModificationType modification) {
    if (file.startsWith("/")) {
      // /opt/xyna/xyna_001/revisions/rev_x/saved/svn/bla/servicegroups/myservicegroup...Impl.java
      if (file.startsWith(revisionsPath)) {
        String relativeToRevisions = FileUtils.getRelativePath(revisionsPath, file);
        file = ".." + Constants.fileSeparator + Constants.REVISION_PATH + Constants.fileSeparator + relativeToRevisions;          
      }
    } else if (file.startsWith(".." + Constants.fileSeparator + Constants.REVISION_PATH)) {
      //ok
      // ../revisions/rev_x/saved/svn/bla/servicegroups/myservicegroup...Impl.java
    } else {
      // unknown path, no adjustments
    }
    this.file = file;
    this.modification = modification;
  }
  
  @Override
  public String toString() {
    return modification.getStringRepresentation() + " " + file;
  }
  
  public RepositoryModificationType getModification() {
    return modification;
  }
  
  /**
   * pfad relativ zum server verzeichnis
   */
  public String getFile() {
    return file;
  }
  
  /**
   * Pfad relativ zu base
   * @param base
   * @return
   */
  public String getRelativePath(String base) {
    try {
      String basePath = new File(base).getCanonicalPath();
      String path = new File(file.replaceFirst("\\(bin\\)", "").trim()).getCanonicalPath();
      return FileUtils.getRelativePath(basePath, path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static enum RepositoryModificationType {
    Deleted("D"),
    Added("A"),
    Updated("U"),
    Conflict("C"),
    Merged("G"),
    Existed("E"),
    Modified("M"),
    Unversioned("?");
    
    private final String stringRepresentation;


    private RepositoryModificationType(String charRepresentation) {
      this.stringRepresentation = charRepresentation;
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }
    
  }
  
}
