/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import java.io.File;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.gip.xyna.FileUtils;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;



public class CompilationTest extends TestCase {

  private static String CLASS_TEST1;
  private static String CLASS_TEST2;
  private static String CLASS_TEST3_WITH_INNER_CLASS;
  private static String CLASS_TEST4;
  private static String CLASS_TEST5;
  private static String CLASS_TEST6;
  private static String CLASS_TEST7;
  private static String CLASS_TEST7_WITH_DIFFERENT_INNER_CLASS;
  private static String CLASS_TEST7_WITH_COMPILEERROR;
  static {
    CodeBuffer cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST1 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test2 {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST2 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test3 {").addLB();
    cb.addLine("private static class Bla {").addLB();
    cb.addLine("}").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST3_WITH_INNER_CLASS = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test4 {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return new Test5().test()");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST4 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test5 {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST5 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test6 {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return new Test7.Inner().test()");
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST6 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test7 {").addLB();
    cb.addLine("static class Inner {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST7 = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test7 {").addLB();
    cb.addLine("static class OtherInner {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
    cb.addLine("}");
    CLASS_TEST7_WITH_DIFFERENT_INNER_CLASS = cb.toString();
    cb = new CodeBuffer("test");
    cb.addLine("package test.compilation").addLB();
    cb.addLine("public class Test7 {").addLB();
    cb.addLine("static class OtherInner {").addLB();
    cb.addLine("public String test() {");
    cb.addLine("return \"test\"");
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
    cb.addLine("private void x() {Test100.a();}");
    cb.addLine("}");
    CLASS_TEST7_WITH_COMPILEERROR = cb.toString();
  }


  public void testObsoleteInnerClassesAreRemoved() throws XPRC_CompileError {

    File fMain = new File("Test7.class");
    File fInner1 = new File("Test7$Inner.class");
    File fInner2 = new File("Test7$OtherInner.class");
    fMain.delete();
    fInner1.delete();
    fInner2.delete();

    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7);
    s.addToCompile(jsfs);
    s.compile();

    assertTrue(fMain.exists());
    assertTrue(fInner1.exists());
    assertFalse(fInner2.exists());

    s = new InMemoryCompilationSet(false, true, false);
    jsfs = new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7_WITH_DIFFERENT_INNER_CLASS);
    s.addToCompile(jsfs);
    s.compile();

    assertTrue(fMain.exists());
    assertFalse(fInner1.exists());
    assertTrue(fInner2.exists());    

  }

  public void testObsoleteInnerClassesAreNotRemovedAtCompileError() throws XPRC_CompileError {
    File fMain = new File("Test7.class");
    File fInner1 = new File("Test7$Inner.class");
    File fInner2 = new File("Test7$OtherInner.class");
    fMain.delete();
    fInner1.delete();
    fInner2.delete();
    File fTest1 = new File("Test.class");
    fTest1.delete();

    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7);
    s.addToCompile(jsfs);
    s.compile();

    s = new InMemoryCompilationSet(false, true, false);
    jsfs = new JavaSourceFromString("test.compilation.Test", CLASS_TEST1);
    s.addToCompile(jsfs);
    jsfs = new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7_WITH_COMPILEERROR);
    s.addToCompile(jsfs);
    try {
      s.compile();
      assertTrue("expected compileerror", false);
    } catch (XPRC_CompileError e) {
      //ok
      assertTrue(s.getSuccessfullyCompiled().contains("test.compilation.Test"));
    }

    assertTrue(fTest1.exists());
    assertTrue(fMain.exists());
    assertTrue(fInner1.exists());
    assertFalse(fInner2.exists());  
  }

  public void testEmpty() throws XPRC_CompileError {
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    s.compile();
  }


  public void testSingleClass() throws XPRC_CompileError {
    File f = new File("Test.class");
    f.delete();
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test", CLASS_TEST1);
    s.addToCompile(jsfs);
    s.compile();
    assertTrue(new File("Test.class").exists());
  }


  public void testClassDir() throws XPRC_CompileError {
    File f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test", CLASS_TEST1);
    s.addToCompile(jsfs);
    s.compile();
    assertTrue(new File("mydir1/test/compilation/Test.class").exists());
  }


