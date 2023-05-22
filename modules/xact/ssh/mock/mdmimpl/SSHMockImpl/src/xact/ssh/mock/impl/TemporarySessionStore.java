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
package xact.ssh.mock.impl;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.maps.TimeoutMap;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;

public class TemporarySessionStore {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(TemporarySessionStore.class);

  private TimeoutMap<String, MockData> map;
 
  public TemporarySessionStore() {
    map = new TimeoutMap<String, MockData>();
  }
  
  public void clear() {
    map.clear();
  }

  public void remove(String key) {
    map.remove(key);
  }

  public void store(String key, AbsRelTime duration, MockData md) {
    long millis = duration.getRelativeTime(TimeUnit.MILLISECONDS);
    logger.info(" store "+key+" for "+millis+" ms");
    map.replace(key, md, millis );
  }

  public MockData get(String key) {
    logger.info( "Stored mock data: "+ map.listAllKeysOrdered(System.currentTimeMillis()));
    MockData md = map.get(key);
    logger.info( "found " + key+" -> "+md);
    return md;
  }

}
