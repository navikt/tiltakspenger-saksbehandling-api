package no.nav.tiltakspenger.vedtak.clients.meldekort

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

fun Meldeperiode.tilBrukerDTO(): MeldeperiodeDTO {
    return MeldeperiodeDTO(
        id = this.id.toString(),
        kjedeId = this.kjedeId.toString(),
        versjon = this.versjon.value,
        fnr = this.fnr.verdi,
        saksnummer = this.saksnummer.toString(),
        sakId = this.sakId.toString(),
        opprettet = this.opprettet,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        antallDagerForPeriode = this.antallDagerForPeriode,
        girRett = this.girRett,
    )
}
