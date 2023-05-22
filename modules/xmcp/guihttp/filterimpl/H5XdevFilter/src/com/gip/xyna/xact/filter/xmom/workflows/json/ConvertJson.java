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

package com.gip.xyna.xact.filter.xmom.workflows.json;



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class ConvertJson extends XMOMGuiJson {

  private int revision = 0;
  private String label;
  private String path;
  private String targetType;


  private static class ConvertJsonVisitor extends EmptyJsonVisitor<ConvertJson> {

    private ConvertJson cj = new ConvertJson();


    @Override
    public ConvertJson get() {
      return cj;
    }


    @Override
    public ConvertJson getAndReset() {
      ConvertJson ret = cj;
      cj = new ConvertJson();
      return ret;
    }


    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if(Tags.REVISION.equals(label) ) {
        cj.revision = Integer.parseInt(value);
        return;
      }
      if(Tags.CONVERT_LABEL.equals(label) ) {
        cj.label = value;
        return;
      }
      if(Tags.CONVERT_PATH.equals(label) ) {
        cj.path = value;
        return;
      }
      if(Tags.CONVERT_TARGET_TYPE.equals(label) ) {
        cj.targetType = value;
        return;
      }
      throw newUnexpectedJSONContentException(label);
    }

  }
  
  public static JsonVisitor<ConvertJson> getJsonVisitor() {
    return new ConvertJsonVisitor();
   }


  public String getLabel() {
    return label;
  }


  public String getPath() {
    return path;
  }


  public String getTargetType() {
    return targetType;
  }
  
  public int getRevision() {
    return revision;
  }

}
