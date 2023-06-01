/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xnwh.persistence;



/**
 * beschreibt den typ von in storable definierten spalten, bzw das mapping zwischen java-type und dem type in der
 * persistence-schicht (implementierungsabhängig).
 */
public enum ColumnType {
  INHERIT_FROM_JAVA,
  BLOBBED_JAVAOBJECT, //wird in db geschrieben, indem value in einen objectoutputstream geschrieben wird  
  BYTEARRAY; //wird direkt binär in db geschrieben.
}
