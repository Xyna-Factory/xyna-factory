/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.filemgmt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.streams.StreamUtils.BufferWorthy;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;


/**
 *
 */
public class FileManagement extends FunctionGroup {
  
  private static Logger logger = CentralFactoryLogging.getLogger(FileManagement.class);
  
  public static final String DEFAULT_NAME = "FileManagement";
  
  private final static SleepCounter BACKOFF = new SleepCounter(25, 500, 5);

  
  private final ScheduledExecutorService timeoutExecutor;
  private final ConcurrentMap<String, InternalTransientFile> registeredFiles;
  
  private File tmpDir;
  private IDGenerator idGenerator;
  

  public FileManagement() throws XynaException {
    super();
    timeoutExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
        return new Thread(r, "Scheduled Timeout Executor Thread");
      }
    });
    registeredFiles = new ConcurrentHashMap<String, InternalTransientFile>();
  }
  
  
  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(FileManagement.class,"FileManagement.initFileMgmt").
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() { initFileMgmt(); }});
  }
  
  private void initFileMgmt() {
    XynaProperty.FILE_MANAGEMENT_TEMP_DIR.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    File tmpDir = new File(XynaProperty.FILE_MANAGEMENT_TEMP_DIR.readOnlyOnce());
    try {
      initFileMgmt(tmpDir, IDGenerator.getInstance());
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  // extracted for TestCase, called via reflection
  private void initFileMgmt(File tmpDir, IDGenerator idGenerator) {
    this.tmpDir = tmpDir;
    if( ! tmpDir.exists() ) {
      tmpDir.mkdirs();
    }
    this.idGenerator = idGenerator;
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }
  

  @Override
  protected void shutdown() throws XynaException {
    for (String id : registeredFiles.keySet()) {
      remove(id);
    }
  }
  


  public String store(String location, String fileName, InputStream inputStream) {
    Triple<String,OutputStream,String> triple  = store(location, fileName);
    OutputStream os = triple.getSecond();
    try {
      StreamUtils.copy(inputStream, os);
    } catch( IOException e ) {
      logger.warn("Could not store file "+triple.getThird(), e);
      throw new RuntimeException(e);
    } finally {
      try {
        os.close();
      } catch (IOException e) {
        logger.warn("Failed to close stream",e);
      }
    }
    return triple.getFirst();
  }
  
  public Triple<String,OutputStream,String> store(String location, String fileName) {
    InternalTransientFile itf = createTransientFile(location, fileName);
    
    logger.info("Storing "+fileName+ " in "+itf.getAbsolutePath());
    if (registeredFiles.putIfAbsent(itf.getId(), itf) != null) {
      itf.cancel(false);
      throw new RuntimeException("ID collison!");
    } else {
      return Triple.of(itf.getId(), itf.openOutputStream(), itf.getAbsolutePath());
    }
  }


  private InternalTransientFile createTransientFile(String location, String fileName) {
    String absLocation = createLocation(location);
    String id = String.valueOf(idGenerator.getUniqueId());
    File file = new File(absLocation, id);
    
    Duration timeout = XynaProperty.FILE_MANAGEMENT_DEFAULT_TIMEOUT.get();
    TimeUnit unit = TimeUnit.SECONDS; // at least 1 sec delay to prevent us from trying to delete before uploading
    ScheduledFuture<Boolean> futureTimeout = timeoutExecutor.schedule(new TimeoutCallable(id), Math.max(1, timeout.getDuration(unit)), unit);
    
    return new InternalTransientFile(id, fileName, file.getAbsolutePath(), futureTimeout);
  }


  public boolean remove(String id) {
    InternalTransientFile itf = registeredFiles.get(id);
    if (itf != null) {
      logger.info("Removing "+ itf.getOriginalFilename() + " stored as "+itf.getAbsolutePath());
      if (itf.delete()) {
        registeredFiles.remove(id);
        return true;
      } else {
        return false;
      }
    } else {
      logger.debug("No file with id " +  id + " registered.");
      return true;
    }
  }
  
  
  public TransientFile retrieve(String id) {
    InternalTransientFile itf = getRegisteredFile(id);
    return new InternalTransientFileWrapper(itf);
  }
  
  public FileInfo getFileInfo(String id) {
    InternalTransientFile itf = getRegisteredFile(id);
    String location = ""; //TODO woher?
    return new FileInfo(id, itf.getOriginalFilename(), itf.getSize(), location );
  }

  public String getAbsolutePath(String id) {
    return getRegisteredFile(id).getAbsolutePath();
  }

  private InternalTransientFile getRegisteredFile(String id) {
    InternalTransientFile itf = registeredFiles.get(id);
    if (itf != null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Retrieving "+ itf.getOriginalFilename() + " stored as "+itf.getAbsolutePath());
      }
      return itf;
    } else {
      logger.debug("Retrieval of " + id + " failed, not registered.");
      throw new RuntimeException("File not found " + id);
    }
  }
  
  
  private String createLocation(String location) {
    String absLocation = null;
    if( location != null ) {
      absLocation = tmpDir.getAbsoluteFile()+File.separator+location+File.separator;
    } else {
      absLocation = tmpDir.getAbsoluteFile()+File.separator;
    }
    File dir = new File(absLocation);
    if( ! dir.exists() ) {
      dir.mkdirs();
    }
    return absLocation;
  }
  
  
  public static interface TransientFile {
    
    public String getId();
    
    public long getSize();
    
    public String getOriginalFilename();
    
    public InputStream openInputStream();
    
    public OutputStream openOutputStream();
    
  }
  
  
  private static enum TransientFileState {
    UPLOAD(-2), // START
    DOWNLOAD(1) {
      @Override
      public boolean validateState(int internalState) {
        return internalState > 0;
      }
    }, // READ
    DELETE(-1), // WRITE
    IDLE(0);

    private final int integerRepresentation;
    
    private TransientFileState(int integerRepresentation) {
      this.integerRepresentation = integerRepresentation;
    }
    
    public boolean validateState(int internalState) {
      return integerRepresentation == internalState;
    }
  }
  
  public static class PlainTransientFile implements TransientFile {
    
    protected final String id;
    protected final String originalFilename;
    protected final String absolutePath;
    protected transient Long size;
    
    
    public PlainTransientFile(String id, String filename, String absolutePath) {
      this.id = id;
      this.originalFilename = filename;
      this.absolutePath = absolutePath;
    }
    
    public String getId() {
      return id;
    }
    
    public String getOriginalFilename() {
      return originalFilename;
    }
    
    public InputStream openInputStream() {
      try {
        return new FileInputStream(new File(absolutePath));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    
    public boolean delete() {
      return FileUtils.deleteFileWithRetries(new File(absolutePath));
    }

    public long getSize() {
      if (size == null) {
        size = new File(absolutePath).length();
      }
      return size.longValue();
    }


    public OutputStream openOutputStream() {
      File file = new File(absolutePath);
      try {
        return new FileOutputStream(file);
      } catch (FileNotFoundException e) {
        try {
          file.createNewFile();
          return new FileOutputStream(file);
        } catch (IOException e1) {
          throw new RuntimeException(e);
        }
      }
    }
    
  }
  
  
  /*
   * if we want refresh functionality we could track the TimeoutCallable and set a reschedule flag that makes it reschedule itself instead of removing
   */
  private final static class InternalTransientFile extends PlainTransientFile {
    
    protected final ScheduledFuture<Boolean> timeout;
    /* > 0: # concurrent downloads
     * 0: IDLE
     * -1: DELETE
     * -2: UPLOAD
     */
    private final AtomicInteger state;
    
    public InternalTransientFile(String id, String filename, String absolutePath, ScheduledFuture<Boolean> timeout) {
      super(id, filename, absolutePath);
      this.timeout = timeout;
      state = new AtomicInteger(-2);
    }
    
    
    public void cancel(boolean mayInterruptIfRunning) {
      timeout.cancel(mayInterruptIfRunning);
    }


    protected boolean progressState(TransientFileState newState) {
      SleepCounter backoff = FileManagement.BACKOFF.clone();
      boolean success = false;
      while (!success) {
        int oldState = state.get();
        switch (newState) {
          case DOWNLOAD :
            if (oldState >= 0) {
              success = state.compareAndSet(oldState, oldState + 1);
            } else if (oldState == -1) {
              return false;
            }
            break;
          case DELETE :
            if (oldState == 0) {
              success = state.compareAndSet(0, -1);
            } else if (oldState == -1) {
              return true;
            }
            break;
          case IDLE :
            if (oldState > 0) {
              state.decrementAndGet();
              success = true;
            } else if (oldState < 0) {
              success = state.compareAndSet(oldState, 0);
            } else if (oldState == 0) {
              success = true;
            }
            break;
          default :
            break;
        }
        if (!success) {
          // property.get could be outside the loop, having it inside makes endless retries abortable
          int maxRetries = XynaProperty.FILE_MANAGEMENT_STATE_TRANSITION_RETRIES.get();
          if (newState == TransientFileState.DELETE) {
          }
          if (maxRetries >= 0 && backoff.iterationCount() >= maxRetries) {
            return false;
          }
          try {
            backoff.sleep();
          } catch (InterruptedException e) {
            logger.debug(e);
            return false;
          }
        }
      }
      return success;
    }
    
    
    @Override
    public InputStream openInputStream() {
      if (progressState(TransientFileState.DOWNLOAD)) {
        return new BufferedInputStream(new TransientFileInputStream(super.openInputStream()));
      } else {
        throw new RuntimeException("Failed to open stream!");
      }
    }
    
    public OutputStream openOutputStream() {
      if (TransientFileState.UPLOAD.validateState(state.get())) {
        return new BufferedOutputStream(new TransientFileOutputStream(super.openOutputStream()));
      } else {
        throw new RuntimeException("Failed to open stream!");
      }
    }
    

    public boolean delete() {
      if (progressState(TransientFileState.DELETE)) {
        timeout.cancel(true);
        return super.delete();
      } else {
        return false;
      }
    }
    
    
    private class TransientFileInputStream extends InputStream implements BufferWorthy {
      
      private final InputStream wrappedStream;
      private final AtomicBoolean open;
      
      private TransientFileInputStream(InputStream stream) {
        this.wrappedStream = stream;
        open = new AtomicBoolean(true);
      }

      public int read() throws IOException {
        return wrappedStream.read();
      }
      
      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return wrappedStream.read(b, off, len);
      }

      @Override
      public int read(byte[] b) throws IOException {
        return wrappedStream.read(b);
      }

      public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
          InternalTransientFile.this.progressState(TransientFileState.IDLE);
          wrappedStream.close();
        }
        super.close();
      }
      
      protected void finalize() throws Throwable {
        close();
        super.finalize();
      }
      
      public int available() throws IOException {
        return wrappedStream.available();
      }
      
    }
    
    public String getAbsolutePath() {
      return absolutePath;
    }
    
    private class TransientFileOutputStream extends OutputStream implements BufferWorthy {
      
      private final OutputStream wrappedStream;
      private final AtomicBoolean open;
      
      private TransientFileOutputStream(OutputStream wrappedStream) {
        this.wrappedStream = wrappedStream;
        open = new AtomicBoolean(true);
      }

      public void write(int b) throws IOException {
        wrappedStream.write(b);
      }
      
      @Override
      public void flush() throws IOException {
        wrappedStream.flush();
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        wrappedStream.write(b, off, len);
      }

      @Override
      public void write(byte[] b) throws IOException {
        wrappedStream.write(b);
      }

      protected void finalize() throws Throwable {
        close();
        super.finalize();
      }
      
      public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
          boolean success = InternalTransientFile.this.progressState(TransientFileState.IDLE);
          if (!success) {
            logger.debug("Failed to progressState to IDLE of TransientFile " + InternalTransientFile.this.getId());
          }
          wrappedStream.close();
        }
        super.close();
      }
    
    }
    
  }
  
  
  private static class InternalTransientFileWrapper implements TransientFile {
    
    private final InternalTransientFile wrappedFile;
    private transient InputStream is;
    private transient OutputStream os;
    
    protected InternalTransientFileWrapper(InternalTransientFile file) {
      wrappedFile = file;
    }

    public String getId() {
      return wrappedFile.getId();
    }

    public String getOriginalFilename() {
      return wrappedFile.getOriginalFilename();
    }

    public InputStream openInputStream() {
      if (is == null) {
        is = wrappedFile.openInputStream();
      }
      return is;
    }

    public long getSize() {
      return wrappedFile.getSize();
    }

    public OutputStream openOutputStream() {
      if (os == null) {
        os = wrappedFile.openOutputStream();
      }
      return os;
    }
    
  }
  
  
  private final class TimeoutCallable implements Callable<Boolean> {
    
    private final String id;
    
    public TimeoutCallable(String id) {
      this.id = id;
    }

    public Boolean call() throws Exception {
      try {
        return FileManagement.this.remove(id);
      } catch (RuntimeException e) {
        throw e;
      }
    }
    
  }


  public void copyToDir(File tmpDir, String fileId, boolean originalName) throws Ex_FileAccessException {
    InternalTransientFile file = getRegisteredFile(fileId);
    File out = new File( tmpDir, originalName ? file.getOriginalFilename() : fileId );
    FileUtils.copyFile( new File(file.getAbsolutePath()), out, true);
  }
  
  /*
  
  public String store( String prefix, List<File> files );
  
  public String store( String prefix, List<InputStream> streams );
  
  public boolean remove( List<String> ids );
  
  public boolean refresh( String id );
  
  public List<File> retrieveAsFiles( List<String> ids );
  
  public List<File> retrieveAsStreams( List<String> ids );
  
 */
  
}
