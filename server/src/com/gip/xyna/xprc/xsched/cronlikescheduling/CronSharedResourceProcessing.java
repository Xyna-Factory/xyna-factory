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
package com.gip.xyna.xprc.xsched.cronlikescheduling;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.ShareResourceEntryManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWork;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWorkManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceWorkManagementThread.SharedResourceWorkManagementThreadConfig;



/** Keeps track of which cronLikeOrders belong to us.
*   this prevents multiple factories from trying to execute the same cronLikeOrder.<br><br>
*   
*   The ourCrons-member contains the IDs of all cronLikeOrders we are responsible for.
*   When a conLikeOrder is created by this factory, it is added to ourCrons.<br><br>
*   
*   When it is our turn to work, we check for cronLikeOrders that are not associated with
*   any factory. In that case, we add that cronLikeOrder to ourIds and update our
*   shared resource management entry.
*/
public class CronSharedResourceProcessing {

  public static final SharedResourceDefinition<SharedResourceCrons> XYNA_CRON_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(CronLikeScheduler.XYNA_CRONLIKE_SR, SharedResourceCrons.class, ArrayList.class);

  private static final Logger logger = CentralFactoryLogging.getLogger(CronSharedResourceProcessing.class);

  private final Set<Long> ourCrons;
  private final SharedResourceManagement srm;
  private SharedResourceWorkManagementThread<SharedResourceDefinition<SharedResourceCrons>, SharedResourceCrons> processingThread;


  public CronSharedResourceProcessing() {
    ourCrons = new HashSet<>();
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  public void addCronResponsibility(Long id) {
    ourCrons.add(id); // always add it to our cronLikeOrders, even if we do not have shared resource connectivity
    if (processingThread.getOurId() == -1) {
      return;
    }
    srm.update(XYNA_CRON_SR_DEF, List.of(String.valueOf(processingThread.getOurId())), x -> {
      x.getValue().getCronIds().add(id);
      if (logger.isDebugEnabled()) {
        logger.debug("Added " + id + " to our cron ids. Our cron ids are now " + x.getValue().getCronIds());
      }
      return new SharedResourceInstance<>(String.valueOf(processingThread.getOurId()), System.currentTimeMillis(), x.getValue());
    });
  }


  public void removeCronResponsibility(Long id) {
    ourCrons.remove(id);
    if (processingThread.getOurId() == -1) {
      return;
    }
    srm.update(XYNA_CRON_SR_DEF, List.of(String.valueOf(processingThread.getOurId())), x -> {
      x.getValue().getCronIds().remove(id);
      if (logger.isDebugEnabled()) {
        logger.debug("Removed " + id + " to our cron ids. Our cron ids are now " + x.getValue().getCronIds());
      }
      return new SharedResourceInstance<>(String.valueOf(processingThread.getOurId()), System.currentTimeMillis(), x.getValue());
    });
  }


  public boolean isResponsibleFor(Long id) {
    return ourCrons.contains(id);
  }


  public void start() {
    ourCrons.clear();
    SharedResourceWorkManagementThreadConfig<SharedResourceDefinition<SharedResourceCrons>, SharedResourceCrons> config;
    ShareResourceEntryManagement<SharedResourceCrons> idAccess = new CronEntryMgmt();
    SharedResourceWorkManagement workMgmt = new CronWorkManagement(ourCrons);
    config = new SharedResourceWorkManagementThreadConfig<>("CronSharedResourceProcessing", XYNA_CRON_SR_DEF, idAccess, workMgmt, 5000);
    processingThread = new SharedResourceWorkManagementThread<>(config);
    processingThread.start();
  }


  public void stop() {
    ourCrons.clear();
    processingThread.end();
    processingThread.interrupt();
    try {
      processingThread.join();
    } catch (InterruptedException e) {
    }
    processingThread.removeOurEntry();
  }


  private static class CronEntryMgmt implements ShareResourceEntryManagement<SharedResourceCrons> {

    @Override
    public long readId(SharedResourceCrons nextEntry) {
      return nextEntry.cronIds.get(0);
    }


    @Override
    public SharedResourceCrons createNextIdEntry(long value) {
      return new SharedResourceCrons(value);
    }


    @Override
    public SharedResourceCrons createNewEntry() {
      return new SharedResourceCrons();
    }


    @Override
    public void updateNextIdEntry(SharedResourceCrons nextIdEntry, long value) {
      nextIdEntry.cronIds.clear();
      nextIdEntry.cronIds.add(value);
    }


  }

  private static class CronWorkManagement implements SharedResourceWorkManagement {

    private static final String sql = "SELECT " + CronLikeOrder.COL_ID + " FROM " + CronLikeOrder.TABLE_NAME;
    private static final ResultSetReader<? extends Long> idReader = new ResultSetReader<Long>() {

      @Override
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(CronLikeOrder.COL_ID);
      }
    };


    private final Set<Long> ourIds;
    private final PreparedQueryCache cache;


    public CronWorkManagement(Set<Long> ourIds) {
      this.ourIds = ourIds;
      cache = new PreparedQueryCache();
    }


    @Override
    public List<SharedResourceWork> queryWork(long ourId) {
      List<SharedResourceWork> result = new ArrayList<>();
      List<? extends Long> candidateCrons = queryAllCronIds();
      List<Long> idsWithSharedResourceEntry = querySharedResourceCronIds();
      if (idsWithSharedResourceEntry == null) {
        return result;
      }
      candidateCrons.removeIf(x -> idsWithSharedResourceEntry.contains(x));

      if (!candidateCrons.isEmpty()) {
        result.add(new AssociateCronsWork(ourId, new ArrayList<>(candidateCrons), ourIds));
      }

      return result;
    }


    private List<Long> querySharedResourceCronIds() {
      SharedResourceManagement srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
      SharedResourceRequestResult<SharedResourceCrons> readResult = srm.readAll(XYNA_CRON_SR_DEF);
      if (!readResult.isSuccess() || readResult.getResources() == null) {
        return null;
      }
      List<Long> result = new ArrayList<>();
      for (SharedResourceInstance<SharedResourceCrons> entry : readResult.getResources()) {
        if (Objects.equals(SharedResourceWorkManagementThread.NEXT_ID_KEY, entry.getId())) {
          continue;
        }
        result.addAll(entry.getValue().getCronIds());
      }
      return result;
    }


    private List<? extends Long> queryAllCronIds() {
      ODS ods = ODSImpl.getInstance();
      ODSConnection con = null;
      try {
        con = ods.openConnection();

        PreparedQuery<? extends Long> query = cache.getQueryFromCache(sql, con, idReader, CronLikeOrder.TABLE_NAME);
        return con.query(query, Parameter.EMPTY_PARAMETER, -1);
      } catch (PersistenceLayerException e) {
      } finally {
        if (con != null) {
          try {
            con.closeConnection();
          } catch (PersistenceLayerException e) {
          }
        }
      }
      return Collections.emptyList();
    }
  }

