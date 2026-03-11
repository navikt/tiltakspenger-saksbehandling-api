@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningMedSimulering
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.genererSimuleringFraBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingConsumer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoBehovDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeHenteUtbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import java.time.Clock

class UtbetalingFakeKlient(
    private val sakRepo: SakRepo,
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val clock: Clock = fixedClock,
) : Utbetalingsklient {

    override suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        val response = SendtUtbetaling(
            utbetaling.toUtbetalingRequestDTO(forrigeUtbetalingJson),
            "response - ${utbetaling.id}",
            responseStatus = 202,
        )

        val sak = sakRepo.hentForSakId(utbetaling.sakId)!!

        val harFeilutbetaling = when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeBehandling ->
                sak.hentRammebehandling(utbetaling.beregningKilde.id)?.utbetaling?.simulering.harFeilutbetaling()

            is BeregningKilde.BeregningKildeMeldekort ->
                sak.hentMeldekortBehandling(utbetaling.beregningKilde.id)?.simulering.harFeilutbetaling()
        }

        if (harFeilutbetaling) {
            TilbakekrevingConsumer.consume(
                key = utbetaling.fnr.verdi,
                value = serialize(
                    TilbakekrevingInfoBehovDTO(
                        eksternFagsakId = utbetaling.saksnummer.toString(),
                        hendelseOpprettet = nå(clock),
                        kravgrunnlagReferanse = utbetaling.id.uuidPart(),
                    ),
                ),
                clock = clock,
                tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            )
        }

        return response.right()
    }

    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<KunneIkkeHenteUtbetalingsstatus, Utbetalingsstatus> {
        return Utbetalingsstatus.Ok.right()
    }

    override suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        saksbehandler: String,
        beregning: Beregning,
        brukersNavkontor: Navkontor,
        kanSendeInnHelgForMeldekort: Boolean,
        forrigeUtbetalingJson: String?,
        forrigeUtbetalingId: UtbetalingId?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        val sak = sakRepo.hentForSakId(sakId)!!
        return sak.genererSimuleringFraBeregning(beregning = beregning, clock = clock).right()
    }

    private fun Simulering?.harFeilutbetaling(): Boolean {
        return (this as? Simulering.Endring)?.harFeilutbetaling ?: false
    }
}
