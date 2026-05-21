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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

import freemarker.template.Configuration;


/**
 * Builds a pipeline from a json string.
 * See pipeline.schema.json for the expected format.
 */
public class PipelineBuilder {

    private Configuration cfg;


    public PipelineBuilder(Configuration cfg) {
        this.cfg = cfg;
    }

    public <T, D> Pipeline<T, D> build(int maxSuggestions, String json) throws InvalidJSONException, UnexpectedJSONContentException {
        // parse the pipeline json file
        JsonParser parser = new JsonParser();
        return parser.parse(json, new PipelineJsonVisitor<T, D>(maxSuggestions));
    }


    class InferenceParametersJsonVisitor extends EmptyJsonVisitor<InferenceParameters> {
        int maxSuggestions;
        
        public InferenceParametersJsonVisitor(int maxSuggestions) {
          this.maxSuggestions = maxSuggestions;
        }
        
        class LogitBiasJsonVisitor extends EmptyJsonVisitor<Map<String, Double>> {

            private Map<String, Double> logitBias = new HashMap<>();

            @Override
            public Map<String, Double> get() {
                return logitBias;
            }

            @Override
            public Map<String, Double> getAndReset() {
                Map<String, Double> result = logitBias;
                logitBias = new HashMap<>();
                return result;
            }

            @Override
            public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
                if (type != Type.Number) {
                    throw new UnexpectedJSONContentException("Unexpected type for logit_bias: " + type + ", expected a number");
                }
                logitBias.put(label, Double.parseDouble(value));
            }

        }

        private InferenceParameters parameters = new InferenceParameters();

        @Override
        public InferenceParameters get() {
            return parameters;
        }

        @Override
        public InferenceParameters getAndReset() {
            InferenceParameters result = parameters;
            parameters = new InferenceParameters();
            return result;
        }

