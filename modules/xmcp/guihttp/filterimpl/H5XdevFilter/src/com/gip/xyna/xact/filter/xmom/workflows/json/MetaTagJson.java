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
package com.gip.xyna.xact.filter.xmom.workflows.json;



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class MetaTagJson extends XMOMGuiJson {

  private boolean deletable;
  private String tag;


  public boolean getDeletable() {
    return deletable;
  }

  public String getTag() {
    return tag;
  }


  public static class MetaTagJsonVisitor extends EmptyJsonVisitor<MetaTagJson> {

    private MetaTagJson vj = new MetaTagJson();


    @Override
    public MetaTagJson get() {
      return vj;
    }

    @Override
    public MetaTagJson getAndReset() {
      MetaTagJson ret = vj;
      vj = new MetaTagJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!Tags.META_TAG.equals(value)) {
          throw new UnexpectedJSONContentException(label, Tags.META_TAG);
        }
        return;
      }

      if (label.equals(Tags.TAG)) {
        vj.tag = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
