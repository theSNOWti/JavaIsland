# „Speichern bedeutet Existenz“
- Die Welt besteht aus Werten.
- Wenn ein Wert nicht gespeichert wird, verschwindet er.
- Um Werte zu speichern, brauchst du Variablen.

## Tutorial
### Was ist eine Variable?
Eine Variable ist ein benannter Speicherplatz im Arbeitsspeicher.
Sie hat:
- einen Typ
- einen Namen
- einen Wert

Beispiel:
```
int trees = 3;
// int → Typ
// trees → Name
// 3 → gespeicherter Wert
```

### Was ist ein Datentyp?
Ein Datentyp bestimmt, welche Art von Wert gespeichert werden darf.
Die Insel akzeptiert keine falschen Formen.

>Wichtige Datentypen in Java (Grundlagen)
>1. String
>Speichert Text.
>String name = "Alex";
>Text steht immer in Anführungszeichen.
>2. int
>Speichert ganze Zahlen (ohne Komma).
>int trees = 5;
>3. double
>Speichert Kommazahlen.
>double treeHeight = 12.5;
>4. boolean
>Speichert Wahrheitswerte.
>Nur zwei mögliche Werte:
>boolean isStable = true;
>boolean hasKey = false;

### Variablen verwenden
Eine Variable kann in einer Ausgabe verwendet werden:
`System.out.println(trees);`
Oder kombiniert mit Text:
`System.out.println("Bäume: " + trees);`
Das `+` verbindet Text und Werte.

### Variablen verändern
Der gespeicherte Wert kann geändert werden:
```
trees = trees + 2;
// Kurzform:
trees += 2;
```

Die Variable behält ihren Namen.
Nur ihr Inhalt ändert sich.

### Wichtig
- Der Typ darf nicht geändert werden.
- Ein String ist keine Zahl.
- Ein boolean ist nur true oder false.
- Eine Variable muss deklariert werden, bevor sie benutzt wird.

# Das Gedächtnis der Insel
- Nach dem Fragment der Struktur
## Visual:
Der Core pulsiert gleichmäßig.
Ein schwacher Wind zieht über das graue Land.
Der Boden beginnt leicht zu leuchten – als würde er auf Eingaben warten.

# Task 1 – Dein Name ist gespeichert
```Text
Core (ruhig, mechanisch):
Wissen wurde gelesen.
Doch Wissen ohne Anwendung ist bedeutungslos.
Speichere deinen Namen.
Gib ihm einen Platz in dieser Welt.
Text nennt man: String.
```

## Anweisung
>Erstelle eine Variable vom Typ String mit dem Namen name.
>Speichere deinen Namen.
>Gib ihn mit System.out.println() aus.

## Hint
>Text steht in Anführungszeichen.
>Eine Variable muss deklariert werden, bevor sie ausgegeben werden kann.

## Bei Erfolg
### Visual
- Ein kleiner grüner Fleck durchbricht den Boden.
- Der Name hat Wurzeln geschlagen.
```Text
Core:
Identität gespeichert.
Struktur bestätigt.
```

# Task 2 – Die ersten Bäume
- Der Boden bleibt grau.
- Nur ein kleiner grüner Punkt.
```Text
Core:
Leben braucht Menge.
Menge braucht Zahlen.
Ganze Zahlen nennt man: int.
Wie viele Bäume soll diese Insel tragen?
```

## Anweisung
>Erstelle eine Variable int trees.
>Weise ihr eine Zahl zu (z.B. 3).
>Gib aus:
>Bäume: X
>(X soll durch die Variable ersetzt werden.)

## Hint
>Text und Variable werden mit + verbunden.
>Eine int-Zahl hat kein Komma.

## Bei Erfolg
### Visual
- Mehrere kleine Baum-Silhouetten erscheinen.
- Noch grau. Noch schwach.
```Text
Core:
Anzahl gespeichert.
Objekte materialisieren.
```

# Task 3 – Wachstum kontrollieren
- Die neuen Bäume beginnen zu wachsen.
- Zu schnell.
- Ihre Kronen strecken sich immer höher in den Himmel.
- Einige wirken, als würden sie nie aufhören zu wachsen.
- Der Boden bebt leicht.
```Text
Core:
Unkontrolliertes Wachstum erkannt.
Diese Welt benötigt Grenzen.
Höhen lassen sich nicht immer mit ganzen Zahlen beschreiben.
Manche Werte brauchen Präzision.
Für solche Werte existiert: double.
Definiere die maximale Höhe,
die Bäume auf dieser Insel erreichen dürfen.
```

## Anweisung
>Erstelle eine Variable double maxTreeHeight.
>Speichere eine Kommazahl (z.B. 5.5 für Meter).
>Gib aus:
>Maximale Baumhöhe: X
>(X soll durch die Variable ersetzt werden.)

## Hint
>Kommazahlen verwenden einen Punkt statt eines Kommas.

## Bei Erfolg
### Visual
- Das Wachstum der Bäume verlangsamt sich.
- Ihre Kronen stoppen exakt auf gleicher Höhe.
- Die Insel wirkt geordneter.
```Text
Core:
Grenzwert gespeichert.
Wachstum stabilisiert.
```

# Task 4 – Zustand prüfen
- Der Wind wird stärker.
- Der Boden zittert kurz.
```Text
Core:
Manche Zustände sind binär.
Wahr oder falsch.
boolean speichert Entscheidungen.
Ist diese Insel stabil?
```

## Anweisung
>Erstelle eine Variable boolean isStable.
>Setze sie auf true.
>Gib aus:
>Stabil: true

## Hint
>boolean kennt nur true oder false.
>Keine Anführungszeichen bei true.

## Bei Erfolg
### Visual
- Der Boden beruhigt sich.
- Die Bäume stehen fest.
- Ein kleiner See bildet sich.
```Text
Core:
Stabilität bestätigt.
```

# Task 5 – Wachstum
- Die Insel lebt – aber sie ist klein.
```Text
Core:
Werte sind nicht statisch.
Erhöhe die Anzahl der Bäume.
Manipuliere die gespeicherte Zahl.
```

## Anweisung
>Erhöhe trees um 5.
>Gib den neuen Wert aus.

## Hint
>`trees = trees + 5;` oder `trees += 5;`

## Bei Erfolg
### Visual
- Die Baum-Silhouetten werden echte Bäume.
- Grün breitet sich weiter aus.
- Das Grau zieht sich langsam zurück.
- Der Core leuchtet nun konstant.
```Text
Core (leiser):
Du speicherst.
Du veränderst.
Die Insel beginnt, sich zu erinnern.
```

# Kapitelabschluss
```Text
Variablen geben der Welt Gedächtnis.
Datentypen geben ihr Form.
Doch sie reagiert noch nicht von selbst.
Der Core zeigt ein neues Fragment.
Es flackert.
```