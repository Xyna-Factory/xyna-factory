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

package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Graph.HasUniqueStringIdentifier;

public final class XsdWithDependencies implements HasUniqueStringIdentifier {

  private final static Pattern EXTRACTION_PATTERN = Pattern.compile("<[\\w]+:i(nclude|mport)\\s+[^>]*schemaLocation=\"([^\"]+)\"");
  
  private final String filename;
  byte[] content;
  
  
  public XsdWithDependencies(String filename) {
    this.filename = filename;
  }
  
  public byte[] getXSD() {
    return content;
  }

  public XsdWithDependencies(String filename, byte[] content) {
    this(filename);
    this.content = content;
  }
  
  public String getId() {
    return filename;
  }
  
  public void setContent(byte[] content) {
    this.content = content;
  }
  
  public Collection<String> getImportsAndIncludes() {
    List<String> dependencies = new ArrayList<String>();
    Matcher matcher = EXTRACTION_PATTERN.matcher(new String(content));
    while (matcher.find()) {
      dependencies.add(ValidationUtils.pathToFilename(matcher.group(2)));
    }
    return dependencies;
  }
}