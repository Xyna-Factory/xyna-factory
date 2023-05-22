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
package com.gip.xyna.utils.exceptions;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.utils.FileUtils;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaClass;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntryProvider;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry_1_0;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionEntry_1_1;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance_1_0;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageInstance_1_1;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParser;
import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParserFactory;
import com.gip.xyna.utils.exceptions.xmlstorage.InvalidValuesInXMLException;
import com.gip.xyna.utils.exceptions.xmlstorage.XSDNotFoundException;



/**
 * laed Fehlermeldungen aus XML File(s) und registriert sie beim ExceptionHandler.
 */
public class ExceptionStorage {

  private ExceptionStorage() {
  }


  private static Logger logger = Logger.getLogger("xyna.utils.exceptions");

  private static boolean languageSet = false; //merkt sich, wenn durch das xml parsing die sprache bereits gesetzt worden ist, damit es nicht mehrfach geschieht.

  /**
   * Name of the system property over which the file ExceptionStorage File can be reached
   */
  public static final String EXCEPTIONSTORAGE_FILE_SYSTEM_PROPERTY = "exceptions.storage";


  public static void init() {
    //lade exceptions im eigenen jar-file
    try {
      logger.debug("loading internal Exceptions.xml");
      String fileName = "Exceptions.xml";
      InputStream is = FileUtils.getInputStreamToResource(fileName);
      loadFromStream(is, fileName);
    } catch (Exception e) {
      logger.error("Error loading internal Exception.xml from jar file", e);
      throw new RuntimeException(e);
    }
    languageSet = false; //nicht die spracheinstellung im file im jarfile benutzen

    try {
      String file = System.getProperty(EXCEPTIONSTORAGE_FILE_SYSTEM_PROPERTY);
      if (file == null || file.length() == 0) {
        logger.warn("Couldn't load error messages. No ExceptionStorage file specified (java start parameter \""
            + EXCEPTIONSTORAGE_FILE_SYSTEM_PROPERTY + "\").");
        return;
      }
      loadFromFile(file, 0);
    } catch (Exception e) {
      logger
          .error("Error while loading File \"" + System.getProperty(EXCEPTIONSTORAGE_FILE_SYSTEM_PROPERTY) + "\".", e);
    }
  }


  /**
   * laed exceptions aus file fileName. falls importDepth = 0, wird der Wert des Attributs DefaultLanguage als default
   * Sprache fuer den ExceptionHandler gesetzt. Ist das Attribut nicht gesetzt, verbleibt die default Sprache so wie sie
   * ist.<br>
   * Imports werden durchgefuehrt, bevor die Messages der aktuellen Datei verarbeitet werden
   * @param fileName
   * @param importDepth
   * @throws InvalidXMLException
   * @throws FileNotFoundException
   * @throws DuplicateExceptionCodeException
   * @throws XSDNotFoundException
   */
  protected static void loadFromFile(String fileName, int importDepth) throws FileNotFoundException,
      InvalidXMLException, XSDNotFoundException, DuplicateExceptionCodeException {
    Document doc = XMLUtils.getDocumentFromFile(fileName);
    parseAndRegisterMessages(doc);
  }


  /**
   * achtung: methode wird von altem generiertem code aus aufgerufen
   * @see ExceptionStorageInstance_1_0#generateJavaClasses(boolean, ExceptionEntryProvider)
   * @param is
   * @throws InvalidXMLException
   * @throws DuplicateExceptionCodeException
   * @throws XSDNotFoundException
   */
  public static void loadFromStream(InputStream is) throws InvalidXMLException, XSDNotFoundException,
      DuplicateExceptionCodeException {
    Document doc = XMLUtils.getDocumentFromStream(is, "unknown");
    parseAndRegisterMessages(doc);
  }


  /**
   * diese methode wird von neuem generiertem code aus aufgerufen
   * @param is
   * @param fileName
   * @throws InvalidXMLException
   * @throws DuplicateExceptionCodeException
   * @throws XSDNotFoundException
   * @throws Exception
   */
  public static void loadFromStream(InputStream is, String fileName) throws InvalidXMLException, XSDNotFoundException,
      DuplicateExceptionCodeException {
    Document doc = XMLUtils.getDocumentFromStream(is, fileName);
    parseAndRegisterMessages(doc);
  }


