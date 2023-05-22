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

package com.gip.xyna.xnwh.xwarehousejobs;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_WarehouseJobRunnableParameterInvalidException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;



public class WarehouseJobRunnable extends JavaDestination {

  private static final long serialVersionUID = 4765500676420946699L;

  private static Logger logger = CentralFactoryLogging.getLogger(WarehouseJobRunnable.class);
  
  private WarehouseJob parentJob;
  private Runnable r;

  private String description;
  private WarehouseJobActionType actionType;
  private String[] parameters;


  public WarehouseJobRunnable(WarehouseJob parentJob, String fqName, WarehouseJobActionType actionType,
                              String... parameters) throws XNWH_WarehouseJobRunnableParameterInvalidException {
    super(fqName);
    this.parentJob = parentJob;
    this.r = getRunnableByActionType(actionType, parameters);
    this.actionType = actionType;
    this.parameters = parameters;
  }


  protected WarehouseJob getParentJob() {
    return parentJob;
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XynaException {

    getParentJob().setRunning(true);
    try {
      r.run();
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Error while executing warehouse job: " + t.getMessage(), t);
      XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaWarehouseJobManagement()
          .removeJob(getParentJob().getId());
    } finally {
      getParentJob().setRunning(false);
      if (!getParentJob().getJobSchedule().needsToRunAgain()) {
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaWarehouseJobManagement()
            .removeJob(getParentJob().getId());
      }
    }

    // TODO maybe it would be nice if the job would return some info bean at this point 
    return input;

  }


  public String[] getActionParameters() {
    return parameters;
  }


  public WarehouseJobActionType getActionType() {
    return actionType;
  }


  public void setDescription(String s) {
    this.description = s;
  }


  public String getDescription() {
    return description;
  }


  private static Runnable getRunnableByActionType(WarehouseJobActionType actionType, String... parameters) throws XNWH_WarehouseJobRunnableParameterInvalidException {

    switch (actionType) {
      case Copy:
      case Replace:
        if (parameters == null || parameters.length < 3) {
          throw new RuntimeException("Too few parameters");
        }
        boolean replace = actionType == WarehouseJobActionType.Replace;
        
        ODSConnectionType source;
        try {
          source = ODSConnectionType.getByString(parameters[0]);
        } catch (IllegalArgumentException e) {        
          throw new XNWH_WarehouseJobRunnableParameterInvalidException("source type", parameters[0], e);
        }
        
        ODSConnectionType target;
        try {
          target = ODSConnectionType.getByString(parameters[1]);
        } catch (IllegalArgumentException e) {
          throw new XNWH_WarehouseJobRunnableParameterInvalidException("target type", parameters[1], e);
        }
        
        String tableName = parameters[2];
        Class<? extends Storable> clazz = ODSImpl.getInstance().getStorableByTableName(tableName);
        if (clazz == null) {
          if (!XynaFactory.getInstance().isStartingUp()) {
            throw new RuntimeException("storable for table " + tableName + " is not registered.");
          }
        }
        
        if (parameters.length == 3) {
          try {
            return new WarehouseCopyOrReplaceRunnable(clazz, source, target, replace);
          } catch (PersistenceLayerException e) {
            throw new XNWH_WarehouseJobRunnableParameterInvalidException("source type is not queryable", parameters[1], e);
          }
        } else {
          String whereClause = parameters[3];
          boolean concurrencyProtection = false;
          if (parameters.length > 4) {
            concurrencyProtection = Boolean.parseBoolean(parameters[4]);
          }
          try {
            return new WarehouseCopyOrReplaceRunnable(clazz, source, target, replace, whereClause, concurrencyProtection);
          } catch (PersistenceLayerException e) {
            throw new XNWH_WarehouseJobRunnableParameterInvalidException("source type is not queryable", parameters[1], e);
          }
        }
      case Delete:
        if (parameters == null || parameters.length < 2) {
          throw new RuntimeException("Too few parameters");
        }
        
        String tableNameForDeletion = parameters[0];
        Class<? extends Storable> deletionClazz = ODSImpl.getInstance().getStorableByTableName(tableNameForDeletion);
        if (deletionClazz == null) {
          if (!XynaFactory.getInstance().isStartingUp()) {
            throw new RuntimeException("storable for table " + tableNameForDeletion + " is not registered.");
          }
        }
        
        List<ODSConnectionType> types = new ArrayList<ODSConnectionType>();
        for (int i = 1; i < parameters.length; i++) {
          try {
            types.add(ODSConnectionType.getByString(parameters[i]));
          } catch (IllegalArgumentException e) {
            // this is ugly but what choices do we have
          }
        }
        
        if (parameters.length == types.size()+1) {
          try {
            return new WarehouseDeleteRunnable(deletionClazz, types);
          } catch (PersistenceLayerException e) {
            throw new XNWH_WarehouseJobRunnableParameterInvalidException("source type is not queryable", parameters[1], e);
          }
        } else {
          String whereClause = parameters[types.size() + 1];
          boolean concurrencyProtection = false;
          if (parameters.length > types.size() + 2) {
            concurrencyProtection = Boolean.parseBoolean(parameters[types.size() + 2]);
          }
          try {
            return new WarehouseDeleteRunnable(deletionClazz, types, whereClause, concurrencyProtection);
          } catch (PersistenceLayerException e) {
            throw new XNWH_WarehouseJobRunnableParameterInvalidException("source type is not queryable", parameters[1], e);
          }
        }
      default:
        throw new XNWH_WarehouseJobRunnableParameterInvalidException("action type", actionType.toString());
    }
  }

  
  private static abstract class WarehouseBaseRunnable<S extends Storable<S>> implements Runnable {
    
    private final static Pattern forUpdateContainmentPattern = Pattern.compile(".*FOR UPDATE.*", Pattern.CASE_INSENSITIVE);
    private final static Pattern whereClauseBeginningPattern = Pattern.compile("WHERE .*", Pattern.CASE_INSENSITIVE);
    
    protected final Class<S> clazz;
    protected final ODS ods = ODSImpl.getInstance();
    protected final Set<ODSConnectionType> sourceConnectionTypes;
    protected final String sqlString;
    
    protected WarehouseBaseRunnable(Class<S> clazz, List<ODSConnectionType> sourceConnectionTypes, String whereClause, boolean queryForUpdate) throws PersistenceLayerException {
      this.clazz = clazz;
      this.sourceConnectionTypes = new HashSet<ODSConnectionType>(sourceConnectionTypes);
      sqlString = buildQueryString(whereClause, queryForUpdate);
    }
    
    
    private String buildQueryString(String whereClause, boolean queryForUpdate) {
      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM ");
      queryBuilder.append(Storable.getPersistable(clazz).tableName())
                  .append(' ');
      if (whereClause != null &&
          !whereClause.isEmpty()) {
        if (!whereClauseBeginningPattern.matcher(whereClause).matches()) {
          queryBuilder.append("WHERE ");
        }
        queryBuilder.append(whereClause);
      }
      if (queryForUpdate && !forUpdateContainmentPattern.matcher(whereClause).matches()) {
        queryBuilder.append(" FOR UPDATE");
      }
      return queryBuilder.toString();
    }
    
    private ResultSetReader<? extends S> getReader(Class<S> clazz) {
      try {
        return clazz.getConstructor().newInstance().getReader(); //ugly! cache in Storable?
      } catch (Exception e) {
        throw new RuntimeException("Could not instantiate Storable " + clazz.getName() + ".", e);
      }
    }
    
    
    public void run() {
      try {
        initRun();
        for (ODSConnectionType conType : sourceConnectionTypes) {
          ODSConnection con = ods.openConnection(conType);
          try {
            FactoryWarehouseCursor<? extends S> cursor = 
              con.getCursor(sqlString, Parameter.EMPTY_PARAMETER, getReader(clazz), XynaProperty.WAREHOUSE_JOB_BATCH_SIZE.get());
            Collection<? extends S> batch = cursor.getRemainingCacheOrNextIfEmpty();
            while (!batch.isEmpty()) {
              executeForBatch(con, batch);
              batch = cursor.getRemainingCacheOrNextIfEmpty();
            }
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              logger.error(null, e);
            }
          }
        }
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    
    
    protected abstract void initRun() throws PersistenceLayerException;
    
    protected abstract void executeForBatch(ODSConnection con, Collection<? extends S> batch) throws PersistenceLayerException;
    
  }
  
  
  private static class WarehouseCopyOrReplaceRunnable<S extends Storable<S>> extends WarehouseBaseRunnable<S> {

    private final ODSConnectionType target;
    private final boolean replaceContent;
    
    protected WarehouseCopyOrReplaceRunnable(Class<S> clazz, ODSConnectionType source, ODSConnectionType target, boolean replace) throws PersistenceLayerException {
      this(clazz, source, target, replace, null, false);
    }
    
    protected WarehouseCopyOrReplaceRunnable(Class<S> clazz, ODSConnectionType source, ODSConnectionType target,
                                             boolean replace, String whereClause, boolean concurrencyProtection) throws PersistenceLayerException {
      super(clazz, Collections.singletonList(source), whereClause, concurrencyProtection);
      this.target = target;
      this.replaceContent = replace;
    }

    @Override
    protected void executeForBatch(ODSConnection con, Collection<? extends S> batch) throws PersistenceLayerException {
      ODSConnection targetCon = ods.openConnection(target);
      try {
        targetCon.persistCollection(batch);
        targetCon.commit();
      } finally {
        targetCon.closeConnection();
      }
    }

    @Override
    protected void initRun() throws PersistenceLayerException {
      if (replaceContent) {
        ODSConnection targetCon = ods.openConnection(target);
        try {
          targetCon.deleteAll(clazz);
          targetCon.commit();
        } finally {
          targetCon.closeConnection();
        }
      }
    }


  }
  
  
  private static class WarehouseDeleteRunnable<S extends Storable<S>> extends WarehouseBaseRunnable<S> {
    
    
    protected WarehouseDeleteRunnable(Class<S> clazz, List<ODSConnectionType> connectionTypes) throws PersistenceLayerException {
      this(clazz, connectionTypes, null, false);
    }
    
    protected WarehouseDeleteRunnable(Class<S> clazz, List<ODSConnectionType> connectionTypes, String whereClause, boolean concurrencyProtection) throws PersistenceLayerException {
      super(clazz, connectionTypes, whereClause, concurrencyProtection);
    }


    @Override
    protected void initRun() throws PersistenceLayerException {
    }

    @Override
    protected void executeForBatch(ODSConnection con, Collection<? extends S> batch) throws PersistenceLayerException {
      con.delete(batch);
      con.commit();
    }

  }

}
