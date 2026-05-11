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

import com.gip.xyna.CentralFactoryLogging;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import org.apache.log4j.Logger;

public class XynaFileChannel extends FileChannel {
  static final Logger logger = CentralFactoryLogging.getLogger(XynaFileChannel.class);

  private final XynaFilterDelegatingPath path;

  private final long size;

  private long position = 0L;

  private final XynaBackedFile file;

  public XynaFileChannel(XynaFilterDelegatingPath path, XynaBackedFile file) {
    this.path = path;
    this.file = file;
    this.position = 0L;
    this.size = file.size();
    if (logger.isDebugEnabled())
      logger.debug("got content, position: " + this.position + ", size: " + this.size);
  }

  public XynaFilterDelegatingPath getPath() {
    return this.path;
  }

  public int read(ByteBuffer dst) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("simple read position: " + this.position + ", size: " + this.size);
    int read = read(dst, this.position);
    if (read > 0)
      this.position += read;
    return read;
  }

  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("read interleaved position: " + this.position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'read'");
  }

  public int write(ByteBuffer src) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("simple write position: " + this.position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'write'");
  }

  public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("interleaved write position: " + this.position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'write'");
  }

  public long position() throws IOException {
    return this.position;
  }

  public FileChannel position(long newPosition) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("got new position: " + newPosition + " @ " + this.position + ", size: " + this.size);
    this.position = newPosition;
    return this;
  }

  public long size() throws IOException {
    return this.size;
  }

  public FileChannel truncate(long size) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("truncate to " + size + " position: " + this.position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'truncate'");
  }

  public void force(boolean metaData) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("force position: " + this.position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'force'");
  }

  public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("transferTo position: " + position + ", count: " + count);
    throw new UnsupportedOperationException("Unimplemented method 'transferTo'");
  }

  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("transferFrom position: " + position + ", count: " + count);
    throw new UnsupportedOperationException("Unimplemented method 'transferFrom'");
  }

  public int read(ByteBuffer dst, long readFrom) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("simple readfrom: " + readFrom + ", position: " + this.position + ", size: " + this.size);
    if (this.file == null) {
      if (logger.isDebugEnabled())
        logger.debug("no content available");
      return -1;
    }
    if (readFrom >= this.size) {
      if (logger.isDebugEnabled())
        logger.debug("read all content");
      return -1;
    }
    int from = Math.toIntExact(readFrom);
    int to = Math.min(from + dst.remaining(), Math.toIntExact(this.size));
    int bytesToRead = to - from;

    if (bytesToRead > 0) {
      dst.put(this.file.getContent(), from, bytesToRead);
      if (logger.isTraceEnabled())
        logger.trace("read " + (to - from));
      return bytesToRead;
    }
    if (logger.isDebugEnabled())
      logger.debug("nothing to read");
    return -1;
  }

  public int write(ByteBuffer src, long position) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("simple writeto position: " + position + ", size: " + this.size);
    throw new UnsupportedOperationException("Unimplemented method 'write'");
  }

  public MappedByteBuffer map(FileChannel.MapMode mode, long position, long size) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("map position: " + position + ", size: " + size);
    throw new UnsupportedOperationException("Unimplemented method 'map'");
  }

  public FileLock lock(long position, long size, boolean shared) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("lock position: " + position + ", size: " + size);
    throw new UnsupportedOperationException("Unimplemented method 'lock'");
  }

  public FileLock tryLock(long position, long size, boolean shared) throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("trylock position: " + position + ", size: " + size);
    throw new UnsupportedOperationException("Unimplemented method 'tryLock'");
  }

  protected void implCloseChannel() throws IOException {
    if (logger.isTraceEnabled())
      logger.trace("implCloseChannel position: " + this.position + ", size: " + this.size);
    if (this.file != null)
      this.position = Math.max(this.size - 1L, 0L);
  }
}
