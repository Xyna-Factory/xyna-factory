/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package pkg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import xact.XScrpt.services.StringHelper;



public class TestSplitCommand {

  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  @Test
  public void test1() throws Exception {
    String cmd = "mkdir tmp";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"mkdir", "tmp"}, res);
  }
  
  @Test
  public void test2() throws Exception {
    String cmd = "cat /tmp/test123.txt";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"cat", "/tmp/test123.txt"}, res);
  }
  
  @Test
  public void test3() throws Exception {
    String cmd = "echo 'test' > /tmp/test123.txt";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"echo", "'test'", ">", "/tmp/test123.txt"}, res);
  }
  
  @Test
  public void test4() throws Exception {
    String cmd = "echo \"'test' > /tmp/test123.txt\"";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"echo", "\"'test' > /tmp/test123.txt\""}, res);
  }
  
  @Test
  public void test5() throws Exception {
    String cmd = "echo '\\'test\\' > \"/tmp/test123.txt\"'";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"echo", "'\\'test\\' > \"/tmp/test123.txt\"'"}, res);
  }
  
  
  public void testFail() throws Exception {
    String cmd = "mkdir tmp";
    String[] res = new StringHelper().splitCmdDefault(cmd);
    validate(new String[] {"mkdir", "tmp-"}, res);
  }
  
  private void validate(String[] expected, String[] data) {
    if (expected.length != data.length) {
      fail("Array length does not match expected length");
    }
    log("");
    log("######");
    for (int i = 0; i < data.length; i++) {
      log("##");
      log("Expected value: " + expected[i]);
      log("Received value: " + data[i]);
      assertEquals(expected[i], data[i]);
    }
  }
  
  
  public static void main(String[] args) {
    try {
      new TestSplitCommand().testFail();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
