package org.carlspring.strongbox.controllers.layout.nuget;

import org.carlspring.strongbox.artifact.coordinates.NugetArtifactCoordinates;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.domain.RemoteArtifactEntry;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.rest.common.NugetRestAssuredBaseTest;
import org.carlspring.strongbox.services.ArtifactEntryService;
import org.carlspring.strongbox.storage.metadata.nuget.rss.PackageFeed;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.testing.artifact.ArtifactManagementTestExecutionListener;
import org.carlspring.strongbox.testing.artifact.NugetTestArtifact;
import org.carlspring.strongbox.testing.repository.NugetRepository;
import org.carlspring.strongbox.testing.storage.repository.RepositoryManagementTestExecutionListener;

import javax.inject.Inject;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author Sergey Bespalov
 * @author Pablo Tirado
 */
@IntegrationTest
public class NugetArtifactControllerTest extends NugetRestAssuredBaseTest
{

    private static final String PACKAGE_ID_LAST_VERSION = "Org.Carlspring.Strongbox.Nuget.Test.LastVersion";

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJTdHJvbmdib3giLCJqdGkiOiJ0SExSbWU4eFJOSnJjN" +
                                          "XVXdTVkZDhRIiwic3ViIjoiYWRtaW4iLCJzZWN1cml0eS10b2tlbi1rZXkiOiJhZG1pbi1zZWN" +
                                          "yZXQifQ.xRWxXt5yob5qcHjsvV1YsyfY3C-XFt9oKPABY0tYx88";

    private final static String STORAGE_ID = "storage-nuget-test";

    private static final String REPOSITORY_RELEASES_1 = "nuget-test-releases";

    @Inject
    private ArtifactEntryService artifactEntryService;

    @Override
    @BeforeEach
    public void init()
        throws Exception
    {
        super.init();

        RestAssuredMockMvcConfig config = RestAssuredMockMvcConfig.config();
        config.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails();
        mockMvc.config(config);
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testPackageDelete(@NugetRepository(storageId = STORAGE_ID,
                                                   repositoryId = REPOSITORY_RELEASES_1)
                                  Repository repository,
                                  @NugetTestArtifact(storageId = STORAGE_ID,
                                                     repositoryId = REPOSITORY_RELEASES_1,
                                                     id = "Org.Carlspring.Strongbox.Examples.Nuget.Mono.Delete",
                                                     versions = "1.0.0")
                                  Path packagePath)
        throws Exception
    {
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        NugetArtifactCoordinates coordinates = (NugetArtifactCoordinates) RepositoryFiles.readCoordinates(
                (RepositoryPath) packagePath.normalize());

        // Delete
        String url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/{artifactId}/{artifactVersion}";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .header("X-NuGet-ApiKey", API_KEY)
               .when()
               .delete(url, storageId, repositoryId, coordinates.getId(), coordinates.getVersion())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value());
    }

