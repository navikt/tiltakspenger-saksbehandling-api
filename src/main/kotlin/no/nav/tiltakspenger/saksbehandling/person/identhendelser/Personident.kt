package no.nav.tiltakspenger.saksbehandling.person.identhendelser

import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type

data class Personident(
    val ident: String,
    val historisk: Boolean,
    val identtype: Identtype,
)

enum class Identtype {
    FOLKEREGISTERIDENT,
    NPID,
    AKTORID,
}

fun Identifikator.toPersonident() =
    Personident(
        ident = idnummer,
        historisk = !gjeldende,
        identtype = type.toIdenttype(),
    )

private fun Type.toIdenttype(): Identtype {
    return when (this) {
        Type.FOLKEREGISTERIDENT -> Identtype.FOLKEREGISTERIDENT
        Type.NPID -> Identtype.NPID
        Type.AKTORID -> Identtype.AKTORID
    }
}
