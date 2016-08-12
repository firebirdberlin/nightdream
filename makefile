all:
	ant -f Pro_build.xml release
	make install

debug:
	ant -f Pro_build.xml debug

install:
	adb $(OPT) install -r bin/Pro/NightDream-release.apk

installemulator:
	adb -e install -r bin/Pro/NightDream-release.apk

installdebug:
	adb install -r bin/Pro/NightDream-debug.apk

installdebugemulator:
	adb -e install -r bin/Pro/NightDream-debug.apk

uninstall:
	adb -d uninstall com.firebirdberlin.nightdream

clean:
	ant -f Pro_build.xml clean
	find . -name "*.sw*" -exec rm {} \;

clear-data:
	adb $(OPT) shell pm clear com.firebirdberlin.nightdream

start:
	adb shell am start -n com.firebirdberlin.nightdream/com.firebirdberlin.nightdream.NightDreamActivity

revoke-permissions:
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.READ_EXTERNAL_STORAGE
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.RECORD_AUDIO

screenshot:
	adb shell screencap -p | sed 's/\r$//' > screen.png
