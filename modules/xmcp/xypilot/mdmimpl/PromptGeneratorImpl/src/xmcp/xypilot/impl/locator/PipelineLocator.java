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
package xmcp.xypilot.impl.locator;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

import freemarker.template.Configuration;
import xmcp.xypilot.impl.Config;
import xmcp.xypilot.impl.gen.pipeline.Pipeline;
import xmcp.xypilot.impl.gen.pipeline.PipelineBuilder;

public class PipelineLocator {

    private final static Logger logger = Logger.getLogger("XyPilot");

    private final static Configuration cfg = Config.getTemplateConfiguration();

    private final static PipelineBuilder pipelineBuilder = new PipelineBuilder(cfg);

    public static String getPath(String pipelineName) {
        return Config.PIPELINE_PACKAGE_PATH + "/" + Config.model() + "/" + pipelineName + ".json";
    }

    public static <T, D> Pipeline<T, D> getPipeline(String pipelineName) throws IOException, InvalidJSONException, UnexpectedJSONContentException {
        logger.debug("Loading pipeline " + getPath(pipelineName) + "...");

        // load the pipeline from the jar
        try (InputStream in = PipelineLocator.class.getResourceAsStream(getPath(pipelineName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String json = reader.lines().reduce("", (a, b) -> a + b + "\n");
            return pipelineBuilder.build(json);
        }
    }
}
