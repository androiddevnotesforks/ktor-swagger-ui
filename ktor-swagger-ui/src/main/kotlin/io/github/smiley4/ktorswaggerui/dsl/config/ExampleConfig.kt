package io.github.smiley4.ktorswaggerui.dsl.config

import io.github.smiley4.ktorswaggerui.data.ExampleConfigData
import io.github.smiley4.ktorswaggerui.data.ExampleDescriptor
import io.github.smiley4.ktorswaggerui.data.SwaggerExampleDescriptor
import io.github.smiley4.ktorswaggerui.data.ValueExampleDescriptor
import io.github.smiley4.ktorswaggerui.dsl.OpenApiDslMarker
import io.github.smiley4.ktorswaggerui.dsl.routes.ValueExampleDescriptorDsl
import io.swagger.v3.oas.models.examples.Example

/**
 * Configuration for schemas
 */
@OpenApiDslMarker
class ExampleConfig {

    val sharedExamples = mutableMapOf<String, ExampleDescriptor>()

    fun example(example: ExampleDescriptor) {
        sharedExamples[example.name] = example
    }

    fun example(name: String, example: Example) = example(SwaggerExampleDescriptor(name, example))

    fun example(name: String, example: ValueExampleDescriptorDsl.() -> Unit) = example(
        ValueExampleDescriptorDsl()
            .apply(example)
            .let { result ->
                ValueExampleDescriptor(
                    name = name,
                    value = result.value,
                    summary = result.summary,
                    description = result.description
                )
            }
    )

    fun build() = ExampleConfigData(
        sharedExamples = sharedExamples
    )

}
