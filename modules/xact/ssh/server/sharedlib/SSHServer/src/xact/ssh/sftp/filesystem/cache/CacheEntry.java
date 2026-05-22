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

import xact.ssh.sftp.filesystem.XynaFilterDelegatingPath;

public class CacheEntry<T> {

    private CacheKey key;
    private T file;

    private long accessTime;

    private FileCache cache;

    public CacheEntry(String path, T file) {
        this.file = file;
        this.key = new CacheKey(path);
        updateAccessTime();
    }

    public CacheEntry(XynaFilterDelegatingPath path, T file) {
        this.file = file;
        this.key = new CacheKey(path);
        updateAccessTime();
    }

    public CacheEntry(CacheKey key, T file) {
        this.file = file;
        this.key = key;
        updateAccessTime();
    }

    public CacheEntry(CacheEntry<T> other) {
        this.file = other.file;
        this.key = other.key;
        this.cache = other.cache;
        this.accessTime = other.accessTime;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public CacheKey getKey() {
        return key;
    }

    public T getFile() {
        updateAccessTime();
        return file;
    }

    public void updateAccessTime() {
        this.accessTime = System.currentTimeMillis();
    }

    public void clear() {
        this.cache.remove(getKey(), false);
    }

    public void setCache(FileCache cache) {
        this.cache = cache;
    }
}
