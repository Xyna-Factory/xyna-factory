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

package com.gip.xyna.xprc.xpce.manualinteraction.selectmi;

import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;



public interface WhereClausesContainer<I extends WhereClausesContainerBase<I>> extends WhereClausesContainerBase<I> {

  public WhereClauseNumber<WhereClausesContainerImpl> whereId();
  
  public WhereClauseString<WhereClausesContainerImpl> whereTodo();
  
  public WhereClauseString<WhereClausesContainerImpl> whereType();
  
  public WhereClauseString<WhereClausesContainerImpl> whereReason();
  
  public WhereClauseString<WhereClausesContainerImpl> whereUsergroup();

}
