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
package gip.base.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * OBDTO
 */
@SuppressWarnings("serial")
public class OBDTO extends OBObject implements OBDTOInterface {

  public HashMap<String,Object> parameters = new HashMap<String, Object>();
  
  
  public interface UsedForValue {
    public static final long NORMAL = 0;
    public static final long FORM = 1;
    // Bug 3180: Polling nach nicht quittierten Meldungen soll den OBIdleController nicht
    // zuruecksetzen, weil sonst das autom. Beenden des SmartClient nicht mehr funktioniert
    public static final long ALARM_NOTIFIER = 2;    
  }


  //Typen von DTOs
  public interface DTOType {
    static public final int terminalAida=0;
    static public final int terminalCnm=1;
    static public final int terminalAi=2;
  }
  
  // fuer Benachrichtigungen der TK-Objekte
  public interface NotificationType {
    static public final int DELETE = 0; 
  }
  
  public interface NotificationParameter {
    static public final int BEFORE_DELETE = 1;
    static public final int AFTER_DELETE = 0;
  }

  
  public final static int PREFIX_find = 1;
  public final static int PREFIX_findPK = 2;
  public final static int PREFIX_findFilter = 3;
  public final static int PREFIX_findWC = 4;
  public final static int PREFIX_findAll = 10;
  public final static int PREFIX_findAllPKs = 11;
  public final static int PREFIX_findAllFilter = 12;
  public final static int PREFIX_findAllWC = 13;
  public final static int PREFIX_count = 20;
  public final static int PREFIX_countPKs = 21;
  public final static int PREFIX_countFilter = 22;
  public final static int PREFIX_countWC = 23;
  
  public final static int PREFIX_delete = 30;
  public final static int PREFIX_findAndDeletePKs = 31;
  public final static int PREFIX_findAndDeleteFilter = 32;
  public final static int PREFIX_findAndDeleteWC = 33;
  public final static int PREFIX_findAndDeleteList = 34;
  public final static int PREFIX_notifyTk = 35;
  
  public final static int PREFIX_setLock = 36;
  public final static int PREFIX_getLock = 37;
  public final static int PREFIX_resetLock = 38;

  // Notification Parameter
  protected int notify;
  protected int notify_param;
  
  public final static String nameUpdate = "update"; //$NON-NLS-1$

  private long capabilityId;
  private long usedFor;
  private Boolean update;
  private long[] pks;
  private String whereClause;


  /**
   * Standard-Konstruktor 
   */
  public OBDTO() {
    super();
  }

  /** 
   * Liefert zu einem Schema/Objekt eine Darstellung, die vor dem Ausfuehren des SQLs korrekt ersetzt wird
   * Wenn moeglich bitte getSQLRepresentation() der speziellen DTOs verwenden 
   * @param schema z.B. "ipnet"
   * @param objectName z.B. "iRouter"
   * @return Schema
   */
  public static String getSQLRepresentation(String schema, String objectName) {
    return OBObject.START_PROJECT_SCHEMA +  schema + OBObject.END_PROJECT_SCHEMA + 
           "." + objectName; //$NON-NLS-1$
  }

  /**
   * @see gip.base.common.OBDTOInterface#getCapabilityId()
   */
  public long getCapabilityId() { 
    return capabilityId; 
  }
  
  
  /**
   * @see gip.base.common.OBDTOInterface#setCapabilityId(long)
   */
  public void setCapabilityId(long _capabilityId) throws OBException { 
    this.capabilityId = _capabilityId; 
  }

  
  /** 
   * Methode, die sagt, ob fuer die Capability ueberhaupt geprueft werden soll.
   * @param _capabilityId Id der Capability
   * @return false, wenn vom Check ausgenommen
   */
  public boolean isCapabilityToCheck(long _capabilityId) {
    // NOTE: Code macht eigentlich keinen Sinn. 
    // Wird aber sowieso in den abgeleiteten Klassen ueberschrieben!
    return true;
  }

  
  /** 
   * Liefert die eigentlich zu pruefende Capability
   * @param _capabilityId angeforderte Capability
   * @return zu pruefende Capability bzw. OBAttribute.NULL, wenn nichts zu pruefen ist
   */
  public long getCheckCapability(long _capabilityId) {
    if (!isCapabilityToCheck(_capabilityId)) {
      return OBAttribute.NULL;
    }
    return _capabilityId;
  }
  
