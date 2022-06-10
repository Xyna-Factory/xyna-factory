/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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




public interface CronLikeOrderSelect {

  public CronLikeOrderSelect selectId();
  public CronLikeOrderSelect selectLabelName();
  public CronLikeOrderSelect selectOrdertype();
  public CronLikeOrderSelect selectStarttime();
  public CronLikeOrderSelect selectNextExecution();
  public CronLikeOrderSelect selectInterval();
  public CronLikeOrderSelect selectStatus();
  public CronLikeOrderSelect selectOnError();
  public CronLikeOrderSelect selectApplicationName();
  public CronLikeOrderSelect selectVersionName();
  public CronLikeOrderSelect selectWorkspaceName();
  public CronLikeOrderSelect selectEnabled();
  public CronLikeOrderSelect selectTimeZoneID();
  public CronLikeOrderSelect selectConsiderDaylightSaving();
  public CronLikeOrderSelect selectCustom0();
  public CronLikeOrderSelect selectCustom1();
  public CronLikeOrderSelect selectCustom2();
  public CronLikeOrderSelect selectCustom3();
  
  public CronLikeOrderSelect select(CronLikeOrderColumn column);

  
}
