package org.sonatype.nexus.plugins.r.internal;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

public class RGroupIT extends RITSupport
{
  private RClient client;

  private Repository repository;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }


}
