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

import org.junit.jupiter.api.Test;

import com.gip.xyna.openapi.DateTypeValidator;

public class OpenAPIDateTypeTest {
  @Test
  void testNullIsValid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue(null);
    assert (st.checkValid().size() == 1);

    st.setNullable();
    assert (st.checkValid().size() == 0);
  }

  @Test
  void testDateIsValid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-08-01");
    assert (st.checkValid().size() == 0);
  }

  @Test
  void testDateTimeIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-08-01T12:34:56Z");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testYearIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testYearMonthIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-12");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testSingleDigitMonthIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-2-28");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testSingleDigitDayIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-12-2");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testInvalidMonthIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-13-2");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testInvalidDayIsInvalid() {
    DateTypeValidator st = new DateTypeValidator();
    st.setName("test");
    st.setValue("2022-04-32");
    assert (st.checkValid().size() == 1);
  }

  @Test
  void testFeburary() {
    DateTypeValidator st28 = new DateTypeValidator();
    st28.setName("test");
    st28.setValue("2022-02-28");
    assert (st28.checkValid().size() == 0);
    DateTypeValidator st29 = new DateTypeValidator();
    st29.setName("test");
    st29.setValue("2022-02-29");
    assert (st29.checkValid().size() == 0);
    DateTypeValidator st30 = new DateTypeValidator();
    st30.setName("test");
    st30.setValue("2022-02-30");
    assert (st30.checkValid().size() == 1);
    DateTypeValidator st31 = new DateTypeValidator();
    st31.setName("test");
    st31.setValue("2022-02-31");
    assert (st31.checkValid().size() == 1);
  }

  @Test
  void test30Days() {
    DateTypeValidator apr30 = new DateTypeValidator();
    apr30.setName("test");
    apr30.setValue("2022-04-30");
    assert (apr30.checkValid().size() == 0);

    DateTypeValidator apr31 = new DateTypeValidator();
    apr31.setName("test");
    apr31.setValue("2022-04-31");
    assert (apr31.checkValid().size() == 1);
    
    DateTypeValidator jun30 = new DateTypeValidator();
    jun30.setName("test");
    jun30.setValue("2022-06-30");
    assert (jun30.checkValid().size() == 0);
    
    DateTypeValidator jun31 = new DateTypeValidator();   
    jun31.setName("test");
    jun31.setValue("2022-06-31");
    assert (jun31.checkValid().size() == 1);

    DateTypeValidator sep30 = new DateTypeValidator();
    sep30.setName("test");
    sep30.setValue("2022-09-30");
    assert (sep30.checkValid().size() == 0);
    
    DateTypeValidator sep31 = new DateTypeValidator();
    sep31.setName("test");
    sep31.setValue("2022-09-31");
    assert (sep31.checkValid().size() == 1);

    DateTypeValidator nov30 = new DateTypeValidator();
    nov30.setName("test");
    nov30.setValue("2022-11-30");
    assert (nov30.checkValid().size() == 0);
    
    DateTypeValidator nov31 = new DateTypeValidator();
    nov31.setName("test");
    nov31.setValue("2022-11-31");
    assert (nov31.checkValid().size() == 1);
  }

  @Test
  void test31Days() {
    DateTypeValidator jan = new DateTypeValidator();
    jan.setName("test");
    jan.setValue("2022-01-31");
    assert (jan.checkValid().size() == 0);

    DateTypeValidator mar = new DateTypeValidator();
    mar.setName("test");
    mar.setValue("2022-03-31");
    assert (mar.checkValid().size() == 0);

    DateTypeValidator mai = new DateTypeValidator();
    mai.setName("test");
    mai.setValue("2022-05-31");
    assert (mai.checkValid().size() == 0);

    DateTypeValidator jul = new DateTypeValidator();
    jul.setName("test");
    jul.setValue("2022-07-31");
    assert (jul.checkValid().size() == 0);

    DateTypeValidator aug = new DateTypeValidator();
    aug.setName("test");
    aug.setValue("2022-08-31");
    assert (aug.checkValid().size() == 0);

    DateTypeValidator oct = new DateTypeValidator();
    oct.setName("test");
    oct.setValue("2022-10-31");
    assert (oct.checkValid().size() == 0);

    DateTypeValidator dec = new DateTypeValidator();
    dec.setName("test");
    dec.setValue("2022-12-31");
    assert (dec.checkValid().size() == 0);
  }
}
