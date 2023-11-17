package com.gip.xyna.openapi;

import java.util.ArrayList;
import java.util.List;

public abstract class OpenAPIBaseType {
    private final String name;
    private boolean isNullable;
    private boolean isRequired;

    public OpenAPIBaseType(final String name) {
        this.name = name;
    }

    public final String getName() {
        return this.name;
    }

    public final String getType() {
        return this.getClass().getName();
    }

    public OpenAPIBaseType setNullable() {
        this.isNullable = true;
        return this;
    }

    public OpenAPIBaseType setRequired() {
        this.isRequired = true;
        return this;
    }

    public List<String> checkValid() {
        List<String> errorMessages = new ArrayList<String>();
        
        if (this.isRequired && this.isNull()) {
            errorMessages.add(this.name + ": Property is required, but has value null");
        }
        
        if (!this.isNullable && this.isNull()) {
            errorMessages.add(this.name + ": Property must not be null");
        }
        
        return errorMessages;
    }

    abstract boolean isNull();
}
