package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldeperiode(
    val id: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val versjon: HendelseVersjon,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val maksAntallDagerForMeldeperiode: Int,
    val girRett: Map<LocalDate, Boolean>,
    val rammevedtak: Periodisering<VedtakId>?,
) : Comparable<Meldeperiode> {
    val antallDagerSomGirRett = girRett.values.count { it }
    val ingenDagerGirRett = antallDagerSomGirRett == 0

    fun girIngenDagerRett(): Boolean {
        return girRett.values.toList().all { !it }
    }

    // TODO Anders: når skal vi tillate at meldekortet fylles ut? Siste fredag i perioden?
    fun erKlarTilUtfylling(clock: Clock): Boolean {
        return periode.fraOgMed <= LocalDate.now(clock)
    }

    fun erLik(meldeperiode: Meldeperiode): Boolean {
        /**
         * Må oppdaters dersom det kommer nytt felt som vi har lyst å sammenligne på,
         * men er bedre at det ikke opprettes nye meldeperioder
         */
        return this.kjedeId == meldeperiode.kjedeId && this.sakId == meldeperiode.sakId && this.saksnummer == meldeperiode.saksnummer && this.fnr == meldeperiode.fnr && this.periode == meldeperiode.periode && this.maksAntallDagerForMeldeperiode == meldeperiode.maksAntallDagerForMeldeperiode && this.girRett == meldeperiode.girRett
    }

    override fun compareTo(other: Meldeperiode): Int {
        require(!this.periode.overlapperMed(other.periode)) { "Meldeperiodene kan ikke overlappe" }
        return this.periode.fraOgMed.compareTo(other.periode.fraOgMed)
    }

    init {
        if (ingenDagerGirRett) {
            require(maksAntallDagerForMeldeperiode == 0) { "Dersom ingen dager gir rett, må antallDagerForPeriode være 0" }
        }
        require(maksAntallDagerForMeldeperiode <= antallDagerSomGirRett) {
            """
            Antall dager som gir rett kan ikke være mindre enn antall dager for periode
                antallDagerForPeriode: $maksAntallDagerForMeldeperiode
                antallDagerSomGirRett: $antallDagerSomGirRett
            """.trimIndent()
        }
    }

    companion object {
        fun opprettMeldeperiode(
            periode: Periode,
            utfallsperioder: Periodisering<Utfallsperiode>,
            fnr: Fnr,
            saksnummer: Saksnummer,
            sakId: SakId,
            antallDagerForPeriode: Int,
            versjon: HendelseVersjon = HendelseVersjon.ny(),
            rammevedtak: Periodisering<VedtakId>,
            clock: Clock,
        ): Meldeperiode {
            val meldeperiode = Meldeperiode(
                kjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                id = MeldeperiodeId.random(),
                fnr = fnr,
                saksnummer = saksnummer,
                sakId = sakId,
                maksAntallDagerForMeldeperiode = antallDagerForPeriode,
                periode = periode,
                opprettet = nå(clock),
                versjon = versjon,
                girRett = periode.tilDager().associateWith {
                    (utfallsperioder.hentVerdiForDag(it) == Utfallsperiode.RETT_TIL_TILTAKSPENGER)
                },
                rammevedtak = rammevedtak,
            )

            return meldeperiode
        }
    }
}

fun Periode.validerMeldeperiode() {
    require(this.antallDager == 14L) { "En meldeperiode må være 14 dager, men var ${this.antallDager}" }
    require(this.fraOgMed.dayOfWeek == DayOfWeek.MONDAY) { "Meldeperioden må starte på en mandag" }
    require(this.tilOgMed.dayOfWeek == DayOfWeek.SUNDAY) { "Meldeperioden må slutte på en søndag" }
}
