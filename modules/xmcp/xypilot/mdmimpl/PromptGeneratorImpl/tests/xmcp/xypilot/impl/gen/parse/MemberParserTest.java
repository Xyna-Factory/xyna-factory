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
import com.gip.xyna.xprc.xfractwfe.generation.DOM;

import xmcp.xypilot.MemberVariable;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.util.DOMUtils;

public class MemberParserTest {
    private static final String fqn = "test.xypilot.IPv4Address";
    private DOM dom;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
    }

    @Before
    public void setUpBeforeTest() throws XynaException {
        dom = DOMUtils.loadDOMFromResourceDirectory(fqn);
    }


    @Test
    public void parseMembersTest() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_members.json"));

        List<String> availableTypes = XynaFactory.getInstance().getDeployedDatatypes().get(-1L);
        List<MemberVariable> gotMembers = MemberParser.parseMembers(json, dom, availableTypes);

        assertEquals(1, gotMembers.size());
        MemberVariable octets = gotMembers.get(0);

        assertEquals("octets", octets.getName());
        assertEquals(null, octets.getReferenceType());
        assertEquals("String", octets.getPrimitiveType());
        assertEquals(true, octets.getIsList());
        assertEquals("the octets of the IP address as a list of strings", octets.getDocumentation());
    }
}
