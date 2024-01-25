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
package xmcp.gitintegration.impl;

import java.io.Closeable;
import java.io.IOException;

import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class TmpSessionAuthWrapper implements Closeable {

  private TemporarySessionAuthentication data;
  
  public TmpSessionAuthWrapper(String userName, String role) throws Exception {
    TemporarySessionAuthentication tsa = TemporarySessionAuthentication.tempAuthWithUniqueUser(userName, role);
    tsa.initiate();
    data = tsa;
  }
  
  @Override
  public void close() throws IOException {
    try {
      data.destroy();
    } catch (PersistenceLayerException e) {
    } catch (XFMG_PredefinedXynaObjectException e) {
    }
  }
  
  public TemporarySessionAuthentication getTSA() {
    return data;
  }

}
