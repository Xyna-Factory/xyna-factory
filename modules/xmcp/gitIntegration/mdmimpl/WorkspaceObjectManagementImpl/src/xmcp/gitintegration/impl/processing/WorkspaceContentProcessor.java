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
package xmcp.gitintegration.impl.processing;



import java.util.Collection;
import java.util.List;
import org.w3c.dom.Node;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentItem;



//when adding new supported workspace content, create an implementer of this interface
//and register at WorkspaceContentProcessingPortal
//implementers of this interface should not hold state
public interface WorkspaceContentProcessor<T extends WorkspaceContentItem> {

  // 'read' interface - do not modify data

  /**
   * Compare a list of WorkspaceContentItems in from with items in to. 
   * Creating one WorkspaceContentDifference for each WorkspaceContentItem, 
   * unless that item is unchanged.
   * Does not set the id in result entries.
   * @param from
   * Starting point of comparison - usually from the current factory's workspace content
   * @param to
   * WorkspaceContent to compare against - usually read from workspace.xml
   * @return
   * A list of all differences between from and to belonging to type T.
   * If a workspaceContentItem is present only in from, a WorkspaceContentDifference with 
   * DifferenceType DELETE is created
   * If a workspaceContentItem is present only in to, a WorkspaceContentDifference with
   * DifferenceType CREATE is created
   * If a workspaceContentItem is present in both from and to, but at least one member is
   * different, a WorkspaceContentDifference with DifferenceType MODIFY is created. Multiple
   * difference between matching workspaceContentItems result in a single WorkspaceContentDifference.
   */
  public List<WorkspaceContentDifference> compare(Collection<? extends T> from, Collection<? extends T> to);


  /**
   * Parses a given XML node into a WorkspaceContentItem of type T
   * @param node
   * XML node matching the result of writeItem
   * @return
   * A WorkspaceContentItem of type T, representing the values stored in node
   */
  public T parseItem(Node node);


  /**
   * Creates an XML node representing the values item
   * @param builder
   * XmlBuilder to write the result into
   * @param item
   * WorkspaceContentItem to convert
   */
  public void writeItem(XmlBuilder builder, T item);


  /**
   * Tag name of the WorkspaceContentItem. When calling writeItem, an XML entry
   * enclosed in a tag of this name is created.
   * @return
   * Tag name of the WorkspaceContentItem
   */
  public String getTagName();


  /**
   * Creates a String containing information on fields identifying item.
   * @param item
   * WorkspaceContentItem to extract key information from
   * @return
   * String containing information on fields identifying item
   */
  public String createItemKeyString(T item);


  /**
   * Creates a String representing all differences between from and to.
   * If there are multiple differences between the two WorkspaceContentItems,
   * the first line is empty and every following line is indented
   * @param from
   * Starting point of comparison
   * @param to
   * WorkspaceContentItem to compare against
   * @return
   * String representing all differences between from and to
   */
  public String createDifferencesString(T from, T to);


  /**
   * Creates a list of WorkspaceContentItems currently configured in the factory
   * @param revision
   * Revision of the workspace to create items for
   * @return
   * List of all WorkspaceContentItems of type T
   */
  public List<T> createItems(Long revision);


  // 'write' interface - modify data


  /**
   * Creates a WorkspaceContentItem in the given workspace
   * @param item
   * WorkspaceContentItem defining the information to add
   * @param revision
   * revision of the workspace to add the WorkspaceContentItem into
   */
  public void create(T item, long revision);


  /**
   * Modifies the WorkspaceContentItem defined in from to match to. Throws an exception, if
   * the current workspace configuration does not match from.
   * @param from
   * WorkspaceContentItem as it should be in the workspace before modification
   * @param to
   * WorkspaceContentItem as it should be in the workspace after modification
   * @param revision
   * revision of the workspace to add the WorkspaceContentItem into
   */
  public void modify(T from, T to, long revision);


  /**
   * Deletes the WorkspaceContentItem from the workspace
   * @param item
   * WorkspaceContentItem to delete
   * @param revision
   * revision of the workspace to remove the WorkspaceCOntentItem from
   */
  public void delete(T item, long revision);

}
