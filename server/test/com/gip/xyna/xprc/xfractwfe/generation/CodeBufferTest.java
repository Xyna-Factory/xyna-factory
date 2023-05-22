/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation;

import junit.framework.TestCase;


public class CodeBufferTest extends TestCase {

  private static final String DEP = "testdep";

  private static final String HEADER = "/*----------------------------------------------------\n" + "* Xyna 6.1 (Black Edition)\n" + "* "
  + DEP + "\n" + "*----------------------------------------------------\n"
  + "* Copyright GIP AG 2015\n" + "* (http://www.gip.com)\n" + "* Hechtsheimer Str. 35-37\n"
  + "* 55131 Mainz\n" + "*----------------------------------------------------\n" + "* $"
  + "Revision:$\n" + "* $" + "Date:$\n" + "*----------------------------------------------------\n"
  + "*/\n";
  
  
  public void testLines() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.addLine("a");
    cb.addLine("b");
    cb.addLine("c");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "a;\nb;\nc;\n", code);
  }
  
  public void testIntendation() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.addLine("a {");
    cb.addLine("b");
    cb.addLine("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "a {\n  b;\n}\n", code);
  }
  
  public void testIntendation2() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.addLine("a {");
    cb.addLine("b");
    cb.addLine("{");
    cb.addLine("c");
    cb.addLine("}");
    cb.addLine("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "a {\n  b;\n  {\n    c;\n  }\n}\n", code);
  }
  
  public void testIntendationWithSegments() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.add("{").addLB();
    cb.add("{").add("a").add("b,").add("c").add("}").addLB();
    cb.add("{").add("a;").add("}").addLB();
    cb.add("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "{\n  {ab,c}\n  {a;}\n}", code);
  }
  
  public void testLinePartsAndLB() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.add("a").add("b").add("c").addLB().add("d").addLB(2);
    cb.addLine("p {");
    cb.add("a").addLB();
    cb.addLine("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "abc;\nd;\n\np {\n  a;\n}\n", code);
  }
  
  public void testComments() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.addLine("//a");
    cb.addLine("/**b");
    cb.addLine("*b");
    cb.addLine("*/");
    cb.addLine("{");
    cb.addLine("int a = 3"); //codebuffer ist dumm - dass das "*4" zu dieser zeile geh�rt, erkennt er nicht
    cb.addLine("*4");
    cb.addLine("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "//a\n/**b\n*b\n*/\n{\n  int a = 3;\n  *4\n}\n", code);
  }
  
  public void testListElements() {
    CodeBuffer cb = new CodeBuffer(DEP);
    cb.addLine("{");
    cb.add("int[] a = {");
    cb.addListElement("a");
    cb.addListElement("b");
    cb.addListElement("{");
    cb.addListElement("c");
    cb.add("}").addLB();
    cb.addLine("}");
    String code = cb.toString();
    assertEquals("expected different code", HEADER + "{\n  int[] a = {a, b, {, c}\n}\n", code);
  }

}
