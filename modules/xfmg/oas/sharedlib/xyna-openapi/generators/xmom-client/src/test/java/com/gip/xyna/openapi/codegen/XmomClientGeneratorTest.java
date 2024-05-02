/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.openapi.codegen;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/***
 * This test allows you to easily launch your code generation software under a debugger.
 * Then run this test under debug mode.  You will be able to step through your java code
 * and then see the results in the out directory.
 *
 * To experiment with debugging your code generator:
 * 1) Set a break point in XmomClientGenerator.java in the postProcessOperationsWithModels() method.
 * 2) To launch this test in Eclipse: right-click | Debug As | JUnit Test
 *
 */
public class XmomClientGeneratorTest {

  // use this test to launch you code generator in the debugger.
  // this allows you to easily set break points in MyclientcodegenGenerator.
  @Test
  public void launchCodeGenerator() {

    String specFile = "";
    PrintStream originalOut = System.out;
    try {
      Path outputFilePath = Paths.get("../../test/output/xmom-client/" + specFile.substring(0, specFile.lastIndexOf('.')) + "/operations.json");
      File file = outputFilePath.toFile();
      if(!file.exists()) {
        file.getParentFile().mkdirs();
        file.createNewFile();
      }
      PrintStream out = new PrintStream(Files.newOutputStream(outputFilePath), true);
      System.setOut(out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // to understand how the 'openapi-generator-cli' module is using 'CodegenConfigurator', have a look at the 'Generate' class:
    // https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-cli/src/main/java/org/openapitools/codegen/cmd/Generate.java
    final CodegenConfigurator configurator = new CodegenConfigurator()
      .setGeneratorName("xmom-client") // use this codegen library
      .setInputSpec("../../test/resources/" + specFile) // sample OpenAPI file
      .setOutputDir("../../test/output/xmom-client/" + specFile.substring(0, specFile.lastIndexOf('.'))) // output directory
      .addAdditionalProperty("debugXO", Boolean.TRUE);
    final ClientOptInput clientOptInput = configurator.toClientOptInput();
    DefaultGenerator generator = new DefaultGenerator();
    generator.opts(clientOptInput).generate();
    
    
    System.setOut(originalOut);
  }
}