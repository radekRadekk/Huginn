# Huginn
Język ten został zaprojektowany, aby tworzony kod jak najbardziej trzymał się oczekiwanego standardu, co ma zminimalizować błędy.


W języku Huginn wyróżniamy trzy typy zmiennych:

1. INTEGER - do przechowywania liczb całkowitych,
2. REAL - do przechowywania liczb zmiennoprzecinkowych,
3. BOOL - do przechowywania wartości logicznych.

----
Komendy kończą się średnikiem.

// na początku linii oznacza komentarz.

----
Dostępne są następujące operacje:
- deklaracja zmiennej:
``` DECLARE <TYP> <NAZWA> <WARTOŚĆ>;``` na przykład:
```
DECLARE INTEGER x 10;
```

- przypisanie do zmiennej: ``` ASSIGN <NAZWA> <WARTOŚĆ/NAZWA/WYRAŻENIE>;``` na przykład:
    - wartość
    ```
    ASSIGN x 12;
    ```
    - nazwa
    ```
    ASSIGN x y;
    ```
    - wyrażenie
    ```
    ASSIGN x [x * 2];
    ```

- wczytanie do zmiennej: ``` READ_TO <NAZWA>;``` na przykład:
```
READ_TO x;
```
- wypisanie zmiennej: ``` SELECT <NAZWA>;``` na przykład:
```
SELECT x;
```

----

Wyrażenia w języku Huginn są to podstawowe operacje matematyczne:
- dodawnie,
- odejmowanie,
- mnożenie,
- dzielenie.

Składnikami operacji mogą być tylko elementy tego samego typu.
Przykład:
```
// Dodawanie
DECLARE INTEGER sum_result 0;
DECLARE INTEGER x 1;
DECLARE INTEGER y 2;
ASSIGN sum_result [x + y];

// Odejmowanie
DECLARE INTEGER sub_result 0;
DECLARE INTEGER z 2;
ASSIGN sub_result [z - 1];

// Mnożenie
DECLARE INTEGER mul_result 0;
ASSIGN mul_result [5 * 6];

// Dzielenie
DECLARE REAL mul_result 0;
ASSIGN REAL a 4.0;
ASSIGN mul_result [6.0 / a];
```

----

Instrukcja warunkowa IF:
```
IF <NAZWA/WARTOŚĆ> [
    <OPERACJE>
];
```

Blok wyznaczony nawiasami kwadratowymi zostanie wykonany tylko wtedy, gdy wartość będzie *true* lub gdy zmienna podana jako warunek będzie miała wartość *true*.

Bloki IF mogą być zagnieżdżane.

----

Funkcje definiujemy w następujący sposób (przykład):
```
funkcja_param(INTEGER q | REAL r |)[
    SELECT q;
    SELECT r;
    ASSIGN q 21;
    ASSIGN r [r * r];
];
```
a wywołujemy:
```
CALL funkcja_param(xx | yy|);
```

Zmienne przekazywane jako parametry są typu IN/OUT.

----

Kontekst zmiennych: zmienne definiowane dostępne są we wrzystkich blokach poniżej (np. blokach warunkowych if).
Zmienne nie są dostępne w blokach będących rodzicem.