  /** 
   * Pruefen ob eine notify Capability vorhanden ist
   * @return true wenn eine notify Capability vorhanden ist, sonst false
   */
  public boolean hasNotifyCapabilityId() {
    return false;
  }

  
  /**
   * @return Name der DM-Klasse
   */
  public String getDMClassName() {
    return "gip.base.db.demultiplexing.OBDM"; //$NON-NLS-1$
  }
  
  
  /**
   * @return Benutzungs-Flag
   */
  public long getUsedFor() {
    return usedFor;
  }

  
  /**
   * @param _usedFor Parameter, an dem Zusatzaktionen abgeleitet werden koennen
   */
  public void setUsedFor(long _usedFor) {
    this.usedFor = _usedFor;
  }

  
  /**
   * @return update-Flag
   */
  public boolean getUpdate() {
    return update.booleanValue();
  }


  /**
   * @param _update Update-/Insert-Flag
   */
  public void setUpdate(boolean _update) {
    update = new Boolean(_update);
  }


  /**
   * @return ist das update-Flag null
   */
  public boolean isUpdateNull() {
    return update == null;
  }


  /**
   * @return Wird update ignoriert
   */
  public boolean isUpdateIgnored() {
    return false;
  }


  /**
   * 
   */
  public void setUpdateNull() {
    update = null;
  }



  /**
   * @return liefert ein pk-Array
   */
  public long[] getPks() {
    return pks;
  }


  /**
   * @param _pks PKs, nach denen gesucht werden soll
   */
  public void setPks(long[] _pks) {
    this.pks = _pks;
  }


  /**
   * @return Where-Clause
   */
  public String getWhereClause() {
    return whereClause;
  }


  /**
   * @param wc Where-Clause
   */
  public void setWhereClause(String wc) {
    this.whereClause = wc;
  }


  /**
   * @return dtoId
   */
  public long getDTOId() {
    return OBAttribute.NULL;
  }
  
  // Setzen der Notification Parameter
  public void setNotification(int n, int np) {
    notify = n;
    notify_param = np;
  }
  
  public int getNotification() { return notify; }
  public int getNotifyParam() { return notify_param; }


  /**
   * @see gip.base.common.OBCheckListener#isValueValid(java.lang.String, java.lang.String)
   */
  /* derzeit nicht eingesetzt */
  public String isValueValid(String p_attribKey_p, String p_attribValue_p) {
    return null;
  }
  
  /**
   * Wandelt ein OBDBOject in eine HashMap um (key = attribute.name, value =
   * attribute.value)
   * 
   * @return HashMap, der dem OBDBObject entspricht
   */
  @SuppressWarnings("unchecked")
  public HashMap<String, Object> convertToHashMap() {
    HashMap<String, Object> retVal = new HashMap<String, Object>();
    for (int i = 0; i < attArr.length; i++) {
      retVal.put(attArr[i].getName(), attArr[i].getValue());
    }
    
    
    for (String key : parameters.keySet()) {
      Object o = parameters.get(key);
      if (o!=null) {
        if (o instanceof OBObject) {
          retVal.put(key, ((OBObject) o).convertToHashMap());
        }
        else if (o instanceof OBListObject<?>) {
          retVal.put(key, ((OBListObject<OBObject>) o).convertToList());
        }
        else if (o instanceof String ||
                 o instanceof Integer ||
                 o instanceof Long ||
                 o instanceof Boolean) {
          retVal.put(key, String.valueOf(o));
        }
        else if (o instanceof String[] ||
                 o instanceof Integer[] ||
                 o instanceof Long[] ||
                 o instanceof Boolean[]) {
          List<Object> list = new ArrayList<Object>();
          Object[] oatt = (Object[]) o;
          for (int j = 0; j < oatt.length; j++) {
            list.add(String.valueOf(oatt[j]));
          }
          retVal.put(key, list);
        }
      }
    }
    
    return retVal;
  }

