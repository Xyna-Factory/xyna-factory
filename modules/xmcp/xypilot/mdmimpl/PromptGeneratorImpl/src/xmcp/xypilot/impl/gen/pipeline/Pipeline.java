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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import xmcp.xypilot.impl.openai.Client;
import xmcp.xypilot.impl.openai.CompletionBody;

/**
 * A pipeline holds all information to implement a xypilot feature.
 * This includes the prompt template, inference parameters to pass to the ai model, and a parser to convert the resulting completion to a xyna datatype.
 */
public class Pipeline<T, D> {
    private static final Logger logger = Logger.getLogger("XyPilot");

    private Template template;
    private Parser<T, D> parser;
    private InferenceParameters inferenceParameters;
    
    public Pipeline() { }

    /**
     * Runs the pipeline.
     * 1. Generates a prompt from the template and data model.
     * 2. Sends a completion request using the prompt and inference parameters.
     * 3. Parses the completion response.
     *
     * @return The result of parsing the completion response.
     * @throws TemplateException
     * @throws IOException
     */
    public Suggestions<T> run(D dataModel, String baseUri) throws TemplateException, IOException {
        Prompt prompt = Prompt.generate(dataModel, template);
        List<String> completions = getCompletions(prompt, baseUri);
        return new Suggestions<>(
            completions.stream().map(completion -> parser.parse(completion, dataModel)).collect(Collectors.toList())
        );
    }

    public Template getTemplate() {
        return template;
    }

    public Parser<T, D> getParser() {
        return parser;
    }

    public InferenceParameters getInferenceParameters() {
        return inferenceParameters;
    }

    public Pipeline<T, D> setTemplate(Template template) {
        this.template = template;
        return this;
    }

    public Pipeline<T, D> setParser(Parser<T, D> parser) {
        this.parser = parser;
        return this;
    }

    public Pipeline<T, D> setInferenceParameters(InferenceParameters parameters) {
        this.inferenceParameters = parameters;
        return this;
    }

    /**
     * Sends a completion request using the given prompt and internal inference parameters.
     * @param prompt
     * @return
     */
    private List<String> getCompletions(Prompt prompt, String baseUri) {
        CompletionBody body = new CompletionBody();
        body.model = inferenceParameters.model;
        body.prompt = prompt.prefix;
        body.suffix = prompt.suffix;
        body.max_tokens = inferenceParameters.max_tokens;
        body.temperature = inferenceParameters.temperature;
        body.top_p = inferenceParameters.top_p;
        body.n = inferenceParameters.n;
        body.stream = inferenceParameters.stream;
        body.logprobs = inferenceParameters.logprobs;
        body.echo = inferenceParameters.echo;
        body.stop = inferenceParameters.stop;
        body.presence_penalty = inferenceParameters.presence_penalty;
        body.frequency_penalty = inferenceParameters.frequency_penalty;
        body.best_of = inferenceParameters.best_of;
        body.logit_bias = inferenceParameters.logit_bias;
        body.user = inferenceParameters.user;


        logger.debug("Prompt:\n" + prompt);

        List<String> res = Client.getCompletion(body, baseUri);

        for (int i = 0; i < res.size(); i++) {
            logger.debug("Choice " + i + ":\n" + res.get(i));
        }

        return res;
    }

}
