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

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.pipeline.Prompt;
import xmcp.xypilot.impl.gen.util.WorkflowUtils;
import xmcp.xypilot.impl.util.DOMUtils;


public class MappingTemplateTest {
    private static final String fqn = "test.xypilot.BuildIPv4Subnet";
    private static final String stepId = "step287";
    private static TestTemplateConfiguration config;
    private static WF wf;
    private static StepMapping mapping;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
        config = new TestTemplateConfiguration();
        wf = DOMUtils.loadWFFromResourceDirectory(fqn);
        mapping = WorkflowUtils.findStepMapping(wf, stepId);
    }

    public static void mappingAssignmentsTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/mapping-assignments.ftl");
        Prompt prompt = Prompt.generate(new MappingModel(mapping) , template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void mappingLabelTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/mapping-label.ftl");
        Prompt prompt = Prompt.generate(new MappingModel(mapping) , template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    @Test
    public void copilotMappingAssignmentsTest() throws XynaException, TemplateException, IOException {
        mappingAssignmentsTest("copilot");
    }

    @Test
    public void copilotMappingLabelTest() throws XynaException, TemplateException, IOException {
        mappingLabelTest("copilot");
    }

    @Test
    public void fauxpilotMappingAssignmentsTest() throws XynaException, TemplateException, IOException {
        mappingAssignmentsTest("fauxpilot");
    }

    @Test
    public void fauxpilotMappingLabelTest() throws XynaException, TemplateException, IOException {
        mappingLabelTest("fauxpilot");
    }
}
