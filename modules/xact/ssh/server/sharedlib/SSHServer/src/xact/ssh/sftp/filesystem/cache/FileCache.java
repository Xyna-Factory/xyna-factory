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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;

import xact.ssh.sftp.filesystem.XynaBackedFile;
import xact.ssh.sftp.filesystem.XynaFilterDelegatingPath;

public class FileCache {

    private static final Logger logger = CentralFactoryLogging.getLogger(FileCache.class);

    public final static FileCache INSTANCE = new FileCache();

    private static ScheduledExecutorService cacheTimeouts = Executors.newSingleThreadScheduledExecutor();
    private final static ConcurrentHashMap<CacheKey, CacheEntry<XynaBackedFile>> cache = new ConcurrentHashMap<CacheKey, CacheEntry<XynaBackedFile>>();

    private final static List<AccessLogEntry> EVENT_LOG = new ArrayList<AccessLogEntry>();
    private final static XynaPropertyInt EVENT_LOG_MAX_SIZE = new XynaPropertyInt("xact.sftp.eventlog.size", 50);

    public static synchronized void startCleanUp() {
        if (cacheTimeouts != null)
            return;

        cacheTimeouts = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized void stopCleanUp() {
        if (cacheTimeouts != null) {
            cacheTimeouts.shutdown();
            cacheTimeouts = null;
        }
        clear();
    }

    private FileCache() {
    }

    public static void clear() {
        for (var e : cache.values())
            e.clear();
    }

    public Optional<CacheEntry<XynaBackedFile>> get(CacheKey key) {
        var entry = Optional.ofNullable(cache.get(key));
        if (entry.isPresent())
            entry.get().updateAccessTime();

        if (logger.isDebugEnabled()) {
            if (entry.isPresent())
                logger.debug("returing cached entry for key: " + key);
            else
                logger.debug("no cached entry for key: " + key);
        }
        return entry;
    }

    public Optional<CacheEntry<XynaBackedFile>> get(String path) {
        return get(new CacheKey(path));
    }

    public Optional<CacheEntry<XynaBackedFile>> get(XynaFilterDelegatingPath path) {
        return get(new CacheKey(path));
    }

    public Optional<CacheEntry<XynaBackedFile>> lookup(XynaFilterDelegatingPath path) {

        return get(new CacheKey(path.toString()))
                .or(() -> get(new CacheKey(path)));
    }

    public void put(CacheEntry<XynaBackedFile> file) {
        if (file != null) {
            file.setCache(INSTANCE);
            cache.put(file.getKey(), file);
            if (logger.isDebugEnabled()) {
                logger.debug("adding entry to cache for key: " + file.getKey());
            }
        }
    }

    public CacheEntry<XynaBackedFile> putIfAbsent(CacheEntry<XynaBackedFile> file) {
        if (file != null) {
            file.setCache(INSTANCE);
            var oldFile = cache.putIfAbsent(file.getKey(), file);
            addAccess(file.getFile(), oldFile != null);
            if (logger.isDebugEnabled()) {
                logger.debug("adding new (" + (oldFile == null) + ") entry to cache for key: " + file.getKey());
                logger.debug(file.getFile());
            }
            return oldFile;
        }
        return null;
    }

    public void putWithTimeoutIfAbsent(CacheEntry<XynaBackedFile> file, Duration timeout) {
        if (file != null) {
            var timedFile = new TimedCacheEntry<XynaBackedFile>(file, timeout, cacheTimeouts);
            var oldFile = putIfAbsent(timedFile);
            if (oldFile != null) {
                oldFile.updateAccessTime();
                timedFile.cancel();
            }
        }
    }

    public void remove(CacheKey key, boolean clear) {
        if (key != null) {
            var e = get(key);
            if (e.isPresent()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("removing entry from cache for key: " + key);
                }
                cache.remove(key);
                if (clear)
                    e.get().clear();
            }
        }
    }

    public void remove(String path, boolean clear) {
        if (path != null)
            remove(new CacheKey(path), clear);
    }

    public void remove(XynaFilterDelegatingPath path, boolean clear) {
        if (path != null)
            remove(new CacheKey(path), clear);
    }

    public final static class AccessLogEntry {

        private final long time;
        private final String user;
        private final String ip;
        private final String path;
        private final boolean wasCached;
        private final String fileType;
        private Duration lookup;

        AccessLogEntry(XynaBackedFile file, boolean wasCached) {
            this.time = System.currentTimeMillis();
            this.user = file.getUsername();
            this.ip = file.getRemoteAddress();
            this.path = file.getPathAsString();
            this.wasCached = wasCached;
            this.fileType = file.getCacheParameter().getType().name();
        }

        public long getTime() {
            return time;
        }

        public String getUser() {
            return user;
        }

