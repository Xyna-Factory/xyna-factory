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
package com.gip.xyna.utils.scheduler;


/**
 * Soll Daten sammeln, die während eines Schedulerlaufs anfallen.
 * Scheduler kann dann über getSchedulerInformation() dann die Daten herausgeben.
 * 
 *
 */
public interface SchedulerInformationBuilder<I> {
  
  public enum Timestamp { Start, PreparationFinished, SchedulingFinished, Finished}

  void loopEndedRegularily(boolean loopEndedRegularily);

  void scheduledOrders(int scheduled);

  void iteratedOrders(int iterated);

  long timestamp(Timestamp timestamp);

  /**
   * Am Ende jedes SchedulingLaufs gerufen, um Daten zwischenzuspeichern und über getSchedulerInformation()
   * ausgeben zu können. 
   */
  I build();

  void waitingForTags(int waitingForTags);

  void waitingForTagNull(int waitingForTagNull);

  void waitingForUnknown(int waitingForUnknown);

  void schedulerRunNumber(long schedulerRunNumber);
  
}