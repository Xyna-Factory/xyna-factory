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
package xint.inference.impl.storage;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

import xint.inference.Download;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;



public class DownloadManager {

  private static final long SAVE_EVERY_BYTES = 2L * 1024 * 1024; // 2 MB

  private static final String DOWNLOAD_TYPE_MODEL = "model";
  private static final String DOWNLOAD_TYPE_SERVER = "inference server";

  private static Logger logger = CentralFactoryLogging.getLogger(DownloadManager.class);

  private final List<DownloadThread> downloadThreads;

  private final InferenceServerManagementRequestHistoryStorage requestHistoryStorage;


  public DownloadManager() {
    downloadThreads = new ArrayList<>();
    requestHistoryStorage = new InferenceServerManagementRequestHistoryStorage();
  }


  public List<Download> listDownloads() {
    List<Download> result = new ArrayList<>();
    String modelPath = InferenceServerMgmtServiceOperationImpl.MODEL_PATH.get();
    String serverPath = InferenceServerMgmtServiceOperationImpl.SERVER_PATH.get();
    List<Path> metaFiles = findMetaFiles(Path.of(modelPath));
    result.addAll(createDownloadInfo(metaFiles, DOWNLOAD_TYPE_MODEL));
    metaFiles = findMetaFiles(Path.of(serverPath));
    result.addAll(createDownloadInfo(metaFiles, DOWNLOAD_TYPE_SERVER));
    return result;
  }


  public void abortDownload(Download download) {
    synchronized (downloadThreads) {
      Optional<DownloadThread> thread = downloadThreads.stream().filter(x -> x.requestId == download.getRequestId()).findAny();
      if (thread.isPresent()) {
        thread.get().stopDownload();
        thread.get().interrupt();
        downloadThreads.remove(thread.get());
        String desc = String.format("Paused download of %s for deletion", thread.get().url);
        requestHistoryStorage.persistEntry(download.getRequestId(), desc);
      }

      Path path = determinePathOfDownload(download);
      Path partPath = path.resolveSibling(path.getFileName() + ".part");
      Path metaPath = path.resolveSibling(path.getFileName() + ".meta");
      try {
        deleteIfExists(partPath);
        deleteIfExists(metaPath);
      } catch (Exception e) {
        String desc = String.format("Failed to clean up %s after aborting download", path.toAbsolutePath().toString());
        requestHistoryStorage.persistEntry(download.getRequestId(), desc);
        throw new RuntimeException(e);
      }
      String desc = String.format("Sucessfully aborted download of %s", download.getUrl());
      requestHistoryStorage.persistEntry(download.getRequestId(), desc);
    }
  }


  public void pauseDownload(Download download) {
    synchronized (downloadThreads) {
      Optional<DownloadThread> thread = downloadThreads.stream().filter(x -> x.requestId == download.getRequestId()).findAny();
      if (thread.isEmpty()) {
        String desc = String.format("Pause request for download of %s was discarded, because download is not running", thread.get().url);
        requestHistoryStorage.persistEntry(download.getRequestId(), desc);
        return;
      } else {
        thread.get().stopDownload();
        thread.get().interrupt();
        downloadThreads.remove(thread.get());
        String desc = String.format("Paused download of %s", thread.get().url);
        requestHistoryStorage.persistEntry(download.getRequestId(), desc);
      }
    }
  }


  public void resumeDownload(Download download) {
    synchronized (downloadThreads) {
      Optional<DownloadThread> thread = downloadThreads.stream().filter(x -> x.requestId == download.getRequestId()).findAny();
      if (thread.isPresent()) {
        String desc = String.format("Resume request for %s was discarded, because download is already ongoing", thread.get().url);
        requestHistoryStorage.persistEntry(download.getRequestId(), desc);
        return;
      } else {
        Path finalPath = determinePathOfDownload(download);
        downloadAsync(download.getRequestId(), download.getUrl(), finalPath);
      }
    }
  }


