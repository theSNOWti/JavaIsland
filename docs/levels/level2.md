# „Logik erschafft Konsequenzen“
- Werte allein verändern nichts.
- Erst Bedingungen entscheiden, wann etwas passiert.
- Eine Welt ohne Bedingungen reagiert nicht.

## Tutorial

### Was ist eine Bedingung?
Eine Bedingung ist ein Ausdruck, der zu true (wahr) oder false (falsch) wird.

Beispiel:
```
trees >= 5
```

Diese Bedingung ist entweder wahr oder falsch.

### Die if-Struktur
Mit if wird Code nur ausgeführt, wenn eine Bedingung wahr ist.

```
if (trees >= 5) {
    System.out.println("Genug Bäume vorhanden.");
}
```

Wenn die Bedingung nicht erfüllt ist, passiert nichts.

### else
Mit else wird festgelegt, was passiert, wenn die Bedingung nicht erfüllt ist.

```
if (trees >= 5) {
    System.out.println("Genug Bäume.");
} else {
    System.out.println("Nicht genug Bäume.");
}
```

### Vergleichsoperatoren

| Operator | Bedeutung |
|--------|--------|
| == | gleich |
| != | ungleich |
| > | größer |
| < | kleiner |
| >= | größer oder gleich |
| <= | kleiner oder gleich |

Wichtig:
- == prüft Gleichheit
- = weist einen Wert zu (kein Vergleich!)

### Bedingungen kombinieren

UND – &&
Beide Bedingungen müssen wahr sein.

```
if (trees >= 5 && waterLevel > 2.0)
```

ODER – ||
Mindestens eine Bedingung muss wahr sein.

```
if (trees >= 5 || isStable == true)
```

### Wichtige Hinweise
- Eine Bedingung muss immer true oder false ergeben.
- Zahlenvergleiche funktionieren mit int und double.
- boolean kann direkt geprüft werden:

```
if (isStable)
```

oder

```
if (!isStable)
```

# Die Regeln der Welt

## Visual
Die Insel ist teilweise grün.

Doch sie reagiert noch nicht selbstständig.

Der Core pulsiert unruhig.

# Task 1 – Die Brücke reparieren

```Text
Core:
Eine Schlucht trennt diese Insel.
Die Brücke liegt zerbrochen darin.

Die Brücke repariert sich nur,
wenn genügend Material vorhanden ist.

Prüfe die Anzahl der Bäume.
```

## Anweisung
>Wenn trees >= 5  
>Ausgabe: Die Brücke wurde repariert.

>Sonst  
>Nicht genügend Material.

## Hint
>Nutze eine if-Struktur.  
>Für den zweiten Fall brauchst du else.

## Bei Erfolg

### Visual
- Steine beginnen zu schweben.
- Sie setzen sich Stück für Stück zusammen.
- Die Brücke spannt sich über die Schlucht.

```Text
Core:
Struktur wiederhergestellt.
Verbindung hergestellt.
```

# Task 2 – Der See entsteht

- Hinter der Brücke liegt ein trockenes Becken.

```Text
Core:
Ein See entsteht nicht allein durch Material.

Er benötigt Wasser.
Und Struktur.

Beide Bedingungen müssen erfüllt sein.
```

## Anweisung
>Wenn  
>trees >= 5 && waterLevel > 2.0

>Dann
>Der See füllt sich.

>Sonst  
>Das Becken bleibt trocken.

## Hint
>&& bedeutet UND.  
>Beide Bedingungen müssen wahr sein.

## Bei Erfolg

### Visual
- Wasser sammelt sich im Becken.
- Ein kleiner See entsteht.
- Die Luft wirkt feuchter.

```Text
Core:
Feuchtigkeit erkannt.
Ökosystem erweitert.
```

# Task 3 – Vegetation wächst

- Feuchtigkeit liegt in der Luft.
- Doch der Boden bleibt teilweise karg.

```Text
Core:
Wasser allein erschafft kein Leben.

Der Boden besitzt einen Wert:
Fruchtbarkeit.

Prüfe ihn.
```

(Vorausgesetzt: double fertility existiert.)

## Anweisung
>Wenn  
>waterLevel > 2.0 && fertility > 0.5

>Dann
>Vegetation breitet sich aus.

>Sonst  
>Der Boden bleibt karg.

## Hint
>Auch hier müssen zwei Bedingungen gleichzeitig erfüllt sein.

## Bei Erfolg

### Visual
- Gräser sprießen rund um den See.
- Erste Büsche entstehen.
- Die Insel wirkt lebendiger.

```Text
Core:
Vegetation etabliert.
Leben breitet sich aus.
```

# Task 4 – Ein Energieschild entsteht

- Risse am Rand der Insel öffnen sich ins Nichts.

```Text
Core:
Ein Schutzschild aktiviert sich,
wenn entweder die Resonanz hoch genug ist
ODER genügend Struktur vorhanden ist.
```

## Anweisung
>Wenn  
>resonance > 0.8 || trees >= 8  

>Dann
>Das Energieschild aktiviert sich.

>Sonst  
>Die Risse bleiben offen.

## Hint
>`||` bedeutet **ODER**.  
>Nur **eine** der Bedingungen muss wahr sein.

## Bei Erfolg

### Visual
- Ein bläuliches Energiefeld spannt sich über den Rand der Insel.
- Die Risse schließen sich langsam.
- Die Luft beginnt leicht zu vibrieren.

```Text
Core:
Schutzsystem aktiviert.
Instabilität reduziert.
```

# Kapitelabschluss

```Text
Du hast gelernt:

Werte alleine verändern nichts.
Erst Bedingungen erschaffen Konsequenzen.

Doch die Welt reagiert nur einmal.

Wiederholung steht bevor.
```