  public void testDependencies() throws XPRC_CompileError, InterruptedException {
    File f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    s.addToCompile(new JavaSourceFromString("test.compilation.Test5", CLASS_TEST5));
    s.compile();
    assertTrue(new File("mydir1/test/compilation/Test5.class").exists());
    assertFalse(new File("mydir1/test/compilation/Test4.class").exists());

    f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    s.addToCompile(new JavaSourceFromString("test.compilation.Test4", CLASS_TEST4));
    try {
      s.compile();
      assertTrue("expected compileerror", false);
    } catch (XPRC_CompileError e) {
      assertTrue(e.getMessage().contains("cannot find symbol"));
      Pattern symbolPattern = Pattern.compile(".*symbol\\s*\\:\\s*class Test5.*", Pattern.DOTALL);
      assertTrue(symbolPattern.matcher(e.getMessage()).matches());
    }
    assertFalse(new File("mydir1/test/compilation/Test4.class").exists());
    assertFalse(new File("mydir1/test/compilation/Test5.class").exists());

    f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    s.addToCompile(new JavaSourceFromString("test.compilation.Test4", CLASS_TEST4));
    s.addToCompile(new JavaSourceFromString("test.compilation.Test5", CLASS_TEST5));
    s.compile();
    assertTrue(new File("mydir1/test/compilation/Test4.class").exists());
    assertTrue(new File("mydir1/test/compilation/Test5.class").exists());

  }


  public void testDependenciesToInnerClass() throws XPRC_CompileError, InterruptedException {
    File f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    s.addToCompile(new JavaSourceFromString("test.compilation.Test6", CLASS_TEST6));
    s.addToCompile(new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7));
    s.compile();
    assertTrue(new File("mydir1/test/compilation/Test6.class").exists());
    assertTrue(new File("mydir1/test/compilation/Test7.class").exists());
  }


  public void testPartialCompileError() throws XPRC_CompileError, InterruptedException {
    File f = new File("mydir1");
    FileUtils.deleteDirectory(f);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    s.setClassDir("mydir1");
    /*
     * compilereihenfolge richtet sich scheinbar danach, in welcher reihenfolge man die argumente an den compiler übergibt.
     * bei test4 bricht er ab, deshalb wird 6 und 7 nicht mehr kompiliert.
     * 
     * so einfach ist es nicht. es kommt auf die art von compilefehler an. bei schlimmeren fehlern wird schon früher abgebrochen und dann gar nichts kompiliert.
     */
    s.addToCompile(new JavaSourceFromString("test.compilation.Test", CLASS_TEST1));
    s.addToCompile(new JavaSourceFromString("test.compilation.Test4", CLASS_TEST4)); //-> fehler
    s.addToCompile(new JavaSourceFromString("test.compilation.Test6", CLASS_TEST6));
    s.addToCompile(new JavaSourceFromString("test.compilation.Test7", CLASS_TEST7));
    try {
      s.compile();
    } catch (XPRC_CompileError e) {
    }
    assertTrue(new File("mydir1/test/compilation/Test.class").exists());
    assertFalse(new File("mydir1/test/compilation/Test4.class").exists());
    assertFalse(new File("mydir1/test/compilation/Test6.class").exists());
    assertEquals(1, s.getSuccessfullyCompiled().size());
    assertTrue(s.getSuccessfullyCompiled().contains("test.compilation.Test"));
    assertEquals(1, s.getUnsuccessfullyCompiled().size());
    assertTrue(s.getUnsuccessfullyCompiled().keySet().contains("test.compilation.Test4"));
  }


  public void testSeparateOutputDirs() throws XPRC_CompileError {
    File f = new File("mydir");
    FileUtils.deleteDirectory(f);
    File f2 = new File("mydir2");
    FileUtils.deleteDirectory(f2);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test", CLASS_TEST1);
    //jsfs.setClassOutputLocation("mydir");
    s.addToCompile(jsfs);
    JavaSourceFromString jsfs2 = new JavaSourceFromString("test.compilation.Test2", CLASS_TEST2);
    //jsfs2.setClassOutputLocation("mydir2");
    s.addToCompile(jsfs2);
    s.compile();
    assertTrue(new File("mydir/test/compilation/Test.class").exists());
    assertTrue(new File("mydir2/test/compilation/Test2.class").exists());
  }


  public void testOutputDirOfInnerClass() throws XPRC_CompileError {
    File f = new File("mydir3");
    FileUtils.deleteDirectory(f);
    InMemoryCompilationSet s = new InMemoryCompilationSet(false, true, false);
    JavaSourceFromString jsfs = new JavaSourceFromString("test.compilation.Test3", CLASS_TEST3_WITH_INNER_CLASS);
    //jsfs.setClassOutputLocation("mydir3");
    s.addToCompile(jsfs);
    s.compile();
    assertTrue(new File("mydir3/test/compilation/Test3.class").exists());
    assertTrue(new File("mydir3/test/compilation/Test3$Bla.class").exists());
  }

}