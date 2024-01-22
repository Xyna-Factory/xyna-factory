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

public class OpenAPIDateTimeType extends OpenAPIDateType {

    final static private String hourPattern = "([01][0-9]|2[0-3])";
    final static private String minutePattern = "[0-5][0-9]";
    final static private String secondsPattern = "([0-5][0-9]|60)";
    final static private String fracSecondsPattern = "(\\.[0-9]+)?";
    final static private String tzPattern = "(Z|[-+]" + hourPattern + ":" + minutePattern + ")";

    private String getTimePattern() {
        return hourPattern + ":" + minutePattern + ":" + secondsPattern + fracSecondsPattern
                + tzPattern;
    }

    public OpenAPIDateTimeType(String name, String value) {
        super(name, value);
        setFormat("date-time");
        setPattern("^" + getDatePattern() + "T" + getTimePattern() + "$");
    }

}
