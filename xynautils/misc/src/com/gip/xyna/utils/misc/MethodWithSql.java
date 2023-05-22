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
package com.gip.xyna.utils.misc;

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.utils.db.SQLUtils;


import java.rmi.RemoteException;

import org.apache.log4j.Logger;


/**
 * erweiterung von methodwrapper, so dass sqlutils bereitgestellt sind. die
 * connection wird am ende der methode commited oder im fehlerfall rollbacked.
 * und immer geschlossen.<p>
 * es bietet sich an, eine hiervon abgeleitete klasse zu benutzen, die createSQLUtils
 * projektspezifisch implementiert.
 */
public abstract class MethodWithSql<T, U> extends MethodWrapper<T, U> {

  private SQLUtils sqlUtils;

  public MethodWithSql(String methodName, Logger logger) {
    super(methodName, logger);
  }

  /**
   * erzeugung der sqlutils
   * @return
   * @throws Exception
   */
  public abstract SQLUtils createSQLUtils() throws Exception;

  public abstract U doStuff(T req) throws Exception;

  public U tryBlock(T req) throws Exception {
    U ret = super.tryBlock(req);
    if (sqlUtils != null) {
      sqlUtils.commit();
    }
    return ret;
  }

  public U onError(Exception e) throws XynaFault_ctype, RemoteException {
    if (sqlUtils != null) {
      sqlUtils.rollback();
    }
    return super.onError(e);
  }

  public void finallyBlock(U ret) {
    if (sqlUtils != null) {
      sqlUtils.closeConnection();
    }
    super.finallyBlock(ret);
  }

  /**
   * sicherer getter
   * @return
   * @throws Exception
   */
  public SQLUtils getSQLUtils() throws Exception {
    if (sqlUtils == null) {
      sqlUtils = createSQLUtils();
    }
    return sqlUtils;
  }

}