  private Path determinePathOfDownload(Download download) {
    if (DOWNLOAD_TYPE_MODEL.equals(download.getDownloadType())) {
      return createModelPath(download.getRequestId(), download.getUrl());
    } else if (DOWNLOAD_TYPE_SERVER.equals(download.getDownloadType())) {
      return createInferenceServerPath(download.getRequestId(), download.getUrl());
    }
    String desc = String.format("Could not determine local path for download. Download type %s is unknown.", download.getDownloadType());
    requestHistoryStorage.persistEntry(download.getRequestId(), desc);
    throw new RuntimeException("Unknown downloadType: " + download.getDownloadType());
  }


  private List<Download> createDownloadInfo(List<Path> paths, String type) {
    List<Download> result = new ArrayList<>();
    for (Path p : paths) {
      try {
        Meta m = Meta.load(p);
        boolean running = downloadThreads.stream().filter(x -> x.url.equals(m.url)).findFirst().isPresent();
        result.add(new Download.Builder() //
            .status(running ? "running" : "paused") //
            .url(m.url) //
            .downloadType(type) //
            .progress(String.format("%d %%", ((m.downloaded * 100) / m.totalLength))) //
            .requestId(m.requestId) //
            .instance());
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Exception during download info creation for " + p.normalize().toAbsolutePath().toString(), e);
        }
        continue;
      }
    }
    return result;
  }


  private List<Path> findMetaFiles(Path rootDir) {
    try (Stream<Path> paths = Files.walk(rootDir)) {
      return paths.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".meta")).collect(Collectors.toList());
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }


  private Path createModelPath(long requestId, String url) {
    String modelName = url.substring(url.lastIndexOf("/"));
    String basePath = InferenceServerMgmtServiceOperationImpl.MODEL_PATH.get();
    return new File(basePath, modelName).toPath();
  }


  public void downloadModel(long requestId, String url) {
    if (!url.endsWith(".gguf")) {
      throw new RuntimeException("not a .gguf model");
    }
    downloadAsync(requestId, url, createModelPath(requestId, url));
  }


  private Path createInferenceServerPath(long requestId, String url) {
    String basePath = InferenceServerMgmtServiceOperationImpl.SERVER_PATH.get();
    String fileName = "downloads/" + requestId + "_inferenceServer_" + url.substring(url.lastIndexOf("/") + 1);
    return new File(basePath, fileName).toPath();
  }


  public void downloadInferenceServer(long requestId, String url) {
    downloadAsync(requestId, url, createInferenceServerPath(requestId, url));
  }


  private void downloadAsync(long requestId, String url, Path finalPath) {
    DownloadThread thread = new DownloadThread(url, requestId, finalPath);

    downloadThreads.add(thread);
    thread.start();
    for (int i = 0; i < 5; i++) {
      if (thread.hasWrittenMeta()) {
        return;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
  }


  private static void moveAtomicallyOrFallback(Path from, Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }


  private static long detectTotalLength(HttpResponse<InputStream> resp, long localSize, boolean append) {
    Optional<String> cr = resp.headers().firstValue("Content-Range");
    if (cr.isPresent()) {
      // Example: bytes 100-999/5000
      String s = cr.get();
      int slash = s.lastIndexOf('/');
      if (slash > 0 && slash + 1 < s.length()) {
        String total = s.substring(slash + 1).trim();
        if (!"*".equals(total)) {
          try {
            return Long.parseLong(total);
          } catch (NumberFormatException ignored) {
          }
        }
      }
    }

    Optional<String> cl = resp.headers().firstValue("Content-Length");
    if (cl.isPresent()) {
      try {
        long len = Long.parseLong(cl.get());
        return append ? localSize + len : len;
      } catch (NumberFormatException ignored) {
      }
    }
    return -1L;
  }


  private static long parseContentRangeStart(Optional<String> contentRange) throws IOException {
    if (contentRange.isEmpty()) {
      throw new IOException("Missing Content-Range for 206 response.");
    }
    // Expected: bytes start-end/total
    String s = contentRange.get().trim();
    if (!s.startsWith("bytes ")) {
      throw new IOException("Invalid Content-Range: " + s);
    }
    int space = s.indexOf(' ');
    int dash = s.indexOf('-', space + 1);
    if (dash < 0) {
      throw new IOException("Invalid Content-Range: " + s);
    }
    String start = s.substring(space + 1, dash).trim();
    try {
      return Long.parseLong(start);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid Content-Range start: " + s, e);
    }
  }


  private static void deleteIfExists(Path p) throws IOException {
    if (Files.exists(p)) {
      Files.delete(p);
    }
  }


  private static class Meta {

    long requestId;
    String url;
    String etag;
    String lastModified;
    long totalLength = -1L;
    long downloaded = 0L;


    public static Meta load(Path p) throws IOException {
      Meta m = new Meta();
      if (!Files.exists(p)) {
        return m;
      }
      Properties props = new Properties();
      try (InputStream in = Files.newInputStream(p)) {
        props.load(in);
      }
      m.requestId = parseLong(props.getProperty("requestId"), -1L);
      m.url = props.getProperty("url");
      m.etag = props.getProperty("etag");
      m.lastModified = props.getProperty("lastModified");
      m.totalLength = parseLong(props.getProperty("totalLength"), -1L);
      m.downloaded = parseLong(props.getProperty("downloaded"), 0L);
      return m;
    }


    public void save(Path path) throws IOException {

      if (!path.getParent().toFile().exists()) {
        Files.createDirectories(path.getParent());
      }

      Properties props = new Properties();
      props.setProperty("requestId", Long.toString(requestId));
      if (url != null) {
        props.setProperty("url", url);
      }
      if (etag != null) {
        props.setProperty("etag", etag);
      }
      if (lastModified != null) {
        props.setProperty("lastModified", lastModified);
      }
      props.setProperty("totalLength", Long.toString(totalLength));
      props.setProperty("downloaded", Long.toString(downloaded));

      try (OutputStream out = Files.newOutputStream(path)) {
        props.store(out, "resumable-download-metadata");
      }
    }


    private static long parseLong(String s, long fallback) {
      if (s == null)
        return fallback;
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        return fallback;
      }

    }
  }


  private static class DownloadThread extends Thread {

    private final String url;
    private final long requestId;
    private final Path finalPath;
    private boolean running;
    private boolean metaWritten;

    private final HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();


    public DownloadThread(String url, long requestId, Path finalPath) {
      super("Inference Server Management - Download " + url);
      this.url = url;
      this.requestId = requestId;
      this.finalPath = finalPath;
      this.running = true;
      this.metaWritten = false;
    }


    public void stopDownload() {
      running = false;
    }


    public boolean hasWrittenMeta() {
      return metaWritten;
    }


    @Override
    public void run() {
      InferenceServerManagementRequestHistoryStorage requestHistoryStorage = new InferenceServerManagementRequestHistoryStorage();

      try {
        String desc = String.format("Start/Resume download of %s.", url);
        requestHistoryStorage.persistEntry(requestId, desc);
        download(requestId, url, finalPath);
      } catch (InterruptedException e) {
        String desc = String.format("Interrupted download of %s.", url);
        requestHistoryStorage.persistEntry(requestId, desc);
      } catch (Exception e) {
        String desc = String.format("Unexpectedly interrupted download of %s.", url);
        requestHistoryStorage.persistEntry(requestId, desc);
      }
    }


    private void download(long requestId, String url, Path finalPath) throws IOException, InterruptedException {
      InferenceServerManagementRequestHistoryStorage requestHistoryStorage = new InferenceServerManagementRequestHistoryStorage();
      Path partPath = finalPath.resolveSibling(finalPath.getFileName() + ".part");
      Path metaPath = finalPath.resolveSibling(finalPath.getFileName() + ".meta");

      Meta meta = Meta.load(metaPath);
      long localSize = Files.exists(partPath) ? Files.size(partPath) : 0L;
      if (meta.url != null && !meta.url.equals(url.toString())) {
        deleteIfExists(partPath);
        deleteIfExists(metaPath);
        meta = new Meta();
        localSize = 0L;
      }
      meta.requestId = requestId;

      HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).GET();

      if (localSize > 0) {
        req.header("Range", "bytes=" + localSize + "-");
        if (meta.etag != null && !meta.etag.isBlank()) {
          req.header("If-Range", meta.etag);
        } else if (meta.lastModified != null && !meta.lastModified.isBlank()) {
          req.header("If-Range", meta.lastModified);
        }
      }


      HttpResponse<InputStream> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      int status = resp.statusCode();

      boolean isResumeAttempt = localSize > 0;
      boolean append = false;


      if (!isResumeAttempt) {
        if (status != 200) {
          throw new IOException("Expected 200 for new download, got " + status);
        }
        append = false;
      } else {
        if (status == 206) {
          // Resume accepted. Validate server start offset.
          long rangeStart = parseContentRangeStart(resp.headers().firstValue("Content-Range"));
          if (rangeStart != localSize) {
            throw new IOException("Server resumed from wrong offset. local=" + localSize + ", server=" + rangeStart);
          }

          String newEtag = resp.headers().firstValue("ETag").orElse(null);
          String newLm = resp.headers().firstValue("Last-Modified").orElse(null);

          if (meta.etag != null && newEtag != null && !meta.etag.equals(newEtag)) {
            throw new IOException("ETag changed during resume; remote file likely changed.");
          }
          if (meta.lastModified != null && newLm != null && !meta.lastModified.equals(newLm)) {
            throw new IOException("Last-Modified changed during resume; remote file likely changed.");
          }

          append = true;
        } else if (status == 200) {
          // Server ignored range or file changed. Restart from scratch.
          deleteIfExists(partPath);
          localSize = 0L;
          append = false;
        } else {
          throw new IOException("Unexpected status for resume attempt: " + status);
        }
      }


      // Create/update metadata from current response headers.
      meta.url = url.toString();
      meta.etag = resp.headers().firstValue("ETag").orElse(meta.etag);
      meta.lastModified = resp.headers().firstValue("Last-Modified").orElse(meta.lastModified);
      meta.totalLength = detectTotalLength(resp, localSize, append);
      meta.downloaded = localSize;
      meta.save(metaPath);
      metaWritten = true;

      // Ensure parent exists.
      if (finalPath.getParent() != null) {
        Files.createDirectories(finalPath.getParent());
      }


      try (InputStream in = resp.body(); RandomAccessFile raf = new RandomAccessFile(partPath.toFile(), "rw")) {
        if (append) {
          raf.seek(localSize);
        } else {
          raf.setLength(0L);
        }

        byte[] buf = new byte[64 * 1024];
        long sinceLastSave = 0L;
        int read;
        while ((read = in.read(buf)) != -1) {
          if (!running) {
            return;
          }
          raf.write(buf, 0, read);
          meta.downloaded += read;
          sinceLastSave += read;

          if (sinceLastSave >= SAVE_EVERY_BYTES) {
            meta.save(metaPath);
            sinceLastSave = 0L;
          }
        }
      }


      // Final integrity check when total length is known.
      long finalSize = Files.size(partPath);
      if (meta.totalLength > 0 && finalSize != meta.totalLength) {
        meta.downloaded = finalSize;
        meta.save(metaPath);
        throw new IOException("Download incomplete. Have " + finalSize + " of " + meta.totalLength + " bytes.");
      }

      moveAtomicallyOrFallback(partPath, finalPath);
      deleteIfExists(metaPath);


      String desc = String.format("Download of %s complete", url);
      requestHistoryStorage.persistEntry(requestId, desc);

      // validate and handle inference server downloads
      String serverPath = InferenceServerMgmtServiceOperationImpl.SERVER_PATH.get();
      if (finalPath.toAbsolutePath().normalize().startsWith(Path.of(serverPath).toAbsolutePath().normalize())) {
        handleInferenceServerDownloadCompletion(finalPath, requestId);
      }
    }


    private void handleInferenceServerDownloadCompletion(Path finalPath, long requestId) throws IOException {
      InferenceServerManagementRequestHistoryStorage requestHistoryStorage = new InferenceServerManagementRequestHistoryStorage();
      String desc;
      Collection<InferenceServerManagement> mgmts = InferenceServerManagementRegistry.getInstance().getAllServerMgmt();
      for (InferenceServerManagement mgmt : mgmts) {
        if (mgmt.handleDownload(finalPath)) {
          desc = String.format("Inference server download handled by %s", mgmt.serverType());
          requestHistoryStorage.persistEntry(requestId, desc);
          deleteIfExists(finalPath);
          return;
        }
      }
      desc = String.format("No inference server type could handle download. Deleting file.");
      requestHistoryStorage.persistEntry(requestId, desc);
      deleteIfExists(finalPath);
    }
  }


  public void shutdown() {
    for (Thread t : downloadThreads) {
      t.interrupt();
    }
  }
}
