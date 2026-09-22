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
package com.gip.xyna.openapi.codegen.templating.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.Writer;

public class PathParameterLambda implements Mustache.Lambda {

  public PathParameterLambda() {

  }

  /**
   * create concat statement with replaced path parameters for mappings
   * use: {{#lambda.pathparam}}{{path}}{{/lambda.pathparam}}
   */
  @Override
  public void execute(Template.Fragment fragment, Writer writer) throws IOException {
    String text = fragment.execute();
    text = "\"" + text + "\"";
    if (text.contains("{") && text.contains("}")) {
        text = text.replaceAll("\\{", "\", %1%.");
        text = text.replaceAll("\\}\"", "");  // if parameter at the end of the path
        text = text.replaceAll("\\}", ", \"");
        writer.write("concat(" + text + ")");
    }
    else {
        writer.write(text);
    }
  }

}
