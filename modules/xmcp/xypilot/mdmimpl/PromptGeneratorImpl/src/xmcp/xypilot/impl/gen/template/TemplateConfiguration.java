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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.Version;
import xmcp.xypilot.impl.gen.template.preprocess.TemplatePreprocessor;

public class TemplateConfiguration {

    @SafeVarargs
    public static Configuration getBaseConfiguration(
        Version version,
        TemplateLoader baseLoader,
        TemplatePreprocessor ...preprocessors
    ) {
        Configuration cfg = new Configuration(version);

        if (preprocessors.length > 0) {

            List<TemplatePreprocessor> preprocessorsList = new ArrayList<>();
            Queue<TemplatePreprocessor> preprocessorsQueue = new ArrayDeque<>(List.of(preprocessors));

            // add required dependency preprocessors
            while (!preprocessorsQueue.isEmpty()) {
                TemplatePreprocessor preprocessor = preprocessorsQueue.poll();
                preprocessorsList.add(preprocessor);
                preprocessorsQueue.addAll(
                    preprocessor.dependencies().stream()
                        .filter(dep -> preprocessorsList.stream().noneMatch(p -> p.getClass().equals(dep)))
                        .map(dep -> {
                            try {
                                return dep.getConstructor().newInstance();
                            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                                throw new RuntimeException(
                                    "Missing dependency template preprocessor'"
                                    + dep.getSimpleName()
                                    + "'' could not be instantiated. Try adding it to the preprocessors list manually.", e);
                            }
                        })
                        .collect(Collectors.toList())
                );
            }

            // sort preprocessors by dependencies
            preprocessorsList.sort((a, b) -> {
                if (a.dependencies().contains(b.getClass())) {
                    return 1;
                } else if (b.dependencies().contains(a.getClass())) {
                    return -1;
                } else {
                    return 0;
                }
            });

            // add required directives
            for (TemplatePreprocessor preprocessor : preprocessorsList) {
                for (Map.Entry<String, Class< ? extends TemplateDirectiveModel>> entry : preprocessor.requiredDirectives().entrySet()) {
                    try {
                        cfg.setSharedVariable(
                            entry.getKey(),
                            entry.getValue().getConstructor().newInstance()
                        );
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // handles preprocessing of templates
            cfg.setTemplateLoader(new CustomTemplateLoader(
                baseLoader,
                preprocessorsList
            ));

        } else {
            cfg.setTemplateLoader(baseLoader);
        }

        return cfg;
    }

}