        public String getIp() {
            return ip;
        }

        public String getPath() {
            return path;
        }

        public boolean isWasCached() {
            return wasCached;
        }

        public String getFileType() {
            return fileType;
        }

        public Duration getLookup() {
            return lookup;
        }

    }

    private static class AccessLogEntryTableFormatter extends TableFormatter {

        public enum AccessLogEntryColumn {
            Time {
                public String extract(AccessLogEntry ale) {
                    return Constants.defaultUTCSimpleDateFormatWithMS().format(ale.getTime());
                }
            },
            User {
                public String extract(AccessLogEntry ale) {
                    return ale.getUser();
                }
            },
            Ip {
                public String extract(AccessLogEntry ale) {
                    return ale.getIp();
                }
            },
            Path {
                public String extract(AccessLogEntry ale) {
                    return ale.getPath();
                }
            },
            FileType {
                public String extract(AccessLogEntry ale) {
                    return ale.getFileType();
                }
            },
            LookupDuration {
                public String extract(AccessLogEntry ale) {
                    if (ale.wasCached) {
                        return "-";
                    } else {
                        return ale.getLookup() == null ? "!" : ale.getLookup().toString();
                    }
                }
            };

            public abstract String extract(AccessLogEntry ale);
        }

        private List<List<String>> rows;
        private List<String> header;

        private List<AccessLogEntryColumn> columns;

        public AccessLogEntryTableFormatter(List<AccessLogEntry> logEntries) {
            columns = Arrays.asList(AccessLogEntryColumn.values());
            generateRowsAndHeader(logEntries);
        }

        public List<String> getHeader() {
            return header;
        }

        @Override
        public List<List<String>> getRows() {
            return rows;
        }

        private void generateRowsAndHeader(List<AccessLogEntry> logEntries) {
            header = new ArrayList<String>();
            for (AccessLogEntryColumn ac : columns) {
                header.add(ac.toString());
            }
            rows = new ArrayList<List<String>>();
            for (AccessLogEntry ale : logEntries) {
                rows.add(generateRow(ale));
            }
        }

        private List<String> generateRow(AccessLogEntry ale) {
            List<String> row = new ArrayList<String>();
            for (AccessLogEntryColumn alec : columns) {
                row.add(alec.extract(ale));
            }
            return row;
        }

    }

    private static class CacheKeyTableFormatter extends TableFormatter {

        public enum CacheKeyColumn {
            Path {
                public String extract(CacheKey ck) {
                    return ck.getPath();
                }
            },
            User {
                public String extract(CacheKey ck) {
                    return ck.getUsername().orElse("-");
                }
            },
            Ip {
                public String extract(CacheKey ck) {
                    return ck.getSourceIp().orElse("-");
                }
            };

            public abstract String extract(CacheKey ck);
        }

        private List<List<String>> rows;
        private List<String> header;

        private List<CacheKeyColumn> columns;

        public CacheKeyTableFormatter(Collection<CacheKey> logEntries) {
            columns = Arrays.asList(CacheKeyColumn.values());
            generateRowsAndHeader(logEntries);
        }

        public List<String> getHeader() {
            return header;
        }

        @Override
        public List<List<String>> getRows() {
            return rows;
        }

        private void generateRowsAndHeader(Collection<CacheKey> logEntries) {
            header = new ArrayList<String>();
            for (CacheKeyColumn ac : columns) {
                header.add(ac.toString());
            }
            rows = new ArrayList<List<String>>();
            for (CacheKey ale : logEntries) {
                rows.add(generateRow(ale));
            }
        }

        private List<String> generateRow(CacheKey ale) {
            List<String> row = new ArrayList<String>();
            for (CacheKeyColumn alec : columns) {
                row.add(alec.extract(ale));
            }
            return row;
        }

    }

    public static Set<CacheKey> listCacheKeys() {
        return Collections.unmodifiableSet(FileCache.cache.keySet());
    }

    public static TableFormatter listCacheKeysAsTable() {
        return new CacheKeyTableFormatter(listCacheKeys());
    }

    public static synchronized void addAccess(XynaBackedFile file, boolean wasCached) {
        AccessLogEntry logEntry = new AccessLogEntry(file, wasCached);
        logEntry.lookup = new Duration(file.getCreationFinishedTime() - file.getCreationStartTime());

        EVENT_LOG.add(logEntry);
        while (EVENT_LOG.size() > EVENT_LOG_MAX_SIZE.get()) {
            EVENT_LOG.remove(0);
        }
    }

    public static List<AccessLogEntry> getCacheAccessHistory() {
        return Collections.unmodifiableList(EVENT_LOG);
    }

    public static TableFormatter getCacheAccessHistoryAsTable() {
        return new AccessLogEntryTableFormatter(getCacheAccessHistory());
    }

}
