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
package com.gip.xyna.xdev.xlibdev.repository;



import java.io.InputStream;

import com.gip.xyna.utils.exceptions.XynaException;



public interface Repository {

  public static class VersionedObject {

    private final String fileNameWithRelativePath;
    private final InputStream content;


    /**
     * @param fileNameWithRelativePath bsp: a/b/c.txt
     */
    public VersionedObject(String fileNameWithRelativePath, InputStream content) {
      this.fileNameWithRelativePath = fileNameWithRelativePath;
      this.content = content;
    }


    public String getFileNameWithRelativePath() {
      return fileNameWithRelativePath;
    }


    public InputStream getContent() {
      return content;
    }

  }

  public enum ObjectChange {
    CREATED, DELETED, MODIFIED, ERROR;

    public String shortName() {
      return String.valueOf(name().charAt(0));
    }


    public static ObjectChange valueOfShortName(String shortName) throws IllegalArgumentException {
      if (shortName.equals("C")) {
        return CREATED;
      } else if (shortName.equals("D")) {
        return DELETED;
      } else if (shortName.equals("M")) {
        return MODIFIED;
      } else if (shortName.equals("E")) {
        return ERROR;
      }
      throw new IllegalArgumentException("Invalid ObjectChange found in history file: " + shortName);
    }
  }
  
  //TODO filename und comment?!
  public static class Revision {
    
    private final long rev;
    private final long timestamp;
    private final ObjectChange change;
    
    public Revision(long revision, long timestamp, ObjectChange change) {
      this.rev = revision;
      this.timestamp = timestamp;
      this.change = change;
    }

    
    public long getRev() {
      return rev;
    }

    
    public long getTimestamp() {
      return timestamp;
    }

    
    public ObjectChange getChange() {
      return change;
    }

    
  }

  public long getCurrentRevision();


  /**
   * �bergebene InputStreams werden nicht geschlossen, sondern nur bis EOF ausgelesen
   * @return revision, in der objekte gespeichert wurden
   */
  public long saveFilesInNewRevision(VersionedObject[] objects, String comment) throws XynaException;


  public long deleteFilesInNewRevision(String[] fileNamesWithRelativePath, String comment) throws XynaException;


  /**
   * zur�ckgegebener inputstream muss vom aufrufer geschlossen werden
   * @return null falls das file in der revision nicht existierte
   */
  public InputStream getContentOfFileInRevision(String fileNameWithRelativePath, long revision) throws XynaException;


  public String[] listFiles(long revision);


  public void shutdown();


  public void cleanupEarlierThan(long timestampMillis) throws XynaException;

  /**
   * gibt die informationen zum letzten change &lt;= der �bergebenen revision zur�ck
   */
  public Revision getRevision(long revision);
}
