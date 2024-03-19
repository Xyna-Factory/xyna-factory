/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xsched.cronlikescheduling;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.RemoteCronLikeOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderColumn;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.JodaTimeControlUnit;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;



@Persistable(primaryKey = CronLikeOrder.COL_ID, tableName = CronLikeOrder.TABLE_NAME)
public class CronLikeOrder extends ClusteredStorable<CronLikeOrder> {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(CronLikeOrder.class);

  
  private class CronLikeOrderPlanningRunnable extends XynaRunnable implements Serializable {

    private static final long serialVersionUID = -2449006465761527211L;

    CronLikeOrderPlanningRunnable() {
      super( "CronLikeScheduler" );
    }

    public void run() {
      try {
        startUnderlyingOrderAlgorithm.startUnderlyingOrder(CronLikeOrder.this, getCreationParameters(),
                                                           defaultCountingResponseListener);
      } catch (Throwable t) {
        logger.error("Unexpected error during cron like order planning.", t);
      }
    }
    
    @Override
    public String toString() {
      return "CronLikeScheduler("+id+")";
    }

  }


  public static class CronLikeOrderReader implements ResultSetReader<CronLikeOrder> {

    public CronLikeOrder read(ResultSet rs) throws SQLException {
      CronLikeOrder order = new CronLikeOrder();
      fillByResultSet(order, rs);
      return order;
    }

  }
  
  public static class DynamicCronLikeOrderReader implements ResultSetReader<CronLikeOrder> {

    private Set<CronLikeOrderColumn> selectedCols;

    public DynamicCronLikeOrderReader(Set<CronLikeOrderColumn> selected) {
      selectedCols = selected;
    }

