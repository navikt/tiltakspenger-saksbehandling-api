package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

class TiltaksdeltakerIdJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val behandlingRepo: BehandlingRepo,
) {
    private val log = KotlinLogging.logger {}

    fun opprettInternTiltaksdeltakerIdForSaksopplysninger() {
        val behandlinger = behandlingRepo.hentBehandlingerUtenInternDeltakelseId()
        log.debug { "Fant ${behandlinger.size} behandlinger som mangler intern deltakerid" }
        behandlinger.forEach { behandling ->
            val correlationId = CorrelationId.generate()
            try {
                val oppdaterteTiltak = behandling.saksopplysninger.tiltaksdeltakelser.map {
                    if (it.internDeltakelseId == null) {
                        val internId = tiltaksdeltakerRepo.hentEllerLagre(it.eksternDeltakelseId)
                        log.info { "Opprettet eller hentet internId $internId for eksternId ${it.eksternDeltakelseId} for behandling ${behandling.id}" }
                        it.copy(internDeltakelseId = internId)
                    } else {
                        it
                    }
                }
                if (behandling.innvilgelsesperioder != null && behandling.innvilgelsesperioder!!.valgteTiltaksdeltagelser.any { it.verdi.internDeltakelseId == null }) {
                    val oppdaterteInnvilgelsesperioder = behandling.innvilgelsesperioder!!.periodisering.map {
                        if (it.verdi.valgtTiltaksdeltakelse.internDeltakelseId == null) {
                            val internId = oppdaterteTiltak.first { tiltak -> tiltak.eksternDeltakelseId == it.verdi.valgtTiltaksdeltakelse.eksternDeltakelseId }.internDeltakelseId
                                ?: throw IllegalStateException("Fant ikke internId for eksternId ${it.verdi.valgtTiltaksdeltakelse.eksternDeltakelseId} fra saksopplysningene")
                            it.verdi.copy(valgtTiltaksdeltakelse = it.verdi.valgtTiltaksdeltakelse.copy(internDeltakelseId = internId))
                        } else {
                            it.verdi
                        }
                    }
                    behandlingRepo.oppdaterInnvilgelsesperioder(
                        behandlingId = behandling.id,
                        innvilgelsesperioder = Innvilgelsesperioder(oppdaterteInnvilgelsesperioder),
                    )
                    log.info { "Oppdaterte innvilgelsesperioder med internId for tiltaksdeltakelser for behandling ${behandling.id}" }
                }
                behandlingRepo.oppdaterSaksopplysninger(
                    behandlingId = behandling.id,
                    saksopplysninger = behandling.saksopplysninger.copy(tiltaksdeltakelser = Tiltaksdeltakelser(oppdaterteTiltak)),
                )
                log.info { "Oppdaterte saksopplysninger med internId for tiltaksdeltakelser for behandling ${behandling.id}" }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved lagring av internId i saksopplysninger for behandling med id ${behandling.id}, correlationId $correlationId" }
            }
        }
    }
}
