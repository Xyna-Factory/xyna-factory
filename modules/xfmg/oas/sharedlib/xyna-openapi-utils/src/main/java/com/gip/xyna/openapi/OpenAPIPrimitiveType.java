package com.gip.xyna.openapi;

public abstract class OpenAPIPrimitiveType<T> extends OpenAPIBaseType {

    final private T value;

    public OpenAPIPrimitiveType(String name, T value) {
        super(name);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    boolean isNull() {
        return value == null;
    }

}
