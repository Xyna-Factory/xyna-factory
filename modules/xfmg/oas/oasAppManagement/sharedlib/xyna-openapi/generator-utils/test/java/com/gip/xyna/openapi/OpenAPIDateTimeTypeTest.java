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

import com.gip.xyna.openapi.DateTimeTypeValidator;

public class OpenAPIDateTimeTypeTest {
  // https://www.rfc-editor.org/rfc/rfc3339#section-5.8
  @Test
  void testIsValid() {
    DateTimeTypeValidator st1 = new DateTimeTypeValidator();
    st1.setName("test");
    st1.setValue("1985-04-12T23:20:50.52Z");
    assert (st1.checkValid().size() == 0);

    DateTimeTypeValidator st2 = new DateTimeTypeValidator();
    st2.setName("test");
    st2.setValue("1996-12-19T16:39:57-08:00");
    assert (st2.checkValid().size() == 0);

    DateTimeTypeValidator st3 = new DateTimeTypeValidator();
    st3.setName("test");
    st3.setValue("1990-12-31T23:59:60Z");
    assert (st3.checkValid().size() == 0);

    DateTimeTypeValidator st4 = new DateTimeTypeValidator();
    st4.setName("test");
    st4.setValue("1990-12-31T15:59:60-08:00");
    assert (st4.checkValid().size() == 0);

    DateTimeTypeValidator st5 = new DateTimeTypeValidator();
    st5.setName("test");
    st5.setValue("1937-01-01T12:00:27.87+00:20");
    assert (st5.checkValid().size() == 0);
  }

  @Test
  void testLowercaseIsInvalid() {
    DateTimeTypeValidator st1 = new DateTimeTypeValidator();
    st1.setName("test");
    st1.setValue("1985-04-12t23:20:50.52Z");
    assert (st1.checkValid().size() == 1);

    DateTimeTypeValidator st2 = new DateTimeTypeValidator();
    st2.setName("test");
    st2.setValue("1985-04-12T23:20:50.52z");
    assert (st2.checkValid().size() == 1);
  }
}
