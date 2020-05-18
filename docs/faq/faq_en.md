[< back](../index.md)

# Frequently Asked Questions (FAQ)

[> deutsch](faq_de.md)

## Table of Contents

- [Ads](#ads)
- [In-App payments](#in-app-payments)
- [Alarms](#alarms)
- [Autostart](#autostart)
- [Night mode](#night-mode)
- [Brightness settings](#brightness-settings)
- [Web Radio](#web-radio)
- [Misc](#misc)

## Where do I find support ?

If your question is not listed here, please try to post it to the public mailing list at
https://groups.google.com/d/forum/night-clock.

## Ads

### How do I remove the ads ?

Night Clock does not show any advertisements, neither in the free version nor in the paid versions. 
If you observe any ads check the other apps on your device. I don't want to contribute to a world 
which is full of ads in all possible situations of life.

[Top](#Table-of-Contents)

## In-App payments

### Which payment options do I have ?

There a different packages available.

#### Full version

Gives access to all paid features which are described below.
The full version is only available as long as you didn't purchase one
of the packages.

#### Weather and design package

Adds weather information a a status line below the clock in the main
view as well as in the widget.

Only one digital clock layout and one analog clock layout can be accessed
in the free version. The package adds new analog clock faces. Which can
freely be modified using an integrated designer.

#### Web radio package

Adds the ability to play web radio streams from the main interface.
Custom sounds or radio streams can be used as alarm clock.

#### Abilities

Adds additional abilities to the app.
- Always On: Start the app automatically without connecting a power
  source.
- Scheduled Autostart: Start the app at a certain time (without
  having to setup an alarm)
- Notifications: start the app when new notifications show up.

#### Donation

If you like to send a special appreciation for the app you can choose a
donation. I will grant access to the paid features. As a special gift
the donation unlocks a retro flip card design.

[Top](#Table-of-Contents)

## Alarms

### How do I stop or snooze an alarm ?

An alarm is stopped by tapping the lower right corner. Snooze is enabled by tapping anywhere else.
The snooze time is adjustbable in preferences > alarms.

Upcoming alarms can be stopped from the notification area. The notification is shown one hour
before the alarm time.

## Custom mp3 as alarm tone

In the dialog for setting the alarm tone you can choose your custom MP3 file, at the bottom of the 
dialog. Just navigate to your music folder and select the file you like. With the next release this 
dialog will move to the alarm overview allowing you to set different alarm tones per alarm.

[Top](#Table-of-Contents)

## Autostart

The app has different options for auto starting. If you enable the autostart it starts as soon as
it gets connected to a power source. If the app is interupted it waits until the device goes back
into standby, i.e. the screen turns off and restarts the night clock. The "always on" feature is
starting the app while the device is not charging. In order to save some battery I advise to stop
the app after some minutes (Preferences > Autostart > Stop automatically > on battery timeout).

[Top](#Table-of-Contents)

## Night mode

Night Clock has two modes of operation: day mode and night mode.

The night mode has the following features:
* You can set different colors.
* There is no automatic brightness control. The display brightness is set to a fixed value, even
  if the automatic brightness feature is enabled.
* The display may be switched off. The display is switched back on if the luminosity exceeds a
  predefined value (default: 20 lux). If the ambient noise detection is activated, then noise will
  also re-activate the screen. Noise may also slightly increase the brightness of the display in
  both of the modes.
* The night mode can be activated automatically as soon as it gets dark (and silent if ambient
  noise detection is active). Other modes of activation are *scheduled* and *manual*. These are
  meant for devices which do not have a light sensor. If you want to benefit from the automatic
  brightness control you should leave the night mode activation settings at *automatic*.

[Top](#Table-of-Contents)

## Brightness settings

### Manual brightness mode

Sliding along the upper rim of the display you can tune its brightness. The app remembers the
current brightness value in night mode which is different from the setting in day mode. Thus,
when the mode changes the brightness may increase/ decrease.

While operating on battery the brightness may never be larger than the maximum brightness value
defined at *Preferences > Appearance > Maximum screen brightness on battery*

### Automatic brightness mode

In the automatic brightness mode the device sets the display brightness depending on the luminosity.
The brightness is automatically set within the pre-defined bounds of the *minimum brightness* and
the *maximum brightness*. If the device is on battery another value of the maximum brightness is
used.  The *maximum brightness on battery* should be reduced as much as possible as the display
consumes most of the energy.

Changes in the ambient light conditions should take effect after approximately 20s.

The *brightness offset* helps to adjust the automatic brightness control to your personal needs.
The type of the display, eg. LCD or AMOLED, and the colors shown on the display, have a strong
influence on how the display looks like. With the offset you can tune the brightness to be lower
or brighter than the default value *0*. Because in low light conditions another offset is needed
than in a bright environment, the offset can be tuned from the main view by sliding your finger
along the upper rim of your display.

In low light conditions the automatic brightness control may not be very accurate. This is due to
the fact that most of the devices do not report accurate values below 10 lux.

[Top](#Table-of-Contents)

## Web Radio

### How to play a radio stream

The web radio panel appears by tapping the radio icon from the sidebar.

Tab the "+" button to add a new radio station to the panel. You can search for radio stations or
enter a url you know. This can be a plain mp3 stream url but also m3u/pls playlists.

Stop playing a radio station by pressing its button again.

### How to remove or edit a radio station button

Just tap on a radio station button a bit longer ("long press") to open the radio streams
configuration window. There you can alter the radio stream for that button or remove it.

[Top](#Table-of-Contents)

## Misc

### Custom fonts

You can choose plain .ttf and .otf files, but also .zip archives which may contain multiple font
files. Just navigate to your download folder and choose a file you downloaded before. 
There are sources of free fonts in the web, such as [fonts.google.com](https://fonts.google.com/) 
and [Font Squirrel](https://www.fontsquirrel.com/).

### AM/PM indicator not working properly

The seven segment font is not able to draw uppercase letters very precisely. An alternative font is 
a 14 segments digital font as found at https://www.keshikan.net/fonts-e.html.

### The landscape orientation does not work

Starting from Android 5 Daydreams have a bug. The screen orientation changes to portrait as soon 
as the screen lock is activated.
* Solution 1: Disable Daydreams completely (System Settings > Display > Daydream). Instead you 
can setup the auto start feature of the app to your needs.
* Solution 2: Try to enable Settings > Appearance > Force auto rotation in DayDream.

### Weather data is not shown

Weather data usually are updated once within 2 hours.

Weather data may not be shown for several reasons.
* They are not displayed if they are outdated for more than 8 hours.
* The current location cannot be retrieved.
  - Please grant permissions for accessing the location (Android 6+)
  - Please check if your location services are activated (e.g. battery saving, high accuracy)
* The network may not be connected. Check your network connection.

### The clock widget is not updated

The clock widget needs to be updated once per minute. Due to restrictions of the Android system
this is a difficult task. In order to ensure that the update works a foreground service is needed. 
The same service is managing the autostart of the app. It is indicated by showing a permanent 
notification in the notification area. If this service is not properly running (or interrupted by 
the android system) the widget is no longer updated. If you want to (re-) enable this service you 
can disable and re-enable the autostart of the app. This triggers the start of the service. 

### The interface is locked

If Night Clock shows a lock symbol in the top left corner instead of the menu icon (aka burger icon)
the user interface is locked. In order to unlock simply long press the lock icon. Vice versa the
user interface is locked by long pressing the menu icon.

If the standby mode is enabled, the user interface always starts in locked mode.

[Top](#Table-of-Contents)