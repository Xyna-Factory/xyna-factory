package xmcp.xypilot.impl.gen.template;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.model.DomVariableModel;
import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.pipeline.Prompt;
import xmcp.xypilot.impl.gen.util.DomUtils;
import xmcp.xypilot.impl.util.DOMUtils;


public class DatatypeTemplateTest {
    private static final String fqn = "test.xypilot.IPv4Address";
    private static final String method = "asText";

    private static TestTemplateConfiguration config;
    private static DOM dom;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
        config = new TestTemplateConfiguration();
        dom = DOMUtils.loadDOMFromResourceDirectory(fqn);
    }

    public static void datatypeVariablesTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-variables.ftl");
        Prompt prompt = Prompt.generate(new DomModel(dom), template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".json"));
    }

    public static void datatypeMethodsTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-methods.ftl");
        Prompt prompt = Prompt.generate(new DomModel(dom), template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".json"));
    }

    public static void datatypeDocumentationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-documentation.ftl");
        Prompt prompt = Prompt.generate(new DomModel(dom), template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void datatypeMethodImplementationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-method-implementation.ftl");
        Object model = new DomMethodModel(dom, dom.getOperationByName(method));
        Prompt prompt = Prompt.generate(model, template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void datatypeMemberDocumentationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-variable-documentation.ftl");
        Object model = new DomVariableModel(dom, DomUtils.getVariableByName(dom, "address"));
        Prompt prompt = Prompt.generate(model, template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    public static void datatypeMethodDocumentationTest(String path) throws XynaException, TemplateException, IOException {
        Template template = config.get().getTemplate(path + "/dom-method-documentation.ftl");
        Object model = new DomMethodModel(dom, dom.getOperationByName(method));
        Prompt prompt = Prompt.generate(model, template);
        FileUtils.writeStringToFile(prompt.toString(), new File(DOMUtils.resourceDirectory, fqn + ".java"));
    }

    @Test
    public void copilotDatatypeVariablesTest() throws XynaException, TemplateException, IOException {
        datatypeVariablesTest("copilot");
    }

    @Test
    public void copilotDatatypeMethodsTest() throws XynaException, TemplateException, IOException {
        datatypeMethodsTest("copilot");
    }

    @Test
    public void copilotDatatypeDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeDocumentationTest("copilot");
    }

    @Test
    public void copilotDatatypeMethodImplementationTest() throws XynaException, TemplateException, IOException {
        datatypeMethodImplementationTest("copilot");
    }

    @Test
    public void copilotDatatypeMemberDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeMemberDocumentationTest("copilot");
    }

    @Test
    public void copilotDatatypeMethodDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeMethodDocumentationTest("copilot");
    }

    @Test
    public void fauxpilotDatatypeVariablesTest() throws XynaException, TemplateException, IOException {
        datatypeVariablesTest("fauxpilot");
    }

    @Test
    public void fauxpilotDatatypeMethodsTest() throws XynaException, TemplateException, IOException {
        datatypeMethodsTest("fauxpilot");
    }

    @Test
    public void fauxpilotDatatypeDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeDocumentationTest("fauxpilot");
    }

    @Test
    public void fauxpilotDatatypeMethodImplementationTest() throws XynaException, TemplateException, IOException {
        datatypeMethodImplementationTest("fauxpilot");
    }

    @Test
    public void fauxpilotDatatypeMemberDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeMemberDocumentationTest("fauxpilot");
    }

    @Test
    public void fauxpilotDatatypeMethodDocumentationTest() throws XynaException, TemplateException, IOException {
        datatypeMethodDocumentationTest("fauxpilot");
    }
}
