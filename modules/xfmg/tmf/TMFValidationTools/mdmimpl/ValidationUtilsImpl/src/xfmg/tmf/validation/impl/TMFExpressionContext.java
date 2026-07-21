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
package xfmg.tmf.validation.impl;



import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.collections.Cache;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;



public class TMFExpressionContext {

  public final List<String> paths;
  public final RuntimeContext rtc;
  private List<DocumentContext> ctxs = new ArrayList<>();


  public TMFExpressionContext(String json, List<String> paths, RuntimeContext rtc) {
    this.rtc = rtc;
    addToContext(json);
    this.paths = paths;
  }


  private void addToContext(String json) {
    Configuration conf = Configuration.defaultConfiguration();
    conf = conf.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
    ctxs.add(JsonPath.using(conf).parse(json));
  }


  private void addToContext(Object previouslyParsedObject) {
    Configuration conf = Configuration.defaultConfiguration();
    conf = conf.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
    ctxs.add(JsonPath.using(conf).parse(previouslyParsedObject));
  }


  public Object eval(String path) {
    return eval(0, path);
  }


  public Object eval(int ctxIdx, String path) {
    try {
      return ctxs.get(ctxIdx).read(resolve(path));
    } catch (PathNotFoundException e) {
      if (isWhiteListed(e.getMessage())) {
        return null;
      }
      throw new RuntimeException("Json path not found: " + resolve(path), e);
    } catch (InvalidPathException e) {
      throw new RuntimeException("Invalid json path: " + resolve(path), e);
    }
  }


  public Object eval(String otherJson, String path) {
    try {
      return JsonPath.parse(otherJson).read(resolve(path));
    } catch (PathNotFoundException e) {
      if (isWhiteListed(e.getMessage())) {
        return null;
      }
      throw new RuntimeException("Json path not found: " + resolve(path), e);
    } catch (InvalidPathException e) {
      throw new RuntimeException("Invalid json path: " + resolve(path), e);
    }
  }


  private static final XynaPropertyString exceptionMessageWhiteListProperty =
      new XynaPropertyString("xfmg.tmf.validation.constraint.jsonpath.exception.whitelist",
                             "^Missing property in path.*$, ^No results for path:.*$")
                                 .setDefaultDocumentation(DocumentationLanguage.EN,
                                                          "Comma separated list of regular expressions that should each start and end with ^ and $. If during "
                                                              + "constraint validation a json path can not be evaluated successfully, exceptions "
                                                              + "(PathNotFoundException) that have a white listed message will be converted into returning null instead.");


  public static final Cache<String, Pattern> regexCache = new Cache<>(Pattern::compile);


  private boolean isWhiteListed(String exceptionMessage) {
    String propVal = exceptionMessageWhiteListProperty.get();
    String[] regexes = StringUtils.fastSplit(propVal, ',', -1);
    for (String regex : regexes) {
      if (regexCache.getOrCreate(regex.trim()).matcher(exceptionMessage).matches()) {
        return true;
      }
    }
    return false;
  }


  private String resolve(String path) {
    if (path.length() > 1 && path.charAt(0) == '$') {
      char c2 = path.charAt(1);
      /*
       * https://github.com/json-path/JsonPath
       * JsonPath expressions can use the dot notation
       * $.store.book[0].title
       * or the bracket notation
       * $['store']['book'][0]['title']
       */
      switch (c2) {
        case '.' :
        case '[' :
          //path directly as string argument
          return path;
        default :
          break;
      }
      //path as reference
      try {
        int idx = Integer.valueOf(path.substring(1));
        if (idx >= paths.size()) {
          throw new RuntimeException("path reference " + path + " could not be resolved. there are only " + paths.size()
              + " paths registered.");
        }
        return paths.get(idx);
      } catch (NumberFormatException e) {
        throw new RuntimeException("path reference " + path + " could not be resolved", e);
      }
    } else {
      throw new RuntimeException("Expected path reference (like $3) instead of <" + path + ">.");
    }
  }


  public void add(Object ctxObj) {
    addToContext(ctxObj);
  }


  public void pop() {
    ctxs.remove(ctxs.size() - 1);
  }

}
