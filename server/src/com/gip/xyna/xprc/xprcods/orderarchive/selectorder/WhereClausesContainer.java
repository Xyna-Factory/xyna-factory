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

package com.gip.xyna.xprc.xprcods.orderarchive.selectorder;

import com.gip.xyna.xnwh.selection.WhereClauseEnum;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public interface WhereClausesContainer<W extends WhereClausesContainerBase<W>> extends WhereClausesContainerBase<W> {

  public WhereClauseNumber<W> whereId();


  public WhereClauseNumber<W> whereParentId();


  public WhereClauseString<W> whereOrderType();


  public WhereClauseString<W> whereExecutionType();


  public WhereClauseNumber<W> wherePriority();


  public WhereClauseString<W> whereStatus();

  
  public WhereClauseEnum<W,OrderInstanceStatus> whereStatusEnum();


  public WhereClauseNumber<W> whereStartTime();


  public WhereClauseNumber<W> whereLastUpdate();


  public WhereClauseNumber<W> whereMonitoringLevel();


  public WhereClauseString<W> whereCustom0();


  public WhereClauseString<W> whereCustom1();


  public WhereClauseString<W> whereCustom2();


  public WhereClauseString<W> whereCustom3();
}
