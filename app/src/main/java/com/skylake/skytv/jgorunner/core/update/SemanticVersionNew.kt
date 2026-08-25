package com.skylake.skytv.jgorunner.core.update

/**
 * Represents an artifact version adhering to the
 * [Semantic Versioning 2.0](https://semver.org/) specification.
 */

data class SemanticVersionNew(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
    val build: List<String> = emptyList()
) : Comparable<SemanticVersionNew> {

    companion object {
        private const val NUMERIC_IDENTIFIER = "0|[1-9]\\d*"
        private const val NON_NUMERIC_IDENTIFIER = "\\d*[a-zA-Z-][a-zA-Z0-9-]*"
        private const val PRERELEASE_IDENTIFIER = "(?:$NUMERIC_IDENTIFIER|$NON_NUMERIC_IDENTIFIER)"
        private const val BUILD_IDENTIFIER = "[0-9A-Za-z-]+"
        private val SEMVER_REGEX = Regex(
            "v?($NUMERIC_IDENTIFIER)\\.($NUMERIC_IDENTIFIER)\\.($NUMERIC_IDENTIFIER)" +
                    "(?:-($PRERELEASE_IDENTIFIER(?:\\.$PRERELEASE_IDENTIFIER)*))?" +
                    "(?:\\+($BUILD_IDENTIFIER(?:\\.$BUILD_IDENTIFIER)*))?"
        )

        fun parse(version: String): SemanticVersionNew {
            var trimmedVersion = version.trim()

            // If version is pre-release, override it
            if (trimmedVersion.contains("develop", ignoreCase = true)) {
                trimmedVersion = "v99.99.99"
            }

            val match = SEMVER_REGEX.matchEntire(trimmedVersion)
                ?: throw IllegalArgumentException("Invalid semantic version: $trimmedVersion")

            val (major, minor, patch, preReleaseStr, buildStr) = match.destructured
            return SemanticVersionNew(
                major.toInt(),
                minor.toInt(),
                patch.toInt(),
                preReleaseStr.split(".").filter { it.isNotEmpty() },
                buildStr.split(".").filter { it.isNotEmpty() }
            )
        }
    }

    val coreVersion: String
        get() = "$major.$minor.$patch"

    val normalizedVersion: String
        get() {
            val preReleasePart = if (preRelease.isNotEmpty()) "-${preRelease.joinToString(".")}" else ""
            val buildPart = if (build.isNotEmpty()) "+${build.joinToString(".")}" else ""
            return "$coreVersion$preReleasePart$buildPart"
        }

    override fun compareTo(other: SemanticVersionNew): Int {
        // Compare core versions
        val coreComparison = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
        if (coreComparison != 0) return coreComparison

        // Compare pre-release identifiers
        if (preRelease.isEmpty() && other.preRelease.isNotEmpty()) return 1
        if (preRelease.isNotEmpty() && other.preRelease.isEmpty()) return -1
        for (i in preRelease.indices) {
            if (i >= other.preRelease.size) return 1
            val comp = compareIdentifiers(preRelease[i], other.preRelease[i])
            if (comp != 0) return comp
        }
        if (preRelease.size < other.preRelease.size) return -1

        return 0 // Build metadata is ignored in precedence
    }

    private fun compareIdentifiers(a: String, b: String): Int {
        val aInt = a.toIntOrNull()
        val bInt = b.toIntOrNull()
        return when {
            aInt != null && bInt != null -> aInt.compareTo(bInt)
            aInt != null -> -1 // Numeric identifiers have lower precedence
            bInt != null -> 1
            else -> a.compareTo(b)
        }
    }

    override fun toString(): String = normalizedVersion
}
