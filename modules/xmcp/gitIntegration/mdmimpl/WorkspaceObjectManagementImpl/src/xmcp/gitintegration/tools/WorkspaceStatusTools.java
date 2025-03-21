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

package xmcp.gitintegration.tools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import base.Text;
import xmcp.gitintegration.InfoWorkspaceContentDiffGroup;
import xmcp.gitintegration.InfoWorkspaceContentDiffGroupList;
import xmcp.gitintegration.InfoWorkspaceContentDiffItem;
import xmcp.gitintegration.ListId;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.WorkspaceXmlPath;
import xmcp.gitintegration.impl.OutputCreator;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.WorkspaceContentItemDifferenceSelector;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;


public class WorkspaceStatusTools {

  private static Logger _logger = Logger.getLogger(WorkspaceStatusTools.class);


  public WorkspaceContent createWorkspaceContentFromText(List<? extends Text> list) {
    if (list == null) { throw new IllegalArgumentException("Parameter text list is empty"); }
    if (list.size() < 1) { throw new IllegalArgumentException("Parameter text is empty"); }
    WorkspaceContentCreator creator = new WorkspaceContentCreator();
    List<String> adapted = list.stream().filter(Objects::nonNull).map(x -> x.getText()).collect(Collectors.toList());
    WorkspaceContent ret = creator.createWorkspaceContentFromText(adapted);
    return ret;
  }
  
  
  public WorkspaceContent createWorkspaceContentFromFile(base.File fileIn) {
    if (fileIn == null) { throw new IllegalArgumentException("Parameter file is empty"); }
    if (fileIn.getPath() == null) { throw new IllegalArgumentException("Parameter file is empty"); }
    WorkspaceContentCreator creator = new WorkspaceContentCreator();
    File file = new File(fileIn.getPath());
    WorkspaceContent ret = creator.createWorkspaceContentFromFile(file);
    return ret;
  }
  
  
  public InfoWorkspaceContentDiffGroupList adaptWorkspaceDifferenceList(ListId listid) {
    if (listid == null) { throw new IllegalArgumentException("Parameter list id is empty"); }
    InfoWorkspaceContentDiffGroupList ret = new InfoWorkspaceContentDiffGroupList();
    Map<String, InfoWorkspaceContentDiffGroup> groupmap = new TreeMap<>();
    try {
      WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
      WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
      WorkspaceContentDifferences difflist = storage.loadDifferences(listid.getListId());
      if (difflist == null) { return ret; }
      if (difflist.getDifferences() == null) { return ret; }
      ret.unversionedSetListId(listid.getListId());
      ret.unversionedSetWorkspaceName(difflist.getWorkspaceName());
      OutputCreator<WorkspaceContentItem, WorkspaceContentDifference, WorkspaceContentItemDifferenceSelector> outputCreator = 
        new OutputCreator<WorkspaceContentItem, WorkspaceContentDifference, WorkspaceContentItemDifferenceSelector>(
          new WorkspaceContentItemDifferenceSelector());
      for (WorkspaceContentDifference diff : difflist.getDifferences()) {
        String output = outputCreator.createOutput(diff, portal);
        InfoWorkspaceContentDiffGroup group = groupmap.get(diff.getContentType());
        if (group == null) {
          group = new InfoWorkspaceContentDiffGroup();
          group.unversionedSetContentType(diff.getContentType());
          ret.addToInfoWorkspaceContentDiffGroup(group);
          group.unversionedSetGroupIndex(ret.getInfoWorkspaceContentDiffGroup().size() - 1);
          groupmap.put(diff.getContentType(), group);
        }
        InfoWorkspaceContentDiffItem item = new InfoWorkspaceContentDiffItem();
        item.unversionedSetDifferenceInfo(output);
        item.unversionedSetDifferenceType(diff.getDifferenceType());
        group.addToDifferenceList(item);
        item.unversionedSetItemIndex(group.getDifferenceList().size() - 1);
        item.unversionedSetNumberOfLines(output.split("\n").length);
        item.unversionedSetEntryId(diff.getEntryId());
        String suggested = (diff.getDifferenceType() == null) ? null : diff.getDifferenceType().getClass().getSimpleName(); 
        item.unversionedSetSuggestedResolution(suggested);
      }
    }
    catch (Exception e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }


  public WorkspaceXmlPath getPathToWorkspaceXml(RepositoryConnection repconn) {
    if (repconn == null) { throw new IllegalArgumentException("Parameter repositoryConnection is empty"); }
    if (repconn.getWorkspaceName() == null) { throw new IllegalArgumentException("Parameter repositoryConnection is incomplete"); }
    try {
      WorkspaceContentCreator creator = new WorkspaceContentCreator();
      File file = creator.determineWorkspaceXMLFile(repconn.getWorkspaceName());
      WorkspaceXmlPath ret = new WorkspaceXmlPath();
      ret.setPathInRevisionDir(file.getPath());
      ret.setRepositoryPath(repconn.getPath());
      if (repconn.getSplitted()) {
        Path path = Paths.get(repconn.getSubpath(), WorkspaceContentCreator.WORKSPACE_XML_SPLITNAME);
        ret.setRepositorySubpath(path.toString());
      } else {
        Path path = Paths.get(repconn.getSubpath(), WorkspaceContentCreator.WORKSPACE_XML_FILENAME);
        ret.setRepositorySubpath(path.toString());
      }
      return ret;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
