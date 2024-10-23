package io.github.smiley4.ktorswaggerui.builder.route

import io.github.smiley4.ktorswaggerui.data.PluginConfigData
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.kotest.matchers.shouldBe
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.response.respond
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test


class RouteCollectorTest {

    private fun allRoutes(root: RoutingNode): List<RoutingNode> {
        return (listOf(root) + root.children.flatMap { allRoutes(it) })
            .filter { it.selector is HttpMethodRouteSelector }
    }

    private fun Application.allRoutesPlease(): Pair<RouteCollector, List<RoutingNode>> {
        val routeCollector = RouteCollector(RouteDocumentationMerger())
        val rootNode = plugin(RoutingRoot)
        val allRoutes = allRoutes(rootNode)
        return routeCollector to allRoutes
    }

    private class TransparentRouteSelector : RouteSelector() {
        override suspend fun evaluate(
            context: RoutingResolveContext,
            segmentIndex: Int,
        ): RouteSelectorEvaluation = RouteSelectorEvaluation.Transparent

        override fun toString(): String = ""
    }

    @Test
    fun `should be able to handle transparent route selectors`() {
        testApplication {
            application {
                routing {
                    createChild(TransparentRouteSelector()).route {
                        get("/v1/get_this") { call.respond("Api") }
                    }

                }
                val (routeCollector, allRoutes) = allRoutesPlease()

                val actual = routeCollector.getPath(allRoutes[0], PluginConfigData.DEFAULT)
                println("actual = $actual")
                actual shouldBe "/v1/get_this"
            }
        }
    }
}
