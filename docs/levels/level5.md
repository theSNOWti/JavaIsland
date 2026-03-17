# Wesen mit Eigenschaften

Ein Tier ist mehr als ein Name.

Es hat eine Art.  
Es hat Energie.  
Es hat einen Ort.

Diese Eigenschaften gehören zusammen.

In Java nennt man solche Dinge Objekte.

## Tutorial

### Was ist ein Objekt?
Ein Objekt ist ein konkretes Ding in deiner Welt.

Zum Beispiel ein Tier.

Ein Tier kann besitzen:
- eine Art
- ein Alter
- Energie

Diese Eigenschaften gehören zusammen.

### Klassen

Eine Klasse beschreibt, wie ein Objekt aufgebaut ist.

Sie ist ein Bauplan.

Beispiel:

```
class Animal {
    String species;
    int age;
    int energy;
}
```

Diese Klasse beschreibt ein Tier.

### Ein Objekt erzeugen

Mit `new` wird aus dem Bauplan ein echtes Tier.

```
Animal a1 = new Animal();
```

Jetzt existiert ein Tier auf der Insel.

### Eigenschaften setzen

```
a1.species = "Hirsch";
a1.age = 2;
a1.energy = 10;
```

Das Tier besitzt nun Eigenschaften.

### Mehrere Tiere

Aus einer Klasse können viele Tiere entstehen.

```
Animal a2 = new Animal();
Animal a3 = new Animal();
```

Alle gehören zur Klasse `Animal`,  
aber jedes Tier ist ein eigenes Objekt.

### Verhalten

Objekte können auch Verhalten besitzen.

Beispiel:

```
void move() {
    System.out.println("Das Tier bewegt sich.");
}
```

### Wichtig

- Klasse: Bauplan  
- Objekt: echtes Ding  
- new: erzeugt ein Objekt  
- Eigenschaften können unterschiedlich sein

# Struktur wird zu Existenz

# Task 1 – Die Tierklasse erstellen

## Story

Die ersten Tiere beginnen, die Insel zu betreten.

Der Core braucht einen Bauplan für sie.

## Anweisung
>Erstelle eine Klasse: ``Animal``
>mit folgenden Eigenschaften:  
>```
>String species
>int age
>int energy
>```

## Erwartete Lösung

```
class Animal {
    String species;
    int age;
    int energy;
}
```

# Task 2 – Das erste Tier erschaffen

## Story
Ein Tier nähert sich vorsichtig dem Fluss.

Der Core formt seine Struktur.

## Anweisung
>Erstelle ein Objekt: ``Animal a1``

## Erwartete Lösung

```
Animal a1 = new Animal();
```

# Task 3 – Eigenschaften setzen

## Story
Das Tier wird genauer definiert.

Es ist ein Hirsch, jung und voller Energie.

## Anweisung
>Setze:
>```
>species = "Hirsch"  
>age = 2
>energy = 10
>```

## Erwartete Lösung

```
a1.species = "Hirsch";
a1.age = 2;
a1.energy = 10;
```

# Task 4 – Ein zweites Tier

## Story
Der Hirsch bleibt nicht allein.

Ein weiteres Tier kommt zum Fluss.

## Anweisung
>Erstelle ein zweites Objekt: ``Animal a2``

## Erwartete Lösung

```
Animal a2 = new Animal();
```

# Task 5 – Verhalten hinzufügen

## Story
Die Tiere beginnen, sich über die Insel zu bewegen.

Der Core definiert eine Handlung.

## Anweisung
>Füge der Klasse eine Methode hinzu: ``move()``
>Ausgabe: ``Das Tier bewegt sich über die Insel.``

## Erwartete Lösung

```
void move() {
    System.out.println("Das Tier bewegt sich über die Insel.");
}
```

# Task 6 – Das Tier bewegen

## Story
Der Hirsch bewegt sich vom Fluss in den Wald.

## Anweisung
>Rufe move() für a1 auf.

## Erwartete Lösung

```
a1.move();
```

# Kapitelabschluss

```
Der Hirsch verschwindet zwischen den Bäumen.

Ein zweites Tier trinkt am Fluss.  
Ein drittes bewegt sich am Rand des Sees.

Die Insel ist nicht mehr leer.

Der Core pulsiert ruhig.

Die Welt besitzt nun Leben.
```