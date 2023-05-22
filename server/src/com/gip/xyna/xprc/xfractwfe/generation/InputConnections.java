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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;

public class InputConnections {
  private String[] varIds;
  private String[] paths;
  private Boolean[] userConnected;
  private Boolean[] constant;
  private String[] expectedTypes;
  private List<List<Element>> unknownMetaTags;

  public InputConnections(int length) {
    varIds = new String[length];
    paths = new String[length];
    userConnected = new Boolean[length];
    initializeBooleanArray(userConnected, false);
    constant = new Boolean[length];
    initializeBooleanArray(constant, false);
    expectedTypes = new String[length];
    unknownMetaTags = createMetaTagList(length);
  }

  public InputConnections(String[] varIds) {
    this(varIds, new String[varIds.length], new Boolean[varIds.length], new Boolean[varIds.length], new String[varIds.length]);
  }

  public InputConnections(String[] varIds, String[] paths, Boolean[] userConnected, Boolean[] constant, String[] expectedTypes) {
    this.varIds = varIds;
    this.paths = paths;
    this.userConnected = userConnected;
    this.constant = constant;
    this.expectedTypes = expectedTypes;
    this.unknownMetaTags = createMetaTagList(varIds.length);
  }

  public void addInputConnection(int index) {
    addInputConnection(index, null, null, false, false, null);
  }

  public void addInputConnection(int index, String varId, String path, boolean isUserConnected, boolean isConstant, String expectedType) {
    varIds = ArrayUtils.addToArray(varIds, index, varId);
    paths = ArrayUtils.addToArray(paths, index, path);
    userConnected = ArrayUtils.addToArray(userConnected, index, isUserConnected);
    constant = ArrayUtils.addToArray(constant, index, isConstant);
    expectedTypes = ArrayUtils.addToArray(expectedTypes, index, expectedType);
    unknownMetaTags.add(index, new ArrayList<Element>());
  }

  public void removeInputConnection(int index) {
    varIds = ArrayUtils.removeFromStringArray(varIds, index);
    paths = ArrayUtils.removeFromStringArray(paths, index);
    userConnected = ArrayUtils.removeFromBoolArray(userConnected, index);
    constant = ArrayUtils.removeFromBoolArray(constant, index);
    expectedTypes = ArrayUtils.removeFromStringArray(expectedTypes, index);
    unknownMetaTags.remove(index);
  }

  private static void initializeBooleanArray(Boolean[] array, boolean value) {
    for (int i = 0; i < array.length; i++) {
      array[i] = value;
    }
  }

  private static List<List<Element>> createMetaTagList(int size) {
    List<List<Element>> metaTags = new ArrayList<List<Element>>(size);
    while (metaTags.size() < size) {
      metaTags.add(new ArrayList<Element>());
    }

    return metaTags;
  }

  protected void parseSourceElement(Element sourceEl, int idx) {
    varIds[idx] = sourceEl.getAttribute(GenerationBase.ATT.REFID);
    paths[idx] = sourceEl.getAttribute(GenerationBase.ATT.PATH);

    Element meta = XMLUtils.getChildElementByName(sourceEl, GenerationBase.EL.META);
    if (meta != null) {
      Element linktype = XMLUtils.getChildElementByName(meta, GenerationBase.EL.LinkType);
      if (linktype != null) {
        String tc = XMLUtils.getTextContent(linktype);
        userConnected[idx] = EL.LINKTYPE_USER_CONNECTED.equals(tc);
        constant[idx] = EL.LINKTYPE_CONSTANT_CONNECTED.equals(tc);
      }

      Element expectedTypeEl = XMLUtils.getChildElementByName(meta, GenerationBase.EL.EXPECTED_TYPE);
      if (expectedTypes != null) {
        expectedTypes[idx] = XMLUtils.getTextContent(expectedTypeEl);
      }

      unknownMetaTags.set(idx, XMLUtils.getFilteredSubElements(meta, Arrays.asList(EL.LinkType, EL.EXPECTED_TYPE)));
    }
  }

  public String[] getVarIds() {
    return varIds;
  }

  public String[] getPaths() {
    return paths;
  }
  
  public Boolean[] getConstantConnected() {
    return constant;
  }

  public Boolean[] getUserConnected() {
    return userConnected;
  }

  public String[] getExpectedTypes() {
    return expectedTypes;
  }

  public boolean isUserConnected(String varId) {
    if ( (varId == null) || (varId.length() == 0) ) {
      return false;
    }

    int varIdx = getVarIdx(varId);
    if (varIdx < 0) {
      return false;
    }

    return getUserConnected()[varIdx];
  }
  
  public boolean isConstantConnected(String varId) {
    if ( (varId == null) || (varId.length() == 0) ) {
      return false;
    }

    int varIdx = getVarIdx(varId);
    if (varIdx < 0) {
      return false;
    }

    return getConstantConnected()[varIdx];
  }

  public int length() {
    return varIds.length;
  }

  public int getVarIdx(String varId) {
    String[] varIds = getVarIds();
    for (int varIdx = 0; varIdx < varIds.length; varIdx++) {
      if (varId.equals(varIds[varIdx])) {
        return varIdx;
      }
    }

    return -1;
  }

  public List<List<Element>> getUnknownMetaTags() {
    return unknownMetaTags;
  }

  public List<Element> getUnknownMetaTags(String varId) {
    if ( (varId == null) || (varId.length() == 0) ) {
      return new ArrayList<Element>();
    }

    int varIdx = getVarIdx(varId);
    if (varIdx < 0) {
      return new ArrayList<Element>();
    }

    return getUnknownMetaTags().get(varIdx);
  }

}
