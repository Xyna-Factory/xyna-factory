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
package xfmg.oas.mcp.impl.prompts;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xint.mcp.schema.GetPromptResult;
import xint.mcp.schema.Prompt;
import xint.mcp.schema.PromptArgument;
import xint.mcp.schema.PromptMessage;
import xint.mcp.schema.TextContent;



public class ImportSpecPrompt extends Prompt {

  private static final long serialVersionUID = 1L;


  public ImportSpecPrompt() {
    List<PromptArgument> args = new ArrayList<>();
    args.add(new PromptArgument.Builder().name("specFile").title("Specification file").description("Specification to import").required(true)
        .instance());
    args.add(new PromptArgument.Builder().name("createProvider").title("Create Provider Application")
        .description("Should a provider application be generated").required(false).instance());
    args.add(new PromptArgument.Builder().name("createClient").title("Create Client Application")
        .description("Should a client application be generated").required(false).instance());
    unversionedSetArguments(args);
    unversionedSetDescription("Generated Oas applications based on a specification file");
    unversionedSetName("importOasSpec");
    unversionedSetTitle("Import Oas Specification");
  }


  @Override
  public GetPromptResult get(JSONObject input) {
    boolean generateProvider = readBoolean("createProvider", input);
    boolean generateClient = readBoolean("createClient", input);
    String specFile = readString("specFile", input);
    boolean isJson = specFile.endsWith(".json");

    StringBuilder description = new StringBuilder();
    description.append("Generate ");
    if (generateProvider && generateClient) {
      description.append("provider and client applications ");
    } else if (generateProvider) {
      description.append("the provider application ");
    } else if (generateClient) {
      description.append("the client application ");
    } else {
      description.append("the datamodel application ");
    }
    description.append("from an OpenAPI specification.");
    StringBuilder contentBuilder = new StringBuilder();
    contentBuilder.append("Generate the Xyna ");

    if (generateProvider && generateClient) {
      contentBuilder.append("provider and client applications ");
    } else if (generateProvider) {
      contentBuilder.append("provider application ");
    } else if (generateClient) {
      contentBuilder.append("client application ");
    } else {
      contentBuilder.append("datamodel application ");
    }

    contentBuilder.append("from the OpenAPI specification in ");
    contentBuilder.append(specFile);
    contentBuilder.append(". Call the importOasSpec tool with spec set to the ");
    if (!isJson) {
      contentBuilder.append("exact contents of ");
      contentBuilder.append(specFile);
      contentBuilder.append(" as a string");
    } else {
      contentBuilder.append("parsed contents of ");
      contentBuilder.append(specFile);
    }
    contentBuilder.append(", and options set to { \"createProvider\": ");
    contentBuilder.append(generateProvider);
    contentBuilder.append(", \"createClient\": ");
    contentBuilder.append(generateClient);
    contentBuilder.append(" }.");


    PromptMessage.Builder message = new PromptMessage.Builder();
    message.role("user");
    message.content(new TextContent.Builder().type("text").text(contentBuilder.toString()).instance());

    return new GetPromptResult.Builder().description(description.toString()).messages(List.of(message.instance())).instance();
  }


  private String readString(String name, JSONObject input) {
    JSONValue member = input.getMember(name);
    if (member == null) {
      return "<missing>";
    }
    return member.getStringOrNumberValue();
  }


  private boolean readBoolean(String name, JSONObject input) {
    JSONValue member = input.getMember(name);
    if (member == null) {
      return false;
    }
    if (Objects.equals(member.getType(), "BOOLEAN")) {
      return member.getBooleanValue();
    }
    if (Objects.equals(member.getType(), "STRING")) {
      return "true".equalsIgnoreCase(member.getStringOrNumberValue());
    }
    return false;
  }
}
