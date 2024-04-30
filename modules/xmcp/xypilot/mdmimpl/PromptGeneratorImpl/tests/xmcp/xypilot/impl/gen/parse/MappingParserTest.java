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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.xypilot.MappingAssignment;
import xmcp.xypilot.impl.factory.TestXynaFactory;
import xmcp.xypilot.impl.factory.XynaFactory;
import xmcp.xypilot.impl.gen.util.WorkflowUtils;
import xmcp.xypilot.impl.util.DOMUtils;


public class MappingParserTest {
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
    public void parseAssignmentsTest() {
        String[][] inputs = {
            {
                "this.iPv4Subnet292.b = text928[15];",
                "this.iPv4Subnet292 = text928;"
            },
            {
                "huhu",
                "if (a = b) { return null; }",
                "this.iPv4Subnet292.=text928"
            },
            {
                "this.iPv4Subnet292.ente = text928.kuh;",
                "this.iPv4Subnet292.maus = text928.pferd",
                "       this.iPv4Subnet292=text928.aal      ",
                "",
                "this.iPv4Subnet292.baum =text928;",
                "this.iPv4Subnet292.a.b= text928.c.d.e;  ",
                "this.iPv4Subnet292.a = concat(\"hallo text928\", text928, text928.asString(), 12L);",
                "this.iPv4Subnet292 = getAll(text928.getArr()[text928], text928[14]);",
                "this.iPv4Subnet292=concat(\"Du musst test[0] aufrufen\", text928[0])",  // don't change things in string literals
            }
        };
        List<List<String>> expectedVals = List.of(
            Arrays.asList(
                "%1%.b=%0%[\"15\"]",
                "%1%=%0%"
            ),
            Arrays.asList(
                null,
                null,
                null
            ),
            Arrays.asList(
                "%1%.ente=%0%.kuh",
                "%1%.maus=%0%.pferd",
                "%1%=%0%.aal",
                null,
                "%1%.baum=%0%",
                "%1%.a.b=%0%.c.d.e",
                "%1%.a=concat(\"\\\"hallo text928\\\"\", %0%, %0%.asString(), \"12L\")",
                "%1%=getAll(%0%.getArr()[%0%], %0%[\"14\"])",
                "%1%=concat(\"\\\"Du musst test[0] aufrufen\\\"\", %0%[\"0\"])"
            )
        );

        for (int i = 0; i < inputs.length; i++) {
            List<String> expected = expectedVals.get(i);

            List<MappingAssignment> got = Arrays.asList(inputs[i]).stream()
                .map(input -> MappingParser.parseAssignment(input, mapping))
                .collect(Collectors.toList());

            for (int k = 0; k < expected.size(); k++) {
                assertEquals(expected.get(k), got.get(k) == null? null : got.get(k).getExpression());
            }
        }
    }

    @Test
    public void parserExpressionCompletionTest() {
        MappingParser.parseExpressionCompletion(".bestand = \"OKAY\";", mapping);
    }

    @Test
    public void parseExpressionTest() {
        String[] inputs = {
                "\"Hello\"",
                "12",
                "12L",
                "12l",
                "10.6",
                "-10.6d",
                "-10.6f",
                "-18.9456F",
                "true",
                "false",
                "\"12\"",
                "\"This is the number 4 four\"",
                "\"Hello \\\"Ben\\\"\"",
                "\"12\", 10L, true, \"false 17.0f\", \"abc\"",
                "hutziButzi(\"12\", 10L, true, \"false 17.0f\", \"abc\")",
                "hutziButzi(\"12\", text928.add(text928), text928.text, \"false 17.0f\", \"abc\")",
                "concat(\"This is a long string\", \" replacements text928.member.length \", \" should happen here: false\", \"var[1]\")",
                "this.iPv4Subnet292",
                "text928.lengthof",
                "text928.oflength",
                "Hello this.iPv4Subnet292, text928.hutzi.butzi.length",
                "text928.add(text928.test, 12, \"hutzi\", text928.length)",
                "this.iPv4Subnet292",
                "0, 1, 2, 3",
                "-1, -2, -3, 1",
                "hallo5d 13k -4L",
                "15.6f",
                "0.0D",
                "huhu 15La",
                "a.test(10, 20L, math.pi, -36.7d)",
                "true",
                "false",
                "hello_true",
                "fuschi\" false,",
                ";true;false,false,true",
                "hi(true, 10, false, yellow-true)",
                ".bestand = \"OKAY\""
        };
        String[] expectedVals = {
                "\"\\\"Hello\\\"\"",
                "\"12\"",
                "\"12L\"",
                "\"12l\"",
                "\"10.6\"",
                "\"-10.6d\"",
                "\"-10.6f\"",
                "\"-18.9456F\"",
                "\"true\"",
                "\"false\"",
                "\"\\\"12\\\"\"",
                "\"\\\"This is the number 4 four\\\"\"",
                "\"\\\"Hello \\\"Ben\\\"\\\"\"",
                "\"\\\"12\\\"\", \"10L\", \"true\", \"\\\"false 17.0f\\\"\", \"\\\"abc\\\"\"",
                "hutziButzi(\"\\\"12\\\"\", \"10L\", \"true\", \"\\\"false 17.0f\\\"\", \"\\\"abc\\\"\")",
                "hutziButzi(\"\\\"12\\\"\", %0%.add(%0%), %0%.text, \"\\\"false 17.0f\\\"\", \"\\\"abc\\\"\")",
                "concat(\"\\\"This is a long string\\\"\", \"\\\" replacements text928.member.length \\\"\", \"\\\" should happen here: false\\\"\", \"\\\"var[1]\\\"\")",
                "%1%",
                "%0%.lengthof",
                "%0%.oflength",
                "Hello %1%, length(%0%.hutzi.butzi)",
                "%0%.add(%0%.test, \"12\", \"\\\"hutzi\\\"\", length(%0%))",
                "%1%",
                "\"0\", \"1\", \"2\", \"3\"",
                "\"-1\", \"-2\", \"-3\", \"1\"",
                "hallo5d 13k \"-4L\"",
                "\"15.6f\"",
                "\"0.0D\"",
                "huhu 15La",
                "a.test(\"10\", \"20L\", math.pi, \"-36.7d\")",
                "\"true\"",
                "\"false\"",
                "hello_true",
                "fuschi\" \"false\",",
                ";\"true\";\"false\",\"false\",\"true\"",
                "hi(\"true\", \"10\", \"false\", yellow-\"true\")",
                ".bestand = \"\\\"OKAY\\\"\""
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = MappingParser.parseExpression(input, mapping);

            assertEquals(expected, got);
        }
    }


    @Test
    public void buildAssignmentExpressionTest() {
        assertEquals(
            "%1%[%0%.index + 15].sub.member=%0%.hello(15)",
            MappingParser.buildAssignmentExpression(1, "%0%.index + 15", "sub.member", "%0%.hello(15)")
        );
    }


    @Test
    public void enquoteLiterals() {
        String[] inputs = {
            "\"12\", 10L, true, \"false 17.0f\", \"abc\""
        };
        String[] expectedVals = {
            "\"12\", \"10L\", \"true\", \"false 17.0f\", \"abc\""
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = MappingParser.enquoteLiterals(input);

            assertEquals(expected, got);
        }
    }

}
