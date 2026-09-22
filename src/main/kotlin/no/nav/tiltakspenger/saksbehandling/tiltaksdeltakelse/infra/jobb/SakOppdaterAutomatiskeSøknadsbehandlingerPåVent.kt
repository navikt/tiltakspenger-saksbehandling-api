package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock

private val log = KotlinLogging.logger {}

internal fun Sak.oppdaterAutomatiskeSøknadsbehandlingerPåVent(
    tiltaksdeltakerId: TiltaksdeltakerId,
    rammebehandlingRepo: RammebehandlingRepo,
    minutterForsinkelse: Long,
    clock: Clock,
) {
    rammebehandlinger.åpneSøknadsbehandlinger
        .filter { it.søknad.tiltak?.tiltaksdeltakerId == tiltaksdeltakerId && it.erUnderAutomatiskBehandling && it.ventestatus.erSattPåVent }
        .forEach {
            it.oppdaterVenterTil(
                nyVenterTil = nå(clock).plusMinutes(minutterForsinkelse),
                clock = clock,
            ).let { behandling ->
                rammebehandlingRepo.lagre(behandling)
            }
            log.info { "Har oppdatert venterTil for automatisk behandling med id ${it.id} pga endring på deltaker med intern id $tiltaksdeltakerId" }
        }
}
