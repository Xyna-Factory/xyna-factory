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



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class MemberMethodJson extends XMOMGuiJson {

  private String label;
  private String implementation;
  private String documentation;

  private ArrayList<VariableJson> inputVars = new ArrayList<VariableJson>();
  private ArrayList<VariableJson> outputVars = new ArrayList<VariableJson>();
  private ArrayList<VariableJson> thrownExceptions = new ArrayList<VariableJson>();


  public String getLabel() {
    return label;
  }

  public String getImplementation() {
    return implementation;
  }

  public String getDocumentation() {
    return documentation;
  }

  public List<VariableJson> getInputVars() {
    return inputVars;
  }

  public List<VariableJson> getOutputVars() {
    return outputVars;
  }

  public List<VariableJson> getThrownExceptions() {
    return thrownExceptions;
  }

  public static class MemberMethodJsonVisitor extends EmptyJsonVisitor<MemberMethodJson> {

    private MemberMethodJson vj = new MemberMethodJson();


    @Override
    public MemberMethodJson get() {
      return vj;
    }

    @Override
    public MemberMethodJson getAndReset() {
      MemberMethodJson ret = vj;
      vj = new MemberMethodJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!Tags.DATA_TYPE_MEMBER_METHOD.equals(value)) {
          throw new UnexpectedJSONContentException(label, Tags.DATA_TYPE_MEMBER_METHOD);
        }
        return;
      }

      if (label.equals(Tags.LABEL)) {
        vj.label = value;
        return;
      }

      if (label.equals(Tags.DATA_TYPE_IMPLEMENTATION)) {
        vj.implementation = value;
        return;
      }

      if (label.equals(Tags.DATA_TYPE_DOCUMENTATION)) {
        vj.documentation = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      if (label.equals(Tags.DATA_TYPE_INPUT) || label.equals(Tags.DATA_TYPE_OUTPUT) || label.equals(Tags.DATA_TYPE_THROWS)) {
        return new VariableJson.VariableJsonVisitor();
      }

      throw new UnexpectedJSONContentException(label);
    }

    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {

      if (label.equals(Tags.DATA_TYPE_INPUT)) {
        get().inputVars = new ArrayList<VariableJson>();
        for (Object variableJson : values) {
          get().inputVars.add((VariableJson)variableJson);
        }
        return;
      }

      if (label.equals(Tags.DATA_TYPE_OUTPUT)) {
        get().outputVars = new ArrayList<VariableJson>();
        for (Object variableJson : values) {
          get().outputVars.add((VariableJson)variableJson);
        }
        return;
      }

      if (label.equals(Tags.DATA_TYPE_THROWS)) {
        get().thrownExceptions = new ArrayList<VariableJson>();
        for (Object variableJson : values) {
          get().thrownExceptions.add((VariableJson)variableJson);
        }
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {

      if (label.equals(Tags.DATA_TYPE_INPUT)) {
        get().inputVars = new ArrayList<VariableJson>();
        return;
      }

      if (label.equals(Tags.DATA_TYPE_OUTPUT)) {
        get().outputVars = new ArrayList<VariableJson>();
        return;
      }

      if (label.equals(Tags.DATA_TYPE_THROWS)) {
        get().thrownExceptions = new ArrayList<VariableJson>();
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
