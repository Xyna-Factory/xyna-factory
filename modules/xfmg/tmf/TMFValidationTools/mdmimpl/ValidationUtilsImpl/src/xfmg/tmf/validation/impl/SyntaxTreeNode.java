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
package xfmg.tmf.validation.impl;

import xfmg.tmf.validation.impl.functioninterfaces.TMFFunction;
import xfmg.tmf.validation.impl.functioninterfaces.TMFInfixFunction;

public class SyntaxTreeNode {

    final TMFFunction f; //expression extension or null (value) or inbuild (arithmetic, boolean comparators)
    final SyntaxTreeNode[] args; //only set if f!=null
    final String value; //only set if value exists (can be any string/number as string or a path)
    final int lastIdx; //last character index of this function in the expression 


    public SyntaxTreeNode(TMFFunction f, SyntaxTreeNode[] args, String value, int lastIdx) {
      super();
      this.f = f;
      this.args = args;
      this.value = value;
      this.lastIdx = lastIdx;
    }


    public Object eval(TMFExpressionContext context) {
      if (f == null) {
        return value;
      }
      return f.evalLazy(context, args);
    }


    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (f != null) {
        if (f instanceof TMFInfixFunction) {
          if (args != null && args[0] != null) {
            sb.append(args[0].toString()).append(" ");
          } else {
            sb.append("<null> ");
          }
        }
        sb.append(f.getName());
        if (f instanceof TMFInfixFunction) {
          sb.append(" ");
        } else {
          sb.append("(");
        }
      } else if (value != null) {
        sb.append("\"").append(value).append("\"");
      }
      if (args != null) {
        if (f instanceof TMFInfixFunction) {
          if (args.length > 1 && args[1] != null) {
            sb.append(args[1].toString());
          } else {
            sb.append("<null>");
          }
        } else {
          for (int i = 0; i < args.length; i++) {
            sb.append(args[i].toString());
            if (i != args.length - 1) {
              sb.append(", ");
            }
          }
          sb.append(")");
        }
      } else if (f != null) {
        if (f instanceof TMFInfixFunction) {
          sb.append("<null>");
        } else {
          sb.append(")");
        }
      }
      return sb.toString();
    }


    public void validate() {
      if (f != null) {
        f.validate(this, args);
        if (args != null) {
          for (SyntaxTreeNode n : args) {
            n.validate();
          }
        }
      }
    }
}
