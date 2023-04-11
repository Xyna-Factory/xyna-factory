/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class FileUtils {


  private static final AtomicLong lastGC = new AtomicLong(0);
  private static final long MIN_GC_INTERVAL = 1000;

  private static final Logger logger = CentralFactoryLogging.getLogger(FileUtils.class);

  private static final Random random = new Random();

  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir) throws Ex_FileAccessException {
    zipDir(dirToZip, zos, basedir, null, null, null);
  }
  
  public interface FileInputStreamCreator {

    InputStream create(File f) throws FileNotFoundException;
    
  }
  

  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir, String zipPrefixDirectory)
                  throws Ex_FileAccessException {
    zipDir(dirToZip, zos, basedir, null, zipPrefixDirectory, null);
  }


  public static boolean deleteDirectory(File dir) {
    if (!dir.exists()) {
      return true;
    }
    if (!dir.isDirectory()) {
      return false;
    }
    if (logger.isTraceEnabled()) {
      logger.trace("deleting dir " + dir.getAbsolutePath());
    }
    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        if (!deleteDirectory(files[i])) {
          return false;
        }
      } else {
        if (!deleteFileWithRetries(files[i], Constants.DELETE_FILE_RETRIES)) {
          return false;
        }
      }
    }
    return deleteFileWithRetries(dir, Constants.DELETE_FILE_RETRIES);
  }


  
  /**
  * Löscht das angegebene Verzeichnis, falls es leer ist und rekursiv alle übergeordneten Verzeichnisse,
  * solange diese leer sind und nicht dem Verzeichnis 'except' entsprechen.
  * @param basedir
  * @param except
  */
 public static void deleteEmptyDirectoryRecursively(File basedir, File except) {
   if (basedir == null || !basedir.isDirectory()) {
     //basedir ist kein Verzeichnis
     return;
   }
   
   if (except != null && basedir.getAbsolutePath().equals(except.getAbsolutePath())) {
     //Verzeichnis soll nicht gelöscht werden
     return;
   }
   
   if (basedir.delete()) { //Verzeichnis löschen, falls es leer ist
     if (logger.isTraceEnabled()) {
       logger.trace("Empty directory " + basedir.getPath() + " deleted");
     }
     //Verzeichnis war leer -> parent ist nun evtl. auch leer
     deleteEmptyDirectoryRecursively(basedir.getParentFile(), except);
   }
 }


  private static boolean deleteFileWithRetries(File f, int retryMaxCnt) {
    int retryCnt = 0;
    while (!f.delete() && f.exists() && retryCnt++ < retryMaxCnt) {
      if (retryCnt % 10 == 1) {
        long lastgc = lastGC.get();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastgc > MIN_GC_INTERVAL) {
          if (lastGC.compareAndSet(lastgc, currentTime)) {
            System.gc();
          }
        }
        Thread.yield();
      } else {
        if (retryCnt > 2) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
          }
        }
      }
    }
    if (retryCnt > retryMaxCnt) {
      if (logger.isInfoEnabled()) {
        logger.info("could not delete file " + f.getAbsolutePath());
      }
      return false;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("deleted file " + f.getAbsolutePath());
      }
      return true;
    }
  }


  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir, FilenameFilter ff)
                  throws Ex_FileAccessException {
    zipDir(dirToZip, zos, basedir, ff, null, null, null);
  }
  
  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir, FilenameFilter ff,
                            String zipPrefixDirectory) throws Ex_FileAccessException {
    zipDir(dirToZip, zos, basedir, ff, zipPrefixDirectory, null, null);
  }

  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir, FilenameFilter ff,
                            String zipPrefixDirectory, FileInputStreamCreator fileCreator) throws Ex_FileAccessException {
    zipDir(dirToZip, zos, basedir, ff, zipPrefixDirectory, fileCreator, null);
  }

  
  public interface ZipEntryVisitor {
    public void visit(ZipEntry entry);
  }
  
  /**
   * Entries im ZipFile sind relativ zu basedir.
   * @param dirToZip welches verzeichnis gezipped wird
   * @param zos wohin gezipped wird
   * @param basedir prefix von dirToZip, welches "abgeschnitten" werden soll, d.h. nur seine unterverzeichnisse sollen ins zip
   * @param ff welche dateien werden berücksichtigt
   * @param zipPrefixDirectory entries im zipfile bekommen dieses prefix-dir
   * @param fileCreator möglichkeit, file content zu überschreiben mit anderem content
   * @throws Ex_FileAccessException
   */
  public static void zipDir(File dirToZip, ZipOutputStream zos, File basedir, FilenameFilter ff,
                            String zipPrefixDirectory, FileInputStreamCreator fileCreator, ZipEntryVisitor zev) throws Ex_FileAccessException {

    File[] content = dirToZip.listFiles(ff);
    if (content == null || content.length == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("No files found in directory " + dirToZip.getPath());
      }
      return;
    }
    for (File f : content) {
      addToZip(zos, f, basedir, ff, zipPrefixDirectory, fileCreator, zev);
    }
  }


  private static void addToZip(ZipOutputStream zos, File f, File basedir, FilenameFilter ff, String zipPrefixDirectory, FileInputStreamCreator fileCreator,
                               ZipEntryVisitor zev) throws Ex_FileAccessException {
    byte[] data = new byte[2048];
    int length;
    
    String path = getRelativePath(basedir.getAbsolutePath(), f.getAbsolutePath());
    if (zipPrefixDirectory != null) {
      path = zipPrefixDirectory + Constants.fileSeparator + path;
    }
    path = path.replace(File.separatorChar, '/'); // TODO this is just a workaround to make it work under windows

    try {
      if (f.isDirectory()) {

        //create directory entry
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Adding path = " + path);
          }
          ZipEntry ze = new ZipEntry(path + '/');
          ze.setLastModifiedTime(FileTime.fromMillis(f.lastModified()));
          if (zev != null) {
            zev.visit(ze);
          }
          zos.putNextEntry(ze);
        } finally {
          zos.closeEntry();
        }


        zipDir(f, zos, basedir, ff, zipPrefixDirectory, fileCreator, zev);
      } else {
        InputStream fis;
        try {
          if (fileCreator == null) {
            fis = new FileInputStream(f);
          } else {
            fis = fileCreator.create(f);
          }
        } catch (FileNotFoundException e) {
          throw new Ex_FileAccessException(f.getAbsolutePath(), e);
        }
        if (fis != null) {
          try {
            if (logger.isDebugEnabled()) {
              logger.debug("Adding path = " + path);
            }
            ZipEntry ze = new ZipEntry(path);
            ze.setLastModifiedTime(FileTime.fromMillis(f.lastModified()));
            if (zev != null) {
              zev.visit(ze);
            }
            zos.putNextEntry(ze);
            while ((length = fis.read(data)) != -1) {
              zos.write(data, 0, length);
            }
          } finally {
            try {
              zos.closeEntry();
            } finally {
              fis.close();
            }
          }
        }
      }
    } catch (IOException e) {
      throw new Ex_FileWriteException(path, e);
    }
  }


  /**
   * Base und path beginnen mit gleichem substring, zurückgegeben wird der anteil von path, der nach base beginnt.
   */
  public static String getRelativePath(String base, String path) {
    if (!path.startsWith(base)) {
      throw new IllegalArgumentException("path '" + path + "' must start with '" + base + "'.");
    }
    String r = path.substring(base.length());
    if (r.startsWith(File.separator)) {
      r = r.substring(1);
    }
    return r;
  }


  public static void zipDirectory(File zipFile, File dirToZip) throws Ex_FileAccessException {
    zipDirectory(zipFile, dirToZip, null);
  }


  /**
   * Erstellt ein ZipFile, welches den Inhalt des angegebenen Verzeichnisses enthält (ohne das Verzeichnis selbst).
   * @param zipFile
   * @param dirToZip
   * @param zipPrefixDirectory
   * @throws Ex_FileAccessException
   */
  public static void zipDirectory(File zipFile, File dirToZip, String zipPrefixDirectory) throws Ex_FileAccessException {
    try {
      if (!zipFile.exists()) {
        if (zipFile.getParentFile() != null) {
          zipFile.getParentFile().mkdirs();
        }
        zipFile.createNewFile();
      }
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
      try {
        zipDir(dirToZip, zos, dirToZip, zipPrefixDirectory);
      } finally {
        zos.flush();
        zos.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(zipFile.getAbsolutePath(), e);
    }
  }


  /**
   * Kopiert "in" oder falls "in" ein verzeichnis ist den Inhalt von "in" in das Verzeichnis "outDir" kopiert keine .svn
   * Ordner.
   * @throws Ex_FileAccessException falls IO Fehler einem File zugeordnet werden kann
   */
  public static void copyRecursively(File in, File outDir) throws Ex_FileAccessException {
    if (in.isDirectory()) {
      if (in.getName().equals(".svn")) {
        return;
      }
      File[] files = in.listFiles();
      if (files.length == 0) {
        if (!outDir.exists()) {
          outDir.mkdirs();
        }
      } else {
        for (File f : files) {
          copyRecursively(f, outDir);
        }
      }
    } else {
      File outFile = new File(outDir, in.getName());
      if (!outFile.exists()) {
        outDir.mkdirs();
        try {
          outFile.createNewFile();
        } catch (IOException e) {
          throw new Ex_FileAccessException(outFile.getAbsolutePath(), e);
        }
      }
      copyFile(in, outFile);
    }
  }
  
  public static void copyRecursivelyWithFolderStructure(File in, File outDir) throws Ex_FileAccessException {
    if (in.isDirectory()) {
      if (in.getName().equals(".svn")) {
        return;
      }
      File[] files = in.listFiles();
      if (files.length == 0) {
        if (!outDir.exists()) {
          outDir.mkdirs();
        }
      } else {
        for (File f : files) {
          if(f.isDirectory()) {
            copyRecursivelyWithFolderStructure(f, new File(outDir, f.getName()));
          } else {
            copyRecursivelyWithFolderStructure(f, outDir);
          }
        }
      }
    } else {
      File outFile = new File(outDir, in.getName());
      if (!outFile.exists()) {
        outDir.mkdirs();
        try {
          outFile.createNewFile();
        } catch (IOException e) {
          throw new Ex_FileAccessException(outFile.getAbsolutePath(), e);
        }
      }
      copyFile(in, outFile);
    }
  }

  
  private static int getDefaultBufferSize(long contentLength) {
    return 256*1024;
  }
  
  
  
  public static void writeStringToFile(String content, File f, String charsetName) throws Ex_FileWriteException {
    executeWriteStringToFile(content, f, charsetName, false);
  }
  
  public static void writeStringToFile(String content, File f) throws Ex_FileWriteException {
    executeWriteStringToFile(content, f, Constants.DEFAULT_ENCODING, false);
  }
  
  public static void appendStringToFile(String content, File f, String charsetName) throws Ex_FileWriteException {
    executeWriteStringToFile(content, f, charsetName, true);
  }

  private static final HashParallelReentrantLock<String> fileLock = new HashParallelReentrantLock<>(100);
  
  private static void executeWriteStringToFile(String content, File f, String charsetName, boolean append) throws Ex_FileWriteException {
    String p = f.getAbsolutePath();
    //bei append vor concurrency beim write schützen. bei !append nur vor concurrency beim file.create
    if (append) {
      fileLock.lock(p);
    }
    try {
      if (!f.exists()) {
        if (!append) {
          fileLock.lock(p);
        }
        try {
          if (append || !f.exists()) {
            if (f.getParentFile() != null) {
              f.getParentFile().mkdirs();
            }
            f.createNewFile();
          }
        } finally {
          if (!append) {
            fileLock.unlock(p);
          }
        }
      }
      try (FileOutputStream out = new FileOutputStream(f, append);
           BufferedWriter bw =
               new BufferedWriter(new OutputStreamWriter(out, charsetName), getDefaultBufferSize(content.length()))) {
        bw.write(content);
        bw.flush();
      }
    } catch (IOException e) {
      throw new Ex_FileWriteException(f.getAbsolutePath(), e);
    } finally {
      if (append) {
        fileLock.unlock(p);
      }
    }
  }

  public static String readFileAsString(File f, boolean tryForceRead) throws Ex_FileWriteException {
    return readFileAsString(f, tryForceRead, Constants.DEFAULT_ENCODING);
  }


  public static String readFileAsString(File f, boolean tryForceRead, String encoding) throws Ex_FileWriteException {
    try {
      if (!f.exists()) {
        throw new Ex_FileWriteException(f.getAbsolutePath());
      }
      if (f.length() == 0 && !tryForceRead) {
        return "";
      }
      int fileLength = f.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)f.length();

      StringBuilder stringBuilder;
      if (fileLength == 0 && tryForceRead) {
        stringBuilder = new StringBuilder();
      } else {
        stringBuilder = new StringBuilder(fileLength);
      }

      FileInputStream in = new FileInputStream(f);
      
      BufferedReader br;
      if (fileLength == 0 && tryForceRead) {
        br = new BufferedReader(new InputStreamReader(in, encoding));
      } else {
        br = new BufferedReader(new InputStreamReader(in, encoding), Math.min(fileLength, getDefaultBufferSize(fileLength)));
      }
      
      try {
        char[] buffer = new char[2048];
        int countRead=0;
        while((countRead=br.read(buffer)) != -1){
            String readData = String.valueOf(buffer, 0, countRead);
            stringBuilder.append(readData);
        }
      } finally {
        br.close();
      }
      return stringBuilder.toString();
    } catch (IOException e) {
      throw new Ex_FileWriteException(f.getAbsolutePath(), e);
    }
  }
  
  public static String readFileAsString(File f) throws Ex_FileWriteException {
    return readFileAsString(f, false);
  }
  
  
  public static boolean compareXMLs(File file1, File file2) throws Ex_FileWriteException {
    return compareXMLs(readFileAsString(file1), readFileAsString(file2));
  }
  
  
  //returns true if equal, false otherwise
  public static boolean compareXMLs(String xml1, String xml2) {
    String skippedXml1 = skipPastFirstComment(xml1);
    String skippedXml2 = skipPastFirstComment(xml2);
    return skippedXml1.equals(skippedXml2);
  }


  private static String skipPastFirstComment(String xml) {
    final String COMMENT_END = "-->";
    int index = xml.indexOf(COMMENT_END);
    if (index >= 0) {
      return xml.substring(index + COMMENT_END.length());
    } else {
      return xml;
    }
  }
  
  
  public static void writeStreamToFile(InputStream is, File f) throws Ex_FileWriteException {
    try {
      if (!f.exists()) {
        if (f.getParentFile() != null) {
          f.getParentFile().mkdirs();
        }
        f.createNewFile();
      }
      //writeToFileInternallyViaBufferedStreams(is, f, 256*1024);
      writeToFileInternallyViaChannel(is, f, getDefaultBufferSize(is.available()));
    } catch (IOException e) {
      throw new Ex_FileWriteException(f.getAbsolutePath(), e);
    }
  }


  /**
   * does not close inputstream 
   */
  private static void writeToFileInternallyViaChannel(InputStream is, File f, int bufferSize) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    try {
      FileChannel channelOut = out.getChannel();

      ReadableByteChannel channelIn = Channels.newChannel(new BufferedInputStream(is, bufferSize));
      channelOut.transferFrom(channelIn, 0, Integer.MAX_VALUE / 2);     
    } finally {
      out.close();
    }
  }


  /**
   * does not close inputstream 
   */
  private static void writeToFileInternallyViaBufferedStreams(InputStream is, File f, int bufferSize) throws IOException {
    try (FileOutputStream out = new FileOutputStream(f);
         BufferedOutputStream bos = new BufferedOutputStream(out, bufferSize)) {
      BufferedInputStream bis = new BufferedInputStream(is, bufferSize);
      byte[] data = new byte[bufferSize];
      int length = 0;
      while ((length = bis.read(data)) != -1) {
        bos.write(data, 0, length);
      }
    }   
  }
  
  /**
   * füllt die liste mit gefundenen files
   */
  public static void findFilesRecursively(File basedir, List<File> list, FilenameFilter ff) {
    File[] files = basedir.listFiles(ff);
    if (files == null)
      return;

    for (File f : files) {
      if (f.isDirectory()) {
        findFilesRecursively(f, list, ff);
      } else {
        list.add(f);
      }
    }
  }

  
  public static File makeTemporaryDirectory() throws IOException {
    final Path temp = Files.createTempDirectory("xyna");
    return temp.toFile();
  }


  public static boolean deleteDirectoryRecursively(File basedir) {
    return deleteDirectoryRecursively(basedir, Constants.DELETE_FILE_RETRIES);
  }


  public static boolean deleteDirectoryRecursively(File basedir, int maxRetries) {
    if (!basedir.exists()) {
      return true;
    }
    if (!basedir.isDirectory()) {
      return false;
    }
    File[] files = basedir.listFiles();
    boolean allFilesDeleted = true;
    for (File f : files) {
      if (f.isDirectory()) {
        if (!deleteDirectoryRecursively(f)) {
          allFilesDeleted = false;
        }
      } else {
        if (!deleteFileWithRetries(f, maxRetries)) {
          allFilesDeleted = false;
        }
      }
    }
    if (allFilesDeleted) {
      return deleteFileWithRetries(basedir, maxRetries);
    } else {
      //braucht man gar nicht erst versuchen
      return false;
    }
  }


  /**
   * @param in
   * @param out
   * @throws Ex_FileAccessException falls eines der beiden files nicht vorhanden ist
   */
  public static void copyFile(File in, File out) throws Ex_FileAccessException {
    copyFile(in,out,false);
  }
  
  /**
   * @param in
   * @param out
   * @param createOut
   * @throws Ex_FileAccessException falls eines der beiden files nicht vorhanden ist oder out nicht angelegt werden konnte
   */
  public static void copyFile(File in, File out, boolean createOut) throws Ex_FileAccessException {
    FileInputStream fis;
    try {
      fis = new FileInputStream(in);
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(in.getAbsolutePath(), e);
    }
    try {
      try {
        FileOutputStream fos;
        try {
          if( createOut ) {
            out.getParentFile().mkdirs();
            out.createNewFile();
          }
          fos = new FileOutputStream(out);
        } catch (IOException e) {
          throw new Ex_FileAccessException(out.getAbsolutePath(), e);
        }

        try {
          FileChannel inChannel = fis.getChannel();
          FileChannel outChannel = fos.getChannel();
          long transfer = 0;
          while (transfer < inChannel.size()) {
            transfer += inChannel.transferTo(transfer, inChannel.size() - transfer, outChannel);
          }
          fos.flush();
        } finally {
          fos.close();
        }
      } finally {
        fis.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(in.getAbsolutePath() + ", " + out.getAbsolutePath(), e);
    }
  }


  /**
   * Kopiert alle sourceFiles ins targetDir, relativ zum in relativeDir angegebenen pfad beispiel: copy
   * "/bla/blubb/pfad/file" "/bla/blubb" "/targetDir" erstellt datei /targetDir/pfad/file
   * @param sourceFiles
   * @param relativeDir - Das Verzeichnis, relativ zu dem die pfade der sourceFiles ins targetDir kopiert werden.
   * @param targetDir
   * @throws Ex_FileAccessException falls ein IO Fehler einem file zugeordnet werden kann
   * @throws IOException falls ein IO Fehler nicht einem file zugeordnet werden kann
   */
  public static void copyFiles(List<File> sourceFiles, File relativeDir, File targetDir)
                  throws Ex_FileAccessException, IOException {
    for (File source : sourceFiles) {
      String relativeDirCanonicalPath;
      try {
        relativeDirCanonicalPath = relativeDir.getCanonicalPath();
      } catch (IOException e) {
        throw new Ex_FileAccessException(relativeDir.getAbsolutePath(), e);
      }
      String sourceCanonicalPath;
      try {
        sourceCanonicalPath = source.getCanonicalPath();
      } catch (IOException e) {
        throw new Ex_FileAccessException(source.getAbsolutePath(), e);
      }
      File target = new File(targetDir, FileUtils.getRelativePath(relativeDirCanonicalPath, sourceCanonicalPath));
      if (source.isDirectory()) {
        target.mkdirs();
      } else {
        target.getParentFile().mkdirs();
        try {
          target.createNewFile();
        } catch (IOException e) {
          throw new Ex_FileAccessException(target.getAbsolutePath(), e);
        }
        copyFile(source, target);
      }
    }
  }


  /**
   * Achtung: Stream wird nicht geschlossen. FIXME ACT Fehler raus
   */
  public static File[] saveZipToDir(ZipInputStream zipStream, File dirToSaveTo) throws Ex_FileAccessException,
                  XACT_JarFileUnzipProblem {
    List<File> files = new ArrayList<File>();
    File f = null;
    ZipEntry ze = null;
    try {
      while ((ze = zipStream.getNextEntry()) != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("found entry " + ze.getName());
        }
        f = new File(dirToSaveTo, ze.getName());
        if (ze.isDirectory()) {
          if (logger.isDebugEnabled()) {
            logger.debug("creating dir " + f.getPath());
          }
          f.mkdirs();
          f.setLastModified(ze.getLastModifiedTime().toMillis());
          continue;
        }
        saveToFile(zipStream, f);
        f.setLastModified(ze.getLastModifiedTime().toMillis());
        files.add(f);
      }
      zipStream.closeEntry();
      return files.toArray(new File[0]);
    } catch (IOException e) {
      throw new XACT_JarFileUnzipProblem(f != null ? f.getPath() : "<unknown>", e.getMessage(), e);
    }
  }

  /**
   * Achtung: InputStream wird nicht geschlossen. (koennte zipinputstream sein) 
   */
  public static void saveToFile(InputStream is, File f) throws Ex_FileAccessException {
    try {
      if (!f.exists()) {
        File parentDir = f.getParentFile();
        if (parentDir != null) {
          f.getParentFile().mkdirs();
        }
        f.createNewFile();
      }
      FileOutputStream out = new FileOutputStream(f);
      BufferedOutputStream bos = new BufferedOutputStream(out);
      int length = 0;
      try {
        byte[] data = new byte[2048];
        while ((length = is.read(data)) != -1) {
          bos.write(data, 0, length);
        }
        bos.flush();
      } finally {
        bos.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(f.getAbsolutePath(), e);
    }
  }


  public static void moveFile(File source, File target) throws Ex_FileAccessException {
    if (!source.renameTo(target)) {
      copyFile(source, target);
      deleteFileWithRetries(source);
    }
  }
  

  public static boolean deleteFileWithRetries(File f) {
    return deleteFileWithRetries(f, Constants.DELETE_FILE_RETRIES);
  }
        

  public static List<File> getMDMFiles(File basedir, List<File> list) {
    File[] files = basedir.listFiles();
    if (files == null) {
      if (basedir.exists()) {
        RuntimeException ex = new RuntimeException("Unexpected error: Failed to read XML files");
        logger.error("Error reading files from directory '" + basedir.getAbsolutePath() + "'", ex);
        throw ex;
      } else {
        return list;
      }
    }
    for (File f : files) {
      if (f.isDirectory()) {
        getMDMFiles(f, list);
      } else {
        if (f.getName().endsWith(".xml")) {
          list.add(f);
        }
      }
    }
    return list;
  }
  
  
  public static List<File> getMDMFiles(String application, String version) {
    List<File> result = new ArrayList<>();
    
    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(application, version, null);
    } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("Unknown application: (name='" + application + "', version='" + version + "')", e);
    }
    File basedir = new File(RevisionManagement.getPathForRevision(PathType.XMOM, revision));
    
    File[] files = basedir.listFiles();
    if (files == null) {
      RuntimeException ex = new RuntimeException("Unexpected error: Failed to read XML files");
      logger.error("Error reading files from directory '" + basedir.getAbsolutePath() + "'", ex);
      throw ex;
    }
    
    for (File f : files) {
      if (f.isDirectory()) {
        getMDMFiles(f, result);
      } else {
        if (f.getName().endsWith(".xml")) {
          result.add(f);
        }
      }
    }
    
    return result;
  }
  
  
  /**
   * Löscht alle Files und Directories unterhalb von startFolder, die nicht 
   * durch notToDelete ausgeschlossen werden. Wenn in notToDelete ein Verzeichnis
   * angegeben ist, bleibt der gesamte Inhalt bestehen.
   * @param startFolder
   * @param notToDelete
   */
  public static void deleteAllBut(File startFolder, FileFilter notToDelete) {
    //zu löschende Files/Directories bestimmen
    FileFilter toDelete = new InverseFilter(notToDelete);
    File[] deleteFiles = startFolder.listFiles(toDelete);
    if (deleteFiles == null || deleteFiles.length == 0) {
      return;
    }
    
    for (File childfile : deleteFiles) {
      if (childfile.isDirectory()) {
        //Directories rekursiv löschen
        deleteAllBut(childfile, notToDelete);
      }
      
      //File bzw. leeres Directory löschen
      childfile.delete();
    }
  }

  /**
   * FileFilter, der das Ergebnis eines anderen Filters invertiert.
   */
  private static class InverseFilter implements FileFilter {
    
    private FileFilter filter;
    
    public InverseFilter(FileFilter filter) {
      this.filter = filter;
    }

    public boolean accept(File pathname) {
      return !filter.accept(pathname);
    }
  }

  
  /**
   * @param zipFile zip to substitute files in
   * @param directoryOfFilesToZip basisverzeichnis von zu zippenden files, wobei die zipentry namen relativ zu diesem verzeichnis sind
   * @param filenameFilter files to be added to zip
   */
  public static void substituteOrAddFilesInZipFile(File zipFile, File directoryOfFilesToZip,
                                                   FilenameFilter filenameFilter, boolean compress) throws Ex_FileAccessException {
    String directoryOfFilesToZipPath = directoryOfFilesToZip.getAbsolutePath();
    String zipPath = zipFile.getAbsolutePath();
    
    List<File> newFiles = new ArrayList<>();
    findFilesRecursively(directoryOfFilesToZip, newFiles, filenameFilter);
    List<String> fileNames = new ArrayList<>();
    Set<String> relativePaths = new HashSet<>();
    for (File f : newFiles) {
      String s = f.getAbsolutePath();
      fileNames.add(s);
      relativePaths.add(getRelativePath(directoryOfFilesToZipPath, s));
    }

    File tempFile = new File(zipPath + ".tmp");
    String tempFilePath = tempFile.getAbsolutePath();
    ZipFile oldZip;
    try {
      oldZip = new ZipFile(zipFile);
    } catch (IOException e) {
      throw new Ex_FileAccessException(zipPath, e);
    }

    boolean success = false;
    try {
      try {
        ZipOutputStream newZipStream;
        try {
          newZipStream =
              new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile), getDefaultBufferSize(zipFile.length()
                  + newFiles.size() * 1024)));
          if (!compress) {
            newZipStream.setLevel(Deflater.NO_COMPRESSION);
          }
        } catch (FileNotFoundException e) {
          throw new Ex_FileAccessException(tempFilePath, e);
        }
        try {

          byte[] b = new byte[getDefaultBufferSize(0)];
          Enumeration<? extends ZipEntry> entries = oldZip.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (relativePaths.contains(entry.getName())) {
              continue;
            }

            try {
              //neuen entry erstellen, falls sich compression-art ändert
              ZipEntry newEntry = new ZipEntry(entry.getName());
              newEntry.setComment(entry.getComment());
              newEntry.setTime(entry.getTime());
              newZipStream.putNextEntry(newEntry);
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFilePath, e);
            }
            if (!entry.isDirectory()) {
              int length;
              try {
                InputStream is = oldZip.getInputStream(entry);
                while ((length = is.read(b)) != -1) {
                  newZipStream.write(b, 0, length);
                }
              } catch (IOException e) {
                throw new Ex_FileAccessException(zipPath, e);
              }

            }
            try {
              newZipStream.closeEntry();
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFilePath, e);
            }
          }

          //neue files anhängen
          for (int i = 0; i<newFiles.size(); i++) {
            File f = newFiles.get(i);
            String fName = fileNames.get(i);
            ZipEntry entry =
                new ZipEntry(getRelativePath(directoryOfFilesToZipPath, fName));
            try {
              newZipStream.putNextEntry(entry);
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFilePath, e);
            }
            InputStream bis;
            try {
              bis = new BufferedInputStream(new FileInputStream(f));
            } catch (FileNotFoundException e) {
              throw new Ex_FileAccessException(fName, e);
            }
            try {
              int length;
              try {
                while ((length = bis.read(b)) != -1) {
                  newZipStream.write(b, 0, length);
                }
              } catch (IOException e) {
                throw new Ex_FileAccessException(fName, e);
              }
            } finally {
              try {
                bis.close();
              } catch (IOException e) {
                throw new Ex_FileAccessException(fName, e);
              }
            }
            try {
              newZipStream.closeEntry();
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFilePath, e);
            }
          }

        } finally {
          try {
            newZipStream.close();
          } catch (IOException e) {
            throw new Ex_FileAccessException(tempFilePath, e);
          }
        }
      } finally {
        try {
          oldZip.close();
        } catch (IOException e) {
          throw new Ex_FileAccessException(zipPath, e);
        }
      }

      //neues zip umbenennen in alten namen
      zipFile.delete();
      tempFile.renameTo(zipFile);
      success = true;
    } finally {
      if (!success) {
        tempFile.delete();
      }
    }
  }

  
  // TODO merge with substituteOrAddFilesInZipFile
  public static void removeFromZipFile(File zipFile, FileFilter fileFilter, boolean compress) throws Ex_FileAccessException {

    File tempFile = new File(zipFile.getAbsolutePath() + ".tmp");
    ZipFile oldZip;
    try {
      oldZip = new ZipFile(zipFile);
    } catch (IOException e) {
      throw new Ex_FileAccessException(zipFile.getAbsolutePath(), e);
    }

    boolean success = false;
    try {
      try {
        ZipOutputStream newZipStream;
        try {
          newZipStream = new ZipOutputStream(new FileOutputStream(tempFile));
        } catch (FileNotFoundException e) {
          throw new Ex_FileAccessException(tempFile.getAbsolutePath(), e);
        }
        if (!compress) {
          newZipStream.setLevel(Deflater.NO_COMPRESSION);
        }
        try {

          byte[] b = new byte[getDefaultBufferSize(0)];
          Enumeration<? extends ZipEntry> entries = oldZip.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            
            if (fileFilter.accept(new File(entry.getName()))) {
              continue;
            }

            try {
              //neuen entry erstellen, falls sich compression-art ändert
              ZipEntry newEntry = new ZipEntry(entry.getName());
              newEntry.setComment(entry.getComment());
              newEntry.setTime(entry.getTime());
              newZipStream.putNextEntry(newEntry);
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFile.getAbsolutePath(), e);
            }
            if (!entry.isDirectory()) {
              int length;
              try {
                InputStream is = oldZip.getInputStream(entry);
                while ((length = is.read(b)) != -1) {
                  newZipStream.write(b, 0, length);
                }
              } catch (IOException e) {
                throw new Ex_FileAccessException(zipFile.getAbsolutePath(), e);
              }

            }
            try {
              newZipStream.closeEntry();
            } catch (IOException e) {
              throw new Ex_FileAccessException(tempFile.getAbsolutePath(), e);
            }
          }

        } finally {
          try {
            newZipStream.close();
          } catch (IOException e) {
            throw new Ex_FileAccessException(tempFile.getAbsolutePath(), e);
          }
        }
      } finally {
        try {
          oldZip.close();
        } catch (IOException e) {
          throw new Ex_FileAccessException(zipFile.getAbsolutePath(), e);
        }
      }

      //neues zip umbenennen in alten namen
      zipFile.delete();
      tempFile.renameTo(zipFile.getAbsoluteFile());
      success = true;
    } finally {
      if (!success) {
        tempFile.delete();
      }
    }
  }


  /**
   * entzippt alle auf den filter passenden files des zips ins targetdir. überschreibt ggfs vorhandene dateien.
   */
  public static void unzip(String zipFile, String targetDir, FileFilter fileFilter) throws Ex_FileAccessException {
    ZipFile oldZip;
    try {
      oldZip = new ZipFile(zipFile);
    } catch (IOException e) {
      throw new Ex_FileAccessException(zipFile, e);
    }
    List<Pair<Long, File>> createdDirectories = new ArrayList<>();
    try {
      Enumeration<? extends ZipEntry> entries = oldZip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File targetFile = new File(targetDir, entry.getName());
        if (!fileFilter.accept(targetFile)) {
          continue;
        }
        if (!targetFile.exists()) {
          if (entry.isDirectory()) {
            targetFile.mkdirs();
            //directory-timestamp am ende setzen, weil jede darin erzeugte datei den zeitstempel umsetzt
            createdDirectories.add(Pair.of(entry.getLastModifiedTime().toMillis(), targetFile));
            continue;
          } else {
            targetFile.getParentFile().mkdirs();
          }
          try {
            targetFile.createNewFile();
          } catch (IOException e) {
            throw new Ex_FileAccessException(targetFile.getAbsolutePath(), e);
          }
        } else if (entry.isDirectory()) {
          continue;
        }
        InputStream is;
        try {
          is = oldZip.getInputStream(entry);
        } catch (IOException e) {
          throw new Ex_FileAccessException(zipFile, e);
        }
        try {
          writeToFileInternallyViaChannel(is, targetFile, getDefaultBufferSize(0));
        } catch (IOException e) {
          throw new Ex_FileAccessException(targetFile.getAbsolutePath(), e);
        }
        targetFile.setLastModified(entry.getLastModifiedTime().toMillis());
      }
    } finally {
      try {
        oldZip.close();
      } catch (IOException e) {
        throw new Ex_FileAccessException(zipFile, e);
      }
    }
    for (Pair<Long, File> createdDir : createdDirectories) {
      createdDir.getSecond().setLastModified(createdDir.getFirst());
    }
  }


  public static int countFilesRecursively(File basedir, FilenameFilter filenameFilter) {
    List<File> list = new ArrayList<File>();
    findFilesRecursively(basedir, list, filenameFilter);
    return list.size();
  }


  public static synchronized InputStream getInputStreamFromResource(String fileName, ClassLoader cl) throws Ex_FileAccessException {
    try {
      Enumeration<URL> urls = cl.getResources(fileName);
      URL url = null;
      while (urls.hasMoreElements()) {
        url = urls.nextElement();
      }
      //die letzte ressource zurückgeben, weil die reihenfolge ist: erst parent-classloader, dann lokaler.
      if (url != null) {
        URLConnection urlcon = (URLConnection) url.openConnection();
        //deactivate cache to not get an old version
        boolean b = urlcon.getUseCaches();
        urlcon.setUseCaches(false);
        try {
          return urlcon.getInputStream();
        } finally {
          //reset caching!
          try {
            urlcon.setUseCaches(b);
          } catch (IllegalStateException e) {
            //ntbd - dann halt nicht
          }
        }
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(fileName, e);
    }
    return null;
  }


  public static void copyFileToDir(File file, File dir) throws Ex_FileAccessException {
    File outFile = new File(dir, file.getName());
    FileUtils.copyFile(file, outFile);
  }
  
  public static String getSystemTempDir() {
    String dir = System.getProperty("java.io.tmpdir");
    if (!dir.endsWith(File.separator)) {
      dir += File.separator;
    }
    return dir;
  }
  
  
  /**
   * Generiert einen Filenamen mit Zufallszahl im Format &lt;baseDir&gt;/&lt;prefix&gt;random&lt;suffix&gt;
   * @param baseDir
   * @param prefix
   * @param suffix
   * @return
   */
  public static String generateRandomFilename(String baseDir, String prefix, String suffix) {
    StringBuilder nameBuilder = new StringBuilder();
    if (baseDir != null && !baseDir.isEmpty()) {
      nameBuilder.append(baseDir)
                 .append(Constants.fileSeparator);
    }
    if (prefix != null) {
      nameBuilder.append(prefix);
    }
    nameBuilder.append(Math.abs(random.nextLong() / 2));
    if (suffix != null) {
      nameBuilder.append(suffix);
    }
    return nameBuilder.toString();
  }
  
  
  /**
   * Generiert einen eindeutigen Filenamen im Format &lt;baseDir&gt;/&lt;prefix&gt;_cnt&lt;suffix&gt;,
   * indem solange cnt hochgezaehlt wird bis der Filename noch nicht existiert.
   * @param baseDir
   * @param prefix
   * @param suffix
   * @return
   */
  public static File generateUniqueFileIncrementally(String baseDir, String prefix, String suffix) {
    String pref = prefix != null ? prefix : "";
    String suff = suffix != null ? suffix : "";
    String base = (baseDir != null && !baseDir.isEmpty()) ? baseDir + Constants.fileSeparator : "";
    File f = new File(base + pref + suff);
    int cnt = 0;
    while (f.exists()) {
      f = new File(base + pref + "_" + cnt++ + suff);
    }
    return f;
  }

  /**
   * zips the files into new zipfile. files may be directories that will be included entirely (recursively)
   */
  public static void createZip(List<File> files, File zipFile) throws Ex_FileAccessException {
    ConcurrentHashMap<String, File> addedFiles = new ConcurrentHashMap<>();
    try {
      if (!zipFile.exists()) {
        if (zipFile.getParentFile() != null) {
          zipFile.getParentFile().mkdirs();
        }
        zipFile.createNewFile();
      }
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
      try {
        for (File f : files) {
          ZipEntryVisitor zev = ze -> {
            if (ze.isDirectory()) {
              return;
            }
            if (null != addedFiles.putIfAbsent(ze.getName(), f)) {
              throw new RuntimeException("Duplicate file name <" + ze.getName() + "> found in " + f.getAbsolutePath() + " and "
                  + addedFiles.get(ze.getName()).getAbsolutePath());
            }
          };
          if (f.isDirectory()) {
            zipDir(f, zos, f.getParentFile(), (filename, s) -> true, null, null, zev);
          } else {
            addToZip(zos, f, f.getParentFile(), (filename, s) -> true, null, null, zev);
          }
        }
      } finally {
        zos.flush();
        zos.close();
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(zipFile.getName(), e);
    }
  }
  
  public static String deriveFqNameFromPath(File rootPath, Path fullPath) {
    return fullPath.toFile().getAbsolutePath().substring(rootPath.getAbsolutePath().length() + 1, fullPath.toFile().getAbsolutePath().length() - 4).replace(System.getProperty("file.separator"), ".");
  }
}
