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
package gip.base.common;


/** Moegliche Vergleichsoperatoren
*/
public class CompOperator {
  public static final String defaultOp = ""; //$NON-NLS-1$
  public static final String equal = " = "; //$NON-NLS-1$
  public static final String notEqual = " <> "; //$NON-NLS-1$
  public static final String less = " < "; //$NON-NLS-1$
  public static final String lessEqual = " <= "; //$NON-NLS-1$
  public static final String greater = " > "; //$NON-NLS-1$
  public static final String greaterEqual = " >= "; //$NON-NLS-1$
  public static final String like = " LIKE "; //$NON-NLS-1$
  public static final String notLike = " NOT LIKE "; //$NON-NLS-1$
  public static final String isNull = " IS NULL "; //$NON-NLS-1$
  public static final String isNotNull = " IS NOT NULL "; //$NON-NLS-1$

  /** Negiert einen Vergleichsoperator, z.B. = zu &lt;&gt;, IS NULL zu IS NOT NULL, &lt; zu &gt;=, ...
      @param op Der zu negierende Vergleichsoperator
      @return Der negierte Vergleichsoperator
      @throws OBException Unbekannter Vergleichsoperator
  */
  public static String negateCompOp(String op) throws OBException {
    if (op.equals(equal)) {
      return notEqual;
    }
    if (op.equals(notEqual)) {
      return equal;
    }
    if (op.equals(less)) {
      return greaterEqual;
    }
    if (op.equals(lessEqual)) {
      return greater;
    }
    if (op.equals(greater)) {
      return lessEqual;
    }
    if (op.equals(greaterEqual)) {
      return less;
    }
    if (op.equals(like)) {
      return notLike;
    }
    if (op.equals(notLike)) {
      return like;
    }
    if (op.equals(isNull)) {
      return isNotNull;
    }
    if (op.equals(isNotNull)) {
      return isNull;
    }
  
    throw new OBException(OBException.OBErrorNumber.unkownCompOp1, new String[]{op});
  }
  
  
}