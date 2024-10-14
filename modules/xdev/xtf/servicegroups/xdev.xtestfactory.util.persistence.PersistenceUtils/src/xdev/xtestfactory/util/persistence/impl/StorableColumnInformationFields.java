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
package xdev.xtestfactory.util.persistence.impl;

import java.lang.reflect.Field;

import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.DirectStorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;

enum StorableColumnInformationFields {
  REFERENCE("reference") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      return true;
    }
  },
  /*REFERENCE_NAME("referenceName") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      return joinTargetColumn.getParentStorableInfo().getFqXmlName();
    }
  },
  REFERENCE_REV("referenceRevision") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      return joinTargetColumn.getParentStorableInfo().getRevision();
    }
  },*/
  CORRESPONDING_STORABLE("correspondingStorable") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      //return XMOMStorableStructureCache.identifierOf(joinTargetColumn.getParentStorableInfo());
      return new DirectStorableStructureIdentifier(joinTargetColumn.getParentStorableInfo());
    }
  },
  REFERENCE_ID_COL("correspondingReferenceIdColumnName") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      return sourceColumn.getColumnName();
    }
  },
  REFERENCED_ID_COL("correspondingReferencedIdColumn") {
    @Override
    public Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
      return joinTargetColumn;
    }
  };
  
  private final String fieldname;
  
  private StorableColumnInformationFields(String fieldname) {
    this.fieldname = fieldname;
  }
  
  public abstract Object getValue(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn);
  
  public void adjustField(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
    try {
    Field field = getSCIField();
    field.set(sourceColumn, getValue(sourceColumn, joinTargetColumn));
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }
  
  private Field getSCIField() throws NoSuchFieldException, SecurityException {
    Field field = StorableColumnInformation.class.getDeclaredField(fieldname);
    field.setAccessible(true);
    return field;
  }
  
}