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
package xint.inference.impl.supportedservers;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;

import xint.inference.InferenceServer;
import xint.inference.InferenceServerConfiguration;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;
import xint.inference.impl.ProcessInfo;
import xint.inference.impl.ProcessInteraction;
import xint.inference.impl.storage.InferenceServerManagementRequestHistoryStorage;



public class LlamaCppServerManagement implements InferenceServerManagement {


  private static final Pattern versionPattern = Pattern.compile("version:\\s*(\\d+)\\b");
  private static Logger logger = CentralFactoryLogging.getLogger(LlamaCppServerManagement.class);


  @Override
  public String serverType() {
    return "llama.cpp";
  }


  @Override
  public Long getPid(InferenceServerConfiguration serverConfig, List<ProcessInfo> processes) {
    String expectedArgString = createArguments(serverConfig);
    for (ProcessInfo process : processes) {
      if (!process.getCommand().endsWith("llama-server")) {
        continue;
      }

      String argsAsString = String.join(" ", process.getArgs());
      if (!Objects.equals(argsAsString, expectedArgString)) {
        continue;
      }
      return process.getPid();
    }
    return null;
  }


  private String createArguments(InferenceServerConfiguration serverConfig) {
    String ctxSize = serverConfig.getContextWindowSize() == 0 ? "" : " -c " + serverConfig.getContextWindowSize();
    String modelPath = Path.of(InferenceServerMgmtServiceOperationImpl.MODEL_PATH.get()).toAbsolutePath().normalize().toString();
    String model = new File(modelPath, serverConfig.getModel()).toString();
    int port = serverConfig.getPort();
    String additionals = serverConfig.getAdditionalParameters() == null ? "" : " " + serverConfig.getAdditionalParameters();
    return String.format("-m %s%s --port %d%s", model, ctxSize, port, additionals);
  }


  private Path getLlamaccpPath() {
    String basePath = InferenceServerMgmtServiceOperationImpl.SERVER_PATH.get();
    return Path.of(basePath, "llamacpp").normalize().toAbsolutePath();
  }


  @Override
  public List<InferenceServer> listServers() {
    if (!Files.exists(getLlamaccpPath())) {
      return Collections.emptyList();
    }
    return listServers(getLlamaccpPath());
  }


  private List<InferenceServer> listServers(Path path) {
    try (Stream<Path> paths = Files.list(path)) {
      return paths.map(this::convertToInferenceServer).filter(x -> x != null).collect(Collectors.toList());
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }


  private InferenceServer convertToInferenceServer(Path p) {
    InferenceServer.Builder builder = new InferenceServer.Builder();
    String version = readVersionInfo(p);
    if (version == null) {
      return null;
    }
    String info = determineInfo(p);
    String id = String.format("%s/%s/%s", serverType(), version, info);
    builder.id(id);
    builder.serverVersion(version);
    builder.type(serverType());
    builder.information(info);
    return builder.instance();
  }


  private String determineInfo(Path path) {
    try {
      if (existsMatchingFile(path, "*cuda*")) {
        return "cuda";
      }
      if (existsMatchingFile(path, "*vulkan*")) {
        return "vulkan";
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Exception checking server type at " + path.toAbsolutePath().normalize().toString(), e);
      }
    }
    return "cpu";
  }


  public static boolean existsMatchingFile(Path rootDir, String globPattern) throws IOException {
    if (!Files.isDirectory(rootDir)) {
      return false;
    }

    PathMatcher matcher = rootDir.getFileSystem().getPathMatcher("glob:" + globPattern);

    try (Stream<Path> paths = Files.walk(rootDir)) {
      return paths.filter(Files::isRegularFile).anyMatch(path -> matcher.matches(path.getFileName()) || matcher.matches(path));
    }
  }


  private String readVersionInfo(Path path) {
    if (!Files.exists(Path.of(path.toString(), "llama-server"))) {
      return null;
    }
    ProcessBuilder processBuilder = new ProcessBuilder(path.toString() + "/llama-server", "--version");
    processBuilder.redirectErrorStream(true);

    try {
      Process process = processBuilder.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          Matcher matcher = versionPattern.matcher(line);

          if (matcher.find()) {
            return matcher.group(1);
          }
        }
      }

      int exitCode = process.waitFor();
      if (exitCode != 0) {
        return null;
      }
    } catch (Exception e) {
      return null;
    }


