/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xprc;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;



public abstract class ExecutionTimeoutConfiguration implements Serializable {

  private static final long serialVersionUID = 8386727393226208852L;

  protected long time;
  protected TimeUnit unit;
  protected long executionStartInMillis;


  public abstract long getRelativeTimeoutForNowIn(TimeUnit unit);


  public static class ExecutionTimeoutConfigurationAbsolute extends ExecutionTimeoutConfiguration {

    private static final long serialVersionUID = -4659322340787210016L;


    public ExecutionTimeoutConfigurationAbsolute(long time, TimeUnit unit) {
      this.time = time;
      this.unit = unit;
    }


    @Override
    public long getRelativeTimeoutForNowIn(TimeUnit unit) {
      long now = System.currentTimeMillis();
      // "unit == MILLISECONDS" does not really cost anything due to the implementation of "convert"
      long absoluteInMillis = TimeUnit.MILLISECONDS.convert(this.time, this.unit);
      return unit.convert(absoluteInMillis - now, TimeUnit.MILLISECONDS);
    }

  }


  public static class ExecutionTimeoutConfigurationRelative extends ExecutionTimeoutConfiguration {

    private static final long serialVersionUID = 5419519163704739072L;


    public ExecutionTimeoutConfigurationRelative(long time, TimeUnit unit) {
      this.time = time;
      this.unit = unit;
    }


    @Override
    public long getRelativeTimeoutForNowIn(TimeUnit unit) {
      // "unit == this.unit" does not really cost anything due to the implementation of "convert"
      return unit.convert(this.time, this.unit);
    }

  }


  public static ExecutionTimeoutConfigurationRelative generateRelativeExecutionTimeout(long time, TimeUnit unit) {
    return new ExecutionTimeoutConfigurationRelative(time, unit);
  }


  public static ExecutionTimeoutConfigurationAbsolute generateAbsoluteExecutionTimeout(long time, TimeUnit unit) {
    return new ExecutionTimeoutConfigurationAbsolute(time, unit);
  }

}
