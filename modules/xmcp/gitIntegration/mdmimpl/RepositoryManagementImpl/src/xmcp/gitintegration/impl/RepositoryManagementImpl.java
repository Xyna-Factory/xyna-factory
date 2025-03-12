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
package xmcp.gitintegration.impl;



import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;

import xmcp.gitintegration.repository.Repository;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryConnectionGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;



public class RepositoryManagementImpl {

  private static Logger _logger = Logger.getLogger(RepositoryManagementImpl.class); 
  private static Pattern pattern = Pattern.compile("<workspaceConfig workspaceName=\"(.*?)\">");

  private static PreparedQueryCache queryCache = new PreparedQueryCache();

  private static final String CONFIG = "config";
  private static final String SAVED = "saved";
  private static final String XMOM = "XMOM";


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(RepositoryConnectionStorable.class);
    queryCache = new PreparedQueryCache();
  }


  public static void shutdown() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.unregisterStorable(RepositoryConnectionStorable.class);
  }


  private static String replaceSymbolicLink(Path linkPath) {
    // check symbolic link and its target
    Path targetPath;
    try {
      targetPath = linkPath.toRealPath();
      if (!Files.isDirectory(targetPath)) {
        return "Error: Could not follow symbolic link '" + linkPath + "'!";
      }
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not find symbolic link '" + linkPath + "'!";
    }
    // delete symbolic link
    try {
      Files.delete(linkPath);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not delete symbolic link '" + linkPath + "'!";
    }
    // create new directory at link path
    try {
      Files.createDirectory(linkPath);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Could not create directory '" + linkPath + "'!";
    }
    // copy content from target path to newly created link path
    if (!copyDirectoryContent(targetPath, linkPath)) {
      return "Error: Could not copy files from '" + targetPath + "' to '" + linkPath + "!";
    }
    return null;
  }


  private static boolean copyDirectoryContent(Path source, Path target) {
    try {
      Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          Files.createDirectories(target.resolve(source.relativize(dir).toString()));
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.copy(file, target.resolve(source.relativize(file).toString()));
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static boolean deleteDirectoryContent(Path path) {
    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static boolean deleteDirectory(Path path) {
    if (!deleteDirectoryContent(path)) {
      return false;
    }
    try {
      Files.delete(path);
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return false;
    }
    return true;
  }


  private static Long getRevision(String workspaceName) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return rm.getRevision(null, null, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
  }


  private static Long createWorkspace(String workspaceName) {
    WorkspaceManagement wm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    try {
      wm.createWorkspace(new Workspace(workspaceName));
      return getRevision(workspaceName);
    } catch (XFMG_CouldNotBuildNewWorkspace e) {
      return null;
    }
  }


  private static Long deleteWorkspace(String workspaceName) {
    WorkspaceManagement wm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    Long revision = getRevision(workspaceName);
    if (revision == null) {
      return null;
    }
    try {
      RemoveWorkspaceParameters params = new RemoveWorkspaceParameters();
      params.setForce(true);
      params.setCleanupXmls(true);
      wm.removeWorkspace(new Workspace(workspaceName), params);
      return revision;
    } catch (XFMG_CouldNotRemoveWorkspace e) {
      return null;
    }
  }
  private static boolean matchWsFile(Path filePath, BasicFileAttributes fileAttr) {
    return fileAttr.isRegularFile() && filePath.endsWith("workspace.xml");
  }


  public static String addRepositoryConnection(String path, String workspace, boolean full) {
    // check, if path exists
    if (!new File(path).isDirectory()) {
      return "Error: Path '" + path + "' is not a directory!";
    }
    Path basePath = Paths.get(path);
    // collect a list of all paths to workspace.xml files
    List<Path> wsXmls = new ArrayList<>();
    try {
      wsXmls.addAll(Files.find(basePath, Integer.MAX_VALUE, RepositoryManagementImpl::matchWsFile).collect(Collectors.toList()));
    } catch (IOException e) {
      _logger.error(e.getMessage(), e);
      return "Error: Exception occured while searching for workspace.xml files!";
    }
    // map workspace name to workspace xml paths
    Map<String, Path> workspaceXmlPathMap = new HashMap<>();
    wsXmls.stream().forEach(workspaceXmlPath -> {
      String fileContent;
      try {
        fileContent = Files.readString(workspaceXmlPath, StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(fileContent);
        if (matcher.find()) {
          String workspaceName = matcher.group(1);
          if (workspaceName != null && (full || workspaceName.equals(workspace))) {
            workspaceXmlPathMap.put(workspaceName, workspaceXmlPath);
          }
        }
      } catch (IOException e) {
        _logger.error(e.getMessage(), e);
      }
    });
    if (full && workspaceXmlPathMap.isEmpty()) {
      return "Error: Could not find any workspaces in path!";
    }
    if (!full && !workspaceXmlPathMap.containsKey(workspace)) {
      return "Error: Could not find given workspace in path!";
    }
    // make sure, workspaces don't exist within the factory
    for (String workspaceName : workspaceXmlPathMap.keySet()) {
      if (getRevision(workspaceName) != null) {
        return "Error: Workspace '" + workspaceName + "' already exists within the factory!";
      }
    } ;
    // set workspace name is within a config directory
    Set<String> workspaceXmlConfig = new HashSet<>();
    // map workspace name to workspace xml sub paths
    Map<String, Path> workspaceXmlSubPathMap = new HashMap<>();
    boolean isSplitted = false;
    for (String workspaceName : workspaceXmlPathMap.keySet()) {
      Path workspaceXmlPath = workspaceXmlPathMap.get(workspaceName);
      Path subPath = workspaceXmlPath.getParent();
      if (subPath.endsWith(CONFIG)) {
        subPath = subPath.getParent();
        workspaceXmlConfig.add(workspaceName);
        isSplitted = true;
      }
      workspaceXmlSubPathMap.put(workspaceName, subPath);
    }
    // make sure, saved/XMOM or XMOM is located in sub paths
    for (String workspaceName : workspaceXmlSubPathMap.keySet()) {
      Path subPath = workspaceXmlSubPathMap.get(workspaceName);
      if (!subPath.resolve(SAVED).resolve(XMOM).toFile().isDirectory() && !subPath.resolve(XMOM).toFile().isDirectory()) {
        return "Error: Sub path of workspace.xml for workspace '" + workspaceName + "' does not contain the " + SAVED + "/" + XMOM + " or "
            + XMOM + " directory! Subpath: " + subPath;
      }
    }
    // create workspaces within the factory
    Map<String, Long> workspaceRevisionMap = new HashMap<>();
    for (String workspaceName : workspaceXmlSubPathMap.keySet()) {
      Long revision = createWorkspace(workspaceName);
      if (revision == null) {
        return "Error: Could not create workspace '" + workspaceName + "' within the factory!";
      }
      workspaceRevisionMap.put(workspaceName, revision);
    }
    // connect newly created workspace to repository
    int count = 0;
    for (String workspaceName : workspaceXmlSubPathMap.keySet()) {
      Path subPath = workspaceXmlSubPathMap.get(workspaceName);
      Long revision = workspaceRevisionMap.get(workspaceName);
      Path revisionPath = Path.of(Constants.BASEDIR, Constants.REVISION_PATH, Constants.PREFIX_REVISION + revision);
      boolean savedInRepo = subPath.resolve(SAVED).resolve(XMOM).toFile().isDirectory();
      // check, whether saved/XMOM exists
      if (savedInRepo) {
        if (deleteDirectory(revisionPath)) {
          return "Error: Could not delete directory '" + revisionPath + "' within the factory!";
        }
        try {
          Files.createSymbolicLink(revisionPath, subPath);
        } catch (IOException e) {
          _logger.error(e.getMessage(), e);
          return "Error: Could not create symbolic link '" + revisionPath + "' within the factory!";
        }
      } else {
        try {
          Files.createDirectory(revisionPath.resolve(SAVED));
        } catch (IOException e) {
          _logger.error(e.getMessage(), e);
          return "Error: Could not create directory '" + revisionPath.resolve(SAVED) + "' within the factory!";
        }
        try {
          Files.createSymbolicLink(revisionPath.resolve(SAVED).resolve(XMOM), subPath.resolve(XMOM));
        } catch (IOException e) {
          _logger.error(e.getMessage(), e);
          return "Error: Could not create symbolic link '" + revisionPath.resolve(SAVED).resolve(XMOM) + "' within the factory!";
        }
        // create symlink for config directory, if any
        if (workspaceXmlConfig.contains(workspaceName)) {
          try {
            Files.createSymbolicLink(revisionPath.resolve(CONFIG), subPath.resolve(CONFIG));
          } catch (IOException e) {
            _logger.error(e.getMessage(), e);
            return "Error: Could not create symbolic link '" + revisionPath.resolve(CONFIG) + "' within the factory!";
          }
        } else {
          Path workspaceXmlPath = workspaceXmlPathMap.get(workspaceName);
          Path filename = workspaceXmlPath.getFileName();
          try {
            Files.createSymbolicLink(revisionPath.resolve(filename), workspaceXmlPath);
          } catch (IOException e) {
            _logger.error(e.getMessage(), e);
            return "Error: Could not create symbolic link '" + revisionPath.resolve(filename) + "' within the factory!";
          }
        }
      }
      // persist storable
      String subPathString = subPath.toString().substring(basePath.toString().length() + 1); //+1 for "/"
      persistRepositoryConnectionStorable(new RepositoryConnectionStorable(workspaceName, basePath.toString(), subPathString, savedInRepo,
                                                                           isSplitted));
      count++;
    }

    return "Successfully linked " + count + " workspace(s) to the repository.";
  }


  public static List<RepositoryConnection> listRepositoryConnections() {
    List<RepositoryConnection> result = new ArrayList<>();
    List<RepositoryConnectionStorable> storables = loadRepositoryConnections();
    for(RepositoryConnectionStorable storable : storables) {
      result.add(convert(storable));
    }
    return result;
  }

  private static RepositoryConnection convert(RepositoryConnectionStorable storable) {
    RepositoryConnection.Builder result = new RepositoryConnection.Builder();
    result.path(storable.getPath())
        .savedinrepo(storable.getSavedinrepo())
        .splitted(storable.getSplitted())
        .subpath(storable.getSubpath())
        .workspaceName(storable.getWorkspacename());
    return result.instance();
  }

  public static String removeRepositoryConnection(String workspace, boolean full, boolean delete) {
    List<? extends RepositoryConnectionStorable> storables = loadRepositoryConnections();
    if (!full) {
      storables.removeIf(storable -> !storable.getWorkspacename().equals(workspace));
    }
    int count = 0;
    for (RepositoryConnectionStorable storable : storables) {
      String workspaceName = storable.getWorkspacename();
      Long revision = getRevision(workspaceName);
      if (revision == null) {
        return "Error: Workspace '" + workspaceName + "' does not exist within the factory!";
      }
      Path revisionPath = Path.of(Constants.BASEDIR, Constants.REVISION_PATH, Constants.PREFIX_REVISION + revision);
      String error = replaceSymbolicLinks(revisionPath, storable);
      if(error != null) {
        return error;
      }

      deleteRepositoryConnectionStorable(workspaceName);
      if (delete) {
        deleteWorkspace(workspaceName);
      }
      count++;
    }
    return "Successfully removed " + count + " workspace(s) from the repository.";
  }


  private static String replaceSymbolicLinks(Path revisionPath, RepositoryConnectionStorable storable) {
    String error = null;
    List<Path> toReplace = new ArrayList<>();
    toReplace.add(storable.getSavedinrepo() ? revisionPath : revisionPath.resolve(SAVED).resolve(XMOM));
    if (storable.getSplitted() && Files.isSymbolicLink(revisionPath.resolve(CONFIG))) {
      toReplace.add(revisionPath.resolve(CONFIG));
    }
    for (Path pathToReplace : toReplace) {
      error = replaceSymbolicLink(pathToReplace);
      if (error != null) {
        return error;
      }
    }
    return error;
  }

  private static class LoadRepositoryConnections implements WarehouseRetryExecutableNoException<List<RepositoryConnectionStorable>> {

    @Override
    public List<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return new ArrayList<>(con.loadCollection(RepositoryConnectionStorable.class));
    }
  }


  public static List<RepositoryConnectionStorable> loadRepositoryConnections() {
    List<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new LoadRepositoryConnections());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class LoadConnectionsForSingleRepository implements WarehouseRetryExecutableNoException<List<RepositoryConnectionStorable>> {

    private String repo;


    public LoadConnectionsForSingleRepository(String repo) {
      this.repo = repo;
    }


    @Override
    public List<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      ResultSetReader<? extends RepositoryConnectionStorable> reader = new RepositoryConnectionStorable().getReader();
      PreparedQuery<? extends RepositoryConnectionStorable> query = queryCache.getQueryFromCache(QUERY_ENTRIES_FOR_LIST, con, reader);
      List<? extends RepositoryConnectionStorable> result = con.query(query, new Parameter(repo), -1);
      return new ArrayList<RepositoryConnectionStorable>(result);
    }


    private static final String QUERY_ENTRIES_FOR_LIST =
        "select * from " + RepositoryConnectionStorable.TABLE_NAME + " where " + RepositoryConnectionStorable.COL_PATH + "=?";

  }


  public static List<? extends RepositoryConnectionStorable> loadConnectionsForSingleRepository(String repo) {
    List<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new LoadConnectionsForSingleRepository(repo));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class LoadRepositoryConnectionForWorkspace implements WarehouseRetryExecutableNoException<Optional<RepositoryConnectionStorable>> {

    private String workspace;


    public LoadRepositoryConnectionForWorkspace(String workspace) {
      this.workspace = workspace;
    }


    @Override
    public Optional<RepositoryConnectionStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      try {
        RepositoryConnectionStorable result = new RepositoryConnectionStorable(workspace);
        con.queryOneRow(result);
        return Optional.of(result);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return Optional.empty();
      }
    }
  }


  public static Optional<RepositoryConnectionStorable> loadRepositoryConnectionForWorkspace(String workspace) {
    Optional<RepositoryConnectionStorable> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class))
          .execute(new LoadRepositoryConnectionForWorkspace(workspace));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return result;
  }


  private static class PersistRepositoryConnectionStorable implements WarehouseRetryExecutableNoResult {

    private RepositoryConnectionStorable content;


    public PersistRepositoryConnectionStorable(RepositoryConnectionStorable content) {
      this.content = content;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.persistObject(content);
    }
  }


  public static void persistRepositoryConnectionStorable(RepositoryConnectionStorable storable) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new PersistRepositoryConnectionStorable(storable));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class DeleteRepositoryConnectionStorable implements WarehouseRetryExecutableNoResult {

    private String workspaceName;


    public DeleteRepositoryConnectionStorable(String workspaceName) {
      this.workspaceName = workspaceName;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteOneRow(new RepositoryConnectionStorable(workspaceName));
    }
  }


  public static void deleteRepositoryConnectionStorable(String workspaceName) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class))
          .execute(new DeleteRepositoryConnectionStorable(workspaceName));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private static class DeleteAllRepositoryConnectionStorables implements WarehouseRetryExecutableNoResult {

    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.deleteAll(RepositoryConnectionStorable.class);
    }
  }


  public static void deleteAllRepositoryConnectionStorables() {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(RepositoryConnectionStorable.class)).execute(new DeleteAllRepositoryConnectionStorables());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static RepositoryConnection getRepositoryConnection(String workspaceName) {
    Optional<RepositoryConnectionStorable> opt = loadRepositoryConnectionForWorkspace(workspaceName);
    if(opt.isEmpty()) {
      throw new RuntimeException("No RepositoryConnection found (Workspace: " + workspaceName + ")");
    }
    RepositoryConnection repositoryConnection = new RepositoryConnection();
    repositoryConnection.setWorkspaceName(workspaceName);
    repositoryConnection.setPath(opt.get().getPath());
    repositoryConnection.setSubpath(opt.get().getSubpath());
    repositoryConnection.setSavedinrepo(opt.get().getSavedinrepo());
    repositoryConnection.setSplitted(opt.get().getSplitted());
    return repositoryConnection;
  }
  
  public static void updatetRepositoryConnection(RepositoryConnection repositoryConnection) {
    Optional<RepositoryConnectionStorable> opt = loadRepositoryConnectionForWorkspace(repositoryConnection.getWorkspaceName());
    if(opt.isEmpty()) {
      throw new RuntimeException("No RepositoryConnection found (Workspace: " + repositoryConnection.getWorkspaceName() + ")");
    }
    // Update Attributes
    opt.get().setPath(repositoryConnection.getPath());
    opt.get().setSubpath(repositoryConnection.getSubpath());
    opt.get().setSavedinrepo(repositoryConnection.getSavedinrepo());
    opt.get().setSplitted(repositoryConnection.getSplitted());
    persistRepositoryConnectionStorable(opt.get());
  }


  public static List<? extends RepositoryConnectionGroup> listRepositoryConnectionGroups() {
    List<RepositoryConnection> connections = RepositoryManagementImpl.listRepositoryConnections();
    List<RepositoryConnectionGroup> result = new ArrayList<>();
    Map<String, List<RepositoryConnection>> groups = new HashMap<>();
    for(RepositoryConnection connection: connections) {
      groups.putIfAbsent(connection.getPath(), new ArrayList<>());
      groups.get(connection.getPath()).add(connection);
    }
    for(String repoGroup : groups.keySet()) {
      Repository repo = new Repository.Builder().path(repoGroup).instance();
      List<RepositoryConnection> conns = groups.get(repoGroup);
      RepositoryConnectionGroup group = new RepositoryConnectionGroup.Builder().repository(repo).repositoryConnection(conns).instance();
      result.add(group);
    }
    return result;
  }
  
}
