package io.github.smiley4.ktorswaggerui.spec.schema

import io.github.smiley4.ktorswaggerui.SwaggerUIPluginConfig
import io.github.smiley4.ktorswaggerui.dsl.CustomArraySchemaRef
import io.github.smiley4.ktorswaggerui.dsl.CustomJsonSchema
import io.github.smiley4.ktorswaggerui.dsl.CustomObjectSchemaRef
import io.github.smiley4.ktorswaggerui.dsl.CustomOpenApiSchema
import io.github.smiley4.ktorswaggerui.dsl.CustomSchemaRef
import io.github.smiley4.ktorswaggerui.dsl.OpenApiBaseBody
import io.github.smiley4.ktorswaggerui.dsl.OpenApiMultipartBody
import io.github.smiley4.ktorswaggerui.dsl.OpenApiRequestParameter
import io.github.smiley4.ktorswaggerui.dsl.OpenApiResponse
import io.github.smiley4.ktorswaggerui.dsl.OpenApiSimpleBody
import io.github.smiley4.ktorswaggerui.dsl.RemoteSchema
import io.github.smiley4.ktorswaggerui.dsl.SchemaType
import io.github.smiley4.ktorswaggerui.dsl.getTypeName
import io.github.smiley4.ktorswaggerui.spec.route.RouteMeta
import io.github.smiley4.ktorswaggerui.spec.schemaV2.SchemaBuilder
import io.github.smiley4.ktorswaggerui.spec.schemaV2.SchemaDefinitions
import io.swagger.v3.oas.models.media.Schema
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class SchemaContext(
    private val config: SwaggerUIPluginConfig,
    private val schemaBuilder: SchemaBuilder
) {

    private val schemas = mutableMapOf<String, SchemaDefinitions>()
    private val customSchemas = mutableMapOf<String, Schema<*>>()


    fun initialize(routes: Collection<RouteMeta>): SchemaContext {
        routes.forEach { handle(it) }
        config.getDefaultUnauthorizedResponse()?.also { handle(it) }
        return this
    }


    private fun handle(route: RouteMeta) {
        route.documentation.getRequest().getBody()?.also { handle(it) }
        route.documentation.getRequest().getParameters().forEach { handle(it) }
        route.documentation.getResponses().getResponses().forEach { handle(it) }
    }


    private fun handle(response: OpenApiResponse) {
        response.getHeaders().forEach { (_, header) ->
            header.type?.also { headerType ->
                createSchema(headerType)
            }
        }
        response.getBody()?.also { handle(it) }
    }


    private fun handle(body: OpenApiBaseBody) {
        return when (body) {
            is OpenApiSimpleBody -> handle(body)
            is OpenApiMultipartBody -> handle(body)
        }
    }


    private fun handle(body: OpenApiSimpleBody) {
        if (body.customSchema != null) {
            body.customSchema?.also { createSchema(it) }
        } else {
            body.type?.also { createSchema(it) }
        }
    }


    private fun handle(body: OpenApiMultipartBody) {
        body.getParts().forEach { part ->
            if (part.customSchema != null) {
                part.customSchema?.also { createSchema(it) }
            } else {
                part.type?.also { createSchema(it) }
            }
        }
    }


    private fun handle(parameter: OpenApiRequestParameter) {
        createSchema(parameter.type)
    }


    private fun createSchema(type: SchemaType) {
        if (schemas.containsKey(type.getTypeName())) {
            return
        }
        addSchema(type, schemaBuilder.create(type))
    }


    private fun createSchema(customSchemaRef: CustomSchemaRef) {
        if (customSchemas.containsKey(customSchemaRef.schemaId)) {
            return
        }
        val customSchema = config.getCustomSchemas().getSchema(customSchemaRef.schemaId)
        if (customSchema == null) {
            addSchema(customSchemaRef, Schema<Any>())
        } else {
            when (customSchema) {
                is CustomJsonSchema -> {
                    schemaBuilder.create(customSchema.provider()).root
                }
                is CustomOpenApiSchema -> {
                    customSchema.provider()
                }
                is RemoteSchema -> {
                    Schema<Any>().apply {
                        type = "object"
                        `$ref` = customSchema.url
                    }
                }
            }.let { schema ->
                when (customSchemaRef) {
                    is CustomObjectSchemaRef -> schema
                    is CustomArraySchemaRef -> Schema<Any>().apply {
                        this.type = "array"
                        this.items = schema
                    }
                }
            }.also {
                addSchema(customSchemaRef, it)
            }
        }
    }

    fun addSchema(type: SchemaType, schema: SchemaDefinitions) {
        schemas[type.getTypeName()] = schema
    }

    fun addSchema(customSchemaRef: CustomSchemaRef, schema: Schema<*>) {
        customSchemas[customSchemaRef.schemaId] = schema
    }

    fun getComponentSection(): Map<String, Schema<*>> {
        val componentSection = mutableMapOf<String, Schema<*>>()
        schemas.forEach { (_, schemaDefinitions) ->
            val rootSchema = schemaDefinitions.root
            if (isPrimitive(rootSchema) || isPrimitiveArray(rootSchema)) {
                // skip
            } else {
                componentSection.putAll(schemaDefinitions.definitions)
            }
        }
        customSchemas.forEach { (schemaId, schema) ->
            componentSection[schemaId] = schema
        }
        return componentSection
    }


    fun getSchema(customSchemaRef: CustomSchemaRef): Schema<*> {
        val schema = customSchemas[customSchemaRef.schemaId]
            ?: throw IllegalStateException("Could not retrieve schema for type '${customSchemaRef.schemaId}'")
        return buildInlineSchema(customSchemaRef.schemaId, schema)
    }


    fun getSchema(type: SchemaType): Schema<*> {
        return getSchemaDefinitions(type).root
    }


    private fun buildInlineSchema(schemaId: String, schema: Schema<*>): Schema<*> {
        if (isPrimitive(schema)) {
            return schema
        }
        if (isPrimitiveArray(schema)) {
            return schema
        }
        return Schema<Any>().also {
            it.`$ref` = "#/components/schemas/$schemaId"
        }
    }


    private fun getSchemaDefinitions(type: SchemaType): SchemaDefinitions {
        return type.getTypeName().let { typeName ->
            schemas[typeName] ?: throw IllegalStateException("Could not retrieve schema for type '${typeName}'")
        }
    }


    private fun isPrimitive(schema: Schema<*>): Boolean {
        return schema.type != "object" && schema.type != "array" && schema.type != null
    }

    private fun isPrimitiveArray(schema: Schema<*>): Boolean {
        return schema.type == "array" && (isPrimitive(schema.items) || isPrimitiveArray(schema.items))
    }

    private fun isReference(schema: Schema<*>): Boolean {
        return schema.type == null && schema.`$ref` != null
    }

}
