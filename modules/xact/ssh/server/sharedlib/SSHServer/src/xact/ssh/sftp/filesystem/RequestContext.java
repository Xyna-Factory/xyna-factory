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


import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

public class RequestContext {
  private static final Logger logger = CentralFactoryLogging.getLogger(RequestContext.class);
  private XynaFilterDelegatingPath path;

  public long getCreationTime() {
    return creationTime;
  }

  private long creationTime;

  public XynaFilterDelegatingPath getPath() {
    return path;
  }

  public RequestContext(XynaFilterDelegatingPath path) {
    this.path = path;
    this.creationTime = System.currentTimeMillis();
  }

  public String getUsername() {
    return getPath().getFileSystem().getUsername();
  }

  public String getRemoteIp() {
    return getPath().getFileSystem().getRemoteAddress();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Username: " + this.getUsername()).append(", ");
    sb.append("RemoteIp: " + this.getRemoteIp()).append(", ");
    sb.append("Path: '" + this.getPath()).append("', ");
    sb.append("Created at: " + this.getCreationTime());
    return sb.toString();
  }
}
