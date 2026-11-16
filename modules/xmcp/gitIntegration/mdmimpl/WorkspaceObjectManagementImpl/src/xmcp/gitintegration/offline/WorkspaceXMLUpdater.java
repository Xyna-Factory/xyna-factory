/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xmcp.gitintegration.offline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.xfcli.scriptentry.XMOMCompiler;

import xmcp.gitintegration.RuntimeContextDependency;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.impl.WorkspaceConfigSplit;
import xmcp.gitintegration.impl.WorkspaceContentCreator;
import xmcp.gitintegration.impl.processing.RuntimeContextDependencyProcessor;
import xmcp.gitintegration.impl.xml.WorkspaceContentXmlConverter;


/**
 * Update application references in workspace configurations.
 * Input arguments are paths to scan for applications and workspaces.
 * Reports written version changes grouped by application and version transition.
 */
public class WorkspaceXMLUpdater {

  public static void main(String[] args) throws Exception {
    List<File> paths = parseInput(args);
    List<Application> applications = collectApplications(paths);
    List<File> workspaces = collectWorkspaces(paths);
    List<DependencyChange> changes = new ArrayList<>();

    for (File workspace : workspaces) {
      WorkspaceContent content = createWorkspaceContent(workspace);
      List<DependencyChange> workspaceChanges = updateWorkspaceContent(content, applications);
      if (!workspaceChanges.isEmpty()) {
        writeWorkspaceContent(workspace, content);
        changes.addAll(workspaceChanges);
      }
    }
    printChanges(changes);
  }


  private static List<File> parseInput(String[] args) {
    List<File> paths = new ArrayList<>();
    for (String arg : args) {
      paths.add(new File(arg));
    }
    return paths;
  }


  private static List<Application> collectApplications(List<File> paths) throws IOException {
    List<Path> applicationRoots = new ArrayList<>();
    for (File path : paths) {
      try (Stream<Path> files = Files.walk(path.toPath())) {
        applicationRoots.addAll(files.filter(XMOMCompiler::isApplicationRootFolder).collect(Collectors.toList()));
      }
    }

    List<Application> applications = new ArrayList<>();
    for (Path applicationRoot : applicationRoots) {
      ApplicationXmlEntry applicationXml = XMOMCompiler.readApplicationXml(applicationRoot);
      applications.add(new Application(applicationXml.getApplicationName(), applicationXml.getVersionName()));
    }
    return applications;
  }


  private static List<File> collectWorkspaces(List<File> paths) throws IOException {
    List<File> workspaces = new ArrayList<>();
    for (File path : paths) {
      try (Stream<Path> files = Files.walk(path.toPath())) {
        workspaces.addAll(files.filter(Files::isRegularFile)
                               .filter(file -> file.getFileName().toString().equals("workspace.xml"))
                               .map(Path::toFile)
                               .collect(Collectors.toList()));
      }
    }
    return workspaces;
  }


  private static WorkspaceContent createWorkspaceContent(File workspace) {
    WorkspaceContentCreator creator = new WorkspaceContentCreator();
    WorkspaceContent content = creator.createWorkspaceContentFromFile(workspace);
    String split = content.getSplit();
    if (split == null || split.isEmpty()) {
      return content;
    }
    WorkspaceConfigSplit splitType = WorkspaceConfigSplit.fromId(split)
        .orElseThrow(() -> new IllegalArgumentException("Invalid workspace split type '" + split + "' in " + workspace));
    if (splitType == WorkspaceConfigSplit.NONE) {
      return content;
    }
    return creator.createWorkspaceContentFromFile(workspace.getAbsoluteFile().getParentFile());
  }


