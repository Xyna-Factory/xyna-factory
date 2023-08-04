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

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;

import xmcp.gitintegration.repository.Repository;



public class RepositoryManagementStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(RepositoryStorable.class);
  }


  public Repository queryRepository(String path) {
    try {
      return buildExecutor().execute(new QueryRepository(path));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<Repository> listAllRepositories() {
    try {
      return buildExecutor().execute(new ListAllRepositories());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void addRepository(Repository repository) {
    try {
      buildExecutor().execute(new AddRepository(repository));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void removeRepository(Repository repository) {
    try {
      buildExecutor().execute(new RemoveRepository(repository));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private WarehouseRetryExecutorBuilder buildExecutor() {
    return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY).storable(RepositoryStorable.class);
  }


  private static class RemoveRepository implements WarehouseRetryExecutableNoResult {

    private Repository repository;


    public RemoveRepository(Repository repository) {
      this.repository = repository;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      RepositoryStorable storable = new RepositoryStorable(repository.getPath());
      con.deleteOneRow(storable);
    }
  }

  private static class AddRepository implements WarehouseRetryExecutableNoResult {

    private Repository repository;


    public AddRepository(Repository repository) {
      this.repository = repository;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      RepositoryStorable storable = new RepositoryStorable(repository.getPath(), repository.getUsesAuth());
      con.persistObject(storable);
    }

  }

  private static class ListAllRepositories implements WarehouseRetryExecutableNoException<List<Repository>> {

    @Override
    public List<Repository> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      return con.loadCollection(RepositoryStorable.class).stream().map(x -> convert(x)).collect(Collectors.toList());
    }

  }

  private static class QueryRepository implements WarehouseRetryExecutableNoException<Repository> {

    private String repo;


    public QueryRepository(String repo) {
      this.repo = repo;
    }


    @Override
    public Repository executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      RepositoryStorable storable = new RepositoryStorable(repo);
      try {
        con.queryOneRow(storable);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      return convert(storable);
    }
  }


  private static Repository convert(RepositoryStorable storable) {
    Repository.Builder result = new Repository.Builder();
    result.path(storable.getRepopath());
    result.usesAuth(storable.getUserauth());
    return result.instance();
  }

}
