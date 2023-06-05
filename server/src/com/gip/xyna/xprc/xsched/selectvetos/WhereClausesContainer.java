/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xsched.selectvetos;

import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;


public interface WhereClausesContainer<W extends WhereClausesContainerBase<W>> extends WhereClausesContainerBase<W> {

  public WhereClauseString<W> whereVetoName();
  
  public WhereClauseNumber<W> whereUsingOrderId();
  
  public WhereClauseNumber<W> whereUsingRootOrderId();
  
  public WhereClauseString<W> whereUsingOrderType();
  
  public WhereClauseString<W> whereDocumentation();

}
