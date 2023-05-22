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
package common;

import com.gip.xyna.utils.db.failover.FailoverSource;
import com.gip.xyna.utils.snmp.OID;

/**
 * FailoverSourceOID: Failover wird am Status des FailoverOidSingleHandler erkannt
 */
public class FailoverSourceOID implements FailoverSource {

  private OID oid;

  /**
   * Konstruktor f�r erste Registrierung in FailoverSources
   */
  public FailoverSourceOID() {/*nichts zu tun*/}
  
  /**
   * @param oid
   */
  public FailoverSourceOID(OID oid) {
    this.oid = oid;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.db.failover.FailoverSource#isFailover()
   */
  public boolean isFailover() {
    return FailoverOidSingleHandler.getInstance().isFailover( oid );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.db.failover.FailoverSource#newInstance(java.lang.String)
   */
  public FailoverSource newInstance(String failoverParam) {
    return new FailoverSourceOID( new OID( failoverParam ) );
  }

}
