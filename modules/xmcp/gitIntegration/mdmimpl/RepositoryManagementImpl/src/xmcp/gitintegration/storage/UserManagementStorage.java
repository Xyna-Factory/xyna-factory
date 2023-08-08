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
package xmcp.gitintegration.storage;



import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xmcp.gitintegration.repository.RepositoryUser;



public class UserManagementStorage {

  public static final String SEC_STORE_DESTINATION = "repositoryusers";
  public static final String CLI_USERNAME = "<CLI_USER>";

  private static PreparedQueryCache queryCache = new PreparedQueryCache();


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(RepositoryUserStorable.class);
  }


  public String loadPassword(String factoryUser, String repository) throws XynaException {
    String id = RepositoryUserStorable.createIdentifier(factoryUser, repository);
    return (String) SecureStorage.getInstance().retrieve(SEC_STORE_DESTINATION, id);
  }


  public void AddUserToRepository(String factoryUser, String repoUser, String repository, String password, String mail) {
    try {
      buildExecutor().execute(new AddUserToRepository(factoryUser, repoUser, repository, mail));
      SecureStorage sec = SecureStorage.getInstance();
      sec.store(SEC_STORE_DESTINATION, RepositoryUserStorable.createIdentifier(factoryUser, repository), password);
    } catch (Exception e) {
      throw new RuntimeException("Could not add user to Repository");
    }
  }


  public RepositoryUser loadUser(String factoryUser, String repository) {
    try {
      return buildExecutor().execute(new LoadUser(factoryUser, repository));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<RepositoryUser> listAllUsers() {
    try {
      return buildExecutor().execute(new ListAllUsers());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<RepositoryUser> listUsersOfRepo(String repository) {
    try {
      return buildExecutor().execute(new ListUsersOfRepo(repository));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<String> listReposOfUser(String username) {
    try {
      return buildExecutor().execute(new ListReposOfUser(username));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void removeUserFromRepository(String repository, String factoryUser, String repoUser) {
    try {
      buildExecutor().execute(new RemoveUserFromRepository(repository, factoryUser, repoUser));
    } catch (PersistenceLayerException e) {
      new RuntimeException(e);
    }
  }


  public void removeAllUsersFromRepository(String repository) {
    try {
      buildExecutor().execute(new RemoveAllUsersFromRepositoy(repository));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void removeUserFromAllRepositories(String user) {
    try {
      buildExecutor().execute(new RemoveUserFromAllRepositoies(user));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(RepositoryUserStorable.class);
  }


  private static final String QUERY_REPOS_OF_USER =
      "SELECT * FROM " + RepositoryUserStorable.TABLE_NAME + " WHERE " + RepositoryUserStorable.COL_FACTORY_USERNAME + "=?";
  private static final String QUERY_USERS_OF_REPO =
      "SELECT * FROM " + RepositoryUserStorable.TABLE_NAME + " WHERE " + RepositoryUserStorable.COL_REPOSITORY + "=?";
  private static final String QUERY_REPO_USER = "SELECT * FROM " + RepositoryUserStorable.TABLE_NAME + " WHERE "
      + RepositoryUserStorable.COL_FACTORY_USERNAME + "=? AND " + RepositoryUserStorable.COL_REPOSITORY + "=?";


  private static class LoadUser implements WarehouseRetryExecutableNoException<RepositoryUser> {

    private String factoryUser;
    private String repository;


    public LoadUser(String factoryUser, String repository) {
      this.factoryUser = factoryUser;
      this.repository = repository;
    }


    @Override
    public RepositoryUser executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PreparedQuery<RepositoryUserStorable> query = queryCache.getQueryFromCache(QUERY_REPO_USER, con, RepositoryUserStorable.reader);
      RepositoryUserStorable storable = con.queryOneRow(query, new Parameter(factoryUser, repository));
      return convert(storable);
    }

  }

  private static class AddUserToRepository implements WarehouseRetryExecutableNoResult {

    private String factoryUser;
    private String repoUser;
    private String repository;
    private String mail;

    public AddUserToRepository(String factoryUser, String repoUser, String repository, String mail) {
      this.factoryUser = factoryUser;
      this.repoUser = repoUser;
      this.repository = repository;
      this.mail = mail;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      RepositoryUserStorable storable = new RepositoryUserStorable(factoryUser, repoUser, repository, System.currentTimeMillis(), mail);
      con.persistObject(storable);
    }

  }

  private static class ListAllUsers implements WarehouseRetryExecutableNoException<List<RepositoryUser>> {

    @Override
    public List<RepositoryUser> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return con.loadCollection(RepositoryUserStorable.class).stream().map(x -> convert(x)).collect(Collectors.toList());
    }

  }

  private static class ListUsersOfRepo implements WarehouseRetryExecutableNoException<List<RepositoryUser>> {

    private String repository;


    public ListUsersOfRepo(String repository) {
      this.repository = repository;
    }


    @Override
    public List<RepositoryUser> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PreparedQuery<RepositoryUserStorable> query = queryCache.getQueryFromCache(QUERY_USERS_OF_REPO, con, RepositoryUserStorable.reader);
      List<RepositoryUserStorable> storableCollection = con.query(query, new Parameter(repository), -1);
      return storableCollection.stream().map(x -> convert(x)).collect(Collectors.toList());
    }
  }

  private static class ListReposOfUser implements WarehouseRetryExecutableNoException<List<String>> {

    private String user;


    public ListReposOfUser(String username) {
      this.user = username;
    }


    @Override
    public List<String> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PreparedQuery<RepositoryUserStorable> query = queryCache.getQueryFromCache(QUERY_REPOS_OF_USER, con, RepositoryUserStorable.reader);
      List<RepositoryUserStorable> storableCollection = con.query(query, new Parameter(user), -1);
      return storableCollection.stream().map(x -> x.getRepopath()).collect(Collectors.toList());
    }
  }

  private static class RemoveUserFromRepository implements WarehouseRetryExecutableNoResult {

    private String repository;
    private String factoryUser;
    private String repoUser;


    public RemoveUserFromRepository(String repository, String factoryUser, String repoUser) {
      this.repository = repository;
      this.factoryUser = factoryUser;
      this.repoUser = repoUser;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      RepositoryUserStorable storable = new RepositoryUserStorable(factoryUser, repoUser, repository, -1l, "");
      con.deleteOneRow(storable);
    }
  }

  private static class RemoveAllUsersFromRepositoy implements WarehouseRetryExecutableNoResult {

    private String repository;


    public RemoveAllUsersFromRepositoy(String repository) {
      this.repository = repository;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PreparedQuery<RepositoryUserStorable> query = queryCache.getQueryFromCache(QUERY_USERS_OF_REPO, con, RepositoryUserStorable.reader);
      List<RepositoryUserStorable> storableCollection = con.query(query, new Parameter(repository), -1);
      con.delete(storableCollection);

    }
  }

  private static class RemoveUserFromAllRepositoies implements WarehouseRetryExecutableNoResult {

    private String user;


    public RemoveUserFromAllRepositoies(String user) {
      this.user = user;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      PreparedQuery<RepositoryUserStorable> query = queryCache.getQueryFromCache(QUERY_REPOS_OF_USER, con, RepositoryUserStorable.reader);
      List<RepositoryUserStorable> storableCollection = con.query(query, new Parameter(user), -1);
      con.delete(storableCollection);
    }

  }


  private static RepositoryUser convert(RepositoryUserStorable storable) {

    if (storable == null) {
      throw new RuntimeException("Can't convert null value to RepositoryUser");
    }

    RepositoryUser.Builder result = new RepositoryUser.Builder();
    result.factoryUsername(storable.getFactoryusername());
    result.repositoryUsername(storable.getRepousername());
    result.repository(storable.getRepopath());
    result.created(storable.getCreatedtimestamp());
    result.mail(storable.getMail());
    return result.instance();
  }
}
