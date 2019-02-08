# S3 Maven Wagon Provider

An Apache Maven Wagon provider for Amazon S3 that deploys artifacts to S3 bucket.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/17aec7c14fd647b6b48f6f9b12165a63)](https://app.codacy.com/app/ikelin/s3-maven-wagon-provider?utm_source=github.com&utm_medium=referral&utm_content=ikelin/s3-maven-wagon-provider&utm_campaign=Badge_Grade_Dashboard)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ikelin/s3-maven-wagon-provider/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ikelin/s3-maven-wagon-provider)
[![Build Status](https://travis-ci.org/ikelin/s3-maven-wagon-provider.svg?branch=master)](https://travis-ci.org/ikelin/s3-maven-wagon-provider)
[![Coverage Status](https://coveralls.io/repos/github/ikelin/s3-maven-wagon-provider/badge.svg?branch=master)](https://coveralls.io/github/ikelin/s3-maven-wagon-provider?branch=master)

## Usage

Maven

```xml
<build>
  <extensions>
    <extension>
      <groupId>com.ikelin</groupId>
      <artifactId>s3-maven-wagon-provider</artifactId>
      <version>{VERSION}</version>
    </extension>
  </extensions>
</build>

<distributionManagement>
  <snapshotRepository>
    <id>aws-s3-snapshot</id>
    <name>AWS S3 Snapshot Repository</name>
    <url>s3://{BUCKET}/snapshot</url>
  </snapshotRepository>
  <repository>
      <id>aws-s3-release</id>
      <name>AWS S3 Release Repository</name>
      <url>s3://{BUCKET}/release</url>
    </repository>
</distributionManagement>

```

Supply AWS credentials using ways from [Working with AWS Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).
