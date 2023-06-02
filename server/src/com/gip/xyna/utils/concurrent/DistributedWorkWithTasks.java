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
package com.gip.xyna.utils.concurrent;



import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;



/**
 * liste von auszuführenden tasks, die in beliebiger reihenfolge ausgeführt werden dürfen.
 * beliebig viele threads können sich an der arbeit beteiligen. auch rekursion möglich (ein thread kann ein anderes task
 * ausführen, während er bereits an einem arbeitet).
 * bei rekursion darf der rekursions-aufruf von {@link #executeAndWaitForCompletion()} nicht warten, sondern muss nach
 * getaner arbeit sofort zurückkehren.
 * 
 * falls die ausführung eines tasks einen fehler hat, merkt dies nur der eine thread. für die anderen threads sieht es so
 * aus als wäre das task erfolgreich ausgeführt.
 * 
 * zwei verwendungsmuster.
 * entweder:
 * DistributedWorkWithTasks work = new DistributedWorkWithTasks(list);
 * work.executeAndWaitForCompletion();
 * 
 * oder siehe oberklassen {@link DistributedWork}
 *
 * @param <E> exception der tasks
 */
public class DistributedWorkWithTasks<E extends XynaException> extends DistributedWork {

  public interface Task<E extends XynaException> {

    void run() throws E;

  }


  private final List<Task<E>> work;


  public DistributedWorkWithTasks(List<Task<E>> work) {
    super(work.size());
    this.work = work;
  }


  /**
   * kann mehrfach vom gleichen oder verschiedenen threads aufgerufen werden.
   * @throws InterruptedException 
   */
  public void executeAndWaitForCompletion() throws InterruptedException, E {
    Task<E> t;
    while (null != (t = findAndLockOpenTask())) {
      try {
        t.run();
      } finally {
        taskDone();
      }
    }
    waitForCompletion();
  }


  private Task<E> findAndLockOpenTask() {
    int idx = getAndLockNextOpenTaskIdx();
    if (idx > -1) {
      return work.get(idx);
    }
    return null;
  }

}
