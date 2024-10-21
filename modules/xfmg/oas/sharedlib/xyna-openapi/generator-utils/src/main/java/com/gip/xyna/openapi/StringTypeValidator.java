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



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;
import com.gip.xyna.XMOM.base.net.internal.IPv6Address;
import com.gip.xyna.XMOM.base.net.internal.IPv6NetmaskData;



public class StringTypeValidator extends PrimitiveTypeValidator<String> {

  private Integer min;
  private Integer max;
  private String format;
  private String pattern;

  // supported formats (case insensitive)
  private static final String UUID = "uuid";
  private static final String MAC = "mac";
  private static final String MAC_ADDRESS = "macaddress"; // same as mac
  private static final String IPV4 = "ipv4";
  private static final String IPV4_PREFIX = "ipv4prefix"; // address with prefix e.g. 192.168.1.0/24
  private static final String IPV4_SUBNET = "ipv4netmask"; // e.g. 255.255.255.0
  private static final String IPV6 = "ipv6";
  private static final String IPV6_PREFIX = "ipv6prefix"; // address with prefix e.g. 2001:0db8:85a3:08d3:1319:8a2e:0370:7347/64

  private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
  private static final String MAC_ADDRESS_REGEX = "^[a-fA-F0-9]{2}(([:-]?[a-fA-F0-9]{2}){5})$";
  private static final String IPV4_REGEX = "^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])\\.?\\b){4}$";
  private static final String IPV4_PREFIX_REGEX = "^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])\\.?\\b){4}/(3[0-2]|[1-2][0-9]|[0-9])$";

  private static final Map<String, Function<String, Boolean>> FormatValidatorMap = buildFormatValidatorMap();


  private static Map<String, Function<String, Boolean>> buildFormatValidatorMap() {
    Map<String, Function<String, Boolean>> result = new HashMap<String, Function<String, Boolean>>();
    result.put(UUID, createFormatValidatorFunction(UUID_REGEX));
    result.put(MAC, createFormatValidatorFunction(MAC_ADDRESS_REGEX));
    result.put(MAC_ADDRESS, createFormatValidatorFunction(MAC_ADDRESS_REGEX));
    result.put(IPV4, createFormatValidatorFunction(IPV4_REGEX));
    result.put(IPV4_PREFIX, createFormatValidatorFunction(IPV4_PREFIX_REGEX));
    result.put(IPV4_SUBNET, toCheck -> validateFormat(toCheck, IPV4_SUBNET));
    result.put(IPV6, toCheck -> validateFormat(toCheck, IPV6));
    result.put(IPV6_PREFIX, toCheck -> validateFormat(toCheck, IPV6_PREFIX));

    return result;
  }


  private static Function<String, Boolean> createFormatValidatorFunction(String pattern) {
    return toCheck -> checkPattern(pattern, toCheck);
  }


  public void setFormat(String f) {
    format = f.toLowerCase();
  }


  public void setPattern(String p) {
    // delimiters are cleaned in stringConstraints
    pattern = p;
  }


  public void setMinLength(Integer m) {
    min = m;
  }


  public void setMaxLength(Integer m) {
    max = m;
  }


  @Override
  public List<String> checkValid() {
    List<String> errorMessages = super.checkValid();

    if (isNull()) {
      return errorMessages;
    }

    if (!checkMinLength()) {
      errorMessages.add(String.format("%s: String value \"%s\" is too short as minimum length is %d", getName(), getValue(), min));
    }

    if (!checkMaxLength()) {
      errorMessages.add(String.format("%s: String value \"%s\" is too long as maximum length is %d", getName(), getValue(), max));
    }

    if (!checkPattern()) {
      errorMessages.add(String.format("%s: Value \"%s\" does not match pattern \"%s\"", getName(), getValue(), pattern));
    }

    if (!checkFormat()) {
      errorMessages.add(String.format("%s: Value \"%s\" does not match format \"%s\"", getName(), getValue(), format));
    }

    return errorMessages;
  }


  private boolean checkMaxLength() {
    return max == null || (max != null && getValue().length() <= max);
  }


  private boolean checkMinLength() {
    return min == null || (min != null && getValue().length() >= min);
  }


  private boolean checkPattern() {
    return checkPattern(pattern, getValue());
  }


  private static boolean checkPattern(String pattern, String toCheck) {
    if (pattern == null)
      return true;

    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(toCheck);
    return m.find();
  }


  private boolean checkFormat() {
    if (format == null) {
      return true;
    }

    Function<String, Boolean> validatorFunction = FormatValidatorMap.getOrDefault(format, null);
    if (validatorFunction == null) {
      return true; //unknown format
    }

    return validatorFunction.apply(getValue());
  }


  @SuppressWarnings("unused")
  private static boolean validateFormat(String toCheck, String format) {
    try {
      switch (format) {
        case "ipv4subnet" :
          IPv4NetmaskData ipv4mask = new IPv4NetmaskData(toCheck);
          return true;
        case "ipv6prefix" :
          String[] ipv6Prefix = toCheck.split("/");
          if (ipv6Prefix.length != 2)
            return false;
          IPv6NetmaskData ipv6mask = new IPv6NetmaskData(ipv6Prefix[1]);
          toCheck = ipv6Prefix[0];
        case "ipv6" :
          IPv6Address ipv6Address = new IPv6Address(toCheck);
          return true;
        default :
          return false;
      }

    } catch (Exception e) {
      return false;
    }
  }


}
