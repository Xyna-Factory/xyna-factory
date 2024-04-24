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
