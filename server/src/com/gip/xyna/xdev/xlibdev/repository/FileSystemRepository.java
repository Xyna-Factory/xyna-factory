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
package com.gip.xyna.xdev.xlibdev.repository;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;



public class FileSystemRepository implements Repository {

  private static final Logger logger = CentralFactoryLogging.getLogger(FileSystemRepository.class);

  private final String rootDir;
  private final ConcurrentMap<String, Map<Long, Revision>> revisionMap = new ConcurrentHashMap<String, Map<Long, Revision>>(16,
                                                                                                                                    0.75f,
                                                                                                                                    2);
  private final File history;
  private volatile Writer historyWriter;
  private final boolean closeToFlush;


  public interface RevisionNumberProvider {


    public long getCurrentRevision();


    public long incrementAndGetRevision();

  }

  private static class LocalRevisionNumberProvider implements RevisionNumberProvider {

    private final AtomicLong currentRevision;


    public LocalRevisionNumberProvider(long max) {
      currentRevision = new AtomicLong(max);
    }


    public long getCurrentRevision() {
      return currentRevision.get();
    }


    public long incrementAndGetRevision() {
      return currentRevision.incrementAndGet();
    }
  }


  private final RevisionNumberProvider idProvider;


  public FileSystemRepository(String rootDir, boolean closeToFlush, RevisionNumberProvider idProvider) {
    this.rootDir = rootDir;
    this.closeToFlush = closeToFlush;
    history = new File(rootDir, "history");
    if (!history.exists()) {
      if (!history.getParentFile().exists()) {
        history.getParentFile().mkdirs();
      }
      try {
        history.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException("Could not create file.", e);
      }
    }
    try {
      long max = initRevisionMap();
      if (idProvider == null) {
        idProvider = new LocalRevisionNumberProvider(max);
      }
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not access history", e);
    }
    this.idProvider = idProvider;
  }


  public FileSystemRepository(String rootDir, boolean closeToFlush) {
    this(rootDir, closeToFlush, null);
  }


  public synchronized void shutdown() {
    Writer w = historyWriter;
    if (w != null) {
      try {
        w.close();
      } catch (IOException e) {
        logger.warn("could not close stream to " + history.getAbsolutePath(), e);
      }
    }
  }


  public long deleteFilesInNewRevision(String[] fileNamesWithRelativePath, String comment) throws Ex_FileAccessException {
    long revision = nextRevision();
    for (String o : fileNamesWithRelativePath) {
      deleteFile(o, revision, comment);
    }
    return revision;
  }


  public long getCurrentRevision() {
    return idProvider.getCurrentRevision();
  }


  public long saveFilesInNewRevision(VersionedObject[] objects, String comment) throws Ex_FileAccessException {
    long revision = nextRevision();
    for (VersionedObject o : objects) {
      saveFile(o, revision, comment);
    }
    return revision;
  }

  private static final Pattern pspace = Pattern.compile(" ");

