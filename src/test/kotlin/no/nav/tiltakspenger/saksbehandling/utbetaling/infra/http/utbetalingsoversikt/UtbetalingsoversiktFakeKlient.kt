package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/** Svarer med det som er lagt inn for fnr-et, ellers med et vellykket, tomt svar. */
class UtbetalingsoversiktFakeKlient : Utbetalingsoversiktklient {
    private val svar = ConcurrentHashMap<Fnr, Either<KunneIkkeHenteUtbetalingsoversikt, List<RegistrertUtbetaling>>>()
    private val mottatteOppslag = ConcurrentHashMap<Fnr, Oppslag>()

    /** Settes når en test trenger at flere svar har samme tidspunkt. */
    @Volatile
    var responsMottatt: LocalDateTime? = null

    fun leggTilUtbetalinger(fnr: Fnr, utbetalinger: List<RegistrertUtbetaling>) {
        svar[fnr] = utbetalinger.right()
    }

    fun leggTilFeil(fnr: Fnr, feil: KunneIkkeHenteUtbetalingsoversikt) {
        svar[fnr] = feil.left()
    }

    fun sisteOppslagFor(fnr: Fnr): Oppslag? = mottatteOppslag[fnr]

    override suspend fun hent(
        fnr: Fnr,
        periode: Periode,
        periodetype: Oppslagsperiodetype,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteUtbetalingsoversikt, HttpKlientResponse<List<RegistrertUtbetaling>>> {
        mottatteOppslag[fnr] = Oppslag(periode, periodetype)
        return (svar[fnr] ?: emptyList<RegistrertUtbetaling>().right()).map {
            val response = ObjectMother.httpKlientResponse(body = it, rawRequestString = RÅ_REQUEST, rawResponseString = RÅ_RESPONSE)
            response.copy(metadata = response.metadata.copy(tidsstempler = response.metadata.tidsstempler.copy(responsMottatt = responsMottatt)))
        }
    }

    companion object {
        const val RÅ_REQUEST = "POST http://test/utbetalingsoversikt"
        const val RÅ_RESPONSE = "[]"
    }
}
