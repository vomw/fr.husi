package fr.husi.platform

data class PathLimits(
    val maxNameLength: Int,
    val maxPathLength: Int,
    val countsUtf8Bytes: Boolean,
) {
    fun lengthOf(text: String): Int = text.encodeToByteArray().size

    fun acceptsName(name: String): Boolean = lengthOf(name) <= maxNameLength

    fun acceptsPath(path: String): Boolean = lengthOf(path) <= maxPathLength

    companion object {
        /** Android's app storage uses Linux filesystems with a 255-byte name limit. */
        val current = PathLimits(
            maxNameLength = 255,
            maxPathLength = 4096,
            countsUtf8Bytes = true,
        )
    }
}
