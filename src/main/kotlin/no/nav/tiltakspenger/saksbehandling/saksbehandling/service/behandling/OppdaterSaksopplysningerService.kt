package no.nav.tiltakspenger.saksbehandling.saksbehandling.service.behandling

import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseGateway

class OppdaterSaksopplysningerService(
    private val sakService: SakService,
    private val personService: PersonService,
    private val tiltaksdeltagelseGateway: TiltaksdeltagelseGateway,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun oppdaterSaksopplysninger(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Behandling {
        // Denne sjekker tilgang til person og sak.
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere saksopplysinger. Fant ikke sak. sakId=$sakId, behandlingId=$behandlingId")
        }
        val behandling = sak.hentBehandling(behandlingId)!!
        val oppdaterteSaksopplysninger: Saksopplysninger = hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            saksopplysningsperiode = behandling.saksopplysningsperiode!!,
        )
        return behandling.oppdaterSaksopplysninger(saksbehandler, oppdaterteSaksopplysninger).also {
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
        val alleRelevanteTiltak = runBlocking {
            tiltaksdeltagelseGateway.hentTiltaksdeltagelse(
                fnr = fnr,
                correlationId = correlationId,
            )
            // Vi ønsker ikke filtrere bort tiltak som det ikke er søkt på, siden vi kun tillater de å søke på ett tiltak om gangen. I tillegg kan det ha dukket opp nye tiltak etter brukeren søkte.
        }
        val overlappendeTiltak = alleRelevanteTiltak.filter { it.overlapperMedPeriode(saksopplysningsperiode) }
        return Saksopplysninger(
            fødselsdato = personopplysninger.fødselsdato,
            tiltaksdeltagelse = overlappendeTiltak,
        )
    }
}
