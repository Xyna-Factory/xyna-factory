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
package xact.ssh.sftp.filesystem.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.timing.Duration;
import xact.ssh.sftp.filesystem.XynaFilterDelegatingPath;

public class TimedCacheEntry<T> extends CacheEntry<T> {

    private class TimeoutCallable implements Callable<Void> {

        public Void call() throws Exception {
            clear();
            return null;
        }

    }

    private Duration timeout;
    private ScheduledExecutorService ses;
    private ScheduledFuture<Void> cleaner;

    public TimedCacheEntry(TimedCacheEntry other) {
        super(other);
        this.timeout = other.timeout;
        this.ses = other.ses;
        this.cleaner = other.cleaner;
    }

    public TimedCacheEntry(CacheEntry<T> other, Duration timeout, ScheduledExecutorService ses) {
        super(other);

        init(timeout, ses);
    }

    public TimedCacheEntry(CacheKey key, T file, Duration timeout, ScheduledExecutorService ses) {
        super(key, file);

        init(timeout, ses);
    }

    public TimedCacheEntry(String path, T file, Duration timeout, ScheduledExecutorService ses) {
        super(path, file);

        init(timeout, ses);
    }

    public TimedCacheEntry(XynaFilterDelegatingPath path, T file, Duration timeout, ScheduledExecutorService ses) {
        super(path, file);

        init(timeout, ses);
    }

    private void init(Duration timeout, ScheduledExecutorService ses) {
        this.timeout = timeout;
        this.ses = ses;

        registerTimeout();
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public void clear() {
        cancel();
        super.clear();
    }

    @Override
    public void updateAccessTime() {
        super.updateAccessTime();
        refreshTimeout();
    }

    private void registerTimeout() {
        cleaner = ses.schedule(new TimeoutCallable(), timeout.getDurationInMillis(), TimeUnit.MILLISECONDS);
    }

    private void refreshTimeout() {
        if (cancel())
            registerTimeout();
    }

    public boolean cancel() {
        return cleaner.cancel(true);
    }
}
