/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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


package com.gip.xyna.xprc.xfractwfe.python;

import com.gip.xyna.xprc.xfractwfe.python.PythonGeneration.MemberInformation;

public class DocumentationHandler {

  public static class PythonDocuConfig {
    private boolean genDocu = true;
    public boolean isGenDoku() {
      return genDocu;
    }
    
    public PythonDocuConfig genDocu(boolean genDoku) {
      this.genDocu = genDoku;
      return this;
    }
  }
  
  
  public static enum AddEmptyLine { YES, NO }
  
  private static final String TRIPLE_QUOTE = "'''";
  private static final String TRIPLE_QUOTE_ESCAPED = "\\'\\'\\'";
  
  
  public String buildDocumentationString(MemberInformation info, int indentLength, AddEmptyLine addEmptyLine, PythonMdmConfig config) {
    if (config == null) { return ""; }
    if (!config.isGenDoku()) { return ""; }
    if (info == null) { return ""; }
    if (info.documentation == null) { return ""; }
    if (info.documentation.isBlank()) { return ""; }
    String docu = "### Member attribute '" + info.name + "': " + info.documentation;
    return buildDocumentationString(docu, indentLength, addEmptyLine, new PythonDocuConfig().genDocu(config.isGenDoku()));
  }
  
  
  public String buildDocumentationString(String documentation, int indentLength, AddEmptyLine addEmptyLine, PythonMdmConfig config) {
    return buildDocumentationString(documentation, indentLength, addEmptyLine, new PythonDocuConfig().genDocu(config.isGenDoku()));
  }
  
  
  public String buildDocumentationString(String documentation, int indentLength, AddEmptyLine addEmptyLine, PythonDocuConfig config) {
    if (config == null) { return ""; }
    if (!config.isGenDoku()) { return ""; }
    if (documentation == null) { return ""; }
    if (documentation.isBlank()) { return ""; }
    String indent = "";
    for (int i = 0; i < indentLength; i++) {
      indent += " ";
    }
    String[] lines = documentation.split("\n");
    String result = indent + TRIPLE_QUOTE + " ";
    boolean isfirst = true;
    for (String line : lines) {
      if (isfirst) { isfirst = false; }
      else { result += "\n" + indent; }
      if (line.contains(TRIPLE_QUOTE)) {
        line = escape(line);
      }
      result += line;
    }
    if (lines.length > 1) {
      result += "\n" + indent;
    }
    result += TRIPLE_QUOTE + "\n";
    if (addEmptyLine == AddEmptyLine.YES) {
      result += "\n";
    }
    return result;
  }
  
  
  private String escape(String line) {
    return line.replace(TRIPLE_QUOTE, TRIPLE_QUOTE_ESCAPED);
  }
  
}
