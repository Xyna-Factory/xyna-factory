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
package com.gip.xyna.xnwh.persistence.xmom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;

public class PersistenceAccessDelegator {
  
  private Map<String, StorableStructureInformation> infos = new HashMap<>();
  public static final Logger logger = CentralFactoryLogging.getLogger(PersistenceAccessDelegator.class);

  public PersistenceAccessDelegator(Set<StorableStructureInformation> structures) {
    for (StorableStructureInformation structure : structures) {
      infos.put(structure.fqClassNameOfDatatype, structure);
    }
  }
  
  
  public Method getTransformMethodFor(XynaObject storable) {
    return infos.get(storable.getClass().getName()).getTransformDatatypeToStorableMethod();
  }
  
  
  private static Method getTransformMethod(XynaObject storable, StorableStructureInformation info) {
    if (info.getPersistenceAccessDelegator() == null) {
      if (info.getFqClassNameForDatatype().equals(storable.getClass().getName()) ||
          info.isSynthetic) {
        return info.getTransformDatatypeToStorableMethod();
      } else {
        return findTransformMethodRecursivly(storable, info);
      }
    } else {
      return info.getPersistenceAccessDelegator().getTransformMethodFor(storable);
    }
  }
  
  private static Method findTransformMethodRecursivly(XynaObject storable, StorableStructureInformation info) {
    if (info.getSubEntries() != null) {
      for (StorableStructureIdentifier ssi : info.getSubEntries()) {
        if (ssi.getInfo().getFqClassNameForDatatype().equals(storable.getClass().getName())) {
          return ssi.getInfo().getTransformDatatypeToStorableMethod();
        }
      }
      for (StorableStructureIdentifier ssi : info.getSubEntries()) {
        Method m = findTransformMethodRecursivly(storable, ssi.getInfo());
        if (m != null) {
          return m;
        }
      }
    }
    return null;
  }


