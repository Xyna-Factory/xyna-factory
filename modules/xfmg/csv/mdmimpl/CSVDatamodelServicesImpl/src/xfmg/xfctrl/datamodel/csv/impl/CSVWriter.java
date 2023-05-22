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
package xfmg.xfctrl.datamodel.csv.impl;

public class CSVWriter {

  public static final String DEFAULT_NULL_REPRESENTATION = "";
  public static final String DEFAULT_LINE_SEPARATOR = "\n";
  public static final String DEFAULT_SEPARATOR = ",";
  public static final String DEFAULT_MASKER = "\"";
  public static final String DEFAULT_EOF = "";
  public static final String DEFAULT_HEADER = CSVXynaObjectWriter.Header.NONE.name().toLowerCase();
  
  private StringBuilder csv = new StringBuilder();
  private String nullRepresentation = DEFAULT_NULL_REPRESENTATION;
  private String lineSeparator = DEFAULT_LINE_SEPARATOR;
  private String separator = DEFAULT_SEPARATOR;
  private String masker = DEFAULT_MASKER;
  private String endOfFile = DEFAULT_EOF;
  private boolean maskAlways = false;
  private boolean startLine = true;
  private boolean includeMetaColumn = false;
  
  @Override
  public String toString() {
    return csv.toString();
  }
  
  void appendObject(Object o) {
    if( o == null ) {
      appendStringNonNull(nullRepresentation);
    } else {
      appendStringNonNull(String.valueOf(o));
    }
  }
  
  
  public void appendString(String s) {
    if( s == null ) {
      appendStringNonNull(nullRepresentation);
    } else {
      appendStringNonNull(s);
    }
  }
  
  private void appendStringNonNull(String s) {
    if( ! startLine  ) {
      csv.append(separator);
    } else {
      startLine = false;
    }
    boolean mask = maskAlways;
    if( s.contains(masker) ) {
      mask = true;
      s = s.replace(masker, masker+masker);
    }
    mask = mask || s.contains(separator) || s.contains(lineSeparator);
    
    if( mask ) {
      csv.append(masker).append(s).append(masker);
    } else {
      csv.append(s);
    }
  }
  
  public void appendLineSeparator() {
    startLine = true;
    csv.append(lineSeparator);
  }
  
  public void appendEndOfFile() {
    csv.append(endOfFile);
  }
  
  
  
  
  
  public String getNullRepresentation() {
    return nullRepresentation;
  }

  public void setNullRepresentation(String nullRepresentation) {
    if( nullRepresentation == null ) {
      throw new IllegalArgumentException("Null Representation cannot be null");
    }
    this.nullRepresentation = nullRepresentation;
  }
  
  public String getLineSeparator() {
    return lineSeparator;
  }

  public void setLineSeparator(String lineSeparator) {
    this.lineSeparator = lineSeparator;
  }

  public String getSeparator() {
    return separator;
  }
  
  public void setSeparator(String separator) {
    this.separator = separator;
  }
  
  public String getMasker() {
    return masker;
  }

  public void setMasker(String masker) {
    this.masker = masker;
  }

  public String getEndOfFile() {
    return endOfFile;
  }

  public void setEndOfFile(String endOfFile) {
    this.endOfFile = endOfFile;
  }

  public boolean isMaskAlways() {
    return maskAlways;
  }

  public void setMaskAlways(boolean maskAlways) {
    this.maskAlways = maskAlways;
  }
  
  public boolean doIncludeMetaColumn() {
    return includeMetaColumn;
  }
  
  public void setIncludeMetaColumn(boolean includeMetaColumn) {
    this.includeMetaColumn = includeMetaColumn;
  }

}
