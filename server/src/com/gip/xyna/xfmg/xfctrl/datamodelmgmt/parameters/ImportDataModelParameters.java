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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 */
public class ImportDataModelParameters extends DataModelParameters implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static final String BASE_TYPE_NAME = "baseTypeName";
  public static final String BASE_PATH = "basePath";
  public static final String OVERWRITE = "overwrite";

  private List<String> fileIds;
  private List<String> files;
  
  public ImportDataModelParameters(String dataModelType) {
    super(dataModelType,null, null);
    this.fileIds = new ArrayList<String>();
  }

  public void addBaseTypeName(String baseTypeName) {
    addParameter( BASE_TYPE_NAME, baseTypeName);
  }


  public void addBasePath(String basePath) {
    addParameter( BASE_PATH, basePath);
  }

  public void addOverwrite(boolean overwrite) {
    addParameter( OVERWRITE, overwrite);
  }


  /**
   * @param fileIds
   */
  public void setFileIds(String[] fileIds) {
    if (fileIds != null) {
      this.fileIds = Arrays.asList(fileIds);
    }
  }

  public List<String> getFileIds() {
    return fileIds;
  }

  
  public List<String> getFiles() {
    return files;
  }
  
  public void setFiles(List<String> files) {
    this.files = files;
  }
}
