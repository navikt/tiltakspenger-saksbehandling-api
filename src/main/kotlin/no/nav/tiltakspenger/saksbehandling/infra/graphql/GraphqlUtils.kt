package no.nav.tiltakspenger.saksbehandling.infra.graphql

data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLResponseError>?,
)

data class GraphQLResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?,
)

data class ErrorLocation(
    val line: String?,
    val column: String?,
)

data class ErrorExtension(
    val code: String?,
    val classification: String?,
)
