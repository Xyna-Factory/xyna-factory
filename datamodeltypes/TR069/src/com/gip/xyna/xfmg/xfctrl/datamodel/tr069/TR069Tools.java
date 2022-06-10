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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class TR069Tools {

  private final static Pattern NAME_PATTERN = Pattern.compile("(tr-\\d+(?:-\\d+)*?)((?:-0)*)?(-[a-zA-Z]\\w*)?\\.xml");

  public static String createReference(String fileName) {
    Matcher m = NAME_PATTERN.matcher(fileName);
    if( m.matches() ) {
      String newName = null;
      if( m.group(3) == null ) {
        newName = m.group(1);
      } else {
        newName = m.group(1)+m.group(3);
      }
      //System.err.println("Match "+name +" -> "+newName );
      return newName;
    } else {
      //System.err.println("No match "+name);
    }
    return fileName;
  }

  
}
