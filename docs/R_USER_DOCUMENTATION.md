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
## R Repositories

### Introduction

[R](https://www.r-project.org/) is a language used for statistical analysis and machine learning.
R and R Studio both allow you to install packages from repositories, allowing convenient access to a large amount of
packages from the remote Comprehensive R Archive Network (CRAN). This reduces the complexity of their development
efforts and improves the resulting applications.

This allows the repository manager to take advantage of the packages in the official CRAN repository and other public
repositories without incurring repeated downloads of packages.

The official CRAN repository is available for browsing at [https://cran.r-project.org/](https://cran.r-project.org/).

You can publish your own packages to a private R repository as a hosted repository on the repository manager and
then expose the remote and private repositories to R as a repository group, which is a repository that merges
and exposes the contents of multiple repositories in one convenient URL. This allows you to reduce time and
bandwidth usage for accessing R packages as well as share your packages within your organization in
a hosted repository.

### Proxying R Repositories

You can set up an R proxy repository to access a remote repository location, for example the official R
registry at [https://cran.r-project.org/](https://cran.r-project.org/) that is configured as the default in R.

To proxy a R repository, create a new 'r (proxy)' as shown in the documented example in [Repository Management](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-AddingaNewRepository) 
in detail. Minimal configuration steps for R proxy are:

- Define 'Name'
- Define URL for 'Remote storage' e.g. [https://cran.r-project.org/](https://cran.r-project.org/)
- Select a 'Blob store' for 'Storage'

### Hosting R Repositories

Creating a R hosted repository allows you to register packages in the repository manager. The hosted repository 
acts as an authoritative location for these components.

To add a hosted R repository, create a new repository with the recipe 'r (hosted)' as shown in the documented 
example in [Repository Management](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-AddingaNewRepository). Minimal configuration steps for R hosted are:

- Define 'Name' - e.g. `r-internal`
- Select 'Blob store' for 'Storage'

### R Repository Groups

A repository group is the recommended way to expose all your R repositories from the repository manager to
your users, with minimal additional client side configuration. A repository group allows you to expose the
aggregated content of multiple proxy and hosted repositories as well as other repository groups with one URL in
tool configuration. This is possible for R repositories by creating a new repository with the 'r (group)'
recipe as shown in the documented example in [Repository Management](https://help.sonatype.com/display/NXRM3/Repository+Management#RepositoryManagement-AddingaNewRepository). Minimal configuration steps for R group are:

- Define 'Name' - e.g. `r-all`
- Select 'Blob store' for 'Storage'
- Add R repositories to the 'Members' list in the desired order

### Installing R

R is typically installed either as just R itself, or R with R Studio. R can be obtained from CRAN at
[https://cran.r-project.org/](https://cran.r-project.org/), and R Studio can be obtained from
[https://www.rstudio.com/products/rstudio/download2/](https://www.rstudio.com/products/rstudio/download2/). You
will need to install R before you can install and run R Studio, if you choose to do so.

### Configuring R Package Download

Once you have set up your repositories for R packages, and installed R, you can adjust your R startup script to use
 your repository URLs. A suggested way to do so is to create a `.Rprofile` file, and include a snippet similar to the
following in it.

````
## Default repo
local({r <- getOption("repos")
       r["Nexus"] <- "http://host:port/repository/r-all"
       options(repos=r)
})
````

This will set your default R repository as the group repository. For more information on adjusting R startup files, please
visit [https://www.r-bloggers.com/fun-with-rprofile-and-customizing-r-startup/](https://www.r-bloggers.com/fun-with-rprofile-and-customizing-r-startup/).

If anonymous access to the repository manager is disabled, you have to specify the credentials for the accessing
the repository manager as part of the URL like `http://username:password@host:port/repository/r-all`

Downloaded packages are cached, do not have to be retrieved from the remote repositories again and can be
inspected in the user interface.

### Browsing R Repositories and Searching Packages

You can browse R repositories in the user interface inspecting the components and assets and their details, as
described in [Search for Components](https://help.sonatype.com/display/NXRM3/Searching+for+Components).

Searching for R packages can be performed in the user interface, too. It finds all packages that are currently
stored in the repository manager, either because they have been pushed to a hosted repository or they have been
proxied from an upstream repository and cached in the repository manager.

### Publishing R Packages

If you are authoring your own packages and want to distribute them to other users in your organization, you have
to upload them to a hosted repository on the repository manager. The consumers can then download it via the
repository group as documented in <<r-download>>.

Authentication is managed in the same manner as for proxying with anonymous access disabled as documented in
[r-download](#configuring-r-package-download).

With this configuration you can run a command such as

`curl -v --user 'user:pass' --upload-file example_1.0.0.tar.gz http://localhost:8081/repository/r-hosted/src/contrib/example_1.0.0.tar.gz`

The package will now be contained within your hosted repository and consumers can install the package using a
command similar to

`install.packages("example", repos="http://localhost:8081/repository/r-all")`
