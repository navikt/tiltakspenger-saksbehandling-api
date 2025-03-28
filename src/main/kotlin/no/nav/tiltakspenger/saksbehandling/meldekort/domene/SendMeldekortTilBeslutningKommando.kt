package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Dager.Dag
import java.time.LocalDate

/**
 * Representerer en saksbehandler som fyller ut hele meldekortet, godkjenner og sender til beslutter.
 * Denne flyten vil bli annerledes for veileder og bruker.
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling.MeldekortBehandlet]
 *
 */
class SendMeldekortTilBeslutningKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val saksbehandler: Saksbehandler,
    val dager: Dager,
    val correlationId: CorrelationId,
    val meldekortbehandlingBegrunnelse: MeldekortbehandlingBegrunnelse?,
) {
    val periode: Periode = Periode(dager.first().dag, dager.last().dag)

    data class Dager(
        val dager: NonEmptyList<Dag>,
    ) : List<Dag> by dager {
        val antallDager: Int = dager.size
        val antallDagerMedFraværEllerDeltatt: Int = dager.count { it.status.deltattEllerFravær() }

        data class Dag(
            val dag: LocalDate,
            val status: Status,
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

        fun erFravær(): Boolean {
            return when (this) {
                FRAVÆR_SYK, FRAVÆR_SYKT_BARN, FRAVÆR_VELFERD_GODKJENT_AV_NAV, FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> true
                SPERRET, DELTATT_UTEN_LØNN_I_TILTAKET, DELTATT_MED_LØNN_I_TILTAKET, IKKE_DELTATT -> false
            }
        }

        fun harDeltatt(): Boolean {
            return when (this) {
                DELTATT_UTEN_LØNN_I_TILTAKET, DELTATT_MED_LØNN_I_TILTAKET -> true
                SPERRET, IKKE_DELTATT, FRAVÆR_SYK, FRAVÆR_SYKT_BARN, FRAVÆR_VELFERD_GODKJENT_AV_NAV, FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> false
            }
        }

        fun deltattEllerFravær(): Boolean {
            return when (this) {
                DELTATT_UTEN_LØNN_I_TILTAKET, DELTATT_MED_LØNN_I_TILTAKET, FRAVÆR_SYK, FRAVÆR_SYKT_BARN, FRAVÆR_VELFERD_GODKJENT_AV_NAV, FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> true
                SPERRET, IKKE_DELTATT -> false
            }
        }

        fun girRett() = SPERRET != this
    }
}
