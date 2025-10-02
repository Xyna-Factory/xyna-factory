/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl.builtinfunctions;



import java.util.List;

import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;



public class IfNullFunction implements TMFDirectFunction {

  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    if (isNull(args[0])) {
      return args[1];
    } else {
      return args[0];
    }
  }


  private boolean isNull(Object o) {
    if (o == null) {
      return true;
    }
    if (o instanceof List) {
      List list = (List) o;
      switch (list.size()) {
        case 0 :
          return true;
        case 1 :
          return list.get(0) == null;
        default :
          return false;
      }
    } else {
      return false;
    }
  }


  @Override
  public String getName() {
    return "IFNULL";
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length != 2) {
      throw new RuntimeException(getName() + " needs 2 arguments.");
    }
  }

}
