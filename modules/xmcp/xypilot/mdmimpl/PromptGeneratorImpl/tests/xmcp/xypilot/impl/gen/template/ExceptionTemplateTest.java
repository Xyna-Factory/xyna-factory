package xmcp.xypilot.impl.gen.template;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.ExceptionModel;
import xmcp.xypilot.impl.gen.model.ExceptionVariableModel;
import xmcp.xypilot.impl.gen.pipeline.Prompt;
import xmcp.xypilot.impl.gen.util.DomUtils;
import xmcp.xypilot.impl.util.DOMUtils;

public class ExceptionTemplateTest {

    private static final String fqn = "test.xypilot.InvalidIPv4AddressException";
    private static TestTemplateConfiguration config;
    private static ExceptionGeneration exception;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
        config = new TestTemplateConfiguration();
        exception = DOMUtils.loadExceptionFromResourceDirectory(fqn);
    }

    public static void exceptionMessagesTest(String path) throws XynaException, IOException, TemplateException {
        Template template = config.get().getTemplate(path + "/exception-messages.ftl");
        Prompt prompt = Prompt.generate(new ExceptionModel(exception) , template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void exceptionVariablesTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/exception-variables.ftl");
        Prompt prompt = Prompt.generate(new ExceptionModel(exception) , template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".json"));
    }

    public static void exceptionDocumentationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/exception-documentation.ftl");
        Prompt prompt = Prompt.generate(new ExceptionModel(exception) , template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void exceptionMemberDocumentationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/exception-variable-documentation.ftl");
        Object model = new ExceptionVariableModel(exception, DomUtils.getVariableByName(exception, "ipAddress"));
        Prompt prompt = Prompt.generate(model, template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    @Test
    public void copilotExceptionMessagesTest() throws XynaException, IOException, TemplateException {
        exceptionMessagesTest("copilot");
    }

    @Test
    public void copilotExceptionVariablesTest() throws XynaException, IOException, TemplateException {
        exceptionVariablesTest("copilot");
    }

    @Test
    public void copilotExceptionDocumentationTest() throws XynaException, IOException, TemplateException {
        exceptionDocumentationTest("copilot");
    }

    @Test
    public void copilotExceptionMemberDocumentationTest() throws XynaException, IOException, TemplateException {
        exceptionMemberDocumentationTest("copilot");
    }

    @Test
    public void fauxpilotExceptionMessagesTest() throws XynaException, IOException, TemplateException {
        exceptionMessagesTest("fauxpilot");
    }

    @Test
    public void fauxpilotExceptionVariablesTest() throws XynaException, IOException, TemplateException {
        exceptionVariablesTest("fauxpilot");
    }

    @Test
    public void fauxpilotExceptionDocumentationTest() throws XynaException, IOException, TemplateException {
        exceptionDocumentationTest("fauxpilot");
    }

    @Test
    public void fauxpilotExceptionMemberDocumentationTest() throws XynaException, IOException, TemplateException {
        exceptionMemberDocumentationTest("fauxpilot");
    }

}
