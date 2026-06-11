package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periode.overlapper
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.finnAntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.collections.sorted
import kotlin.math.min

typealias OppdaterteKjederOgMeldeperioder = Pair<MeldeperiodeKjeder, List<Meldeperiode>>

/**
 *  Genererer hypotetiske meldeperioder ut fra en ikke-vedtatt behandling.
 *
 *  Skal kun benyttes for validering/testing, ikke for å generere faktiske meldeperioder som persisteres!
 * */
fun Sak.genererMeldeperioderForValidering(rammebehandling: Rammebehandling, clock: Clock): List<Meldeperiode> {
    val vedtaksperiode = rammebehandling.vedtaksperiode

    requireNotNull(vedtaksperiode) {
        "Behandling må ha satt en vedtaksperiode for å generere meldeperioder"
    }

    require(rammebehandling.status != Rammebehandlingsstatus.VEDTATT) {
        "For vedtatte behandlinger skal meldeperioder genereres kun ut fra vedtak på saken"
    }

    return this.meldeperiodeKjeder.genererMeldeperioder(
        vedtaksperioder = rammevedtaksliste.vedtaksperioder
            .plus(vedtaksperiode)
            .leggSammen(true),
        hentAntallDager = { periode ->
            val antallDagerFraSak by lazy { rammevedtaksliste.antallDagerForMeldeperiode(periode) }
            val antallDagerFraBehandling by lazy {
                rammebehandling.antallDagerPerMeldeperiode?.finnAntallDagerForMeldeperiode(periode)
            }

            when {
                // Meldeperioden er i sin helhet innenfor behandlingens vedtaksperiode -> bruk verdien fra behandlingen
                vedtaksperiode.inneholderHele(periode) -> antallDagerFraBehandling

                // Meldeperioden er i sin helhet utenfor behandlingens vedtaksperiode -> bruk verdien fra saken
                !vedtaksperiode.overlapperMed(periode) -> antallDagerFraSak

                // Delvis overlapp -> bruk den høyeste av verdiene fra saken og behandlingen
                else -> listOfNotNull(antallDagerFraSak, antallDagerFraBehandling).maxOrNull()
            }
        },
        hentVedtakIder = { periode ->
            val vedtakIderFraSak = rammevedtaksliste.vedtakForPeriode(periode)
            val overlappMedBehandling = periode.overlappendePeriode(vedtaksperiode)
            if (overlappMedBehandling == null) {
                // Meldeperioden er i sin helhet utenfor behandlingens vedtaksperiode -> bruk vedtakene fra saken
                vedtakIderFraSak as IkkeTomPeriodisering
            } else {
                // Overskriver den delen som overlapper behandlingen med den nye (midlertidige) vedtakId-en,
                // og beholder eventuelle vedtak fra saken for resten av perioden.
                vedtakIderFraSak.setVerdiForDelperiode(VedtakId.random(), overlappMedBehandling)
            }
        },
        harRett = { dato ->
            if (vedtaksperiode.inneholder(dato)) {
                rammebehandling.innvilgelsesperioder?.hentVerdiForDag(dato) != null
            } else {
                rammevedtaksliste.harInnvilgetTiltakspengerPåDato(dato)
            }
        },
        fnr = fnr,
        saksnummer = saksnummer,
        sakId = id,
        clock = clock,
    )
}

fun MeldeperiodeKjeder.genererMeldeperioder(
    vedtaksliste: Rammevedtaksliste,
    clock: Clock,
): OppdaterteKjederOgMeldeperioder {
    if (vedtaksliste.isEmpty()) {
        require(this.isEmpty()) { "Forventet ingen meldeperioder dersom det ikke er noen vedtaksperioder" }
        return this to emptyList()
    }

    return this.genererMeldeperioder(
        vedtaksperioder = vedtaksliste.vedtaksperioder,
        hentAntallDager = { vedtaksliste.antallDagerForMeldeperiode(it) },
        hentVedtakIder = { vedtaksliste.vedtakForPeriode(it) as IkkeTomPeriodisering },
        harRett = { vedtaksliste.harInnvilgetTiltakspengerPåDato(it) },
        fnr = vedtaksliste.fnr!!,
        saksnummer = vedtaksliste.saksnummer!!,
        sakId = vedtaksliste.sakId!!,
        clock = clock,
    ).let {
        this.oppdaterEllerLeggTilNy(it)
    }
}

