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
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtaksliste
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class MeldeperiodeKjeder(
    private val meldeperiodeKjeder: List<MeldeperiodeKjede>,
) : List<MeldeperiodeKjede> by meldeperiodeKjeder {

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
            require(a.periode.fraOgMed <= b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sortert på periode - ${a.kjedeId} og ${b.kjedeId} var i feil rekkefølge (sak ${a.sakId})"
            }
            require(a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sammenhengende - feilet for ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})"
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

    fun oppdaterMedNyStansperiode(periode: Periode): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
        return meldeperiodeKjeder.associate {
            it.oppdaterMedNyStansperiode(periode)
        }.let {
            MeldeperiodeKjeder(it.keys.toList()) to it.values.toList().filterNotNull()
        }
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        val meldeperiodeKjede = meldeperiodeKjeder.single { it.kjedeId == meldeperiode.kjedeId }
        return meldeperiode == meldeperiodeKjede.last()
    }

    fun oppdaterEllerLeggTilNy(meldeperiode: Meldeperiode): Pair<MeldeperiodeKjeder, Meldeperiode?> {
        val kjede = hentMeldeperiodeKjedeForPeriode(meldeperiode.periode)
            ?: return MeldeperiodeKjeder(
                (this.meldeperiodeKjeder.plus(listOf(MeldeperiodeKjede(nonEmptyListOf(meldeperiode))))).sorted(),
            ) to meldeperiode

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

//    fun genererMeldeperioder(vedtaksliste: Vedtaksliste): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
//        if (meldeperioder.isEmpty()) {
//            return genererMeldeperioder(vedtaksliste)
//        }
//        TODO()
//    }

    fun genererMeldeperioder(
        vedtaksliste: Vedtaksliste,
    ): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
        if (vedtaksliste.isEmpty()) {
            require(this.isEmpty()) { "Forventet ingen meldeperioder ved tom vedtaksliste" }
            return this to emptyList()
        }
        val innvilgelsesperioder = vedtaksliste.innvilgelsesperioder
        val vedtaksperiode = innvilgelsesperioder.first().fraOgMed
        var nærmesteMeldeperiode = Companion.finnNærmesteMeldeperiode(vedtaksperiode)

        val potensielleNyeMeldeperioder = mutableListOf<Meldeperiode>()

        // før eller samme dag
        while (!nærmesteMeldeperiode.etter(innvilgelsesperioder.last().tilOgMed)) {
            if (!innvilgelsesperioder.overlapper(nærmesteMeldeperiode) && !this.harMeldeperiode(nærmesteMeldeperiode)) {
                // hvis perioden ikke overlapper, og den ikke finnes fra før, så skal ikke vi oppdatere noe
                continue
            }

            val kjede = this.hentMeldeperiodeKjedeForPeriode(nærmesteMeldeperiode)
            val versjon = kjede?.nesteVersjon() ?: HendelseVersjon.ny()
            // Hvis den er lik den forrige meldeperioden, blir den ikke lagt til
            val potensiellNyMeldeperiode = Meldeperiode.opprettMeldeperiode(
                periode = nærmesteMeldeperiode,
                utfallsperioder = vedtaksliste.utfallForPeriode(nærmesteMeldeperiode),
                fnr = vedtaksliste.fnr!!,
                saksnummer = vedtaksliste.saksnummer!!,
                sakId = vedtaksliste.sakId!!,
                antallDagerForPeriode = vedtaksliste.hentAntallDager(nærmesteMeldeperiode)!!,
                versjon = versjon,
            )
            potensielleNyeMeldeperioder.add(potensiellNyMeldeperiode)
            nærmesteMeldeperiode = nærmesteMeldeperiode.nesteMeldeperiode()
        }
        return this.oppdaterEllerLeggTilNy(potensielleNyeMeldeperioder)
    }

    // TODO - test
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

    companion object {
        /**
         * Skal kun kalles/brukes på en sak som aldri har hatt en meldeperiode før
         */
//        fun genererMeldeperioder(
//            vedtaksliste: Vedtaksliste,
//            fnr: Fnr,
//            saksnummer: Saksnummer,
//            sakId: SakId,
//        ): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
//            val innvilgelsesperioder = vedtaksliste.innvilgelsesperioder
//            val vedtaksperiode = innvilgelsesperioder.first().fraOgMed
//            var nærmesteMeldeperiode = finnNærmesteMeldeperiode(vedtaksperiode)
//
//            val mutMeldePeriodeList: MutableList<Meldeperiode> = mutableListOf()
//
//            // før eller samme dag
//            while (!nærmesteMeldeperiode.etter(innvilgelsesperioder.last().tilOgMed)) {
//                if (!innvilgelsesperioder.overlapper(nærmesteMeldeperiode)) {
//                    // Dette vil bare fungere hvis det ikke finnes noen meldeperioder fra før
//                    nærmesteMeldeperiode = nærmesteMeldeperiode.nesteMeldeperiode()
//                    continue
//                }
//
//                mutMeldePeriodeList.add(
//                    Meldeperiode.opprettMeldeperiode(
//                        periode = nærmesteMeldeperiode,
//                        utfallsperioder = vedtaksliste.utfallsperioder.krymp(nærmesteMeldeperiode),
//                        fnr = fnr,
//                        saksnummer = saksnummer,
//                        sakId = sakId,
//                        antallDagerForPeriode = vedtaksliste.hentAntallDager(nærmesteMeldeperiode)!!,
//                    ),
//                )
//                nærmesteMeldeperiode = nærmesteMeldeperiode.nesteMeldeperiode()
//            }
//            return MeldeperiodeKjeder(listOf(MeldeperiodeKjede(mutMeldePeriodeList.toNonEmptyListOrNull()!!))) to mutMeldePeriodeList
//        }

        fun List<Periode>.overlapper(periode: Periode): Boolean = this.any { it.overlapperMed(periode) }

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

// fun Sak.opprettManglendeMeldeperioder(rammevedtak: Rammevedtak): Pair<Sak, List<Meldeperiode>> {
//    requireNotNull(this.vedtaksliste.førstegangsvedtak) { "Kan ikke opprette første meldeperiode uten førstegangsvedtak" }
//    requireNotNull(this.vedtaksliste.innvilgelsesperioder) { "Kan ikke opprette første meldeperiode uten minst én periode som gir rett til tiltakspenger" }
//    require(this.vedtaksliste.any { it.id == rammevedtak.id }) { "Sak må inneholde rammevedtak vi skal oppette meldeperiode for" }
//
//    if (this.meldeperiodeKjeder.isEmpty()) {
//        return opprettFørsteMeldeperiode(rammevedtak)
//    }
//    return appendNyMeldeperiode(rammevedtak)
// }

// private fun Sak.appendNyMeldeperiode(rammevedtak: Rammevedtak): Pair<Sak, List<Meldeperiode>> {
//    require(this.meldeperiodeKjeder.periode!!.overlapperMed(rammevedtak.periode)) {
//        "Kan ikke legge til ny periode som overlapper med eksisterende periode"
//    }
//    val periode = this.finnNærmesteMeldeperiode(rammevedtak.periode.fraOgMed)
//
//    if (this.meldeperiodeKjeder.periode.fraOgMed.isAfter(periode.tilOgMed)) {
//        val mutList = mutableListOf<Meldeperiode>()
//        var mutPeriode = periode
//
//        while (mutPeriode.overlapperMed(rammevedtak.periode)) {
//            val utfallsperioder = this.vedtaksliste.utfallsperioder.krymp(mutPeriode)
//            val nyMeldeperiode = Meldeperiode.opprettMeldeperiode(
//                periode = mutPeriode,
//                utfallsperioder = utfallsperioder,
//                fnr = fnr,
//                saksnummer = saksnummer,
//                sakId = rammevedtak.sakId,
//                antallDagerForPeriode = rammevedtak.antallDagerPerMeldeperiode,
//            )
//            mutList.add(nyMeldeperiode)
//            mutPeriode = mutPeriode.nesteMeldeperiode()
//        }
//
//        val sak = this.copy(
//            meldeperiodeKjeder = this.meldeperiodeKjeder.oppdaterEllerLeggTilNy(mutList),
//        )
//        return sak to mutList
//    }
//
//    val utfallsperioder = this.vedtaksliste.utfallsperioder.krymp(periode)
//
//    val nyMeldeperiode = Meldeperiode.opprettMeldeperiode(
//        periode = periode,
//        utfallsperioder = utfallsperioder,
//        fnr = fnr,
//        saksnummer = saksnummer,
//        sakId = rammevedtak.sakId,
//        antallDagerForPeriode = rammevedtak.antallDagerPerMeldeperiode,
//    )
//
//    val sak = this.copy(
//        meldeperiodeKjeder = this.meldeperiodeKjeder.oppdaterEllerLeggTilNy(nyMeldeperiode),
//    )
//
//    return sak to listOf(nyMeldeperiode)
// }
