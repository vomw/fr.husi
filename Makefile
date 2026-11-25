GOROOT = $(shell go env GOROOT)
GO_PATCH_1230 = "https://github.com/golang/go/commit/76a8409eb81eda553363783dcdd9d6224368ae0e.patch"
GO_PATCH_1234 = "https://github.com/golang/go/commit/59b7d40774b29bd1da1aa624f13233111aff4ad2.patch"
CLIP = sh -c 'if [ -n "$$WAYLAND_DISPLAY" ]; then exec wl-copy; \
              elif [ -n "$$DISPLAY" ]; then exec xclip -selection clipboard; \
              else echo "No display detected (WAYLAND_DISPLAY/DISPLAY missing)"; exit 1; fi'

.PHONY: build libcore_android aboutlibraries aboutlibraries_go aboutlibraries_android apk apk_debug assets icon proto proto_install test test_gradle test_go lint_android plugin generate_option lint_go lint_go_android lint_go_install fmt_go fmt_go_install patch_go1230 patch_go1234

build: libcore_android assets apk

libcore_android:
	./run lib core --android

aboutlibraries: aboutlibraries_go aboutlibraries_android

aboutlibraries_go:
	cd libcore && go run ./cmd/internal/licencecollect -d ../composeApp/src/commonMain/aboutlibraries/libraries -clean

aboutlibraries_android:
	./gradlew :composeApp:exportLibraryDefinitions

apk:
	BUILD_PLUGIN=none ./gradlew androidApp:assembleFossRelease

apk_debug:
	BUILD_PLUGIN=none ./gradlew androidApp:assembleFossDebug

assets:
	./run lib assets

icon:
	./run icon

proto:
	./run proto

proto_install:
	@echo "protoc: install via your package manager (e.g. pacman -S protobuf)."
	@echo "Go plugins are built from libcore/go.mod pins automatically."

lint_go: lint_go_android

lint_go_android:
	cd libcore/ && GOOS=android GOARCH=arm64 golangci-lint run ./...

lint_go_install:
	go install -v github.com/golangci/golangci-lint/v2/cmd/golangci-lint@latest

fmt_go:
	cd libcore/ && golangci-lint fmt ./...

fmt_go_install: lint_go_install

test_go:
	cd libcore/ && go test -v -count=1 -tags with_quic,badlinkname -ldflags=-checklinkname=0 ./...

test: test_gradle test_go

test_gradle:
	BUILD_PLUGIN=none ./gradlew :composeApp:allTests

lint_android:
	BUILD_PLUGIN=none ./gradlew :composeApp:lintAndroidMain

plugin:
	BUILD_PLUGIN=$(PLUGIN) ./gradlew :plugin:$(PLUGIN):assembleFossRelease

generate_option:
	cd ./libcore/cmd/internal/boxoption && go run . | $(CLIP)

patch_go1230:
	curl $(GO_PATCH_1230) | sudo patch --verbose -p 1 -d $(GOROOT)

patch_go1234:
	curl $(GO_PATCH_1234) | sudo patch --verbose -p 1 -d $(GOROOT)