    @ExtendWith(RepositoryManagementTestExecutionListener.class)
    @Test
    public void testGetMetadata(@NugetRepository(storageId = STORAGE_ID,
                                                 repositoryId = REPOSITORY_RELEASES_1)
                                Repository repository)
    {
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        String url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/$metadata;client=127.0.0.1";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId)
               .then()
               .statusCode(HttpStatus.OK.value());
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testHeaderFetch(@NugetRepository(storageId = STORAGE_ID,
                                                 repositoryId = REPOSITORY_RELEASES_1)
                                Repository repository,
                                @NugetTestArtifact(storageId = STORAGE_ID,
                                                   repositoryId = REPOSITORY_RELEASES_1,
                                                   id = "Org.Carlspring.Strongbox.Examples.Nuget.Mono.Header",
                                                   versions = "1.0.0")
                                Path packagePath)
            throws Exception
    {
        //Hosted repository
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        NugetArtifactCoordinates coordinates = (NugetArtifactCoordinates) RepositoryFiles.readCoordinates(
                (RepositoryPath) packagePath.normalize());

        String url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/{artifactId}/{artifactVersion}";
        Headers headersFromGET = mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                                        .header("X-NuGet-ApiKey", API_KEY)
                                        .accept(ContentType.BINARY)
                                        .when()
                                        .get(url, storageId, repositoryId, coordinates.getId(),
                                             coordinates.getVersion())
                                        .getHeaders();

        Headers headersFromHEAD = mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                                         .header("X-NuGet-ApiKey", API_KEY)
                                         .accept(ContentType.BINARY)
                                         .when()
                                         .head(url, storageId, repositoryId, coordinates.getId(),
                                               coordinates.getVersion())
                                         .getHeaders();

        assertHeadersEquals(headersFromGET, headersFromHEAD);
    }

    protected void assertHeadersEquals(Headers h1, Headers h2)
    {
        assertThat(h1).isNotNull();
        assertThat(h2).isNotNull();

        for (Header header : h1)
        {
            if (h2.hasHeaderWithName(header.getName()))
            {
                assertThat(h2.getValue(header.getName())).isEqualTo(header.getValue());
            }
        }
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testPackageCommonFlow(@NugetRepository(storageId = STORAGE_ID,
                                                       repositoryId = REPOSITORY_RELEASES_1)
                                      Repository repository,
                                      @NugetTestArtifact(id = "Org.Carlspring.Strongbox.Examples.Nuget.Mono",
                                                         versions = "1.0.0")
                                      Path packagePath)
        throws Exception
    {
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();
        final String packageId = "Org.Carlspring.Strongbox.Examples.Nuget.Mono";
        final String packageVersion = "1.0.0";

        long packageSize = Files.size(packagePath);
        byte[] packageContent = readPackageContent(packagePath);

        // Push
        String url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/";
        createPushRequest(packageContent)
               .when()
               .put(url, storageId, repositoryId)
               .peek()
               .then()
               .statusCode(HttpStatus.CREATED.value());

        //Find by ID

        url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/FindPackagesById()?id='{artifactId}'";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, packageId)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body("feed.title", equalTo("Packages"))
               .and()
               .assertThat()
               .body("feed.entry[0].title", equalTo(packageId));

        // We need to mute `System.out` here manually because response body logging hardcoded in current version of
        // RestAssured, and we can not change it using configuration (@see `RestAssuredResponseOptionsGroovyImpl.peek(...)`).
        PrintStream originalSysOut = muteSystemOutput();
        try
        {
            // Get1
            url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/download/{artifactId}/{artifactVersion}";
            mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                   .when()
                   .get(url, storageId, repositoryId, packageId, packageVersion)
                   .peek()
                   .then()
                   .statusCode(HttpStatus.OK.value())
                   .assertThat()
                   .header(HttpHeaders.CONTENT_LENGTH, equalTo(String.valueOf(packageSize)));

            // Get2
            url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/{artifactId}/{artifactVersion}";
            mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                   .when()
                   .get(url, storageId, repositoryId, packageId, packageVersion)
                   .peek()
                   .then()
                   .statusCode(HttpStatus.OK.value())
                   .assertThat()
                   .header(HttpHeaders.CONTENT_LENGTH, equalTo(String.valueOf(packageSize)));
        }
        finally
        {
            System.setOut(originalSysOut);
        }
    }

    /**
     * Mute the system output to avoid malicious logging (binary content for example).
     *
     * @return
     */
    private PrintStream muteSystemOutput()
    {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream()
        {
            public void write(int b)
            {
                //DO NOTHING
            }
        }));

        return original;
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testPackageSearch(@NugetRepository(storageId = STORAGE_ID,
                                                   repositoryId = REPOSITORY_RELEASES_1)
                                  Repository repository,
                                  @NugetTestArtifact(storageId = STORAGE_ID,
                                                     repositoryId = REPOSITORY_RELEASES_1,
                                                     id = "Org.Carlspring.Strongbox.Nuget.Test.Search",
                                                     versions = "1.0.0")
                                  Path packagePath)
        throws Exception
    {
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        // Count
        String url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()/$count?searchTerm={searchTerm}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, "Test.Search")
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .log().body().and()
               .assertThat()
               .body(equalTo("1"));

        // Search
        NugetArtifactCoordinates coordinates = (NugetArtifactCoordinates) RepositoryFiles.readCoordinates(
                (RepositoryPath) packagePath.normalize());

        url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()?$skip={skip}&$top={stop}&searchTerm={searchTerm}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, 0, 30, "Test.Search")
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body("feed.title", equalTo("Packages"))
               .and()
               .assertThat()
               .body("feed.entry[0].title", equalTo(coordinates.getId()));
    }

    @ExtendWith({ RepositoryManagementTestExecutionListener.class,
                  ArtifactManagementTestExecutionListener.class })
    @Test
    public void testLastVersionPackageSearch(@NugetRepository(storageId = STORAGE_ID,
                                                              repositoryId = REPOSITORY_RELEASES_1)
                                             Repository repository,
                                             @NugetTestArtifact(id = PACKAGE_ID_LAST_VERSION,
                                                                versions = { "1.0.0",
                                                                             "2.0.0" })
                                             List<Path> packagePaths)
        throws Exception
    {
        final String storageId = repository.getStorage().getId();
        final String repositoryId = repository.getId();

        String filter = String.format("tolower(Id) eq '%s' and IsLatestVersion", PACKAGE_ID_LAST_VERSION.toLowerCase());

        // VERSION 1.0.0
        Path packagePathV1 = packagePaths.get(0);
        byte[] packageContent = readPackageContent(packagePathV1);
        // Push
        String url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/";
        createPushRequest(packageContent)
               .when()
               .put(url, storageId, repositoryId)
               .peek()
               .then()
               .statusCode(HttpStatus.CREATED.value());

        // Count
        url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()/$count?$filter={filter}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, filter)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body(equalTo("1"));

        // Search
        url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()?$filter{filter}&$skip{skip}&$top={stop}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, filter, 0, 30)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body("feed.title", equalTo("Packages"))
               .and()
               .assertThat()
               .body("feed.entry[0].title", equalTo(PACKAGE_ID_LAST_VERSION))
               .body("feed.entry[0].properties.Version", equalTo("1.0.0"));

        // VERSION 2.0.0
        Path packagePathV2 = packagePaths.get(1);
        packageContent = readPackageContent(packagePathV2);
        // Push
        url = getContextBaseUrl() + "/storages/{storageId}/{repositoryId}/";
        createPushRequest(packageContent)
               .when()
               .put(url, storageId, repositoryId)
               .peek()
               .then()
               .statusCode(HttpStatus.CREATED.value());
        
        // Count
        url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()/$count?$filter={filter}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, filter)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body(equalTo("1"));

        // Search
        url = getContextBaseUrl() +
              "/storages/{storageId}/{repositoryId}/Search()?$filter={filter}&$skip={skip}&$top={stop}&targetFramework=";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, storageId, repositoryId, filter, 0, 30)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body("feed.title", equalTo("Packages"))
               .and()
               .assertThat()
               .body("feed.entry[0].title", equalTo(PACKAGE_ID_LAST_VERSION))
               .body("feed.entry[0].properties.Version", equalTo("2.0.0"));
    }

    public MockMvcRequestSpecification createPushRequest(byte[] packageContent)
    {
        return mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                      .header("X-NuGet-ApiKey", API_KEY)
                      .contentType("multipart/form-data; boundary=---------------------------123qwe")
                      .body(packageContent);
    }

    @Test
    public void testRemoteProxyGroup()
    {
        final String packageId = "NHibernate";
        final String packageVersion =  "4.1.1.4000";

        String url = getContextBaseUrl() + "/storages/public/nuget-group/FindPackagesById()?id={artifactId}";
        mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
               .when()
               .get(url, packageId)
               .then()
               .statusCode(HttpStatus.OK.value())
               .and()
               .assertThat()
               .body("feed.title", equalTo("Packages"))
               .and()
               .assertThat()
               .body("feed.entry[0].title", equalTo(packageId));

        Map<String, String> coordinatesMap = new HashMap<>();
        coordinatesMap.put("id", packageId);
        coordinatesMap.put("version", packageVersion);

        List<ArtifactEntry> artifactEntryList = artifactEntryService.findArtifactList("storage-common-proxies",
                                                                                      "nuget.org",
                                                                                      coordinatesMap,
                                                                                      true);
        assertThat(artifactEntryList).isNotEmpty();

        ArtifactEntry artifactEntry = artifactEntryList.iterator().next();
        assertThat(artifactEntry).isInstanceOf(RemoteArtifactEntry.class);
        assertThat(((RemoteArtifactEntry)artifactEntry).getIsCached()).isFalse();

        PrintStream originalSysOut = muteSystemOutput();
        try
        {
            url = getContextBaseUrl() + "/storages/public/nuget-group/package/{artifactId}/{artifactVersion}";
            mockMvc.header(HttpHeaders.USER_AGENT, "NuGet/*")
                   .when()
                   .get(url, packageId, packageVersion)
                   .peek()
                   .then()
                   .statusCode(HttpStatus.OK.value())
                   .assertThat()
                   .header(HttpHeaders.CONTENT_LENGTH, equalTo(String.valueOf(1499857)));
        }
        finally
        {
            System.setOut(originalSysOut);
        }
    }

    @Test
    public void testRemoteLastVersion()
    {
        String url = getContextBaseUrl()
                     + "/storages/public/nuget-group/FindPackagesById()?id=NHibernate&$orderby=Version";
        PackageFeed feed = mockMvc.log()
                                  .all()
                                  .header(HttpHeaders.USER_AGENT, "NuGet/*")
                                  .when()
                                  .get(url)
                                  .body()
                                  .as(PackageFeed.class);

        assertThat(feed.getEntries()
                       .stream()
                       .reduce((first,
                                second) -> second)
                       .filter(e -> Boolean.TRUE.equals(e.getProperties().getIsLatestVersion()))
                       .isPresent())
                .isTrue();
    }

}
