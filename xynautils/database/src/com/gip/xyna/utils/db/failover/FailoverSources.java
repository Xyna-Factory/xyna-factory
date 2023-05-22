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
package com.gip.xyna.utils.db.failover;

import java.util.HashMap;

/**
 *
 */
public class FailoverSources {

  
  private FailoverSources() {/*only static usage*/}
  
  private static HashMap<String,FailoverSource> failovers = new HashMap<String,FailoverSource>();
  static {
    failovers.put("none", new FailoverSourceNone() );
    failovers.put("file", new FailoverSourceFile() );
  }
  
  /**
   * Gets a Failover
   * @param failoverSourceName
   * @return
   */
  public static FailoverSource getFailoverSource(String failoverSourceName, String failoverParam ) {
    if( failoverSourceName == null ) {
      throw new IllegalArgumentException("failoverSource is null");
    }
    FailoverSource failoverSource = failovers.get( failoverSourceName );
    if( failoverSource == null ) {
      throw new IllegalArgumentException("unknown failoverSource");
    }
    return failoverSource.newInstance(failoverParam);
  }

  /**
   * Adds a new failover
   * @param failoverSourceName
   * @param failoverSource
   */
  public static void addFailover(String failoverSourceName, FailoverSource failoverSource ) {
    if( failoverSourceName == null ) {
      throw new IllegalArgumentException("failoverSourceName is null");
    }
    if( failoverSource == null ) {
      throw new IllegalArgumentException("failoverSource is null");
    }
    if( failovers.containsKey(failoverSourceName)  ) {
      throw new IllegalArgumentException("failoverSource already exists");
    }
    failovers.put(failoverSourceName, failoverSource);
  }
  
  /**
   * Remove a failoverSource
   * @param failoverSource
   */
  public static void removeFailover(String failoverSourceName) {
    if( failoverSourceName == null ) {
      throw new IllegalArgumentException("failoverSourceName is null");
    }
    if( ! failovers.containsKey(failoverSourceName)  ) {
      throw new IllegalArgumentException("failoverSource does not exist");
    }
    failovers.remove(failoverSourceName);
  }
  
}
