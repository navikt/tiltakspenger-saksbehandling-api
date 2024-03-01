package no.nav.tiltakspenger.vedtak.service.vedtak

import kotliquery.TransactionalSession
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.tiltakspenger.domene.behandling.BehandlingIverksatt
import no.nav.tiltakspenger.domene.behandling.Personopplysninger
import no.nav.tiltakspenger.domene.vedtak.Vedtak
import no.nav.tiltakspenger.domene.vedtak.VedtaksType
import no.nav.tiltakspenger.domene.vedtak.Vedtaksperiode
import no.nav.tiltakspenger.felles.BehandlingId
import no.nav.tiltakspenger.felles.VedtakId
import no.nav.tiltakspenger.vedtak.repository.sak.PersonopplysningerRepo
import no.nav.tiltakspenger.vedtak.repository.vedtak.VedtakRepo
import no.nav.tiltakspenger.vedtak.service.brev.sendBrev
import no.nav.tiltakspenger.vedtak.service.utbetaling.UtbetalingService
import java.time.LocalDateTime

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

class VedtakServiceImpl(
    private val utbetalingService: UtbetalingService,
    private val vedtakRepo: VedtakRepo,
    private val personopplysningerRepo: PersonopplysningerRepo,
    private val rapidsConnection: RapidsConnection,
) : VedtakService {
    override fun hentVedtak(vedtakId: VedtakId): Vedtak? {
        return vedtakRepo.hent(vedtakId)
    }

    override fun hentVedtakForBehandling(behandlingId: BehandlingId): List<Vedtak> {
        return vedtakRepo.hentVedtakForBehandling(behandlingId)
    }

    override suspend fun lagVedtakForBehandling(behandling: BehandlingIverksatt, tx: TransactionalSession): Vedtak {
        val vedtak = Vedtak(
            id = VedtakId.random(),
            sakId = behandling.sakId,
            behandling = behandling,
            vedtaksdato = LocalDateTime.now(),
            vedtaksType = if (behandling is BehandlingIverksatt.Innvilget) VedtaksType.INNVILGELSE else VedtaksType.AVSLAG,
            vedtaksperioder = behandling.utfallsperioder.map {
                Vedtaksperiode(
                    fom = it.fom,
                    tom = it.tom,
                    antallBarn = it.antallBarn,
                    tiltak = it.tiltak,
                    antDagerMedTiltak = it.antDagerMedTiltak,
                    utfall = it.utfall,
                )
            },
            periode = behandling.vurderingsperiode,
            saksopplysninger = behandling.saksopplysninger(),
            vurderinger = behandling.vilkårsvurderinger,
            saksbehandler = behandling.saksbehandler!!,
            beslutter = behandling.beslutter,
        )

        val lagretVedtak = vedtakRepo.lagreVedtak(vedtak, tx)
        val personopplysninger =
            personopplysningerRepo.hent(vedtak.sakId).filterIsInstance<Personopplysninger.Søker>().first()

        utbetalingService.sendBehandlingTilUtbetaling(lagretVedtak)
        // Hvis kallet til utbetalingService feiler, kastes det en exception slik at vi ikke lagrer vedtaket og
        // sender melding til brev og meldekortgrunnlag. Dette er med vilje.

        sendMeldekortGrunnlag(lagretVedtak, rapidsConnection)
        sendBrev(lagretVedtak, rapidsConnection, personopplysninger)

        return lagretVedtak
    }
}
