# Handlungen mit Namen

Manche Handlungen wiederholen sich.

Ein Baum wächst.  
Ein Stein wird gesetzt.  
Ein Bereich wird gescannt.

Wenn dieselbe Handlung mehrfach benötigt wird,  
kann man ihr einen Namen geben.

Dieser Name ist eine Methode.

## Tutorial

### Was ist eine Methode?

Eine Methode ist ein benannter Codeblock, der eine bestimmte Aufgabe ausführt.

Beispiel:

```
public static void growTree() {
    System.out.println("Der Baum wächst.");
}
```

Der Code in der Methode wird erst ausgeführt, wenn sie aufgerufen wird.

### Eine Methode aufrufen

Um eine Methode zu benutzen, schreibt man ihren Namen:

```
growTree();
```

Dann wird der Code in der Methode ausgeführt.

### Methodenstruktur

Eine Methode besteht aus:

```
public static void methodName() {
    // Code
}
```

Bedeutung:

| Teil | Bedeutung |
|------|----------|
| public | Methode ist sichtbar |
| static | gehört zur Klasse |
| void | Methode gibt keinen Wert zurück |
| methodName | Name der Methode |

### Methoden mit Parametern

Manchmal braucht eine Methode Informationen.  
Diese nennt man Parameter.

Beispiel:

```
public static void growTrees(int amount) {
    for (int i = 1; i <= amount; i++) {
        System.out.println("Baum wächst.");
    }
}
```

Aufruf:

```
growTrees(3);
```

Die Methode erhält dann amount = 3.

### Methoden mit Rückgabewert

Eine Methode kann auch ein Ergebnis zurückgeben.

```
public static int doubleNumber(int x) {
    return x * 2;
}
```

Aufruf:

```
int result = doubleNumber(4);
```

result enthält dann 8.

**Wichtig:**

>Methoden helfen, Code übersichtlich zu halten  
>Sie können mehrfach aufgerufen werden
>Sie können Parameter erhalten
>Sie können Werte zurückgeben

# Strukur erschafft Handlung

# Task 1 – Eine Quelle am Berg rufen

## Story
Hoch oben im Berg sammelt sich Wasser.

Doch es tritt nur hervor, wenn der Core den richtigen Impuls sendet.

```
Core:
Gib dieser Handlung einen Namen.
Wenn du Wasser rufen willst,
soll ein einziger Aufruf genügen.
```

## Anweisung
>Erstelle eine Methode: ``callSpring``
>Sie soll ausgeben: ``Die Quelle bricht aus dem Berg hervor.``

## Hinweis
>Die Methode soll keinen Wert zurückgeben (void).

## Erwartete Lösung

```
public static void callSpring() {
    System.out.println("Die Quelle bricht aus dem Berg hervor.");
}
```

# Task 2 – Mehr Wasser in den Fluss schicken

## Story
Die Quelle speist den Fluss.

Ein einzelner Ausbruch reicht jedoch nicht aus.

Mehr Wasser muss den Hang hinunterfließen.

```
Core:
Wiederhole die Handlung.
Nutze ihren Namen.
```

## Anweisung
>Rufe ``callSpring()`` dreimal auf.

## Erwartete Lösung

```
callSpring();
callSpring();
callSpring();
```

## Bei Erfolg

### Visual

- Mehr Wasser strömt aus dem Berg.
- Der Fluss beginnt stärker zu fließen.

# Task 3 – Regen über dem Berg auslösen

## Story
Manchmal reicht eine Quelle nicht.

Dann sammelt sich Wasser in den Wolken über dem Berg  
und fällt als Regen in den Flusslauf.

Doch Regen fällt nicht immer gleich stark.

```
Core:
Eine Handlung kann eine Menge erhalten.
Dann lässt sie sich anpassen.
```

## Anweisung
>Erstelle eine Methode: ``makeRain``
>Parameter: ``int amount``
>Die Methode soll amount mal ausgeben:  
>Regen fällt in den Fluss.

## Hinweis
>Verwende eine for-Schleife.

## Erwartete Lösung

```
public static void makeRain(int amount) {
    for (int i = 1; i <= amount; i++) {
        System.out.println("Regen fällt in den Fluss.");
    }
}
```

# Task 4 – Regen mit gespeicherter Inselgröße auslösen

## Story
Der Core kennt bereits Werte der Insel.

Die Menge des Regens soll nicht fest sein,  
sondern sich an einem vorhandenen Wert orientieren.

Der Core entscheidet:  
Je mehr Bäume die Insel besitzt,  
desto mehr Wasser benötigt der Fluss.

## Anweisung
>Rufe ``makeRain()`` mit der Variable trees auf.

## Erwartete Lösung

```
makeRain(trees);
```

## Bei Erfolg

### Visual

- Regen fällt mehrfach in den Flusslauf.
- Der Strom wird breiter.

# Task 5 – Den Flusspegel berechnen

## Story
Durch Quellen und Regen steigt der Pegel des Flusses.

Der Core möchte berechnen,  
wie hoch der Wasserstand nach einem Zufluss sein wird.

## Anweisung
>Erstelle eine Methode: ``raiseRiverLevel``
>Parameter: ``double level``
>Die Methode soll zurückgeben: ``level + 1.0``

## Hinweis
>Nutze return.

## Erwartete Lösung

```
public static double raiseRiverLevel(double level) {
    return level + 1.0;
}
```

# Task 6 – Den neuen Pegel speichern

## Story
Der Fluss fließt nun kräftiger vom Berg zum See.

Der Core möchte den neuen Wasserstand speichern.

## Anweisung
>Speichere das Ergebnis der Methode in einer Variable: ``newRiverLevel``
>Nutze dabei ``waterLevel`` als Eingabewert.

## Erwartete Lösung

```
double newRiverLevel = raiseRiverLevel(waterLevel);
```

# Kapitelabschluss

```
Vom Berg fließt nun dauerhaft Wasser.

Die Quelle speist den Strom.  
Regen verstärkt ihn.  

Der Fluss windet sich durch das Tal  
und mündet schließlich in den See.

Der Core pulsiert ruhig.

Du hast gelernt, Handlungen zu formen.
```