  private static void parseAndRegisterMessages(Document doc) throws InvalidXMLException, XSDNotFoundException,
      DuplicateExceptionCodeException {
    ExceptionStorageParser parser = ExceptionStorageParserFactory.getParser(doc);
    ExceptionStorageInstance esi = parser.parse(true, 0);
    registerMessages(esi);
  }


  private static ExceptionStorageInstance parse(String xmlFile) throws InvalidXMLException, XSDNotFoundException,
      FileNotFoundException {
    ExceptionStorageParser parser = ExceptionStorageParserFactory.getParser(xmlFile);
    ExceptionStorageInstance esi = parser.parse(true, 0);
    return esi;
  }


  private static void registerMessages(ExceptionStorageInstance esi) throws DuplicateExceptionCodeException {
    if (!languageSet && esi.getDefaultLanguage() != null && esi.getDefaultLanguage().length() > 0) {
      synchronized (ExceptionStorage.class) {
        if (!languageSet) {
          ExceptionHandler.setLanguage(esi.getDefaultLanguage());
          languageSet = true;
        }
      }
    }
    esi.mergeWithIncludes();
    for (ExceptionEntry entry : esi.getEntries()) {
      String code = entry.getCode();
      Iterator<String> languageIterator = entry.getMessages().keySet().iterator();
      while (languageIterator.hasNext()) {
        String lang = languageIterator.next();
        String message = entry.getMessages().get(lang);
        ExceptionHandler.cacheErrorMessage(true, code, message, lang);
      }
    }
  }


  private static void generateJavaClasses(final ExceptionStorageInstance rootEsi, ExceptionStorageInstance currentEsi,
                                          String srcDir, boolean loadFromResource, String xmlFile)
      throws InvalidValuesInXMLException, IOException {
    JavaClass[] jcs = currentEsi.generateJavaClasses(loadFromResource, new ExceptionEntryProvider() {

      public ExceptionEntry get(String path, String name) {
        return search(path, name, rootEsi);
      }


      private ExceptionEntry search(String path, String name, ExceptionStorageInstance esi) {
        for (ExceptionEntry entry : esi.getEntries()) {
          if (entry instanceof ExceptionEntry_1_1) {
            ExceptionEntry_1_1 e11 = (ExceptionEntry_1_1) entry;
            if (e11.getName().equals(name) && e11.getPath().equals(path)) {
              return e11;
            }
          } else if (entry instanceof ExceptionEntry_1_0) {

          }
        }
        if (esi instanceof ExceptionStorageInstance_1_1) {
          for (ExceptionStorageInstance importedEsi : ((ExceptionStorageInstance_1_1) esi).getImports()) {
            ExceptionEntry ee = search(path, name, importedEsi);
            if (ee != null) {
              return ee;
            }
          }
        }
        for (ExceptionStorageInstance includedEsi : esi.getIncludes()) {
          ExceptionEntry ee = search(path, name, includedEsi);
          if (ee != null) {
            return ee;
          }
        }
        return null;
      }

    }, xmlFile);
    for (JavaClass jc : jcs) {
      String fileName = createFileNameFromFQClassName(jc.getFQClassName(), srcDir);
      FileUtils.writeToFile(jc.getSourceCode("Utils"), fileName);
    }
    for (ExceptionStorageInstance importedEsi : currentEsi.getIncludes()) {
      generateJavaClasses(rootEsi, importedEsi, srcDir, loadFromResource, importedEsi.getXmlFile());
    }
  }


  private static String createFileNameFromFQClassName(String fqClassName, String srcDir) {
    return srcDir + (srcDir.endsWith(File.separator) ? "" : File.separator)
        + fqClassName.replaceAll("\\.", Matcher.quoteReplacement(File.separator)) + ".java";
  }


  private static class CacheKey {

    private int clhash;
    private String clString;
    private String fileName;


