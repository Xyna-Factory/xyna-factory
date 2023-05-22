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
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;



public class TemplateJson extends XMOMGuiJson {

  private String label;

  public TemplateJson(StepMapping step) {
    this.label = step.getLabel();
  }

  private TemplateJson() {
  }
  
  public String getLabel() {
    return label;
  }

  public static class TemplateJsonVisitor extends EmptyJsonVisitor<TemplateJson> {

    TemplateJson tj = new TemplateJson();


    @Override
    public TemplateJson get() {
      return tj;
    }


    @Override
    public TemplateJson getAndReset() {
      TemplateJson ret = tj;
      tj = new TemplateJson();
      return ret;
    }


    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!value.equals(Tags.TEMPLATE)) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.TEMPLATE);
        } else {
          return;
        }
      }
      if( label.equals(Tags.LABEL) ) {
        tj.label = value;
        return;
      }
    }

  }

}
