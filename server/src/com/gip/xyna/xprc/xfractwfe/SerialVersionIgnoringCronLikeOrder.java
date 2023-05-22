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
package com.gip.xyna.xprc.xfractwfe;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;


public class SerialVersionIgnoringCronLikeOrder extends CronLikeOrder {

  private static final long serialVersionUID = 5600866663259192093L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SerialVersionIgnoringCronLikeOrder.class);


  public SerialVersionIgnoringCronLikeOrder() {
  }
  
  public SerialVersionIgnoringCronLikeOrder(Long id) {
    super(id);
  }
  
  @Override
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    logger.trace("getObjectInputStreamForStorable: returning SerialVersionIgnoringObjectInputStream");
    return new SerialVersionIgnoringObjectInputStream(in, getRevision());
  }
  
  
  private static ResultSetReader<SerialVersionIgnoringCronLikeOrder> reader = new SerialVersionIgnoringReader();


  public static ResultSetReader<SerialVersionIgnoringCronLikeOrder> getSerialVersionIgnoringReader() {
    return reader;
  }
  
  private static class SerialVersionIgnoringReader implements ResultSetReader<SerialVersionIgnoringCronLikeOrder> {

    public SerialVersionIgnoringCronLikeOrder read(ResultSet rs) throws SQLException {
      
      SerialVersionIgnoringCronLikeOrder sviclo = new SerialVersionIgnoringCronLikeOrder();
      SerialVersionIgnoringCronLikeOrder.fillByResultSet(sviclo, rs, true /* do as much as possible and don't abort (it might be safer to delete that entry) */);
      return sviclo;
    }
  }
  
}
