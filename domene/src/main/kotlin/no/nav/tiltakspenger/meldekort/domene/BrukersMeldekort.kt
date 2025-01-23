package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * saksbehandling-api er ikke master for brukers meldekort, så i førsteomgang legger vi ikke på begrensninger i [InnmeldtStatus] her, det må ligge i meldekort-api.
 * Hvis vi ønsker en slik sperre, kan vi legge den i [MeldekortBehandling] eller [MeldeperiodeBeregning]
 *
 * @param id Unik identifikator for denne innsendingen
 * @param meldeperiode En gitt versjon av meldeperioden, slik som den var da bruker sendte inn meldekortet.
 * @param mottatt Tidspunktet mottatt fra bruker
 * @param dager Et innslag per dag i meldeperioden. Må være sortert.
 */
data class BrukersMeldekort(
    val id: MeldekortId,
    val mottatt: LocalDateTime,
    val meldeperiode: Meldeperiode,
    val sakId: SakId,
    val dager: List<BrukersMeldekortDag>,
) {
    val meldeperiodeId: MeldeperiodeId = meldeperiode.id
    val meldeperiodeVersjon: HendelseVersjon = meldeperiode.versjon
    val periode: Periode = meldeperiode.periode

    data class BrukersMeldekortDag(
        val status: InnmeldtStatus,
        val dato: LocalDate,
    )
    init {
        dager.zipWithNext().forEach { (dag, nesteDag) ->
            require(dag.dato.isBefore(nesteDag.dato)) { "Dager må være sortert" }
        }
        require(dager.first().dato == periode.fraOgMed) { "Første dag i meldekortet må være lik første dag i meldeperioden" }
        require(dager.last().dato == periode.tilOgMed) { "Siste dag i meldekortet må være lik siste dag i meldeperioden" }
        require(dager.size.toLong() == periode.antallDager) { "Antall dager i meldekortet må være lik antall dager i meldeperioden" }
    }
}

enum class InnmeldtStatus {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_REGISTRERT,
}