    return null;
  }


  private Path findPathForVersion(String version) {
    if (!Files.exists(getLlamaccpPath())) {
      throw new RuntimeException("Could not find path to executable. Directory does not exist");
    }
    try {
      List<Path> paths = Files.list(getLlamaccpPath()).collect(Collectors.toList());
      for (Path p : paths) {
        String currentVersion = readVersionInfo(p);
        if (Objects.equals(currentVersion, version)) {
          return p;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not find path to executable", e);
    }
    throw new RuntimeException("Could not find path to llama-server executable for version " + version);
  }


  private boolean checkHealth(int port) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/health")).timeout(Duration.ofSeconds(5)).GET().build();

    int maxRetries = 10;
    long retryDelayMillis = 500;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 503) {
          throw new ConnectException();
        }
        return response.statusCode() == 200;
      } catch (ConnectException e) {
        if (attempt == maxRetries) {
          return false;
        }

        try {
          Thread.sleep(retryDelayMillis);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return false;
        }
      } catch (Exception e) {
        return false;
      }
    }

    return false;
  }


  @Override
  public boolean start(long requestId, InferenceServerConfiguration serverConfig) {
    InferenceServerManagementRequestHistoryStorage storage = new InferenceServerManagementRequestHistoryStorage();
    
    try {
      String shell = System.getProperty("SHELL");
      if (shell == null || shell.isBlank()) {
        shell = "sh";
      }
      String pathForVersion = findPathForVersion(serverConfig.getServerVersion()).toString();
      String pathToBin = new File(pathForVersion, "llama-server").toPath().toAbsolutePath().normalize().toString();
      String arguments = createArguments(serverConfig);
      String program = String.format("%s %s &", pathToBin, arguments);
      
      new ProcessBuilder(shell, "-c", program).start();
      if (!checkHealth(serverConfig.getPort())) {
        if (logger.isWarnEnabled()) {
          logger.warn("Could not start server " + serverConfig + " reqId: " + requestId + ", because healthcheck failed");
        }
        throw new RuntimeException("service did not start correctly");
      } else {
        Long pid = getPid(serverConfig, ProcessInteraction.listProcesses());
        if (pid == null) {
          if (logger.isWarnEnabled()) {
            logger.warn("Could not start server " + serverConfig + " reqId: " + requestId + ", because pid could not be found");
          }
          throw new RuntimeException("service did not start correctly");
        }
        String desc = String.format("Started inference server %s/%s and model %s on port %d with pid %d", //
                                    serverConfig.getServerType(), serverConfig.getServerVersion(), serverConfig.getModel(), //
                                    serverConfig.getPort(), pid);
        storage.persistEntry(requestId, desc);

      }
    } catch (Exception e) {
      String desc = String.format("Failed to start inference server %s/%s and model %s on port %d", //
                                  serverConfig.getServerType(), serverConfig.getServerVersion(), serverConfig.getModel(), //
                                  serverConfig.getPort());
      storage.persistEntry(requestId, desc);
      return false;
    }
    return true;
  }


  @Override
  public boolean stop(long requestId, InferenceServerConfiguration serverConfig) {
    InferenceServerManagementRequestHistoryStorage storage = new InferenceServerManagementRequestHistoryStorage();
    Optional<ProcessHandle> handleOpt = ProcessHandle.of(serverConfig.getPid());
    if (handleOpt.isEmpty()) {
      String desc = String.format("Could not stop process with pid %d. Process not found", serverConfig.getPid());
      storage.persistEntry(requestId, desc);
      return false;
    }
    ProcessHandle handle = handleOpt.get();
    handle.destroy();
    handle.onExit().orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> null).join();
    if (!handle.isAlive()) {
      String desc = String.format("Gracefully terminated process %d", serverConfig.getPid());
      storage.persistEntry(requestId, desc);
      return true;
    }

    handle.destroyForcibly();
    handle.onExit().orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> null).join();
    if (!handle.isAlive()) {
      String desc = String.format("Forcefully terminated process %d", serverConfig.getPid());
      storage.persistEntry(requestId, desc);
      return true;
    }

    String desc = String.format("Failed to terminate process %d", serverConfig.getPid());
    storage.persistEntry(requestId, desc);
    return false;
  }


  @Override
  public boolean deleteServer(InferenceServer server) {
    Path path = getLlamaccpPath();
    File dir = null;
    try {
      List<Path> candidates = Files.list(path).filter(x -> x.toFile().isDirectory()).collect(Collectors.toList());
      for (Path candidate : candidates) {
        InferenceServer candidateServer = convertToInferenceServer(candidate);
        if (server == null) {
          continue;
        }
        if (Objects.equals(candidateServer.getId(), server.getId())) {
          dir = candidate.toFile();
          break;
        }
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to delete inference Server " + server.getId() + " at " + path.toAbsolutePath().normalize().toString(), e);
      }
      return false;
    }
    if (dir == null) {
      return false;
    }
    return FileUtils.deleteDirectoryRecursively(dir);
  }


  @Override
  public boolean handleDownload(Path path) {
    if (!path.toString().endsWith(".tar.gz")) {
      return false;
    }
    Path tmpDir = null;
    try {
      tmpDir = new File(path.getParent().normalize().toAbsolutePath().toString(), //
                        path.getFileName().toString() + "_llamaCpp_check").toPath().normalize().toAbsolutePath();
      extractTarGz(path, tmpDir);
      if (listServers(tmpDir).size() > 0) {
        Path targetDir = getLlamaccpPath();
        //Can't use fileUtils, because they don't copy access rights
        copyDirectoryReplaceExisting(tmpDir, targetDir);
        return true;
      }
    } catch (Exception e) {
      return false;
    } finally {
      if (tmpDir != null) {
        FileUtils.deleteDirectoryRecursively(tmpDir.toFile());
      }
    }

    return false;
  }


  public static void copyDirectoryReplaceExisting(Path sourceDir, Path targetDir) throws IOException {
    if (!Files.isDirectory(sourceDir)) {
      throw new IllegalArgumentException("Source is not a directory: " + sourceDir);
    }

    Files.createDirectories(targetDir);

    Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(dir);
        Path targetSubDir = targetDir.resolve(relative);
        Files.createDirectories(targetSubDir);
        return FileVisitResult.CONTINUE;
      }


      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relative = sourceDir.relativize(file);
        Path targetFile = targetDir.resolve(relative);

        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        return FileVisitResult.CONTINUE;
      }
    });
  }


  public static void extractTarGz(Path archive, Path targetDir) throws IOException, InterruptedException {
    if (!Files.exists(archive)) {
      throw new IOException("Archive does not exist: " + archive);
    }
    Files.createDirectories(targetDir);

    // Ensure tar is available
    ProcessInteraction.runOrThrow(List.of("tar", "--version"), "tar is not available on PATH");

    // Extract .tar.gz into target directory
    ProcessInteraction.runOrThrow(List.of("tar", "-xzf", archive.toAbsolutePath().toString(), "-C", targetDir.toAbsolutePath().toString()),
                                  "tar extraction failed");
  }


}
