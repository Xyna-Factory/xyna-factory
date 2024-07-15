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
import java.io.Reader;
import java.util.List;

import freemarker.cache.TemplateLoader;
import xmcp.xypilot.impl.gen.template.preprocess.TemplatePreprocessor;

/**
 * This class is used to load templates and modify them before they are passed to the FreeMarker engine.
 */
public class CustomTemplateLoader implements TemplateLoader {

    private TemplateLoader baseTemplateLoader;
    private List<TemplatePreprocessor> preprocessors;


    public CustomTemplateLoader(TemplateLoader baseTemplateLoader) {
        this.baseTemplateLoader = baseTemplateLoader;
    }

    public CustomTemplateLoader(TemplateLoader baseTemplateLoader, TemplatePreprocessor ...preprocessors) {
        this.baseTemplateLoader = baseTemplateLoader;
        this.preprocessors = java.util.Arrays.asList(preprocessors);
    }

    public CustomTemplateLoader(TemplateLoader baseTemplateLoader, List<TemplatePreprocessor> preprocessors) {
        this.baseTemplateLoader = baseTemplateLoader;
        this.preprocessors = preprocessors;
    }

    public TemplateLoader getBaseTemplateLoader() {
        return baseTemplateLoader;
    }

    public void setBaseTemplateLoader(TemplateLoader baseTemplateLoader) {
        this.baseTemplateLoader = baseTemplateLoader;
    }

    public List<TemplatePreprocessor> getPreprocessors() {
        return preprocessors;
    }

    public void setPreprocessors(List<TemplatePreprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    public void addPreprocessor(TemplatePreprocessor preprocessor) {
        preprocessors.add(preprocessor);
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return baseTemplateLoader.findTemplateSource(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        return baseTemplateLoader.getLastModified(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        Reader reader = baseTemplateLoader.getReader(templateSource, encoding);
        for (TemplatePreprocessor preprocessor : preprocessors) {
            reader = preprocessor.process(reader);
        }
        return reader;
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        baseTemplateLoader.closeTemplateSource(templateSource);
    }

}
