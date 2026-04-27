package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import java.time.LocalDate

/**
 * Representerer en saksbehandler som fyller ut hele meldekortet, godkjenner, lagrer og eventuelt sender til beslutter.
 * Denne flyten vil bli annerledes for veileder og bruker.
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell]
 *
 */
class OppdaterMeldekortbehandlingKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val saksbehandler: Saksbehandler,
    val meldeperioder: NonEmptyList<OppdatertMeldeperiode>,
    val begrunnelse: Begrunnelse?,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val skalSendeVedtaksbrev: Boolean,
    val correlationId: CorrelationId,
) {
    val periode: Periode = Periode(
        meldeperioder.first().first().dag,
        meldeperioder.last().last().dag,
    )

    data class OppdatertMeldeperiode(
        val dager: NonEmptyList<OppdatertDag>,
        val kjedeId: MeldeperiodeKjedeId,
    ) : List<OppdatertDag> by dager {

        data class OppdatertDag(
            val dag: LocalDate,
            val status: Status,
        )

        fun tilUtfyltMeldeperiode(meldeperiode: Meldeperiode): UtfyltMeldeperiode {
            require(meldeperiode.kjedeId == kjedeId)

            return UtfyltMeldeperiode(
                this.map {
                    MeldekortDag(
                        dato = it.dag,
                        status = it.status.tilMeldekortDagStatus(),
                    )
                },
                meldeperiode,
            )
        }
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
        FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
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
            FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU -> MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
            FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
            FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
            IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
            IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
        }
    }
}
