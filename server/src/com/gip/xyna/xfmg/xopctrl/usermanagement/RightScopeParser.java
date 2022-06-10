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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope.ScopePartType;


public class RightScopeParser {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(RightScopeParser.class);
  
  private static final Pattern SCOPED_RIGHT_SPERATOR = Pattern.compile("(?<!\\\\)[:]");
  private static final Pattern ENUM_PART_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.]+");
  private static final Pattern ENUM_VALUE_SEPERATOR_PATTERN = Pattern.compile("\\s*,\\s*");
  
  protected boolean acceptRightScope(String scope) {
    String[] parts = splitRightScopeIntoParts(scope);
    if (parts.length > 0 && 
        acceptKey(parts[0])) {
      for (int i = 1; i < parts.length; i++) {
        if (!acceptScopePart(parts[i])) {
          return false;
        }
      }
    } else {
      return false;
    }
    return true;
  }
  
  
  protected boolean acceptKey(String key) {
    Matcher keyMatcher = getDefaultRightPatternMatcher(key);
    return keyMatcher.matches();
  }
  
  
  protected boolean acceptScopePart(String scopePart) {
    ScopePartType type = ScopePartType.determineScopePartType(scopePart);
    switch (type) {
      case PRIMITIVE :
        return acceptPrimitiveScopePart(scopePart);
      case ENUMERATION :
        return acceptEnumerationScopePart(scopePart);
      case REGEXP :
        return acceptRegExpScopePart(scopePart);
      case WILDCARD :
        return acceptWildcardScopePart(scopePart);
      default :
        throw new UnsupportedOperationException("Unknown scopePartType: " + type);
    }
  }
  
  
  protected boolean acceptPrimitiveScopePart(String primitiveScopePart) {
    return primitiveScopePart.length() == 0;
  }
  
  
  protected boolean acceptWildcardScopePart(String wildcardScopePart) {
    return wildcardScopePart.equals("*");
  }
  
  
  protected boolean acceptEnumerationScopePart(String enumScopePart) {
    if (enumScopePart.startsWith("[") &&
        enumScopePart.endsWith("]")) {
      String[] enumParts = splitEnumValues(enumScopePart);
      for (String enumPart : enumParts) {
        if (!acceptEnumPart(enumPart)) {
          return false;
        }
      }
    } else {
      return false;
    }
    return true;
  }
  
  
  protected boolean acceptRegExpScopePart(String regExpScopePart) {
    if (regExpScopePart.startsWith("/") &&
        regExpScopePart.endsWith("/")) {
      try {
        Pattern.compile(regExpScopePart.substring(1, regExpScopePart.length() -1));
        return true;
      } catch (PatternSyntaxException e) {
        logger.warn(null, e);
        return false;
      }
    } else {
      // TODO we might want to support Flags after closing / (like i for caseInsensitive)
      return false;
    }
  }
  
  
  protected boolean acceptEnumPart(String enumPart) {
    return (enumPart.equals("*") ||
            ENUM_PART_PATTERN.matcher(enumPart).matches());
  }
  
  
  private Matcher getDefaultRightPatternMatcher(String toMatch) {
    return UserManagement.RIGHT_PATTERN_PATTERN.matcher(toMatch);
  }
  
  
  static String[] splitRightScopeIntoParts(String scope) {
    List<String> parts = new ArrayList<String>();
    StringBuilder partBuilder = new StringBuilder();
    boolean enumOpen = false;
    boolean regExpOpen = false;
    int index = 0;
    while (index < scope.length()) {
      switch (scope.charAt(index)) {
        case ':' :
          if (enumOpen || regExpOpen) {
            partBuilder.append(':');
          } else {
            parts.add(partBuilder.toString());
            partBuilder = new StringBuilder();
          }
          break;
        case '\\' :
          partBuilder.append('\\');
          index++;
          partBuilder.append(scope.charAt(index));
          break;
        case '[' :
          if (regExpOpen) {
            partBuilder.append(scope.charAt(index));
          } else {
            if (enumOpen) {
              throw new UnsupportedOperationException("nested enums!");
            }
            enumOpen = true;
            partBuilder.append(scope.charAt(index));
          }
          break;
        case ']' :
          if (regExpOpen) {
            partBuilder.append(scope.charAt(index));
          } else {
            if (!enumOpen) {
              throw new UnsupportedOperationException("closing brace encountered without open!");
            }
            enumOpen = false;
            partBuilder.append(scope.charAt(index));
          }
          break;
        case '/' :
          if (enumOpen) {
            partBuilder.append(scope.charAt(index));
          } else {
            if (regExpOpen) {
              regExpOpen = false;
            } else {
              regExpOpen = true;
            }
            partBuilder.append(scope.charAt(index));
          }
          break;
        default :
          partBuilder.append(scope.charAt(index));
          break;
      }
      index++;
    }
    parts.add(partBuilder.toString());
    return parts.toArray(new String[0]);
  }
  
  
  static String[] splitScopedRightIntoParts(String right) {
    return SCOPED_RIGHT_SPERATOR.split(right, -1); //limit = -1, damit Leerstrings am Ende erhalten bleiben
  }
  
  
  static String[] splitEnumValues(String enumValue) {
    return ENUM_VALUE_SEPERATOR_PATTERN.split(enumValue.substring(1, enumValue.length() -1));
  }
  
  
}
