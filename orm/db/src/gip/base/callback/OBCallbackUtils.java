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
package gip.base.callback;

import gip.base.common.OBException;
import gip.base.db.OBContext;

import java.rmi.RemoteException;


public class OBCallbackUtils {

  public static final String inputToShort2() { return "inputToShort2"; } //$NON-NLS-1$
  public static final String inputToLong2() { return "inputToLong2"; } //$NON-NLS-1$
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
    OBException.addErrorMessage(inputToLong2(),"Der Inhalt des Feldes {0} darf höchstens {1} Zeichen lang sein."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongChar2(),"Der Inhalt des Feldes {0} darf das Zeichen {1} nicht enthalten. Bitte verwenden Sie nur Zeichen aus der Menge {2}."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongString2(),"Der Inhalt des Feldes {0} darf die Zeichenfolge {1} nicht enthalten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongRegexp2(),"Der Inhalt des Feldes {0} entspricht nicht dem Ausdruck {1}."); //$NON-NLS-1$
    OBException.addErrorMessage(inputNotNullable1(),"Das Feld {0} darf nicht leer sein."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongStartChar2(),"Der Inhalt des Feldes {0} darf nicht mit dem Zeichen {1} beginnen."); //$NON-NLS-1$
    OBException.addErrorMessage(inputWrongNotRegexp2(),"Der Inhalt des Feldes {0} darf nicht dem Ausdruck {1} entsprechen."); //$NON-NLS-1$
    OBException.addErrorMessage(inputNoNeededChars3(),"Der Inhalt des Feldes {0} muss mindestens {1} Zeichen aus der Menge {2} enthalten."); //$NON-NLS-1$
    OBException.addErrorMessage(inputToLong3(),"Der Inhalt des Feldes {0} darf höchstens {1} Zeilen enthalten."); //$NON-NLS-1$
  }

  /**
   * Hier als Util-Metode implementiert, damit man in der TK-Schicht nicht explizit 
   * Remote-Exceptions fangen muss.
   * @param context
   * @param decision
   * @return Entscheidung
   * @throws OBException
   */
  public static boolean decideBoolean (OBContext context, DecisionContainer decision) throws OBException {
    try {
      if (context.getCallBack()==null) {
        context.setCallBack(new SimpleCallbackDefault());
      }
      boolean dec = context.getCallBack().decideBoolean(decision);
      return dec;
    }
    catch (RemoteException e) {
      throw new OBException(OBException.OBErrorNumber.callback);
    }
  }
  
  /**
   * Hier als Util-Metode implementiert, damit man in der TK-Schicht nicht explizit 
   * Remote-Exceptions fangen muss.
   * @param context
   * @param decision
   * @return Entscheidung
   * @throws OBException
   */
  public static String decideMultipleChoice(OBContext context, DecisionContainer decision) throws OBException {
    try {
      if (context.getCallBack()==null) {
        context.setCallBack(new SimpleCallbackDefault());
      }
      String answer = context.getCallBack().decideMultipleChoice(decision);
      return answer;
    }
    catch (RemoteException e) {
      throw new OBException(OBException.OBErrorNumber.callback);
    }
  }
 
}
