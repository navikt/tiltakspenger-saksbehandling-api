package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.Identoppslag
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.KunneIkkeHenteTiltakshistorikk
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.TiltakshistorikkHenter
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.infra.http.tiltakshistorikk.TiltakshistorikkResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import java.time.Clock

/**
 * Klient for å hente tiltaksdeltakelser, bygget på `TiltakshistorikkHenter` i tiltakspenger-libs.
 * Henter direkte fra `tiltakshistorikk` (Team Valp), med identoppslag mot PDL — uten den avviklede appen `tiltakspenger-tiltak` som mellomledd.
 *
 * Kildekode: https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-tiltakshistorikk
 * Slack: #team-valp
 *
 * Klienten logger ikke feil selv: [KunneIkkeHenteTiltakshistorikk] returneres uendret, og feillogging gjøres én gang i kallende service ([no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService], [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.TiltaksdeltakelseService]), som i tillegg har domenekonteksten.
 * Derimot logges hver vellykkede henting — rå respons til sikkerlogg — fordi libs-kontrakten legger det ansvaret på konsumenten (det lå tidligere i `tiltakspenger-tiltak`).
 */
class TiltakshistorikkHttpKlient(
    private val henteTjeneste: TiltakshistorikkHenter,
    private val clock: Clock,
) : TiltaksdeltakelseKlient {
    private val logger = KotlinLogging.logger {}

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, TiltaksdeltakelserFraRegister> {
        return hentOgLogg(fnr, correlationId).map {
            it.tiltakshistorikk.tilTiltaksdeltakelserFraRegister(tiltaksdeltakelserDetErSøktTiltakspengerFor, clock)
        }
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, List<TiltaksdeltakelseMedArrangørnavn>> {
        return hentOgLogg(fnr, correlationId).map {
            it.tiltakshistorikk.tilTiltaksdeltakelserMedArrangørnavn(harAdressebeskyttelse, clock)
        }
    }

    private suspend fun hentOgLogg(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteTiltakshistorikk, TiltakshistorikkResultat> {
        return henteTjeneste.hentTiltakshistorikk(fnr, correlationId).onRight { resultat ->
            resultat.respons.loggSuksess(logger, "Hentet tiltakshistorikk.")
            if (resultat.identoppslag is Identoppslag.FaltTilbakeTilInnsendtFnr) {
                // Historikken ble da kun slått opp for innsendt fnr — deltakelser på tidligere identer kan mangle uten at noe feilet.
                logger.warn { "Identoppslag mot PDL ga ingen brukbare identer for tiltakshistorikk — falt tilbake til innsendt fnr. correlationId: $correlationId" }
            }
            val ukjente = resultat.tiltakshistorikk.ukjenteKildeverdier
            if (ukjente.isNotEmpty()) {
                // Kodene er kildens egne enum-verdier, ikke personopplysninger, og tåler vanlig logg.
                logger.warn { "Tiltakshistorikken inneholdt ${ukjente.size} ukjente kildeverdier som utelates fra uttrekket: ${ukjente.map { "${it.hva}: ${it.kodeIKontrakten}" }.distinct()}" }
            }
        }
    }
}
