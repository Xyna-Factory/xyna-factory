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
package com.gip.xyna.utils.exceptions;



import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;



/**
 * Klasse für das generische Exceptionhandling
 * <p>
 * XynaFault hat im Feld "code" einen Xyna internen Fehlercode der Form "XYNA-12345", wobei die ersten 2 Ziffern
 * Xyna-Department spezifisch sein sollten.<br>
 * Im Feld "summary" befindet sich die Fehlernachricht.<br>
 * Im Feld "details" befindet sich die Fehlernachricht und der Stacktrace (inkl der Causes falls vorhanden).
 * <p>
 * Entwickler: Im Code sollten Fehler geworfen werden, indem nur ihr Fehlercode angegeben wird (XynaException), sofern
 * es sich um eine statische Fehlermeldung handelt. Wenn die Fehler Parameter enthalten, werden die Parameter als
 * String-Array separat mitübergeben:<br>
 * <code>throw new
 * XynaException("XYNA-12345", "parameter1").initCause(e);</code><br>
 * Oder noch besser:<br>
 * <code>
 * private static final String[] getMyCode(String beschreibungDesParameters) {<br>
 *   return new String[]{"XYNA-12345", beschreibungDesParameters};<br>
 * }<br>
 * throw new XynaException(getMyCode("parameter1")).initCause(e);
 * </code> Bevor Fehler serialisiert/angezeigt werden sollen, muss dann ein
 * <code>ExceptionHandler.handleException()</code> aufgerufen werden, welches je nach Einstellung die Fehlermeldung
 * generiert. Alternativ geht auch {@link #toXynaFault(Exception)} oder {@link XynaException#toXynaFault()}.
 * <p>
 * Features: Unterstützt unterschiedliche Sprachen, und eigene Filter zur Umwandlung von bestimmten Exceptions in
 * XynaFaults.
 * <p>
 * Anfangs wird versucht mittels {@link ExceptionStorage} FehlerCodes und Nachrichten aus zu initialisieren (aus XML
 * File oder ähnlichem, abhängig von der Implementierung)
 */
public class ExceptionHandler {

  private static final Logger logger = Logger.getLogger("xyna.utils.exceptions");

  /**
   * Fehlernachrichten können Parameter in der Form %zahl% haben, wobei die Zahl angibt, der wievielte Parameter hier
   * eingesetzt wird. Es wird mit 0 angefangen zu zählen.
   * 
   * @see #getErrorParameterLocator(int)
   */
  private static final String ERROR_PARAMETER_LOCATOR = "%";

  private static final String ERRORCODE_UNKNOWN = "XYNA-99999";
  private static final String ERRORMSG_UNKNOWN_DE = "Dieser Fehler hat keine Fehlernachricht erzeugt";
  private static final String ERRORMSG_UNKNOWN_EN = "Error was not caused by XynaException.";

  /**
   * Hier steht drin: Welche Fehlermeldung gehört zu welchem Code. Für Mehrsprachlichkeit kann es für verschiedene
   * Sprachen Fehlermeldungen geben. Die HashMap ordnet dazu intern jedem Code eine Hashmap zu, die wiederum jeder
   * Sprache eine Fehlermeldung zuordnet: errorMessagesCache = new HashMap<String, HashMap<String, String>>();
   */
  private static ConcurrentMap<Long, ConcurrentMap<String, ConcurrentMap<String, String>>> errorMessagesCache = null;
  public static final String LANG_DE = "DE";
  public static final String LANG_EN = "EN";
  private static final String LANG_ALL = "ALL"; // nur für internen Gebrauch, damit bei auch bei
  // falsch eingestellter sprache sicher eine fehlermeldung zurückkommt.

  private static final long DEFAULT_REVISION = -1L;
  
  /**
   * Standardsprache Deutsch.
   */
  private static volatile String language = LANG_DE;
  private static volatile boolean showForeignLanguageErrorIfNoneOtherAvailable = true;
  private static final List<ExceptionFilter> filter = new Vector<ExceptionFilter>();
  private static volatile boolean initialized = false;
  
  /**
   * nicht instanziieren
   */
  private ExceptionHandler() {
  }


  private static ConcurrentMap<String, ConcurrentMap<String, String>> getOrCreateErrorMessagesCache(Long revision) {
    ConcurrentMap<String, ConcurrentMap<String, String>> result = errorMessagesCache.get(revision);
    if(result == null) {
      synchronized (errorMessagesCache) {
        result = errorMessagesCache.get(revision);
        if(result == null) {
          result = new ConcurrentHashMap<String, ConcurrentMap<String, String>>();
          errorMessagesCache.put(revision, result);
        }
      }
    }
    return result;
  }
  
