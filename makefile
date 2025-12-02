# Get the version from build.gradle, assuming a line like: version = "x.y.z"
VERSION := $(shell grep -m 1 'versionName "' app/build.gradle  | sed -e 's/.*versionName "\(.*\)"/\1/')
TAG := $(VERSION)
BRANCH := $(shell git rev-parse --abbrev-ref HEAD)

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


.PHONY: publish

publish:
	@if [ "$(BRANCH)" != "master" ]; then \
		echo "Error: Publishing is only allowed from the master branch."; \
		exit 1; \
	fi
	@echo "Version from build.gradle: $(VERSION)"
	@echo "Git tag to be created: $(TAG)"
	@echo "Current branch: $(BRANCH)"
	@# Check if the tag already exists locally
	@if git rev-parse $(TAG) >/dev/null 2>&1; then \
		echo "Tag $(TAG) already exists. Nothing to do."; \
	else \
		echo "Pushing current branch to origin..."; \
		git push origin $(BRANCH); \
		echo "Tag $(TAG) does not exist. Creating and pushing tag."; \
		git tag -a $(TAG) -m "Release $(VERSION)"; \
		git push origin $(TAG); \
		echo "Tag $(TAG) created and pushed to origin."; \
	fi