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
