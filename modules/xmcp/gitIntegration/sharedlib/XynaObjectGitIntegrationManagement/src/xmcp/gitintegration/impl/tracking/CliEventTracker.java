/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xmcp.gitintegration.impl.tracking;



import java.io.OutputStream;

import com.gip.xyna.xmcp.xfcli.CommandLineWriter;



public class CliEventTracker implements OperationTracker {

  private final OutputStream statusOutputStream;


  public CliEventTracker(OutputStream statusOutputStream) {
    this.statusOutputStream = statusOutputStream;
  }


  @Override
  public void trackInfo(String message) {
    CommandLineWriter.createCommandLineWriter(statusOutputStream).writeLineToCommandLine("INFO: " + message);
  }


  @Override
  public void trackError(String message) {
    CommandLineWriter.createCommandLineWriter(statusOutputStream).writeLineToCommandLine("ERROR: " + message);
  }


}
