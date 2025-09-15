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



import com.gip.xyna.XynaFactory;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;



public class PropertyFunction implements TMFDirectFunction {

  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    String propName = ConversionUtils.getString(args[0]);
    if (propName == null) {
      throw new RuntimeException(getName() + " can not be evaluated. First parameter is null");
    }
    String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().getProperty(propName);
    if (args.length > 1 && val == null) {
      return ConversionUtils.getString(args[1]);
    }
    return val;
  }


  @Override
  public String getName() {
    return "PROPERTY";
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length == 0 || args.length > 2) {
      throw new RuntimeException(getName() + " does not support " + args.length
          + " parameters. It expects either one or two parameters: Property name and optionally default value.");
    }
  }

}