  protected static synchronized int initErrorMessageCache() {
    if (initialized) {
      return 0;
    }
    initialized = true;
    // Interne Fehlermeldungen initialisieren
    errorMessagesCache = new ConcurrentHashMap<Long, ConcurrentMap<String, ConcurrentMap<String, String>>>();
    ConcurrentMap<String, String> m = new ConcurrentHashMap<String, String>();
    m.put(LANG_DE, ERRORMSG_UNKNOWN_DE);
    m.put(LANG_EN, ERRORMSG_UNKNOWN_EN);
    getOrCreateErrorMessagesCache(DEFAULT_REVISION).put(ERRORCODE_UNKNOWN, m);
    // FehlerCodes initialisieren
    ExceptionStorage.init();
    return 0;
  }


  /**
   * fügt einen eigenen ExceptionFilter zum Fehlerhandling hinzu. ExceptionFilter-Klassen müssen die Methode
   * filterException() implementieren.
   * 
   * @see ExceptionFilter
   * @param f
   */
  public static void addFilter(ExceptionFilter f) {
    filter.add(f);
  }


  /**
   * legt fest, in welcher Sprache Fehler standardmässig ausgegeben werden
   * 
   * @see #LANG_DE
   * @see #LANG_EN
   * @param lang
   */
  public static void setLanguage(String lang) {
    language = lang;
  }


  /**
   * Sollen Fehlermeldungen auch in einer nicht eingestellten Sprache angezeigt werden, falls in der eingestellten keine
   * gefunden wird?<br>
   * Default = true
   * 
   * @param b
   */
  public static void setShowForeignLanguageErrorIfNoneOtherAvailable(boolean b) {
    showForeignLanguageErrorIfNoneOtherAvailable = b;
  }
  
  public static void deregisterErrorCode(String code) {
    deregisterErrorCode(code, DEFAULT_REVISION);
  }
  
  public static void deregisterErrorCode(String code, Long revision) {
    initErrorMessageCache();
    getOrCreateErrorMessagesCache(revision).remove(code);
  }


  /**
   * cacht die übergebene Fehlernachricht zugeordnet zum Fehlercode für die aktuell eingestellte Sprache
   * 
   * @param code
   * @param message
   * @throws DuplicateExceptionCodeException 
   */
  public static void cacheErrorMessage(String code, String message) throws DuplicateExceptionCodeException {
    cacheErrorMessage(code, message, language);
  }


  /**
   * cacht die übergebene Fehlernachricht zugeordnet zum Fehlercode für die übergebene Sprache
   * 
   * @param code
   * @param message
   * @param lang Am besten die Konstanten LANG_DE, LANG_EN etc benutzen
   * @throws DuplicateExceptionCodeException 
   */
  public static void cacheErrorMessage(String code, String message, String lang) throws DuplicateExceptionCodeException {
    cacheErrorMessage(false, code, message, lang);
  }


  /**
   * @param forceOverwrite default = false
   * @param code
   * @param message
   * @throws DuplicateExceptionCodeException 
   */
  public static void cacheErrorMessage(boolean forceOverwrite, String code, String message) throws DuplicateExceptionCodeException {
    cacheErrorMessage(forceOverwrite, code, message, language);
  }


  public static synchronized void cacheErrorMessage(boolean forceOverwrite, String code, String message, String lang)
                  throws DuplicateExceptionCodeException {
    
    cacheErrorMessage(forceOverwrite, code, message, lang, DEFAULT_REVISION);
  }


  public static synchronized void cacheErrorMessage(boolean forceOverwrite, String code, String message, String lang,
                                                    Long revision) throws DuplicateExceptionCodeException {
    initErrorMessageCache();
    if (logger.isDebugEnabled()) {
      logger.debug("trying to cache: " + code + " (" + lang + ") " + message);
    }

    ConcurrentMap<String, String> langMap = getOrCreateErrorMessagesCache(revision).get(code);
    if (langMap != null) {
      String previousMessage = langMap.putIfAbsent(lang, message);
      if (previousMessage == null) {
        return;
      }
      if (previousMessage.equals(message)) {
        return; // gleiche message
      } else if (forceOverwrite) {
        langMap.put(lang, message);
      } else {
        // keine zwei fehlercodes mit unterschiedlicher message.
        throw new DuplicateExceptionCodeException(code, lang);
      }
    } else {
      langMap = new ConcurrentHashMap<String, String>();
      langMap.put(lang, message);
      getOrCreateErrorMessagesCache(revision).putIfAbsent(code, langMap);
    }
  }


  /**
   * Deletes all cached error messages.
   */
  public static synchronized void deleteErrorMessageCache() {
    initialized = true;
    errorMessagesCache = new ConcurrentHashMap<Long, ConcurrentMap<String, ConcurrentMap<String, String>>>();
  }


