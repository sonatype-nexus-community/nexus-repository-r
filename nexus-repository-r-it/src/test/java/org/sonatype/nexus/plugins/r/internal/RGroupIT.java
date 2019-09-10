package org.sonatype.nexus.plugins.r.internal;

import java.io.InputStream;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.http.HttpResponse;
import org.apache.shiro.util.ThreadContext;
import org.apache.tika.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class RGroupIT
    extends RITSupport
{
  private RClient proxyClient;

  private RClient hostedClient;

  private RClient groupClient;

  private Server remote;

  private Repository repoGroup;

  private Repository repoProxy;

  private Repository repoHosted;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setUp() throws Exception {
    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));
    remote = Server.withPort(0)
        .serve("/*").withBehaviours(error(OK))
        .serve("/" + AGRICOLAE_PATH_FULL_121_TARGZ)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_PKG_FILE_NAME_121_TARGZ)))
        .serve("/" + "bla/02/myfunpackage_1.0.tar.gz")
        .withBehaviours(file(testData.resolveFile("myfunpackage_1.0.tar.gz")))
        .start();

    repoProxy = repos.createRProxy("r-proxy", remote.getUrl().toExternalForm());
    repoHosted = repos.createRHosted("r-hosted");
    repoGroup = repos.createRGroup("r-group",  repoHosted.getName(),repoProxy.getName());

    proxyClient  = createRClient(repoProxy);
    hostedClient = createRClient(repoHosted);
    groupClient = createRClient(repoGroup);

    assertThat(status(hostedClient.put(AGRICOLAE_PATH_FULL_131_TARGZ,
        fileToHttpEntity(AGRICOLAE_PKG_FILE_NAME_131_TARGZ))), is(OK));

  }

  @After
  public void tearDown() throws Exception {
    remote.stop();
  }

  @Test
  @Ignore("https://issues.sonatype.org/browse/NEXUS-21089")
  public void whenRequestUnknownRpmShouldReturnError() throws Exception {
    assertThat(status(groupClient.fetch(DOES_NOT_EXIST_PKG_TGZ)), is(NOT_FOUND));
  }

  @Test
  public void whenRequestValidPackageFromProxyShouldReturnSuccess() throws Exception {
    final HttpResponse response = groupClient.fetch(AGRICOLAE_PATH_FULL_121_TARGZ);
    assertSuccessResponseMatches(response, AGRICOLAE_PKG_FILE_NAME_121_TARGZ);
  }

  @Test
  public void whenRequestValidPackageFromHostedShouldReturnSuccess() throws Exception {
    final HttpResponse response = groupClient.fetch(AGRICOLAE_PATH_FULL_131_TARGZ);
    assertSuccessResponseMatches(response, AGRICOLAE_PKG_FILE_NAME_131_TARGZ);
  }

  @Test
  public void whenRequestMetadataFromGroupShouldReturnSuccess() throws Exception {
    final InputStream content = groupClient.fetch(PACKAGES_PATH_FULL).getEntity().getContent();
    verifyTextGzipContent(is(equalTo("bla bla bla")), content);
  }

  private void verifyTextGzipContent(Matcher<String> expectedContent, InputStream is) throws Exception {
    try (InputStream cin = new CompressorStreamFactory().createCompressorInputStream(GZIP, is)) {
      final String downloadedPackageData = IOUtils.toString(cin);
      assertThat(downloadedPackageData, expectedContent);
    }
  }
}