  /**
   * Baut aus einem OBDTO einen String, der selbiges repraesentiert
   * @param indent Einrueckung
   * @param ignoreList Liste der zu *nenden Werte
   * @return String Repraesentation des Objektes
   * @throws OBException Fehlermeldung
   */
  public String convertToString(String indent, String[] ignoreList) throws OBException {
    StringBuilder sb = new StringBuilder();
    sb.append(super.convertToString(indent, ignoreList));
    for(String key : parameters.keySet() ) {
      Object val = parameters.get(key);
      if (val instanceof String) {
        sb.append(indent).append(key).append("=").append(OBUtils.hideValue(key, val, ignoreList)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      else if (val instanceof Integer) {
        sb.append(indent).append(key).append("=").append(OBUtils.hideValue(key, String.valueOf(val), ignoreList)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      else if (val instanceof Long) {
        sb.append(indent).append(key).append("=").append(OBUtils.hideValue(key, String.valueOf(val), ignoreList)).append("\n");//$NON-NLS-1$ //$NON-NLS-2$
      }
      else if (val instanceof Boolean) {
        sb.append(indent).append(key).append("=").append(OBUtils.hideValue(key, String.valueOf(val), ignoreList)).append("\n");//$NON-NLS-1$ //$NON-NLS-2$
      }
      else if (val instanceof Double) {
        sb.append(indent).append(key).append("=").append(OBUtils.hideValue(key, String.valueOf(val), ignoreList)).append("\n");//$NON-NLS-1$ //$NON-NLS-2$
      }
      else if (val instanceof OBListDTO<?>) {
        sb.append(indent).append(key).append("=").append("[\n");//$NON-NLS-1$ //$NON-NLS-2$
        for (int i = 0; i < ((OBListDTO<?>)val).size(); i++) {
          if (i>0) { sb.append(",\n"); }//$NON-NLS-1$
          sb.append(((OBListDTO<?>)val).elementAt(i).convertToString(indent+"  ", ignoreList));//$NON-NLS-1$
        }
        sb.append(indent).append("]\n");//$NON-NLS-1$
      }
      else if (val instanceof OBDTO) {
        sb.append(indent).append(key).append("=").append("{\n");//$NON-NLS-1$ //$NON-NLS-2$
        sb.append(((OBDTO)val).convertToString(indent+"  ", ignoreList));//$NON-NLS-1$
        sb.append(indent).append("}\n");//$NON-NLS-1$
      }
      else if (val instanceof String[]) {
        sb.append(indent).append(key).append("=").append("[");//$NON-NLS-1$//$NON-NLS-2$
        for (int i = 0; i < ((String[])val).length; i++) {
          if (i>0) { sb.append(','); }
          sb.append(OBUtils.hideValue(key,((String[])val)[i], ignoreList));
        }
        sb.append("]\n");//$NON-NLS-1$
      }
      else if (val instanceof int[]) {
        sb.append(indent).append(key).append("=").append("[");//$NON-NLS-1$//$NON-NLS-2$
        for (int i = 0; i < ((int[])val).length; i++) {
          if (i>0) { sb.append(','); }
          sb.append(OBUtils.hideValue(key,String.valueOf(((int[])val)[i]), ignoreList));
        }
        sb.append("]\n");//$NON-NLS-1$
      }
      else if (val instanceof long[]) {
        sb.append(indent).append(key).append("=").append("[");//$NON-NLS-1$//$NON-NLS-2$
        for (int i = 0; i < ((long[])val).length; i++) {
          if (i>0) { sb.append(','); }
          sb.append(OBUtils.hideValue(key,String.valueOf(((long[])val)[i]), ignoreList));
        }
        sb.append("]\n");//$NON-NLS-1$
      }
      else if (val instanceof List<?>) {
        sb.append(indent).append(key).append("=").append("[\n");//$NON-NLS-1$//$NON-NLS-2$
        for (int i = 0; i < ((List<?>)val).size(); i++) {
          if (i>0) { sb.append(",\n"); }//$NON-NLS-1$
          sb.append(indent+ "  ").append(OBUtils.hideValue(key,String.valueOf(((List<?>)val).get(i)), ignoreList));//$NON-NLS-1$
        }
        sb.append(indent).append("]\n");//$NON-NLS-1$
      }
    }
    String details = sb.toString();
    if( details.length() > 1 ) {
      details = details.substring(0,details.length()-1);
    }

    return sb.toString();
  }

  /**
   * Wird in den generierten Klassen ueberschrieben
   * @return OBDTO
   */
  public static OBDTO getInstance() {
    return new OBDTO();
  }
  
  public Object getParameterAndFillIfNull(String key) {
    if (parameters.get(key)!=null) {
      return parameters.get(key);
    }
    return "";//$NON-NLS-1$
  }
}


