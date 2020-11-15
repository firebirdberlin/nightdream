[< zurück](../index.md)

# Häufig gestellte Fragen

[> english](faq_en.md)


## Wo finde ich Hilfe ?

Wenn Ihre Frage hier nicht beantwortet wird, dann stellen Sie diese bitte auf
der öffentlichen Mailingliste <MAILTO: night-clock@googlegroups.com>:
https://groups.google.com/d/forum/night-clock.


## Inhaltsverzeichnis

 - [Werbung](#werbung)
 - [In-App Käufe](#in-app-käufe)
 - [Benutzeroberfläche](#benutzeroberfläche)
 - [Wetter](#wetter)
 - [Web Radio](#web-radio)
 - [Autostart](#autostart)
 - [Wecker](#wecker)
 - [Nachtmodus](#nachtmodus)
 - [Helligkeitseinstellungen](#helligkeitseinstellungen)
 - [Widget](#widget)
 - [Slideshow](#slideshow)

## Werbung

### Wie kann ich die Werbung abschalten ?

Nachtuhr zeigt keine Werbung - weder in der kostenlosen noch in der kostenpflichtigen Version.
Falls Sie irgendwelche Werbung sehen, überprüfen Sie bitte andere Apps auf Ihrem Gerät. 
Ich möchte nicht zu einer Welt beitragen, die in allen möglichen Lebenssituationen voll mit Werbung ist.

## In-App Käufe

### Welche Pakete kann ich kaufen ?

#### Vollversion

Schaltet alle bezahlten Funktionen auf einmal frei. Die Vollversion ist 
verfügbar, solange kein Einzelpaket erworben wurde.

#### Wetter- und Designpaket

Fügt Wetterinformationen hinzu (als Statuszeile und als Wettervorschau)

Nur ein analoges und ein digitales Uhrendesign sind in der freien Version 
verfügbar. Das Wetter- und Designpaket schaltet weitere analoge Designs frei, 
die über einen eingebauten Editor kreativ verändert werden können. 

#### Internetradio

Diese Paket erlaubt das Abspielen von Radiostreams. Ebenso können eigene Töne 
oder Radiostreams zum Wecken verwendet werden.

#### Fähigkeiten

Fügt zusätzliche Fähigkeiten hinzu:
 - Always On: Start der App ohne eine angeschlossene Stromquelle.
 - Programmierter Autostart: Start der App zu einer voreingestellten Uhrzeit 
   (ohne einen Alarm stellen zu müssen)
 - Autostart bei neuen Benachrichtigungen

#### Spende

Um Ihre Anerkennung für diese App zu zeigen und zusätzlich zu unterstützen, 
können Sie eine Spende hinterlassen. 
Damit werden die bezahlten Features ebenfalls freigeschaltet. Als besondere 
Aufmerksamkeit gibt es eine exklusive Überraschung: ein Retro-Flipkarten Design.  

## Benutzeroberfläche

### Wie kann ich die Größe der Uhr ändern ?

Mit einer Zwei-Finger-Pinch-Geste können Sie die Größe des Uhr-Layouts frei ändern.

### Die Oberfläche ist gesperrt

Wenn Nachtuhr ein Vorhängeschlosssymbol in der oberen linken Ecke anstelle des Menü - Symbols 
(auch bekannt als Burger-Symbol) anzeigt, ist die Benutzeroberfläche  gesperrt. Um zu entsperren
drücken Sie einfach lange auf das Schlosssymbol. Umgekehrt ist die Benutzeroberfläche durch 
langes Drücken des Menüsymbols gesperrt.

Im Standby-Modus startet die Benutzeroberfläche immer im gesperrten Modus.

### AM / PM-Anzeige funktioniert nicht richtig

Die Schriftart 7 Segment Digital bildet Großbuchstaben nicht gut ab. 
Verwenden Sie alternativ eine andere Schriftart, z.B. DSEG 14CLASSIC.   

### Benutzerdefinierte Schriftarten

Es können .ttf- und .otf-Dateien als auch .zip-Archive die mehrere Schriftdateien enthalten ausgewählt werden. 
Navigieren Sie einfach zu Ihrem Download-Ordner und wählen Sie eine Datei, die Sie zuvor heruntergeladen haben.
Quellen für kostenlose Schriftarten im Web sind z.B.:
[fonts.google.com](https://fonts.google.com/) und
[Font Squirrel](https://www.fontsquirrel.com/).

### Die Querformatausrichtung (Landscape) funktioniert nicht

Ab Android 5 haben Screensaver / Bildschirmschoner den Fehler, dass sich die Bildschirmausrichtung sofort zum Hochformat
ändert, sobald die Bildschirmsperre aktiviert ist.
* Lösung 1: Deaktivieren Sie den Screensaver vollständig (Systemeinstellungen> Anzeige> Screensaver). Stattdessen 
können Sie die Autostart-Funktion der App nach Ihren Wünschen einrichten.
* Lösung 2: Versuchen Sie, "Einstellungen> Darstellung> Automatische Drehung im Screensaver erzwingen" zu aktivieren.

## Wetter

Screenshot                                      | Beschreibung
------------------------------------------------|-----------------------
![Preferences](weather_status_300.png)          | Wetter - Statuszeile
![Preferences](weather_settings_300.png)        | Wetter - Einstellungen
![Preferences](weather_location_search_300.png) | Ortsuche
![Preferences](weather_forecast_300.png)        | 5-Tage Wetter - Vorschau

### Wetter - Statuszeile

Zeigt die aktuelle Temperatur und optional die Windrichtung und Windgeschwindigkeit an.
Die Wetterdaten werden ungefähr einmal pro Stunde aktualisiert, wenn sich die App im 
Vordergrund befindet.

### Weather - Einstellungen

Ermöglicht die Konfiguration der in der Statuszeile angezeigten Informationen. 
Wenn Sie den Ort leer lassen, benötigt die App die Berechtigung zum Lesen Ihres aktuellen (grob)
Standorts.

Die Wetterdaten werden bereitgestellt durch: [OpenWeather](http://www.openweathermap.org).
Der Anbieter [Dark Sky](http://www.darksky.net) wird seinen Service Ende des Jahres 2021 einstellen.

### Ortsuche

Sie können selbst nach einem Ort suchen. In diesem Fall können Sie die 
Berechtigung zum Lesen Ihres Standorts widerrufen.

### 5-Tage Wetter - Vorschau

Die Überschrift zeigt den aktuellen Standort, was besonders nützlich ist, falls dieser 
automatisch erkannt wurde.

Die folgenden Informationen werden angezeigt
  - Temperatur (gefühlte Temperatur)
  - Windrichtung und Windgeschwindigkeit
  - Bedeckungsgrad mit Wolken in *% *
  -  rel. Luftfeuchtigkeit in *% *
  - 3 Stunden Regenvolumen in * mm * (nur wenn Regen erwartet wird)

### Wetterdaten werden nicht angezeigt

Wetterdaten werden möglicherweise aus mehreren Gründen nicht angezeigt.
* Wetterdaten werden nicht angezeigt, wenn diese mehr als 8 Stunden veraltet sind (z. B. im Flug
   Modus).
* Der aktuelle Standort kann nicht abgerufen werden:
   - Können Wetterinformationen abgerufen werden, wenn Sie den Standort manuell eingeben?
   - Bitte erteilen Sie die Berechtigung für den Zugriff auf den Standort (Android 6+)
   - Überprüfen Sie, ob Ihre Ortungsdienste aktiviert sind (z. B. Batteriesparen
     oder hohe Genauigkeit)
* Das Netzwerk ist möglicherweise nicht verbunden. Überprüfen Sie Ihre Netzwerkverbindung.

## Web Radio

### So spielen Sie einen Radiostream ab

Das Webradio-Bedienfeld wird angezeigt, indem Sie in der Seitenleiste auf das Radiosymbol tippen.

Klicken Sie auf die Schaltfläche "+", um dem Bedienfeld einen neuen Radiosender hinzuzufügen. Sie können nach Radiosendern suchen oder
geben Sie eine URL ein, die Sie kennen. Dies kann eine einfache MP3-Stream-URL sein, aber auch eine m3u / pls-Wiedergabeliste.

Beenden Sie die Wiedergabe eines Radiosenders, indem Sie die Taste erneut drücken.

### So entfernen oder bearbeiten Sie einen gespeicherten Radiosender

Tippen Sie einfach etwas länger auf einen gespeicherten Radiosender ("langes Drücken"), um das Konfigurationsmenu 
der Radiostreams zu öffnen. Dort können Sie den Radiostream für diese Schaltfläche ändern oder entfernen.

## Autostart

Für den Autostart gibt es zwei Möglichkeiten: Autstart und Always On

### Autostart

Der Autostart wird aktiv, wenn das Gerät mit einer zuvor definierten 
Stromquelle verbunden wird (Ladegerät, USB, kabelloses, Ladegerät oder Dock).
Wird die Nachtuhr unterbrochen, dann startet sie sich neu, sobald das Gerät 
in den Standby-Modus wechselt, z.B. wenn der Bildschirm aus geht.

Der Zeitraum, in dem der Autostart aktiv ist, kann definiert werden. Sind zum 
Startzeitpunkt alle Bedingungen erfüllt, wird die Nachtuhr dann gestartet. 
Am Ende des Zeitraumes wird die App entsprechend beendet, wenn die 
entsprechende Option aktiviert (Automatisch beenden > am Ende des 
Autostartzeitraumes) ist.

### Always On

Always on ergänzt den Autostart im Fall, dass keine Stromquelle verbunden ist. 
Die App wird dann gestartet, wenn das Gerät in den Standby wechselt. Es kann 
ein Zeitraum definiert werden, in dem die Funktion aktiv ist. Unter einem 
einstellbaren Ladestand des Akkus ist die Funktion abgeschaltet, um den Akku 
nicht zu schnell zu entladen. Ist die Funktion "mit Bildschirm nach oben" 
aktiviert, dann muss das Gerät auf einen Tisch liegen, damit die "Always On" 
Funktion aktiviert wird.

Ich empfehle ergänzend die Funktion Automatisch beenden > Nach Timeout (im 
Akkubetrieb) zu verwenden, denn ein aktiviertes Display entlädt den Akku 
schnell. 

## Wecker

### Warum funktioniert die Aktivierung des WLANs für den Radiowecker nicht ? 

Neuere Versionen von Android verbieten es leider, WLAN automatisch zu 
aktivieren, wenn sich das Gerät im Flugmodus befindet. Wenn sie das 
Internetradio als Wecker verwenden möchten, dann bleibt nur die Option auf den 
Flugmodus zu verzichten. Statt dessen könnten Sie Bluetooth, WLAN und 
Datenverbindungen manuell deaktivieren. 

### Ich bin vor dem Wecker aufgewacht und möchte heute nicht mehr geweckt werden.

Etwa eine Stunde vor der eigentlichen Weckzeit wird eine Benachrichtigung 
angezeigt. Um einen wiederholenden Wecker einmalig zu deaktivieren, können 
Sie in dieser Benachrichtigung AUSLASSEN auswählen. Damit wird die nächste 
Weckzeit übersprungen und der Wecker bleibt für den folgenden Wecktag aktiv. 

## Nachtmodus

Nachtuhr verfügt über zwei Betriebsarten: Tagesmodus und Nachtmodus.

Der Nachtmodus verfügt über folgende Funktionen:
* Sie können verschiedene Farben einstellen.
* Es gibt keine automatische Helligkeitsregelung. Die Anzeigehelligkeit ist sogar auf einen festen Wert eingestellt,
   wenn die automatische Helligkeitsfunktion aktiviert ist.
* Das Display ist möglicherweise ausgeschaltet. Das Display wird wieder eingeschaltet, wenn die Leuchtkraft einen 
   vordefinierter Wert (Standard: 20 Lux) überschreitet. Wenn die Umgebungsgeräuscherkennung aktiviert ist, 
   wird auch ein Geräusch den Bildschirm aktivieren. Ein Geräusch kann auch die Helligkeit des Displays in
   beiden Modi leicht erhöhen.
* Der Nachtmodus kann automatisch aktiviert werden, sobald es dunkel wird. 
   Andere Aktivierungsmodi sind * geplant * und * manuell *. Diese sind
   für Geräte ohne Lichtsensor gedacht. Wenn Sie von der automatischen Helligkeitsregelung profitieren möchten, 
   sollten Sie die Aktivierungseinstellungen für den Nachtmodus auf * automatisch * lassen.

## Helligkeitseinstellungen

### Manueller Helligkeitsmodus

Wenn Sie am oberen Rand des Displays wischen, können Sie die Helligkeit einstellen. Die App merkt sich den 
aktuellen Helligkeitswert im Nachtmodus, der sich von der Einstellung im Tagmodus unterscheidet. 
Wenn sich der Modus ändert, kann sich die Helligkeit erhöhen / verringern.
Während des Betriebs mit Batterie darf die Helligkeit niemals größer als der maximal
unter * Einstellungen> Aussehen> Maximale Bildschirmhelligkeit im Akkubetrieb * definierte Helligkeitswert sein.

### Automatischer Helligkeitsmodus

Im automatischen Helligkeitsmodus stellt das Gerät die Displayhelligkeit abhängig vom Umgebungslicht ein.
Die Helligkeit wird automatisch innerhalb der vordefinierten Grenzen der * Mindesthelligkeit * und 
der * maximalen Helligkeit * eingestellt. Wenn das Gerät im Akkubetrieb ist, wird ein anderer Wert für die maximale Helligkeit
benutzt. Die * maximale Helligkeit des Akkus * sollte zum Energiesparen so weit wie möglich reduziert werden, da
das Display den größten Teil der Energie verbraucht.

Änderungen der Umgebungslichtbedingungen sollten nach ca. 20 s wirksam werden.

Der * Helligkeits - Offset * hilft, die automatische Helligkeitsregelung an Ihre persönlichen Bedürfnisse anzupassen.
Die Art des Displays (z.B. LCD oder AMOLED) und die auf dem Display angezeigten Farben haben einen starken Einfluss darauf, 
wie das Bild aussieht. Mit dem Offset können Sie die Helligkeit so einstellen, dass sie niedriger oder heller 
als der Standardwert * 0 * ist. Bei schlechten Lichtverhältnissen kann ein anderer Offset - Wert erforderlich sein als in einer 
hellen Umgebung. Der Offset - Wert kann von der Hauptansicht aus eingestellt werden, indem Sie mit Ihrem Finger über den oberen Rand 
Ihres Displays wischen.

Da die meisten Geräte keinen genauen Werte unter 10 Lux melden, ist bei schlechten Lichtverhältnissen 
die automatische Helligkeitsregelung möglicherweise nicht sehr genau.

## Widget

### Das Uhr-Widget wird nicht aktualisiert

Das Uhr-Widget wird einmal pro Minute aktualisiert. 
Aufgrund von Einschränkungen des Android-Systems wird ein Vordergrunddienst benötigt (der gleiche Dienst verwaltet den Autostart der App). 
Dieses wird durch eine permanente Benachrichtigung im Benachrichtigungscenter angezeigt.
Falls dieser Dienst nicht ordnungsgemäß ausgeführt wird (oder vom Android-System unterbrochen wird), 
wird das Widget nicht mehr aktualisiert. Wenn Sie diesen Dienst (wieder) aktivieren möchten, können Sie den Autostart 
der App deaktivieren und wieder aktivieren. Dies löst den Start des Dienstes erneut aus.

## Slideshow

### Welche Bilder werden in der Slideshow angezeigt

Die App zeigt alle Bilder im JPG - Format an, die mit der Kamera aufgenommen wurden bzw. im Foto-Verzeichnis liegen. 
Videos oder Bilder in einem anderen Format werden nicht angezeigt.
Aktuell ist es nicht möglich eine Auswahl einzelner Bilder für die Slideshow einzustellen.

### Es wird nur ein schwarzer Bildschirm angezeigt

Bitte prüfen Sie, ob auf dem Gerät Bilder im JPG - Format vorliegen.
Möglicherweise ist die Option *Ausblenden - Das Hintergrundbild soll im Nachtmodus ausgeblendet werden* aktiv. 
Im Nachtmodus wird dann nur ein schwarzer Hintergrund statt der Diashow angezeigt.
