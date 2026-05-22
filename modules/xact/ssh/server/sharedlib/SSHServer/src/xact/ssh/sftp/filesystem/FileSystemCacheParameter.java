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
package xact.ssh.sftp.filesystem;



import com.gip.xyna.utils.timing.Duration;



public abstract class FileSystemCacheParameter {

  private final CacheType type;


  FileSystemCacheParameter(CacheType type) {
    this.type = type;
  }


  public CacheType getType() {
    return type;
  }


  public static enum CacheType {
    NONE, TIMED, SESSION_ISOLATION;
  }

  public static class SimpleCacheParameter extends FileSystemCacheParameter {

    SimpleCacheParameter(CacheType type) {
      super(type);
    }

  }


  public static class TimeBasedCacheParameter extends FileSystemCacheParameter {

    private final Duration duration;


    TimeBasedCacheParameter(Duration duration) {
      super(CacheType.TIMED);
      this.duration = duration;
    }


    public Duration getDuration() {
      return duration;
    }
  }


  public static FileSystemCacheParameter noCaching() {
    return new SimpleCacheParameter(CacheType.NONE);
  }


  public static FileSystemCacheParameter sessionIsolated() {
    return new SimpleCacheParameter(CacheType.SESSION_ISOLATION);
  }


  public static FileSystemCacheParameter timed(Duration duration) {
    return new TimeBasedCacheParameter(duration);
  }

}