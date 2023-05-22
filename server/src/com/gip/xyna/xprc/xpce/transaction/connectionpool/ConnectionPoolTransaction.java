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
package com.gip.xyna.xprc.xpce.transaction.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.db.pool.DefaultValidationStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xpce.transaction.TypedTransaction;

public class ConnectionPoolTransaction implements TypedTransaction {

  private ConnectionBundle bundle;
  private static ValidationStrategy validation;
  
  public ConnectionPoolTransaction(ConnectionBundle bundle) {
    this.bundle = bundle;
  }

  private Connection unwrap(Connection con) {
    if (con instanceof OperationPreventingConnection) {
      return ((OperationPreventingConnection) con).getWrappedConnection();
    } else {
      return con;
    }
  }
  
  public void commit() throws Exception {
    test(); //falls eine connection kaputt ist, gar keine committen, statt einer teilmenge
    for (Connection connection : bundle) {
      unwrap(connection).commit();
    }
  }

  public void rollback() throws Exception {
    test(); //falls eine connection kaputt ist, gar keine rollbacken, statt einer teilmenge
    for (Connection connection : bundle) {
      unwrap(connection).rollback();
    }
  }

  public void end() throws Exception {
    List<SQLException> exceptions = null;
    List<Throwable> otherExceptions = null;
    for (Connection connection : bundle) {
      try {
        unwrap(connection).close();
      } catch (SQLException e) {
        if (exceptions == null) {
          exceptions = new ArrayList<>();
        }
        exceptions.add(e);
      } catch (Throwable t) {
        if (otherExceptions == null) {
          otherExceptions = new ArrayList<>();
        }
        otherExceptions.add(t);
      }
    }
    if (exceptions != null) {
      if (otherExceptions != null) {
        Throwable[] ts = new Throwable[exceptions.size() + otherExceptions.size()];
        for (int i = 0; i < exceptions.size(); i++) {
          ts[i] = exceptions.get(i);
        }
        for (int i = exceptions.size(); i < ts.length; i++) {
          ts[i] = otherExceptions.get(i - exceptions.size());
        }
        throw new SQLException("Multiple Connections could not be closed", new XynaException("placeholder").initCauses(ts));
      } else {
        throw new SQLException("Multiple Connections could not be closed",
                               new XynaException("").initCauses(exceptions.toArray(new Throwable[0])));
      }
    } else if (otherExceptions != null) {
      throw new RuntimeException("Multiple Connections could not be closed",
                             new XynaException("").initCauses(otherExceptions.toArray(new Throwable[0])));
    }
  }

  
  private void test() throws Exception {
    for (Connection connection : bundle) {
      ValidationStrategy vs = getValidationStrategy();
      Exception e = vs.validate(unwrap(connection));
      if (e != null) {
        throw e;
      }
    }
  }
  
  
  private ValidationStrategy getValidationStrategy() {
    if (validation == null) {
      validation = new DefaultValidationStrategy(1000);
    }
    return validation;
  }


  public Connection getConnection() {
    return bundle.getConnection();
  }
  
  public Connection getConnection(int index) {
    return bundle.getConnection(index);
  }
  
  public Connection getConnection(String poolname) {
    return bundle.getConnection(poolname);
  }

  
}