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
package xint.inference.impl.services;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import xint.inference.Model;
import xint.inference.impl.InferenceServerMgmtServiceOperationImpl;



public class ListModels {

  public List<Model> listModels() {
    List<Model> result = new ArrayList<>();

    String modelPath = InferenceServerMgmtServiceOperationImpl.MODEL_PATH.get();
    if (!Files.exists(Path.of(modelPath))) {
      return Collections.emptyList();
    }

    List<Path> files;
    try {
      files = Files.list(Path.of(modelPath)).collect(Collectors.toList());
    } catch (IOException e) {
      files = Collections.emptyList();
    }
    for (Path p : files) {
      if (!p.toFile().isFile()) {
        continue;
      }
      if (p.getFileName() == null || !p.getFileName().toString().endsWith(".gguf")) {
        continue;
      }
      result.add(convertToModel(p));
    }

    return result;
  }


  private Model convertToModel(Path p) {
    Model.Builder builder = new Model.Builder();
    builder.name(p.getFileName().toString());
    String size = "";
    try {
      long sizeLong = Files.size(p);
      long sizeMb = sizeLong / (1024 * 1024);
      if (sizeMb > 1000) {
        size = String.format("%d G", sizeMb / 1024);
      } else {
        size = String.format("%d M", sizeMb);
      }
    } catch (IOException e) {
      size = "unknown";
    }
    builder.size(size);
    return builder.instance();
  }
}
