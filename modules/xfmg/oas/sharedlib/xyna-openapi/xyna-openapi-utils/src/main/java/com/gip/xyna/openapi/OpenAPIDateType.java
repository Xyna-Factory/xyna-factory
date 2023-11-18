package com.gip.xyna.openapi;

public class OpenAPIDateType extends OpenAPIStringType {

    final static private String yearPattern = "[0-9]{4}";
    final static private String febuaryPattern = "02-(0[1-9]|[12][0-9])";
    final static private String month30Pattern = "(0[469]|11)-(0[1-9]|[12][0-9]|30)";
    final static private String month31Pattern = "(0[13578]|10|12)-(0[1-9]|[12][0-9]|3[01])";

    protected String getDatePattern() {
        return yearPattern + "-" + "(" + febuaryPattern + "|" + month30Pattern + "|"
                + month31Pattern + ")";
    }

    public OpenAPIDateType(String name, String value) {
        super(name, value);
        setFormat("date");
        setPattern("^" + getDatePattern() + "$");
    }

}
