/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.yang.xml;


public class CharEscapeTool {

  public String escapeCharacters(String str) {
    if (str == null) { return ""; }
    String ret = str;
    ret = ret.replace("&", "&amp;");
    ret = ret.replace(",", "&comma;");
    ret = ret.replace("#", "&num;");
    ret = ret.replace("%", "&percnt;");
    ret = ret.replace("=", "&equals;");
    ret = ret.replace("~", "&tilde;");
    return ret;
  }
  
  
  public String unescapeCharacters(String str) {
    if (str == null) { return ""; }
    String ret = str;
    ret = ret.replace("&comma;", ",");
    ret = ret.replace("&num;", "#");
    ret = ret.replace("&percnt;", "%");
    ret = ret.replace("&equals;", "=");
    ret = ret.replace("&tilde;", "~");
    ret = ret.replace("&amp;", "&");
    return ret;
  }
  
}
