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



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;

import xmcp.gitintegration.impl.references.ReferenceObjectType;



/***
*
* 'Interface' between the outside world and the persistence classes.
* Users of this class no not need to be aware of how the data backing
* References work.
*
*/
public class ReferenceStorage {

  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();

    ods.registerStorable(ReferenceStorable.class);
  }


  public List<ReferenceStorable> getAllReferences() {
    try {
      return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new LoadReferences());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<ReferenceStorable> getAllReferencesForWorkspace(Long revision) {
    try {
      return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new LoadReferences(revision));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public List<ReferenceStorable> getAllReferencesForObject(Long revision, String objectName) {
    try {
      return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new LoadReferences(revision, objectName));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }

  public List<ReferenceStorable> getAllReferencesForType(Long revision, ReferenceObjectType objectType) {
    try {
      return WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new LoadReferences(revision, objectType));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }

  public void persist(ReferenceStorable storable) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new PersistReference(storable));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteReference(String path, Long revision, String objectName) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(ReferenceStorable.class)).execute(new DeleteReference(path, revision, objectName));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  private class LoadReferences implements WarehouseRetryExecutableNoException<List<ReferenceStorable>> {

    private Long revision;
    private String objectName;
    private ReferenceObjectType objectType;


    public LoadReferences() {

    }


    public LoadReferences(Long revision) {
      this.revision = revision;
    }


    public LoadReferences(Long revision, String objectName) {
      this.revision = revision;
      this.objectName = objectName;
    }
    
    public LoadReferences(Long revision, ReferenceObjectType objectType) {
      this.revision = revision;
      this.objectType = objectType;
    }


    @Override
    public List<ReferenceStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<ReferenceStorable> result = new ArrayList<ReferenceStorable>(con.loadCollection(ReferenceStorable.class));

      if (revision != null) {
        result.removeIf(x -> !revision.equals(x.getWorkspace()));
      }

      if (objectName != null) {
        result.removeIf(x -> !objectName.equals(x.getObjectName()));
      }
      
      if(objectType != null) {
        result.removeIf(x -> !objectType.toString().equals(x.getObjecttype()));
      }

      return result;
    }


  }

  private class PersistReference implements WarehouseRetryExecutableNoResult {

    private ReferenceStorable content;


    public PersistReference(ReferenceStorable content) {
      this.content = content;
      if(this.content.getIndex() == null || this.content.getIndex().isEmpty()) {
        this.content.setIndex(createIndex(content));
      }
    }
    
    private String createIndex(ReferenceStorable content) {
      return String.format("%s:%s:%s:%s:%s", 
                           content.getPath(), 
                           content.getReftype(), 
                           content.getObjecttype(), 
                           content.getWorkspace(), 
                           content.getObjectName());
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      con.persistObject(content);
    }
  }


  private class DeleteReference implements WarehouseRetryExecutableNoResult {

    private String path;
    private Long revision;
    private String objectName;


    public DeleteReference(String path, Long revision, String objectName) {
      this.path = path;
      this.revision = revision;
      this.objectName = objectName;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      Collection<ReferenceStorable> candidates = con.loadCollection(ReferenceStorable.class);
      candidates.removeIf(x -> !x.getPath().equals(path));
      candidates.removeIf(x -> !x.getWorkspace().equals(revision));
      candidates.removeIf(x -> !x.getObjectName().equals(objectName));
      for(ReferenceStorable storable : candidates) {
        con.deleteOneRow(storable);
      }
    }

  }
}
