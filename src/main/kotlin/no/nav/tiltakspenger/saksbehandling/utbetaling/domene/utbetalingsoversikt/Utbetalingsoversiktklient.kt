package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.periode.Periode

/** Henter utbetalingene økonomisystemet har registrert for én person, avgrenset til [Utbetalingsoversiktgrunnlag] før noe forlater klienten. */
interface Utbetalingsoversiktklient {
    suspend fun hent(
        grunnlag: Utbetalingsoversiktgrunnlag,
        periode: Periode,
        periodetype: Oppslagsperiodetype,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteUtbetalingsoversikt, Utbetalingsoversiktsvar>
}

/**
 * Et vellykket, avgrenset svar.
 *
 * @param avgrensetSvar Svaret i kildens format etter avgrensningen, uten kontonummer og navn, og det [utbetalinger] er lest fra.
 * @param metadata Tider, statuskode og antall forsøk fra kallet.
 */
data class Utbetalingsoversiktsvar(
    val utbetalinger: List<RegistrertUtbetaling>,
    val avgrensetSvar: String,
    val avgrensning: Utbetalingsoversiktavgrensning,
    val metadata: HttpKlientMetadata,
)

/**
 * Periodetypen for oppslag mot økonomisystemet.
 * [UTBETALINGSPERIODE] filtrerer på posteringsdato og godtar ikke en sluttdato etter dagens dato.
 * [YTELSESPERIODE] filtrerer på perioden ytelsen gjelder, er en tung spørring som eierteamet fraråder, og beholdes for lagrede oppslag.
 */
enum class Oppslagsperiodetype {
    UTBETALINGSPERIODE,
    YTELSESPERIODE,
}
