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

package com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons;

import com.gip.xyna.xnwh.selection.WhereClauseBoolean;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;


public interface CronLikeOrderWhereClausesContainer extends WhereClausesContainerBase<CronLikeOrderWhereClausesContainerImpl> {

  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereId();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereLabel();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereOrdertype();
  
  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereStartTime();
  
  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereNextExecution();
  
  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereInterval();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereStatus();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereOnError();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereApplicationname();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereVersionname();
  
  public WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl> whereEnabled();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereTimeZoneID();
  
  public WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl> whereConsiderDaylightSaving();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom0();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom1();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom2();
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom3();

}
