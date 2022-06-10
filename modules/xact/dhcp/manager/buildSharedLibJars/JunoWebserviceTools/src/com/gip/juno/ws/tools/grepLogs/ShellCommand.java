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

package com.gip.juno.ws.tools.grepLogs;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.LocationTools.LocationsRow;
import com.gip.juno.ws.tools.ssh.SshTools;
import com.gip.xyna.utils.ssh.Ssh;


/**
 * class that wraps a StringBuilder;
 * intended to build a string that contains a huge unix shell command;
 *
 * method execute() executes the content of the StringBuilder as shell command via ssh;
 *
 * the other methods do all the same thing: adding content to the string builder; but they allow
 * to construct the shell command in a more organized and self-explaining way than by directly
 * accessing a StringBuilder
 *
 */
public class ShellCommand {

  protected StringBuilder _builder = new StringBuilder("");

  public ShellCommand addCommand(String val) {
    _builder.append(" ").append(val);
    return this;
  }

  public void addPipeSign() {
    _builder.append(" |");
  }

  public void addLogicalAnd() {
    _builder.append(" &&");
  }

  public ShellCommand option(String val) {
    _builder.append(" ").append(val);
    return this;
  }

  public ShellCommand option(String name, String val) {
    _builder.append(" ").append(name).append(" ").append(val);
    return this;
  }

  public ShellCommand option(String name, int val) {
    _builder.append(" ").append(name).append(" ").append(val);
    return this;
  }

  public ShellCommand parameter(String val) {
    _builder.append(" ").append(val);
    return this;
  }

  public ShellCommand parameter(int val) {
    _builder.append(" ").append(val);
    return this;
  }

  public ShellCommand parameterList(List<String> list) {
    for (String str : list) {
      _builder.append(" ").append(str);
    }
    return this;
  }

  public String getCommandString() {
    return _builder.toString();
  }

  public String execute(LocationsRow row, Logger logger) throws RemoteException {
    Ssh ssh = SshTools.openSshConnection(row, logger);
    String command = _builder.toString();
    String ret = SshTools.execForOutput(ssh, command, logger);
    SshTools.closeConnection(ssh, logger);
    return ret;
  }

}
