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
package com.gip.xyna.xact.trigger;

import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;

import com.gip.xyna.utils.concurrent.FakedFuture;
import xact.ssh.sftp.filesystem.FileSystemCacheParameter;
import xact.ssh.sftp.filesystem.RequestContext;
import xact.ssh.sftp.filesystem.XynaBackedFile;
import com.gip.xyna.xfmg.Constants;

public class SFTPTriggerConnection extends SSHDTriggerConnection {

  private static final long serialVersionUID = -5147487763750951711L;

  private final transient RequestContext requestCtx;
  private final transient FakedFuture<XynaBackedFile> filterResponse;
  private transient FileSystemCacheParameter cacheParameter;

  public SFTPTriggerConnection(FakedFuture<XynaBackedFile> futureFile, RequestContext info) {
    this.requestCtx = info;
    this.filterResponse = futureFile;
    this.cacheParameter = FileSystemCacheParameter.noCaching();
  }

  public String getUsername() {
    return getRequestCtx().getUsername();
  }

  public FileSystemCacheParameter getCacheParameter() {
    return cacheParameter;
  }

  public String getSourceIp() {
    return getRequestCtx().getRemoteIp();
  }

  public String getPath() {
    return getRequestCtx().getPath().toString();
  }

  public void setCacheParameter(FileSystemCacheParameter cacheParameter) {
    this.cacheParameter = cacheParameter;
  }

  public void reply(String content) {
    var file = new XynaBackedFile(content, getCacheParameter(), getRequestCtx().getPath());
    file.setCreationStartTime(getRequestCtx().getCreationTime());
    file.setCreationFinishedTime(System.currentTimeMillis());
    file.setUsername(getRequestCtx().getUsername());
    file.setRemoteAddress(getRequestCtx().getRemoteIp());
    getFilterResponse().set(file);
  }

  public void reply(byte[] content) {
    var file = new XynaBackedFile(content, getCacheParameter(), getRequestCtx().getPath());
    file.setCreationStartTime(getRequestCtx().getCreationTime());
    file.setCreationFinishedTime(System.currentTimeMillis());
    file.setUsername(getRequestCtx().getUsername());
    file.setRemoteAddress(getRequestCtx().getRemoteIp());
    getFilterResponse().set(file);
  }

  public void fileNotFound() {
    getFilterResponse().cancel(true);
  }

  public void cancel() {
    getFilterResponse().cancel(true);
  }

  @Override
  public synchronized void close() {
    if (getFilterResponse().isDone()) {
      cancel();
    }
    super.close();
  }

  private RequestContext getRequestCtx() {
    if (requestCtx == null) {
      throw new IllegalStateException("Session lost due to serialization");
    } else {
      return requestCtx;
    }
  }

  private FakedFuture<XynaBackedFile> getFilterResponse() {
    if (filterResponse == null) {
      throw new IllegalStateException("Session lost due to serialization");
    } else {
      return filterResponse;
    }
  }

  @Override
  public void handleProcessingRejected(String cause) {
    this.cancel();
  };

  @Override
  public void handleNoFilterFound() {
    this.fileNotFound();
  }

}
