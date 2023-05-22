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



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class FormulaJson extends XMOMGuiJson {

  private String expression;
  private ArrayList<VariableJson> variables;


    public FormulaJson() {}


  public String getExpression() {
    return expression;
  }

  public ArrayList<VariableJson> getVariables() {
    return variables;
  }

  public static class FormulaJsonVisitor extends EmptyJsonVisitor<FormulaJson> {

    FormulaJson lj = new FormulaJson();

    @Override
    public FormulaJson get() {
      return lj;
    }

    @Override
    public FormulaJson getAndReset() {
      FormulaJson ret = lj;
      lj = new FormulaJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (Tags.TYPE.equals(label)) {
        if (!Tags.FORMULA.equals(value) && !Tags.QUERY_FILTER_CRITERION.equals(value) 
            && !Tags.QUERY_SORT_CRITERION.equals(value) 
            && !Tags.QUERY_SELECTION_MASK.equals(value)) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.FORMULA + ", " + Tags.QUERY_FILTER_CRITERION + ", " + Tags.QUERY_SELECTION_MASK + " or " + Tags.QUERY_SORT_CRITERION);
        }
        return;
      }

      if (label.equals(Tags.EXPRESSION)) {
        lj.expression = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      if (!label.equals(Tags.VARIABLES)) {
        throw new UnexpectedJSONContentException(label);
      }

      return new VariableJson.VariableJsonVisitor();
    }

    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      if (!label.equals(Tags.VARIABLES)) {
        throw new UnexpectedJSONContentException(label);
      }

      get().variables = new ArrayList<VariableJson>();
      for (Object variableJson : values) {
        get().variables.add((VariableJson)variableJson);
      }
    }

    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
      if (!label.equals(Tags.VARIABLES)) {
        throw new UnexpectedJSONContentException(label);
      }

      get().variables = new ArrayList<VariableJson>();
    }

  }

}
