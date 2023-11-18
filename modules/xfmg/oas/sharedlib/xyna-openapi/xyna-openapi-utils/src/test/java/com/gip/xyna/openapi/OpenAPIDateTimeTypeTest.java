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

public class OpenAPIDateTimeTypeTest {
    // https://www.rfc-editor.org/rfc/rfc3339#section-5.8
    @Test
    void testIsValid() {
        OpenAPIDateTimeType st1 = new OpenAPIDateTimeType("test", "1985-04-12T23:20:50.52Z");
        assert (st1.checkValid().size() == 0);

        OpenAPIDateTimeType st2 = new OpenAPIDateTimeType("test", "1996-12-19T16:39:57-08:00");
        assert (st2.checkValid().size() == 0);

        OpenAPIDateTimeType st3 = new OpenAPIDateTimeType("test", "1990-12-31T23:59:60Z");
        assert (st3.checkValid().size() == 0);

        OpenAPIDateTimeType st4 = new OpenAPIDateTimeType("test", "1990-12-31T15:59:60-08:00");
        assert (st4.checkValid().size() == 0);

        OpenAPIDateTimeType st5 = new OpenAPIDateTimeType("test", "1937-01-01T12:00:27.87+00:20");
        assert (st5.checkValid().size() == 0);
    }

    @Test
    void testLowercaseIsInvalid() {
        OpenAPIDateTimeType st1 = new OpenAPIDateTimeType("test", "1985-04-12t23:20:50.52Z");
        assert (st1.checkValid().size() == 1);

        OpenAPIDateTimeType st2 = new OpenAPIDateTimeType("test", "1985-04-12T23:20:50.52z");
        assert (st2.checkValid().size() == 1);
    }
}
