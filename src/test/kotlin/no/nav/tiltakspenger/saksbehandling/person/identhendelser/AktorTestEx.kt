package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Bygger en [Aktor] slik den kommer inn på pdl.aktor-v2, med en aktørId i tillegg til fnr-ene.
 * Send `gjeldendeFnr = null` for en hendelse uten gjeldende folkeregisterident.
 */
fun aktor(gjeldendeFnr: Fnr?, historiskeFnr: List<Fnr>): Aktor =
    Aktor(
        buildList {
            gjeldendeFnr?.let { add(Identifikator(it.verdi, Type.FOLKEREGISTERIDENT, true)) }
            historiskeFnr.forEach { add(Identifikator(it.verdi, Type.FOLKEREGISTERIDENT, false)) }
            add(Identifikator("1234567890123", Type.AKTORID, true))
        },
    )
