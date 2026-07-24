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
package xint.inference.impl;



import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;



public class ProcessInteraction {

  public static List<ProcessInfo> listProcesses() {
    List<ProcessInfo> result = new ArrayList<>();
    Stream<ProcessHandle> processes = ProcessHandle.allProcesses();
    processes.forEach(ph -> {
      ProcessHandle.Info info = ph.info();
      long pid = ph.pid();
      String command = info.command().orElse("unknown");
      String user = info.user().orElse("unknown");
      String[] args = info.arguments().orElse(new String[0]);
      result.add(new ProcessInfo(pid, command, user, args));

    });
    return result;
  }
  
  
  public static void runOrThrow(List<String> command, String errorPrefix) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    Process p = pb.start();
    String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exit = p.waitFor();

    if (exit != 0) {
      throw new IOException(errorPrefix + " (exit " + exit + ")\n" + "Command: " + String.join(" ", command) + "\n" + "Output:\n" + output);
    }
  }
}
