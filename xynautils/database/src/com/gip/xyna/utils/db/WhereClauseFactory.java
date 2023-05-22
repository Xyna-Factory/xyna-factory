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
package com.gip.xyna.utils.db;

public class WhereClauseFactory {

   public static WhereClause equals(final String name, Object value) {
      WhereClause equals = new WhereClause() {
         Parameter parameter;

         public String getWhere() {
            return name + "=?";
         }

         public Parameter getParameter() {
            return parameter;
         }

         public WhereClause setParameter(Object param) {
            parameter = new Parameter(param);
            return this;
         }

      }.setParameter(value);
      return equals;
   }

   public static WhereClause and(WhereClause... wcs) {
      WhereClause and = new WhereClause() {
         WhereClause[] wcs;

         public String getWhere() {
            if( wcs.length == 0 ) {
              return "1=1"; //leere Liste
            }
            StringBuffer where = new StringBuffer();
             for (WhereClause w : wcs) {
               where.append(" AND ");
               where.append(w.getWhere());
            }
            return where.toString().substring(5);
         }

         public Parameter getParameter() {
            Parameter parameter = new Parameter();
            for (WhereClause w : wcs) {
               parameter.addParameter(w.getParameter());
            }
            return parameter;
         }

         public WhereClause setWhereClauses(WhereClause[] wcs) {
            this.wcs = wcs;
            return this;
         }

      }.setWhereClauses(wcs);
      return and;
   }

   public static WhereClause or(WhereClause... wcs) {
      WhereClause and = new WhereClause() {
         WhereClause[] wcs;

         public String getWhere() {
           if( wcs.length == 0 ) {
             return "1=1"; //leere Liste
           }
           StringBuffer where = new StringBuffer();
            for (WhereClause w : wcs) {
               where.append(" OR ");
               where.append(w.getWhere());
            }
            return "(" + where.toString().substring(4) + ")";
         }

         public Parameter getParameter() {
            Parameter parameter = new Parameter();
            for (WhereClause w : wcs) {
               parameter.addParameter(w.getParameter());
            }
            return parameter;
         }

         public WhereClause setWhereClauses(WhereClause[] wcs) {
            this.wcs = wcs;
            return this;
         }

      }.setWhereClauses(wcs);
      return and;
   }

   public static WhereClause fixedString(final String where) {
      WhereClause fixedString = new WhereClause() {
         public String getWhere() {
            return where;
         }

         public Parameter getParameter() {
            return new Parameter();
         }
      };
      return fixedString;
   }

}
