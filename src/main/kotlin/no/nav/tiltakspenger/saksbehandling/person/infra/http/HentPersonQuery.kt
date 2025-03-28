package no.nav.tiltakspenger.saksbehandling.person.infra.http

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlQuery

internal fun hentPersonQuery(fnr: Fnr): GraphqlQuery {
    return GraphqlQuery(
        query = query,
        variables = mapOf(
            "ident" to fnr.verdi,
        ),
    )
}

// Kew: fjernet forelderBarnRelasjon siden vi ikke skal behandle data om dette i første omgang.
private val query = """
query(${'$'}ident: ID!){
    hentPerson(ident: ${'$'}ident) {
        adressebeskyttelse(historikk: false) {
            gradering
            folkeregistermetadata {
                ...folkeregistermetadataDetails
            }
            metadata {
                ...metadataDetails
            }
        }
        navn(historikk: false) {
            fornavn
            mellomnavn
            etternavn
            folkeregistermetadata {
                ...folkeregistermetadataDetails
            }
            metadata {
                ...metadataDetails
            }
        }
        foedselsdato {
            foedselsdato
            folkeregistermetadata {
                ...folkeregistermetadataDetails
            }
            metadata {
                ...metadataDetails
            }
        }
    }
}

fragment folkeregistermetadataDetails on Folkeregistermetadata {
    aarsak
    ajourholdstidspunkt
    gyldighetstidspunkt
    kilde
    opphoerstidspunkt
    sekvens
}

fragment metadataDetails on Metadata {
    endringer {
        kilde
        registrert
        registrertAv
        systemkilde
        type
    }
    master
}
""".trimIndent()
