/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.persistence.xsor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;


public class PreparedClusterCommandCreator {
  
  private static final Pattern deletePattern = Pattern.compile("^delete (.*)$", Pattern.CASE_INSENSITIVE);
  
  
  public PreparedClusterCommand prepareCommand(Command command, Class<? extends Storable> storableClazz, List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions) throws PersistenceLayerException {
    if (command.getSqlString().startsWith("delete")) {

      Matcher m = deletePattern.matcher(command.getSqlString());
      if (!m.matches()) {
        throw new UnsupportedOperationException("Invalid delete command");
      }
      String sqlStringFromAndWhere = m.group(1);

      StringBuilder sqlSelectString = new StringBuilder().append("select * ");
      sqlSelectString.append(" ").append(sqlStringFromAndWhere);
      
      PreparedClusterQuery<?> preparedQuery = new PreparedClusterQueryCreator().prepareQuery(command.getTable(), sqlSelectString.toString(), storableClazz, indexDefinitions);
      return new PreparedClusterCommand(preparedQuery);
    } else {
      return null;
    }
  }
  

}