  private static class AssociateCronsWork implements SharedResourceWork {

    private final Long ourId;
    private final List<Long> ids;
    private final Set<Long> ourCrons;


    public AssociateCronsWork(Long ourId, List<Long> ids, Set<Long> ourCrons) {
      this.ourId = ourId;
      this.ids = ids;
      this.ourCrons = ourCrons;
    }


    @Override
    public void execute() {
      SharedResourceManagement srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
      long now = System.currentTimeMillis();
      SharedResourceRequestResult<SharedResourceCrons> success = srm.update(XYNA_CRON_SR_DEF, List.of(String.valueOf(ourId)), x -> {
        x.getValue().cronIds.addAll(ids);
        if (logger.isDebugEnabled()) {
          logger.debug("Added " + ids + " to our cron ids. Our cron ids are now " + x.getValue().getCronIds());
        }
        return new SharedResourceInstance<>(x.getId(), now, x.getValue());
      });
      if (!success.isSuccess()) {
        if (logger.isWarnEnabled()) {
          logger.warn("Failed to associate ids " + ids + " to us.");
        }
        return;
      }
      ourCrons.addAll(ids);
      if (logger.isDebugEnabled()) {
        logger.debug("updated ourCrons to " + ourCrons + ". Recreating queue.");
      }
      XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().recreateQueue();
    }
  }


  private static class SharedResourceCrons {

    private List<Long> cronIds;


    public SharedResourceCrons() {
      cronIds = new ArrayList<>();
    }


    public SharedResourceCrons(long id) {
      this();
      cronIds.add(id);
    }


    public List<Long> getCronIds() {
      return cronIds;
    }
  }

}
