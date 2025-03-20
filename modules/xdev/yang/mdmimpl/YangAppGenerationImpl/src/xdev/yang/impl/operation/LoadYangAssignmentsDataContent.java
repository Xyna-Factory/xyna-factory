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

package xdev.yang.impl.operation;

import xdev.yang.impl.Constants;
import xmcp.yang.LoadYangAssignmentsData;


public class LoadYangAssignmentsDataContent {

  private final String[] parts;
  private final String[] namespaceParts;
  private final String[] keywordParts;
  
  
  public LoadYangAssignmentsDataContent(LoadYangAssignmentsData data) {
    this(data.getTotalYangPath(), data.getTotalNamespaces(), data.getTotalKeywords());
  }
  
  
  public LoadYangAssignmentsDataContent(String path, String namespaces, String keywords) {
    if (path == null) {
      throw new IllegalArgumentException("Parameter LoadYangAssignmentsData incomplete: TotalYangPath empty");
    }
    else if (namespaces == null) {
      throw new IllegalArgumentException("Parameter LoadYangAssignmentsData incomplete: TotalNamespaces empty");
    }
    else if (keywords == null) {
      throw new IllegalArgumentException("Parameter LoadYangAssignmentsData incomplete: TotalKeywords empty");
    }
    
    parts = path.split("\\/");
    namespaceParts = namespaces.split(Constants.NS_SEPARATOR);
    keywordParts = keywords.split(" ");
    
    if (parts.length != namespaceParts.length) {
      throw new IllegalArgumentException("Parameter LoadYangAssignmentsData inconsistent: " +
                                         "Number of segments in TotalYangPath does not match TotalNamespaces");
    }
    else if (parts.length != keywordParts.length) {
      throw new IllegalArgumentException("Parameter LoadYangAssignmentsData inconsistent: " +
                                         "Number of segments in TotalYangPath does not match TotalKeywords");
    }
  }

  public int getLength() {
    return parts.length;
  }
  
  public String[] getParts() {
    return parts;
  }

  public String[] getNamespaceParts() {
    return namespaceParts;
  }

  public String[] getKeywordParts() {
    return keywordParts;
  }

  public String getPart(int i) {
    return parts[i];
  }

  public String getNamespacePart(int i) {
    return namespaceParts[i];
  }

  public String getKeywordPart(int i) {
    return keywordParts[i];
  }
  
}
