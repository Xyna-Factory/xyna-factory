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
package com.gip.xyna.xdev.xlibdev.repositoryaccess;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xdev.exceptions.XDEV_AlreadyVersionedException;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.exceptions.XDEV_NotVersionedException;
import com.gip.xyna.xdev.exceptions.XDEV_PathLockedException;
import com.gip.xyna.xdev.exceptions.XDEV_PathNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RepositoryAccessException;
import com.gip.xyna.xdev.exceptions.XDEV_TimeoutException;
import com.gip.xyna.xdev.exceptions.XDEV_UnversionedParentException;


public interface RepositoryAccess {
  
  public void init(String name, String typeName, String localRepositoryBase, Map<String, Object> paramMap) throws XDEV_CodeAccessInitializationException;
  
  public void shutdown();
  
  public String getName();

  public String getLabel();

  public String getTypename();
  
  public String getLocalRepository();
  
  public Map<String, Object> getParamMap();
  
  public RepositoryTransaction beginTransaction(String clientIdentifier);
  
  public RepositoryRevision getHeadVersion();
  
  public void registerListener(RevisionChangeListener listener);
  
  public void unregisterListener(RevisionChangeListener listener);
  
  public List<StringParameter<?>> getParameterInformation();
  
  public void writeExtendedInformation(String[] args, Writer writer);
  
  public interface RepositoryTransaction {
    
    public List<RepositoryItemModification> update(String[] path, RepositoryRevision revision, RecursionDepth depth) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_TimeoutException;
    
    public List<RepositoryItemModification> delete(String[] path) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_TimeoutException;
    
    public RepositoryRevision commit(String[] path, String message, RecursionDepth depth) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_PathLockedException,
             XDEV_TimeoutException, XDEV_UnversionedParentException;
    
    public List<RepositoryItemModification> add(String[] path, RecursionDepth depth) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_AlreadyVersionedException, XDEV_TimeoutException;
    
    public List<RepositoryItemModification> status(String[] path)
      throws XDEV_RepositoryAccessException, XDEV_TimeoutException, XDEV_PathNotFoundException, XDEV_NotVersionedException;
    
    public List<RepositoryItemModification> checkout(String[] path, RepositoryRevision revision) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_TimeoutException;

    public List<RepositoryItemModification> checkout(String[] path, RepositoryRevision revision, RecursionDepth depth) 
                    throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_TimeoutException;
    
    public List<String> revert(String path[], RecursionDepth depth) 
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_TimeoutException;
    
    public List<RepositoryItemModification> move(String[] paths, String targetpath)
                    throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_TimeoutException;

    public String createBranch(String sourcepath[], String targetpath, String message)
      throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException, XDEV_NotVersionedException, XDEV_TimeoutException;

    public List<String> listBranches() throws XDEV_RepositoryAccessException, XDEV_PathNotFoundException;
    
    public void rollback() throws XDEV_RepositoryAccessException;
    
    public void endTransaction() throws XDEV_RepositoryAccessException;
    
    public void setTransactionProperty(String identifier, Object value) throws IllegalArgumentException;
    
  }
  
  
  public interface RevisionChangeListener {
    
    public void newVersion(RepositoryRevision version);
    
  }


  public static enum RecursionDepth {
    /**
     * Just the named directory D, no entries.
     */
    TARGET_ONLY,
    /**
     * D + its file children, but not subdirs.
     */
    TARGET_AND_DIRECT_CHILDREN,
    /**
     * D + all descendants (full recursion from D).
     */
    FULL_RECURSION;

  }
  
  
  public static interface RepositoryRevision extends Comparable<RepositoryRevision> {
    
    public String getStringRepresentation();
    
  }
  

}
