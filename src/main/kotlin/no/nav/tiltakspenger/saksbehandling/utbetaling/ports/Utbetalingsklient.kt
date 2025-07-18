package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeHenteUtbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak

interface Utbetalingsklient {
    suspend fun iverksett(
        vedtak: Utbetalingsvedtak,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling>

    suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<KunneIkkeHenteUtbetalingsstatus, Utbetalingsstatus>

    suspend fun simuler(
        behandling: MeldekortBehandling,
        brukersNavkontor: Navkontor,
        forrigeUtbetalingJson: String?,
        forrigeVedtakId: VedtakId?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata>
}

class KunneIkkeUtbetale(
    val request: String? = null,
    val response: String? = null,
    val responseStatus: Int? = null,
)

data class SendtUtbetaling(
    val request: String,
    val response: String,
    val responseStatus: Int,
)
