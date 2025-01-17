package no.nav.tiltakspenger.vedtak.clients.veilarboppfolging

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlQuery

internal fun hentOppfolgingsenhetQuery(fnr: Fnr): GraphqlQuery {
    return GraphqlQuery(
        query = query,
        variables = mapOf(
            "fnr" to fnr.verdi,
        ),
    )
}

private val query = """
query(${'$'}fnr: String!){
    oppfolgingsEnhet(fnr: ${'$'}fnr) {
        enhet {
            id
            navn
        }
    }
}
""".trimIndent()
