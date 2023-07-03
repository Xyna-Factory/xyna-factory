Mit dem generateRaygressionTestData Script lassen sich Raygression Testserien im XTF Modeller automatisch erstellen.

Beispiel:
M�chte man beispielsweise 25 Raygressionstests erstellen dann müsste man für jeden Test einen Eintrag mit
case: 'Raygression: Testserienname_1', file: 'testcases/raygression/testserienname/testserienname_1/testseries.json'
erstellen. Das Script automatisiert das Eintragen der Testserien.

Benutzung:
node generateRaygressionTestData <start-index> <end-index> <serien-name> <?root-pfad>
    start-index: Index mit dem die Indizierung startet
    end-index: Index mit dem die Indizierung endet
    serien-name: Der Name der Testserie. Muss für die gesamte Ordnerstruktur gleich sein.
        (Am besten das generateTestFolders Script benutzen)
    root-pfad: Zurzeit nicht unterstützt. Wenn notwendig bitte Noel Schwabenland kontaktieren.