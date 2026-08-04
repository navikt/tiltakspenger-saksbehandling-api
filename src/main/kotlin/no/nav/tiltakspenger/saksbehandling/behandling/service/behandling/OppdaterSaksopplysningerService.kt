package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KunneIkkeOppdatereSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdaterSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class OppdaterSaksopplysningerService(
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
) {
    suspend fun oppdaterSaksopplysninger(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KunneIkkeOppdatereSaksopplysninger, Pair<Sak, Rammebehandling>> {
        val sak = sakService.hentForSakId(sakId)
        val behandling = sak.hentRammebehandling(behandlingId)!!
        val oppdaterteSaksopplysninger: Saksopplysninger = hentSaksopplysingerService.hentSaksopplysningerFraRegistre(
            fnr = sak.fnr,
            correlationId = correlationId,
            tiltaksdeltakelserDetErSøktTiltakspengerFor = sak.tiltaksdeltakelserDetErSøktTiltakspengerFor,
            aktuelleTiltaksdeltakelserForBehandlingen = when (behandling) {
                is Revurdering -> sak.tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }
                is Søknadsbehandling -> listOfNotNull(behandling.søknad.tiltak?.tiltaksdeltakerId)
            },
            inkluderOverlappendeTiltaksdeltakelserDetErSøktOm = when (behandling) {
                is Revurdering -> false
                is Søknadsbehandling -> true
            },
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            behandlingId = behandling.id,
        )

        return behandling.oppdaterSaksopplysninger(saksbehandler, oppdaterteSaksopplysninger, clock).map {
            val oppdatertSak = sak.oppdaterRammebehandling(it)

            rammebehandlingRepo.lagre(it)

            oppdatertSak to it
        }
    }
}
