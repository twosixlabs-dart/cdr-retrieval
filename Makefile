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

docker-login:
	docker login -u ${DOCKER_HUB_USERNAME} -p ${DOCKER_HUB_PASSWORD}

docker-logout:
	docker logout

docker-build:
	sbt clean assembly
	docker build -t $(IMG):$(APP_VERSION) .

docker-push: docker-login docker-build docker-logout
	docker push $(IMG):$(APP_VERSION)
