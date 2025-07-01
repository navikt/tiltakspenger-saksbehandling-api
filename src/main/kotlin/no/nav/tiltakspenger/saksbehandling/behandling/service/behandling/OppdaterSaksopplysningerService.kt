package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.SokosUtbetaldataClient

class OppdaterSaksopplysningerService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val tiltaksdeltagelseKlient: TiltaksdeltagelseKlient,
    private val behandlingRepo: BehandlingRepo,
    private val sokosUtbetaldataClient: SokosUtbetaldataClient,
) {
    suspend fun oppdaterSaksopplysninger(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Behandling> {
        // Denne sjekker tilgang til person og rollene SAKSBEHANDLER eller BESLUTTER.
        val sak = sakService.sjekkTilgangOgHentForSakId(sakId, saksbehandler, correlationId)
        val behandling = sak.hentBehandling(behandlingId)!!
        val oppdaterteSaksopplysninger: Saksopplysninger = hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            saksopplysningsperiode = behandling.saksopplysningsperiode,
        )
        // Denne validerer saksbehandler
        return behandling.oppdaterSaksopplysninger(saksbehandler, oppdaterteSaksopplysninger).mapLeft {
            it
        }.onRight {
            behandlingRepo.lagre(it)
        }
    }

    /**
     * @param saksopplysningsperiode: Perioden som saksopplysningene må overlappe med.
     */
    suspend fun hentSaksopplysningerFraRegistre(
        fnr: Fnr,
        correlationId: CorrelationId,
        saksopplysningsperiode: Periode,
    ): Saksopplysninger {
        val personopplysninger = personService.hentPersonopplysninger(fnr)
        val alleRelevanteTiltak = tiltaksdeltagelseKlient.hentTiltaksdeltagelser(
            fnr = fnr,
            correlationId = correlationId,
        )
        // Vi ønsker ikke filtrere bort tiltak som det ikke er søkt på, siden vi kun tillater de å søke på ett tiltak om gangen. I tillegg kan det ha dukket opp nye tiltak etter brukeren søkte.
        val overlappendeTiltak = alleRelevanteTiltak.filter { it.overlapperMedPeriode(saksopplysningsperiode) }
        val ytelser = sokosUtbetaldataClient.hentYtelserFraUtbetaldata(fnr, saksopplysningsperiode, correlationId)
        return Saksopplysninger(
            fødselsdato = personopplysninger.fødselsdato,
            tiltaksdeltagelse = overlappendeTiltak,
            periode = saksopplysningsperiode,
            ytelser = ytelser,
        )
    }
}
