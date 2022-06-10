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
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.paths.Tags;

public class LabelJson extends XMOMGuiJson {

  private String label;
  
  public LabelJson() {}
  
  public LabelJson(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  public static JsonVisitor<LabelJson> getJsonVisitor() {
    return new LabelJsonVisitor();
  }
  
  private static class LabelJsonVisitor extends EmptyJsonVisitor<LabelJson> {
    LabelJson lj = new LabelJson();
    
    @Override
    public LabelJson get() {
      return lj;
    }
    @Override
    public LabelJson getAndReset() {
      LabelJson ret = lj;
      lj = new LabelJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals(Tags.LABEL) ) {
        lj.label = value;
        return;
      }
    }

  }

}
