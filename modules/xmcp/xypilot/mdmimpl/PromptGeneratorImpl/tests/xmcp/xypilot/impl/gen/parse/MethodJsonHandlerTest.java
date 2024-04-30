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

import xmcp.xypilot.MethodDefinition;
import xmcp.xypilot.Parameter;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.parse.json.JsonLineParser;
import xmcp.xypilot.impl.gen.parse.json.MethodJsonHandler;
import xmcp.xypilot.impl.util.DOMUtils;

public class MethodJsonHandlerTest {

    private static final String fqn = "test.xypilot.IPv4Address";
    private static DOM dom;

    MethodJsonHandler handler = null;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
    }

    @Before
    public void setUpBeforeTest() throws XynaException {
        dom = (DOM) DOMUtils.loadDomOrExceptionFromResourceDirectory(fqn);
        List<String> availableTypes = XynaFactory.getInstance().getDeployedDatatypes().get(-1L);
        List<String> availableExceptionTypes = XynaFactory.getInstance().getDeployedExceptions().get(-1L);

        handler = new MethodJsonHandler(dom, availableTypes, availableExceptionTypes);
    }

    @Test
    public void parseMethods() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_methods.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMethods().size());
        MethodDefinition parse = handler.getMethods().get(0);

        assertEquals("parse", parse.getName());
        assertEquals("Creates a new IPv4Address from a textual representation.", parse.getDocumentation());
        assertEquals(1, parse.getInputParams().size());
        assertEquals(1, parse.getOutputParams().size());
        assertEquals(1, parse.getThrowParams().size());

        Parameter text11 = parse.getInputParams().get(0);
        assertEquals("text11", text11.getName());
        assertEquals("base.Text", text11.getType());
        assertEquals(false, text11.getIsList());

        Parameter iPv4Address15 = parse.getOutputParams().get(0);
        assertEquals(null, iPv4Address15.getName());
        assertEquals("test.xypilot.IPv4Address", iPv4Address15.getType());
        assertEquals(false, iPv4Address15.getIsList());

        Parameter invalidAddressException = parse.getThrowParams().get(0);
        assertEquals(null, invalidAddressException.getName());
        assertEquals("test.xypilot.InvalidIPv4AddressException", invalidAddressException.getType());
        assertEquals(false, invalidAddressException.getIsList());

    }

    @Test
    public void parseMethodsIncomplete() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_methods_incomplete.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMethods().size());
        MethodDefinition parse = handler.getMethods().get(0);

        assertEquals("parse", parse.getName());
        assertEquals("Creates a new IPv4Address from a textual representation.", parse.getDocumentation());
        assertEquals(1, parse.getInputParams().size());
        assertEquals(1, parse.getOutputParams().size());
        assertEquals(0, parse.getThrowParams().size());

        Parameter text11 = parse.getInputParams().get(0);
        assertEquals("text11", text11.getName());
        assertEquals("base.Text", text11.getType());
        assertEquals(false, text11.getIsList());

        Parameter iPv4Address15 = parse.getOutputParams().get(0);
        assertEquals(null, iPv4Address15.getName());
        assertEquals("test.xypilot.IPv4Address", iPv4Address15.getType());
        assertEquals(false, iPv4Address15.getIsList());

    }

    @Test
    public void parseMethodsDirty0() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_methods_dirty_0.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMethods().size());
        MethodDefinition parse = handler.getMethods().get(0);

        assertEquals("parse", parse.getName());
        assertEquals("Creates a new IPv4Address from a textual representation.", parse.getDocumentation());
        assertEquals(1, parse.getInputParams().size());
        assertEquals(1, parse.getOutputParams().size());
        assertEquals(1, parse.getThrowParams().size());

        Parameter text11 = parse.getInputParams().get(0);
        assertEquals("text11", text11.getName());
        assertEquals("base.Text", text11.getType());
        assertEquals(false, text11.getIsList());

        Parameter iPv4Address15 = parse.getOutputParams().get(0);
        assertEquals(null, iPv4Address15.getName());
        assertEquals("test.xypilot.IPv4Address", iPv4Address15.getType());
        assertEquals(false, iPv4Address15.getIsList());

        Parameter invalidAddressException = parse.getThrowParams().get(0);
        assertEquals(null, invalidAddressException.getName());
        assertEquals("test.xypilot.InvalidIPv4AddressException", invalidAddressException.getType());
        assertEquals(false, invalidAddressException.getIsList());

    }

    @Test
    public void parseMethodsDirty1() throws Ex_FileWriteException {
        String json = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, fqn + "_parse_methods_dirty_1.json"));

        JsonLineParser.parse(json, handler);

        assertEquals(1, handler.getMethods().size());
        MethodDefinition parse = handler.getMethods().get(0);

        assertEquals("parse", parse.getName());
        assertEquals("Creates a new IPv4Address from a textual representation.", parse.getDocumentation());
        assertEquals(1, parse.getInputParams().size());
        assertEquals(1, parse.getOutputParams().size());
        assertEquals(1, parse.getThrowParams().size());

        Parameter text11 = parse.getInputParams().get(0);
        assertEquals("text11", text11.getName());
        assertEquals("base.Text", text11.getType());
        assertEquals(false, text11.getIsList());

        Parameter iPv4Address15 = parse.getOutputParams().get(0);
        assertEquals(null, iPv4Address15.getName());
        assertEquals("test.xypilot.IPv4Address", iPv4Address15.getType());
        assertEquals(false, iPv4Address15.getIsList());

        Parameter invalidAddressException = parse.getThrowParams().get(0);
        assertEquals(null, invalidAddressException.getName());
        assertEquals("test.xypilot.InvalidIPv4AddressException", invalidAddressException.getType());
        assertEquals(false, invalidAddressException.getIsList());

    }
}
