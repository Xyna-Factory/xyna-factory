/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.python;



import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;


public abstract class PythonInterpreterFactory {

  public abstract PythonInterpreter createInterperter(ClassLoaderBase classloader);

  public abstract void init();

  public abstract void invalidateRevisions(Collection<Long> revisions);
  
  public abstract Map<String, Object> convertToPython(GeneralXynaObject obj);

  public abstract GeneralXynaObject convertToJava(Context context, Object obj);
  
  public abstract Object convertToJava(Context context, String type, Object value);

  public abstract Object convertToPython(Object value);

  public abstract Object invokeService(Context context, String fqn, String serviceName, List<Object> args);

  public abstract Object invokeInstanceService(Context context, Object obj, String serviceName, List<Object> args);
}
