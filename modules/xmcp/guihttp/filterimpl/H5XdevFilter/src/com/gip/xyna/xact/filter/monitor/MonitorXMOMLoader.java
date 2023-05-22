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

package com.gip.xyna.xact.filter.monitor;

import java.util.Map;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.StringXMLSource;


public class MonitorXMOMLoader extends XMOMLoader {
  
  private StringXMLSource inputSource;
  
  public MonitorXMOMLoader(Map<String, String> xmlsWfAndImports) {
    inputSource = new StringXMLSource(xmlsWfAndImports);
  }
  
  @Override
  public GenerationBaseObject load(FQName fqName, boolean readOnly) throws XynaException {
    try {
      GenerationBase gb = GenerationBase.getOrCreateInstance(fqName.getFqName(), new GenerationBaseCache(), fqName.getRevision(), inputSource);
      gb.parseGeneration(false/*saved*/, false, false);
  
      return createGBO(fqName, gb);
    } catch (Ex_FileAccessException ex) {
      if("base.AnyType".equals(fqName.getFqName())) { // TODO PMOD-149
        throw new RuntimeException(fqName.getFqName() + " not supported yet.");
      } else {
        throw ex;
      }
    } catch (XynaException ex) {
      throw ex;
    }
  }

}
