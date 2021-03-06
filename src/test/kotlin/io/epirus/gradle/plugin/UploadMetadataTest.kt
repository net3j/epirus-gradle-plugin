package io.epirus.gradle.plugin

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.MultipartValuePatternBuilder
import com.github.tomakehurst.wiremock.matching.RegexPattern
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA

/**
 * Test for the `uploadMetadata` Gradle task.
 */
class UploadMetadataTest {

    /**
     * Project base directory containing the Gradle build file.
     */
    @get:Rule
    val testProjectDir = TemporaryFolder()

    /**
     * Epirus platform mock.
     */
    @get:Rule
    val epirusMock = WireMockRule()

    private lateinit var buildFile: File
    private lateinit var sourceDir: File

    @Before
    fun setUp() {
        buildFile = testProjectDir.newFile("build.gradle")

        val resource = javaClass.classLoader
            .getResource("solidity/StandardToken.sol")

        sourceDir = File(resource!!.file).parentFile

        val buildFileContent = """
            plugins {
               id 'io.epirus'
            }
            web3j {
               generatedPackageName = 'com.web3labs.epirus.test'
               includedContracts = ['StandardToken']
            }
            epirus {
                url = 'http://localhost:8080'
            }
            sourceSets {
               main {
                   solidity {
                       srcDir {
                           '${sourceDir.absolutePath}'
                       }
                   }
               }
            }
            repositories {
              mavenCentral()
            }
        """.trimIndent()

        Files.write(buildFile.toPath(), buildFileContent.toByteArray())
    }

    @Test
    fun uploadMetadata() {
        epirusMock.givenThat(
            post("/metadata")
                .withHeader(CONTENT_TYPE, RegexPattern(MEDIA_TYPE_REGEX))
                .withMultipartRequestBody(
                    MultipartValuePatternBuilder()
                        .withBody(AnythingPattern())
                ).willReturn(ok())
        )

        val gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("uploadMetadata")
            .withPluginClasspath()
            .forwardOutput()

        val success = gradleRunner.build()
        assertNotNull(success.task(":uploadMetadata"))
        assertEquals(SUCCESS, success.task(":uploadMetadata")!!.outcome)

        verify(
            postRequestedFor(urlEqualTo("/metadata"))
                .withHeader(CONTENT_TYPE, matching(MEDIA_TYPE_REGEX))
        )

        // Check second run is up to date 
        val upToDate = gradleRunner.build()
        assertNotNull(upToDate.task(":uploadMetadata"))
        assertEquals(UP_TO_DATE, upToDate.task(":uploadMetadata")!!.outcome)
    }

    companion object {
        private const val MEDIA_TYPE_REGEX = "$MULTIPART_FORM_DATA;boundary=.+"
    }
}
