/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

//import org.junit.jupiter.api.Test;  // if Junit 5 is used?
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gip.xyna.xact.trigger.MessageEndCursor;
import com.gip.xyna.xact.trigger.SshjCipherFactory;
import com.gip.xyna.xact.trigger.SshjKeyAlgorithm;
import com.gip.xyna.xact.trigger.SshjMacFactory;
import com.hierynomus.sshj.key.KeyAlgorithm;

import net.schmizz.sshj.common.Factory;


public class TestNetConfTrigger {

  private void log(String txt) {
    System.out.println(txt);
  }
  
  @Test
  public void test1() throws Exception {
    try {
     Factory.Named<KeyAlgorithm> factory = com.hierynomus.sshj.key.KeyAlgorithms.SSHRSA();
     log(factory.getName());
     String name = "ssh-rsa";
     SshjKeyAlgorithm algo = new SshjKeyAlgorithm(name);
     log(algo.getName() + " | " + algo.getFactory().getName());
     assertEquals(factory.getName(), algo.getFactory().getName());
     log(SshjKeyAlgorithm.getDescription());
     log(SshjKeyAlgorithm.getDescription(SshjKeyAlgorithm.getDefaults()));
     log(SshjMacFactory.getDescription());
     log(SshjCipherFactory.getDescription());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testMessageEnd() throws Exception {
    String msg = "blabla ]]>]] bla ]]> ]]> bla]]>]]>blabla";
    MessageEndCursor pos = new MessageEndCursor();
    int matchpos = -1;
    for (int i = 0; i < msg.length(); i++) {
      char c = msg.charAt(i);
      pos.registerChar(c);
      boolean matched = pos.isMessageEndTokenFullyMatched();
      if (matched) {
        matchpos = i;
      }
      log("" + i + " -> " + String.valueOf(c) + " : " + matched);
    }
    assertEquals(33, matchpos);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestNetConfTrigger().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }

}
