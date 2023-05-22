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
package com.gip.xyna.utils.shell;

import java.io.File;
import java.util.concurrent.TimeUnit;


public class ShellCommand {
  
  private String command;
  private File executionDirectory = new File("."); 
  private String[] enviromentalProperties = new String[0];
  private long timeoutMillis = -1;
  private BehaviourOnTimeout behaviourOnTimeout = BehaviourOnTimeout.RETURN_PARTIAL_RESULT;
  private boolean resetTimeoutOnReceive;
  
  public static enum BehaviourOnTimeout {
    ERROR, RETURN_PARTIAL_RESULT;
  }
  
  private ShellCommand() {
  }
  
  
  public static ShellCommand cmd(String command) {
    ShellCommand cmd = new ShellCommand();
    cmd.command = command;
    return cmd;
  }
  
  
  public ShellCommand execDir(String executionDirectory) {
    return execDir(new File(executionDirectory));
  }
  
  
  public ShellCommand execDir(File executionDirectory) {
    this.executionDirectory = executionDirectory;
    return this;
  }
  
  
  public ShellCommand env(String[] enviromentalProperties) {
    this.enviromentalProperties = enviromentalProperties;
    return this;
  }
  
  
  public ShellCommand timeout(long timeoutMillis, BehaviourOnTimeout behaviourOnTimeout) {
    return timeout(timeoutMillis, behaviourOnTimeout, false);
  }
  
  public ShellCommand timeout(long timeoutMillis, BehaviourOnTimeout behaviourOnTimeout, boolean resetTimeoutOnReceive) {
    this.timeoutMillis = timeoutMillis;
    this.behaviourOnTimeout = behaviourOnTimeout;
    this.resetTimeoutOnReceive = resetTimeoutOnReceive;
    return this;
  }
  
  
  public ShellCommand timeout(long timeoutMillis) {
    return timeout(timeoutMillis, BehaviourOnTimeout.RETURN_PARTIAL_RESULT);
  }
  
  
  public ShellCommand timeout(int timeout, TimeUnit unit, BehaviourOnTimeout behaviourOnTimeout) {
    return timeout(unit.toMillis(timeout), behaviourOnTimeout);
  }
  
  public ShellCommand timeout(int timeout, TimeUnit unit, BehaviourOnTimeout behaviourOnTimeout, boolean resetTimeoutOnReceive) {
    return timeout(unit.toMillis(timeout), behaviourOnTimeout, resetTimeoutOnReceive);
  }
  
  public ShellCommand timeout(int timeout, TimeUnit unit) {
    return timeout(unit.toMillis(timeout));
  }

  
  public String getCommand() {
    return command;
  }

  
  public File getExecutionDirectory() {
    return executionDirectory;
  }

  
  public String[] getEnviromentalProperties() {
    return enviromentalProperties;
  }

  
  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  
  public BehaviourOnTimeout getBehaviourOnTimeout() {
    return behaviourOnTimeout;
  }
  
  
  public boolean doResetTimeoutOnReceive() {
    return resetTimeoutOnReceive;
  }
  
  
  @Override
  public String toString() {
    return command;
  }
  
  
}
