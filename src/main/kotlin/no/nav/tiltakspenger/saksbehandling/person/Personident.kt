package no.nav.tiltakspenger.saksbehandling.person

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

// TODO: Fila står i whitelisten til GenererteWiretyperKonsistTest fordi mappingen under importerer Avro-typene fra PDL.
//  Unntaket er ikke målet: domenetypen skal ikke ligge i samme fil som mappingen fra kildens skjema, jf. «mapping er infrastruktur» i AGENTS.md.
//  Fiks: flytt `toPersonident` og `toIdenttype` til infrastrukturen ved siden av AktorV2Consumer, og la denne fila kun bære `Personident` og `Identtype`.
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
