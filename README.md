bintray-maven-plugin
====================

[ ![Download](https://api.bintray.com/packages/devexperts/Maven/bintray-maven-plugin/images/download.svg) ](https://bintray.com/devexperts/Maven/bintray-maven-plugin/_latestVersion)

The plugin is primary used during *deploy* phase, to add your artifact(s) to a Bintray repository,
additionally to deploying in repositories from *distributionManagement* section.
This plugin works like **maven-deploy-plugin**.

Goals
-----
The Bintray deploy plugin has following goals.

### bintray:deploy ###
This goal is used to automatically install the artifact,
its pom and the attached artifacts produced by a particular project.

#### Parameters: #####

| Name                           | Type    | Description |
|:-------------------------------|:--------|:------------|
| **id**                         | String  | The id can be used to pick up the correct credentials from the *settings.xml*. <br> **User property:** bintray.repository.id.|
| **url**                        | String  | The location of maven repository in Bintray. <br> **User property:** bintray.repository.url. |
| **skip**                       | boolean | Set this to 'true' to bypass artifact deploy. <br> **Default value:** false. <br> **User property:** maven.deploy.skip. |
| **retryFailedDeploymentCount** | int     | Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10. <br> **Default value:** 1. <br> **User property:** retryFailedDeploymentCount. |

### bintray:publish ###
This goal is used to publish all artifacts in Bintray repository.

#### Parameters: #####

| Name                           | Type    | Description |
|:-------------------------------|:--------|:------------|
| **id**                         | String  | The id can be used to pick up the correct credentials from the *settings.xml*. <br> **User property:** bintray.repository.id.|
| **url**                        | String  | The location of maven repository in Bintray. <br> **User property:** bintray.repository.url. |
| **skip**                       | boolean | Set this to 'true' to bypass publishing. <br> **Default value:** false. <br> **User property:** bintray.publish.skip. |

Authentication
--------------
The plugin uses credentials from **settings.xml**.

Example
-------

### pom.xml ###

```xml
<plugin>
    <groupId>com.devexperts.bintray</groupId>
    <artifactId>bintray-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <id>bintray-REPO-deploy</id>
        <url>https://api.bintray.com/maven/SUBJECT/maven/REPO</url>
    </configuration>
    <executions>
        <execution>
            <id>bintray-deploy</id>
            <goals>
                <goal>deploy</goal>
                <goal>publish</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### settings.xml ###

```xml
<server>
    <id>bintray-REPO-deploy</id>
    <username>USER</username>
    <password>API_KEY</password>
</server>
```
