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
package com.gip.xyna.xnwh.persistence.memory.sqlparsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OrderBy {

  private boolean isAsc;
  private String column;

  private static Pattern orderByEntryPattern = Pattern.compile("([^ ]+)((?: +(?:asc|desc))?)",
                                                               Pattern.CASE_INSENSITIVE);


  public OrderBy(String s) throws PreparedQueryParsingException {
    s = s.trim();
    Matcher m = orderByEntryPattern.matcher(s);
    if (m.matches()) {
      column = m.group(1);
      isAsc = !m.group(2).trim().equalsIgnoreCase("desc"); //default = asc
    } else {
      throw new PreparedQueryParsingException("part of orderBy clause invalid: \"" + s + "\"");
    }
  }


  public String getColumnName() {
    return column;
  }
  
  public boolean isAsc() {
    return isAsc;
  }
}
