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
package com.gip.xyna.xfmg.xopctrl.passwordcreation;

import junit.framework.TestCase;

import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.CreationAlgorithm;


public class CreationAlgorithmTest extends TestCase{

  public void testCreateAndCheck() {
    String password = "Xbt&8�J�3$n";
    String salt = "u2(aw1UH!=o";
    Integer rounds = 6;
    
    for (CreationAlgorithm algo : CreationAlgorithm.values()) {
      String hashed = algo.createPassword(password, salt, rounds);
      
      assertTrue(algo.checkPassword(password, hashed));
      assertFalse(algo.checkPassword(password+"x", hashed));
    }
  }
}
