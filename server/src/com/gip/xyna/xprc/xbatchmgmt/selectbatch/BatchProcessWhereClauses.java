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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClauseStringTransformation;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;


public interface BatchProcessWhereClauses<W extends WhereClausesContainerBase<W>> extends WhereClausesContainerBase<W> {

  public WhereClauseNumber<W> whereBatchProcessId();
  
  public WhereClauseString<W> whereLabel();
  
  public WhereClauseString<W> whereApplication();
  
  public WhereClauseString<W> whereVersion();

  public WhereClauseString<W> whereWorkspace();
  
  public WhereClauseString<W> whereComponent();
  
  public WhereClauseString<W> whereSlaveOrderType();

  public WhereClauseString<W> whereCustom0();
  
  public WhereClauseString<W> whereCustom1();
  
  public WhereClauseString<W> whereCustom2();
  
  public WhereClauseString<W> whereCustom3();
  
  public WhereClauseString<W> whereCustom4();
  
  public WhereClauseString<W> whereCustom5();
  
  public WhereClauseString<W> whereCustom6();
  
  public WhereClauseString<W> whereCustom7();
  
  public WhereClauseString<W> whereCustom8();
  
  public WhereClauseString<W> whereCustom9();

  public WhereClauseNumber<W> whereTotal();

  public WhereClauseNumber<W> whereStarted();

  public WhereClauseNumber<W> whereRunning();
  
  public WhereClauseNumber<W> whereFinished();

  public WhereClauseNumber<W> whereFailed();

  public WhereClauseNumber<W> whereCanceled();
  
  public WhereClauseStringTransformation<W> whereStatus();

}
