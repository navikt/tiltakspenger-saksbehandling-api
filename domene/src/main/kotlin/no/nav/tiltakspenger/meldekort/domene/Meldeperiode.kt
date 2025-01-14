package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class Meldeperiode(
    val id: MeldeperiodeId,
    val hendelseId: HendelseId,
    val versjon: HendelseVersjon,

    val periode: Periode,
    val opprettet: LocalDateTime,

    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,

    // Dette gjelder hele perioden
    val antallDagerForPeriode: Int,

    val girRett: Map<LocalDate, Boolean>,

    val sendtTilMeldekortApi: LocalDateTime?,

//    fun settIkkeRettTilTiltakspenger(periode: Periode, tidspunkt: LocalDateTime): Meldeperiode
)

fun Sak.opprettFørsteMeldeperiode(): Meldeperiode {
    requireNotNull(this.vedtaksliste.førstegangsvedtak) { "Kan ikke opprette første meldeperiode uten førstegangsvedtak" }
    requireNotNull(this.vedtaksperiode) { "Kan ikke opprette første meldeperiode uten en vedtaksperiode" }

    val periode = finnFørsteMeldekortsperiode(this.vedtaksperiode)
    val utfallsperioder = this.vedtaksliste.førstegangsvedtak.utfallsperioder

    return this.opprettMeldeperiode(periode, utfallsperioder)
}

fun Sak.opprettNesteMeldeperiode(): Meldeperiode? {
    require(this.vedtaksliste.isNotEmpty()) { "Vedtaksliste kan ikke være tom" }

    val siste = this.meldeperiodeKjeder.hentSisteMeldeperiod()
    // TODO: sjekk at det finnes en gyldig neste periode
    val nestePeriode = Periode(siste.periode.fraOgMed.plusDays(14), siste.periode.tilOgMed.plusDays(14))

    val utfallsperioder = this.vedtaksliste.utfallsperioder
    if (nestePeriode.tilOgMed.isAfter(utfallsperioder.totalePeriode.tilOgMed)) {
        return null
    }

    return this.opprettMeldeperiode(nestePeriode, utfallsperioder)
}

private fun Sak.opprettMeldeperiode(periode: Periode, utfallsperioder: Periodisering<AvklartUtfallForPeriode>?): Meldeperiode {
    val meldeperiode = Meldeperiode(
        id = MeldeperiodeId.fraPeriode(periode),
        hendelseId = HendelseId.random(),
        fnr = this.fnr,
        saksnummer = this.saksnummer,
        sakId = this.id,
        antallDagerForPeriode = this.hentAntallDager()!!,
        periode = periode,
        opprettet = nå(),
        versjon = HendelseVersjon.ny(),
        girRett = periode.tilDager().associateWith {
            (utfallsperioder?.hentVerdiForDag(it) == AvklartUtfallForPeriode.OPPFYLT)
        },
        sendtTilMeldekortApi = null,
    )

    return meldeperiode
}

private fun finnFørsteMeldekortsperiode(periode: Periode): Periode {
    val førsteMandagIMeldekortsperiode = periode.fraOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sisteSøndagIMeldekortsperiode = førsteMandagIMeldekortsperiode.plusDays(13)

    return Periode(førsteMandagIMeldekortsperiode, sisteSøndagIMeldekortsperiode)
}
