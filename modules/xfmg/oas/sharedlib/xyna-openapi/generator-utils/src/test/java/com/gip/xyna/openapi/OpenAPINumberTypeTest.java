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
package com.gip.xyna.openapi;

import java.util.List;

import org.junit.jupiter.api.Test;

public class OpenAPINumberTypeTest {

  @Test
  void testMinInteger() {
    List<String> errorMessages;
    NumberTypeValidator<Integer> oi = new NumberTypeValidator<Integer>();
    oi.setName("int");
    oi.setValue(10);
    oi.setMin(-10);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0);
    assert (oi.checkValid().size() == 0);

    oi.setMin(10);
    assert (oi.checkValid().size() == 0);

    oi.setMin(11);
    errorMessages = oi.checkValid();
    System.out.println(errorMessages);
    assert (errorMessages.size() == 1);

    oi.setMin(10);
    oi.setExcludeMin();
    errorMessages = oi.checkValid();
    System.out.println(errorMessages);
    assert (errorMessages.size() == 1);

    oi.setMin(11);
    errorMessages = oi.checkValid();
    System.out.println(errorMessages);
    assert (oi.checkValid().size() == 1);

    NumberTypeValidator<Integer> oMax = new NumberTypeValidator<Integer>();
    oMax.setName("int");
    oMax.setValue(Integer.MAX_VALUE);
    oMax.setMin(Integer.MAX_VALUE);
    assert (oMax.checkValid().size() == 0);

    oMax.setExcludeMin();
    assert (oMax.checkValid().size() == 1);

    NumberTypeValidator<Integer> oMin = new NumberTypeValidator<Integer>();
    oMin.setName("int");
    oMin.setValue(Integer.MIN_VALUE);
    oMin.setMin(Integer.MIN_VALUE);
    assert (oMin.checkValid().size() == 0);

