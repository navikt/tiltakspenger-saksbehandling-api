package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak

class OppdaterSaksopplysningerService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
) {
    suspend fun oppdaterSaksopplysninger(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Pair<Sak, Rammebehandling>> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.hentRammebehandling(behandlingId)!!
        val oppdaterteSaksopplysninger: Saksopplysninger = hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            tiltaksdeltagelserDetErSøktTiltakspengerFor = sak.tiltaksdeltagelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltagelserForBehandlingen = when (behandling) {
                is Revurdering -> sak.tiltaksdeltagelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.id }
                is Søknadsbehandling -> listOfNotNull(behandling.søknad.tiltak?.id)
            },
            inkluderOverlappendeTiltaksdeltagelserDetErSøktOm = when (behandling) {
                is Revurdering -> false
                is Søknadsbehandling -> true
            },
        )

        return behandling.oppdaterSaksopplysninger(saksbehandler, oppdaterteSaksopplysninger).map {
            val oppdatertSak = sak.oppdaterRammebehandling(it)

            behandlingRepo.lagre(it)

            oppdatertSak to it
        }
    }
}
