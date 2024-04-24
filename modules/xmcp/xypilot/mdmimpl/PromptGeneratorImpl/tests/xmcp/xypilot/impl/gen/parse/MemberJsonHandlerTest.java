package xmcp.xypilot.impl.gen.parse;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;

import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.parse.json.JsonLineParser;
import xmcp.xypilot.impl.gen.parse.json.MemberJsonHandler;
import xmcp.xypilot.impl.util.DOMUtils;

public class MemberJsonHandlerTest {

    private static final String fqn = "test.xypilot.IPv4Address";
    private DomOrExceptionGenerationBase dom;

    MemberJsonHandler handler = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
    }

    @Before
    public void setUpBeforeTest() throws XynaException {
        dom = DOMUtils.loadDomOrExceptionFromResourceDirectory(fqn);

        List<String> availableTypes = XynaFactory.getInstance().getDeployedDatatypes().get(-1L);
        handler = new MemberJsonHandler(dom, availableTypes);
    }

    @Test
    public void parseMembers() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMembers().size());
        MemberVariable octets = handler.getMembers().get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("String", octets.getPrimitiveType());
        assertEquals(true, octets.getIsList());
        assertEquals("the octets of the IP address as a list of strings", octets.getDocumentation());
    }

    @Test
    public void parseMembersIncomplete0() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members_incomplete_0.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMembers().size());
        MemberVariable octets = handler.getMembers().get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("String", octets.getPrimitiveType());
        assertEquals(false, octets.getIsList());
        assertEquals(null, octets.getDocumentation());
    }

    @Test
    public void parseMembersDirty0() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members_dirty_0.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMembers().size());
        MemberVariable octets = handler.getMembers().get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("int", octets.getPrimitiveType());
        assertEquals(true, octets.getIsList());
        assertEquals(null, octets.getDocumentation());
    }

    @Test
    public void parseMembersDirty1() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members_dirty_1.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMembers().size());
        MemberVariable octets = handler.getMembers().get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("int", octets.getPrimitiveType());
        assertEquals(true, octets.getIsList());
        assertEquals(null, octets.getDocumentation());
    }

    @Test
    public void parseMembersEarlyOut1() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members_early_out_0.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMembers().size());
        MemberVariable octets = handler.getMembers().get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("int", octets.getPrimitiveType());
        assertEquals(true, octets.getIsList());
        assertEquals(null, octets.getDocumentation());
    }
}
