package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager.Dag
import java.time.LocalDate

/**
 * Representerer en saksbehandler som fyller ut hele meldekortet, godkjenner, lagrer og eventuelt sender til beslutter.
 * Denne flyten vil bli annerledes for veileder og bruker.
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [MeldekortBehandlet]
 *
 */
class OppdaterMeldekortKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val saksbehandler: Saksbehandler,
    val dager: Dager,
    val begrunnelse: MeldekortBehandlingBegrunnelse?,
    val correlationId: CorrelationId,
) {
    val periode: Periode = Periode(dager.first().dag, dager.last().dag)

    data class Dager(
        val dager: NonEmptyList<Dag>,
    ) : List<Dag> by dager {
        val antallDager: Int = dager.size

        data class Dag(
            val dag: LocalDate,
            val status: Status,
        )

        fun tilMeldekortDager(maksAntallDagerForPeriode: Int) =
            MeldekortDager(
                this.map { MeldekortDag(dato = it.dag, status = it.status.tilMeldekortDagStatus()) },
                maksAntallDagerForPeriode,
            )
    }

    enum class Status {
        /** Vi tar i mot SPERRET siden det er det saksbehandler ser/sender inn, men vi vil validere at dagen matcher med meldekortutkastet. */
        SPERRET,
        DELTATT_UTEN_LØNN_I_TILTAKET,
        DELTATT_MED_LØNN_I_TILTAKET,
        IKKE_DELTATT,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_VELFERD_GODKJENT_AV_NAV,
        FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
        ;

        fun girRett() = SPERRET != this

        fun tilMeldekortDagStatus() = when (this) {
            SPERRET -> MeldekortDagStatus.SPERRET
            DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
            DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
            IKKE_DELTATT -> MeldekortDagStatus.IKKE_DELTATT
            FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
            FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
            FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
            FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        }
    }
}
