package com.gip.xyna.openapi;

import java.util.List;

public class EnumTypeValidator extends PrimitiveTypeValidator<String> {

  private List<String> allowableValues;

  public void setAllowableValues(String... values) {
    allowableValues = List.of(values);
  }

  @Override
  public List<String> checkValid() {
    List<String> errorMessages = super.checkValid();

    if (isNull()) {
      return errorMessages;
    }

    if (!checkPossibleValues()) {
      errorMessages.add(String.format("%s: Enum value \"%s\" is not one of %s",
                                      getName(), getValue(), allowableValues.toString())
          );
    }

    return errorMessages;
  }

  private boolean checkPossibleValues() {
    if (allowableValues != null) {
      return -1 != allowableValues.indexOf(getValue());
    }
    return true;
  }
}
