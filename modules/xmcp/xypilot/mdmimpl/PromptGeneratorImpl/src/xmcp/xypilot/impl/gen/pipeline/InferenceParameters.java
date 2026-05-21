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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InferenceParameters {
    public String model;
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
}
