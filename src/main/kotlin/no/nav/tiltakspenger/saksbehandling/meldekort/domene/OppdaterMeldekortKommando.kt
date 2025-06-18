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
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [MeldekortBehandletManuelt]
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
        data class Dag(
            val dag: LocalDate,
            val status: Status,
        )

        fun tilMeldekortDager(meldeperiode: Meldeperiode) =
            MeldekortDager(
                this.map { MeldekortDag(dato = it.dag, status = it.status.tilMeldekortDagStatus()) },
                meldeperiode,
            )
    }

    /** En spesialisering av [MeldekortDagStatus].
     * Skal kun brukes i kontrakten mot frontend.
     * Dette er de verdiene saksbehandler kan velge. Se egen kommentar for [IKKE_RETT_TIL_TILTAKSPENGER].
     * Merk at vi ikke ønsker IKKE_BESVART i denne listen, da dette kun er et implisitt valg for bruker. Saksbehandler må ta stilling til alle dagene.
     * */
    enum class Status {
        DELTATT_UTEN_LØNN_I_TILTAKET,
        DELTATT_MED_LØNN_I_TILTAKET,
        FRAVÆR_SYK,
        FRAVÆR_SYKT_BARN,
        FRAVÆR_GODKJENT_AV_NAV,
        FRAVÆR_ANNET,
        IKKE_TILTAKSDAG,

        /** Vi tar i mot [IKKE_RETT_TIL_TILTAKSPENGER] siden det er det saksbehandler ser/sender inn, men vi vil validere at dagen matcher med meldekortutkastet. */
        IKKE_RETT_TIL_TILTAKSPENGER,
        ;

        fun girRett() = IKKE_RETT_TIL_TILTAKSPENGER != this

        fun tilMeldekortDagStatus() = when (this) {
            DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
            DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
            FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
            FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
            FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
            FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
            IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
            IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
        }
    }
}
