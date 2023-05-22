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
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class LibJson extends XMOMGuiJson {

  private String fileId;


  public String getFileId() {
    return fileId;
  }


  public static class LibJsonVisitor extends EmptyJsonVisitor<LibJson> {

    private LibJson vj = new LibJson();


    @Override
    public LibJson get() {
      return vj;
    }

    @Override
    public LibJson getAndReset() {
      LibJson ret = vj;
      vj = new LibJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!Tags.SERVICE_GROUP_LIB.equals(value)) {
          throw new UnexpectedJSONContentException(label, Tags.SERVICE_GROUP_LIB);
        }
        return;
      }

      if (label.equals(Tags.SERVICE_GROUP_FILE_ID)) {
        vj.fileId = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
