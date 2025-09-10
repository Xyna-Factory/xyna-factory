/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Generates Xyna Applications representing datamodel & client from Open API yaml schema.");
      System.out
          .println("Parameters: <Open API yaml schema file> <Generation Target (\"datamodel\", \"client\")> <Target directory (where generated application files will be placed)>");
      System.exit(2);
    }
    String yaml = args[0];
    String target = args[2];
    if (!Files.exists(Path.of(target)) || !Files.isDirectory(Path.of(target))) {
      System.out.println("");
      System.exit(4);
    }
    switch (args[1]) {
      case "client" :
      case "all" :
        new OasAppBuilder().createOasAppOffline("xmom-client", target, yaml);
      case "datamodel" :
        new OasAppBuilder().createOasAppOffline("xmom-data-model", target, yaml);
        break;
      default :
        System.out.println("Unexpected Generation Target: \"" + args[1] + "\".");
        System.exit(3);
    }
    System.out.println("Created applications in directory <" + target + "> successfully.");
  }

}
