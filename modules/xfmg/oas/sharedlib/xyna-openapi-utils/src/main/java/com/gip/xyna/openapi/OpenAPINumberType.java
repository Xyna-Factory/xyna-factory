package com.gip.xyna.openapi;

import java.util.List;

public class OpenAPINumberType<N extends Number> extends OpenAPIPrimitiveType<N> {

    private boolean excludeMin;  // if set to true, the condition value > min must be valid, else value >= min
    private boolean excludeMax;  // analog to excludeMin
    private N multipleOf;
    private N min;
    private N max;

    public OpenAPINumberType<N> setMin(N min) {
        this.min = min;
        return this;
    }

    public OpenAPINumberType<N> setMax(N max) {
        this.max = max;
        return this;
    }

    public OpenAPINumberType(final String name, final N value) {
        super(name, value);
    }

    public OpenAPINumberType<N> setMultipleOf(final N m) {
        this.multipleOf = m;
        return this;
    }

    public OpenAPINumberType<N> setExcludeMin() {
        this.excludeMin = true;
        return this;
    }

    public OpenAPINumberType<N> setExcludeMax() {
        this.excludeMax = true;
        return this;
    }

    @Override
    public List<String> checkValid() {
        List<String> errorMessages = super.checkValid();

        if (!this.checkMultipleOf()) {
            errorMessages.add(String.format(
                "%s: Value %s is not multiple of %s", this.getName(), this.getValue().toString(), this.multipleOf.toString())
            );
        }

        if (!this.checkMin()) {
            String condition = this.excludeMin ? ">" : ">=";
            errorMessages.add(String.format(
                "%s: Value is %s, but must be %s %s", this.getName(), this.getValue().toString(), condition, this.min.toString())
            );
        }
        
        if (!this.checkMax()) {
            String condition = this.excludeMax ? "<" : "<=";
            errorMessages.add(String.format(
                "%s: Value is %s, but must be %s %s", this.getName(), this.getValue().toString(), condition, this.max.toString())
            );
        }

        return errorMessages;
    }

    private boolean checkMultipleOf() {
        boolean valid = true;
        if (multipleOf != null) {
            if (multipleOf.intValue() == 0) {
                return getValue().doubleValue() == 0;
            }
            valid = !isNull();
            if (multipleOf instanceof Long) {
                valid = valid && (getValue().longValue() % multipleOf.longValue() == 0L);
            } else if (multipleOf instanceof Integer) {
                valid = valid && (getValue().intValue() % multipleOf.intValue() == (int) 0);
            } else if (multipleOf instanceof Double) {
                valid = valid
                        && (getValue().doubleValue() % multipleOf.doubleValue() == (double) 0.0);
            } else if (multipleOf instanceof Float) {
                valid = valid && (getValue().floatValue() % multipleOf.floatValue() == (float) 0.0);
            }
        }
        return valid;
    }

    private boolean checkMin() {
        boolean valid = true;
        if (min != null) {
            valid = !isNull();
            if (min instanceof Long) {
                valid = valid && (((min.longValue() <= getValue().longValue()) && !excludeMin)
                        || (min.longValue() < getValue().longValue()));
            } else if (min instanceof Integer) {
                valid = valid && (((min.intValue() <= getValue().intValue()) && !excludeMin)
                        || (min.intValue() < getValue().intValue()));
            } else if (min instanceof Double) {
                valid = valid && (((min.doubleValue() <= getValue().doubleValue()) && !excludeMin)
                        || (min.doubleValue() < getValue().doubleValue()));
            } else if (min instanceof Float) {
                valid = valid && (((min.floatValue() <= getValue().floatValue()) && !excludeMin)
                        || (min.floatValue() < getValue().floatValue()));
            }

        }
        return valid;
    }

    private boolean checkMax() {
        boolean valid = true;
        if (max != null) {
            valid = !isNull();
            if (max instanceof Long) {
                valid = valid && (((max.longValue() >= getValue().longValue()) && !excludeMax)
                        || (max.longValue() > getValue().longValue()));
            } else if (max instanceof Integer) {
                valid = valid && (((max.intValue() >= getValue().intValue()) && !excludeMax)
                        || (max.intValue() > getValue().intValue()));
            } else if (max instanceof Double) {
                valid = valid && (((max.doubleValue() >= getValue().doubleValue()) && !excludeMax)
                        || (max.doubleValue() > getValue().doubleValue()));
            } else if (max instanceof Float) {
                valid = valid && (((max.floatValue() >= getValue().floatValue()) && !excludeMax)
                        || (max.floatValue() > getValue().floatValue()));
            }

        }
        return valid;
    }

}
