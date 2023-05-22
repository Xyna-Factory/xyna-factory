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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = ApplicationEntryStorable.COL_ID, tableName = ApplicationEntryStorable.TABLE_NAME)
public class ApplicationEntryStorable extends Storable<ApplicationEntryStorable> {


  private static final long serialVersionUID = -9214843621592998879L;
  
  public final static String TABLE_NAME = "applicationentries";
  public final static String COL_ID = "id";
  public final static String COL_APPLICATION = "application";
  public final static String COL_VERSION = "version";
  public final static String COL_TYPE = "type";
  public final static String COL_NAME = "name";
  public final static String COL_PARENT_REVISION = "parentRevision";

  public static final Comparator<? super ApplicationEntryStorable> COMPARATOR =
      new Comparator<ApplicationEntryStorable>() {

        public int compare(ApplicationEntryStorable o1, ApplicationEntryStorable o2) {
          int r = o1.getName().compareTo(o2.getName());
          if (r == 0) {
            r = o1.getType().compareTo(o2.getType());
          }
          return r;
        }

      };

  @Column(name = COL_ID)
  private Long id;
  
  @Column(name = COL_APPLICATION)
  private String application;
  
  @Column(name = COL_VERSION)
  private String version;
  
  @Column(name = COL_TYPE)
  private String type;
  
  @Column(name = COL_NAME)
  private String name;

  @Column(name = COL_PARENT_REVISION)
  private Long parentRevision;


  public enum ApplicationEntryType {


    WORKFLOW(PathType.XMOM) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return checkXMOMExistence(name, parentRevision);
      }
    },
    DATATYPE(PathType.XMOM) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return checkXMOMExistence(name, parentRevision);
      }
    },
    EXCEPTION(PathType.XMOM) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return checkXMOMExistence(name, parentRevision);
      }
    },
    TRIGGER(PathType.TRIGGER) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        try {
          return null != XynaFactory.getInstance().getActivation().getActivationTrigger().getTriggerInformation(name, parentRevision, false);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        } catch (XACT_TriggerNotFound e) {
          return false;
        }
      }
    },
    TRIGGERINSTANCE(PathType.TRIGGER) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        try {
          return null != XynaFactory.getInstance().getActivation().getActivationTrigger().getTriggerInstanceInformation(name, parentRevision);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
      }
    },
    FILTERINSTANCE(PathType.FILTER) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        try {
          return null != XynaFactory.getInstance().getActivation().getActivationTrigger().getFilterInstanceInformation(name, parentRevision);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
      }
    },
    FILTER(PathType.FILTER) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        try {
          return null != XynaFactory.getInstance().getActivation().getActivationTrigger().getFilterInformation(name, parentRevision, false);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        } catch (XACT_FilterNotFound e) {
          return false;
        }
      }
    },
    ORDERTYPE(null) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return true;
      }
    },
    CAPACITY(null) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return true;
      }
    },
    XYNAPROPERTY(null) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return true;
      }
    },
    SHAREDLIB(PathType.SHAREDLIB) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return true;
      }
    },
    FORMDEFINITION(PathType.XMOM) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        return true;
      }
    },
    ORDERINPUTSOURCE(null) {

      @Override
      public boolean checkForExistence(String name, long parentRevision) {
        OrderInputSourceManagement oism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        try {
          return null != oism.getInputSourceByName(parentRevision, name);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        }
      }
    };

    private final PathType pathType;


    private ApplicationEntryType(PathType pathType) {
      this.pathType = pathType;
    }


    public PathType getPathType() {
      return pathType;
    }


    private static boolean checkXMOMExistence(String name, long parentRevision) {
      try {
        return null != ((ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
            .getApplicationManagement()).getApplicationEntryTypeForXMOMObject(name, parentRevision);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    }


    /**
     * @return true, falls object existiert
     */
    public abstract boolean checkForExistence(String name, long parentRevision);


    public static ApplicationEntryType getByXMOMType(XMOMType type) {
      switch (type) {
        case DATATYPE :
          return DATATYPE;
        case EXCEPTION :
          return EXCEPTION;
        case WORKFLOW :
          return WORKFLOW;
        case FORM :
          return FORMDEFINITION;
        case ORDERINPUTSOURCE :
          return ORDERINPUTSOURCE;
        default :
          throw new RuntimeException("unsupported: " + type);
      }
    }
  }


  public ApplicationEntryStorable() {
  }

  private ApplicationEntryStorable(long id, String application, String version, Long parentRevision, String name, ApplicationEntryType type) {
    if (name == null) {
      throw new IllegalArgumentException("Name may not be null");
    }
    this.application = application;
    this.version = version;
    this.name = name;
    this.type = type.toString();
    this.id = id;
    this.parentRevision = parentRevision;
  }

  public static ApplicationEntryStorable toStore(String application, String version, String name, ApplicationEntryType type) {
    return new ApplicationEntryStorable(XynaFactory.getInstance().getIDGenerator().getUniqueId(), application, version, null, name, type);
  }
  
  public static ApplicationEntryStorable toStore(String application, String version, Long parentRevision, String name, ApplicationEntryType type) {
    return new ApplicationEntryStorable(XynaFactory.getInstance().getIDGenerator().getUniqueId(), application, version, parentRevision, name, type);
  }
  
  /**
   * erzeugt im gegensatz zu tostore keine id
   */
  public static ApplicationEntryStorable create(String application, String version, Long parentRevision, String name, ApplicationEntryType type) {
    return new ApplicationEntryStorable(-1, application, version, parentRevision, name, type);
  }
  
  /**
   * erzeugt im gegensatz zu tostore keine id
   */
  public static ApplicationEntryStorable create(String application, String version, String name, ApplicationEntryType type) {
    return new ApplicationEntryStorable(-1, application, version, null, name, type);
  }
  
  private static ResultSetReader<ApplicationEntryStorable> reader = new ResultSetReader<ApplicationEntryStorable>() {

    public ApplicationEntryStorable read(ResultSet rs) throws SQLException {
      ApplicationEntryStorable data = new ApplicationEntryStorable();
      data.id = rs.getLong(COL_ID);
      data.application = rs.getString(COL_APPLICATION);
      data.version = rs.getString(COL_VERSION);
      data.name = rs.getString(COL_NAME);
      data.type = rs.getString(COL_TYPE);
      data.parentRevision = rs.getLong(COL_PARENT_REVISION);
      return data;
    }

  };


  public static ResultSetReader<ApplicationEntryStorable> getStaticReader() {
    return reader;
  }


  @Override
  public ResultSetReader<? extends ApplicationEntryStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends ApplicationEntryStorable> void setAllFieldsFromData(U data) {
    ApplicationEntryStorable cast = data;
    id = cast.id;
    application = cast.application;
    version = cast.version;
    name = cast.name;
    type = cast.type;
    parentRevision = cast.parentRevision;
  }


  public Long getId() {
    return id;
  }


  public void setId(Long id) {
    this.id = id;
  }

  
  public String getApplication() {
    return application;
  }

  
  public void setApplication(String application) {
    this.application = application;
  }

  
  public String getVersion() {
    return version;
  }


  public void setVersion(String version) {
    this.version = version;
  }


  public String getType() {
    return type;
  }


  public void setType(String type) {
    this.type = type;
  }


  public ApplicationEntryType getTypeAsEnum() {
    return ApplicationEntryType.valueOf(type);
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name may not be null");
    }
    this.name = name;
  }

  public String toString() {
    return name;
  }



  public Long getParentRevision() {
    return parentRevision;
  }


  public void setParentRevision(Long parentRevision) {
    this.parentRevision = parentRevision;
  }


}
