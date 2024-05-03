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

/**
 * Stores one or multiple suggestions to choose from.
 * The number of suggestions generated can be configured in the pipeline.
 */
public class Suggestions<T> {
    private final List<T> choices;

    public Suggestions(List<T> choices) {
        this.choices = choices;
    }

    public List<T> choices() {
        return choices;
    }

    public boolean isEmpty() {
        return choices.isEmpty();
    }

    public boolean isSingle() {
        return choices.size() == 1;
    }

    public int numChoices() {
        return choices.size();
    }

    public T firstChoice() {
        return choices.get(0);
    }

}
