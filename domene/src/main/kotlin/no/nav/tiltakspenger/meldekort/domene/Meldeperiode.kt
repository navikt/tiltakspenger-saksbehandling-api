package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Utfallsperiode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class Meldeperiode(
    val id: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val versjon: HendelseVersjon,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    /** Dette gjelder hele perioden. TODO rename: Noen med fungerende IDE, kan rename denne til maksAntallDagerForPeriode */
    val antallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
    val sendtTilMeldekortApi: LocalDateTime?,
) {
    fun helePeriodenErSperret(): Boolean {
        return girRett.values.toList().all { !it }
    }

    // TODO Anders: når skal vi tillate at meldekortet fylles ut? Siste fredag i perioden?
    fun erKlarTilUtfylling(): Boolean {
        return periode.fraOgMed <= nå().toLocalDate()
    }

    val ingenDagerGirRett = girRett.values.none { it }
}

fun Sak.opprettFørsteMeldeperiode(): Meldeperiode {
    requireNotNull(this.vedtaksliste.førstegangsvedtak) { "Kan ikke opprette første meldeperiode uten førstegangsvedtak" }
    requireNotNull(this.vedtaksliste.innvilgelsesperioder) { "Kan ikke opprette første meldeperiode uten minst én periode som gir rett til tiltakspenger" }
    // TODO John + Anders: Denne fungerer ikke når vi får 2 førstegangsbehandlinger.
    val periode = finnFørsteMeldekortsperiode(this.vedtaksliste.innvilgelsesperioder.single())
    val utfallsperioder = this.vedtaksliste.førstegangsvedtak!!.utfallsperioder

    return this.opprettMeldeperiode(periode, utfallsperioder!!)
}

/**
 * Dersom vi kan opprette en ny meldeperiode, returnerer vi denne. Dersom vi ikke, returnerer vi null.
 * Hvis ingen av dagene i neste meldeperiode
 * Denne funksjonen tar ikke høyde for om det er "for tidlig" og opprette neste meldeperiode. Dette må håndteres av kaller.
 *
 * @return null dersom vi ikke skal opprette en ny meldeperiode (dvs. at vi har nådd slutten av vedtaksperioden)
 * @throws IllegalStateException hvis det ikke finnes noen vedtak
 */
fun Sak.opprettNesteMeldeperiode(): Meldeperiode? {
    check(this.vedtaksliste.isNotEmpty()) { "Vedtaksliste kan ikke være tom når man prøver opprette neste meldeperiode" }

    val siste: Meldeperiode = this.meldeperiodeKjeder.hentSisteMeldeperiode()
    // Kommentar jah: Dersom vi har hull mellom meldeperiodene, så vil ikke dette være godt nok.
    val nestePeriode = Periode(siste.periode.fraOgMed.plusDays(14), siste.periode.tilOgMed.plusDays(14))

    val utfallsperioder = this.vedtaksliste.utfallsperioder
    if (nestePeriode.fraOgMed.isAfter(utfallsperioder.totalePeriode.tilOgMed)) {
        return null
    }

    val nesteMeldeperiode = this.opprettMeldeperiode(nestePeriode, utfallsperioder)
    if (nesteMeldeperiode.ingenDagerGirRett) return null
    return nesteMeldeperiode
}

private fun Sak.opprettMeldeperiode(
    periode: Periode,
    utfallsperioder: Periodisering<Utfallsperiode>,
): Meldeperiode {
    val meldeperiode = Meldeperiode(
        kjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        id = MeldeperiodeId.random(),
        fnr = this.fnr,
        saksnummer = this.saksnummer,
        sakId = this.id,
        antallDagerForPeriode = this.hentAntallDager()!!,
        periode = periode,
        opprettet = nå(),
        versjon = HendelseVersjon.ny(),
        girRett = periode.tilDager().associateWith {
            (utfallsperioder.hentVerdiForDag(it) == Utfallsperiode.RETT_TIL_TILTAKSPENGER)
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
