# CDR Retrieval

CDR retrieval REST API service

[![build and publish](https://github.com/twosixlabs-dart/cdr-retrieval/actions/workflows/build-and-publish.yml/badge.svg)](https://github.com/twosixlabs-dart/cdr-retrieval/actions/workflows/build-and-publish.yml)


## Build

This application is the primary document ingestion pipeline for the DART platform. Documents are ingested via a Kafka topic and then sent through several processing stages. This processing includes checks for duplicated documents in the corpus as well as normalizing and reparing common text extraction errors.

After the initial ingestion, the pipeline sends the document through the annotation pipeline. Annotators enrich the document with additional natural language metadata extracted from the text. Examples of this include Named Entity Recognition (NER), topic modeling, and sentiment analysis. There are a number of pre-built annotators originally developed by Qntfy, and it is also possible to implement your own annotator.

## Building
This project is built using SBT. For more information on installation and configuration of SBT please [see their documentation](https://www.scala-sbt.org/1.x/docs/)

To build and test the code:
```bash
sbt clean test
````

To create a runnable JAR:
```bash
sbt clean assembly
```

To create a Docker image of the runnable application:
```bash
make docker-build
```


## Configuration

Configuration is defined in `src/main/resources/application.conf`. Most properties can be overridden 
by environment variables:

| Name	                       | Description	                                                                 | Example Values                                   |
|-----------------------------|------------------------------------------------------------------------------|--------------------------------------------------|
| CDR_RETRIEVAL_PORT          | Port where CDR REST service will be served                                   | `8090` (default)                                 |
| PERSISTENCE_MODE            | Retrieve docs from local filesystem or AWS                                   | `local` (default) / `aws`                        |
| PERSISTENCE_DIR             | Document persistence directory for local persistence                         | `data` (default)                                 |
| RAW_DOCUMENTS_BUCKET_NAME   | S3 bucket name for AWS persistence mode                                      | `bucket-name` (no default)                       | 
| CREDENTIALS_PROVIDER        | AWS credentials provider for AWS persistence mode                            | `INSTANCE` / `ENVIRONMENT` / `SYSTEM` (default)  |
| ARANGODB_DATABASE           | Database Name for Arango CDR datastore                                       | `dart` (default)                                 |
| ARANGODB_HOST               | Hostname or IP of Arango database instance                                   | `localhost` (default) / `dart-arangodb`          |
| ARANGODB_PORT               | Arango database port                                                         | `8529` (default)                                 |
| DART_AUTH_SECRET            | Auth token secret for keycloak integration                                   | `xxyyzz` (no default)                            |
| DART_AUTH_BYPASS            | If true do not use tokens to authenticate/authorize                          | `"true"` or `"false"`                            |
| DART_AUTH_BASIC_CREDENTIALS | Use these credentials for basic auth authentication if DART_AUTH_BYPASS=true | `user1:pass1,user2:pass2`                        |

## Funding
This software was developed with funding from the following sources.

| Agency | Program(s)                         | Grant #          |
|--------|------------------------------------|------------------|
| DARPA  | Causal Exploration, World Modelers | W911NF-19-C-0080 |
