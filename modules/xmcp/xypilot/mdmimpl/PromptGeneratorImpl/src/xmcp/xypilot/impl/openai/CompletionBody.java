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
package xmcp.xypilot.impl.openai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import xmcp.xypilot.impl.gen.util.JsonUtils;

/**
 * Represents OpenAI Completion body. Provides functionality to parse itself to JSON.
 * Note, that some members aren't supported yet.
 */
public class CompletionBody {
    public String model;
    public String prompt;
    public String suffix;
    public Optional<Integer> max_tokens = Optional.empty();
    public Optional<Double> temperature = Optional.empty();
    public Optional<Double> top_p = Optional.empty();
    public Optional<Integer> n = Optional.empty();
    public Optional<Boolean> stream = Optional.empty();
    public Optional<Integer> logprobs = Optional.empty();
    public Optional<Boolean> echo = Optional.empty();
    public Optional<List<String>> stop = Optional.empty();
    public Optional<Double> presence_penalty = Optional.empty();
    public Optional<Double> frequency_penalty = Optional.empty();
    public Optional<Integer> best_of = Optional.empty();
    public Optional<Map<String, Double>> logit_bias = Optional.empty();
    public Optional<String> user = Optional.empty();

    public Map<String, Object> membersToMap() {
        Map<String, Object> ret = new HashMap<>();

        ret.put("model", model);
        ret.put("prompt", prompt);
        ret.put("suffix", suffix);
        if (max_tokens.isPresent()) {
            ret.put("max_tokens", max_tokens.get());
        }
        if (temperature.isPresent()) {
            ret.put("temperature", temperature.get());
        }
        if (top_p.isPresent()) {
            ret.put("top_p", top_p.get());
        }
        if (n.isPresent()) {
            ret.put("n", n.get());
        }
        if (logprobs.isPresent()) {
            ret.put("logprobs", logprobs.get());
        }
        if (echo.isPresent()) {
            ret.put("echo", echo.get());
        }
        if (stop.isPresent()) {
            ret.put("stop", stop.get());
        }
        if (presence_penalty.isPresent()) {
            ret.put("presence_penalty", presence_penalty.get());
        }
        if (frequency_penalty.isPresent()) {
            ret.put("frequency_penalty", frequency_penalty.get());
        }
        if (best_of.isPresent()) {
            ret.put("best_of", best_of.get());
        }
        if (logit_bias.isPresent()) {
            ret.put("logit_bias", logit_bias.get());
        }
        if (user.isPresent()) {
            ret.put("user", user.get());
        }

        return ret;
    }

    public String toJSON() {
        return JsonUtils.toJSON(membersToMap());
    }
}
