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
package com.gip.xyna.xfmg.xfctrl.filemgmt;

public class FileInfo {

  private String id;
  private String originalFilename;
  private long size;
  private String location;

  public FileInfo(String id, String originalFilename, long size, String location) {
    this.id = id;
    this.originalFilename = originalFilename;
    this.size = size;
    this.location = location;
  }

  public String getId() {
    return id;
  }
  
  public String getOriginalFilename() {
    return originalFilename;
  }
  
  public long getSize() {
    return size;
  }
  
  public String getLocation() {
    return location;
  }
}
