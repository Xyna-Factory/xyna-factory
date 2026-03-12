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

//helper enum only for better overview over all the infix operator precedences
public enum OperatorPrecedence {
  //compare https://introcs.cs.princeton.edu/java/11precedence/
  
  MATHMULTIPLY(5), MATHPLUS(4), RELATIONS(3), EQUAL(2), AND(1), OR(0);

  public int precedence;
  
  OperatorPrecedence(int precedence) {
    this.precedence = precedence;
  }
  
}
