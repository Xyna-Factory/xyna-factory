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

public class IndexLambda implements Mustache.InvertibleLambda {
  
  private int index;

  public IndexLambda(int start) {
    this.index = start;
  }
  
  public IndexLambda() {
    this.index = 0;
  }

  /**
   * write index and increment afterwards
   * use: {{#lambda.index}}{{/lambda.index}}
   * 
   * to reset the index to 'number' use:
   * {{^lambda.index}}number{{/lambda.index}}
   * No output is written in this case!
   * 
   */
  @Override
  public void execute(Template.Fragment fragment, Writer writer) throws IOException {
    this.executeInverse(fragment, writer);
    this.index++;
  }
  
  /**
   * write index and do not increment
   * use: {{^lambda.index}}{{/lambda.index}}
   * see 'execute' for more info
   */
  @Override
  public void executeInverse(Template.Fragment fragment, Writer writer) throws IOException {
    String text = fragment.execute();
    try {
      int setIndex = Integer.parseInt(text);
      this.index = setIndex;
    }
    catch(java.lang.NumberFormatException e){
      writer.write(Integer.toString(index));
    }
  };

}
