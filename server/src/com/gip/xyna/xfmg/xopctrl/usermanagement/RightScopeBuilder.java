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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope.ScopePart;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope.ScopePartType;


public class RightScopeBuilder extends RightScopeParser {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(RightScopeBuilder.class);

  private String key;
  private List<RightScope.ScopePart> parts = new ArrayList<RightScope.ScopePart>();
  
  public RightScope buildRightScope(String scopeDefinition) {
    try {
      if (acceptRightScope(scopeDefinition)) {
        return new RightScope(scopeDefinition, key, parts);
      } else {
        return null;
      }
    } catch (UnsupportedOperationException e) {
      logger.debug("Could not build RightScope from definition: " + scopeDefinition, e);
      return null;
    }
  }
  
  @Override
  protected boolean acceptKey(String key) {
    boolean superResult = super.acceptKey(key);
    if (superResult) {
      this.key = key;
    }
    return superResult;
  }
  
  
  @Override
  protected boolean acceptWildcardScopePart(String wildcardScopePart) {
    boolean superResult = super.acceptWildcardScopePart(wildcardScopePart);
    if (superResult) {
      parts.add(new ScopePart(ScopePartType.WILDCARD, "*"));
    }
    return superResult;
  }
  
  
  @Override
  protected boolean acceptEnumerationScopePart(String enumScopePart) {
    boolean superResult = super.acceptEnumerationScopePart(enumScopePart);
    if (superResult) {
      parts.add(new ScopePart(ScopePartType.ENUMERATION, enumScopePart));
    }
    return superResult;
  }
  
  
  @Override
  protected boolean acceptPrimitiveScopePart(String primitiveScopePart) {
    boolean superResult = super.acceptPrimitiveScopePart(primitiveScopePart);
    if (superResult) {
      parts.add(new ScopePart(ScopePartType.PRIMITIVE, primitiveScopePart));
    }
    return superResult;
  }
  
  @Override
  protected boolean acceptRegExpScopePart(String regExpScopePart) {
    boolean superResult = super.acceptRegExpScopePart(regExpScopePart);
    if (superResult) {
      parts.add(new ScopePart(ScopePartType.REGEXP, regExpScopePart));
    }
    return superResult;
  }
  
  
  
}
