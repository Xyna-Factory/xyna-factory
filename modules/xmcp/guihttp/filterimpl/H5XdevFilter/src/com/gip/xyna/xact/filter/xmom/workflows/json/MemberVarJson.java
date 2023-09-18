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
import com.gip.xyna.xact.filter.session.gb.GBSubObject;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.UnsupportedJavaTypeException;



public class MemberVarJson extends XMOMGuiJson {

  private String label;
  private boolean isList;
  private String fqn;
  private PrimitiveType primitiveType;
  private String documentation;


  public MemberVarJson() {}

  public MemberVarJson(GBSubObject object) {
    AVariableIdentification var = object.getVariable().getVariable();
    this.label = object.getVariable().getVariable().getIdentifiedVariable().getLabel();
    this.isList = var.getIdentifiedVariable().isList();
    this.fqn = var.getIdentifiedVariable().getOriginalPath() + "." + var.getIdentifiedVariable().getOriginalName();
    this.primitiveType = var.getIdentifiedVariable().getJavaTypeEnum();
    this.documentation = var.getIdentifiedVariable().getDocumentation();
  }


  public String getLabel() {
    return label;
  }

  public boolean isList() {
    return isList;
  }

  public String getFqn() {
    return fqn;
  }

  public PrimitiveType getPrimitiveType() {
    return primitiveType;
  }

  public String getDocumentation() {
    return documentation;
  }

  public static class MemberVarJsonVisitor extends EmptyJsonVisitor<MemberVarJson> {

    private MemberVarJson vj = new MemberVarJson();


    @Override
    public MemberVarJson get() {
      return vj;
    }

    @Override
    public MemberVarJson getAndReset() {
      MemberVarJson ret = vj;
      vj = new MemberVarJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!Tags.DATA_TYPE_MEMBER_VAR.equals(value)) {
          throw new UnexpectedJSONContentException(label, Tags.DATA_TYPE_MEMBER_VAR);
        }
        return;
      }

      if (label.equals(Tags.LABEL)) {
        vj.label = value;
        return;
      }

      if (label.equals(Tags.IS_LIST)) {
        vj.isList = Boolean.parseBoolean(value);
        return;
      }

      if (label.equals(Tags.FQN)) {
        vj.fqn = value;
        return;
      }

      if (label.equals(Tags.DATA_TYPE_PRIMITIVE_TYPE)) {
        try {
          vj.primitiveType = PrimitiveType.create(value);
        } catch (UnsupportedJavaTypeException e) {
          throw new UnexpectedJSONContentException(label, e);
        }
        return;
      }

      if (label.equals(Tags.DATA_TYPE_DOCUMENTATION_AREA)) {
        vj.documentation = value;
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

}
