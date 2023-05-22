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
package com.gip.xyna.utils.scheduler;

import java.util.concurrent.BlockingQueue;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;

public interface SchedulerCustomisation<O,I> {


  /**
   * Initialisierung: Sammler der Scheduler-Daten
   * @return
   */
  SchedulerInformationBuilder<I> getInformationBuilder();

  /**
   * Initialisierung: Anpassung der beiden Queues f�r den Auftragseingang und f�r Urgency-�nderungen
   * @return
   */
  Pair<BlockingQueue<Urgency<O>>, BlockingQueue<Urgency<O>>> createQueues();

  /**
   * Initialisierung: Damit auf den Scheduler zugegriffen werden kann
   * @param scheduler
   */
  void setScheduler(Scheduler<O,I> scheduler);

  /**
   * Anpassung: Durchf�hrung des Schedulings
   * @param uo
   * @return
   */
  ScheduleResult trySchedule(Urgency<O> uo);

  /**
   * Berechnung der Urgency
   * @param order
   * @return
   */
  long calculateUrgency(O order);

  /**
   * Optional Anpassung: Gerufen, bevor Auftrag mit angegebener Urgency geschedult wird 
   */
  void beforeScheduling(long urgency);

  /**
   * Optional Anpassung: Gerufen, nachdem alle Auftr�ge geschedult wurden 
   */
  void postparation();

  /**
   * Optional Anpassung: Gerufen, bevor alle Auftr�ge geschedult werden 
   */
  void preparation();

  /**
   * Optional Anpassung: Gerufen am Ende eines Schedulerlaufs
   */
  void endScheduling();

  /**
   * Optional Anpassung: Gerufen zu Beginn eines Schedulerlaufs.
   */
  void beginScheduling();

  /**
   * Optional: Behandlung beliebiger Exceptions, die w�hrend 
   * eines Schedulinglaufs aufgetreten sind
   * @param t
   */
  void handleThrowable(Throwable t);

  /**
   * R�ckgabe einer Id zum Auftrag
   * @param order
   * @return
   */
  long getOrderId(O order);

  
  
}