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
package xmcp.xypilot.impl.gen.util;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.util.DOMUtils;

public class GeneratorUtilsTest {
    private static final String fqn = "test.xypilot.BuildIPv4Subnet";
    private static final String stepId = "step287";
    private static WF wf;
    private static StepMapping mapping;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        XynaFactory.setInstance(new TestXynaFactory());
        wf = DOMUtils.loadWFFromResourceDirectory(fqn);
        mapping = WorkflowUtils.findStepMapping(wf, stepId);
    }


    @Test
    public void turnMappingExpressionsToJavaTest() {
        String[] inputs = {
                "%1%=\"\\\"Write References like this: %0% ; nice\\\"\"", // don't act on string literals
                "%1%=concat(%0%[\"0\"], Hallo[\"\\\"4\\\"\"]; %0%[\"1\"])", // don't act on string literals
                "%1%.a=%0%.member[%0%[%0%[\"10\"]]]",
        };
        String[] expectedVals = {
                "out.iPv4Subnet292 = \"Write References like this: %0% ; nice\"",
                "out.iPv4Subnet292 = concat(in.text928[0], Hallo[\"4\"]; in.text928[1])",
                "out.iPv4Subnet292.a = in.text928.member[in.text928[in.text928[10]]]",
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = GeneratorUtils.turnMappingExpressionToJava(input, mapping, "in", "out");

            assertEquals(expected, got);
        }
    }

    @Test
    public void replaceMappingIdsTest() {
        String[] inputs = {
                "%1%=%0%",
                "%1%.hallo=%0%.fuschi",
                "%1%.a2=%0%.blups.f4",
                "    %NAN% = %0%  ", // unparsable ids are not replaced
                "%123%=concat(%0%.text, %0%.text)", // unkown ids are not replaced
        };
        String[] expectedVals = {
                "out.iPv4Subnet292=in.text928",
                "out.iPv4Subnet292.hallo=in.text928.fuschi",
                "out.iPv4Subnet292.a2=in.text928.blups.f4",
                "    %NAN% = in.text928  ",
                "%123%=concat(in.text928.text, in.text928.text)",
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = GeneratorUtils.replaceMappingIds(input, mapping, "in", "out");

            assertEquals(expected, got);
        }
    }


    @Test
    public void checkForKeyLiteralTest() {
        String[] inputs = {
            "\"true\"",
            "\"false\"",
            "\"True\"",
            "\"12\"",
            "\"0\"",
            "\"-16\"",
            "\"18.6\"",
            "\"-35.2\"",
            "\"70.0\"",
            "\"-99.00000\"",
            "\"Hallo\"",
            "\"Huhu true False\"",
        };
        String[] expectedVals = {
            "true",
            "false",
            "\"True\"",
            "12",
            "0",
            "-16",
            "18.6",
            "-35.2",
            "70.0",
            "-99.00000",
            "\"Hallo\"",
            "\"Huhu true False\"",
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = GeneratorUtils.checkForKeyLiteral(input);

            assertEquals(expected, got);
        }
    }

    @Test
    public void isEscapedTest() {
        String[] inputTexts = {
                "abcd",
                "\\\\a", // <- there are 2 backslashes in this string
                "\\\\\\a",
                "\\a",
                "hulo\\\\a",
                "hulo\\\\\\a",
                "huhu\"Hutzi"
        };
        int[] inputIndices = {
                3,
                2,
                3,
                1,
                6,
                7,
                4,
        };
        boolean[] expectedVals = {
                false,
                false,
                true,
                true,
                false,
                true,
                false,
        };

        for (int i = 0; i < inputTexts.length; i++) {
            String text = inputTexts[i];
            int index = inputIndices[i];
            boolean expected = expectedVals[i];

            boolean got = GeneratorUtils.isEscaped(text, index);
            assertEquals(expected, got);
        }
    }

    @Test
    public void indexOfNextValidTest() {
        String[] inputTexts = {
                "abcd",
                "a\"hallo\"hui",
                "ab\"huhu",
                "abc\"\"",
                "\"19.d\"vf",
                "\"huhu\\\"Baum\\\"\"",
                "\"\\\\\"", // <- this is the quivalent of "\"
                "\"\\\\\\\"\"" // <- this is "\\\""
        };
        int[] startIndices = {
                2,
                1,
                2,
                3,
                0,
                0,
                0,
                0,
        };
        int[] expectedVals = {
                -1,
                7,
                -1,
                4,
                5,
                13,
                3,
                5,
        };

        for (int i = 0; i < inputTexts.length; i++) {
            String input = inputTexts[i];
            int startIndex = startIndices[i];
            int expected = expectedVals[i];

            int got = GeneratorUtils.indexOfNextValid(input, startIndex, '"');

            assertEquals(expected, got);
        }
    }

}
