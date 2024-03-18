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

public class OpenAPIStringTypeTest {
    @Test
    void testNullIsValid() {
        OpenAPIStringType st = new OpenAPIStringType("test", null);
        List<String> errorMessages = st.checkValid();

        System.out.println(errorMessages);
        assert (errorMessages.size() == 1);
        
        st.setNullable();
        assert (st.checkValid().size() == 0);
    }

    @Test
    void testRequiredAndNullIsInvalid() {
        OpenAPIStringType st = new OpenAPIStringType("test", null);
        List<String> errorMessages = st.checkValid();

        System.out.println(errorMessages);
        assert (errorMessages.size() == 1);

        st.setRequired();
        errorMessages = st.checkValid();
        System.out.println(errorMessages);
        assert (errorMessages.size() == 2);

        st.setNullable();
        errorMessages = st.checkValid();
        System.out.println(errorMessages);
        assert (errorMessages.size() == 1);
    }

    @Test
    void testRequiredIsValid() {
        OpenAPIStringType st = new OpenAPIStringType("test", "test");

        assert (st.checkValid().size() == 0);

        st.setRequired();
        assert (st.checkValid().size() == 0);
    }

    @Test
    void testPatternIsValid() {
        OpenAPIStringType st = new OpenAPIStringType("test", "test");

        assert (st.checkValid().size() == 0);

        st.setPattern("^test$");
        assert (st.checkValid().size() == 0);

        st.setPattern(".es.");
        assert (st.checkValid().size() == 0);

        st.setPattern("es");
        assert (st.checkValid().size() == 0);

        st.setPattern("tester");
        assert (st.checkValid().size() == 1);
    }

    @Test
    void testMinLength() {
        OpenAPIStringType st = new OpenAPIStringType("test", "");
        st.setMinLength(0);
        assert (st.checkValid().size() == 0);

        st.setMinLength(1);
        assert (st.checkValid().size() == 1);

        OpenAPIStringType st2 = new OpenAPIStringType("test", "test");
        st2.setMinLength(4);
        assert (st2.checkValid().size() == 0);

        st2.setMinLength(5);
        assert (st2.checkValid().size() == 1);
    }

    @Test
    void testMaxLength() {
        OpenAPIStringType st = new OpenAPIStringType("test", "");
        st.setMaxLength(0);
        assert (st.checkValid().size() == 0);

        st.setMaxLength(1);
        assert (st.checkValid().size() == 0);

        OpenAPIStringType st2 = new OpenAPIStringType("test", "test");
        st2.setMaxLength(0);
        assert (st2.checkValid().size() == 1);

        st2.setMaxLength(1);
        assert (st2.checkValid().size() == 1);

        st2.setMaxLength(4);
        assert (st2.checkValid().size() == 0);

        st2.setMaxLength(5);
        assert (st2.checkValid().size() == 0);
    }
}
