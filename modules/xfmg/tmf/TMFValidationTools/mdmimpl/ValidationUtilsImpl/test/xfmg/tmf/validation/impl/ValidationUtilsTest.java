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
package xfmg.tmf.validation.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import xfmg.tmf.validation.ConstraintV2;
import xfmg.tmf.validation.ConstraintValidationResultV2;

public class ValidationUtilsTest {

  @Test
  public void testReordering() {
    List<ConstraintV2> l = new ArrayList<>();
    l.add(new ConstraintV2.Builder().name("n1").instance());
    l.add(new ConstraintV2.Builder().name("n2").instance());
    l.add(new ConstraintV2.Builder().name("n3").instance());
    l.add(new ConstraintV2.Builder().name("n4").instance());
    l.add(new ConstraintV2.Builder().name("n5").instance());
    l.add(new ConstraintV2.Builder().name("n6").instance());
    l.add(new ConstraintV2.Builder().name("n7").instance());
    l.add(new ConstraintV2.Builder().name("n8").instance());
    l.add(new ConstraintV2.Builder().name("n9").instance());
    List<ConstraintValidationResultV2> r = new ArrayList<>();
    r.add(new ConstraintValidationResultV2.Builder().name("n6").instance());
    r.add(new ConstraintValidationResultV2.Builder().name("n1").instance());
    r.add(new ConstraintValidationResultV2.Builder().name("n7").instance());
    r.add(new ConstraintValidationResultV2.Builder().name("n3").instance());
    r.add(new ConstraintValidationResultV2.Builder().name("n4").instance());
    ValidationUtilsServiceOperationImpl.reorder(r, l);
    assertEquals("n1", r.get(0).getName());
    assertEquals("n3", r.get(1).getName());
    assertEquals("n4", r.get(2).getName());
    assertEquals("n6", r.get(3).getName());
    assertEquals("n7", r.get(4).getName());
  }
  
}
