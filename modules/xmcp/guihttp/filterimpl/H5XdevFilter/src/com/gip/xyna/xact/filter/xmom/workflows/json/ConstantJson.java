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
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;

public class ConstantJson extends XMOMGuiJson {


  private int revision;
  private String constant;
  private String branchId = null;
  
  
  private ConstantJson() {
  }
  
  public ConstantJson(int revision) {
    this.revision = revision;
  }
    
  public static JsonVisitor<ConstantJson> getJsonVisitor() {
   return new ConstantJsonVisitor();
  }

  private static class ConstantJsonVisitor extends EmptyJsonVisitor<ConstantJson> {
    ConstantJson cj = new ConstantJson();

    @Override
    public ConstantJson get() {
      return cj;
    }
    @Override
    public ConstantJson getAndReset() {
      ConstantJson ret = cj;
      cj = new ConstantJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals(Tags.REVISION) ) {
        cj.revision = Integer.valueOf(value);
        return;
      }
      if( label.equals(Tags.CONSTANT) ) {
        cj.constant = value;
        return;
      }
      if( label.equals(Tags.BRANCH_ID) ) {
        cj.branchId = value;
        return;
      }
    }

    @Override
    public JsonVisitor<?> objectStarts(String content) {
      return null; //FIXME wie Objekt erkennen?
    }

    @Override
    public void object(String label, Object value) {
      
    }
  }
    
  public String getConstant() {
    return constant;
  }

  public int getRevision() {
    return revision;
  }
  
  public String getBranchId() {
    return branchId;
  }
}
