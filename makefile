ctags:
	ctags -R .

debug:
	./gradlew assembleDebug

doze:
	adb shell dumpsys deviceidle force-idle

clean:
	find . -name "*.sw*" -exec rm {} \;
	./gradlew clean

gitclean:
	git branch -a --merged remotes/origin/master | grep -v master | grep "remotes/origin/" | cut -d "/" -f 3 | xargs -n 1 git push --delete origin

revoke-permissions:
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.READ_EXTERNAL_STORAGE
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.RECORD_AUDIO
	adb shell pm revoke com.firebirdberlin.nightdream android.permission.ACCESS_COARSE_LOCATION

screenshot:
	adb shell screencap -p | perl -pe 's/\x0D\x0A/\x0A/g' > screen.png

test:
	./gradlew test

strings:
	rm -rf tmp-strings
	mkdir tmp-strings
	rsync -av  res/ tmp-strings/ --prune-empty-dirs --include="*/" --include="strings.xml" --exclude="*"