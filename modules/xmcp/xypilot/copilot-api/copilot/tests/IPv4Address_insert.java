/*
 *
 * - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */


/**
 * IPv4 addresses are 32-bit integers, but they are usually represented as a string of four numbers separated by dots.
 */
public static class IPv4Address {
  // API Reference
  
  // class Text // Wrapper for the String class.  Note: if initialized with an empty constructor the internal value is null.
  // Text(String value);
  // void trim(); // Trims of leading and trailing whitespaces
  // void append(Text); // Appends the given text to its own value
  // Text substring(util.IntegerNumber, util.IntegerNumber); // Returns a substring of this text. If start is null nor negative, it is set to 0. If end is null or greater than the length, it is set to the length.. Start is inclusive; end is exclusive
  // util.IntegerNumber length(); // Returns length of the text
  // void toUppercase(); // Converts text to uppercase
  // void toLowercase(); // Converts text to lowercase
  // util.IntegerNumber indexOf(Text); // Returns the starting index of the first occurence of the given text if it exists.. Return -1 if the given text is no substring
  // String getValue();
  // void setValue(String);
  
  // class InvalidAddressException extends core.exception.XynaExceptionBase // Exception to be thrown when an invalid text shall be parsed as an IPv4 Address
  // InvalidAddressException(String invalidText, Throwable cause);
  // String getInvalidText();
  // void setInvalidText(String);
  
  private int address;

  public IPv4Address() {
  }

  public IPv4Address(int address) {
    this.address = address;
  }


  /**
   * Returns a text representation of this IPv4Address
   */
  public Text asText() {
    int a = (address >> 24) & 0xff;
    int b = (address >> 16) & 0xff;
    int c = (address >> 8) & 0xff;
    int d = address & 0xff;
    return new Text(a + "." + b + "." + c + "." + d);
  }

  public IPv4Address fromText(Text text14) throws InvalidAddressException {
    String text =
  }
}
