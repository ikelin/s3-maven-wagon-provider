# S3 Maven Wagon Provider

A Maven extension that hosts artifacts on Amazon S3.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ikelin/s3-maven-wagon-provider/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ikelin/s3-maven-wagon-provider)
[![Build Status](https://travis-ci.org/ikelin/s3-maven-wagon-provider.svg?branch=master)](https://travis-ci.org/ikelin/s3-maven-wagon-provider)
[![Coverage Status](https://coveralls.io/repos/github/ikelin/s3-maven-wagon-provider/badge.svg?branch=master)](https://coveralls.io/github/ikelin/s3-maven-wagon-provider?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c8cf3d856e754503b5f4ef53be95cfd9)](https://www.codacy.com/app/ikelin/s3-maven-wagon-provider?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ikelin/s3-maven-wagon-provider&amp;utm_campaign=Badge_Grade)

## Usage

Maven `pom.xml`:

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

<!-- deploys artifacts to S3 -->
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

<!-- downloads artifacts from S3 -->
<repositories>
  <repository>
    <id>aws-s3-snapshot</id>
    <url>s3://{BUCKET}/snapshot</url>
    <snapshots>
      <enabled>true</enabled>
      <updatePolicy>always</updatePolicy>
      <checksumPolicy>warn</checksumPolicy>
    </snapshots>
    <releases>
      <enabled>false</enabled>
    </releases>
  </repository>
  <repository>
    <id>aws-s3-release</id>
    <url>s3://{BUCKET}/release</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <releases>
      <enabled>true</enabled>
      <updatePolicy>never</updatePolicy>
      <checksumPolicy>fail</checksumPolicy>
    </releases>
  </repository>
</repositories>

```

Supply AWS credentials using ways from [Working with AWS Credentials](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).
