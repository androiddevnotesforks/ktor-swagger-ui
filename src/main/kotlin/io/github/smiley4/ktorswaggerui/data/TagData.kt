package io.github.smiley4.ktorswaggerui.data

data class TagData(
    val name: String,
    val description: String?,
    val externalDocDescription: String?,
    val externalDocUrl: String?
) {

    companion object {
        val DEFAULT = TagData(
            name = "",
            description = null,
            externalDocDescription = null,
            externalDocUrl = null
        )
    }
}