private fun MeldeperiodeKjeder.genererMeldeperioder(
    vedtaksperioder: List<Periode>,
    hentAntallDager: (periode: Periode) -> AntallDagerForMeldeperiode?,
    hentVedtakIder: (periode: Periode) -> IkkeTomPeriodisering<VedtakId>,
    harRett: (dato: LocalDate) -> Boolean,
    fnr: Fnr,
    saksnummer: Saksnummer,
    sakId: SakId,
    clock: Clock,
): List<Meldeperiode> {
    return this
        .genererPerioder(vedtaksperioder)
        .map { periode ->
            val girRett = periode
                .tilDager()
                .associateWith { harRett(it) }

            // Antall dager som kan meldes kan ikke være høyere enn antall dager som gir rett
            // Tar hensyn til meldeperioder som ikke overlapper 100% med innvilgelsesperiodene
            val antallDagerFraTidslinje = girRett.count { it.value }
            val antallDagerFraVedtak = hentAntallDager(periode)?.value ?: 0
            val antallDagerForPeriode = min(antallDagerFraTidslinje, antallDagerFraVedtak)

            val meldeperiodeVersjon = hentMeldeperiodeKjedeForPeriode(periode)?.nesteVersjon()
                ?: HendelseVersjon.ny()

            Meldeperiode.opprettMeldeperiode(
                periode = periode,
                girRett = girRett,
                fnr = fnr,
                saksnummer = saksnummer,
                sakId = sakId,
                antallDagerForPeriode = antallDagerForPeriode,
                versjon = meldeperiodeVersjon,
                rammevedtak = hentVedtakIder(periode),
                clock = clock,
            )
        }
}

/**
 * @return Alle 14-dagers perioder som overlapper med [vedtaksperioder] eller eksisterende meldeperioder
 *
 * */
private fun MeldeperiodeKjeder.genererPerioder(vedtaksperioder: List<Periode>): List<Periode> {
    val totalPeriode = vedtaksperioder.let {
        Periode(it.first().fraOgMed, it.last().tilOgMed)
    }

    val førstePeriode = finnNærmesteMeldeperiode(totalPeriode.fraOgMed)

    return generateSequence(førstePeriode) { forrige ->
        val nesteFraOgMed = forrige.tilOgMed.plusDays(1)
        Periode(nesteFraOgMed, nesteFraOgMed.plusDays(13))
    }
        .takeWhile { it.fraOgMed <= totalPeriode.tilOgMed }
        .filter { vedtaksperioder.overlapper(it) || harMeldeperiode(it) }
        .toList()
}

/**
 *  @return 14-dagers periode som inneholder [dato] og som er i fase med eksisterende meldeperioder på saken,
 *  eller ny periode fra forrige mandag dersom det ikke finnes eksisterende meldeperioder
 *
 * */
fun MeldeperiodeKjeder.finnNærmesteMeldeperiode(dato: LocalDate): Periode {
    require(dato != LocalDate.MIN && dato != LocalDate.MAX) {
        "Dato må være en gyldig dato, ikke MIN eller MAX"
    }

    val eksisterendeFraOgMed = this.firstOrNull()?.periode?.fraOgMed

    if (eksisterendeFraOgMed == null) {
        val førsteMandag = dato.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sisteSøndag = førsteMandag.plusDays(13)
        return Periode(førsteMandag, sisteSøndag)
    }

    val dagerFraFørstePeriode = ChronoUnit.DAYS.between(eksisterendeFraOgMed, dato)
    val periodeForskyvning = Math.floorDiv(dagerFraFørstePeriode, 14L)
    val fraOgMed = eksisterendeFraOgMed.plusDays(periodeForskyvning * 14)
    return Periode(fraOgMed, fraOgMed.plusDays(13))
}

/** Perioden må matche 1-1 med en eksisterende meldeperiode, hvis ikke legger den til en ny. */
private fun MeldeperiodeKjeder.oppdaterEllerLeggTilNy(meldeperiode: Meldeperiode): Pair<MeldeperiodeKjeder, Meldeperiode?> {
    val kjede = hentMeldeperiodeKjedeForPeriode(meldeperiode.periode)
        ?: return run {
            if (meldeperiode.ingenDagerGirRett) {
                // Det finnes ikke noen kjeder fra før, og meldeperioden gir heller ikke noen rett til de dagene. Så vi legger ikke til noe
                this to null
            } else {
                MeldeperiodeKjeder(
                    (this.plus(listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperiode))))).sorted(),
                ) to meldeperiode
            }
        }

    val (oppdatertKjede, oppdatertMeldeperiode) = kjede.leggTilMeldeperiode(meldeperiode)

    return MeldeperiodeKjeder(
        this.map {
            if (it.kjedeId == oppdatertKjede.kjedeId) {
                oppdatertKjede
            } else {
                it
            }
        },
    ) to oppdatertMeldeperiode
}

private fun MeldeperiodeKjeder.oppdaterEllerLeggTilNy(meldeperioder: List<Meldeperiode>): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
    return meldeperioder.fold(this to listOf()) { acc, m ->
        val ny = acc.first.oppdaterEllerLeggTilNy(m)
        Pair(ny.first, listOfNotNull(ny.second).plus(acc.second).sorted())
    }
}