    public CronLikeOrder read(ResultSet rs) throws SQLException {
      CronLikeOrder order = new CronLikeOrder();
      order.id = rs.getLong(COL_ID);
      if (order.id == 0 && rs.wasNull()) {
        throw new SQLException("ID of a cron like order may not be null");
      }
      
      readCreationParametersPreferByteArray(rs, order);
      
      if(selectedCols.contains(CronLikeOrderColumn.STATUS)) {
        order.status = rs.getString(COL_STATUS);
      }
      if(selectedCols.contains(CronLikeOrderColumn.LABEL)) {
        order.label = rs.getString(COL_LABEL);
      }
      if(selectedCols.contains(CronLikeOrderColumn.ORDERTYPE)) {
        order.ordertype = rs.getString(COL_ORDERTYPE);
      }
      if(selectedCols.contains(CronLikeOrderColumn.STARTTIME)) {
        order.starttime = rs.getLong(COL_STARTTIME);
      }
      if(selectedCols.contains(CronLikeOrderColumn.NEXTEXECUTION)) {
        order.nextExecutionTime = rs.getLong(COL_NEXT_EXEC_TIME);
      }
      if(selectedCols.contains(CronLikeOrderColumn.INTERVAL)) {
        order.interval = rs.getLong(COL_INTERVAL);
      }
      if(selectedCols.contains(CronLikeOrderColumn.ONERROR)) {
        order.onerror = rs.getString(COL_ONERROR);
      }
      if(selectedCols.contains(CronLikeOrderColumn.APPLICATIONNAME)) {
        order.applicationname = rs.getString(COL_APPLICATIONNAME);
      }
      if(selectedCols.contains(CronLikeOrderColumn.VERSIONNAME)) {
        order.versionname = rs.getString(COL_VERSIONNAME);
      }
      if(selectedCols.contains(CronLikeOrderColumn.WORKSPACENAME)) {
        order.workspacename = rs.getString(COL_WORKSPACENAME);
      }
      if(selectedCols.contains(CronLikeOrderColumn.ENABLED)) {
        order.enabled = rs.getBoolean(COL_ENABLED);
      }
      if(selectedCols.contains(CronLikeOrderColumn.TIMEZONEID)) {
        order.timezoneid = rs.getString(COL_TIME_ZONE_ID);
      }
      if(selectedCols.contains(CronLikeOrderColumn.CONSIDERDAYLIGHTSAVING)) {
        order.considerdaylightsaving = rs.getBoolean(COL_CONSIDER_DAYLIGHT_SAVING);
      }
      if(selectedCols.contains(CronLikeOrderColumn.CUSTOM0)) {
        order.cronlikeordercustom0 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM0);
      }
      if(selectedCols.contains(CronLikeOrderColumn.CUSTOM1)) {
        order.cronlikeordercustom1 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM1);
      }
      if(selectedCols.contains(CronLikeOrderColumn.CUSTOM2)) {
        order.cronlikeordercustom2 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM2);
      }
      if(selectedCols.contains(CronLikeOrderColumn.CUSTOM3)) {
        order.cronlikeordercustom3 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM3);
      }
      if(selectedCols.contains(CronLikeOrderColumn.ERROR_MSG)) {
        order.errorMessage = rs.getString(COL_ERROR_MSG);
      }
      if(selectedCols.contains(CronLikeOrderColumn.ROOT_ORDER_ID)) {
        order.rootOrderId = getLong(rs,COL_ASSIGNED_ROOT_ORDER_ID);
      }
      if(selectedCols.contains(CronLikeOrderColumn.EXEC_COUNT)) {
        order.execCount = rs.getInt(COL_EXEC_COUNT);
      }
      if(selectedCols.contains(CronLikeOrderColumn.REMOVE_ON_SHUTDOWN)) {
        order.removeOnShutdown = rs.getBoolean(COL_REMOVE_ON_SHUTDOWN);
      }
      if(selectedCols.contains(CronLikeOrderColumn.REVISION)) {
        order.revision = getLong(rs, COL_REVISION);
      }
      
      return order;
    }

    private Long getLong(ResultSet rs, String colName) throws SQLException {
      Long l = rs.getLong(colName);
      if( rs.wasNull() ) {
        return null;
      }
      return l;
    }
  }
  
  private static volatile CronLikeOrderStartUnderlyingOrderAlgorithm startUnderlyingOrderAlgorithm = null;

  private final ResponseListener defaultCountingResponseListener = new ResponseListener() {
    //achtung: bei refactoring aufpassen: derzeit ist der responselistener das einzige erkennungsmerkmal, um in einem gespawnten
    //         auftrag um erkennen zu können, dass der auftrag ein cron-auftrag ist. das wurde zumindest von Projekten
    //         mal angefragt um es zu verwenden. idee: temporäre lösung -> aber nachfragen ist besser.

    private static final long serialVersionUID = -4378782170450674992L;


    @Override
    public void onError(XynaException[] e, OrderContext ctx) {
      if (e != null && e.length > 0) {
        handleError(e[0]); // TODO other errors?
      } else {
        handleError(null);
      }
    }


    @Override
    public void onResponse(GeneralXynaObject response, OrderContext ctx) {
      reportFinishedSingleExecution();
    }

  };


  public static final String TABLE_NAME = "cronlikeorders";
  public static final String COL_ID = "id";
  public static final String COL_NEXT_EXEC_TIME = "nextexecution";
  public static final String COL_CREATION_PARAMTER = "creationparameters";
  public static final String COL_STATUS = "status";
  public static final String COL_ERROR_MSG = "errormessage";
  public static final String COL_ASSIGNED_ROOT_ORDER_ID = "rootorderid";
  public static final String COL_EXEC_COUNT = "execcount";
  public static final String COL_REMOVE_ON_SHUTDOWN = "removeonshutdown";
  public static final String COL_REVISION = "revision";
  public static final String COL_ENABLED = "enabled";
  public static final String COL_LABEL = "label";
  public static final String COL_ORDERTYPE = "ordertype";
  public static final String COL_STARTTIME = "starttime";
  public static final String COL_INTERVAL = "executioninterval";
  public static final String COL_ONERROR = "onerror";
  public static final String COL_APPLICATIONNAME = "applicationname";
  public static final String COL_VERSIONNAME = "versionname";
  public static final String COL_WORKSPACENAME = "workspacename";
  public static final String COL_TIME_ZONE_ID = "timezoneid";
  public static final String COL_CONSIDER_DAYLIGHT_SAVING = "considerdaylightsaving";
  public static final String COL_TIME_WINDOW_DEFINITION = "timeWindowDefinition";
  public static final String COL_CRON_LIKE_ORDER_CUSTOM0 = "cronlikeordercustom0";
  public static final String COL_CRON_LIKE_ORDER_CUSTOM1 = "cronlikeordercustom1";
  public static final String COL_CRON_LIKE_ORDER_CUSTOM2 = "cronlikeordercustom2";
  public static final String COL_CRON_LIKE_ORDER_CUSTOM3 = "cronlikeordercustom3";
  
  //bei Crons interessiert nur der Zeitpunkt, an dem ein Zeitfenster aufgeht, daher die duration auf 1ms setzen
  public static final String DEFAULT_TIME_WINDOW_DURATION = "[" + JodaTimeControlUnit.MILLISECOND.getStringIdentifier() + "=1]";
  
  public enum OnErrorAction {
    DISABLE, DROP, IGNORE;
  }


  @Column(name = COL_ID)
  private Long id;

  @Column(name = COL_NEXT_EXEC_TIME)
  private Long nextExecutionTime = null;
  
  private transient byte[] creationParameterSerialized;

  @Column(name = COL_CREATION_PARAMTER, type = ColumnType.BLOBBED_JAVAOBJECT)
  private CronLikeOrderCreationParameter creationParameter = null;

  @Column(name = COL_STATUS)
  private String status;
  @Column(name = COL_ERROR_MSG)
  protected String errorMessage;

  /**
   * Count how often the cron like order has executed
   */
  @Column(name = COL_EXEC_COUNT)
  protected int execCount = 0;

  @Column(name = COL_REMOVE_ON_SHUTDOWN)
  private boolean removeOnShutdown = false;
  
  @Column(name = COL_REVISION)
  private Long revision; //das ist die revision, in der der cron "läuft". von hier aus muss ordertype und inputparas auflösbar sein
  
  @Column(name = COL_LABEL)
  private String label;
  
  @Column(name = COL_ORDERTYPE)
  private String ordertype;
  
  @Column(name = COL_STARTTIME)
  private Long starttime;
  
  @Column(name = COL_INTERVAL)
  private Long interval;
  
  @Column(name = COL_ONERROR)
  private String onerror;
  
  @Column(name = COL_APPLICATIONNAME)
  private String applicationname;
  
  @Column(name = COL_VERSIONNAME)
  private String versionname;

  @Column(name = COL_WORKSPACENAME)
  private String workspacename;
  
  @Column(name = COL_ENABLED)
  private Boolean enabled;
  
  @Column(name = COL_TIME_ZONE_ID)
  private String timezoneid;
  
  @Column(name = COL_CONSIDER_DAYLIGHT_SAVING)
  private Boolean considerdaylightsaving;
  
  @Column(name = COL_TIME_WINDOW_DEFINITION, size=4000)
  private TimeWindowDefinition timeWindowDefinition;
  
  @Column(name = COL_CRON_LIKE_ORDER_CUSTOM0)
  private String cronlikeordercustom0;
  
  @Column(name = COL_CRON_LIKE_ORDER_CUSTOM1)
  private String cronlikeordercustom1;

  @Column(name = COL_CRON_LIKE_ORDER_CUSTOM2)
  private String cronlikeordercustom2;
  
  @Column(name = COL_CRON_LIKE_ORDER_CUSTOM3)
  private String cronlikeordercustom3;
  

  
  /**
   * enthält die id des verursachenden auftrags. z.b. wenn ein cronauftrag aus einem wait-schritt in einem wf
   * resultiert, wird die id des wf-auftrags als root id vergeben.
   * 
   * wenn es keinen solchen auftrag gibt, hat dieses feld den wert <code>null</code>.
   * 
   * bei der migration ändern nur cronlikeorders ihr binding, wenn ihre rootorderid ungleich <code>null</code> ist.
   */
  @Column(name = COL_ASSIGNED_ROOT_ORDER_ID)
  private Long rootOrderId; 
  
  
  private static CronLikeOrderReader reader = new CronLikeOrderReader();


  protected static void fillByResultSet(CronLikeOrder order, ResultSet rs) throws SQLException {
    fillByResultSet(order, rs, false);
  }

  protected static void fillByResultSet(CronLikeOrder order, ResultSet rs, boolean catchBlobbedJavaDeserializationException) throws SQLException {
    ClusteredStorable.fillByResultSet(order, rs);
    order.id = rs.getLong(COL_ID);
    if (order.id == 0 && rs.wasNull()) {
      throw new SQLException("ID of a cron like order may not be null");
    }

    // this has to be done quite early since it is required for some serialization processes
    //siehe auch CronLikeOrderIgnoringSerialVersionUID
    order.revision = rs.getLong(COL_REVISION);
    if(order.revision == 0 && rs.wasNull()) {
      order.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }

    order.nextExecutionTime = rs.getLong(COL_NEXT_EXEC_TIME);
    if (order.nextExecutionTime == 0 && rs.wasNull()) {
      order.nextExecutionTime = null;
    }
    order.status = rs.getString(COL_STATUS);
    order.errorMessage = rs.getString(COL_ERROR_MSG);
    order.execCount = rs.getInt(COL_EXEC_COUNT);
    
    if (catchBlobbedJavaDeserializationException) {
      //es wird davon ausgegangen, dass das nicht beim serverstart aufgerufen wird, sondern nur vom deploymentmanagement
    try {
    order.creationParameter = 
        (CronLikeOrderCreationParameter) order.readBlobbedJavaObjectFromResultSet(rs, COL_CREATION_PARAMTER, String.valueOf(order.id) );
    } catch (SQLException e) {
      if (catchBlobbedJavaDeserializationException) {
        logger.warn("Failed to read cronOrderCreationParameter", e);
      } else {
        throw e;
      }
    } catch (RuntimeException e) {
      if (catchBlobbedJavaDeserializationException) {
        logger.warn("Failed to read cronOrderCreationParameter", e);
      } else {
        throw e;
      }
    } catch (Error e) {
      if (catchBlobbedJavaDeserializationException) {
        Department.handleThrowable(e);
        logger.warn("Failed to read cronOrderCreationParameter", e);
      } else {
        throw e;
      }
    }
    } else {
      readCreationParametersPreferByteArray(rs, order);
    }

    order.removeOnShutdown = rs.getBoolean(COL_REMOVE_ON_SHUTDOWN);
    order.rootOrderId = rs.getLong(COL_ASSIGNED_ROOT_ORDER_ID);
    if (order.rootOrderId == 0 && rs.wasNull()) {
      order.rootOrderId = null;
    }
    
    order.label = rs.getString(COL_LABEL);
    order.ordertype = rs.getString(COL_ORDERTYPE);
    order.starttime = rs.getLong(COL_STARTTIME);
    order.interval = rs.getLong(COL_INTERVAL);
    order.onerror = rs.getString(COL_ONERROR);
    order.enabled = rs.getBoolean(COL_ENABLED);
    if (rs.wasNull() || order.enabled == null) {
      order.enabled = false;
    }
    
    order.timezoneid = rs.getString(COL_TIME_ZONE_ID);
    if ( order.timezoneid == null || rs.wasNull()) {
      order.timezoneid = Constants.DEFAULT_TIMEZONE;
    }
    
    order.considerdaylightsaving = rs.getBoolean(COL_CONSIDER_DAYLIGHT_SAVING);
    if (rs.wasNull() || order.considerdaylightsaving == null) {
      order.considerdaylightsaving = false;
    }
    
    if (order.creationParameter != null) {
      fillValuesFromCreationParameter(order);
    }

    if ( order.considerdaylightsaving == null ) {
      order.considerdaylightsaving = false;
    }
    
    if (rs.getString(COL_TIME_WINDOW_DEFINITION) != null && rs.getString(COL_TIME_WINDOW_DEFINITION).length() > 0) {
      order.timeWindowDefinition = TimeWindowDefinition.valueOf(rs.getString(COL_TIME_WINDOW_DEFINITION));
    }
    
    order.applicationname = rs.getString(COL_APPLICATIONNAME);
    order.versionname = rs.getString(COL_VERSIONNAME);
    order.workspacename = rs.getString(COL_WORKSPACENAME);
    
    order.cronlikeordercustom0 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM0);
    order.cronlikeordercustom1 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM1);
    order.cronlikeordercustom2 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM2);
    order.cronlikeordercustom3 = rs.getString(COL_CRON_LIKE_ORDER_CUSTOM3);
  }


  private static void readCreationParametersPreferByteArray(ResultSet rs, CronLikeOrder order) throws SQLException {
    /*
     * falls das objekt deserialisiert werden muss und nicht gerade bereits als objekt vorliegt (memory-pl), preferieren wir das auslesen
     * als byte[]. grund: falls auf xml-pl konfiguriert, wird bereits beim registerstorable der mem-cache befüllt und dazu alle daten 
     * ausgelesen. zu dem zeitpunkt ist aber beim serverstart die workflowdb noch nicht initialisiert und deshalb kann man da die
     * deserialisierung noch nicht durchführen.
     */
    Object o = order.readBlobbedJavaObjectFromResultSet(rs, COL_CREATION_PARAMTER, String.valueOf(order.id), true);
    if (o != null) {
      if (o instanceof CronLikeOrderCreationParameter) {
        order.creationParameter = (CronLikeOrderCreationParameter) o;
      } else if (o instanceof byte[]) {
        order.creationParameterSerialized = (byte[]) o;
      }
    }

  }

  private static void fillValuesFromCreationParameter(CronLikeOrder order) {
    if (order.label == null) {
      order.label = order.creationParameter.getLabel();
    }
    if (order.ordertype == null) {
      order.ordertype = order.creationParameter.getDestinationKey().getOrderType();
    }
    if (order.starttime == 0) {
      order.starttime = order.creationParameter.getStartTime();
    }
    if (order.interval == 0) {
      order.interval = order.creationParameter.getInterval();
    }
    if (order.onerror == null) {
      order.onerror = order.creationParameter.getOnError().toString();
    }
    order.enabled = order.creationParameter.isEnabled();
    order.considerdaylightsaving = order.creationParameter.isConsiderDaylightSaving();
  }

  public static synchronized void setAlgorithm(CronLikeOrderStartUnderlyingOrderAlgorithm newAlgorithm) {
    startUnderlyingOrderAlgorithm = newAlgorithm;
  }
  
  public static synchronized CronLikeOrderStartUnderlyingOrderAlgorithm getAlgorithm() {
    return startUnderlyingOrderAlgorithm;
  }


  public CronLikeOrder() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  protected CronLikeOrder(CronLikeOrderCreationParameter clocp) {
    this(clocp, XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  protected CronLikeOrder(CronLikeOrderCreationParameter clocp, int binding) {

    super(binding);
    
    this.label = clocp.getLabel();
    this.ordertype = clocp.getDestinationKey().getOrderType();
    this.interval = clocp.getInterval();
    this.onerror = clocp.getOnError().toString();
    this.starttime = clocp.getStartTime();
    this.applicationname = clocp.getDestinationKey().getApplicationName();
    this.versionname = clocp.getDestinationKey().getVersionName();
    this.workspacename = clocp.getDestinationKey().getWorkspaceName();
    this.enabled = clocp.isEnabled();
    this.timezoneid = clocp.getOriginTimeZoneID();
    this.considerdaylightsaving = clocp.isConsiderDaylightSaving();
    this.cronlikeordercustom0 = clocp.getCronLikeOrderCustom0();
    this.cronlikeordercustom1 = clocp.getCronLikeOrderCustom1();
    this.cronlikeordercustom2 = clocp.getCronLikeOrderCustom2();
    this.cronlikeordercustom3 = clocp.getCronLikeOrderCustom3();
    this.status = CronLikeOrderStatus.WAITING_FOR_NEXT_EXECUTION;
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.creationParameter = clocp;
    this.rootOrderId = clocp.getRootOrderId();

    try {
      this.revision =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(clocp.getDestinationKey().getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("Can't find revision for runtimeContext "
          + clocp.getDestinationKey().getRuntimeContext(), e);
    }

    if (clocp.getStartTime() != null) {
      if (isSingleExecution()) {
        //Einmal-Crons werden mit der Startzeit in den CronLikeScheduler eingestellt
        nextExecutionTime = clocp.getStartTime();
      } else {
        //für die Berechnung der Ausführungszeitpunkte werden Zeitfenster verwendet, daher hier
        //die TimeWindowDefinition erstellen
        createTimeWindowDefinition();
        
        if (clocp.getStartTime() > System.currentTimeMillis()) {
          //Startzeit noch nicht erreicht
          nextExecutionTime = clocp.getStartTime();
        } else {
          long now;
          if (clocp.executeImmediately()) {
            //es soll für den (schon vergangenen) Startzeitpunkt auch ein Auftrag gestartet werden
            now = clocp.getStartTime()-1;
          } else {
            //der nächste Ausführungszeitpunkt soll ab dem aktuellen Zeitpunkt berechnet werden
            now = System.currentTimeMillis();
          }
          nextExecutionTime = calculateNextExecutionTime(now);
        }
      }
    } else {
      throw new IllegalArgumentException("Illegal start parameters for cron like order: Interval '"
          + clocp.getInterval() + "ms', start time '" + clocp.getStartTime() + "ms'");
    }
  }


  public CronLikeOrder(Long id) {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
    this.id = id;
  }


  public CronLikeOrder(Long id, int binding) {
    super(binding);
    this.id = id;
  }


  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CronLikeOrder)) {
      return false;
    }
    CronLikeOrder otherOrder = (CronLikeOrder) obj;
    return this.id.equals(otherOrder.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public String getErrorMessage() {
    return errorMessage;
  }


  public Long getId() {
    return id;
  }


  public String getLabel() {
    return label;
  }

  
  public void setLabel(String label) {
    this.label = label;
  }
  
  
  public Long getExecutionInterval() {
    return getInterval();
  }
  

  public Long getInterval() {
    CronLikeOrderCreationParameter cp = getCreationParameters();
    if (cp.getInterval() == null) {
      cp.setInterval(0L);
      interval = 0L;
    } else if (interval == null) {
      interval = cp.getInterval();
    }
    return interval;
  }

  /**
   * Liefert die TimeWindowDefinition. Falls sie noch nicht existiert,
   * wird sie neu angelegt
   * @return
   */
  public TimeWindowDefinition getTimeWindowDefinition() {
    if (timeWindowDefinition != null) {
      return timeWindowDefinition;
    }
    
    createTimeWindowDefinition();
    
    return timeWindowDefinition;
  }

  /**
   * Erzeugt eine TimeWindowDefinition aus interval bzw. calendarDefinition
   * sowie startTime, timeZone und considerdaylightsaving dieser CronLikeOrder.
   * Dabei wird eine bereits bestehende TimeWindowDefinition überschrieben.
   */
  private void createTimeWindowDefinition() {
    String rule = getCreationParameters().getCalendarDefinition();
    if (rule != null && rule.length() > 0) {
      String ruleAndDuration = rule;
      if (!rule.endsWith("]" + DEFAULT_TIME_WINDOW_DURATION)) {
        ruleAndDuration += DEFAULT_TIME_WINDOW_DURATION;
      }
      timeWindowDefinition = new RestrictionBasedTimeWindowDefinition(ruleAndDuration, timezoneid, considerdaylightsaving == null ? false : considerdaylightsaving);
      TimeWindow timeWindow = timeWindowDefinition.constructTimeWindow();
      timeWindow.recalculate(getStartTime());
    } else {
      timeWindowDefinition = null;
    }
  }
  

  /**
   * Liefert die calendarDefinition aus dem creationParameter.
   */
  public String getCalendarDefinition() {
    return getCreationParameters().getCalendarDefinition();
  }
  
  public Long getNextExecution() {
    return nextExecutionTime;
  }


  public int getExecCount() {
    return execCount;
  }
  
  public void incExecCount() {
    execCount++;
  }



  public CronLikeOrderCreationParameter getCreationParameters() {
    if (creationParameter == null) {
      if (creationParameterSerialized != null) {
        try {
          creationParameter = (CronLikeOrderCreationParameter) deserializeByColName(COL_CREATION_PARAMTER, new ByteArrayInputStream(creationParameterSerialized));
          creationParameterSerialized = null; //bei fehler später nochmal probieren (?)
        } catch (IOException e) {
          throw new RuntimeException("CronLikeOrderCreationParameters could not be deserialized", e);
        }
      }
    }
    return creationParameter;
  }


  public OnErrorAction getOnErrorAsEnum() {
    if(onerror == null) {
      return null;
    }
    return OnErrorAction.valueOf(onerror);
  }

  public String getOnerror() { //für memory-pl
    return onerror;
  }

  public Boolean getEnabled() {
    return enabled;
  }
  
  public Boolean isEnabled() {
    return enabled;
  }
  
  public String getTimeZoneID() {
    return timezoneid;
  }
  
  public Boolean getConsiderDaylightSaving() {
    return considerdaylightsaving;
  }

  public Boolean isSingleExecution() {
    return getInterval() == 0 && getTimeWindowDefinition() == null;
  }

  public boolean isExecutionTimeReached() {
    return getNextExecution() <= System.currentTimeMillis();
  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  @Override
  public ResultSetReader<? extends CronLikeOrder> getReader() {
    return reader;
  }

  public Long getStartTime() {
    CronLikeOrderCreationParameter cp = getCreationParameters();
    if (cp.getStartTime() == null) {
      cp.setStartTime(System.currentTimeMillis());
      starttime = cp.getStartTime();
    }
    return starttime;
  }


  public String getStatus() {
    return status;
  }


  private void handleError(Exception e) {
    setStatus(CronLikeOrderStatus.ERROR);
    if (e != null) {
      errorMessage = e.getMessage();
    } else {
      errorMessage = "unknown exception";
    }

    if( logger.isInfoEnabled()) {
      StringBuilder logMsg = new StringBuilder("Execution of ").
          append(getClass().getSimpleName()).
          append(" <").append(getId()).
          append("> lead to error (").append(errorMessage).append(")");

      boolean logAll = true;
      Throwable cause = e == null ? null : e.getCause();
      if( cause != null ) {
        String msg = cause.getMessage();
        if( DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.CLO_ALREADY_EXECUTED_OR_CHANGED.equals(msg) ) {
          logAll = false;
          if (logger.isDebugEnabled()) {
            logMsg.append(" caused by (").append(cause).append(")");
            logger.debug(logMsg.toString());
          }
        }
      }
      if (logAll) {
        logger.info(logMsg.toString(), e);
      }
    }

    if (!XynaFactory.getInstance().isShuttingDown() && !isSingleExecution()) {
      switch (getCreationParameters().getOnError()) {
        case DISABLE :
          try {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                .modifyCronLikeOrder(id, null, null, null, null, null, null, null, null, false, null, null, null, null, null);
          } catch (XPRC_CronLikeSchedulerException e2) {
            logger.error("Unable to disable cron like order after error.", e2);
          }
          break;
        case IGNORE :
          errorMessage = "";
          reportFinishedSingleExecution();
          break;
        case DROP :
          try {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                .removeCronLikeOrderWithRetryIfConnectionIsBroken(id);
          } catch (XPRC_CronRemovalException e1) {
            logger.warn("Failed to remove cron like order <" + id + "> after exception", e1);
          }
      }
    }

  }


  public boolean getRemoveOnShutdown() {
    return removeOnShutdown;
  }


  /**
   * Update a cron like order and store it in default connection
   * @throws PersistenceLayerException 
   * @throws XPRC_InvalidCronLikeOrderParametersException 
   */
  protected void update(String label, DestinationKey destination, GeneralXynaObject payload, Long firstStartupTime, String timeZoneID, Long interval, String calendarDefinition, Boolean useDST, Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3, ODSConnection con, boolean writeToDatabase)
      throws PersistenceLayerException, XPRC_InvalidCronLikeOrderParametersException {

    TimeZone tz = TimeZone.getTimeZone((timeZoneID == null) ? this.timezoneid : timeZoneID);
    boolean isDSTSupported = (tz.getDSTSavings() != 0);
    boolean isUsingDST = (useDST == null) ? this.considerdaylightsaving : useDST;
    long finalInterval = (interval == null) ? this.interval : interval;
    boolean isIntervalQualifyingForDST = CronLikeOrderCreationParameter.verifyIntervalQualifiesForDST(finalInterval);
    //für calendarDefinition findet die DST-Überprüfung erst beim Anlegen des Zeitfensters statt

    if (isUsingDST && !isDSTSupported) {
      throw new XPRC_InvalidCronLikeOrderParametersException("Failed to modify Cron Like Order because of inconsistent state. Considering DST is only supported for time zones that actually use it.");
    }
    
    if (isUsingDST && !isIntervalQualifyingForDST) {
      throw new XPRC_InvalidCronLikeOrderParametersException("Failed to modify Cron Like Order because of inconsistent state. Considering DST is only supported for time intervals that are multiple of whole days (24h).");
    }

    boolean timeWindowChanged = false;
    
    getCreationParameters();
    // modify cron like order
    if (label != null) {
      this.label = label;
      creationParameter.setLabel(label);
    }
    if (destination != null && destination.getOrderType().length() > 0) {
      this.ordertype = destination.getOrderType();
      //der RuntimeContext kann bei modifycron nicht geändert werden, hierzu muss copycronlikeorders verwendet werden
      destination.setRuntimeContext(this.getRuntimeContext());
      creationParameter.setDestinationKey(destination);
    }
    if (payload != null) { //ACHTUNG: payload null-en geht damit nicht. es muss in diesem fall ein leerer container übergeben werden.
      creationParameter.setInputPayload(payload);
      if (creationParameter instanceof RemoteCronLikeOrderCreationParameter) {
        //setinputpayload hat nur xml gesetzt
        creationParameter.setInputPayloadDirectly(payload);
      }
    }
    if (interval != null) {
      this.interval = interval;
      creationParameter.setInterval(interval);
      timeWindowChanged = true;
    }
    
    if (calendarDefinition != null) {
      creationParameter.setCalendarDefinition(calendarDefinition);
      this.interval = 0L;
      creationParameter.setInterval(0L);
      timeWindowChanged = true;
    }
    
    if (firstStartupTime != null) {
      creationParameter.setStartTime(firstStartupTime);
      this.starttime = creationParameter.getStartTime();
      timeWindowChanged = true;
    }
    
    if (enabled != null) {
      this.enabled = enabled;
      creationParameter.setEnabled(enabled);
    }
    
    if (timeZoneID != null) {
      this.timezoneid = timeZoneID;
      creationParameter.setOriginTimeZoneID(timeZoneID);
      timeWindowChanged = true;
    }
    
    if (useDST != null) {
      this.considerdaylightsaving = useDST;
      creationParameter.setConsiderDaylightSaving(useDST);
      timeWindowChanged = true;
    }
    
    if (onError != null) {
      this.onerror = onError.toString();
      creationParameter.setOnError(onError);
    }
    
    if (cloCustom0 != null) {
      this.cronlikeordercustom0 = cloCustom0;
      creationParameter.setCronLikeOrderCustom0(cloCustom0);
    }
    
    if (cloCustom1 != null) {
      this.cronlikeordercustom1 = cloCustom1;
      creationParameter.setCronLikeOrderCustom1(cloCustom1);
    }
    
    if (cloCustom2 != null) {
      this.cronlikeordercustom2 = cloCustom2;
      creationParameter.setCronLikeOrderCustom2(cloCustom2);
    }
    
    if (cloCustom3 != null) {
      this.cronlikeordercustom3 = cloCustom3;
      creationParameter.setCronLikeOrderCustom3(cloCustom3);
    }
    
    //Zeitfenster neu berechnen
    if ((timeWindowChanged || enabled != null) && !isSingleExecution()) {
      if (timeWindowChanged) {
        createTimeWindowDefinition();
      }
      calculateNextFutureExecutionTime();
      
      if (getNextExecution() < getStartTime()) {
        setNextExecutionTime(getStartTime());
      }
    }
    
    if (writeToDatabase) {
      CronLikeOrderHelpers.store(this, con);
    }
  }
  
  
  /**
   * Berechnet den nächsten Ausführungszeitpunkt nach 'now'.
   * @param now
   * @return
   */
  protected long calculateNextExecutionTime(long now) {
    TimeWindow timeWindow = getTimeWindowDefinition().constructTimeWindow();
    timeWindow.recalculate(now);
    return timeWindow.getNextOpen();
  }

  /**
   * Setzt die nextExecutionTime auf den nächsten Ausführungszeitpunkt
   * ab der aktuellen Zeit.
   */
  protected void calculateNextFutureExecutionTime() {
    setNextExecutionTime(calculateNextExecutionTime(System.currentTimeMillis()));
  }


  /**
   * Execution of an underlying order has finished.
   */
  // TODO needs to be synchronized for very short intervals?
  protected void reportFinishedSingleExecution() {

    if (logger.isDebugEnabled()) {
      logger.debug("Cron like order " + getId() + " execution finished (execution no " + execCount + ")");
    }

    if (!status.equals(CronLikeOrderStatus.RUNNING_SINGLE_EXECUTION)) {
      setStatus(CronLikeOrderStatus.WAITING_FOR_NEXT_EXECUTION);
    }

  }


  @Override
  public <U extends CronLikeOrder> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    CronLikeOrder cast = data;
    this.id = cast.id;
    this.nextExecutionTime = cast.nextExecutionTime;
    this.status = cast.status;
    this.errorMessage = cast.errorMessage;
    this.execCount = cast.getExecCount();
    this.creationParameter = cast.getCreationParameters();
    this.removeOnShutdown = cast.removeOnShutdown;
    this.rootOrderId = cast.rootOrderId;
    this.revision = cast.revision;
    if(this.revision == null) {
      this.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
    this.starttime = cast.starttime;
    this.interval = cast.interval;
    this.label = cast.label;
    this.onerror = cast.onerror;
    this.ordertype = cast.ordertype;
    this.applicationname = cast.applicationname;
    this.versionname = cast.versionname;
    this.workspacename = cast.workspacename;
    this.enabled = cast.enabled;
    this.timezoneid = cast.timezoneid;
    this.considerdaylightsaving = cast.considerdaylightsaving;
    this.timeWindowDefinition = cast.timeWindowDefinition;
    this.cronlikeordercustom0 = cast.cronlikeordercustom0;
    this.cronlikeordercustom1 = cast.cronlikeordercustom1;
    this.cronlikeordercustom2 = cast.cronlikeordercustom2;
    this.cronlikeordercustom3 = cast.cronlikeordercustom3;
  }


  public void setFirstStartTime(Long nextStartTime) {

    if (nextStartTime == null) {
      this.getCreationParameters().setStartTime(nextStartTime);
      this.starttime = nextStartTime;
      return;
    }

    if (nextStartTime < System.currentTimeMillis()) {
      logger.warn("Could not set first start time for cron like order " + getId()
          + ": Specified date lies in the past.");
    } else {
      setStatus(CronLikeOrderStatus.WAITING_FOR_NEXT_EXECUTION);
      this.getCreationParameters().setStartTime(nextStartTime);
      this.starttime = nextStartTime;
      if (logger.isDebugEnabled()) {
        StringBuilder sb =
            new StringBuilder("cron like order (ID ").append(getId()).append(") rescheduled for first execution time ")
                .append(Constants.defaultUTCSimpleDateFormat().format(new Date(nextStartTime)));
        logger.debug(sb.toString());
      }

    }

  }


  public void setNextExecutionTime(Long nextExecutionTime) {
    this.nextExecutionTime = nextExecutionTime;
  }


  public void setRemoveOnShutdown(boolean removeOnShutdown) {
    this.removeOnShutdown = removeOnShutdown;
  }


  protected void setStatus(String s) {
    status = s;
  }

  public Long getRootOrderId() {
    return rootOrderId;
  }

  
  public void setRootOrderId(Long rootOrderId) {
    this.rootOrderId = rootOrderId;
  }


  protected XynaRunnable getPlanningRunnable() {
    return new CronLikeOrderPlanningRunnable();
  }

  
  public Long getRevision() {
    return revision;
  }


  public void setRevision(Long revision) {
    this.revision = revision;
  }

  
  public String getOrdertype() {
    return ordertype;
  }

  
  public void setOrdertype(String ordertype) {
    this.ordertype = ordertype;
  }
  
  public String getApplicationname() {
    return applicationname;
  }

  
  public String getVersionname() {
    return versionname;
  }

  public String getWorkspacename() {
    return workspacename;
  }
  
  public RuntimeContext getRuntimeContext() {
    return RevisionManagement.getRuntimeContext(applicationname, versionname, workspacename);
  }
  
  public String getCronLikeOrderCustom0() {
    return cronlikeordercustom0;
  }
  
  public String getCronLikeOrderCustom1() {
    return cronlikeordercustom1;
  }
  
  public String getCronLikeOrderCustom2() {
    return cronlikeordercustom2;
  }
  
  public String getCronLikeOrderCustom3() {
    return cronlikeordercustom3;
  }
  
  
  public static class CronLikeOrderFailSafeReader extends CronLikeOrderReader {
    
    private List<Long> failedIds = new ArrayList<Long>();
    
    public List<Long> getFailedIds() {
      return failedIds;
    }
    
    public CronLikeOrder read(ResultSet rs) throws SQLException {
      CronLikeOrder clo = null;
      
      try {
        clo = super.read(rs);
      } catch(SQLException e) {
        // es werden alle SQLException gefangen, um einen Serverstart gewährleisten zukönnen (#14279). 
        Long id = rs.getLong(COL_ID);
        failedIds.add(id);
        clo = new CronLikeOrder(id);
        logger.warn("Could not load cron like order with id = " + id, e);        
      }
      
      return clo;
    }
  }
  
  public static ResultSetReader<CronLikeOrder> getFailSafeReader() {
    return new CronLikeOrderFailSafeReader();
  }
  
  
  static void makeReaderFailSafe( boolean failSafe ) {
    if ( failSafe ) {
      reader = new CronLikeOrderFailSafeReader();
    } else {
      reader = new CronLikeOrderReader();
    }
  }

  
  public void setId(long id) {
    this.id = id;
  }

  public void setVersionName(String versionName) {
    this.versionname = versionName;
  }
  
  public void setApplicationname(String applicationname) {
    this.applicationname = applicationname;
  }
  
  public void setWorkspacename(String workspacename) {
    this.workspacename = workspacename;
  }
  
  
  /**
   * @param cloc
   * @return
   */
  public Object get(CronLikeOrderColumn cloc) {
    switch( cloc ) {
      case ID:
        return id;
      case LABEL:
        return label;
      case ORDERTYPE:
        return ordertype;
      case STARTTIME:
        return starttime;
      case NEXTEXECUTION:
        return nextExecutionTime;
      case INTERVAL:
        return interval;
      case STATUS:
        return status;
      case ONERROR:
        return onerror;
      case APPLICATIONNAME:
        return applicationname;
      case VERSIONNAME:
        return versionname;
      case WORKSPACENAME:
        return workspacename;
      case ENABLED:
        return enabled;
      case CREATIONPARAMETER:
        return getCreationParameters();
      case TIMEZONEID:
        return timezoneid;
      case CONSIDERDAYLIGHTSAVING:
        return considerdaylightsaving;
      case CUSTOM0:
        return cronlikeordercustom0;
      case CUSTOM1:
        return cronlikeordercustom1;
      case CUSTOM2:
        return cronlikeordercustom2;
      case CUSTOM3:
        return cronlikeordercustom3;
      case ERROR_MSG:
        return errorMessage;
      case ROOT_ORDER_ID:
        return rootOrderId;
      case EXEC_COUNT:
        return execCount;
      case REMOVE_ON_SHUTDOWN:
        return removeOnShutdown;
      case REVISION:
        return revision;
      default:
        throw new RuntimeException("Unexpected CronLikeOrderColumn "+cloc);
    }
  }

}
