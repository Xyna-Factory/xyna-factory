/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.XMOM.base.net.internal;

import java.io.Serializable;
import java.util.regex.Pattern;

import com.gip.xyna.XMOM.base.net.exception.MACAddressValidationException;


public class MACAddressData implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final Pattern COLON_SEP_PATTERN = Pattern.compile("([a-fA-F0-9]{2}:){5}[a-fA-F0-9]{2}");
  public static final Pattern HYPHEN_SEP_PATTERN = Pattern.compile("([a-fA-F0-9]{2}-){5}[a-fA-F0-9]{2}");
  
  public static final Pattern DOT_SEP_PATTERN = Pattern.compile("([a-fA-F0-9]{4}\\.){2}[a-fA-F0-9]{4}");
  public static final Pattern NOT_SEP_PATTERN = Pattern.compile("^[a-fA-F0-9]{12}$");
 
  private final char[] _hexDigits = new char[12];
  
  
  public MACAddressData(String val) throws MACAddressValidationException {    
    if (!COLON_SEP_PATTERN.matcher(val).matches()) {
      if (!HYPHEN_SEP_PATTERN.matcher(val).matches()) {
        if (!DOT_SEP_PATTERN.matcher(val).matches()) {
          if (!NOT_SEP_PATTERN.matcher(val).matches()) {
            throw new MACAddressValidationException(val);
          }
        }
      }
    }
    String adjusted = val.replaceAll("[^a-fA-F0-9]", "");
    if (adjusted.length() != 12) {
      throw new MACAddressValidationException(val);
    }
    for (int i = 0; i < 12; i++) {
      _hexDigits[i] = adjusted.charAt(i);
    }
  }
  
  
  public String getColonSeparated() {
    return writeHexDigits(":", 2);
  }
  
  public String getHyphenSeparated() {
    return writeHexDigits("-", 2);
  }
  
  public String getDotSeparated() {
    return writeHexDigits(".", 4);
  }
  
  
  private String writeHexDigits(String separator, int blockLength) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < 12; i++) {
      if ((i > 0) && (i % blockLength == 0)) {
        s.append(separator);
      }
      s.append(_hexDigits[i]);
    }
    return s.toString().toLowerCase();
  }  
  
}
