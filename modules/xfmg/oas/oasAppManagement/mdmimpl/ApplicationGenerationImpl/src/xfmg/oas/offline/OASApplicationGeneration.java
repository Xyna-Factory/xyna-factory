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

import xfmg.oas.generation.tools.AppGenerationData;
import xfmg.oas.generation.tools.OasAppBuilder;
import xfmg.oas.generation.tools.OasImportStatusHandler;



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
              + "--oasVersion <oas-base-version> "
              + "[--generateMock] [--generateDataCapture]");
      System.exit(2);
    }

    String yaml = args[0];
    String generationTarget = args[1];
    String target = args[2];

    boolean generateMock = false;
    boolean generateDataCapture = false;
    String oasVersion = "";

    for (int i = 3; i < args.length; i++) {
      switch (args[i]) {
        case "--generateMock" :
          generateMock = true;
          break;
        case "--generateDataCapture" :
          generateDataCapture = true;
          break;
        case "--oasVersion" :
          if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
            System.out.println("Missing value for option: --oasVersion");
            System.exit(2);
          }
          oasVersion = args[++i];
          break;
        default :
          System.out.println("Unknown option: " + args[i]);
          System.exit(2);
      }
    }
    
    if(oasVersion.isBlank()) {
      System.out.println("--oasVersion required");
      System.exit(3);
    }

    if (!Files.exists(Path.of(target)) || !Files.isDirectory(Path.of(target))) {
      System.out.println("Target parameter must be an existing directory");
      System.exit(4);
    }

    AppGenerationData data = new AppGenerationData(yaml, oasVersion, generateMock, generateDataCapture, new OasImportStatusHandler());
    
    switch (generationTarget) {
      case "all" :
        new OasAppBuilder().createOasAppOffline("xmom-client", target, data);
        new OasAppBuilder().createOasAppOffline("xmom-server", target, data);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, data);
        break;
      case "provider" :
        validateClientOptions(generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-server", target, data);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, data);
        break;
      case "client" :
        new OasAppBuilder().createOasAppOffline("xmom-client", target, data);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, data);
        break;
      case "datamodel" :
        validateClientOptions(generateMock, generateDataCapture);
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, data);
        break;
      default :
        System.out.println("Unexpected Generation Target: \"" + generationTarget + "\".");
        System.exit(3);
    }

    System.out.println("Created applications in directory <" + target + "> successfully.");
  }

}
