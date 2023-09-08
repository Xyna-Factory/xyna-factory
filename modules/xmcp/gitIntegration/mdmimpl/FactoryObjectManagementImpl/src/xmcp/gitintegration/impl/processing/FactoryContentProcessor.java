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
package xmcp.gitintegration.impl.processing;

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Node;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentItem;

public interface FactoryContentProcessor<T extends FactoryContentItem> {

  
  public List<T> createItems();
  public void writeItem(XmlBuilder builder, T item);
  public String getTagName();
  public T parseItem(Node node);
  public List<FactoryContentDifference> compare(Collection<? extends T> from, Collection<? extends T> to);
  public String createItemKeyString(T item);
  public String createDifferencesString(T from, T to);
  
  public void create(T item);
  public void modify(T from, T to);
  public void delete(T item);
  
  List<IgnorePatternInterface<T>> getIgnorePatterns();
}
