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



import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;



/*
 * the evaluation of a path can have different results:
 * 1. a "value" (string/number/etc)
 * 2. a list of values (containing 0 to n elements)
 * 3. a single complex object
 * 4. a list of complex objects
 * 
 * note especially that it can return a list of one element.
 * it may be confusing to understand when exactly a value is returned, and when a list of one element.
 * 
 * for a function like LEN this makes a lot of difference because typically LEN(value) != LEN(list with one element containing the same value) == 1
 * 
 * assuming understanding of the behavior when a list or a value is returned. how to force calculating the LEN of the value in the one-element list?
 * a function is needed to extract the one element from the list. this function must be compatible with there being zero elements in the list?
 *
 * if the element in the list is of type number one can use "+0" to extract the value
 * if the element in the list is of type string one can use CONCAT("", list) to extract the value
 * 
 * 
 */
public class EvalFunction implements TMFDirectFunction {

  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    if (args.length == 1) {
      return outputhandling(context.eval(ConversionUtils.getString(args[0])));
    } else if (args.length == 2) {
      return outputhandling(context.eval(ConversionUtils.ifNull(ConversionUtils.getString(args[0]), "{}"),
                                         ConversionUtils.getString(args[1])));
    }
    throw new RuntimeException();
  }


  private Object outputhandling(Object eval) {
    return eval; //other functions have to be prepared for different type of inputs
  }


  @Override
  public String getName() {
    return "EVAL";
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length < 1 || args.length > 2) {
      throw new RuntimeException("need 1 arg");
    }
  }
}
