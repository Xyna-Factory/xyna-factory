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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAPIStringType extends OpenAPIPrimitiveType<String> {

    private Integer min;
    private Integer max;
    private String format;
    private String pattern;

    public OpenAPIStringType setFormat(String f) {
        this.format = f;
        return this;
    }

    public OpenAPIStringType setPattern(String p) {
        this.pattern = p;
        return this;
    }

    public OpenAPIStringType(String name, String value) {
        super(name, value);
    }

    public OpenAPIStringType setMinLength(Integer m) {
        this.min = m;
        return this;
    }

    public OpenAPIStringType setMaxLength(Integer m) {
        this.max = m;
        return this;
    }

    @Override
    public List<String> checkValid() {
        List<String> errorMessages = super.checkValid();

        if (this.isNull()) {
            return errorMessages;
        }

        if (!this.checkMinLength()) {
            errorMessages.add(String.format(
                "%s: String value \"%s\" is to short, minimum length is %d", this.getName(), this.getValue(), this.min)
            );
        }
        
        if (!this.checkMaxLength()) {
            errorMessages.add(String.format(
                "%s: String value \"%s\" is to long, maximum length is %d", this.getName(), this.getValue(), this.max)
            );
        }

        if (!this.checkPattern()) {
            errorMessages.add(String.format(
                "%s: Invalid format, value \"%s\" does not match format \"%s\"", this.getName(), this.getValue(), this.format)
            );
        }

        return errorMessages;
    }

    private boolean checkPattern() {
        if (pattern == null)
            return true;

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(getValue());
        return m.find();
    }

    private boolean checkMaxLength() {
        return max == null || (max != null && getValue().length() <= max);
    }

    private boolean checkMinLength() {
        return min == null || (min != null && getValue().length() >= min);
    }

}
