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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.apache.sshd.common.file.root.RootedFileSystem;
import org.apache.sshd.common.file.util.BaseFileSystem;
import org.apache.sshd.common.session.SessionContext;

import com.gip.xyna.CentralFactoryLogging;
import xact.ssh.sftp.XynaBackedFileProvider;
import xact.ssh.sftp.filesystem.FileSystemCacheParameter.TimeBasedCacheParameter;
import xact.ssh.sftp.filesystem.cache.CacheEntry;
import xact.ssh.sftp.filesystem.cache.CacheKey;
import xact.ssh.sftp.filesystem.cache.FileCache;
import xact.ssh.sftp.filesystem.cache.TimedCacheEntry;

public class XynaFilterDelegatingFileSystem extends BaseFileSystem<XynaFilterDelegatingPath> {

    private final static String defaultStaticFilePrefix = "/StaticXfc/";

    public final static class Factory implements FileSystemFactory {

        private HashMap<String, Object> properties = new HashMap<>();

        public Factory(XynaBackedFileProvider trigger) {
            this.properties.put("trigger", trigger);
            this.properties.put("staticFilePrefix", defaultStaticFilePrefix);
        }

        public Factory(XynaBackedFileProvider trigger, String rootPath, String staticFilePrefix) {
            String sfp = staticFilePrefix;
            if (staticFilePrefix == null) {
                sfp = defaultStaticFilePrefix;
            } else if (!staticFilePrefix.endsWith("/"))
                sfp = staticFilePrefix + "/";

            this.properties.put("rootPath", rootPath);
            this.properties.put("staticFilePrefix", sfp);
            this.properties.put("trigger", trigger);
        }

        @Override
        public FileSystem createFileSystem(SessionContext arg0) throws IOException {

            if (logger.isDebugEnabled()) {
                var s = arg0.attributeKeys().stream()
                        .map(k -> k.toString().concat(": ").concat(arg0.getAttribute(k).toString()))
                        .reduce("", String::concat);
                logger.debug(s);
            }

            int port = 0;
            String ip = "0.0.0.0";

            SocketAddress addr = arg0.getRemoteAddress();

            if (addr instanceof InetSocketAddress) {
                port = ((InetSocketAddress) addr).getPort();
                ip = ((InetSocketAddress) addr).getAddress().getHostAddress();
            }

            StringBuilder sb = new StringBuilder(XynaFilterDelegatingFileSystemProvider.INSTANCE.getScheme());
            sb.append("://").append(arg0.getUsername()).append("@").append(ip).append(":").append(port);

            String uriStr = sb.toString();
            URI uri;
            try {
                uri = new URI(uriStr);
                return XynaFilterDelegatingFileSystemProvider.INSTANCE.newFileSystem(uri, properties);
            } catch (URISyntaxException e) {
                logger.error("Invalid URI: " + uriStr, e);
                throw new IllegalArgumentException(uriStr, e);
            }

        }

        @Override
        public Path getUserHomeDir(SessionContext arg0) throws IOException {
            String dir = (String) properties.get("staticFilePrefix");
            return Path.of(dir);
        }

    }

    private final static Logger logger = CentralFactoryLogging.getLogger(XynaFilterDelegatingFileSystem.class);

    private boolean _allowNativeFileAccess;

    public boolean allowNativeFileAccess() {
        return _allowNativeFileAccess;
    }

    private String staticFilePrefix;

    private URI uri;

    private RootedFileSystem nativeFileSystem;

    private XynaBackedFileProvider trigger;

    private Path nativeRoot;

    private Set<CacheKey> sessionCacheKeys = new HashSet<>();

    public XynaFilterDelegatingFileSystem(XynaBackedFileProvider trigger, Path root, String staticFilePrefix, URI uri) {
        super(XynaFilterDelegatingFileSystemProvider.INSTANCE);

        init(trigger, root, staticFilePrefix, uri);
    }

    public XynaFilterDelegatingFileSystem(XynaBackedFileProvider trigger, URI uri) {
        super(XynaFilterDelegatingFileSystemProvider.INSTANCE);

        init(trigger, null, null, uri);
    }

