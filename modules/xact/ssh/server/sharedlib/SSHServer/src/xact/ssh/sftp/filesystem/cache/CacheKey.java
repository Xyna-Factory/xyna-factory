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

import java.util.Optional;

import xact.ssh.sftp.filesystem.XynaFilterDelegatingPath;

public class CacheKey {

        private final String path;
        private final Optional<String> username;
        private final Optional<String> sourceIp;

        // Create a simple key with just the path
        public CacheKey(String path) {
            this.path = path;
            this.username = Optional.empty();
            this.sourceIp = Optional.empty();
        }

        public String getPath() {
            return path;
        }

        public Optional<String> getUsername() {
            return username;
        }

        public Optional<String> getSourceIp() {
            return sourceIp;
        }

        // create an isolated key with username and remote ip
        public CacheKey(XynaFilterDelegatingPath path) {
            this(path, false);
        }

       public CacheKey(XynaFilterDelegatingPath path, boolean withoutUsername) {
            this.path = path.toString();
            this.username = withoutUsername ? Optional.empty() : Optional.ofNullable(path.getFileSystem().getUsername());
            this.sourceIp = Optional.ofNullable(path.getFileSystem().getRemoteAddress());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null ||
                    !(obj instanceof CacheKey)) {
                return false;
            } else {
                CacheKey other = (CacheKey) obj;
                if (path.equals(other.path)) {
                    if (username.isPresent() != other.username.isPresent()) {
                        return false;
                    }
                    if (username.isPresent() &&
                            other.username.isPresent() &&
                            !username.get().equals(other.username.get())) {
                        return false;
                    }
                    if (sourceIp.isPresent() != other.sourceIp.isPresent()) {
                        return false;
                    }
                    if (sourceIp.isPresent() &&
                            other.sourceIp.isPresent() &&
                            !sourceIp.get().equals(other.sourceIp.get())) {
                        return false;
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        public int hashCode() {
            int hash = path.hashCode();
            if (username.isPresent()) {
                hash += username.get().hashCode();
            }
            if (sourceIp.isPresent()) {
                hash += sourceIp.get().hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            return username + "@"  + sourceIp + ":" + path;
        }

    }