package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * saksbehandling-api er ikke master for brukers meldekort, så i førsteomgang legger vi ikke på begrensninger i [InnmeldtStatus] her, det må ligge i meldekort-api.
 * Hvis vi ønsker en slik sperre, kan vi legge den i [MeldekortBehandling] eller [MeldekortDager]
 *
 * @param id Unik identifikator for denne utfyllingen/innsendingen.
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
    val journalpostId: JournalpostId,
    val oppgaveId: OppgaveId?,
    val behandlesAutomatisk: Boolean,
    val behandletTidspunkt: LocalDateTime?,
) {
    val kjedeId: MeldeperiodeKjedeId = meldeperiode.kjedeId
    val meldeperiodeId: MeldeperiodeId = meldeperiode.id
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

    fun tilMeldekortDager(): MeldekortDager {
        return MeldekortDager(
            maksAntallDagerForPeriode = meldeperiode.antallDagerSomGirRett,
            verdi = dager.map {
                MeldekortDag(
                    dato = it.dato,
                    status = it.status.tilMeldekortDagStatus(),
                )
            },
        )
    }
}

enum class InnmeldtStatus {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_REGISTRERT,
    IKKE_DELTATT,
    IKKE_RETT_TIL_TILTAKSPENGER,
    ;

    fun tilMeldekortDagStatus(): MeldekortDagStatus = when (this) {
        DELTATT -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        IKKE_REGISTRERT -> MeldekortDagStatus.IKKE_DELTATT
        IKKE_DELTATT -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.SPERRET
    }
}
