all:
	ant -f Pro_build.xml release
	make install OPT="$(OPT)"

debug:
	ant -f Pro_build.xml debug

doze:
	adb shell dumpsys deviceidle force-idle

install:
	adb $(OPT) install -r bin/Pro/NightDream-release.apk

installemulator:
	adb -e install -r bin/Pro/NightDream-release.apk

installdebug:
	adb $(OPT) install -r bin/Pro/NightDream-debug.apk

installdebugemulator:
	adb -e install -r bin/Pro/NightDream-debug.apk

uninstall:
	adb $(OPT) -d uninstall com.firebirdberlin.nightdream

clean:
	ant -f Pro_build.xml clean
	find . -name "*.sw*" -exec rm {} \;

clear-data:
	adb $(OPT) shell pm clear com.firebirdberlin.nightdream

start:
	adb $(OPT) shell am start -n com.firebirdberlin.nightdream/com.firebirdberlin.nightdream.NightDreamActivity

stop:
	adb $(OPT) shell am force-stop com.firebirdberlin.nightdream

revoke-permissions:
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.READ_EXTERNAL_STORAGE
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.RECORD_AUDIO
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.ACCESS_FINE_LOCATION
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.ACCESS_COARSE_LOCATION

screenshot:
	adb shell screencap -p | perl -pe 's/\x0D\x0A/\x0A/g' > screen.png
