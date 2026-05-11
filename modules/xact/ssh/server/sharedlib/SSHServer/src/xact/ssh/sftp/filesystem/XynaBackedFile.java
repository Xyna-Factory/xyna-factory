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


import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;

public class XynaBackedFile {

    static final Logger logger = CentralFactoryLogging.getLogger(XynaBackedFile.class);

    private final byte[] content;
    private final FileSystemCacheParameter cacheParameter;
    private long creationStartTime = 0L;
    private long creationFinishedTime = 0L;
    private long lastAccessTime = 0L;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    private String path;
    private String username;
    private String remoteAddress;

    public XynaBackedFile(byte[] content, FileSystemCacheParameter cacheParameter, XynaFilterDelegatingPath path) {
        this.content = content;
        this.cacheParameter = cacheParameter;
        this.path = path.toString();
    }

    public String getPathAsString() {
        return path;
    }

    public XynaBackedFile(String content, FileSystemCacheParameter cacheParameter, XynaFilterDelegatingPath path) {
        this.cacheParameter = cacheParameter;
        this.path = path.toString();
        byte[] tmp = null;

        try {
            tmp = content.getBytes(Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("Got content with invalid encoding.", e);
        }

        this.content = tmp;
    }

    public long getCreationStartTime() {
        return this.creationStartTime;
    }

    public void setCreationStartTime(long creationTime) {
        this.creationStartTime = creationTime;
        this.lastAccessTime = creationTime;
    }

    public long getCreationFinishedTime() {
        return creationFinishedTime;
    }

    public void setCreationFinishedTime(long lastAccessTime) {
        this.creationFinishedTime = lastAccessTime;
        this.lastAccessTime = lastAccessTime;
    };

    public long getLastAccessTime() {
        return this.lastAccessTime;
    }

    public FileSystemCacheParameter getCacheParameter() {
        return this.cacheParameter;
    };

    public long size() {
        return this.content != null ? this.content.length : 0L;
    }

    public byte[] getContent() {
        lastAccessTime = System.currentTimeMillis();
        return content;
    }

    @Override
    public String toString() {
        return "XynaBackedFile{" +
                "size=" + size() +
                ", preview=\"" + buildSafePreview(64) + "\"" +
                ", cacheParameter=" + cacheParameter.getType().name() +
                ", creationStartTime=" + creationStartTime +
                ", creationFinishedTime=" + creationFinishedTime +
                ", lastAccessTime=" + lastAccessTime +
                ", username=" + username +
                ", remoteAddress=" + remoteAddress +
                '}';
    }

    private String buildSafePreview(int maxBytes) {
        if (content == null || content.length == 0) {
            return "";
        }

        int len = Math.min(content.length, maxBytes);
        StringBuilder sb = new StringBuilder();

        try {
            String raw = new String(content, 0, len, Constants.DEFAULT_ENCODING);

            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);

                switch (c) {
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\"':
                        sb.append("\\\"");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        if (c >= 32 && c <= 126) {
                            // Printable ASCII
                            sb.append(c);
                        } else {
                            // Non-printable → Unicode escape
                            sb.append(String.format("\\u%04x", (int) c));
                        }
                }
            }

            if (content.length > maxBytes) {
                sb.append("...");
            }

        } catch (Exception e) {
            sb.append("<encoding-error>");
        }

        return sb.toString();
    }
}
