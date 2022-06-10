/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xfint.xnumdav;



public final class NumberHelper {

  private NumberHelper() {
  }

  public static Number addNumbers(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot add numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return (Long) a + (Long) b;
    } else if (a instanceof Integer && b instanceof Integer) {
      return (Integer) a + (Integer) b;
    } else if (a instanceof Double && b instanceof Double) {
      return (Double) a + (Double) b;
    } else {
      throw new IllegalArgumentException("Failed to add numbers: <" + a.getClass().getSimpleName() + "> + <"
                      + b.getClass().getSimpleName() + ">");
    }
  }


  public static Number subtractNumbers(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot add numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return (Long) a - (Long) b;
    } else if (a instanceof Integer && b instanceof Integer) {
      return (Integer) a - (Integer) b;
    } else if (a instanceof Double && b instanceof Double) {
      return (Double) a - (Double) b;
    } else {
      throw new IllegalArgumentException("Failed to subtract numbers: <" + a.getClass().getSimpleName() + "> - <"
                      + b.getClass().getSimpleName() + ">");
    }
  }


  public static Number getMinimum(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot get minimum for numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return Math.min((Long) a, (Long) b);
    } else if (a instanceof Integer && b instanceof Integer) {
      return Math.min((Integer) a, (Integer) b);
    } else if (a instanceof Double && b instanceof Double) {
      return Math.min((Double) a, (Double) b);
    } else {
      throw new IllegalArgumentException("Failed to calculate minimum of numbers: <" + a.getClass().getSimpleName()
                      + "> + <" + b.getClass().getSimpleName() + ">");
    }
  }


  public static Number getMaximum(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot get maximum for numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return Math.max((Long) a, (Long) b);
    } else if (a instanceof Integer && b instanceof Integer) {
      return Math.max((Integer) a, (Integer) b);
    } else if (a instanceof Double && b instanceof Double) {
      return Math.max((Double) a, (Double) b);
    } else {
      throw new IllegalArgumentException("Failed to calculate minimum of numbers: <" + a.getClass().getSimpleName()
                      + "> + <" + b.getClass().getSimpleName() + ">");
    }
  }


  public static Number divideNumberByInt(Number a, int divisor) {
    if (a == null) {
      throw new RuntimeException("Cannot divide null by int");
    }
    if (a instanceof Long) {
      return (Long) a / divisor;
    } else if (a instanceof Integer) {
      return (Integer) a / divisor;
    } else if (a instanceof Double) {
      return (Double) a / divisor;
    } else {
      throw new IllegalArgumentException("Failed to divide numbers: <" + a.getClass().getSimpleName() + "> / <int>");
    }
  }


  public static int divideNumberByNumber(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot divide numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return (int) ((Long) a / (Long) b);
    } else if (a instanceof Integer && b instanceof Integer) {
      return (int) ((Integer) a / (Integer) b);
    } else if (a instanceof Double && b instanceof Double) {
      return (int) Math.floor((Double) a / (Double) b);
    } else {
      throw new IllegalArgumentException("Failed to compare numbers: <" + a.getClass().getSimpleName() + ">, <"
                      + b.getClass().getSimpleName() + ">");
    }
  }


  public static double divideNumberByNumberUnRounded(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot divide numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return (double) (1.0d * (Long) a / (Long) b);
    } else if (a instanceof Integer && b instanceof Integer) {
      return (double) (1.0d * (Integer) a / (Integer) b);
    } else if (a instanceof Double && b instanceof Double) {
      return (double) (1.0d * (Double) a / (Double) b);
    } else {
      throw new IllegalArgumentException("Failed to compare numbers: <" + a.getClass().getSimpleName() + ">, <"
          + b.getClass().getSimpleName() + ">");
    }
  }


  public static Number multiply(Number a, int factor) {
    if (a == null) {
      throw new RuntimeException("Cannot multiply with <null>.");
    }
    if (a instanceof Long) {
      return (Long) a * factor;
    } else if (a instanceof Integer) {
      return (Integer) a * factor;
    } else if (a instanceof Double) {
      return (Double) a * factor;
    } else {
      throw new IllegalArgumentException("Failed to multiply numbers: <" + a.getClass().getSimpleName() + "> * <int>");
    }
  }


  public static boolean isFirstArgumentLargerOrEqualToSecond(Number a, Number b) {
    if (a == null || b == null) {
      throw new RuntimeException("Cannot compare numbers <null>");
    }
    if (a instanceof Long && b instanceof Long) {
      return (Long) a >= (Long) b;
    } else if (a instanceof Integer && b instanceof Integer) {
      return (Integer) a >= (Integer) b;
    } else if (a instanceof Double && b instanceof Double) {
      return (Double) a >= (Double) b;
    } else {
      throw new IllegalArgumentException("Failed to compare numbers: <" + a.getClass().getSimpleName() + ">, <"
                      + b.getClass().getSimpleName() + ">");
    }
  }

}
