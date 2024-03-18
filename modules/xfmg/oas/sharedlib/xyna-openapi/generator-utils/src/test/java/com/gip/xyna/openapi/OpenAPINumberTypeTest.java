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
        OpenAPINumberType<Integer> oi = new OpenAPINumberType<Integer>("int", 10);
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

        OpenAPINumberType<Integer> oMax = new OpenAPINumberType<Integer>("int", Integer.MAX_VALUE);
        oMax.setMin(Integer.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMin();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Integer> oMin = new OpenAPINumberType<Integer>("int", Integer.MIN_VALUE);
        oMin.setMin(Integer.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMin();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMinLong() {
        OpenAPINumberType<Long> oi = new OpenAPINumberType<Long>("long", 10L);
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

        OpenAPINumberType<Long> oMax = new OpenAPINumberType<Long>("long", Long.MAX_VALUE);
        oMax.setMin(Long.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMin();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Long> oMin = new OpenAPINumberType<Long>("long", Long.MIN_VALUE);
        oMin.setMin(Long.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMin();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMinFloat() {
        OpenAPINumberType<Float> oi = new OpenAPINumberType<Float>("float", 0.1f);
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

        OpenAPINumberType<Float> oMax = new OpenAPINumberType<Float>("float", Float.MAX_VALUE);
        oMax.setMin(Float.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMin();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Float> oMin = new OpenAPINumberType<Float>("float", Float.MIN_VALUE);
        oMin.setMin(Float.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMin();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMinDouble() {
        OpenAPINumberType<Double> oi = new OpenAPINumberType<Double>("double", 0.1d);
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

        OpenAPINumberType<Double> oMax = new OpenAPINumberType<Double>("double", Double.MAX_VALUE);
        oMax.setMin(Double.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMin();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Double> oMin = new OpenAPINumberType<Double>("double", Double.MIN_VALUE);
        oMin.setMin(Double.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMin();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMaxInteger() {
        List<String> errorMessages;
        OpenAPINumberType<Integer> oi = new OpenAPINumberType<Integer>("int", 10);
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

        OpenAPINumberType<Integer> oMax = new OpenAPINumberType<Integer>("int", Integer.MAX_VALUE);
        oMax.setMax(Integer.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMax();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Integer> oMin = new OpenAPINumberType<Integer>("int", Integer.MIN_VALUE);
        oMin.setMax(Integer.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMax();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMaxLong() {
        OpenAPINumberType<Long> oi = new OpenAPINumberType<Long>("long", 10L);
        oi.setMax(Long.MAX_VALUE);
        assert (oi.checkValid().size() == 0);

        oi.setMax(10L);
        assert (oi.checkValid().size() == 0);

        oi.setExcludeMax();
        assert (oi.checkValid().size() == 1);

        oi.setMax(11L);
        assert (oi.checkValid().size() == 0);

        OpenAPINumberType<Long> oMax = new OpenAPINumberType<Long>("long", Long.MAX_VALUE);
        oMax.setMax(Long.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMax();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Long> oMin = new OpenAPINumberType<Long>("long", Long.MIN_VALUE);
        oMin.setMax(Long.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMax();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMaxFloat() {
        OpenAPINumberType<Float> oi = new OpenAPINumberType<Float>("float", 0.1f);
        oi.setMax(Float.MAX_VALUE);
        assert (oi.checkValid().size() == 0);

        oi.setMax(0.1f);
        assert (oi.checkValid().size() == 0);

        oi.setExcludeMax();
        assert (oi.checkValid().size() == 1);

        oi.setMax(0.1f + java.lang.Math.ulp(0.1f));
        assert (oi.checkValid().size() == 0);

        OpenAPINumberType<Float> oMax = new OpenAPINumberType<Float>("float", Float.MAX_VALUE);
        oMax.setMax(Float.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMax();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Float> oMin = new OpenAPINumberType<Float>("float", Float.MIN_VALUE);
        oMin.setMax(Float.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMax();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testMaxDouble() {
        OpenAPINumberType<Double> oi = new OpenAPINumberType<Double>("double", 0.1d);
        oi.setMax(Double.MAX_VALUE);
        assert (oi.checkValid().size() == 0);

        oi.setMax(0.1d);
        assert (oi.checkValid().size() == 0);

        oi.setExcludeMax();
        assert (oi.checkValid().size() == 1);

        oi.setMax(0.1d + java.lang.Math.ulp(0.1d));
        assert (oi.checkValid().size() == 0);

        OpenAPINumberType<Double> oMax = new OpenAPINumberType<Double>("double", Double.MAX_VALUE);
        oMax.setMax(Double.MAX_VALUE);
        assert (oMax.checkValid().size() == 0);

        oMax.setExcludeMax();
        assert (oMax.checkValid().size() == 1);

        OpenAPINumberType<Double> oMin = new OpenAPINumberType<Double>("double", Double.MIN_VALUE);
        oMin.setMax(Double.MIN_VALUE);
        assert (oMin.checkValid().size() == 0);

        oMin.setExcludeMax();
        assert (oMin.checkValid().size() == 1);
    }

    @Test
    void testSetMultipleOfForInteger() {

        assert ((new OpenAPINumberType<Integer>("int", 10)).setMultipleOf(0).checkValid().size() == 1);

        OpenAPINumberType<Integer> oi = new OpenAPINumberType<Integer>("int", 10);

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

        OpenAPINumberType<Integer> oni = new OpenAPINumberType<Integer>("neg int", -10);

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

        assert ((new OpenAPINumberType<Long>("long", 10L)).setMultipleOf(0L).checkValid().size() == 1);

        OpenAPINumberType<Long> ol = new OpenAPINumberType<Long>("long", Integer.MAX_VALUE * 10L);

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

        OpenAPINumberType<Long> onl =
                new OpenAPINumberType<Long>("neg long", -10L * Integer.MAX_VALUE);

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
