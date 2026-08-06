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
package xfmg.oas.offline;



import java.nio.file.Files;
import java.nio.file.Path;

import xfmg.oas.generation.tools.OasAppBuilder;



public class OASApplicationGeneration {

  private static void validateClientOptions(boolean generateMock, boolean generateDataCapture) {

    if (generateMock || generateDataCapture) {
      System.out.println("--generateMock and --generateDataCapture are only valid for client generation.");
      System.exit(3);
    }
  }


  public static void main(String[] args) {

    if (args.length < 3) {
      System.out.println("Generates Xyna Applications representing datamodel & client from Open API yaml schema.");
      System.out.println("Parameters: <Open API yaml schema file> "
              + "<Generation Target (\"datamodel\", \"client\", \"provider\", \"all\")> "
              + "<Target directory (where generated application files will be placed)> "
              + "[--generateMock] [--generateDataCapture]");
      System.exit(2);
    }

    String yaml = args[0];
    String generationTarget = args[1];
    String target = args[2];

    boolean generateMock = false;
    boolean generateDataCapture = false;

    for (int i = 3; i < args.length; i++) {
      switch (args[i]) {
        case "--generateMock" :
          generateMock = true;
          break;
        case "--generateDataCapture" :
          generateDataCapture = true;
          break;
        default :
          System.out.println("Unknown option: " + args[i]);
          System.exit(2);
      }
    }

    if (!Files.exists(Path.of(target)) || !Files.isDirectory(Path.of(target))) {
      System.out.println("Target parameter must be an existing directory");
      System.exit(4);
    }

    switch (generationTarget) {
      case "all" :
        new OasAppBuilder().createOasAppOffline("xmom-client", target, yaml, generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-server", target, yaml, false, false);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, yaml, false, false);
        break;
      case "provider" :
        validateClientOptions(generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-server", target, yaml, false, false);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, yaml, false, false);
        break;
      case "client" :
        new OasAppBuilder().createOasAppOffline("xmom-client", target, yaml, generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, yaml, false, false);
        break;
      case "datamodel" :
        validateClientOptions(generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, yaml, false, false);
        break;
      default :
        System.out.println("Unexpected Generation Target: \"" + generationTarget + "\".");
        System.exit(3);
    }

    System.out.println("Created applications in directory <" + target + "> successfully.");
  }

}
