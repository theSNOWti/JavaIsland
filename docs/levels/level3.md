# „Wiederholung formt Prozesse“

## Tutorial

### Was ist eine Schleife?
Eine Schleife wiederholt Code mehrfach, ohne dass du ihn mehrfach schreiben musst.

>Wenn du etwas 10-mal tun willst,  
>schreibst du es nicht 10-mal.  
>Du lässt es laufen.

### while-Schleife

Eine while-Schleife wiederholt, solange eine Bedingung wahr ist.

```
while (Bedingung) {
    // wird wiederholt, solange Bedingung true ist
}
```

Beispiel:

```
int i = 0;

while (i < 3) {
    System.out.println("Wächst...");
    i++;
}
```

**Wichtig:**

>Wenn die Bedingung nie false wird, läuft die Schleife endlos.
>
>Damit sie endet, brauchst du oft ein `i++` (Zähler erhöhen).

### for-Schleife

Eine for-Schleife ist praktisch, wenn du genau weißt, wie oft du wiederholen willst.

```
for (Start; Bedingung; Änderung) {
    // wird wiederholt
}
```

Beispiel:

```
for (int i = 1; i <= 3; i++) {
    System.out.println("Baum " + i);
}
```

### Zählvariablen

Oft nutzt man einen Zähler:

``i = 0`` oder ``i = 1``

``i++`` erhöht um 1.

### break

Beendet die Schleife sofort.

```
while (true) {
    break;
}
```

### Zusammenfassung

- **while:** Wiederhole, solange eine Bedingung gilt
- **for:** Wiederhole mit Zähler, fest strukturiert
- Schleifen sind nötig, um Prozesse in der Welt zu erzeugen.

# Wiederholung formt Prozesse

# Task 1 – Der Berg wird gescannt

## Story
Scheinbar fehlen aber noch einige Fragmente.

Der Core beginnt deshalb mit einem ersten Suchlauf des Berges.

Mehrere Höhenstufen werden nacheinander geprüft.

```
Core:
Der Ursprung des Signals ist unklar.
Scanne die unteren Bereiche des Berges.
Einen nach dem anderen.
```

## Anweisung
>Schreibe eine for-Schleife von 1 bis 5.  
>Gib in jedem Durchlauf aus:  
>Höhenstufe X untersucht.

## Hinweis
>Starte bei 1
>Wiederhole bis 5
>Erhöhe den Zähler mit i++

## Erwartete Lösung

```
for (int i = 1; i <= 5; i++) {
    System.out.println("Höhenstufe " + i + " untersucht.");
}
```

## Bei Erfolg

### Visual

- Leuchtende Scanlinien laufen den Hang hinauf.
- Fünf Bereiche glimmen nacheinander auf.
- Das Signal wird klarer – aber noch nicht stark genug.
- Eines steht jedoch fest:  
- Das Fragment befindet sich nahe dem Berg.

# Task 2 – Jeder Baum wird passiert

## Story
Am Bergpfad stehen die Bäume, die du zuvor erschaffen hast.

Jeder einzelne markiert ein Stück des Weges.

Der Aufstieg führt an allen vorbei.

```
Core:
Die Baumreihe bildet den unteren Pfad.
Gehe an jedem Baum vorbei, um den Berg zu erreichen.
```

## Anweisung
>Die Variable trees existiert bereits.  
>Schreibe eine for-Schleife, die von 1 bis trees läuft.

>Gib aus:  
>Baum X passiert.

## Hinweis
>Verwende trees als Obergrenze.  
>Die Schleife passt sich automatisch an die gespeicherte Baumanzahl an.

## Erwartete Lösung

```
for (int i = 1; i <= trees; i++) {
    System.out.println("Baum " + i + " passiert.");
}
```

## Bei Erfolg

### Visual

- Du gehst den unteren Pfad entlang.
- Jeder Baum markiert einen Abschnitt des Weges.
- Am Ende beginnt der steilere Aufstieg.

```
Core:
Unterer Pfad abgeschlossen.
```

# Task 3 – Der Berg wird erklommen

## Story
Nach dem unteren Pfad beginnt der eigentliche Aufstieg.