    public int hashCode() {
      return clhash + clString.hashCode() + fileName.hashCode();
    }


    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (!(o instanceof CacheKey)) {
        return false;
      }
      CacheKey ck = (CacheKey) o;
      if (ck.clhash == clhash && ck.clString.equals(clString) && ck.fileName.equals(fileName)) {
        return true;
      }
      return false;
    }
  }


  private static Map<CacheKey, Long> cache = new ConcurrentHashMap<CacheKey, Long>();
  private static AtomicLong lastCacheCleanup = new AtomicLong(0);


  /**
   * xml aus resource lesen. zb wenn das xml in einem jar file liegt.
   */
  public static void loadFromResource(String fileName, ClassLoader cl) {
    //caching, damit man das gleiche xml nicht andauernd neu parsen muss.
    CacheKey key = new CacheKey();
    key.clhash = cl.hashCode(); //nicht den classloader selbst als key nehmen, weil darauf referenzen zu haben zu outofmemory f�hren kann oder so zeug 
    key.fileName = fileName;
    key.clString = cl.toString();
    Long l = cache.get(key);
    long currentTime = System.currentTimeMillis();
    if (l != null) {
      //der zeitliche aspekt ist nur wegen hash collisionen. innerhalb von 20 sekunden wird davon ausgegangen, dass gleiche hashes sehr unwahrscheinlich gleiche
      //classloader sind (insbesondere, wenn toString auch noch gleich ist).
      if (currentTime < l + 20000) {
        if (logger.isDebugEnabled()) {
          logger.debug("do not load resource " + fileName + " with classloader " + cl
              + ", because it has been loaded before.");
        }
        return; //falls bereits geladen (im cache vorhanden) und zwar weniger als 20 sekunden in der vergangenheit
      }
    }

    //alte eintr�ge aus cache werfen
    if (currentTime - lastCacheCleanup.get() > 60 * 1000 * 10) { //maximal einmal alle 10 minuten (nicht threadsicher, aber dann passiert es halt etwas �fters)
      lastCacheCleanup.set(currentTime);
      cache.clear(); //das mag etwas zuviel sein, aber macht nix.
    }

    if (logger.isDebugEnabled()) {
      logger.debug("trying to load exceptionstorage xml from resource " + fileName + " with classloader " + cl);
    }
    try {
      Enumeration<URL> urls = cl.getResources(fileName);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        if (url != null) {
          URLConnection urlcon = (URLConnection) url.openConnection();
          //deactivate cache to not get an old version
          boolean b = urlcon.getUseCaches();
          urlcon.setUseCaches(false);
          try {
            InputStream is = urlcon.getInputStream();
            try {
              if (is == null) {
                throw new Exception("Resource " + fileName + " not found.");
              }
              loadFromStream(is, fileName);
            } finally {
              is.close();
            }
          } finally {
            //reset caching!
            try {
              urlcon.setUseCaches(b);
            } catch (IllegalStateException e) {
              //ntbd - dann halt nicht
            }
          }
        } else {
          throw new Exception(" Resource " + fileName + " not found.");
        }
      }
    } catch (Exception e) {
      logger.warn("Error loading Errormessages.", e);
      e.printStackTrace();
    } finally {
      cache.put(key, System.currentTimeMillis());
    }
  }


  /**
   * Beispiel: java -classpath classes:lib/log4j-1.2.15.jar com.gip.xyna.utils.exceptions.ExceptionStorage
   * ExampleExceptionStorage.xml
   * @param args
   */
  public static void main(String[] args) {
    //  if (0==0) return;
    //   args = new String[]{"test/Reference.1.1.xml", "test", "y"};
    //  args = new String[]{"ExampleExceptionStorage.1.1.xml", "test", "y"};
    //   args = new String[]{"src/Exceptions.xml", "src", "y"};
    try {
      if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
        System.out.println("Parameter: xmlFile [gendir [loadFromResource (y/n)]]");
        System.out.println("xmlFile ist der Pfad zum Exception.xml, gendir das Verzeichnis, in dem "
            + "die Java Klassen generiert werden. loadFromResource ist y oder n (default n) "
            + "und steuert, ob die generierte Klasse eine statische Initialisierung bekommt, in der "
            + "das xml file als resource geladen wird.");
        return;
      }
      String xmlFile = args[0];
      System.out.println("setting xmlFile to " + xmlFile);
      String gendir = ".";
      if (args.length > 1) {
        gendir = args[1];
        System.out.println("setting gendir to " + args[1]);
      }
      boolean loadFromResource = false;
      if (args.length > 2) {
        if (args[2].equalsIgnoreCase("y")) {
          loadFromResource = true;
        }
      }
      ExceptionStorageInstance esi = parse(xmlFile);
      generateJavaClasses(esi, esi, gendir, loadFromResource, xmlFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
