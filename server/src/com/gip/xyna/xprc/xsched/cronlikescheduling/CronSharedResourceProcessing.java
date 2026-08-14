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



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;



// keeps track of which crons belong to us.
// this prevents multiple factories from trying to execute the same cron
public class CronSharedResourceProcessing {

  public static final SharedResourceDefinition<SharedResourceCrons> XYNA_CRON_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(CronLikeScheduler.XYNA_CRONLIKE_SR, SharedResourceCrons.class);

  private final Set<Long> ourCrons;
  private final SharedResourceManagement srm;

  private long ourId;


  public CronSharedResourceProcessing() {
    ourCrons = new HashSet<>();
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    ourId = -1l;
  }


  public void createCron(Long id) {
    //add id to the IDs we are responsible for
    ourCrons.add(id);
    if (ourId != -1) {
      srm.update(XYNA_CRON_SR_DEF, List.of(String.valueOf(ourId)), x -> {
        x.getValue().getCronIds().add(id);
        return new SharedResourceInstance<>(String.valueOf(ourId), System.currentTimeMillis(), x.getValue());
      });
    }
  }


  public void deleteCron(Long id) {
    //remove id from IDs we are responsible for
    //does nothing, if we are not responsible for this cron
    ourCrons.remove(id);
    if (ourId != -1) {
      srm.update(XYNA_CRON_SR_DEF, List.of(String.valueOf(ourId)), x -> {
        x.getValue().getCronIds().remove(id);
        return new SharedResourceInstance<>(String.valueOf(ourId), System.currentTimeMillis(), x.getValue());
      });
    }
  }


  public boolean checkCron(Long id) {
    //answers the question 'are we responsible for this id?'
    return ourCrons.contains(id);
  }


  public void start() {
    ourCrons.clear();
    //TODO: try to set ourId
    if (ourId != -1) {
      srm.create(XYNA_CRON_SR_DEF,
                 List.of(new SharedResourceInstance<>(String.valueOf(ourId), System.currentTimeMillis(), new SharedResourceCrons())));
    }
  }


  public void stop() {
    ourCrons.clear();
    if (ourId != -1) {
      srm.delete(XYNA_CRON_SR_DEF, List.of(String.valueOf(ourId)));
    }
  }


  private static class SharedResourceCrons {

    private List<Long> cronIds;


    public SharedResourceCrons() {
      cronIds = new ArrayList<>();
    }


    public List<Long> getCronIds() {
      return cronIds;
    }
  }


}
