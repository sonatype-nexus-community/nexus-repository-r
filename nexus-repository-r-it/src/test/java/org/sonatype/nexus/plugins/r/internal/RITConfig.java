package org.sonatype.nexus.plugins.r.internal;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.NEXUS_PROPERTIES_FILE;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.nexusFeature;

public class RITConfig
{
  // Immediately start metadata processing
  public static final long METADATA_PROCESSING_DELAY_MILLIS = 0L;

  public static Option[] configureRBase() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r"),
        systemProperty("nexus-exclude-features").value("nexus-cma-community")
    );
  }

  public static Option[] configureRWithMetadataProcessingInterval() {
    return NexusPaxExamSupport.options(
        configureRBase(),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.r.packagesBuilder.interval",
            String.valueOf(METADATA_PROCESSING_DELAY_MILLIS))
    );
  }
}
