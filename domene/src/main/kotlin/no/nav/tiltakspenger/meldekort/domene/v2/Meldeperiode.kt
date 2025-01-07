package no.nav.tiltakspenger.meldekort.domene.v2

import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.felles.Hendelsesversjon
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.finnFørsteMeldekortsperiode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldeperiode(
    val id: MeldeperiodeId,
    val hendelseId: HendelseId,
    val versjon: Hendelsesversjon,

    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val opprettet: LocalDateTime,

    val periode: Periode,

    // Dette gjelder hele perioden
    val antallDagerForPeriode: Int,

    val girRett: Map<LocalDate, Boolean>,

//    fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): Meldeperiode
)

fun Sak.opprettFørsteMeldeperiode(): Meldeperiode {
    val periode = finnFørsteMeldekortsperiode(this.vedtaksperiode!!)
    val utfallsperioder = this.vedtaksliste.førstegangsvedtak!!.utfallsperioder

    return Meldeperiode(
        id = MeldeperiodeId.fraPeriode(periode),
        hendelseId = HendelseId.random(),
        fnr = this.fnr,
        saksnummer = this.saksnummer,
        sakId = this.id,
        antallDagerForPeriode = this.hentAntallDager()!!,
        periode = periode,
        opprettet = nå(),
        versjon = Hendelsesversjon.ny(),
        girRett = periode.tilDager().associateWith {
            (utfallsperioder.hentVerdiForDag(it) == AvklartUtfallForPeriode.OPPFYLT)
        },
    )
}
