package no.nav.tiltakspenger.saksbehandling.person.infra.http

import org.intellij.lang.annotations.Language

/**
 * GraphQL-spørringene vi sender til PDL.
 * Skjemaet står i https://github.com/navikt/pdl/blob/master/apps/api/src/main/resources/schemas/pdl.graphqls og kan prøves i https://pdl-playground.dev.intern.nav.no/.
 */

@Language("GraphQL")
private const val METADATAFRAGMENTER = """
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
"""

@Language("GraphQL")
private const val PERSONFELTER = """
adressebeskyttelse(historikk: false) {
    gradering
    folkeregistermetadata { ...folkeregistermetadataDetails }
    metadata { ...metadataDetails }
}
navn(historikk: false) {
    fornavn
    mellomnavn
    etternavn
    folkeregistermetadata { ...folkeregistermetadataDetails }
    metadata { ...metadataDetails }
}
foedselsdato {
    foedselsdato
    folkeregistermetadata { ...folkeregistermetadataDetails }
    metadata { ...metadataDetails }
}
doedsfall {
    doedsdato
}
"""

@Language("GraphQL")
const val HENT_PERSON = """
query(${'$'}ident: ID!) {
    hentPerson(ident: ${'$'}ident) {
        $PERSONFELTER
    }
}
$METADATAFRAGMENTER
"""

@Language("GraphQL")
const val HENT_PERSON_BOLK = """
query(${'$'}identer: [ID!]!) {
    hentPersonBolk(identer: ${'$'}identer) {
        ident
        person {
            $PERSONFELTER
        }
        code
    }
}
$METADATAFRAGMENTER
"""

@Language("GraphQL")
const val HENT_FORELDER_BARN_RELASJON = """
query(${'$'}ident: ID!) {
    hentPerson(ident: ${'$'}ident) {
        forelderBarnRelasjon {
            relatertPersonsIdent
            relatertPersonsRolle
            folkeregistermetadata { ...folkeregistermetadataDetails }
            metadata { ...metadataDetails }
        }
    }
}
$METADATAFRAGMENTER
"""

/** Henter bare adressebeskyttelsen, med metadataene `avklarGradering` trenger for å velge gradering. */
@Language("GraphQL")
const val HENT_ADRESSEBESKYTTELSE_BOLK = """
query(${'$'}identer: [ID!]!) {
    hentPersonBolk(identer: ${'$'}identer) {
        ident
        person {
            adressebeskyttelse(historikk: false) {
                gradering
                folkeregistermetadata { ajourholdstidspunkt }
                metadata {
                    endringer { kilde registrert registrertAv systemkilde type }
                    master
                }
            }
        }
        code
    }
}
"""

@Language("GraphQL")
const val HENT_IDENTER = """
query(${'$'}ident: ID!) {
    hentIdenter(ident: ${'$'}ident, grupper: [FOLKEREGISTERIDENT, AKTORID, NPID], historikk: false) {
        identer { ident historisk gruppe }
    }
}
"""

/** Gir både nåværende og historiske fødselsnumre og d-numre. */
@Language("GraphQL")
const val HENT_IDENTER_MED_HISTORIKK = """
query(${'$'}ident: ID!) {
    hentIdenter(ident: ${'$'}ident, grupper: [FOLKEREGISTERIDENT], historikk: true) {
        identer { ident historisk gruppe }
    }
}
"""
