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
                "%s: Invalid pattern, \"%s\" does not match \"%s\"", this.getName(), this.getValue(), this.pattern)
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
