/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.RMIChannelImpl;

public class RemoteFileManagementLanding implements InitializableRemoteInterface, RemoteFileManagementInterface {

  private final static Logger logger = CentralFactoryLogging.getLogger(RemoteFileManagementLanding.class);
  
  public final static String REMOTE_FILE_UPLOAD_LOCATION = "remoteFileMgmt";
  public final static int BUFFERSIZE = 5242880;
  
  private final AtomicLong transactionIdGenerator = new AtomicLong(0L); 
  private final ConcurrentMap<Long, ClosableManagedFileLease> openedFiles = new ConcurrentHashMap<Long, ClosableManagedFileLease>();
  private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "Remote Scheduled Timeout Executor Thread");
      t.setDaemon(true);
      return t;
    }
  });
  
  public void init(Object... initParameters) {
    
  }
  

  public FileTransferAcknowledgment startFileTransfer(XynaCredentials creds, FileTransfer transfer) throws RemoteException, XynaException {
    authenticate(creds);
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    Long transactionId;
    switch (transfer.getDirection()) {
      case Download :
        FileTransfer.Download download = FileTransferDirection.Download.cast(transfer);
        TransientFile file = fm.retrieve(download.getFileId());
        if (file == null) {
          throw new RuntimeException("File " + download.getFileId() + " unknown.");
        }
        transactionId = addOpenStream(download.getFileId(), file.getOriginalFilename(), file.openInputStream());
        return FileTransferAcknowledgment.download(file.getOriginalFilename(), transactionId);
      case Upload :
        FileTransfer.Upload upload = FileTransferDirection.Upload.cast(transfer);
        Triple<String,OutputStream,String> result = fm.store(REMOTE_FILE_UPLOAD_LOCATION, upload.getOriginalFileName());
        transactionId = addOpenStream(result.getFirst(), upload.getOriginalFileName(), result.getSecond());
        return FileTransferAcknowledgment.upload(result.getFirst(), transactionId);
      default :
        throw new IllegalArgumentException("Invalid TransferDirection: " + transfer.getDirection());
    }
  }


  public void sendFilePart(XynaCredentials creds, Long transactionId, byte[] content) throws RemoteException, XynaException {
    authenticate(creds);
    ClosableManagedFileLease lease = getLease(transactionId);
    try {
      OutputStream os = lease.getOpenedOutputStream();
      os.write(content);
    } catch (IOException e) {
      throw new Ex_FileAccessException(lease.getOriginalFileName(), e);
    }
  }
  
  
  public byte[] receiveFilePart(XynaCredentials creds, Long transactionId) throws RemoteException, XynaException {
    authenticate(creds);
    ClosableManagedFileLease lease = getLease(transactionId);
    InputStream is = lease.getOpenedInputStream();
    byte[] buffer = new byte[BUFFERSIZE];
    try {
      int bytes = is.read(buffer);
      if (bytes <= 0) {
        return new byte[0];
      } else {
        return Arrays.copyOfRange(buffer, 0, bytes);
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(lease.getOriginalFileName(), e);
    }
  }


  public void finishFileTransfer(XynaCredentials creds, Long transactionId) throws RemoteException, XynaException {
    authenticate(creds);
    ClosableManagedFileLease lease = openedFiles.remove(transactionId);
    if (lease == null) {
      throw new IllegalArgumentException("ManagedFile lease " + transactionId + " not present");
    }
    try {
      lease.close();
    } catch (IOException e) {
      throw new Ex_FileAccessException(lease.getOriginalFileName(), e);
    }
  }
  
  
  private Role authenticate(XynaCredentials credentials) throws RemoteException {
    return RMIChannelImpl.authenticate(credentials);
  }
  
  
  private ClosableManagedFileLease getLease(Long transactionId) {
    ClosableManagedFileLease lease = openedFiles.get(transactionId);
    if (lease == null) {
      throw new IllegalArgumentException("ManagedFile lease " + transactionId + " not present");
    } else {
      return lease;
    }
  }

  
  private Long addOpenStream(String localFileId, String originalFileName, Closeable openedStream) {
    Long transactionId = transactionIdGenerator.incrementAndGet();
    ClosableManagedFileLease lease = new ClosableManagedFileLease(transactionId, localFileId, originalFileName, openedStream);
    if (openedFiles.putIfAbsent(transactionId, lease) != null) {
      try {
        lease.close();
      } catch (IOException e) {
        logger.debug("Duplicated stream failed to be closed.",e);
      }
    }
    Duration timeout = XynaProperty.FILE_MANAGEMENT_DEFAULT_TIMEOUT.get();
    TimeUnit unit = TimeUnit.SECONDS; // at least 10 sec
    ScheduledFuture<Void> futureTimeout = timeoutExecutor.schedule(lease, Math.max(10, timeout.getDuration(unit) / 2), unit);
    lease.setTimeout(futureTimeout);
    return transactionId;
  }

  private class ClosableManagedFileLease implements Callable<Void> {

    private final Long transactionId;
    private final String localFileId;
    private final String originalFileName;
    private final Closeable openedStream;
    private ScheduledFuture<Void> timeout;
    
    
    private ClosableManagedFileLease(Long transactionId, String localFileId, String originalFileName, Closeable openedStream) {
      this.transactionId = transactionId;
      this.localFileId = localFileId;
      this.originalFileName = originalFileName;
      this.openedStream = openedStream;
    }

    public void setTimeout(ScheduledFuture<Void> timeout) {
      this.timeout = timeout;      
    }

    public void close() throws IOException {
      timeout.cancel(false);
      openedStream.close();
    }

    public Void call() throws Exception {
      openedFiles.remove(transactionId, this);
      openedStream.close();
      return null;
    }
    
    public InputStream getOpenedInputStream() throws XynaException {
      // no need to validate as we're currently uploading
      if (openedStream instanceof InputStream) {
        return (InputStream) openedStream;
      } else {
        throw new Ex_FileAccessException(originalFileName);
      }
    }
    
    public OutputStream getOpenedOutputStream() throws XynaException {
      validateTransientFile();
      if (openedStream instanceof OutputStream) {
        return (OutputStream) openedStream;
      } else {
        throw new Ex_FileAccessException(originalFileName);
      }
    }
    
    private void validateTransientFile() {
      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      TransientFile file = fm.retrieve(localFileId);
      if (file == null) {
        throw new RuntimeException("File " + localFileId + " unknown.");
      }
    }
    
    public String getOriginalFileName() {
      return originalFileName;
    }
    
  }



}

