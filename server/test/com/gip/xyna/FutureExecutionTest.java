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
package com.gip.xyna;



import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;



public class FutureExecutionTest extends TestCase {

  public void testBeforeCorrelation() {
    FutureExecution fe = new FutureExecution("test");
    final int id1 = fe.nextId();
    final AtomicInteger solution = new AtomicInteger(2);
    FutureExecutionTask task1 = new FutureExecutionTask(id1) {

      @Override
      public void execute() {
        solution.addAndGet(1);
      }

    };
    fe.execAsync(task1);

    int id2 = fe.nextId();
    FutureExecutionTask task2 = new FutureExecutionTask(id2) {

      @Override
      public void execute() {
        solution.set(solution.get() * 2);
      }


      @Override
      public int[] before() {
        return new int[] {id1};
      }


    };
    fe.execAsync(task2);
    assertEquals(2, solution.get());

    fe.finishedRegistrationProcess();
    assertEquals(5, solution.get());
  }


  public void testAfterCorrelation() {
    FutureExecution fe = new FutureExecution("test");
    final int id1 = fe.nextId();
    final int id2 = fe.nextId();
    final AtomicInteger solution = new AtomicInteger(2);
    FutureExecutionTask task1 = new FutureExecutionTask(id1) {

      @Override
      public void execute() {
        solution.addAndGet(1);
      }


      @Override
      public int[] after() {
        return new int[] {id2};
      }
    };
    fe.execAsync(task1);

    FutureExecutionTask task2 = new FutureExecutionTask(id2) {

      @Override
      public void execute() {
        solution.set(solution.get() * 2);
      }


    };
    fe.execAsync(task2);
    assertEquals(2, solution.get());

    fe.finishedRegistrationProcess();
    assertEquals(5, solution.get());
  }


  public void testNoWait() {
    FutureExecution fe = new FutureExecution("test");
    final int id1 = fe.nextId();
    final AtomicInteger solution = new AtomicInteger(2);
    FutureExecutionTask task1 = new FutureExecutionTask(id1) {

      @Override
      public void execute() {
        solution.addAndGet(1);
      }


      @Override
      public boolean waitForOtherTasksToRegister() {
        return false;
      }

    };
    fe.execAsync(task1);

    int id2 = fe.nextId();
    FutureExecutionTask task2 = new FutureExecutionTask(id2) {

      @Override
      public void execute() {
        solution.set(solution.get() * 2);
      }


      @Override
      public int[] before() {
        return new int[] {id1};
      }


    };
    fe.execAsync(task2);
    assertEquals(3, solution.get());

    fe.finishedRegistrationProcess();
    assertEquals(6, solution.get());
  }


  public void testCircularDependency() {
    FutureExecution fe = new FutureExecution("test");
    final int id1 = fe.nextId();
    final int id2 = fe.nextId();
    final AtomicInteger solution = new AtomicInteger(2);
    FutureExecutionTask task1 = new FutureExecutionTask(id1) {

      @Override
      public void execute() {
        solution.addAndGet(1);
      }


      @Override
      public int[] after() {
        return new int[] {id2};
      }


    };
    fe.execAsync(task1);

    FutureExecutionTask task2 = new FutureExecutionTask(id2) {

      @Override
      public void execute() {
        solution.set(solution.get() * 2);
      }


      @Override
      public int[] after() {
        return new int[] {id1};
      }


    };
    fe.execAsync(task2);
    assertEquals(2, solution.get());

    try {
      fe.finishedRegistrationProcess();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("circular") || e.getMessage().contains("cyclic"));
      return;
    }
    assertTrue(false);
  }


  public void testMemoryOfOldExecution() {
    FutureExecution fe = new FutureExecution("test");
    final int id1 = fe.nextId();
    final int id2 = fe.nextId();
    final AtomicInteger solution = new AtomicInteger(2);
    FutureExecutionTask task1 = new FutureExecutionTask(id1) {

      @Override
      public void execute() {
        solution.addAndGet(1);
      }


      @Override
      public boolean waitForOtherTasksToRegister() {
        return false;
      }


      @Override
      public int[] after() {
        return new int[] {id2};
      }
    };
    fe.execAsync(task1);

    FutureExecutionTask task2 = new FutureExecutionTask(id2) {

      @Override
      public void execute() {
        solution.set(solution.get() * 2);
      }


    };
    fe.execAsync(task2);
    assertEquals(2, solution.get());

    fe.finishedRegistrationProcess();
    assertEquals(5, solution.get());

    fe.execAsync(task1);
    assertEquals(6, solution.get());
  }

}