  public static com.gip.xyna.xnwh.persistence.Storable<?> transformDatatypeToExpansionStorable(XynaObject storable, Storable<?> possesingStorable,  Storable<?> rootStorable, StorableStructureInformation info) {
    if (storable == null) {
      return null;
    }
    try {
      Storable<?> odsStorable = (Storable<?>) getTransformMethod(storable, info).invoke(null, storable, rootStorable);
      adjustSpecialColumnsForExpansion(odsStorable, possesingStorable, info, -1);
      odsStorable.setValueByColumnName(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME).getColumnName(), storable.getClass().getName());
      return odsStorable;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static List<? extends Storable<?>> transformDatatypeListToExpansionStorableList(List<? extends Object> storableList, Storable<?> possesingStorable, Storable<?> rootStorable, StorableStructureInformation info, boolean primitive) {
    if (storableList == null) {
      return null;
    }
    if (storableList.size() <= 0) {
      return null;
    }
    try {
      List<Storable<?>> odsStorables; 
      if (primitive) {
        odsStorables = (List<Storable<?>>) info.getTransformDatatypeToStorableMethod().invoke(null, storableList);
      } else if (info.isReferencedList()) {
        Optional<? extends Object>  element = findNonNull(storableList);
        if (element.isPresent()) {
          odsStorables = (List<Storable<?>>) getTransformMethod((XynaObject) element.get(), info).invoke(null, storableList);
        } else {
          return Collections.emptyList();
        }
      } else { 
        odsStorables = transformComplexList(storableList, info, rootStorable);
      }
      for (int i = 0; i < odsStorables.size(); i++) {
        Storable<?> odsStorable = odsStorables.get(i);
        if (odsStorable != null) {
          int idx = (int) odsStorable.getValueByColString(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.LIST_IDX).getColumnName());
          adjustSpecialColumnsForExpansion(odsStorable, possesingStorable, info, idx);
          if (!info.isSyntheticStorable()) {
            odsStorable.setValueByColumnName(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME).getColumnName(), storableList.get(idx).getClass().getName());
          }
        }
      }
      return odsStorables;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }  
  }
  
  
  private static Optional<? extends Object> findNonNull(List<? extends Object> storableList) {
    return storableList.stream().filter(t -> t != null).findAny();
  }


  private static List<Storable<?>> transformComplexList(List<? extends Object> storableList, StorableStructureInformation info, Storable<?> rootStorable) {
    List<Storable<?>> resultList = new ArrayList<>();
    for (int i = 0; i < storableList.size(); i++) {
      XynaObject storable = (XynaObject) storableList.get(i);
      try {
        if (storable != null) {
          Storable<?> odsStorable = (Storable<?>) getTransformMethod((XynaObject) storable, info).invoke(null, storable, rootStorable);
          resultList.add(odsStorable);
          if (odsStorable != null) {
            odsStorable.setValueByColumnName(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.LIST_IDX).getColumnName(), i);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return resultList;
  }


  /**
   * spalten wie expansion FK, expansion PK, listidx etc setzen 
   */
  public static void adjustSpecialColumnsForExpansion(Storable<?> possessedStorable, Storable<?> possessingStorable, StorableStructureInformation possessedInfo, int listIdx) {
    Set<StorableColumnInformation> columns = possessedInfo.getColumnInfoAcrossHierarchy();
    for (StorableColumnInformation sci : columns) {
      //TODO anstatt reflection könnte man hier auch methoden des storables direkt aufrufen, wenn man sie z.b. in ein interface packt...
      switch (sci.getType()) {
        case EXPANSION_PARENT_FK :
        case UTILLIST_PARENT_FK :
          Object para = XMOMPersistenceOperationAlgorithms.transformType(String.valueOf(possessingStorable.getPrimaryKey()), sci);
          try {
            sci.getStorableSetter().invoke(possessedStorable, para);
          } catch (IllegalArgumentException e) {
            if (logger.isInfoEnabled()) {
              logIllegalArgumentException(sci, possessedStorable, e);
            }
            //was ist hier passiert? wieso wird einfach der setter aufgerufen, ohne den "kaputten" cache der method zu reparieren?
            //vermutung: cache des setters ist aus irgendeinem grunde veraltet und nicht korrekt. => d.h. dieser code ist nur zur kompensierung eines bugs an anderer stelle, der den cache nicht korrekt updated.
            try {
              sci.getStorableSetter((Class<? extends Storable<?>>) possessedStorable.getClass()).invoke(possessedStorable, para);
            } catch (IllegalArgumentException ee) {
              throw new RuntimeException("calling method " + sci.getStorableSetter((Class<? extends Storable<?>>) possessedStorable.getClass()) + " on "
                  + possessingStorable + ". parameter = " + para + "(" + getType(para) + ")", ee);
            } catch (IllegalAccessException ee) {
              throw new RuntimeException(ee);
            } catch (InvocationTargetException ee) {
              throw new RuntimeException(ee);
            }
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
          }
          break;
        case PK :
          String expansionPK = String.valueOf(possessingStorable.getPrimaryKey());
          if (listIdx >= 0) {
            expansionPK += "#" + listIdx;
          }
          try {
            sci.getStorableSetter().invoke(possessedStorable, expansionPK);
          } catch (IllegalArgumentException e) {
            if (logger.isInfoEnabled()) {
              logIllegalArgumentException(sci, possessedStorable, e);
            }
            try {
              sci.getStorableSetter((Class<? extends Storable<?>>) possessedStorable.getClass()).invoke(possessedStorable, expansionPK);
            } catch (IllegalArgumentException ee) {
              throw new RuntimeException("calling method " + sci.getStorableSetter((Class<? extends Storable<?>>) possessedStorable.getClass()) + " on "
                  + possessingStorable + ". parameter = " + expansionPK + "(" + getType(expansionPK) + ")", ee);
            } catch (IllegalAccessException ee) {
              throw new RuntimeException(ee);
            } catch (InvocationTargetException ee) {
              throw new RuntimeException(ee);
            }
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
          }
          break;
        case LIST_IDX :
        case REFERENCE_FORWARD_FK :
        case DEFAULT :
        default :
      }
    }
  }
  
  
  private static void logIllegalArgumentException(StorableColumnInformation sci, Storable<?> possessedStorable, IllegalArgumentException e) {
    try {
      StorableStructureInformation parentStorableInfo = sci.getParentStorableInfo();
      logger.info("cached setter of type " + sci.getType() + " in sci " + parentStorableInfo.getFqClassNameForStorable() + "."
          + sci.getColumnName() + "/rev=" + sci.getParentStorableInfo().getRevision() + " invalid (cached="
          + sci.getStorableSetter().getDeclaringClass().getClassLoader() + ", current=" + possessedStorable.getClass().getClassLoader()
          + ")", e);
    } catch (Exception ee) {
      logger.warn(null, ee); //npes oder sowas
    }
  }


  public static Storable<?> transformDatatypeToReferencedStorable(XynaObject storable, Storable<?> possessingStorable, Storable<?> rootStorable, StorableColumnInformation possessingColumn) {
    if (storable == null) {
      return null;
    }
    XMOMStorableStructureInformation info = getStorableStructureInformation(storable);
    try {
      Storable<?> referencedStorable = (Storable<?>) getTransformMethod(storable, info).invoke(null, storable, null);
      adjustSpecialColumnsForReference(referencedStorable.getPrimaryKey(), possessingStorable, possessingColumn);
      referencedStorable.setValueByColumnName(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME).getColumnName(), storable.getClass().getName());
      return referencedStorable;
    } catch (Exception e) {
      throw new RuntimeException("Could not transform " + storable.getClass().getName() + " with method from " + info.getFqXmlName()
          + ". Classloader of storable: " + getClassLoaderInfo(storable.getClass()) + ", classloader of method: "
          + getClassLoaderInfo(getTransformMethod(storable, info).getDeclaringClass()), e);
    }
  }

  
  static String getType(Object o) {
    if (o == null) {
      return "null";
    }
    return o.getClass().getName();
  }

  public static Storable<?> transformDatatypeToStorable(XynaObject storable) {
    XMOMStorableStructureInformation info = getStorableStructureInformation(storable);
    return transformDatatypeToStorable(storable, info);
  }
  
  
  public static Storable<?> transformDatatypeToStorable(XynaObject storable, XMOMStorableStructureInformation info) {
    try {
      Storable<?> internalStorable = (Storable<?>) getTransformMethod(storable, info).invoke(null, storable, null);
      internalStorable.setValueByColumnName(info.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME).getColumnName(), storable.getClass().getName());
      return internalStorable;
    } catch (Exception e) {
      throw new RuntimeException("Could not transform " + storable.getClass().getName() + " with method from " + info.getFqXmlName()
          + ". Classloader of storable: " + getClassLoaderInfo(storable.getClass()) + ", classloader of method: "
          + getClassLoaderInfo(getTransformMethod(storable, info).getDeclaringClass()), e);
    }
  }
  
  private static XMOMStorableStructureInformation getStorableStructureInformation(XynaObject storable) {
    return getStructureCache(storable).getStructuralInformation(storable.getClass().getName());
  }
  
  private static Long getRevision(XynaObject storable) {
    return ((MDMClassLoader) storable.getClass().getClassLoader()).getRevision();
  }
  
  private static XMOMStorableStructureCache getStructureCache(XynaObject storable) {
    return getStructureCache(getRevision(storable));
  }
  
  private static XMOMStorableStructureCache getStructureCache(Long revision) {
    return XMOMStorableStructureCache.getInstance(revision);
  }
  
  
  private static String getClassLoaderInfo(Class<?> clazz) {
    ClassLoader cl = clazz.getClassLoader();
    if (cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl;
      return clb.getExtendedDescription(false);
    } else {
      return cl.toString();
    }
  }

  
  private static void adjustSpecialColumnsForReference(Object referenceId, Storable<?> possessingStorable,
                                                       StorableColumnInformation possessingColumn) {
    try {
      //possessingColumn.getCorrespondingReferenceIdColumn().getStorableSetter().invoke(possessingStorable, referenceId);
      // TODO we can be called for a column from a subclass on a merger, merger returns baseClass as Storable class
      possessingColumn.getCorrespondingReferenceIdColumn().getStorableSetter((Class<? extends Storable<?>>) possessingStorable.getClass()).invoke(possessingStorable, referenceId);
    } catch (IllegalArgumentException e) {
      try {
        possessingColumn.getCorrespondingReferenceIdColumn().getStorableSetter((Class<? extends Storable<?>>) possessingStorable.getClass()).invoke(possessingStorable, referenceId);
      } catch (IllegalArgumentException ee) {
        throw new RuntimeException("calling method " + possessingColumn.getCorrespondingReferenceIdColumn().getStorableSetter((Class<? extends Storable<?>>) possessingStorable.getClass()) + " on "
                        + possessingStorable + ". parameter = " + referenceId + "(" + PersistenceAccessDelegator.getType(referenceId) + ")", ee);        
      } catch (IllegalAccessException ee) {
        throw new RuntimeException(ee);
      } catch (InvocationTargetException ee) {
        throw new RuntimeException(ee);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  
}
