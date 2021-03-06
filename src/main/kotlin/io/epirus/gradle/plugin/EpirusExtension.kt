package io.epirus.gradle.plugin

/**
 * Epirus extension for plugin configuration.
 */
open class EpirusExtension {

    /**
     * Epirus platform URL.
     */
    var url: String = "http://localhost/api"

    companion object {
        /**
         * Extension name used in Gradle build files, e.g.
         * 
         * ```groovy
         * epirus {
         *     url = '...'
         * }
         * ```
         */
        internal const val NAME = "epirus"
    }
}
