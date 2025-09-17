/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


package xfmg.oas.generation.tools;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.gip.xyna.FileUtils;

public class OASApplicationData implements Closeable {
  private final String id;
  private final List<File> files;
  
  public OASApplicationData(String id, List<File> files) {
    this.id = id;
    this.files = files;
  }
  
  public String getId() {
    return id;
  }
  
  public List<File> getFiles() {
    return files;
  }

  @Override
  public void close() throws IOException {
    for(File f : files) {
      if(f.isFile()) {
        f.delete();
      } else if(f.isDirectory()) {
        FileUtils.deleteDirectory(f);
      }
    }
  }
}