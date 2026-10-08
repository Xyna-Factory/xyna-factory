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
package xmcp.gitintegration.impl.references.methods;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;

import xmcp.gitintegration.impl.references.InternalReference;
import xmcp.gitintegration.impl.references.ReferenceMethods;



public class BuildMethods implements ReferenceMethods {

  private static Logger logger = CentralFactoryLogging.getLogger(BuildMethods.class);
  
  @Override
  public List<File> execute(InternalReference reference) {

    ProcessBuilder pb = new ProcessBuilder("ant", "build");
    pb.directory(new File(reference.getPathToRepo(), reference.getPath()));
    pb.redirectErrorStream(true);

    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      return Collections.emptyList();
    }

    Thread outputLogger = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          logger.debug(line);
        }
      } catch (IOException e) {
        logger.debug("Failed to read build output", e);
      }
    }, "build-methods-output-logger");
    outputLogger.setDaemon(true);
    outputLogger.start();

    try {
      if (process.waitFor() != 0) {
        try {
          outputLogger.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return Collections.emptyList();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      try {
        outputLogger.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return Collections.emptyList();
    }
    
    try {
      outputLogger.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Collections.emptyList();
    }

    Path deployDir = Path.of(reference.getPathToRepo(), reference.getPath(), "deploy");
    List<File> files = new ArrayList<>();
    FileUtils.findFilesRecursively(deployDir.toFile(), files, (x, y) -> true);
    return files;
  }

}
