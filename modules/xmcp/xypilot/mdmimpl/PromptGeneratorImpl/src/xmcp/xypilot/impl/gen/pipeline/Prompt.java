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
package xmcp.xypilot.impl.gen.pipeline;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * A prompt consists of a prefix and a suffix which are sent to the ai model.
 * The ai model inserts the completion between the prefix and suffix.
 */
public class Prompt {
    // \u00A7 is the unicode character for §
    public static final String Marker = "\u00A7";

    public String prefix;
    public String suffix;

    public Prompt(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Returns a string representation of the prompt, which is the prefix and suffix concatenated with a marker in between.
     */
    public String toString() {
        return suffix.isEmpty() ? prefix : prefix.isEmpty() ? suffix : prefix + Marker + suffix;
    }

    /**
    * Generates a prompt from a template and data model.
    *
    * @param dataModel
    * @param template
    * @return
    * @throws TemplateException
    * @throws IOException
    */
    public static Prompt generate(Object dataModel, Template template) throws TemplateException, IOException {
       StringWriter writer = new StringWriter();
       template.process(dataModel, writer);
       String[] prompt = writer.toString().split(Marker, 2);
       return new Prompt(prompt[0], prompt.length == 2 ? prompt[1] : "");
   }

}
