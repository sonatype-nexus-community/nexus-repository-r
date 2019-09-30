<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2017-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Nexus Repository R Format

[![CircleCI](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-r.svg?style=svg)](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-r) [![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![DepShield Badge](https://depshield.sonatype.org/badges/sonatype-nexus-community/nexus-repository-r/depshield.svg)](https://depshield.github.io)
[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.nexus.plugins/nexus-repository-r.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.sonatype.nexus.plugins%22%20AND%20a:%22nexus-repository-r%22)

> **What's New** R is now part of Nexus Repository Manager. Version 3.20+ includes the R plugin by default. At the same time it is still open for community. 

> **Filing issues:** If using 3.20+ or later please file any issues at https://issues.sonatype.org/.

> **Upgrading to 3.20+:** After upgrading to 3.20+ your existing data will remain intact.
> We have added new features to the community version of the plugin such as:
* Search 
* Cleanup Policies 
* Tagging (PRO only)
* Moving between repositories (PRO only)
* Uploading R packages from UI to a hosted repository
* Support of the 'Repair - Reconcile component database from blobstore' task
* API for Repository creation via Groovy
* Routing Rule support

# Table Of Contents
* [Developing](#developing)
   * [Requirements](#requirements)
   * [Building](#building)
* [Using R with Nexus Repository Manager 3](#using-r-with-nexus-repository-manager-3)
* [Compatibility with Nexus Repository Manager 3 Versions](#compatibility-with-nexus-repository-manager-3-versions)
* [Installing the plugin](#installing-the-plugin)
   * [Permanent Reinstall](#permanent-reinstall)
   * [Easiest Install](#easiest-install)
* [The Fine Print](#the-fine-print)
* [Getting Help](#getting-help)

## Developing

### Contribution Guidelines

Go read [our contribution guidelines](/.github/CONTRIBUTING.md) to get a bit more familiar with how
we would like things to flow.

### Requirements

* [Apache Maven 3.3.3+](https://maven.apache.org/install.html)
* OpenJDK 8
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

Also, there is a good amount of information available at [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development+Overview)

### Building

To build the project and generate the bundle use Maven

    mvn clean install

If everything checks out, the bundle for `R` should be available in the `target` folder

## Using R With Nexus Repository Manager 3

Please view detailed instructions on [how to get started here!](https://help.sonatype.com/repomanager3/formats/r-repositories)

## Compatibility with Nexus Repository Manager 3 Versions

The table below outlines what version of Nexus Repository the plugin was built against:

| Plugin Version | Nexus Repository Version |
|----------------|--------------------------|
| v1.0.0         | <3.8.0-02                |
| v1.0.1         | >=3.8.0-02               |
| v1.0.2         | >=3.14.0-04              |
| v1.0.3         | >=3.15.2-01              |
| v1.0.4         | >=3.18.0-01              |
| v1.1.0 In product | >=3.20.0              |
All released versions can be found [here](https://github.com/sonatype-nexus-community/nexus-repository-r/releases).

## Installing the plugin
Starting from Nexus Repository Manager 3.20+ `R` format is already included. So there are no need to install it.  But if you want to reinstall plugin with your improvements then next instructions will be useful for you.

### Permanent Reinstall

* Copy the new bundle into `<nexus_dir>/system/org/sonatype/nexus/plugins/nexus-repository-r/1.1.1-SNAPSHOT/nexus-repository-r-1.1.1-SNAPSHOT.jar`
* If you are using OSS edition, make these mods in: `<nexus_dir>/system/com/sonatype/nexus/assemblies/nexus-oss-feature/3.x.y/nexus-oss-feature-3.x.y-features.xml`
* If you are using PRO edition, make these mods in: `<nexus_dir>/system/com/sonatype/nexus/assemblies/nexus-pro-feature/3.x.y/nexus-pro-feature-3.x.y-features.xml`

   ```
         <feature version="3.x.y.xy" prerequisite="false" dependency="false">nexus-repository-rubygems</feature>
   +     <feature version="1.1.1.SNAPSHOT" prerequisite="false" dependency="false">nexus-repository-r</feature>
         <feature version="3.x.y.xy" prerequisite="false" dependency="false">nexus-repository-yum</feature>
     </feature>
   ```
   And
   ```
   + <feature name="nexus-repository-r" description="org.sonatype.nexus.plugins:nexus-repository-r" version="1.1.1.SNAPSHOT">
   +     <details>org.sonatype.nexus.plugins:nexus-repository-r</details>
   +     <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-r/1.1.1-SNAPSHOT</bundle>
   + </feature>
    </features>
   ```
This will cause the plugin to be loaded and started with each startup of Nexus Repository.

### Easiest Install for version  
Relevant only for Nexus Repository manager version 3.19 and lower  which doesn't include `R` plugin.
But we strongly recommend to update to the latest Nexus Repository version.

Thanks to some upstream work in Nexus Repository (versions newer than 3.15), it's become a LOT easier to install a plugin. To install the `R` plugin, you can either build locally or download from The Central Repository:

#### Option 1: Build a *.kar file locally from the GitHub Repo
* Clone this repo and `cd` to the cloned directory location
* Build the plugin with `mvn clean package`
* There should now be a `nexus-repository-r-1.1.0.jar` file your `<cloned_repo>/target` directory 

#### Option 2: Download a *.kar file from The Central Repository 
* Download `nexus-repository-r-1.1.0.jar` from [The Central Repository](https://search.maven.org/artifact/org.sonatype.nexus.plugins/nexus-repository-r/1.1.0/bundle)

Once you've completed Option 1 or 2, copy the `nexus-repository-r-1.1.0.jar` file into the `<nexus_dir>/deploy` folder for your Nexus Repository installation.

Restart Nexus Repo, or go ahead and start it if it wasn't running to begin with.

You should see the R repository types (e.g. `r (proxy)`) in the available Repository Recipes to use, if all has gone according to plan :)

## The Fine Print

Starting from version 3.20+ the `R` plugin is supported by Sonatype, but still is a contribution of ours
to the open source community (read: you!)

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin and the Nexus platform, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
