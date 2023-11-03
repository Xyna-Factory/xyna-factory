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