  private static List<DependencyChange> updateWorkspaceContent(WorkspaceContent content, List<Application> applications) {
    List<DependencyChange> changes = new ArrayList<>();
    if (content.getWorkspaceContentItems() == null) {
      return changes;
    }
    for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
      if (!(item instanceof RuntimeContextDependency)) {
        continue;
      }
      RuntimeContextDependency dependency = (RuntimeContextDependency) item;
      if (!RuntimeContextDependencyProcessor.DEP_TYPE_APPLICATION.equals(dependency.getDepType())) {
        continue;
      }
      for (Application application : applications) {
        if (application.getName().equals(dependency.getDepName())) {
          if (!Objects.equals(dependency.getDepAddition(), application.getVersionName())) {
            changes.add(new DependencyChange(content.getWorkspaceName(), application.getName(),
                                             dependency.getDepAddition(), application.getVersionName()));
            dependency.setDepAddition(application.getVersionName());
          }
          break;
        }
      }
    }
    return changes;
  }


  private static class DependencyChange {

    private final String workspace;
    private final String application;
    private final String oldVersion;
    private final String newVersion;


    private DependencyChange(String workspace, String application, String oldVersion, String newVersion) {
      this.workspace = workspace;
      this.application = application;
      this.oldVersion = oldVersion;
      this.newVersion = newVersion;
    }
  }


  private static void printChanges(List<DependencyChange> changes) {
    Set<String> workspaces = new HashSet<>();
    Set<String> applications = new HashSet<>();
    Map<String, Set<String>> references = new LinkedHashMap<>();
    for (DependencyChange change : changes) {
      workspaces.add(change.workspace);
      applications.add(change.application);
      String transition = change.application + ": " + change.oldVersion + " -> " + change.newVersion;
      references.computeIfAbsent(transition, key -> new LinkedHashSet<>()).add(change.workspace);
    }
    System.out.println("Updated " + changes.size() + " runtime context dependencies across " + workspaces.size()
        + " workspaces and " + applications.size() + " applications" + (changes.isEmpty() ? "." : ":"));
    for (Map.Entry<String, Set<String>> reference : references.entrySet()) {
      Set<String> referencingWorkspaces = reference.getValue();
      String firstWorkspace = referencingWorkspaces.iterator().next();
      String more = referencingWorkspaces.size() > 1 ? " and " + (referencingWorkspaces.size() - 1) + " more" : "";
      System.out.println(reference.getKey() + " (referenced by " + firstWorkspace + more + ")");
    }
  }


  private static void writeWorkspaceContent(File workspace, WorkspaceContent content) throws IOException {
    WorkspaceContentXmlConverter converter = new WorkspaceContentXmlConverter();
    String split = content.getSplit();
    if (split == null || split.isEmpty() || WorkspaceConfigSplit.NONE.getId().equals(split)) {
      Files.writeString(workspace.toPath(), converter.convertToXml(content));
      return;
    }

    WorkspaceConfigSplit splitType = WorkspaceConfigSplit.fromId(split)
        .orElseThrow(() -> new IllegalArgumentException("Invalid workspace split type '" + split + "' in " + workspace));
    Path configDirectory = workspace.toPath().toAbsolutePath().getParent();
    List<Path> oldDependencyFiles = new ArrayList<>();
    if (splitType == WorkspaceConfigSplit.FINE) {
      Path dependencyDirectory = configDirectory.resolve(new RuntimeContextDependencyProcessor().getTagName());
      if (Files.exists(dependencyDirectory)) {
        try (Stream<Path> files = Files.walk(dependencyDirectory)) {
          oldDependencyFiles.addAll(files.filter(Files::isRegularFile)
                                        .filter(file -> file.getFileName().toString().endsWith(".xml"))
                                        .collect(Collectors.toList()));
        }
      }
    }

    List<WorkspaceContentItem> dependencies = new ArrayList<>();
    if (content.getWorkspaceContentItems() != null) {
      for (WorkspaceContentItem item : content.getWorkspaceContentItems()) {
        if (item instanceof RuntimeContextDependency) {
          dependencies.add(item);
        }
      }
    }
    WorkspaceContent.Builder dependencyContent = new WorkspaceContent.Builder();
    dependencyContent.workspaceName(content.getWorkspaceName());
    dependencyContent.split(split);
    dependencyContent.workspaceContentItems(dependencies);
    List<Pair<String, String>> files = converter.splitContent(dependencyContent.instance());
    Set<Path> writtenFiles = new HashSet<>();
    for (Pair<String, String> file : files) {
      if (WorkspaceContentCreator.WORKSPACE_XML_FILENAME.equals(file.getFirst())) {
        continue;
      }
      Path output = configDirectory.resolve(file.getFirst());
      Files.createDirectories(output.getParent());
      Files.writeString(output, file.getSecond());
      writtenFiles.add(output);
    }
    for (Path oldDependencyFile : oldDependencyFiles) {
      if (!writtenFiles.contains(oldDependencyFile)) {
        Files.delete(oldDependencyFile);
      }
    }
  }
}