  private long initRevisionMap() throws Ex_FileAccessException {
    try {
      Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(history)), Constants.DEFAULT_ENCODING);
      try {
        s.useDelimiter("\n");
        long max = -1;
        while (s.hasNext()) {
          String line = s.next();
          try {
            Pair<String, Revision> r = parseLine(line);
            if (r == null) {
              continue;
            }
            if (r.getSecond().getRev() > max) {
              max = r.getSecond().getRev();
            }
            Map<Long, Revision> map = revisionMap.get(r.getFirst());
            if (map == null) {
              map = new ConcurrentHashMap<Long, Revision>();
              revisionMap.put(r.getFirst(), map);
            }
            map.put(r.getSecond().getRev(), r.getSecond());
          } catch (RuntimeException e) {
            logger.warn("Invalid line in history " + history.getAbsolutePath() + ": " + line, e);
          }
        }
        return max;
      } finally {
        s.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(history.getAbsolutePath());
    }
  }


  private void saveFile(VersionedObject o, long revision, String comment) throws Ex_FileAccessException {
    File f = getFileInRevision(o.getFileNameWithRelativePath(), revision);
    Map<Long, Revision> revs = revisionMap.get(o.getFileNameWithRelativePath());
    ObjectChange change;
    if (revs == null) {
      revs = new ConcurrentHashMap<Long, Revision>(1, 0.75f, 2);
      Map<Long, Revision> previous = revisionMap.putIfAbsent(o.getFileNameWithRelativePath(), revs);
      if (previous != null) {
        revs = previous;
        change = ObjectChange.MODIFIED;
      } else {
        change = ObjectChange.CREATED;
      }
    } else {
      change = ObjectChange.MODIFIED;
    }
    int state = 0;
    Revision rev;
    try {
      FileUtils.saveToFile(o.getContent(), f);
      state = 1;
      rev = writeToHistory(revision, o.getFileNameWithRelativePath(), change, comment);
      state = 2;
    } finally {
      if (state == 0) {
        //FIXME führt dazu, dass das erneute erste einfügen CREATE nicht korrekt erkennt
        revs.put(revision, new Revision(revision, System.currentTimeMillis(), ObjectChange.ERROR));
      } else if (state == 1) {
        revs.put(revision, new Revision(revision, System.currentTimeMillis(), ObjectChange.ERROR));
        FileUtils.deleteFileWithRetries(f);
      }
    }
    revs.put(revision, rev);
  }


  private void deleteFile(String fileNameWithRelativePath, long revision, String comment) throws Ex_FileAccessException {
    Map<Long, Revision> revs = revisionMap.get(fileNameWithRelativePath);
    if (revs == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Deleting file that was not present in repository before: " + rootDir + "/" + fileNameWithRelativePath);
      }
      revs = new ConcurrentHashMap<Long, Revision>(1);
      Map<Long, Revision> previous = revisionMap.putIfAbsent(fileNameWithRelativePath, revs);
      if (previous != null) {
        revs = previous;
      }
    }
    long current = getStoredRevision(revs, revision);
    if (current == -1 || revs.get(current).getChange() == ObjectChange.DELETED) {
      //ntbd: ist bereits gelöscht
      return;
    }
    Revision r =  writeToHistory(revision, fileNameWithRelativePath, ObjectChange.DELETED, comment);
    revs.put(revision, r);
  }
  
  
  //FIXME die history wird so nicht immer in der richtigen reihenfolge geschrieben. macht aber erstmal nichts.
  private synchronized Revision writeToHistory(long revision, String fileNameWithRelativePath, ObjectChange change, String comment)
      throws Ex_FileAccessException {
    int retries = 0;
    while (retries++ < 3) {
      if (historyWriter == null) {
        openHistoryWriter();
      }
      long ts = System.currentTimeMillis();
      try {
        historyWriter.write(String.valueOf(revision));
        historyWriter.write(" ");
        historyWriter.write(Constants.defaultUTCSimpleDateFormat().format(new Date(ts)));
        historyWriter.write(" ");
        historyWriter.write(fileNameWithRelativePath);
        historyWriter.write(" ");
        historyWriter.write(change.shortName());
        historyWriter.write(" ");
        if (comment != null) {
          for (int i = 0; i < comment.length(); i++) {
            char c = comment.charAt(i);
            switch (c) {
              case '\r' :
                historyWriter.write("\\r");
                break;
              case '\n' :
                historyWriter.write("\\n");
                break;
              case '\b' :
                historyWriter.write("\\b");
                break;
              default :
                historyWriter.write(c);
                break;
            }
          }
        }
        historyWriter.write("\n");
        flushHistoryWriter();
        return new Revision(revision, ts, change);
      } catch (IOException e) {
        historyWriter = null;
        openHistoryWriter();
        if (historyWriter == null) {
          //fehler
          throw new Ex_FileAccessException(history.getAbsolutePath(), e);
        } else {
          //retry
          continue;
        }
      }
    }
    throw new Ex_FileAccessException(history.getAbsolutePath());
  }


  private void flushHistoryWriter() throws IOException {
    historyWriter.flush();
    if (closeToFlush) {
      Writer w = historyWriter;
      historyWriter = null;
      w.close();
    }
  }


  private void openHistoryWriter() throws Ex_FileAccessException {
    try {
      historyWriter = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(history.getAbsolutePath(), true)));
    } catch (IOException e) {
      throw new Ex_FileAccessException(history.getAbsolutePath(), e);
    }
  }


  private File getFileInRevision(String relativePath, long revision) {
    File f = new File(rootDir, relativePath + "_" + revision);
    return f;
  }


  private long nextRevision() {
    return idProvider.incrementAndGetRevision();
  }


  public InputStream getContentOfFileInRevision(String fileNameWithRelativePath, long revision) throws Ex_FileAccessException {
    Map<Long, Revision> revisions = revisionMap.get(fileNameWithRelativePath);
    if (revisions == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("File unknown: " + fileNameWithRelativePath);
      }
      return null;
    }
    long rev = getStoredRevision(revisions, revision);
    if (rev == -1) {
      if (logger.isTraceEnabled()) {
        logger.trace("File " + fileNameWithRelativePath + " didn't exist in revision " + revision + ".");
      }
      return null;
    }
    Revision c = revisions.get(rev);
    if (c.getChange() == ObjectChange.DELETED || c.getChange() == ObjectChange.ERROR) {
      if (logger.isTraceEnabled()) {
        logger.trace("File " + fileNameWithRelativePath + " had state " + c.getRev() + " in revision " + rev + ".");
      }
      return null;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Returning filestream for file " + fileNameWithRelativePath + " from revision " + rev + ".");
    }
    File f = getFileInRevision(fileNameWithRelativePath, rev);
    try {
      return new BufferedInputStream(new FileInputStream(f));
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(fileNameWithRelativePath, e);
    }
  }

  /**
   * gibt das größte r zurück, was in der revisions-map ist, welches noch kleinergleich revision ist.  
   * gibt -1 zurück, falls kein solches r existiert.
   */
  private long getStoredRevision(Map<Long, Revision> revisions, long revision) {
    if (revisions.containsKey(revision)) {
      return revision;
    }
    //in unsortierter liste nach nächstkleinerer id suchen
    long revlower = -1;
    for (long l : revisions.keySet()) {
      if (l > revlower && l < revision) {
        revlower = l;
      }
    }
    return revlower;
  }


  public String[] listFiles(long revision) {
    Set<String> fileSet = new HashSet<String>();
    for (Entry<String, Map<Long, Revision>> e : revisionMap.entrySet()) {
      long rev = getStoredRevision(e.getValue(), revision);
      if (rev != -1) {
        Revision c = e.getValue().get(rev);
        if (c.getChange() == ObjectChange.DELETED || c.getChange() == ObjectChange.ERROR) {
          //kein file
        } else {
          fileSet.add(e.getKey());
        }
      }
    }
    return fileSet.toArray(new String[fileSet.size()]);
  }


  public void cleanupEarlierThan(long timestampMillis) throws Ex_FileAccessException {
    //suche zu löschende files/revisions
    Map<String, Set<Long>> deletionMap = new HashMap<String, Set<Long>>();
    for (Entry<String, Map<Long, Revision>> e : revisionMap.entrySet()) {
      Set<Long> delete = new HashSet<Long>();
      String filename = e.getKey();
      Map<Long, Revision> m = e.getValue();
      //sammle alle revisions, deren timestamp < x ist. merke den höchsten dieser timestamps separat, 
      //weil der darf nicht entfernt werden, weil er ja am ende des zeitintervalls gültig ist
      //wenn der letzte eintrag hingegen ein lösch-eintrag ist, darf er auch entfernt werdne.
      long max = -1;
      Revision maxRev = null;
      for (Entry<Long, Revision> el : m.entrySet()) {
        Revision rev = el.getValue();
        long ts = rev.getTimestamp();
        if (ts < max) {
          //revision gefunden, die älter als das aktuelle max ist. die kann also entfernt werden
          delete.add(rev.getRev());
        } else if (ts > max && ts < timestampMillis) {
          //revision gefunden, die entfernt werden kann, solange sie nicht selbst die maximale (<timestampMillis) ist.
          if (max > -1) {
            //die bisherige max-rev ist nun auf jeden fall redundant und kann entfernt werden
            delete.add(maxRev.getRev());
          }
          //maxrev merken
          maxRev = rev;
          max = ts;
        }
      }
      if (maxRev != null && maxRev.getChange() == ObjectChange.DELETED) {
        //falls maxrev ein delete ist, dann kann man die auch entfernen. es kann ja dann nicht in einem audit vorkommen (nach timestampMillis)
        delete.add(maxRev.getRev());
      }

      if (delete.size() > 0) {
        //aus lokaler map entfernen, damit andere threads die version nicht mehr finden
        for (Long del : delete) {
          m.remove(del);
        }

        deletionMap.put(filename, delete);
      }
    }

    if (deletionMap.size() > 0) {
      //lösche aus history-file
      deleteFromHistory(deletionMap);

      //lösche files
      for (Entry<String, Set<Long>> e : deletionMap.entrySet()) {
        for (Long rev : e.getValue()) {
          File f = getFileInRevision(e.getKey(), rev);
          if (f.exists()) { //könnte auch ein deletion-eintrag in der history sein, dann gibt es dazu kein file
            if (!f.delete()) {
              logger.info("Could not delete " + f.getAbsolutePath());
            } else if (logger.isDebugEnabled()) {
              logger.debug("deleted " + f.getAbsolutePath());
            }
          }
        }
      }
    }
  }


  private synchronized void deleteFromHistory(Map<String, Set<Long>> deletionMap) throws Ex_FileAccessException {
    /*
     * move history -> history.old
     * copy zeilenweise und lasse zu löschende einträge weg.
     */
    if (historyWriter != null) {
      try {
        historyWriter.close();
      } catch (IOException e) {
        //ignore
      }
      historyWriter = null;
    }
    File mvd = new File(history.getAbsolutePath() + "." + Constants.defaultUTCSimpleDateFormat().format(new Date()));
    FileUtils.moveFile(history, mvd);
    try {
      if (!history.createNewFile()) {
        throw new RuntimeException("Could not create history file " + history.getAbsolutePath());
      }
      openHistoryWriter();
      Scanner s = new Scanner(new BufferedInputStream(new FileInputStream(mvd)), Constants.DEFAULT_ENCODING);
      try {
        s.useDelimiter("\n");
        while (s.hasNext()) {
          String line = s.next();
          try {
            Pair<String, Revision> r = parseLine(line);
            if (r == null) {
              //ungültige zeile ignorieren
              continue;
            }
            Set<Long> revs = deletionMap.get(r.getFirst());
            if (revs != null && revs.contains(r.getSecond().getRev())) {
              //zeile nicht wieder rausschreiben, weil gelöscht
              continue;
            }
            //alle nicht gelöschten zeilen wieder zurückschreiben
            historyWriter.write(line);
            historyWriter.write('\n');
          } catch (RuntimeException e) {
            logger.warn("Invalid line in history " + mvd.getAbsolutePath() + ": " + line, e);
          }
        }
        flushHistoryWriter();
      } finally {
        s.close();
      }
      mvd.delete();
    } catch (IOException e) {
      throw new Ex_FileAccessException(history.getAbsolutePath(), e);
    }
  }


  private Pair<String, Revision> parseLine(String line) {
    String[] parts = pspace.split(line, 5);
    //revision, timestmap, filename, ObjectChange, comment
    long revision = Long.valueOf(parts[0]);
    String filename = parts[2];
    ObjectChange change = ObjectChange.valueOfShortName(parts[3]);
    long ts;
    try {
      ts = Constants.defaultUTCSimpleDateFormat().parse(parts[1]).getTime();
    } catch (ParseException e) {
      logger.warn("Could not parse timestamp: " + parts[1] + " in " + history.getAbsolutePath() + ": " + line, e);
      return null;
    }
    return Pair.of(filename, new Revision(revision, ts, change));
  }


  @Override
  public Revision getRevision(long revision) {
    long max = -1;
    Revision rev = null;
    for (Map<Long, Revision> m : revisionMap.values()) {
      for (Long l : m.keySet()) {
        if (l == revision) {
          return m.get(l);
        } else if (l < revision && l > max) {
          max = l;
          rev = m.get(l);
        }
      }
    }
    return rev;
  }

}
