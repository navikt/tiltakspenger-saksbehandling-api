package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.person.harStrengtFortroligAdresse
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.ports.TiltakGateway
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class OppdaterSaksopplysningerService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val sakService: SakService,
    private val personService: PersonService,
    private val tiltakGateway: TiltakGateway,
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
            sakId = sakId,
            saksnummer = sak.saksnummer,
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
        sakId: SakId,
        saksnummer: Saksnummer,
        fnr: Fnr,
        correlationId: CorrelationId,
        saksopplysningsperiode: Periode,
    ): Saksopplysninger {
        val personopplysninger = personService.hentPersonopplysninger(fnr)
        val adressebeskyttelseGradering: List<AdressebeskyttelseGradering>? =
            tilgangsstyringService.adressebeskyttelseEnkel(fnr)
                .getOrElse {
                    throw IllegalArgumentException(
                        "Kunne ikke hente adressebeskyttelsegradering for person. sakId: $sakId og saksnummer: $saksnummer",
                    )
                }
        require(adressebeskyttelseGradering != null) { "Fant ikke adressebeskyttelse for person. sakId: $sakId og saksnummer: $saksnummer" }
        val registrerteTiltak = runBlocking {
            tiltakGateway.hentTiltaksdeltagelse(
                fnr = fnr,
                maskerTiltaksnavn = adressebeskyttelseGradering.harStrengtFortroligAdresse(),
                correlationId = correlationId,
            )
            // Vi ønsker ikke filtrere bort tiltak som det ikke er søkt på, siden vi kun tillater de å søke på ett tiltak om gangen. I tillegg kan det ha dukket opp nye tiltak etter brukeren søkte.
        }.filter { it.deltakelsesperiode.overlapperMed(saksopplysningsperiode) }
        return Saksopplysninger(
            fødselsdato = personopplysninger.fødselsdato,
            // TODO John + Anders: Vurder på hvilket tidspunkt denne kan gjøres om til en liste. Kan det vente til vi har slettet vilkårssettet?
            tiltaksdeltagelse = registrerteTiltak.single(),
        )
    }
}
