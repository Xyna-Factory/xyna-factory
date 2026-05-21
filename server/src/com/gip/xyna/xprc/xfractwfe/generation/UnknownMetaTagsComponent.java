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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class UnknownMetaTagsComponent implements HasMetaTags {

  private List<String> unknownMetaTags;


  @Override
  public boolean hasUnknownMetaTags() {
    return ((unknownMetaTags != null) && (unknownMetaTags.size() > 0));
  }


  @Override
  public List<String> getUnknownMetaTags() {
    return unknownMetaTags;
  }


  @Override
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    this.unknownMetaTags = unknownMetaTags;
  }


  @Override
  public void parseUnknownMetaTags(Element element, List<String> knownMetaTags) {
    Element meta = XMLUtils.getChildElementByName(element, GenerationBase.EL.META);
    List<Element> unknownMetaElements = XMLUtils.getFilteredSubElements(meta, knownMetaTags);
    unknownMetaTags = unknownMetaElements.stream().map(x -> XMLUtils.getXMLString(x, false)).collect(Collectors.toList());
  }


  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {
    if (unknownMetaTags == null) {
      return;
    }

    unknownMetaTags.forEach(tag -> XMLUtils.appendStringAsElement(tag, xml));
  }

}
