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
package xact.http.impl;



import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import base.File;
import xact.http.HTTPResponseResolverServiceOperation;
import xact.http.HeaderField;
import xact.http.SendParameter;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.impl.JSONValue.Type;
import xact.templates.Document;



public class HTTPResponseResolverServiceOperationImpl implements ExtendedDeploymentTask, HTTPResponseResolverServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(HTTPResponseResolverServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    if (ws == null) {
      return;
    }
    cache.clear(); //unregister everything
    try {
      ws.close();
    } catch (IOException e) {
      logger.debug("Could not close WatchService", e);
    }
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public HTTPResponseResolverServiceOperationImpl() {
    if (ws == null) {
      try {
        ws = FileSystems.getDefault().newWatchService();
      } catch (IOException e) {
        logger.error("could not create watchservice");
      }
    }
  }


  static class DirKey {

    private final String dir;


    public DirKey(Path path) throws IOException {
      dir = path.toFile().getCanonicalFile().toString();
    }


    @Override
    public int hashCode() {
      return Objects.hash(dir);
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      DirKey other = (DirKey) obj;
      return Objects.equals(dir, other.dir);
    }

  }


  static WatchService ws;


  static class Condition {

    final String source;
    final String op;
    final String key;
    final JSONValue value;


    public Condition(String source, String op, String key, JSONValue value) {
      this.source = source;
      this.op = op;
      this.key = key;
      this.value = value;
    }


    static Condition[] parseConditions(JSONValue resp) {
      List<Condition> l = new ArrayList<>();
      for (JSONValue cond : resp.list) {
        // { "source": SOURCE, "op": OP[, "key": KEY], "value": VALUE }
        String source = cond.vals.get("source").stringVal;
        String op = cond.vals.get("op").stringVal;
        String key = cond.vals.getOrDefault("key", JSONValue.nullVal()).stringVal;
        JSONValue value = cond.vals.get("value");
        l.add(new Condition(source, op, key, value));
      }
      return l.toArray(new Condition[0]);
    }


    public boolean match(Document request, SendParameter sendParameter) {
      String resolvedSource = resolveSource(request, sendParameter);
      String valueString;
      if (value.type == Type.STRING) {
        valueString = value.stringVal;
      } else {
        valueString = value.toString();
      }

      if (op.equals("equals")) {
        return Objects.equals(resolvedSource, valueString);
      } else if (op.equals("regex")) {
        if (resolvedSource == null || valueString == null) {
          return false;
        }
        return resolvedSource.matches(valueString);
      }
      logger.warn("unexpected condition operation: " + op);
      return false;
    }


    private String resolveSource(Document request, SendParameter sendParameter) {
      if (source.equals("method")) {
        HTTPMethod method = sendParameter.getHTTPMethod();
        return method.getClass().getSimpleName();
      } else if (source.equals("url")) {
        return sendParameter.getURLPath().getPath();
      } else if (source.equals("header")) {
        Optional<? extends HeaderField> field =
            sendParameter.getHeader().getHeaderField().stream().filter(hf -> hf.getName().equals(key)).findFirst();
        if (field.isPresent()) {
          return field.get().getValue();
        } else {
          return null;
        }
      }
      return null;
    }
  }


  private static boolean allMatch(Condition[] conditions, Document request, SendParameter sendParameter) {
    for (Condition c : conditions) {
      if (!c.match(request, sendParameter)) {
        return false;
      }
    }
    return true;
  }


  static class FileCondition {

    private final Condition[] conditions;


    public FileCondition(String content) {
      try {
        conditions = Condition.parseConditions(new JSONFileContent(content).val.vals.get("fileMatch"));
      } catch (InvalidJSONException | UnexpectedJSONContentException | NullPointerException | ArrayIndexOutOfBoundsException e) {
        throw new RuntimeException("JSON could not be parsed successfully for the fileMatch", e);
      }
    }


    public boolean matches(Document request, SendParameter sendParameter) {
      return allMatch(conditions, request, sendParameter);
    }

  }

  static class CacheEntry {

    private boolean stale = true;
    private boolean erroneous = false;
    private FileCondition fc;
    //private String fileContent; could be optimized to cache the content as well


    //return success
    public boolean refresh(java.io.File file) {
      erroneous = false;
      String content;
      try {
        content = FileUtils.readFileAsString(file);
      } catch (Ex_FileWriteException e) {
        erroneous = true;
        return false;
      }
      stale = false;
      try {
        fc = new FileCondition(content);
      } catch (RuntimeException e) {
        logger.info("File " + file.getPath() + " contains invalid/unexpected json.", e);
        erroneous = true;
      }
      
      return true;
    }
  }


  public static class JSONFileContent {

    JSONValue val;


    JSONFileContent(String content) throws InvalidJSONException, UnexpectedJSONContentException {
      val = new JSONParser(content).parse(new JSONTokenizer().tokenize(content));
    }
  }

  public static class HTTPResponse {

    public String responseBody;
    public int responseCode;
    public String reason;


    public HTTPResponse() {

    }


    public HTTPResponse(Map<String, JSONValue> jsonobj) {
      try {
        responseBody = getBody(jsonobj, "response");
        responseCode = getCode(jsonobj, "responseCode");
        reason = getReason(jsonobj, "reason");
      } catch (Exception e) {
        logger.debug("Couldn't parse response", e);
        responseBody = "Couldn't parse response: " + e.getMessage();
        responseCode = 500;
        reason = "Internal Server Error";
      }
    }


    private String getReason(Map<String, JSONValue> jsonobj, String key) {
      JSONValue v = jsonobj.get(key);
      if (v == null) {
        switch (responseCode) {
          case 100 :
            return "Continue";
          case 200 :
            return "OK";
          case 201 :
            return "Created";
          case 202 :
            return "Accepted";
          case 204 :
            return "No Content";
          case 400 :
            return "Bad Request";
          case 401 :
            return "Unauthorized";
          case 403 :
            return "Forbidden";
          case 404 :
            return "Not Found";
          case 500 :
            return "Internal Server Error";
          case 501 :
            return "Not Implemented";
          case 503 :
            return "Service Unavailable";
          default :
            return "";
        }
      }
      if (v.type == Type.STRING) {
        return v.stringVal;
      } else if (v.type == Type.NULL) {
        return "";
      } else {
        throw new RuntimeException("JSON key <" + key + "> must be a string.");
      }
    }


    private int getCode(Map<String, JSONValue> jsonobj, String key) {
      JSONValue v = jsonobj.get(key);
      if (v == null) {
        return 200;
      }
      if (v.type == Type.NUMBER) {
        return Integer.valueOf(v.numberVal);
      } else if (v.type == Type.STRING) {
        return Integer.valueOf(v.stringVal);
      } else {
        throw new RuntimeException("JSON key <" + key + "> must be a number.");
      }
    }


    private String getBody(Map<String, JSONValue> jsonobj, String key) {
      JSONValue v = jsonobj.get(key);
      if (v == null) {
        return "";
      }
      if (v.type == Type.STRING) {
        return v.stringVal; //no "" around the string
      } else if (v.type == Type.NULL) {
        return "";
      }
      return v.toString(); //valid json
    }


    public static HTTPResponse _404() {
      HTTPResponse resp = new HTTPResponse();
      resp.responseBody = "";
      resp.responseCode = 404;
      resp.reason = "Not found";
      return resp;
    }
  }


  static class DirInfo extends ObjectWithRemovalSupport {


    private final long keepAlive = 3600 * 1000L;
    private long lastUsed = System.currentTimeMillis();
    private final String dir;
    private final WatchKey key;

    //cache the file conditions, so that typically on a non-stale cache we only need to access the one file
    private Map<String, CacheEntry> cache = new HashMap<>();


    public DirInfo(String dir) {
      this.dir = dir;
      Path path = Path.of(dir);
      try {
        key = path.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
        Files.list(path).filter(Files::isRegularFile).map(p -> p.getFileName()).forEach(p -> {
          cache.put(p.toString(), new CacheEntry());
        });

      } catch (IOException e) {
        throw new RuntimeException("Could not access dir " + dir, e);
      }
    }


    @Override
    protected boolean shouldBeDeleted() {
      return System.currentTimeMillis() - lastUsed > keepAlive;
    }


    public synchronized HTTPResponse resolve(Document request, SendParameter sendParameter) {
      return cache.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
        CacheEntry ce = e.getValue();
        if (ce.stale) {
          if (!ce.refresh(new java.io.File(dir, e.getKey()))) {
            //deleted? should be handled by another event that is just not processed yet
            return null;
          }
        } else if (ce.erroneous) {
          return null;
        }
        if (e.getValue().fc.matches(request, sendParameter)) {
          HTTPResponse response = resolveResponse(new java.io.File(dir, e.getKey()), request, sendParameter);
          if (response != null) {
            return response;
          } //else look for different file
        }
        return null;
      }).filter(Objects::nonNull).findFirst().orElse(HTTPResponse._404());
    }


    private HTTPResponse resolveResponse(java.io.File file, Document request, SendParameter sendParameter) {
      String content;
      try {
        content = FileUtils.readFileAsString(file);
      } catch (Ex_FileWriteException e) {
        logger.debug("File not found, but still in cache: " + file.getPath(), e);
        return null;
      }
      try {
        for (JSONValue resp : new JSONFileContent(content).val.vals.get("responses").list) {
          Condition[] conditions = Condition.parseConditions(resp.vals.get("match"));
          if (allMatch(conditions, request, sendParameter)) {
            return new HTTPResponse(resp.vals);
          }
        }
      } catch (InvalidJSONException | UnexpectedJSONContentException | NullPointerException | ArrayIndexOutOfBoundsException e) {
        logger.warn("File " + file.getPath() + " contains invalid/unexpected json.", e);
        return null;
      }

      return null; //no match
    }


    @Override
    protected void onDeletion() {
      super.onDeletion();
      key.cancel();
    }


    public void updateLastUsed() {
      lastUsed = System.currentTimeMillis();
    }


    public synchronized void updateAfterChanges(WatchKey wkf) {
      for (WatchEvent<?> we : wkf.pollEvents()) {
        String relativePath = ((Path) we.context()).toString();
        if (we.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
          cache.remove(relativePath);
        } else if (we.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
          cache.get(relativePath).stale = true;
        } else if (we.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          cache.put(relativePath, new CacheEntry());
        }
      }
    }

  }


  private static ConcurrentMapWithObjectRemovalSupport<DirKey, DirInfo> cache =
      new ConcurrentMapWithObjectRemovalSupport<DirKey, DirInfo>() {

        @Override
        public DirInfo createValue(DirKey dk) {
          return new DirInfo(dk.dir);
        }

      };


  //handle watch events
  private void updateCache() {
    WatchKey wk = null;
    while (null != (wk = ws.poll())) {
      Path path = (Path) wk.watchable();
      final WatchKey wkf = wk;
      try {
        cache.<Void> process(new DirKey(path), di -> {
          di.updateAfterChanges(wkf);
          return null;
        });
      } catch (IOException e) {
        throw new RuntimeException("Problem with dir: " + path.toString(), e);
      } finally {
        wk.reset();
      }
    }
  }


  public Container resolveResponse(final Document doc, final SendParameter sendParameter, File dir) {
    updateCache();
    Container result;
    try {
      result = cache.<Container> process(new DirKey(Path.of(dir.getPath())), di -> {
        di.updateLastUsed();
        Document outputDoc = new Document();
        HTTPResponse r = di.resolve(doc, sendParameter);
        outputDoc.setText(r.responseBody);
        return new Container(outputDoc, sendParameter.getHeader(), new HTTPStatusCode(r.responseCode, r.reason));
      });
    } catch (IOException e) {
      throw new RuntimeException("Problem with dir: " + dir.getPath(), e);
    }
    cleanup();
    return result;
  }


  private void cleanup() {
    //check if not in use -> cleanup
    for (DirKey k : new HashSet<>(cache.keySet())) {
      cache.<Void> process(k, fi -> null);
    }
  }

}
