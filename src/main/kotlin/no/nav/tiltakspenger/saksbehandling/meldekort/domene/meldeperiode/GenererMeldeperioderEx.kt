package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
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
import kotlin.math.min

typealias OppdaterteKjederOgMeldeperioder = Pair<MeldeperiodeKjeder, List<Meldeperiode>>

enum class GenererMeldeperioderFeil {
    BehandlingManglerVedtaksperiode,
    FeilKallForVedtattBehandling,
    HarMeldeperioderUtenVedtaksperioder,

    /** Den angitte datoen er ikke en gyldig dato (MIN eller MAX). */
    UgyldigDato,
}

fun MeldeperiodeKjeder.genererMeldeperioderOgOppdaterKjeder(
    vedtaksliste: Rammevedtaksliste,
    clock: Clock,
): Either<GenererMeldeperioderFeil, OppdaterteKjederOgMeldeperioder> {
    if (vedtaksliste.isEmpty()) {
        if (this.isNotEmpty()) {
            return GenererMeldeperioderFeil.HarMeldeperioderUtenVedtaksperioder.left()
        }
        return (this to emptyList<Meldeperiode>()).right()
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
    ).map {
        this.oppdaterEllerLeggTilNy(it)
    }
}

/**
 *  Genererer hypotetiske meldeperioder ut fra en ikke-vedtatt behandling.
 *
 *  Skal kun benyttes for validering/testing, ikke for å generere faktiske meldeperioder som persisteres
 * */
fun Sak.genererMeldeperioderForValidering(
    rammebehandling: Rammebehandling,
    clock: Clock,
): Either<GenererMeldeperioderFeil, List<Meldeperiode>> {
    val vedtaksperiode = rammebehandling.vedtaksperiode
        ?: return GenererMeldeperioderFeil.BehandlingManglerVedtaksperiode.left()

    if (rammebehandling.status == Rammebehandlingsstatus.VEDTATT) {
        return GenererMeldeperioderFeil.FeilKallForVedtattBehandling.left()
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
                // Overskriver den delen som overlapper behandlingen med den nye (midlertidige) vedtakId-en, og beholder eventuelle vedtak fra saken for resten av perioden.
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

private fun MeldeperiodeKjeder.genererMeldeperioder(
    vedtaksperioder: List<Periode>,
    hentAntallDager: (periode: Periode) -> AntallDagerForMeldeperiode?,
    hentVedtakIder: (periode: Periode) -> IkkeTomPeriodisering<VedtakId>,
    harRett: (dato: LocalDate) -> Boolean,
    fnr: Fnr,
    saksnummer: Saksnummer,
    sakId: SakId,
    clock: Clock,
): Either<GenererMeldeperioderFeil, List<Meldeperiode>> {
    return this
        .genererPerioder(vedtaksperioder)
        .map { perioder ->
            perioder.map { periode ->
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
}

/**
 * @return Alle 14-dagers perioder som overlapper med [vedtaksperioder] eller eksisterende meldeperioder
 *
 * */
private fun MeldeperiodeKjeder.genererPerioder(
    vedtaksperioder: List<Periode>,
): Either<GenererMeldeperioderFeil, List<Periode>> {
    val totalPeriode = vedtaksperioder.let {
        Periode(it.first().fraOgMed, it.last().tilOgMed)
    }

    return finnNærmesteMeldeperiode(totalPeriode.fraOgMed).map { førstePeriode ->
        generateSequence(førstePeriode) { forrige ->
            val nesteFraOgMed = forrige.tilOgMed.plusDays(1)
            Periode(nesteFraOgMed, nesteFraOgMed.plusDays(13))
        }
            .takeWhile { it.fraOgMed <= totalPeriode.tilOgMed }
            .filter { vedtaksperioder.overlapper(it) || harMeldeperiode(it) }
            .toList()
    }
}

/**
 *  @return 14-dagers periode som inneholder [dato] og som er i fase med eksisterende meldeperioder på saken, eller ny periode fra forrige mandag dersom det ikke finnes eksisterende meldeperioder
 *
 * */
fun MeldeperiodeKjeder.finnNærmesteMeldeperiode(dato: LocalDate): Either<GenererMeldeperioderFeil, Periode> {
    if (dato == LocalDate.MIN || dato == LocalDate.MAX) {
        return GenererMeldeperioderFeil.UgyldigDato.left()
    }

    val eksisterendeFraOgMed = this.firstOrNull()?.periode?.fraOgMed

    if (eksisterendeFraOgMed == null) {
        val førsteMandag = dato.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sisteSøndag = førsteMandag.plusDays(13)
        return Periode(førsteMandag, sisteSøndag).right()
    }

    val dagerFraFørstePeriode = ChronoUnit.DAYS.between(eksisterendeFraOgMed, dato)
    val periodeForskyvning = Math.floorDiv(dagerFraFørstePeriode, 14L)
    val fraOgMed = eksisterendeFraOgMed.plusDays(periodeForskyvning * 14)
    return Periode(fraOgMed, fraOgMed.plusDays(13)).right()
}

private fun MeldeperiodeKjeder.oppdaterEllerLeggTilNy(meldeperioder: List<Meldeperiode>): OppdaterteKjederOgMeldeperioder {
    return meldeperioder.fold(this to listOf()) { acc, m ->
        val ny = acc.first.oppdaterEllerLeggTilNy(m)
        Pair(ny.first, listOfNotNull(ny.second).plus(acc.second).sorted())
    }
}

/** Perioden må matche 1-1 med en eksisterende meldeperiode, hvis ikke legger den til en ny. */
private fun MeldeperiodeKjeder.oppdaterEllerLeggTilNy(meldeperiode: Meldeperiode): Pair<MeldeperiodeKjeder, Meldeperiode?> {
    val kjede = hentMeldeperiodeKjedeForPeriode(meldeperiode.periode)
        ?: return run {
            if (meldeperiode.ingenDagerGirRett) {
                // Det finnes ikke noen kjeder fra før, og meldeperioden gir heller ikke noen rett til de dagene.
                // Så vi legger ikke til noe
                this to null
            } else {
                MeldeperiodeKjeder(
                    this.plus(listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperiode)))).sorted(),
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
