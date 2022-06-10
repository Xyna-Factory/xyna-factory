Die Klasse ExceptionStorage liest FehlerCodes und zugehörige Meldungen aus einem oder mehreren XML
Files aus. Zur Laufzeit werden dann die Meldungen zu den Codes nachgeschlagen und in der eingestellten
Sprache ausgegeben.

Ausführung der Tests:
ExceptionStorage main Methode ausführen um Exception-Klassen zu generieren.
Dann Test Klassen ausführen.

Beispiel zur Verwendung:

1. xynautils-exceptions-....jar file verwenden

2. Fehlermeldungen in einem XML File definieren (siehe MessageStorage.1.1.xsd)
<ExceptionStore xmlns="http://www.gip.com/xyna/3.0/utils/message/storage/1.1"
              Name="ExampleExceptionStore" Version="1.0" Type="ExceptionMasterFile"
              DefaultLanguage="DE">
  <Description>zeigt, wie man Fehlernachrichten in XML verwaltet.</Description>
  <Include File="AdditionalStorage.1.1.xml"/>
  <ExceptionType Code="XYNATEST2-00001" TypeName="TestException1" TypePath="ex.test" >
    <MessageText Language="DE">Es ist ein Fehler passiert</MessageText>
  </ExceptionType>
  <ExceptionType Code="XYNATEST2-00002" TypeName="TestException2" TypePath="ex.test" >
    <Description>Für total unerwartete Fehler!</Description>
    <Data Label="Fehlerbeschreibung in kurz" VariableName="errorDescription">
    	<Meta>
    		<Type>int</Type>
    	</Meta>
    </Data>
    <MessageText Language="DE">Es ist der unerwartete Fehler %0% passiert</MessageText>
    <MessageText Language="EN">Unexpected Error %0% occurred</MessageText>
  </ExceptionType>
  <ExceptionType Code="XYNATEST2-00002a" TypeName="TestException3" TypePath="ex.test" >
    <MessageText Language="DE">Es ist %0% ein Fehler passiert</MessageText>
  </ExceptionType>
</ExceptionStore>

3. java-start-parameter setzen:
-Dexceptions.storage=$ORACLE_HOME/j2ee/processing/Exceptions.xml

4. Exception-Klassen generieren
zb mit diesem ant target:
 <!-- - - - - - - - - - - - - - - - - - 
             target: genCodeClass                      
            - - - - - - - - - - - - - - - - - -->
  <target name="genCodeClass">
    <java fork="true"
          failonerror="yes"
          classname="com.gip.xyna.utils.exceptions.ExceptionStorage">
      <classpath>
        <fileset dir="${basedir}/lib">
          <include name="**/*.jar" />
        </fileset>
      </classpath>

      <arg value="${basedir}/Exceptions.xml" />
      <arg value="${basedir}/src" />
    </java>
  </target>


4. Coden
...
try {
  if (0==0) throw new TestException2(123);
  if (0==1) throw new TestException1();
} catch (Exception e) {  
  //e.getMessage() enthält die entsprechende fehlermeldung
}

5. FehlerCodes im XML

können über die Klasse com.gip.xyna.utils.eceptions.exceptioncode.ExceptionCodeManagement auch generiert werden.
Ein Management der vorhandenen Codes für Persistenz der bestehenden Codes muss selbst gebaut werden. Siehe beispielsweise 
Implementierung in der Blackedition.
