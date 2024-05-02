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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

import xmcp.xypilot.impl.Config;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.parse.JavaCodeParser;
import xmcp.xypilot.impl.gen.template.TestTemplateConfiguration;
import xmcp.xypilot.impl.util.ResourceUtils;
import xmcp.xypilot.metrics.Code;

public class PipelineBuilderTest {

    private static PipelineBuilder builder;
    private static TestTemplateConfiguration config;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());

        config = new TestTemplateConfiguration();
        builder = new PipelineBuilder(config.get());
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void buildTest() throws Ex_FileWriteException, InvalidJSONException, UnexpectedJSONContentException {
        String json = ResourceUtils.readFileFromResourceDirectory("copilot-dom-method-implementation.json");
        Pipeline<Code, DomMethodModel> pipeline = builder.build(json);

        assertEquals(JavaCodeParser.class, pipeline.getParser().getClass());
        assertEquals("copilot/dom-method-implementation.ftl", pipeline.getTemplate().getName());
        assertEquals("copilot-java", pipeline.getInferenceParameters().model);
        assertEquals(Config.maxSuggestions(), pipeline.getInferenceParameters().n.get().intValue());
        assertEquals("\n  }", pipeline.getInferenceParameters().stop.get().get(0));
    }
}
