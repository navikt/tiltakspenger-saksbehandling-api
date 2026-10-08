package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktavgrensning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktgrunnlag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktsvar
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.sha256
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

    /** Avgrenser ikke; det testes mot den ekte klienten. */
    override suspend fun hent(
        grunnlag: Utbetalingsoversiktgrunnlag,
        periode: Periode,
        periodetype: Oppslagsperiodetype,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteUtbetalingsoversikt, Utbetalingsoversiktsvar> {
        mottatteOppslag[grunnlag.fnr] = Oppslag(periode, periodetype)
        return (svar[grunnlag.fnr] ?: emptyList<RegistrertUtbetaling>().right()).map {
            val metadata = ObjectMother.httpKlientResponse(body = it, rawRequestString = RÅ_REQUEST, rawResponseString = AVGRENSET_SVAR).metadata
            Utbetalingsoversiktsvar(
                utbetalinger = it,
                avgrensetSvar = AVGRENSET_SVAR,
                avgrensning = Utbetalingsoversiktavgrensning(
                    regelversjon = Utbetalingsoversiktgrunnlag.REGELVERSJON,
                    perioder = grunnlag.perioder,
                    antallUtbetalingerMottatt = it.size,
                    antallYtelserMottatt = it.sumOf { utbetaling -> utbetaling.ytelser.size },
                    fjernedeYtelserPerÅrsak = emptyMap(),
                    antallFjernedeUtbetalinger = 0,
                    mottattSvarSha256 = AVGRENSET_SVAR.sha256(),
                    mottattSvarLengde = AVGRENSET_SVAR.length,
                ),
                metadata = metadata.copy(tidsstempler = metadata.tidsstempler.copy(responsMottatt = responsMottatt)),
            )
        }
    }

    companion object {
        const val RÅ_REQUEST = "POST http://test/utbetalingsoversikt"
        const val AVGRENSET_SVAR = "[]"
    }
}
