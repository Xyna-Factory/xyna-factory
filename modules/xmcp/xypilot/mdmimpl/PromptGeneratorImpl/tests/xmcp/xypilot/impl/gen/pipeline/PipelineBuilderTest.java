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