        @Override
        public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
            switch (label) {
                case "model":
                    if (type != Type.String) {
                        throw new UnexpectedJSONContentException("Unexpected type for model: " + type + ", expected a string");
                    }
                    parameters.model = value;
                    break;
                case "max_tokens":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for max_tokens: " + type + ", expected a number");
                    }
                    parameters.max_tokens = Optional.of(Integer.parseInt(value));
                    break;
                case "temperature":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for temperature: " + type + ", expected a number");
                    }
                    parameters.temperature = Optional.of(Double.parseDouble(value));
                    break;
                case "top_p":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for top_p: " + type + ", expected a number");
                    }
                    parameters.top_p = Optional.of(Double.parseDouble(value));
                    break;
                case "n":
                    if (type == Type.String && value.equals("max")) {
                        parameters.n = Optional.of(maxSuggestions);
                    } else if (type == Type.Number) {
                        parameters.n = Optional.of(Integer.parseInt(value));
                    } else {
                        throw new UnexpectedJSONContentException("Unexpected value for n: " + value + ", expected a number or \"max\"");
                    }
                    break;
                case "stream":
                    if (type != Type.Boolean) {
                        throw new UnexpectedJSONContentException("Unexpected type for stream: " + type + ", expected a boolean");
                    }
                    parameters.stream = Optional.of(Boolean.parseBoolean(value));
                    break;
                case "logprobs":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for logprobs: " + type + ", expected a number");
                    }
                    parameters.logprobs = Optional.of(Integer.parseInt(value));
                    break;
                case "echo":
                    if (type != Type.Boolean) {
                        throw new UnexpectedJSONContentException("Unexpected type for echo: " + type + ", expected a boolean");
                    }
                    parameters.echo = Optional.of(Boolean.parseBoolean(value));
                    break;
                case "presence_penalty":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for presence_penalty: " + type + ", expected a number");
                    }
                    parameters.presence_penalty = Optional.of(Double.parseDouble(value));
                    break;
                case "frequency_penalty":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for frequency_penalty: " + type + ", expected a number");
                    }
                    parameters.frequency_penalty = Optional.of(Double.parseDouble(value));
                    break;
                case "best_of":
                    if (type != Type.Number) {
                        throw new UnexpectedJSONContentException("Unexpected type for best_of: " + type + ", expected a number");
                    }
                    parameters.best_of = Optional.of(Integer.parseInt(value));
                    break;
                case "user":
                    if (type != Type.String) {
                        throw new UnexpectedJSONContentException("Unexpected type for user: " + type + ", expected a string");
                    }
                    parameters.user = Optional.of(value);
                    break;
                default:
                    throw new UnexpectedJSONContentException("Unexpected attribute: " + label);
            }
        }

        @Override
        public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
            switch (label) {
                case "stop":
                    if (type != Type.String) {
                        throw new UnexpectedJSONContentException("Unexpected type for stop: " + type + ", expected a list of strings");
                    }
                    parameters.stop = Optional.of(values);
                    break;
                default:
                    throw new UnexpectedJSONContentException("Unexpected list: " + label);
            }
        }

        @Override
        public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
            switch (label) {
                case "logit_bias":
                    return new LogitBiasJsonVisitor();
                default:
                    throw new UnexpectedJSONContentException("Unexpected object: " + label);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void object(String label, Object value) throws UnexpectedJSONContentException {
            switch (label) {
                case "logit_bias":
                    parameters.logit_bias = Optional.of((Map<String, Double>) value);
                    break;
                default:
                    throw new UnexpectedJSONContentException("Unexpected object: " + label);
            }
        }
    }


    class PipelineJsonVisitor<T, D> extends EmptyJsonVisitor<Pipeline<T, D>> {
        private int maxSuggestions;
        private Pipeline<T, D> pipeline = new Pipeline<>();
        
        public PipelineJsonVisitor(int maxSuggestions) {
          this.maxSuggestions = maxSuggestions;
        }

        @SuppressWarnings("unchecked")
        private Parser<T, D> findParserByClassName(String className) throws NoSuchElementException {
            try {
                Class<Parser<T, D>> clazz = (Class<Parser<T, D>>) Class.forName(className);
                return clazz.getDeclaredConstructor().newInstance();
            } catch (
                ClassCastException |
                ClassNotFoundException |
                InstantiationException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException |
                NoSuchMethodException |
                SecurityException e
            ) {
                throw new NoSuchElementException("Could not find parser: " + className + ", " + e.getMessage());
            }
        }

        @Override
        public Pipeline<T, D> get() {
            return pipeline;
        }

        @Override
        public Pipeline<T, D> getAndReset() {
            Pipeline<T, D> result = pipeline;
            pipeline = new Pipeline<T, D>();
            return result;
        }

        @Override
        public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
            switch (label) {
                case "template":
                    if (type != Type.String) {
                        throw new UnexpectedJSONContentException("Unexpected type for template: " + type + ", expected a string");
                    }
                    try {
                        pipeline.setTemplate(cfg.getTemplate(value));
                    } catch (IOException e) {
                        throw new UnexpectedJSONContentException("Could not load template: " + value, e);
                    }
                    break;
                case "parser":
                    if (type != Type.String) {
                        throw new UnexpectedJSONContentException("Unexpected type for parser: " + type + ", expected a string");
                    }
                    try {
                        pipeline.setParser(findParserByClassName(value));
                    } catch (NoSuchElementException e) {
                        throw new UnexpectedJSONContentException("Could not find parser: " + value, e);
                    }
                    break;
                default:
                    throw new UnexpectedJSONContentException("Unexpected attribute: " + label);
            }
        }

        @Override
        public void object(String label, Object value) throws UnexpectedJSONContentException {
            switch (label) {
                case "inference_parameters":
                    pipeline.setInferenceParameters((InferenceParameters) value);
                    break;
                default:
                    throw new UnexpectedJSONContentException("Unexpected object: " + label);
            }
        }


        @Override
        public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
            switch (label) {
                case "inference_parameters":
                    return new InferenceParametersJsonVisitor(maxSuggestions);
                default:
                    throw new UnexpectedJSONContentException("Unexpected object: " + label);
            }
        }

    }
}
