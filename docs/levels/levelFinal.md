# Wenn der Ablauf bricht

Nicht jeder Ablauf endet wie geplant.

Manche Fehler treten erst während der Ausführung auf.

Wenn sie nicht behandelt werden,  
kann der gesamte Prozess scheitern.

## Tutorial

### Was ist eine Exception?
Eine Exception ist ein Fehler, der während der Ausführung eines Programms auftritt.

Das Programm startet normal,  
doch im Ablauf passiert etwas Unerwartetes.

Zum Beispiel:
- Division durch 0  
- Zugriff auf etwas, das nicht existiert  
- ein ungültiger Zustand  

Ohne Behandlung kann das Programm abbrechen.

### try

Code, der fehlschlagen könnte, schreibt man in einen try-Block.

```
try {
    // riskanter Code
}
```

Das bedeutet:  
Versuche, diesen Code auszuführen.

### catch

Wenn dabei ein Fehler auftritt,  
kann er mit catch abgefangen werden.

```
catch (Exception e) {
    // Fehlerbehandlung
}
```

Das bedeutet:  
Wenn ein Fehler passiert, reagiere darauf.

### finally

Der finally-Block wird immer ausgeführt.

Egal, ob ein Fehler auftritt oder nicht.

```
finally {
    // läuft immer
}
```

Das ist nützlich für Dinge, die auf jeden Fall passieren sollen,  
zum Beispiel:
- Stabilisierung  
- Aufräumen  
- Abschluss

### Gesamtstruktur

```
try {
    // normaler Ablauf
} catch (Exception e) {
    // Fehler behandeln
} finally {
    // immer ausführen
}
```

### Letzte Inschrift

Perfekter Code ist selten.  
Stabiler Code erkennt den Fehler  
und macht weiter.

# Stabilität im Chaos

# Task 1 – Der erste Startversuch

## Story
Der Core wird zum ersten Mal vollständig aktiviert.

Niemand weiß, ob der Start stabil ist.

```
Core:
Der Startvorgang ist riskant.
Markiere den Bereich, in dem der Versuch stattfindet.
```

## Anweisung

>Schreibe einen try-Block.
>In ihm soll ausgegeben werden:
>``Der Core startet.``

## Hinweis
>Ein try-Block beginnt mit: ``try {``
>und endet mit:``}``

## Erwartete Lösung

```
try {
    System.out.println("Der Core startet.");
}
```

## Bei Erfolg

Der Core leuchtet auf.  
Die Insel bebt leicht.  

Doch das Licht bleibt instabil.

# Task 2 – Den Fehler abfangen

## Story
Während des Startvorgangs tritt ein Fehler auf.

Energie schießt unkontrolliert durch den Kern.

```
Core:
Ein Fehler wurde erkannt.
Fange ihn ab, bevor der Ablauf zusammenbricht.
```

## Anweisung
>Erweitere den Code:
>Im try-Block soll zuerst ausgegeben werden:  
>``Der Core wird aktiviert.``
>Danach soll absichtlich ein Fehler entstehen:
>``int energy = 10 / 0;``
>Im catch-Block soll ausgegeben werden:  
>``Fehler im Core erkannt.``

## Hinweis
>Verwende:``catch (Exception e)``

## Erwartete Lösung

```
try {
    System.out.println("Der Core wird aktiviert.");
    int energy = 10 / 0;
} catch (Exception e) {
    System.out.println("Fehler im Core erkannt.");
}
```

## Bei Erfolg

Das Licht des Cores bricht kurz zusammen.  

Doch statt die Insel mitzureißen, wird der Fehler abgefangen.

Der Riss im Kern stoppt.

# Task 3 – Die Insel stabilisieren

## Story
Unabhängig davon, ob der Start gelingt oder nicht,  
muss die Insel stabilisiert werden.

Der Fluss darf nicht versiegen.  
Die Tiere dürfen nicht fliehen.  
Die Fragmente dürfen nicht auseinanderbrechen.

```
Core:
Der Abschluss muss immer stattfinden.
```

## Anweisung
>Ergänze den bisherigen Code um einen finally-Block.
>Darin soll ausgegeben werden:  
>``Die Insel wird stabilisiert.``

## Hinweis
>finally steht nach catch.

## Erwartete Lösung

```
try {
    System.out.println("Der Core wird aktiviert.");
    int energy = 10 / 0;
} catch (Exception e) {
    System.out.println("Fehler im Core erkannt.");
} finally {
    System.out.println("Die Insel wird stabilisiert.");
}
```

## Bei Erfolg

Lichtlinien laufen über die Insel.  

Der Fluss beruhigt sich.  
Der Boden schließt seine Risse.

# Task 4 – Der letzte Test

## Story
Der Core ist fast bereit.

Ein letzter Test muss zeigen, ob du wirklich verstanden hast,  
wie mit Fehlern umzugehen ist.

Diesmal soll der Start ohne Fehler ablaufen.  
Doch die Stabilisierung muss trotzdem stattfinden.

```
Core:
Zeige, dass du den Unterschied verstehst.

Fehlerbehandlung bedeutet nicht nur, auf Fehler zu warten.
Sie bedeutet auch, einen Ablauf sauber zu beenden.
```

## Anweisung
>Schreibe einen vollständigen Ablauf mit:
>```
>try  
>catch  
>finally  
>```
>Im try-Block:  
>``Der Core läuft stabil.``
>Es soll kein absichtlicher Fehler eingebaut werden.
>Im catch-Block:  
>``Fehler erkannt.``
>Im finally-Block:
>``Systemabschluss vollständig.``

## Erwartete Lösung

```
try {
    System.out.println("Der Core läuft stabil.");
} catch (Exception e) {
    System.out.println("Fehler erkannt.");
} finally {
    System.out.println("Systemabschluss vollständig.");
}
```

## Bei Erfolg

Der Core stabilisiert sich vollständig.

Alle Fragmente leuchten gleichzeitig.  
Der Himmel klärt sich.  

Der Fluss, der See, die Tiere und die Vegetation reagieren  
in einem einzigen ruhigen Puls.

Die Insel ist repariert.