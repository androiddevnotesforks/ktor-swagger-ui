package io.github.smiley4.ktorswaggerui.builder.openapi

import io.github.smiley4.ktorswaggerui.builder.schema.SchemaContext
import io.github.smiley4.ktorswaggerui.data.OpenApiHeaderData
import io.swagger.v3.oas.models.headers.Header

class HeaderBuilder(
    private val schemaContext: SchemaContext
) {

    fun build(header: OpenApiHeaderData): Header =
        Header().also {
            it.description = header.description
            it.required = header.required
            it.deprecated = header.deprecated
            it.schema = header.type?.let { t -> schemaContext.getSchema(t) }
            it.explode = header.explode
//            it.example = TODO()
        }

}
