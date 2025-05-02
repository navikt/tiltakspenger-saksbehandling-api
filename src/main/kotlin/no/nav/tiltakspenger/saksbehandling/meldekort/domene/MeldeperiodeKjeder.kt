package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.overlapperIkke
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling.Companion.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

data class MeldeperiodeKjeder(
    private val meldeperiodeKjeder: List<MeldeperiodeKjede>,
) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    constructor(meldeperiodeKjede: MeldeperiodeKjede) : this(listOf(meldeperiodeKjede))

    val sakId: SakId? = meldeperiodeKjeder.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: Saksnummer? = meldeperiodeKjeder.map { it.saksnummer }.distinct().singleOrNullOrThrow()
    val fnr: Fnr? = meldeperiodeKjeder.map { it.fnr }.distinct().singleOrNullOrThrow()

    init {
        meldeperiodeKjeder.flatten().nonDistinctBy { it.id }.also {
            require(it.isEmpty()) {
                "Meldeperiodekjedene har duplikate meldeperioder - $it"
            }
        }

        meldeperiodeKjeder.zipWithNext { a, b ->
            require(a.kjedeId != b.kjedeId) {
                "Meldeperiodekjedene kan ikke ha samme kjedeId - ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})"
            }
            // 2 kjeder kan ikke ha samme meldeperiode. Og de må være sortert på periode.
            require(a.periode.tilOgMed < b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sortert på periode - ${a.kjedeId} og ${b.kjedeId} var i feil rekkefølge (sak ${a.sakId})"
            }
            require(ChronoUnit.DAYS.between(a.periode.fraOgMed, b.periode.fraOgMed) % 14 == 0L) {
                """
                    Meldeperiodekjedene må følge meldesyklus - feilet for ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})
                        Første periode: ${a.periode}.
                        Neste periode: ${b.periode}.
                        Antall dager mellom: ${ChronoUnit.DAYS.between(a.periode.fraOgMed, b.periode.fraOgMed)}.
                """.trimIndent()
            }
        }
    }

    /** Siste versjon av meldeperiodene */
    val meldeperioder: List<Meldeperiode> get() = this.map { it.last() }

    val periode: Periode? = meldeperiodeKjeder.map { it.periode }.let {
        if (it.isEmpty()) {
            null
        } else {
            Periode(it.first().fraOgMed, it.last().tilOgMed)
        }
    }

    /**
     * @throws NoSuchElementException hvis det ikke finnes noen meldeperioder
     */
    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperiodeKjeder.last().hentSisteMeldeperiode()
    }

    fun hentMeldeperiode(id: MeldeperiodeId): Meldeperiode? {
        return meldeperiodeKjeder.asSequence().flatten().find { it.id == id }
    }

    fun hentMeldeperiode(periode: Periode): Meldeperiode? {
        return meldeperiodeKjeder.singleOrNullOrThrow {
            it.periode == periode
        }?.hentSisteMeldeperiode()
    }

    fun hentMeldeperiodeKjedeForPeriode(periode: Periode): MeldeperiodeKjede? {
        return meldeperiodeKjeder.singleOrNullOrThrow { it.periode == periode }
    }

    fun harMeldeperiode(periode: Periode): Boolean = hentMeldeperiode(periode) != null

    fun hentSisteMeldeperiodeForKjedeId(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.single { it.kjedeId == kjedeId }.last()
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return hentSisteMeldeperiodeForKjedeId(kjedeId)
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        val meldeperiodeKjede = meldeperiodeKjeder.single { it.kjedeId == meldeperiode.kjedeId }
        return meldeperiode == meldeperiodeKjede.last()
    }

    fun oppdaterEllerLeggTilNy(meldeperiode: Meldeperiode): Pair<MeldeperiodeKjeder, Meldeperiode?> {
        val kjede = hentMeldeperiodeKjedeForPeriode(meldeperiode.periode)
            ?: return run {
                if (meldeperiode.ingenDagerGirRett) {
                    // Det finnes ikke noen kjeder fra får, og meldeperioden gir heller ikke noen rett på de dagene. Så vi legger ikke til noe
                    this to null
                } else {
                    MeldeperiodeKjeder(
                        (this.meldeperiodeKjeder.plus(listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperiode))))).sorted(),
                    ) to meldeperiode
                }
            }

        val (oppdatertKjede, oppdatertMeldeperiode) = kjede.leggTilMeldeperiode(meldeperiode)

        return MeldeperiodeKjeder(
            this.meldeperiodeKjeder.map {
                if (it.kjedeId == oppdatertKjede.kjedeId) {
                    oppdatertKjede
                } else {
                    it
                }
            },
        ) to oppdatertMeldeperiode
    }

    fun oppdaterEllerLeggTilNy(meldeperioder: List<Meldeperiode>): Pair<MeldeperiodeKjeder, List<Meldeperiode>> =
        meldeperioder.fold(this to listOf()) { acc, m ->
            val ny = acc.first.oppdaterEllerLeggTilNy(m)
            Pair(ny.first, listOfNotNull(ny.second).plus(acc.second).sorted())
        }

    fun genererMeldeperioder(
        vedtaksliste: Vedtaksliste,
        ikkeGenererEtter: LocalDate,
        clock: Clock,
    ): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
        if (vedtaksliste.isEmpty()) {
            require(this.isEmpty()) { "Forventet ingen meldeperioder ved tom vedtaksliste" }
            return this to emptyList()
        }
        val vedtaksperioder = vedtaksliste.vedtaksperioder
        val førsteFraOgMed = vedtaksperioder.first().fraOgMed
        var nærmesteMeldeperiode = finnNærmesteMeldeperiode(førsteFraOgMed)

        val potensielleNyeMeldeperioder = mutableListOf<Meldeperiode>()

        // før eller samme dag
        while (!nærmesteMeldeperiode.etter(vedtaksperioder.last().tilOgMed) &&
            !ikkeGenererEtter.isBefore(nærmesteMeldeperiode.tilOgMed)
        ) {
            if (vedtaksperioder.overlapperIkke(nærmesteMeldeperiode) && !this.harMeldeperiode(nærmesteMeldeperiode)) {
                // hvis perioden ikke overlapper, og den ikke finnes fra før, så skal ikke vi oppdatere noe
                continue
            }

            val utfallsperioder = vedtaksliste.utfallForPeriode(nærmesteMeldeperiode)
            val utfallsperiodeCount = nærmesteMeldeperiode.tilDager().count {
                (utfallsperioder.hentVerdiForDag(it) == Utfallsperiode.RETT_TIL_TILTAKSPENGER)
            }
            val antallDagerSomGirRettForMeldePeriode =
                min(utfallsperiodeCount, MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)

            val kjede = this.hentMeldeperiodeKjedeForPeriode(nærmesteMeldeperiode)
            val versjon = kjede?.nesteVersjon() ?: HendelseVersjon.ny()
            // Hvis den er lik den forrige meldeperioden, blir den ikke lagt til
            val potensiellNyMeldeperiode = Meldeperiode.opprettMeldeperiode(
                periode = nærmesteMeldeperiode,
                utfallsperioder = utfallsperioder,
                fnr = vedtaksliste.fnr!!,
                saksnummer = vedtaksliste.saksnummer!!,
                sakId = vedtaksliste.sakId!!,
                antallDagerForPeriode = antallDagerSomGirRettForMeldePeriode,
                versjon = versjon,
                rammevedtak = vedtaksliste.vedtakForPeriode(nærmesteMeldeperiode),
                clock = clock,
            )
            potensielleNyeMeldeperioder.add(potensiellNyMeldeperiode)
            nærmesteMeldeperiode = nærmesteMeldeperiode.nesteMeldeperiode()
        }
        return this.oppdaterEllerLeggTilNy(potensielleNyeMeldeperioder)
    }

    fun finnNærmesteMeldeperiode(dato: LocalDate): Periode {
        if (this.isEmpty()) {
            return Companion.finnNærmesteMeldeperiode(dato)
        }

        val førstePeriode = this.meldeperiodeKjeder.first().periode

        if (dato.isBefore(førstePeriode.fraOgMed)) {
            var periode = førstePeriode
            while (true) {
                periode = periode.forrigeMeldeperiode()
                if (periode.inneholder(dato)) {
                    return periode
                }
            }
        } else if (dato.isAfter(førstePeriode.tilOgMed)) {
            var periode = førstePeriode
            while (true) {
                periode = periode.nesteMeldeperiode()
                if (periode.inneholder(dato)) {
                    return periode
                }
            }
        }

        return førstePeriode
    }

    fun hentMeldeperiodekjedeForKjedeId(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKjede? {
        return meldeperiodeKjeder.singleOrNullOrThrow { it.kjedeId == kjedeId }
    }

    fun hentForegåendeMeldeperiodekjede(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKjede? {
        meldeperiodeKjeder.zipWithNext { a, b -> if (b.kjedeId == kjedeId) return a }
        return null
    }

    companion object {
        /**
         * Skal kun kalles/brukes på en sak som aldri har hatt en meldeperiode før
         */
        fun finnNærmesteMeldeperiode(dato: LocalDate): Periode {
            val førsteMandagIMeldekortsperiode = dato.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sisteSøndagIMeldekortsperiode = førsteMandagIMeldekortsperiode.plusDays(13)
            return Periode(førsteMandagIMeldekortsperiode, sisteSøndagIMeldekortsperiode)
        }

        fun fraMeldeperioder(meldeperioder: List<Meldeperiode>): MeldeperiodeKjeder {
            return meldeperioder
                .groupBy { it.kjedeId }
                .values.mapNotNull { meldeperioderForKjede ->
                    meldeperioderForKjede
                        .sortedBy { it.versjon }
                        .toNonEmptyListOrNull()
                        ?.let { MeldeperiodeKjede(it) }
                }
                .sortedBy { it.periode.fraOgMed }
                .let { MeldeperiodeKjeder(it) }
        }
    }
}

fun Periode.forrigeMeldeperiode(): Periode = Periode(
    this.fraOgMed.minusDays(14),
    this.tilOgMed.minusDays(14),
)

fun Periode.nesteMeldeperiode(): Periode = Periode(
    this.fraOgMed.plusDays(14),
    this.tilOgMed.plusDays(14),
)
