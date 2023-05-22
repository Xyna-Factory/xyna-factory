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



import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonParserUtils;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class CaseJson extends XMOMGuiJson {

  private String expression;
  private String label;


  public String getExpression() {
    return expression;
  }


  public String getLabel() {
    return label;
  }


  //  public void serviceLabelToJson(JsonBuilder jb) {
  //    ServiceUtils.labelToJson(jb, label, fqName != null ? fqName.toString() : null, readOnly, isPrototype);
  //  }

  public static class CaseJsonVisitor extends EmptyJsonVisitor<CaseJson> {

    private static List<String> usedTags = Arrays.asList(Tags.LABEL, Tags.EXPRESSION); // TODO: loeschen?
    CaseJson sj = new CaseJson();


    @Override
    public CaseJson get() {
      return sj;
    }


    @Override
    public CaseJson getAndReset() {
      CaseJson ret = sj;
      sj = new CaseJson();
      return ret;
    }


    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!(value.equals(Tags.CASE))) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.CASE);
        } else {
          return;
        }
      }

      if (label.equals(Tags.EXPRESSION)) {
        sj.expression = value;
        return;
      }
      if (label.equals(Tags.LABEL)) {
        sj.label = value;
        return;
      }

      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }


    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException { // TODO: loeschen?
      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }


    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      JsonParserUtils.checkAllowedLabels(usedTags, label);
      return null;
    }

  }

}
