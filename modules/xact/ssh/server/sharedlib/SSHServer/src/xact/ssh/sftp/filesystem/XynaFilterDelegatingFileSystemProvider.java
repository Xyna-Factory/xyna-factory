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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import xact.ssh.sftp.XynaBackedFileProvider;
import xact.ssh.sftp.filesystem.cache.FileCache;

public class XynaFilterDelegatingFileSystemProvider extends FileSystemProvider {

    static final Logger logger = CentralFactoryLogging.getLogger(XynaFilterDelegatingFileSystemProvider.class);

    private static final String SCHEME = "xynafs";

    private static final Long defaultFileSize = Math.multiplyFull(Integer.MAX_VALUE, 1);

    public static final XynaFilterDelegatingFileSystemProvider INSTANCE = new XynaFilterDelegatingFileSystemProvider();

    private static ConcurrentHashMap<String, FileSystem> filesystems = new ConcurrentHashMap<>();

    private static Map<String, Object> basicAttributesToMap(BasicFileAttributes attrs) {
        Map<String, Object> map = new HashMap<>();
        map.put("creationTime", attrs.creationTime());
        map.put("lastAccessTime", attrs.lastAccessTime());
        map.put("lastModifiedTime", attrs.lastModifiedTime());
        map.put("size", attrs.size());
        map.put("isDirectory", attrs.isDirectory());
        map.put("isRegularFile", attrs.isRegularFile());
        map.put("isSymbolicLink", attrs.isSymbolicLink());
        map.put("isOther", attrs.isOther());
        map.put("fileKey", attrs.fileKey());
        return map;
    }

    private static Map<String, Object> posixAttributesToMap(PosixFileAttributes attrs) {
        Map<String, Object> map = basicAttributesToMap(attrs); // reuse method above
        map.put("owner", attrs.owner());
        map.put("group", attrs.group());
        map.put("permissions", attrs.permissions());
        return map;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        validateScheme(uri);

        if (!filesystems.containsKey(uri.getSchemeSpecificPart())) {

            String root = (String) env.get("rootPath");
            String staticFilePrefix = (String) env.get("staticFilePrefix");
            XynaBackedFileProvider trigger = (XynaBackedFileProvider) env.get("trigger");

            XynaFilterDelegatingFileSystem fs;
            if (root == null) {
                fs = new XynaFilterDelegatingFileSystem(trigger,
                        uri);
            } else {
                fs = new XynaFilterDelegatingFileSystem(trigger,
                        Path.of(root),
                        staticFilePrefix,
                        uri);
            }

            filesystems.put(uri.getSchemeSpecificPart(), fs);

            if (logger.isDebugEnabled()) {
                logger.debug("Creating new Filesystem for " + uri);
            }
        }

        return getFileSystem(uri);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        validateScheme(uri);

        if (!filesystems.containsKey(uri.getSchemeSpecificPart()))
            throw new FileSystemNotFoundException(uri.toString());

        return filesystems.get(uri.getSchemeSpecificPart());
    }

    @Override
    public Path getPath(URI uri) {
        return getFileSystem(uri).getPath(uri.getPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("new byte channel@" + path);
        }

        return this.newFileChannel(path, options, attrs);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("new file channel@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) path).isStaticFile())
                return ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                        .newFileChannel(path.toRealPath(new LinkOption[0]), options, attrs);
            else {
                XynaBackedFile file = ((XynaFilterDelegatingFileSystem) ((XynaFilterDelegatingPath) path)
                        .getFileSystem()).retrieveFile((XynaFilterDelegatingPath) path);

                return new XynaFileChannel((XynaFilterDelegatingPath) path, file);
            }
        }

        throw new UnsupportedOperationException("Unimplemented method 'newFileChannel'");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("new directory stream@" + dir);
        }

