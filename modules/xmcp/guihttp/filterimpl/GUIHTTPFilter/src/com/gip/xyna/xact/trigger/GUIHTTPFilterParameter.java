/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;


public class GUIHTTPFilterParameter extends FilterConfigurationParameter {

  private static final long serialVersionUID = 1L;

  public static final StringParameter<Integer> FILE_UPLOAD_SIZE_LIMIT_KB =
    StringParameter.typeInteger("fileUploadSizeLimitKB")
                    .documentation(Documentation
                                    .de("Limit für Dateigröße beim Upload in Kilobyte.")
                                    .en("File upload size limit in kilobytes.")
                                    .build())
                    .optional().defaultValue(-1).build();


  protected static final List<StringParameter<?>> ALL_PARAMETERS = 
    StringParameter.asList( FILE_UPLOAD_SIZE_LIMIT_KB );

  private Integer fileUploadSizeLimitKB;

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }

   @Override
  public GUIHTTPFilterParameter build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {
    GUIHTTPFilterParameter param = new GUIHTTPFilterParameter();
    param.fileUploadSizeLimitKB = FILE_UPLOAD_SIZE_LIMIT_KB.getFromMap(paramMap);
    return param;
  }

  public int getFileUploadSizeLimitKB() {
    return fileUploadSizeLimitKB == null ? -1 : fileUploadSizeLimitKB;
  }


  public static GUIHTTPFilterParameter createDefaultConfig() {
    GUIHTTPFilterParameter param = new GUIHTTPFilterParameter();
    param.fileUploadSizeLimitKB = FILE_UPLOAD_SIZE_LIMIT_KB.getDefaultValue();
    return param;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GUIHTTPFilterParameter)) {
      return false;
    }
    GUIHTTPFilterParameter that = (GUIHTTPFilterParameter) obj;
    return (fileUploadSizeLimitKB == that.fileUploadSizeLimitKB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileUploadSizeLimitKB);
  }

  @Override
  public String toString() {
    return "GUIHTTPFilterParameter{" +
            "fileUploadSizeLimitKB=" + fileUploadSizeLimitKB + "}";
  }
  
}
