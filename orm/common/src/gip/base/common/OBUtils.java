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
package gip.base.common;

import java.util.Properties;

import org.apache.log4j.Logger;

public class OBUtils {
  
  private transient static Logger logger = Logger.getLogger(OBUtils.class);

  public static final String inputToShort2() { return "inputToShort2"; } //$NON-NLS-1$
  public static final String inputToLong2() { return "inputToLong2"; } //$NON-NLS-1$
  public static final String inputToSmall2() { return "inputToSmall2"; } //$NON-NLS-1$
  public static final String inputToLarge2() { return "inputToLarge2"; } //$NON-NLS-1$
  public static final String inputWrongChar2() { return "inputWrongChar2"; } //$NON-NLS-1$
  public static final String inputWrongString2() { return "inputWrongString2"; } //$NON-NLS-1$
  public static final String inputWrongRegexp2() { return "inputWrongRegexp2"; } //$NON-NLS-1$
  public static final String inputNotNullable1() { return "inputNotNullable1"; } //$NON-NLS-1$
  public static final String inputWrongStartChar2() { return "inputWrongStartChar2"; } //$NON-NLS-1$
  public static final String inputWrongNotRegexp2() { return "inputWrongNotRegexp2"; } //$NON-NLS-1$
  public static final String inputNoNeededChars3() { return "inputNoNeededChars3"; } //$NON-NLS-1$
  public static final String inputToLong3() { return "inputToLong3"; } //$NON-NLS-1$

