/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.mysql;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.gip.xyna.utils.db.StatementCache;
import com.gip.xyna.utils.db.WrappedConnection;

//wenn dedizierte connections den cache verwenden, muss darauf reagiert werden, dass die innere connection evtl ausgetauscht wurde
class MySQLPLStatementCache extends StatementCache {

    private WeakReference<Connection> wr;

    @Override
    public PreparedStatement getPreparedStatement(String sql) {
        PreparedStatement ps = super.getPreparedStatement(sql);
        if (ps == null) {
            return null;
        }
        try {
            Connection innerCon = ps.getConnection();
            if (innerCon instanceof WrappedConnection) {
                innerCon = ((WrappedConnection) innerCon).getWrappedConnection();
            }
            if (wr != null) {
                if (innerCon == null || innerCon.isClosed() || wr.get() != innerCon) {
                    close();
                    ps = null;
                    wr = new WeakReference<Connection>(innerCon);
                }
            } else if (innerCon != null) {
                // erstes mal belegen
                wr = new WeakReference<Connection>(innerCon);
            }
        } catch (SQLException e) {
            close();
            ps = null;
        }
        return ps;
    }

}