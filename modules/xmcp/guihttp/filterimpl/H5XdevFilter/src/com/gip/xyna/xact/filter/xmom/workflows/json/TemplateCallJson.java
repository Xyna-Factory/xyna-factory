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
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public class TemplateCallJson extends XMOMGuiJson {


  private int revision;
  
  
  private TemplateCallJson() {
  }
  
  public TemplateCallJson(int revision) {
    this.revision = revision;
  }

  public static JsonVisitor<TemplateCallJson> getJsonVisitor() {
   return new TemplateCallJsonVisitor();
  }

  private static class TemplateCallJsonVisitor extends EmptyJsonVisitor<TemplateCallJson> {
    TemplateCallJson cj = new TemplateCallJson();

    @Override
    public TemplateCallJson get() {
      return cj;
    }
    @Override
    public TemplateCallJson getAndReset() {
      TemplateCallJson ret = cj;
      cj = new TemplateCallJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals("revision") ) {
        cj.revision = Integer.valueOf(value);
        return;
      }
    }

  }

  public int getRevision() {
    return revision;
  }

}