    oMin.setExcludeMin();
    assert (oMin.checkValid().size() == 1);
  }

  @Test
  void testMinLong() {
    NumberTypeValidator<Long> oi = new NumberTypeValidator<Long>();
    oi.setName("long");
    oi.setValue(10L);
    oi.setMin(-10L);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0L);
    assert (oi.checkValid().size() == 0);

    oi.setMin(10L);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMin();
    assert (oi.checkValid().size() == 1);

    oi.setMin(11L);
    assert (oi.checkValid().size() == 1);

    NumberTypeValidator<Long> oMax = new NumberTypeValidator<Long>();
    oMax.setName("long");
    oMax.setValue(Long.MAX_VALUE);
    oMax.setMin(Long.MAX_VALUE);
    assert (oMax.checkValid().size() == 0);

    oMax.setExcludeMin();
    assert (oMax.checkValid().size() == 1);

    NumberTypeValidator<Long> oMin = new NumberTypeValidator<Long>();
    oMin.setName("long");
    oMin.setValue(Long.MIN_VALUE);
    oMin.setMin(Long.MIN_VALUE);
    assert (oMin.checkValid().size() == 0);

    oMin.setExcludeMin();
    assert (oMin.checkValid().size() == 1);
  }

  @Test
  void testMinFloat() {
    NumberTypeValidator<Float> oi = new NumberTypeValidator<Float>();
    oi.setName("float");
    oi.setValue(0.1f);
    oi.setMin(-1.1f);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0f);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0.1f);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMin();
    assert (oi.checkValid().size() == 1);

    oi.setMin(0.1f + java.lang.Math.ulp(0.1f));
    assert (oi.checkValid().size() == 1);

    NumberTypeValidator<Float> oMax = new NumberTypeValidator<Float>();
    oMax.setName("float");
    oMax.setValue(Float.MAX_VALUE);
    oMax.setMin(Float.MAX_VALUE);
    assert (oMax.checkValid().size() == 0);

    oMax.setExcludeMin();
    assert (oMax.checkValid().size() == 1);

    NumberTypeValidator<Float> oMin = new NumberTypeValidator<Float>();
    oMin.setName("float");
    oMin.setValue(Float.MIN_VALUE);
    oMin.setMin(Float.MIN_VALUE);
    assert (oMin.checkValid().size() == 0);

    oMin.setExcludeMin();
    assert (oMin.checkValid().size() == 1);
  }

  @Test
  void testMinDouble() {
    NumberTypeValidator<Double> oi = new NumberTypeValidator<Double>();
    oi.setName("double");
    oi.setValue(0.1d);
    oi.setMin(-1.1d);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0d);
    assert (oi.checkValid().size() == 0);

    oi.setMin(0.1d);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMin();
    assert (oi.checkValid().size() == 1);

    oi.setMin(0.1d + java.lang.Math.ulp(0.1d));
    assert (oi.checkValid().size() == 1);

    NumberTypeValidator<Double> oMax = new NumberTypeValidator<Double>();
    oMax.setName("double");
    oMax.setValue(Double.MAX_VALUE);
    oMax.setMin(Double.MAX_VALUE);
    assert (oMax.checkValid().size() == 0);

    oMax.setExcludeMin();
    assert (oMax.checkValid().size() == 1);

    NumberTypeValidator<Double> oMin = new NumberTypeValidator<Double>();
    oMin.setName("double");
    oMin.setValue(Double.MIN_VALUE);
    oMin.setMin(Double.MIN_VALUE);
    assert (oMin.checkValid().size() == 0);

    oMin.setExcludeMin();
    assert (oMin.checkValid().size() == 1);
  }

  @Test
  void testMaxInteger() {
    List<String> errorMessages;
    NumberTypeValidator<Integer> oi = new NumberTypeValidator<Integer>();
    oi.setName("int");
    oi.setValue(10);
    oi.setMax(Integer.MAX_VALUE);
    assert (oi.checkValid().size() == 0);

    oi.setMax(10);
    assert (oi.checkValid().size() == 0);

    oi.setMax(9);
    errorMessages = oi.checkValid();
    System.out.println(errorMessages);
    assert (errorMessages.size() == 1);

    oi.setMax(10);
    oi.setExcludeMax();
    errorMessages = oi.checkValid();
    System.out.println(errorMessages);
    assert (errorMessages.size() == 1);

    oi.setMax(11);
    assert (oi.checkValid().size() == 0);
  }

  @Test
  void testMaxLong() {
    NumberTypeValidator<Long> oi = new NumberTypeValidator<Long>();
    oi.setName("long");
    oi.setValue(10L);
    oi.setMax(Long.MAX_VALUE);
    assert (oi.checkValid().size() == 0);

    oi.setMax(10L);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMax();
    assert (oi.checkValid().size() == 1);

    oi.setMax(11L);
    assert (oi.checkValid().size() == 0);
  }

  @Test
  void testMaxFloat() {
    NumberTypeValidator<Float> oi = new NumberTypeValidator<Float>();
    oi.setName("float");
    oi.setValue(0.1f);
    oi.setMax(Float.MAX_VALUE);
    assert (oi.checkValid().size() == 0);

    oi.setMax(0.1f);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMax();
    assert (oi.checkValid().size() == 1);

    oi.setMax(0.1f + java.lang.Math.ulp(0.1f));
    assert (oi.checkValid().size() == 0);
  }

  @Test
  void testMaxDouble() {
    NumberTypeValidator<Double> oi = new NumberTypeValidator<Double>();
    oi.setName("double");
    oi.setValue(0.1d);
    oi.setMax(Double.MAX_VALUE);
    assert (oi.checkValid().size() == 0);

    oi.setMax(0.1d);
    assert (oi.checkValid().size() == 0);

    oi.setExcludeMax();
    assert (oi.checkValid().size() == 1);

    oi.setMax(0.1d + java.lang.Math.ulp(0.1d));
    assert (oi.checkValid().size() == 0);
  }

  @Test
  void testSetMultipleOfForInteger() {
    
    NumberTypeValidator<Integer> oi = new NumberTypeValidator<Integer>();
    oi.setName("int");
    oi.setValue(10);

    oi.setMultipleOf(0);
    assert (oi.checkValid().size() == 1);

    oi.setMultipleOf(3);
    assert (oi.checkValid().size() == 1);

    oi.setMultipleOf(1);
    assert (oi.checkValid().size() == 0);

    oi.setMultipleOf(2);
    assert (oi.checkValid().size() == 0);

    oi.setMultipleOf(5);
    assert (oi.checkValid().size() == 0);

    oi.setMultipleOf(10);
    assert (oi.checkValid().size() == 0);

    NumberTypeValidator<Integer> oni = new NumberTypeValidator<Integer>();
    oni.setName("neg int");
    oni.setValue(-10);

    oni.setMultipleOf(0);
    assert (oni.checkValid().size() == 1);

    oni.setMultipleOf(2);
    assert (oni.checkValid().size() == 0);

    oni.setMultipleOf(1);
    assert (oni.checkValid().size() == 0);

    oni.setMultipleOf(5);
    assert (oni.checkValid().size() == 0);

  }

  @Test
  void testSetMultipleOfForLong() {
    
    NumberTypeValidator<Long> ol = new NumberTypeValidator<Long>();
    ol.setName("long");
    ol.setValue(Integer.MAX_VALUE * 10L);

    ol.setMultipleOf(0L);
    assert (ol.checkValid().size() == 1);

    ol.setMultipleOf(3L);
    assert (ol.checkValid().size() == 1);

    ol.setMultipleOf(1L);
    assert (ol.checkValid().size() == 0);

    ol.setMultipleOf(2L);
    assert (ol.checkValid().size() == 0);

    ol.setMultipleOf(5L);
    assert (ol.checkValid().size() == 0);

    ol.setMultipleOf(10L);
    assert (ol.checkValid().size() == 0);

    NumberTypeValidator<Long> onl = new NumberTypeValidator<Long>();
    onl.setName("neg long");
    onl.setValue(Integer.MAX_VALUE * 1-0L);

    onl.setMultipleOf(0L);
    assert (onl.checkValid().size() == 1);

    onl.setMultipleOf(2L);
    assert (onl.checkValid().size() == 0);

    onl.setMultipleOf(1L);
    assert (onl.checkValid().size() == 0);

    onl.setMultipleOf(5L);
    assert (onl.checkValid().size() == 0);
  }
}