   static {
    OBException.addErrorMessage(inputToShort2(),"Der Inhalt des Feldes {0} muss mindestens {1} Zeichen lang sein."); //$NON-NLS-1$
    OBException.addErrorMessage(inputToLong2(),"Der Inhalt des Feldes {0} darf maximal {1} Zeichen lang sein."); //$NON-NLS-1$
    OBException.addErrorMessage(inputToSmall2(),"Der Inhalt des Feldes {0} darf den Wert {1} nicht unterschreiten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputToLarge2(),"Der Inhalt des Feldes {0} darf den Wert {1} nicht überschreiten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongChar2(),"Der Inhalt des Feldes {0} darf das Zeichen {1} nicht enthalten. Bitte verwenden Sie nur Zeichen aus der Menge {2}."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongString2(),"Der Inhalt des Feldes {0} darf die Zeichenfolge {1} nicht enthalten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongRegexp2(),"Der Inhalt des Feldes {0} entspricht nicht dem Ausdruck {1}."); //$NON-NLS-1$
    OBException.addErrorMessage(inputNotNullable1(),"Das Feld {0} darf nicht leer sein."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongStartChar2(),"Der Inhalt des Feldes {0} darf nicht mit dem Zeichen {1} beginnen."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongNotRegexp2(),"Der Inhalt des Feldes {0} darf nicht dem Ausdruck {1} entsprechen."); //$NON-NLS-1$
    OBException.addErrorMessage(inputNoNeededChars3(),"Der Inhalt des Feldes {0} muss mindestens {1} Zeichen aus der Menge {2} enthalten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputToLong3(),"Der Inhalt des Feldes {0} darf maximal {1} Zeilen enthalten."); //$NON-NLS-1$
  }

   /**
   * Sucht den Wert eines Properties mit Variablen-Aufloesung $(..)
   * @param fprops Properties
   * @param searchString Suchstring
   * @param defVal Default
   * @return Wert des Props
   */
  public static String getProp(Properties fprops,String searchString, String defVal) {
    String prop = fprops.getProperty(searchString, null);
    if (prop==null) {
      String searchShort= shortenSearchString(searchString);
      if (searchShort!=null) {
        return getProp(fprops,searchShort,defVal);
      }
    }
    if (prop !=null && prop.startsWith("$(") && prop.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
      return getProp(fprops, prop.substring(2, prop.length()-1), defVal);
    }
    if (prop==null) {
      return defVal;
    }
    return prop.trim();
  }
  
  private static String shortenSearchString(String searchString) {
    int lastIndex = searchString.lastIndexOf('.');
    if (lastIndex>0) { // Hier mal wirklich > 0 !!!
      int lastButOneIndex = searchString.lastIndexOf('.', lastIndex-1);
      if (lastButOneIndex==-1) {
        return searchString.substring(lastIndex+1);
      }
      else {
        return searchString.substring(0, lastButOneIndex+1)+searchString.substring(lastIndex+1); 
      }
    }
    return null;
  }
  
  /** Check-Methode fuer ein GUI-/Import-Feld
   * 
   * @param checkContext Erster Teil des Identifieres im Property-File, z.B. user
   * @param attName Zweiter Teil des Identifiers im Property-File, z.B. firstName
   * @param attValue Zu checkender Wert
   * @param fprops Property-Datei, bereits eingelesen
   * @param markable zu markierendes Feld
   * @return Evtl geaenderter Wert
   * @throws OBException Wenn das Feld nicht den Vorgaben entspricht
   */
  public static String checkField(String checkContext,
                                  String attName,
                                  String attValue,
                                  Properties fprops, 
                                  String markable) throws OBException {
    String localContext = checkContext+"."+attName+"."; //$NON-NLS-1$ //$NON-NLS-2$
    //OBLog.log.finer("localContext: " + localContext);
    String retValue = attValue;
    if (retValue!=null) { // Bei null machen die Umwandlungen keinen Sinn!
      // Bugz 4024: trim, upperCase, lowerCase 
      String trim = getProp(fprops,localContext+"trim","true"); // default: trim durchfuehren //$NON-NLS-1$ //$NON-NLS-2$
      if (trim.equalsIgnoreCase("true")||trim.equalsIgnoreCase("yes")) { //$NON-NLS-1$ //$NON-NLS-2$
        retValue=retValue.trim();
      }
      String upper = getProp(fprops,localContext+"upper","false"); // default: nix tun //$NON-NLS-1$ //$NON-NLS-2$
      if (upper.equalsIgnoreCase("true")||upper.equalsIgnoreCase("yes")) { //$NON-NLS-1$ //$NON-NLS-2$
        retValue=retValue.toUpperCase();
      }
      String lower = getProp(fprops,localContext+"lower","false"); // default: nix tun //$NON-NLS-1$ //$NON-NLS-2$
      if (lower.equalsIgnoreCase("true")||lower.equalsIgnoreCase("yes")) { //$NON-NLS-1$ //$NON-NLS-2$
        retValue=retValue.toLowerCase();
      }
    }
    if (retValue !=null && retValue.length()>0) {
      // Properties checken:
      // minlength
      {
        String minLength = getProp(fprops,localContext+"min",null); //$NON-NLS-1$
        if (minLength!=null) {
          if (retValue.length()<Integer.parseInt(minLength)) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"min.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "minLenth error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputToShort2()), 
                                  new String[] {label,minLength},
                                  markable);
          }
        }
      }
  
      // maxlength
      {
        String maxLength = getProp(fprops,localContext+"max",null); //$NON-NLS-1$
        if (maxLength!=null) {
          if (retValue.length()>Integer.parseInt(maxLength)) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"max.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "maxLenth error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputToLong2()), 
                                  new String[] {label,maxLength},
                                  markable);
          }
        }
      }
  
      // allow
      {
        String allowedChars = getProp(fprops,localContext+"allow",null); //$NON-NLS-1$
        if (allowedChars!=null) {
          // Zeichenweiser Test
          for (int i = 0; i < retValue.length(); i++) {
            if (allowedChars.indexOf(retValue.charAt(i))<0) {
              String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
              allowedChars = getProp(fprops,localContext+"allowGUI",allowedChars); //$NON-NLS-1$
              String individualErrMsg = getProp(fprops,localContext+"allow.errMsg",null); //$NON-NLS-1$
              logger.debug(localContext + "allow error"); //$NON-NLS-1$
              throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongChar2()), 
                                    new String[] {label,String.valueOf(retValue.charAt(i)),allowedChars},
                                    markable);
            }
          }
        }
      }
  
      // startsWith
      {
        String startsWith = getProp(fprops,localContext+"startsWith",null); //$NON-NLS-1$
        if (startsWith!=null) {
          // Test fuer das erste Zeichen
          if (startsWith.indexOf(retValue.charAt(0))<0) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"startsWith.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "startsWith error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongStartChar2()), 
                                  new String[] {label,String.valueOf(retValue.charAt(0))},
                                  markable);
          }
        }
      }
  
      // notStartsWith
      {
        String notStartsWith = getProp(fprops,localContext+"notStartsWith",null); //$NON-NLS-1$
        if (notStartsWith!=null) {
          // Test fuer das erste Zeichen
          if (notStartsWith.indexOf(retValue.charAt(0))>=0) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"notStartsWith.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "notStartsWith error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongStartChar2()), 
                                  new String[] {label,String.valueOf(retValue.charAt(0))},
                                  markable);
          }
        }
      }
  
      // allowRegexp
      {
        String allowedRegexp = getProp(fprops,localContext+"allowRegexp",null); //$NON-NLS-1$
        if (allowedRegexp!=null) {
          if (!retValue.matches(allowedRegexp)) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String format = getProp(fprops,localContext+"format",allowedRegexp); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"allowRegexp.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "allowRegexp error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongRegexp2()), 
                                  new String[] {label,format},
                                  markable);
          }
        }
      }
      
      // notAllowRegexp
      {
        String notAllowedRegexp = getProp(fprops,localContext+"notAllowedRegexp",null); //$NON-NLS-1$
        if (notAllowedRegexp!=null) {
          if (retValue.matches(notAllowedRegexp)) {
            String label = getProp(fprops,localContext+"label",retValue); //$NON-NLS-1$
            String format = getProp(fprops,localContext+"notFormat",notAllowedRegexp); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"notAllowedRegexp.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "notAllowRegexp error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongNotRegexp2()), 
                                  new String[] {label,format},
                                  markable);
          }
        }
      }
      
      // notallowedstring + notallowedstringsep
      {
        String notAllowedString = getProp(fprops,localContext+"notallowedstring",null); //$NON-NLS-1$
        if (notAllowedString!=null) {
          String notAllowedStringSep = getProp(fprops,localContext+"notallowedstringsep",null); //$NON-NLS-1$
          String[] toCheck = (notAllowedStringSep==null ? new String[]{notAllowedString}
                                                        : notAllowedString.split(notAllowedStringSep));
          for (int i=0; i<toCheck.length; i++) {
            //OBLog.log.finer("OBUtils.checkField :"+toCheck[i]);
            if (retValue.indexOf(toCheck[i])>=0) {
              String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
              String individualErrMsg = getProp(fprops,localContext+"notallowedstring.errMsg",null); //$NON-NLS-1$
              logger.debug(localContext + "notAllowedString error"); //$NON-NLS-1$
              throw new OBException((individualErrMsg != null ? individualErrMsg : inputWrongString2()), 
                                    new String[] {label,toCheck[i]},
                                    markable);
            }
          }
        }
      }
      
      // neededChars + noNeededChars
      {
        String neededChars = getProp(fprops,localContext+"neededChars",null); //$NON-NLS-1$
        String noNeededChars = getProp(fprops,localContext+"noNeededChars",null); //$NON-NLS-1$
        if (neededChars!=null && noNeededChars!=null) {
          int noNeeded = Integer.parseInt(noNeededChars.trim());
          int countChars=0;
          for (int i = 0; i<retValue.length() && countChars<noNeeded; i++) {
            if (neededChars.indexOf(retValue.charAt(i))>=0) countChars++;
          }
          if (countChars<noNeeded) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"neededChars.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "neededChars error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputNoNeededChars3()), 
                                  new String[] {label,noNeededChars,neededChars},
                                  markable);
          }
        }
      }

      // numberMin
      {
        String numberMin = getProp(fprops,localContext+"numberMin",null); //$NON-NLS-1$
        if (numberMin!=null) {
          if (Long.parseLong(retValue)<Long.parseLong(numberMin)) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"numberMin.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "numberMin error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputToSmall2()), 
                                  new String[] {label,numberMin},
                                  markable);
          }
        }
      }
  
      // numberMax
      {
        String numberMax = getProp(fprops,localContext+"numberMax",null); //$NON-NLS-1$
        if (numberMax!=null) {
          if (Long.parseLong(retValue)>Long.parseLong(numberMax)) {
            String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
            String individualErrMsg = getProp(fprops,localContext+"numberMax.errMsg",null); //$NON-NLS-1$
            logger.debug(localContext + "numberMax error"); //$NON-NLS-1$
            throw new OBException((individualErrMsg != null ? individualErrMsg : inputToLarge2()), 
                                  new String[] {label,numberMax},
                                  markable);
          }
        }
      }
    }
    else {
      // nullable
      String nullable = getProp(fprops,localContext+"nullable",null); //$NON-NLS-1$
      if (nullable!=null) {
        if (nullable.equals("false") || //$NON-NLS-1$
            nullable.equals("no")) { //$NON-NLS-1$
          String label = getProp(fprops,localContext+"label",attName); //$NON-NLS-1$
          String individualErrMsg = getProp(fprops,localContext+"nullable.errMsg",null); //$NON-NLS-1$
          logger.debug(localContext + "nullable error"); //$NON-NLS-1$
          throw new OBException((individualErrMsg != null ? individualErrMsg : inputNotNullable1()), 
                                new String[] {label},
                                markable);
        }
      }
    }
    return retValue;
  } 
 
  /** "Multipliziert" ein Zeichen, z.B. multChar('#', 3) = "###"
    @param c     Zeichen das "multipliziert" werden soll
    @param count Anzahl, wie oft das Zeichen multipliziert werden soll
    @return      String des mutl. Zeichens
   */
  public static String multChar(char c, int count) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < count; i++) {
      sb.append(c);
    }
    return sb.toString();
  }

  /** 
   * Zum Ausblenden von Passwortern
   * @param attName Name des Attributes, das geprueft werden soll
   * @param attValue Wert (wird nur geprueft, wenn es ein String ist)
   * @param hidings String-Array mit Worten. Sind diese case insensitive enthalten, wird ausgesternt
   * @return attValue, wenn nicht ausgesternt werden soll, String mit entsprechender Anzahl Sternen sonst
   */
  public static Object hideValue(String attName, Object attValue,String[] hidings) {
    for (int i = 0; hidings != null && i < hidings.length; i++) {
      if (attName.toUpperCase().contains(hidings[i].toUpperCase()) && attValue instanceof String) {
        return multChar('*', ((String) attValue).length());
      }
    }
    return attValue;
  }
}
