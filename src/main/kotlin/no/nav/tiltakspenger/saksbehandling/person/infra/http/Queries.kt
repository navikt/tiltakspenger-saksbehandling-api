package no.nav.tiltakspenger.saksbehandling.person.infra.http

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlQuery

val hentEnkelPersonQuery = PersonHttpklient::class.java.getResource("/pdl/hentEnkelPersonQuery.graphql")!!.readText()

internal fun hentEnkelPersonQuery(fnr: Fnr): GraphqlQuery {
    return GraphqlQuery(
        query = hentEnkelPersonQuery,
        variables = mapOf(
            "ident" to fnr.verdi,
        ),
    )
}
