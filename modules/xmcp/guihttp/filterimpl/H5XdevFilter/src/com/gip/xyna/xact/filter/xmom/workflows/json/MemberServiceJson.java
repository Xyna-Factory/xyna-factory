/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



public class MemberServiceJson extends XMOMGuiJson {

  private String label;


  public String getLabel() {
    return label;
  }


  public static class MemberServiceJsonVisitor extends EmptyJsonVisitor<MemberServiceJson> {

    private MemberServiceJson vj = new MemberServiceJson();


    @Override
    public MemberServiceJson get() {
      return vj;
    }

    @Override
    public MemberServiceJson getAndReset() {
      MemberServiceJson ret = vj;
      vj = new MemberServiceJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!Tags.SERVICE_GROUP_MEMBER_SERVICE.equals(value)) {
          throw new UnexpectedJSONContentException(label, Tags.SERVICE_GROUP_MEMBER_SERVICE);
        }
        return;
      }

      if (label.equals(Tags.LABEL)) {
        vj.label = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
