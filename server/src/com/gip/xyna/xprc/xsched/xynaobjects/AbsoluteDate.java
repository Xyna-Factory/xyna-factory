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
package com.gip.xyna.xprc.xsched.xynaobjects;



import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.LazyDateFormat;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedVersionedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;



@XynaObjectAnnotation(fqXmlName = "base.date.AbsoluteDate")
public class AbsoluteDate extends Date {

  private static final long serialVersionUID = -1588189301038L;
  private static final Logger logger = CentralFactoryLogging.getLogger(AbsoluteDate.class);


  @LabelAnnotation(label="Date")
  private String date;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfdate;


  public String getDate() {
    return date;
  }


  public String versionedGetDate(long _version) {
    if (oldVersionsOfdate == null) {
      return date;
    }
    String _local = date;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfdate.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  @LabelAnnotation(label="Format")
  private transient DateFormat format;

  private transient volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DateFormat> oldVersionsOfformat;


  public DateFormat getFormat() {
    return format;
  }


  public DateFormat versionedGetFormat(long _version) {
    if (oldVersionsOfformat == null) {
      return format;
    }
    DateFormat _local = format;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<DateFormat> _ret = oldVersionsOfformat.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  protected static class InternalBuilder<_GEN_DOM_TYPE extends AbsoluteDate, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>
      extends
        Date.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE> {

    protected InternalBuilder(AbsoluteDate instance) {
      super(instance);
    }


    public AbsoluteDate instance() {
      return (AbsoluteDate) instance;
    }


    public _GEN_BUILDER_TYPE date(String date) {
      this.instance.unversionedSetDate(date);
      return (_GEN_BUILDER_TYPE) this;
    }


    public _GEN_BUILDER_TYPE format(DateFormat format) {
      this.instance.unversionedSetFormat(format);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<AbsoluteDate, Builder> {

    public Builder() {
      super(new AbsoluteDate());
    }


    public Builder(AbsoluteDate instance) {
      super(instance);
    }
  }


  public Builder buildAbsoluteDate() {
    return new Builder(this);
  }


  public AbsoluteDate() {
    super();
  }


  /**
  * Creates a new instance using locally defined member variables.
  */
  public AbsoluteDate(String date, DateFormat format) {
    this();
    this.date = date;
    this.format = format;
  }


  protected void fillVars(AbsoluteDate source, boolean deep) {
    super.fillVars(source, deep);
    this.date = source.date;
    this.format = (DateFormat) XynaObject.clone(source.format, deep);
  }


  public AbsoluteDate clone() {
    return clone(true);
  }


  public AbsoluteDate clone(boolean deep) {
    AbsoluteDate cloned = new AbsoluteDate();
    cloned.fillVars(this, deep);
    return cloned;
  }


  public static class ObjectVersion extends com.gip.xyna.xprc.xsched.xynaobjects.Date.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version,
                         java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }


    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      AbsoluteDate xoc = (AbsoluteDate) xo;
      AbsoluteDate xoco = (AbsoluteDate) other.xo;
      if (!equal(xoc.versionedGetDate(this.version), xoco.versionedGetDate(other.version))) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetFormat(this.version), xoco.versionedGetFormat(other.version), this.version, other.version,
                   changeSetsOfMembers)) {
        return false;
      }
      return true;
    }


    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      AbsoluteDate xoc = (AbsoluteDate) xo;
      String date = xoc.versionedGetDate(this.version);
      hash = hash * 31 + (date == null ? 0 : date.hashCode());
      DateFormat format = xoc.versionedGetFormat(this.version);
      hash = hash * 31 + (format == null ? 0 : format.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version,
                                           java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }

  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }

  public void collectChanges(long start, long end,
                             java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers,
                             java.util.Set<Long> datapoints) {
    super.collectChanges(start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfdate, start, end, datapoints);
    XOUtils.addChangesForComplexMember(format, oldVersionsOfformat, start, end, changeSetsOfMembers, datapoints);
  }


  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public String toXml(String varName, boolean onlyContent, long version,
                      com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
    StringBuilder xml = new StringBuilder();
    long objectId;
    if (!onlyContent) {
      long refId;
      if (cache != null) {
        ObjectVersion ov = new ObjectVersion(this, version, cache.changeSetsOfMembers);
        refId = cache.putIfAbsent(ov);
        if (refId > 0) {
          objectId = -2;
        } else {
          objectId = -refId;
          refId = -1;
        }
      } else {
        objectId = -1;
        refId = -1;
      }
      XMLHelper.beginType(xml, varName, "AbsoluteDate", "base.date", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "date", versionedGetDate(version), version, cache);
      XMLHelper.appendData(xml, "format", versionedGetFormat(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {"date", "format"})));


  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    HashSet<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVarNames());
    return ret;
  }


  public Set<String> getVariableNames() {
    Set<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVariableNames());
    return Collections.unmodifiableSet(ret);
  }


  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[] {"date", "format"};
    Object[] vars = new Object[] {this.date, this.format};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      o = super.get(name);
      if (o == XOUtils.VARNAME_NOTFOUND) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
      }
    }
    return o;
  }


  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("date".equals(name)) {
      XOUtils.checkCastability(o, String.class, "date");
      setDate((String) o);
    } else if ("format".equals(name)) {
      XOUtils.checkCastability(o, DateFormat.class, "format");
      setFormat((DateFormat) o);
    } else {
      super.set(name, o);
    }
  }


  protected String asString_InternalSuperCallDestination(AbsoluteDate internalSuperCallDelegator) {
    return asString_InternalImplementation();
  }


  public String asString() {
    return asString_InternalImplementation();
  }


  protected void fromMillis_InternalSuperCallDestination(AbsoluteDate internalSuperCallDelegator, long millis) {
    fromMillis_InternalImplementation(millis);
  }


  public void fromMillis(long millis) {
    fromMillis_InternalImplementation(millis);
  }


  protected com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate toAbsoluteDate_InternalSuperCallDestination(AbsoluteDate internalSuperCallDelegator,
                                                                                                          DateFormat dateFormat) {
    return toAbsoluteDate_InternalImplementation(dateFormat);
  }


  public com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate toAbsoluteDate(DateFormat dateFormat) {
    return toAbsoluteDate_InternalImplementation(dateFormat);
  }


  protected long toMillis_InternalSuperCallDestination(AbsoluteDate internalSuperCallDelegator) {
    return toMillis_InternalImplementation();
  }


  public long toMillis() {
    return toMillis_InternalImplementation();
  }


  protected void validate_InternalSuperCallDestination(AbsoluteDate internalSuperCallDelegator) {
    validate_InternalImplementation();
  }


  public void validate() {
    validate_InternalImplementation();
  }


  public void onDeployment() throws XynaException {
    super.onDeployment();
  }


  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = AbsoluteDate.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  

  /*------- not generated -------- */

  public void setDate(String date) {
    validateDate(date);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfdate;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdate;
          if (_vo == null) {
            oldVersionsOfdate = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.date);
        this.date = date;
      }
      return;
    }
    this.date = date;
  }


  public void unversionedSetDate(String date) {
    validateDate(date);
    this.date = date;
  }

  public void setFormat(DateFormat format) {
    validateFormat(format);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DateFormat> _vo = oldVersionsOfformat;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfformat;
          if (_vo == null) {
            oldVersionsOfformat = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<DateFormat>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.format);  
        changeFormatAndUpdateDate(format, false);
      }
      return;
    }
    changeFormatAndUpdateDate(format, true);
  }
  
  private void validateFormat(DateFormat format) {
    if (format == null || format.getFormat() == null) {
      //null ist immer erlaubt
      return;
    }
    if (this.date != null) {
      //format muss nur zum date passen, wenn vorher kein format gesetzt war. ansonsten darf man es ändern und date wird automatisch geupdated
      if (format == null || format.getFormat() == null) {
        getOrCreateLazyDateFormat().validate(date, format.getFormat());
      }
    }
  }

  private void validateDate(String date) {
    if (format != null && format.getFormat() != null) {
      getOrCreateLazyDateFormat().validate(date, format.getFormat());
    }
  }

  private void changeFormatAndUpdateDate(DateFormat newFormat, boolean unversionedDateUpdate) {
    if (date == null || getFormatInternally() == null || newFormat == null || newFormat.getFormat() == null) {
      this.format = newFormat; //validierung ist vorher schon passiert
    } else {
      //update date
      long millis = lazyDateFormat.toMillis(date, getFormatInternally());
      this.format = newFormat;
      String newDate = lazyDateFormat.format(millis, newFormat.getFormat());
      if (unversionedDateUpdate) {
        //erneute validierung nicht notwendig
        date = newDate;
      } else {
        setDate(newDate);
      }
    }
  }

  public void unversionedSetFormat(DateFormat format) {
    validateFormat(format);
    changeFormatAndUpdateDate(format, true);
  }

  private String asString_InternalImplementation() {
    return date;
  }


  private void fromMillis_InternalImplementation(long millis) {
    date = getOrCreateLazyDateFormat().format(millis, getFormatInternally());
  }


  protected String getFormatInternally() {
    if (format == null) {
      return null;
    }
    return format.getFormat();
  }


  private AbsoluteDate toAbsoluteDate_InternalImplementation(DateFormat dateFormat) {
    AbsoluteDate ad = new AbsoluteDate("", dateFormat);
    ad.fromMillis(toMillis());
    return ad;
  }


  private long toMillis_InternalImplementation() {
    return getOrCreateLazyDateFormat().toMillis(date, getFormatInternally());
  }


  private void validate_InternalImplementation() {
   //FIXME interface CanBeValidated korrekt verwenden
    getOrCreateLazyDateFormat().validate(date, getFormatInternally());
  }


  public AbsRelTime toAbsRelTime() {
    return new AbsRelTime(toMillis(), false);
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    try {
      s.writeObject(new SerializableClassloadedXynaObject(format));
      s.writeObject(new SerializableClassloadedVersionedObject<DateFormat>(oldVersionsOfformat));
    } catch (IOException e) {
      throw new IOException("Could not serialize format " + format
          + ". It probably contains a non transient reference to a non serializable object.", e);
    }
  }


  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    format = (DateFormat) ((SerializableClassloadedXynaObject) s.readObject()).getXynaObject();
    oldVersionsOfformat = ((SerializableClassloadedVersionedObject<DateFormat>) s.readObject()).getVersions();
  }


  private LazyDateFormat lazyDateFormat;


  protected LazyDateFormat getOrCreateLazyDateFormat() {
    if (lazyDateFormat == null) {
      lazyDateFormat = new LazyDateFormat();
    }
    return lazyDateFormat;
  }
  
 /* public static void main(String[] args) {
    AbsoluteDate ad = new AbsoluteDate();
    CustomDateFormat f = new CustomDateFormat();
    f.setDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    ad.setFormat(f);
    ad.setDate("2017-09-20T11:20:21Z");
    
    
    long millis = ad.toMillis();
    
    SimpleDateFormat sdf = Constants.defaultUTCSimpleDateFormat();
    System.out.println(sdf.format(new java.util.Date(millis)));
    System.out.println(millis);
  }*/
}