Der Hang ist steil, und jeder Schritt muss einzeln gesetzt werden.

Das Signal des Fragments wird mit jeder Höhe stärker.

```
Core:
Der Gipfel wird nicht in einem Sprung erreicht.
Wiederhole den Aufstieg,
solange noch Schritte fehlen.
```

## Anweisung
>Erstelle zuerst:
>// int step = 1;
>Nutze danach eine while-Schleife:

>Solange der Gipfel noch nicht erreicht ist (isAtPeak())
>gib aus: Schritt X zum Gipfel  
>erhöhe step um 1

## Hinweis
>while kann auch mit einem Zähler arbeiten.
>Nutze step++

## Erwartete Lösung

```
int step = 1;

while (!isAtPeak()) {
    System.out.println("Schritt " + step + " zum Gipfel");
    step++;
}
```

## Bei Erfolg

### Visual

- Fünf Felsstufen werden nacheinander überwunden.
- Oben wird das Signal deutlich stärker.

```
Core:
Höhe erreicht.
Fragment-Signatur verstärkt.
```

# Task 4 – Markierungssteine werden gesetzt

## Story
Der Weg zum Gipfel ist gefährlich.

Damit der Rückweg nicht verloren geht, setzt der Core Markierungssteine.

Sie werden nicht bei jedem Meter platziert,  
sondern in größeren Abständen.

```
Core:
Nicht jede Wiederholung nutzt Einerschritte.
Manche Wege werden in Sprüngen markiert.
```

## Anweisung
>Schreibe eine for-Schleife von 1 bis 10.
>Erhöhe den Zähler immer um 2.
>Gib aus:  
>Markierungsstein bei Meter X gesetzt.

## Hinweis
>Nutze``i += 2``
>statt``i++``

## Erwartete Lösung

```
for (int i = 1; i <= 10; i += 2) {
    System.out.println("Markierungsstein bei Meter " + i + " gesetzt.");
}
```

## Bei Erfolg

### Visual

- In gleichmäßigen Abständen erscheinen leuchtende Steine am Pfad.
- Der Weg nach unten ist nun gesichert.

```
Core:
Rückweg markiert.
```

# Task 5 – Das Fragment wird gefunden
Konzept: break

## Story
Nahe dem Gipfel beginnt der Core mit einem präzisen Suchlauf.

Mehrere Bereiche des Felsens werden untersucht.

Dann – plötzlich – schlägt das Signal aus.

Das Fragment ist gefunden.

```
Core:
Sobald das Fragment lokalisiert ist,
endet die Suche sofort.
```

## Anweisung
>Schreibe eine for-Schleife von 1 bis 10.
>Gib in jedem Durchlauf aus:  
>Bereich X wird durchsucht.
>Wenn das Fragment gefunden wurde (fragmentFound()), gilt:
>gib zusätzlich aus: Fragment gefunden.  
>beende die Schleife mit break;

## Erwartete Lösung

```
for (int i = 1; i <= 10; i++) {

    System.out.println("Bereich " + i + " wird durchsucht.");

    if (fragmentFount()) {
        System.out.println("Fragment gefunden.");
        break;
    }

}
```

## Bei Erfolg

### Visual

- Ein Fragment löst sich aus dem Gestein.
- Es hebt sich langsam in die Luft und beginnt zu leuchten.
- Der Core pulsiert kräftiger als zuvor.

```
Core:
Fragment geborgen.
Wiederholung erfolgreich abgeschlossen.
```

# Kapitelabschluss

```
Der Berg wurde gescannt.

Der Pfad wurde begangen.

Der Aufstieg wurde Schritt für Schritt vollzogen.

Der Rückweg wurde markiert.

Und das verlorene Fragment wurde gefunden.

Das neue Fragment schwebt zurück zum Core.

Beim Kontakt mit dem Kern pulsiert die Insel für einen Moment vollständig.

Mehr Grün breitet sich aus.

Der Berg wirkt nicht länger wie ein totes Hindernis,  
sondern wie ein Teil der Welt.

Du hast gelernt, Prozesse zu steuern.

Doch noch muss jede Handlung vollständig ausgeschrieben werden.
```