  /**
   * funktioniert zwar, aber ist nicht besonder sicher (die HashMap muss die korrekte Struktur haben!)
   * 
   * @param cache
   */
  public static synchronized void setErrorMessageCache(ConcurrentMap<String, ConcurrentMap<String, String>> cache) {
    initialized = true;
    errorMessagesCache.put(DEFAULT_REVISION, cache);
  }


  public static String getStackTraceAsString(Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }


  protected static String getErrorMessage(String code, String[] args) throws UnknownExceptionCodeException {
    return getErrorMessage(code, args, language, DEFAULT_REVISION);
  }
  
  protected static String getErrorMessage(String code, String[] args, Long revision) throws UnknownExceptionCodeException {
    return getErrorMessage(code, args, language, revision);
  }
  
  private static String tryGetMessageForLangs(Map<String, String> langMap, String[] langs, int pos) {
    if (pos >= langs.length) {
      return null;
    }
    String msg = langMap.get(langs[pos]);
    if (msg != null) {
      return msg;
    } else {
      return tryGetMessageForLangs(langMap, langs, pos + 1);
    }
  }

  private static String getErrorMessageFromCache(String code, String lang, String[] args, Long revision) throws UnknownExceptionCodeException {
    if (code == null) {
      return "Exception code was specified as null.";
    }
    initErrorMessageCache();
    ConcurrentMap<String, ConcurrentMap<String, String>> cache = getOrCreateErrorMessagesCache(revision);
    Map<String, String> langMap = cache.get(code);
    if (langMap != null) {
      // code exists
      String msg = langMap.get(lang);
      if (msg != null) {
        return msg;
      } else {
        msg = langMap.get(LANG_ALL);
        if (msg != null) {
          return msg;
        } else if (showForeignLanguageErrorIfNoneOtherAvailable) {
          logger.warn("Language \"" + lang + "\" was not registered with ExceptionHandler for code \"" + code + "\".");
          msg = tryGetMessageForLangs(langMap, new String[]{language, LANG_EN, LANG_DE}, 0);
          if (msg != null) {
            return msg;
          } else {
            // dann einfach die erstbeste Sprache nehmen...
            Iterator<String> it = langMap.keySet().iterator();
            if (it.hasNext()) {
              return langMap.get(it.next());
            }
          }
        }
      }        
    }
    if (logger.isTraceEnabled()) {
      logger.trace("-----------registered codes with messages:");
      Iterator<String> codeIt = cache.keySet().iterator();
      while (codeIt.hasNext()) {
        String c = codeIt.next();
        Iterator<String> langIt = cache.get(c).keySet().iterator();
        while (langIt.hasNext()) {
          String s = langIt.next();
          String msg = cache.get(c).get(s);
          logger.trace(c + " (" + s + ") " + msg);
        }
      }
    }
    throw new UnknownExceptionCodeException(code, args == null || args.length == 0 ? "" : getParasAsString(args));
  }


