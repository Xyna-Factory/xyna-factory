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

public class OpenAPIDateTypeTest {
    @Test
    void testNullIsValid() {
        OpenAPIDateType st = new OpenAPIDateType("test", null);

        assert (st.checkValid().size() == 1);

        st.setNullable();
        assert (st.checkValid().size() == 0);
    }

    @Test
    void testDateIsValid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-08-01");

        assert (st.checkValid().size() == 0);
    }

    @Test
    void testDateTimeIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-08-01T12:34:56Z");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testYearIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testYearMonthIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-12");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testSingleDigitMonthIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-2-28");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testSingleDigitDayIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-12-2");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testInvalidMonthIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-13-2");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testInvalidDayIsInvalid() {
        OpenAPIDateType st = new OpenAPIDateType("test", "2022-04-32");

        assert (st.checkValid().size() == 1);
    }

    @Test
    void testFeburary() {
        OpenAPIDateType st28 = new OpenAPIDateType("test", "2022-02-28");

        assert (st28.checkValid().size() == 0);

        OpenAPIDateType st29 = new OpenAPIDateType("test", "2022-02-29");

        assert (st29.checkValid().size() == 0);

        OpenAPIDateType st30 = new OpenAPIDateType("test", "2022-02-30");

        assert (st30.checkValid().size() == 1);

        OpenAPIDateType st31 = new OpenAPIDateType("test", "2022-02-31");

        assert (st31.checkValid().size() == 1);
    }

    @Test
    void test30Days() {
        OpenAPIDateType apr30 = new OpenAPIDateType("test", "2022-04-30");
        assert (apr30.checkValid().size() == 0);
        OpenAPIDateType apr31 = new OpenAPIDateType("test", "2022-04-31");
        assert (apr31.checkValid().size() == 1);

        OpenAPIDateType jun30 = new OpenAPIDateType("test", "2022-06-30");
        assert (jun30.checkValid().size() == 0);
        OpenAPIDateType jun31 = new OpenAPIDateType("test", "2022-06-31");
        assert (jun31.checkValid().size() == 1);

        OpenAPIDateType sep30 = new OpenAPIDateType("test", "2022-09-30");
        assert (sep30.checkValid().size() == 0);
        OpenAPIDateType sep31 = new OpenAPIDateType("test", "2022-09-31");
        assert (sep31.checkValid().size() == 1);

        OpenAPIDateType nov30 = new OpenAPIDateType("test", "2022-11-30");
        assert (nov30.checkValid().size() == 0);
        OpenAPIDateType nov31 = new OpenAPIDateType("test", "2022-11-31");
        assert (nov31.checkValid().size() == 1);
    }

    @Test
    void test31Days() {
        OpenAPIDateType jan = new OpenAPIDateType("test", "2022-01-31");

        assert (jan.checkValid().size() == 0);

        OpenAPIDateType mar = new OpenAPIDateType("test", "2022-03-31");

        assert (mar.checkValid().size() == 0);

        OpenAPIDateType mai = new OpenAPIDateType("test", "2022-05-31");

        assert (mai.checkValid().size() == 0);

        OpenAPIDateType jul = new OpenAPIDateType("test", "2022-07-31");

        assert (jul.checkValid().size() == 0);

        OpenAPIDateType aug = new OpenAPIDateType("test", "2022-08-31");

        assert (aug.checkValid().size() == 0);

        OpenAPIDateType oct = new OpenAPIDateType("test", "2022-10-31");

        assert (oct.checkValid().size() == 0);

        OpenAPIDateType dec = new OpenAPIDateType("test", "2022-12-31");

        assert (dec.checkValid().size() == 0);
    }
}
