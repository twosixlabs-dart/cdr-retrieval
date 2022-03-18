IMAGE_PREFIX = twosixlabsdart/
IMAGE_NAME = cdr-retrieval
IMG := $(IMAGE_PREFIX)$(IMAGE_NAME)

ifndef GITHUB_REF_NAME
	APP_VERSION := "latest"
else ifeq ("$(GITHUB_REF_NAME)", "master")
	APP_VERSION := "latest"
else ifeq ("$(GITHUB_REF_TYPE)", "tag")
	APP_VERSION := $(shell cat version.sbt | cut -d\" -f2 | cut -d '-' -f1)
else
	APP_VERSION := $(GITHUB_REF_NAME)
endif

docker-build:
	sbt clean assembly
	docker build -t $(IMG):$(APP_VERSION) .

docker-push: docker-build
	docker push $(IMG):$(APP_VERSION)