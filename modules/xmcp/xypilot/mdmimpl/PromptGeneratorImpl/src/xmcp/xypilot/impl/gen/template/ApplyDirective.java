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
package xmcp.xypilot.impl.gen.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;

/**
 * Applies prefixes to its body across line breaks, i.e. each line of the body is prefixed.
 * Optionally, a suffix can be appended at the end of the body.
 * The resulting snippet is only rendered if it is not blank.
 */
public class ApplyDirective implements TemplateDirectiveModel {

    private static final String LINE_FEED = "\n";

    private static final DirectiveParameters.Def<TemplateScalarModel> PREFIX = new DirectiveParameters.Def<>(
        "prefix",
        TemplateScalarModel.class,
        true
    );

    private static final DirectiveParameters.Def<TemplateScalarModel> SUFFIX = new DirectiveParameters.Def<>(
        "suffix",
        TemplateScalarModel.class,
        false
    );

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void execute(
        Environment environment,
        Map parameters,
        TemplateModel[] templateModels,
        TemplateDirectiveBody body
    ) throws TemplateException, IOException {

        // read parameters
        final DirectiveParameters params = new DirectiveParameters(parameters);
        params.validate(PREFIX, SUFFIX);
        final String prefix = params.getOrDefault(PREFIX, (TemplateScalarModel) TemplateScalarModel.EMPTY_STRING).getAsString();
        final String suffix = params.getOrDefault(SUFFIX, (TemplateScalarModel) TemplateScalarModel.EMPTY_STRING).getAsString();

        // retrieve the body (content within apply directive) as string
        final StringWriter writer = new StringWriter();
        body.render(writer);
        final String string = writer.toString();

        String lines = Arrays.stream(string.split(LINE_FEED))
            .map(line -> prefix + line)
            .collect(Collectors.joining(LINE_FEED)) + suffix;
        if (!lines.isBlank()) {
            environment.getOut().write(lines);
        }
    }

}