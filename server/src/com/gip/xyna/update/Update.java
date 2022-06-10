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

package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;


public abstract class Update {

  protected static final Logger logger = CentralFactoryLogging.getLogger(Update.class);
  
  private List<AllowedVersionRange> allowedVersions;
  
  //wann soll das update ausgeführt werden
  protected ExecutionTime executionTime = ExecutionTime.initialUpdate;
  
  //wann wird das update gerade ausgeführt?
  private ExecutionTime currentExecutionTime;

  public static enum ExecutionTime {
    initialUpdate, endOfUpdate, endOfUpdateAndEndOfFactoryStart, endOfFactoryStart, afterUpdateGeneratedClassesBeforeRewriteOrderBackup;

    public boolean mustExecuteNow(ExecutionTime now) {
      return now == this || (this == endOfUpdateAndEndOfFactoryStart && (now == endOfUpdate || now == endOfFactoryStart));
    }

    public boolean mustMockFactory() {
      return this != endOfFactoryStart;
    }
  }

  /**
   * gibt neue version zurück falls update durchgeführt werden soll und es das auch wurde. ansonsten
   * wird currentVersion zurückgegeben. 
   */
  public Version update(Version currentVersion) throws XynaException {
    return update(currentVersion, ExecutionTime.initialUpdate);
  }


  public Version update(Version currentVersion, ExecutionTime executionTime) throws XynaException {
    if (isAllowedVersion(currentVersion)) {
      if (logger.isDebugEnabled()) {
        logger.debug(getClass().getSimpleName() + ": updating from version " + currentVersion.getString() + " (executionTime: "
            + executionTime + ")");
      }
      if (this.executionTime.mustExecuteNow(executionTime)) {
        currentExecutionTime = executionTime;
        update();
      } else if (logger.isDebugEnabled()) {
        logger.debug("skipping update execution because of not matching executionTime");
      }
      Version v = getVersionAfterUpdate();
      if (logger.isInfoEnabled()) {
        logger.info("updated to " + executionTime + "-version " + v.getString());
      }
      return v;
    } else {
      if (logger.isDebugEnabled() && this.executionTime.mustExecuteNow(executionTime)) {
        logger.debug(executionTime + ": skipping update " + getClass().getSimpleName() + " from version " + currentVersion.getString()
            + " because it demands version " + getAllowedVersionForUpdate().getString());
      }
    }
    return currentVersion;
  }


  protected abstract void update() throws XynaException;


  protected abstract Version getAllowedVersionForUpdate();


  protected abstract Version getVersionAfterUpdate() throws XynaException;


  public abstract boolean mustUpdateGeneratedClasses();

  private boolean isAllowedVersion(Version currentVersion) {
    if (currentVersion.equals(getAllowedVersionForUpdate())) {
      return true;
    }
    if (allowedVersions != null && allowedVersions.size() > 0) {
      for (AllowedVersionRange range : allowedVersions) {
        if (isInRange(range, currentVersion)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isInRange(AllowedVersionRange range, Version currentVersion) {
    return currentVersion.isEqualOrGreaterThan(range.start) && range.end.isEqualOrGreaterThan(currentVersion);
  }


  /**
   * falls &lt;= 4, werden beim ausführen der update methode alle versionen, die in ihren 
   * ersten (index-1) stellen übereinstimmen und insgesamt größergleich getAllowedVersionForUpdate sind,
   * als passend erkannt und das update wird durchgeführt.
   * 
   * z.B. index = 4 und getAllowedVersionForUpdate = 3.2.5.7
   * =&gt; das update wird angewandt, falls currentVersion = 3.2.5.7, oder 3.2.5.8 oder 3.2.5.10, oder 3.2.5.111 ist,
   * aber nicht, falls currentVersion = 3.2.5.6 oder niedriger, und nicht falls currentVersion = 3.2.6.x.
   */
  public void addFollowingBranchVersionsAsAllowedForUpdate(int index) {
    Version start = getAllowedVersionForUpdate();
    Version end = new Version(start);
    for (int i = index; i <= end.length(); i++) {
      end.increase(index, 10000000);
    }
    addAllowedVersionRangeForUpdate(start, end);
  }
  
  /**
   * erlaubt die Angabe zusätzlicher Versionen, auf die das Update ausgeführt werden kann. Dabei sind alle
   * Versionen enthalten, die sich zwischen den beiden Versionen befinden, inkl der angegebenen Versionen.
   * 
   *  z.B. startVersion = 3.1.5.2, endVersion = 3.3.1.4
   *  =&gt; alle Versionen 3.1.5.x mit x&gt;=2, 3.1.x.* mit x&gt;5, 3.2.*.*, 3.3.0.*, 3.3.1.x mit x&lt;=4.
   */
  public void addAllowedVersionRangeForUpdate(Version startVersion, Version endVersion) {
    if (allowedVersions == null) {
      allowedVersions = new ArrayList<AllowedVersionRange>();
    }
    AllowedVersionRange range = new AllowedVersionRange();
    range.start = startVersion;
    range.end = endVersion;
    allowedVersions.add(range);
  }
  
  private static class AllowedVersionRange {
    private Version start;
    private Version end;
  }

  public boolean mustRewriteWorkflows() {
    return false;
  }


  public boolean mustRewriteDatatypes() {
    return false;
  }


  public boolean mustRewriteExceptions() {
    return false;
  }
  
  
  public ExecutionTime getExecutionTime() {
    return executionTime;
  }
  
  public void setExecutionTime(ExecutionTime executionTime) {
    this.executionTime = executionTime;
  }
  
  protected ExecutionTime getCurrentExecutionTime() {
    return currentExecutionTime;
  }
}
