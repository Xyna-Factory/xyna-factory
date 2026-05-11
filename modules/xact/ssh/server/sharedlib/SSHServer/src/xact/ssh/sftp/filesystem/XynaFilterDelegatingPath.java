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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.sshd.common.file.util.BasePath;

import com.gip.xyna.CentralFactoryLogging;

public class XynaFilterDelegatingPath extends BasePath<XynaFilterDelegatingPath, XynaFilterDelegatingFileSystem> {

    static final Logger logger = CentralFactoryLogging.getLogger(XynaFilterDelegatingPath.class);

    protected XynaFilterDelegatingPath(XynaFilterDelegatingFileSystem fileSystem, String root, List<String> names) {
        super(fileSystem, root, names);
    }

    public boolean isStaticFile() {
        var fs = getFileSystem();
        if (logger.isTraceEnabled()) {
            logger.trace("allow native access: " + fs.allowNativeFileAccess());
            logger.trace("starts with prefix: " + this.startsWith(fs.getStaticFilePrefix()));
        }
        return fs.allowNativeFileAccess() && this.startsWith(fs.getStaticFilePrefix());
    }

    public boolean isFile() throws IOException {
        if (isStaticFile()) {
            if (this.getNameCount() <= 1)
                return false;

            return Files.isRegularFile(this.toRealPath((LinkOption[]) null), (LinkOption[]) null);
        }

        return !this.isDir();
    }

    public boolean isDir() throws IOException {
        if (isStaticFile()) {

            if (this.getNameCount() <= 1)
                return true;

            return Files.isDirectory(this.toRealPath((LinkOption[]) null), (LinkOption[]) null);
        }

        if (this.getParent() == null)
            return true;

        if (isRootDirectory())
            return true;

        return false;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("toRealPath@" + this);
            logger.trace("root: " + (getRoot() != null ? getRoot().toString() : "NULL"));
            logger.trace("names: " + (this.names != null ? String.join(",", this.names.toArray(new String[0])) : "[]"));
        }
        if (isStaticFile()) {
            Path p;
            try {
                var fs = getFileSystem().getNativeFileSystem();

                if (getRoot() != null && getNameCount() <= 1)
                    return fs.getPath("/", new String[]{});

                p = fs.getPath("/",
                        this.subpath(getNameCount() > 1 ? 1 : 0, getNameCount()).names.toArray(new String[0]))
                        .toAbsolutePath()
                        .normalize();
            } catch (Exception e) {
                if (logger.isDebugEnabled())
                    logger.debug("could not convert to real path: " + this.toString(), e);
                throw new IOException("could not convert to real path: " + this.toString(), e);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("toRealPath2" + p);
            }
            return p;
        }

        return this;
    }

    public boolean isRoot() {
        return getRoot() != null && getNameCount() == 0;
    }

    public boolean isDot() {
        return getRoot() == null
                && getNameCount() == 1
                && ".".equals(getName(0).toString());
    }

    public boolean isRootDirectory() {
        return isRoot() || isDot();
    }

}