        if (dir instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) dir).isStaticFile())
                return ((XynaFilterDelegatingPath) dir).getFileSystem().getNativeFileSystem().provider()
                        .newDirectoryStream(dir.toRealPath(new LinkOption[0]), filter);
        }

        throw new UnsupportedOperationException("Unimplemented method 'newDirectoryStream'");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("create directory@" + dir);
        }

        if (dir instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) dir).isStaticFile()) {
                ((XynaFilterDelegatingPath) dir).getFileSystem().getNativeFileSystem().provider()
                        .createDirectory(dir.toRealPath(new LinkOption[0]), attrs);
                return;
            }
        }

        throw new UnsupportedOperationException("Unimplemented method 'createDirectory'");
    }

    @Override
    public void delete(Path path) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("delete@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) path).isStaticFile()) {
                ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                        .delete(path.toRealPath(new LinkOption[0]));
                return;
            }
        }

        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("copy from " + source + " to " + target);
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("move from " + source + " to " + target);
        }

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'move'");
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("is same file@" + path + " for " + path2);
        }

        return path.equals(path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("is hidden@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) path).isStaticFile())
                return ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                        .isHidden(path.toRealPath(new LinkOption[0]));
        }

        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("get filestore@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) path).isStaticFile())
                return ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                        .getFileStore(path.toRealPath(new LinkOption[0]));
        }

        throw new UnsupportedOperationException("Unimplemented method 'getFileStore'");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("check access@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            Path r = path.toRealPath(new LinkOption[0]);
            try {
                if (((XynaFilterDelegatingPath) path).isStaticFile())
                    ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                            .checkAccess(r, modes);
            } catch (FileSystemException | FileNotFoundException f) {
                if (logger.isDebugEnabled()) {
                    logger.debug("check access@" + r + " failed. Throwing ...", f);
                }
                throw f;
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("check access@" + path + " failed. Ignoring ...", e);
                }
            }
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (path instanceof XynaFilterDelegatingPath) {
            if (logger.isDebugEnabled()) {
                logger.debug("get attribute view@" + path);
            }

            if (((XynaFilterDelegatingPath) path).isStaticFile()) {
                Path realPath;
                try {
                    realPath = path.toRealPath(options);
                    return ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                            .getFileAttributeView(realPath, type, options);
                } catch (IOException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("get attribute view@" + path + " failed. Ignoring ...", e);
                    }
                }
            }
            if (BasicFileAttributeView.class.equals(type))
                return (V) getBasicFileAttributeView((XynaFilterDelegatingPath) path);

            if (PosixFileAttributeView.class.equals(type))
                return (V) getPosixFileAttributeView((XynaFilterDelegatingPath) path);

        }

        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("readAttributes(path=" + path + " , type=" + (type != null ? type.getSimpleName() : "null")
                    + ", options=" +
                    Arrays.toString(options) + ")");
        }

        if (path instanceof XynaFilterDelegatingPath) {
            XynaFilterDelegatingPath xfPath = (XynaFilterDelegatingPath) path;

            if (logger.isDebugEnabled()) {
                logger.debug("Path is XynaFilterDelegatingPath: " + xfPath);
                logger.debug("isStaticFile = " + xfPath.isStaticFile());
            }

            // Delegate static files to native filesystem
            if (xfPath.isStaticFile()) {
                Path realPath = path.toRealPath(options);

                if (logger.isDebugEnabled()) {
                    logger.debug("Delegating to native FS provider. realPath=" + realPath);
                }

                A attrs = null;
                try {
                    attrs = xfPath.getFileSystem()
                            .getNativeFileSystem()
                            .provider()
                            .readAttributes(realPath, type, options);
                } catch (FileSystemException | FileNotFoundException f) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Native FS returned exception. Throwing ...", f);
                    }
                    throw f;
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Native FS returned exception. " + xfPath , e);
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Native FS returned attributes: " + attrs);
                }

                return attrs;
            }

            // Dynamic virtual file attributes
            if (BasicFileAttributes.class.equals(type)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Returning virtual BasicFileAttributes for " + path);
                }
                return (A) getBasicFileAttributes(xfPath);
            }

            if (PosixFileAttributes.class.equals(type)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Returning virtual PosixFileAttributes for " + path);
                }
                return (A) getPosixFileAttributes(xfPath);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Unsupported attribute type requested: " + type);
            }
        } else

        {
            if (logger.isDebugEnabled()) {
                logger.debug("Path is NOT XynaFilterDelegatingPath: " + path.getClass());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Returning null attributes for " + path);
        }

        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path,
            String attributes,
            LinkOption... options) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("readAttributes() called");
            logger.debug("  path           = " + path);
            logger.debug("  class of path  = " + path.getClass().getName());
            logger.debug("  attributes     = " + attributes);
            logger.debug("  options        = " + java.util.Arrays.toString(options));
        }

        if (path instanceof XynaFilterDelegatingPath) {
            var xp = (XynaFilterDelegatingPath) path;

            if (logger.isDebugEnabled()) {
                logger.debug("  isStaticFile   = " + xp.isStaticFile());
                logger.debug("  root           = " + xp.getRoot());
                logger.debug("  nameCount      = " + xp.getNameCount());
                logger.debug("  parent         = " + xp.getParent());
                logger.debug("  absolutePath   = " + xp.toAbsolutePath());
            }

            if (xp.isStaticFile()) {

                Path real = xp.toRealPath(options);

                if (logger.isDebugEnabled()) {
                    logger.debug("  resolved real  = " + real);
                }

                try {
                    Map<String, Object> result = xp.getFileSystem()
                            .getNativeFileSystem()
                            .provider()
                            .readAttributes(real, attributes, options);

                    if (logger.isDebugEnabled()) {
                        logger.debug("  native attrs   = " + result);
                    }

                    return result;
                } catch (FileNotFoundException | FileSystemException f) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Native FS returned exception. Throwing ...", f);
                    }
                    throw f;
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Exception getting posix attributes from static file " + real, e);
                    }
                }
            }

            Map<String, Object> m = Collections.emptyMap();
            if (attributes.startsWith("basic:"))
                m = getBasicFileAttributesAsMap(xp);

            if (attributes.startsWith("posix:"))
                m = getPosixFileAttributesAsMap(xp);

            if (logger.isDebugEnabled()) {
                logger.debug("  virtual path -> returning synthetic attributes " + m);
            }

            return m;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("  unsupported path type -> throwing");
        }

        throw new UnsupportedOperationException(
                "readAttributes not supported for " + path);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("set attribute@" + path);
        }

        if (path instanceof XynaFilterDelegatingPath) {
            if (((XynaFilterDelegatingPath) path).isStaticFile()) {
                ((XynaFilterDelegatingPath) path).getFileSystem().getNativeFileSystem().provider()
                        .setAttribute(path.toRealPath(options), attribute, value, options);
                return;
            }
        }

        throw new UnsupportedOperationException("Unimplemented method 'setAttribute'");
    }

    private PosixFileAttributes getPosixFileAttributes(XynaFilterDelegatingPath path) {

        try {
            if (!path.isDir()) {
                return getPosixFileAttributes(path.getFileSystem().retrieveFile(path));
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled())
                logger.debug("Could not retrieve file " + path.toString(), e);
        }

        BasicFileAttributes attr = getBasicFileAttributes(path);

        return new PosixFileAttributes() {

            @Override
            public FileTime lastModifiedTime() {
                return attr.lastModifiedTime();
            }

            @Override
            public FileTime lastAccessTime() {
                return attr.lastAccessTime();
            }

            @Override
            public FileTime creationTime() {
                return attr.creationTime();
            }

            @Override
            public boolean isRegularFile() {
                return attr.isRegularFile();
            }

            @Override
            public boolean isDirectory() {
                return attr.isDirectory();
            }

            @Override
            public boolean isSymbolicLink() {
                return attr.isSymbolicLink();
            }

            @Override
            public boolean isOther() {
                return attr.isOther();
            }

            @Override
            public long size() {
                return attr.size();
            }

            @Override
            public Object fileKey() {
                return attr.fileKey();
            }

            @Override
            public UserPrincipal owner() {
                return new UserPrincipal() {

                    @Override
                    public String getName() {
                        return path.getFileSystem().getUsername();
                    }

                    @Override
                    public String toString() {
                        return getName();
                    }
                };
            }

            @Override
            public GroupPrincipal group() {
                return new GroupPrincipal() {

                    @Override
                    public String getName() {
                        return "xyna";
                    }

                    @Override
                    public String toString() {
                        return getName();
                    }
                };
            }

            @Override
            public Set<PosixFilePermission> permissions() {
                return PosixFilePermissions.fromString("r-xr-xr-x");
            }
        };
    }

    private Map<String, Object> getPosixFileAttributesAsMap(XynaFilterDelegatingPath xp) throws IOException {
        return posixAttributesToMap(getPosixFileAttributes(xp));
    }

    private Map<String, Object> getBasicFileAttributesAsMap(XynaFilterDelegatingPath xp) throws IOException {
        return basicAttributesToMap(getBasicFileAttributes(xp));
    }

    private Map<String, Object> getPosixFileAttributesAsMap(XynaBackedFile file) {
        return posixAttributesToMap(getPosixFileAttributes(file));
    }

    private Map<String, Object> getBasicFileAttributesAsMap(XynaBackedFile file) {
        return basicAttributesToMap(getBasicFileAttributes(file));
    }

    private PosixFileAttributes getPosixFileAttributes(XynaBackedFile file) {
        return new PosixFileAttributes() {

            private BasicFileAttributes attr = getBasicFileAttributes(file);

            @Override
            public FileTime lastModifiedTime() {
                return attr.lastModifiedTime();
            }

            @Override
            public FileTime lastAccessTime() {
                return attr.lastAccessTime();
            }

            @Override
            public FileTime creationTime() {
                return attr.creationTime();
            }

            @Override
            public boolean isRegularFile() {
                return attr.isRegularFile();
            }

            @Override
            public boolean isDirectory() {
                return attr.isDirectory();
            }

            @Override
            public boolean isSymbolicLink() {
                return attr.isSymbolicLink();
            }

            @Override
            public boolean isOther() {
                return attr.isOther();
            }

            @Override
            public long size() {
                return attr.size();
            }

            @Override
            public Object fileKey() {
                return attr.fileKey();
            }

            @Override
            public UserPrincipal owner() {
                return new UserPrincipal() {

                    @Override
                    public String getName() {
                        return file.getUsername();
                    }

                    @Override
                    public String toString() {
                        return getName();
                    }
                };
            }

            @Override
            public GroupPrincipal group() {
                return new GroupPrincipal() {

                    @Override
                    public String getName() {
                        return "xyna";
                    }

                    @Override
                    public String toString() {
                        return getName();
                    }
                };
            }

            @Override
            public Set<PosixFilePermission> permissions() {
                return PosixFilePermissions.fromString("r--r--r--");
            }
        };
    }

    private BasicFileAttributeView getBasicFileAttributeView(XynaFilterDelegatingPath path) {

        return new BasicFileAttributeView() {

            @Override
            public BasicFileAttributes readAttributes() throws IOException {
                return getBasicFileAttributes(path);
            }

            @Override
            public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
                    throws IOException {
                return;
            }

            @Override
            public String name() {
                return "basic";
            }
        };

    }

    private PosixFileAttributeView getPosixFileAttributeView(XynaFilterDelegatingPath path) {

        return new PosixFileAttributeView() {

            private PosixFileAttributes attr = getPosixFileAttributes(path);

            @Override
            public PosixFileAttributes readAttributes() throws IOException {
                return attr;
            }

            @Override
            public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
                    throws IOException {
                return;
            }

            @Override
            public String name() {
                return "posix";
            }

            @Override
            public UserPrincipal getOwner() throws IOException {
                return attr.owner();
            }

            @Override
            public void setOwner(UserPrincipal owner) throws IOException {
                return;
            }

            @Override
            public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
                return;
            }

            @Override
            public void setGroup(GroupPrincipal group) throws IOException {
                return;
            }
        };

    }

    private BasicFileAttributes getBasicFileAttributes(XynaFilterDelegatingPath path) {

        try {
            if (!path.isDir()) {
                return getBasicFileAttributes(path.getFileSystem().retrieveFile(path));
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled())
                logger.debug("Could not retrieve file " + path.toString(), e);
        }

        return new BasicFileAttributes() {

            @Override
            public FileTime lastModifiedTime() {
                return FileTime.fromMillis(System.currentTimeMillis());
            }

            @Override
            public FileTime lastAccessTime() {
                return FileTime.fromMillis(System.currentTimeMillis());
            }

            @Override
            public FileTime creationTime() {
                return FileTime.fromMillis(System.currentTimeMillis());
            }

            @Override
            public boolean isRegularFile() {
                try {
                    return path.isFile();
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public boolean isDirectory() {
                try {
                    return path.isDir();
                } catch (IOException e) {
                    return true;
                }
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                try {
                    return path.isDir() ? 0L : defaultFileSize;
                } catch (IOException e) {
                    return 0L;
                }
            }

            @Override
            public Object fileKey() {
                return null;
            }

        };
    }

    private BasicFileAttributes getBasicFileAttributes(XynaBackedFile file) {
        return new BasicFileAttributes() {

            @Override
            public FileTime lastModifiedTime() {
                return FileTime.fromMillis(file.getLastAccessTime());
            }

            @Override
            public FileTime lastAccessTime() {
                return FileTime.fromMillis(file.getCreationFinishedTime());
            }

            @Override
            public FileTime creationTime() {
                return FileTime.fromMillis(file.getCreationStartTime());

            }

            @Override
            public boolean isRegularFile() {
                return true;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return file.size();
            }

            @Override
            public Object fileKey() {
                return null;
            }

        };
    }

    private void validateScheme(URI uri) throws IllegalArgumentException {
        if (!Objects.equals(getScheme(), uri.getScheme())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid FS for " + uri);
            }
            throw new IllegalArgumentException(
                    "Mismatched FS scheme: '" + getScheme() + "' != '" + uri.getScheme() + "'");
        }
    }

}
