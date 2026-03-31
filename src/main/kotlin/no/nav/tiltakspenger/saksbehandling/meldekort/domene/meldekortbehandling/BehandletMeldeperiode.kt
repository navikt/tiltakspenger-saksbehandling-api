package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDager
import java.time.LocalDate

data class BehandletMeldeperiode(
    val dager: MeldekortDager,
    val meldeperiode: Meldeperiode,
    /** Pdd har kun automatiske behandlinger tilknyttet et brukers meldekort */
    val brukersMeldekort: BrukersMeldekort?,
) {
    val kjedeId: MeldeperiodeKjedeId = meldeperiode.kjedeId

    val periode: Periode = meldeperiode.periode

    val fraOgMed: LocalDate = periode.fraOgMed
    val tilOgMed: LocalDate = periode.tilOgMed

    init {
        require(dager.meldeperiode == meldeperiode) {
            "Utfylte dager må tilhøre samme meldeperiode som behandlingens meldeperiode"
        }
    }
}

data class BehandledeMeldeperioder(
    private val meldeperioder: NonEmptyList<BehandletMeldeperiode>,
) : List<BehandletMeldeperiode> by meldeperioder {

    val fraOgMed: LocalDate = this.first().fraOgMed
    val tilOgMed: LocalDate = this.last().tilOgMed

    val totalPeriode: Periode = Periode(fraOgMed, tilOgMed)

    val rammevedtak: NonEmptyList<VedtakId> by lazy {
        this.flatMap { it.meldeperiode.rammevedtak.verdier }.distinct().toNonEmptyListOrThrow()
    }

    val ingenDagerGirRett: Boolean by lazy { this.all { it.meldeperiode.ingenDagerGirRett } }

    fun oppdaterMeldeperioder(
        oppdaterteKjeder: MeldeperiodeKjeder,
    ): BehandledeMeldeperioder {
        return this.map {
            val sisteMeldeperiode = oppdaterteKjeder.hentSisteMeldeperiodeForKjede(it.kjedeId)

            if (sisteMeldeperiode.versjon < it.meldeperiode.versjon) {
                it
            } else {
                it.copy(
                    meldeperiode = sisteMeldeperiode,
                    dager = sisteMeldeperiode.tilMeldekortDager(),
                )
            }
        }.let { BehandledeMeldeperioder(it.toNonEmptyListOrThrow()) }
    }

    init {
        require(
            meldeperioder.size == 1 || meldeperioder.zipWithNext().all { (a, b) ->
                a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed
            },
        ) { "Meldeperiodene for en behandling må være sammenhengende og sortert" }
    }
}