  private static String getParasAsString(String[] args) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(args[i]);
    }
    return sb.toString();
  }

  protected static String getErrorMessage(String code, String[] args, String lang) throws UnknownExceptionCodeException  {
    return getErrorMessage(code, args, lang, DEFAULT_REVISION);
  }
  

  protected static String getErrorMessage(String code, String[] args, String lang, Long revision)
      throws UnknownExceptionCodeException {
    String msg = getErrorMessageFromCache(code, lang, args, revision);
    StringBuffer sb = new StringBuffer();
    Pattern p = Pattern.compile(ERROR_PARAMETER_LOCATOR + "(\\d+)" + ERROR_PARAMETER_LOCATOR);
    Matcher m = p.matcher(msg);

    while (m.find()) {
      try {
        int i = Integer.valueOf(m.group(1));
        if (args == null || i >= args.length) {
          continue;
        }
        String s = args[i];
        if (s == null) {
          s = "";
        }
        m.appendReplacement(sb, Matcher.quoteReplacement(s));
      } catch (NumberFormatException e) {
        //ignore
      }
    }
    m.appendTail(sb);

    return sb.toString();
  }


  /**
   * baut den Teil String einer Fehlermeldung, der durch den i-ten Parameter ersetzt werden soll.
   * 
   * @param i
   * @return
   */
  protected static String getErrorParameterLocator(int i) {
    return ERROR_PARAMETER_LOCATOR + i + ERROR_PARAMETER_LOCATOR;
  }


  /**
   * RemoteException wird so gelassen und geworfen.<br>
   * XynaErrors werden in reine XynaFaults konvertiert (damit geht ihre Dynamic verloren), also die Fehlermeldung zum
   * Fehlercode in der aktuell eingestellten Sprache aus dem Cache erzeugt.<br>
   * XynaFaults bekommen ggfs den Stacktrace in die Details geschrieben.<br>
   * alle anderen Fehler werden in XynaFaults konvertiert.
   * 
   * @param e
   * @throws XynaFault_ctype
   * @throws RemoteException
   */
  public static void handleException(Throwable e) throws XynaFault_ctype, RemoteException {
    handleException(e, language);
  }


  /**
   * man kann zusätzlich die Sprache angeben, in der die Fehlermeldung erzeugt wird.
   * 
   * @see #handleException(Exception)
   * @param e
   * @param lang
   * @throws XynaFault_ctype
   * @throws RemoteException
   */
  public static void handleException(Throwable e, String lang) throws XynaFault_ctype, RemoteException {
    if (e instanceof RemoteException) {
      throw (RemoteException) e;
    }
    XynaFault_ctype xf = toXynaFault(e, lang);
    throw xf;
  }


  /**
   * wie HandleException(), wirft aber keine RemoteException. Diese werden auch in XynaFaults umgewandelt.
   * 
   * @see #handleException(Exception)
   * @param e
   * @throws XynaFault_ctype
   */
  public static void handleExceptionNoRE(Throwable e) throws XynaFault_ctype {
    throw toXynaFault(e, language);
  }


  /**
   * Creates a XynaFault from a given XynaException.
   * 
   * @param xe XynaException to transform
   * @param lang Message language
   * @return a XynaFault
   * @throws UnknownExceptionCodeException 
   * @throws RemoteException
   * @throws XynaFault_ctype
   */
  private static XynaFault_ctype createXynaFault(XynaException xe, String lang, Long revision) throws UnknownExceptionCodeException {
    XynaFault_ctype xf = new XynaFault_ctype();
    xf.setCode(xe.getCode());
    xf.setSummary(getErrorMessage(xe.getCode(), xe.getArgs(), lang, revision));
    xf.setDetails(buildDetails(xe));
    return xf;
  }


  /**
   * If needed adds the StrackTrace to the faults details.
   * 
   * @param xf
   */
  private static void updateXynaFault(XynaFault_ctype xf) {
    if (xf.getDetails() == null || xf.getDetails().length() == 0) {
      xf.setDetails(buildDetails(xf));
    }
  }


  /**
   * Checks if a filter matches to the given exception.
   * 
   * @param e
   * @throws XynaFault_ctype
   */
  private static XynaFault_ctype processFilters(Throwable e) {
    for (ExceptionFilter f : filter) {
      e = f.filterException(e);
      if (e instanceof XynaFault_ctype) {
        return (XynaFault_ctype) e;
      }
    }
    return null;
  }


  private static String buildDetails(Throwable t) {
    return getStackTraceAsString(t);
  }


  /**
   * Creates a default XynaFault. For use within a web service. Sets Code to UNKNOWN = "XYNA-99999". Sets Summary to
   * message of exception. Sets Details to stacktrace of exception and its causes if there are any.
   * 
   * @param t
   * @return
   */
  private static XynaFault_ctype createDefaultXynaFault(Throwable t) {
    XynaFault_ctype xf = new XynaFault_ctype();
    xf.setCode(ERRORCODE_UNKNOWN);
    xf.setSummary(t.getMessage() == null ? t.getClass().getName() : t.getMessage()); // koennte null sein
    xf.setStackTrace(t.getStackTrace()); // wird nicht serialisiert,
    // falls man XynaFault_ctype in
    // einem Webservice benutzt.
    xf.setDetails(buildDetails(t));
    xf.initCause(t.getCause());
    return xf;
  }

  public static XynaFault_ctype toXynaFault(Throwable e, String lang) {
    return toXynaFault(e, lang, DEFAULT_REVISION);
  }
  
  
  public static XynaFault_ctype toXynaFault(Throwable e, String lang, Long revision) {
    if (e instanceof XynaException) {
      try {
        return createXynaFault((XynaException) e, lang, revision);
      } catch (UnknownExceptionCodeException xe) {
        // code nicht gefunden
        xe.initCause(e);
        try {
          return createXynaFault(xe, lang, revision);
        } catch (UnknownExceptionCodeException xeInt) {
          logger.error("Interner Fehler", xeInt);
          // nun gibts ein default xynafault ohne fehlermeldung
        }
      }
    }
    if (e instanceof XynaFault_ctype) {
      XynaFault_ctype xf = (XynaFault_ctype) e;
      updateXynaFault(xf);
      return xf;
    }
    XynaFault_ctype xf = processFilters(e);
    if (xf != null) {
      return xf;
    }
    // ansonsten standardbehandlung:
    return createDefaultXynaFault(e);
  }


  public static XynaFault_ctype toXynaFault(Exception e) {
    return toXynaFault(e, language);
  }
  
  public static XynaFault_ctype toXynaFault(Exception e, Long revision) {
    return toXynaFault(e, language, revision);
  }


}
