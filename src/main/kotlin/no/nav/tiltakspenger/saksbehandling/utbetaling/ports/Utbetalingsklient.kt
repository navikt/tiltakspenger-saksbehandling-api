package no.nav.tiltakspenger.saksbehandling.utbetaling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.rawResponseString
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling

interface Utbetalingsklient {
    suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling>

    suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<HttpKlientError, Utbetalingsstatus>

    suspend fun simuler(
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
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata>
}

sealed interface UtbetalingResponse {
    val request: String?
    val response: String?
    val responseStatus: Int?
}

/**
 * @param request Payloaden vi selv bygde og sendte til helved.
 * Ikke noe [feil] vet om, derfor eget felt.
 * Persisteres for notoritet.
 * @param feil Underliggende HTTP-feil fra httpklient. [response] og [responseStatus] utledes herfra (persisteres for notoritet); brukes også til feillogging i kallende service.
 */
class KunneIkkeUtbetale(
    override val request: String,
    val feil: HttpKlientError,
) : UtbetalingResponse {
    override val response: String? get() = feil.rawResponseString
    override val responseStatus: Int? get() = feil.metadata.statusCode
}

/**
 * @param alleredeMottattTidligere `true` når utsjekk svarte at den hadde mottatt akkurat denne iverksettingen fra før (dedup, typisk fordi et tidligere forsøk lyktes uten at vi fikk registrert det).
 * Behandles som vanlig suksess; flagget finnes kun så kallere kan logge at dette skjedde uten å kjenne HTTP-kontrakten.
 */
data class SendtUtbetaling(
    override val request: String,
    override val response: String,
    override val responseStatus: Int,
    val alleredeMottattTidligere: Boolean,
) : UtbetalingResponse