    private void init(XynaBackedFileProvider trigger, Path root, String staticFilePrefix, URI uri) {
        this.trigger = trigger;
        this.nativeRoot = root;

        if (this.nativeRoot != null) {
            try {
                RootedFileSystemProvider rfs = new RootedFileSystemProvider();
                nativeFileSystem = (RootedFileSystem)rfs.newFileSystem(root, Collections.emptyMap());
                _allowNativeFileAccess = true;
            } catch (IOException e) {
                logger.error("Could not create rooted filesystem at '" + root + "'", e);
                _allowNativeFileAccess = false;
            }
        } else {
            _allowNativeFileAccess = false;
        }

        if (staticFilePrefix == null) {
            this.staticFilePrefix = "/StaticXfc/";
        } else if (!staticFilePrefix.endsWith("/"))
            this.staticFilePrefix = staticFilePrefix + "/";
        else
            this.staticFilePrefix = staticFilePrefix;

        this.uri = uri;

        if (logger.isDebugEnabled()) {
            if (allowNativeFileAccess()) {
                if (logger.isDebugEnabled())
                    logger.debug("created new filesystem for '" + this.uri + "' with native file access with rootDir: '"
                            + root + "' for prefix '"
                            + this.staticFilePrefix + "'");
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("created new filesystem for '" + this.uri + "' with no native file access.");
            }
        }
    }

    public String getStaticFilePrefix() {
        return staticFilePrefix;
    }

    public  RootedFileSystem getNativeFileSystem() {
        return nativeFileSystem;
    }

    public String getUsername() {
        return this.uri.getUserInfo();
    }

    public String getRemoteAddress() {
        return this.uri.getHost();
    }

    public String getRemotePort() {
        return String.valueOf(this.uri.getPort());
    }

    public XynaBackedFile retrieveFile(XynaFilterDelegatingPath path) throws IOException {
        var normalPath = path.normalize();

        RequestContext context = new RequestContext(normalPath);
        if (logger.isDebugEnabled())
            logger.debug("Context:" + context);

        try {
            java.util.Optional<XynaBackedFile> oFile = FileCache.INSTANCE.lookup(normalPath)
                    .map(e -> e.getFile());

            XynaBackedFile file = null;
            if (oFile.isPresent()) {
                file = oFile.get();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("starting filter delegation for file: " + normalPath.toString() + " "
                            + System.currentTimeMillis());
                }
                file = this.trigger.requestFile(context)
                        .get(this.trigger.getRequestTimeout().getDurationInMillis(),
                                TimeUnit.MILLISECONDS);
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "filter delegation for file returned: " + normalPath.toString() + " "
                                    + System.currentTimeMillis());
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Got file for path " + normalPath);
                logger.debug(file);
            }
            file.setUsername(context.getUsername());
            file.setRemoteAddress(context.getRemoteIp());
            if (logger.isDebugEnabled()) {
                logger.debug("Updated file for path " + normalPath);
                logger.debug(file);
            }

            switch (file.getCacheParameter().getType()) {
                case TIMED: {
                    // add entry to cache without username or ip
                    FileCache.INSTANCE.putWithTimeoutIfAbsent(
                            new CacheEntry<XynaBackedFile>(new CacheKey(normalPath.toString()), file),
                            ((TimeBasedCacheParameter) file.getCacheParameter()).getDuration());

                    break;
                }
                case SESSION_ISOLATION:
                case NONE:
                default: {
                    // add entry to cache with username and ip for lookup of filesize
                    var key = new CacheKey(normalPath);
                    sessionCacheKeys.add(key);
                    FileCache.INSTANCE.putIfAbsent(
                            new CacheEntry<XynaBackedFile>(key, file));
                    break;
                }

            }

            return file;

        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception retrieving file " + path + " from xyna.", e);
            }
            throw new IOException("Exception retrieving file " + path + " from xyna.", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("close@" + uri);
        }

        sessionCacheKeys.stream().forEach(e -> FileCache.INSTANCE.remove(e, true));
    }

    @Override
    public boolean isOpen() {
        if (logger.isDebugEnabled()) {
            logger.debug("isOpen@" + uri);
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isOpen'");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        if (logger.isDebugEnabled()) {
            logger.debug("supportedFileAttributeViews@" + uri);
        }
        return Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList("basic", "posix")));
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        if (logger.isDebugEnabled()) {
            logger.debug("getUserPrincipalLookupService@" + uri);
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserPrincipalLookupService'");
    }

    @Override
    protected XynaFilterDelegatingPath create(String arg0, List<String> arg1) {
        if (logger.isDebugEnabled()) {
            logger.debug("create Path@" + uri + " for " + arg0 + " and " + String.join(",", arg1));
        }
        return new XynaFilterDelegatingPath(this, arg0, arg1);
    }

}
