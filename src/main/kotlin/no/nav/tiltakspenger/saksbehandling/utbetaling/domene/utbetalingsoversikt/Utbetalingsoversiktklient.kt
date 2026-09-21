package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.periode.Periode

/** Henter utbetalingene økonomisystemet har registrert for én person. */
interface Utbetalingsoversiktklient {
    suspend fun hent(
        fnr: Fnr,
        periode: Periode,
        periodetype: Oppslagsperiodetype,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteUtbetalingsoversikt, HttpKlientResponse<List<RegistrertUtbetaling>>>
}

/**
 * [UTBETALINGSPERIODE] filtrerer på posteringsdato, [YTELSESPERIODE] på perioden ytelsen gjelder.
 * Eierteamet omtaler [YTELSESPERIODE] som en tung spørring.
 */
enum class Oppslagsperiodetype {
    UTBETALINGSPERIODE,
    YTELSESPERIODE,
}
