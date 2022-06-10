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
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public class TypeJson extends XMOMGuiJson {

  private int revision;
  private FQNameJson fqName;
  
  private TypeJson() {}
 
  public int getRevision() {
    return revision;
  }
  
  public FQNameJson getFQName() {
    return fqName;
  }

  public static JsonVisitor<TypeJson> getJsonVisitor() {
    return new TypeJsonVisitor();
  }

  private static class TypeJsonVisitor extends EmptyJsonVisitor<TypeJson> {
    TypeJson tj = new TypeJson();

    @Override
    public TypeJson get() {
      return tj;
    }
    @Override
    public TypeJson getAndReset() {
      TypeJson ret = tj;
      tj = new TypeJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals("revision") ) {
        tj.revision = Integer.valueOf(value);
        return;
      }
      tj.fqName = FQNameJson.parseAttribute(tj.fqName, label, value);
    }

  }


}
