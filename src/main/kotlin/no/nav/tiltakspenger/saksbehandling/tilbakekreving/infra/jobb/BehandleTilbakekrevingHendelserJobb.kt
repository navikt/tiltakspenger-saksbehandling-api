package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoSvarHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingMottaker
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingRevurdering
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO.TilbakekrevingUtvidPeriode
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import java.time.Clock

class BehandleTilbakekrevingHendelserJobb(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo,
    private val sakService: SakService,
    private val tilbakekrevingProducer: TilbakekrevingProducer,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun håndterUbehandledeHendelser() {
        logger.debug { "Kjører jobb for å behandle tilbakekreving-hendelser" }

        val ubehandledeHendelser = Either.catch { tilbakekrevingHendelseRepo.hentUbehandledeHendelser() }.getOrElse {
            logger.error(it) { "Feil ved henting av ubehandlede tilbakekreving-hendelser" }
            return
        }

        ubehandledeHendelser.forEach { hendelse ->
            val sakId = hendelse.sakId

            if (sakId == null) {
                logger.error { "Tilbakekreving-hendelse ${hendelse.id} er ikke knyttet til en sak, og kan derfor ikke behandles" }
                tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                    hendelse.id,
                    "Hendelse med fagsak-id/saksnummer ${hendelse.eksternFagsakId} har ingen sak hos oss",
                )
                return@forEach
            }

            val sak = sakService.hentForSakId(sakId)

            Either.catch {
                when (hendelse) {
                    is TilbakekrevingInfoBehovHendelse -> sak.håndterInfoBehov(hendelse)
                    is TilbakekrevingBehandlingEndretHendelse -> sak.håndterBehandlingEndret(hendelse)
                    is TilbakekrevingInfoSvarHendelse -> throw IllegalStateException("InfoSvar hendelse skal ikke behandles av oss")
                }
            }.onLeft {
                logger.error(it) {
                    "Feil ved behandling av tilbakekreving-hendelse ${hendelse.id} / ${hendelse.hendelsestype} på sak ${hendelse.sakId}"
                }
            }
        }
    }

    private fun Sak.håndterInfoBehov(hendelse: TilbakekrevingInfoBehovHendelse) {
        val utbetaling = utbetalinger.hentUtbetalingForUuid(hendelse.kravgrunnlagReferanse)

        if (utbetaling == null) {
            logger.error { "Fant ingen utbetaling for kravgrunnlagReferanse ${hendelse.kravgrunnlagReferanse} på sak $id" }
            tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                hendelse.id,
                "Fant ikke utbetaling for kravgrunnlagReferanse",
            )
            return
        }

        val infoSvarDTO = when (utbetaling.beregningKilde) {
            is BeregningKilde.BeregningKildeMeldekort ->
                meldekortbehandlinger.hentMeldekortBehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)

            is BeregningKilde.BeregningKildeBehandling ->
                rammebehandlinger.hentRammebehandling(utbetaling.beregningKilde.id)
                    ?.tilSvarDTO(hendelse)
        }

        if (infoSvarDTO == null) {
            logger.error { "Fant ingen behandling for beregningskilde ${utbetaling.beregningKilde.id} på sak $id" }
            tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                hendelse.id,
                "Fant ikke behandling for utbetalingens beregningskilde",
            )
            return
        }

        logger.info { "Produserer svar på tilbakekreving info-behov ${hendelse.id} for sak $id med kravgrunnlagReferanse ${hendelse.kravgrunnlagReferanse}" }

        val svar = tilbakekrevingProducer.produserInfoSvar(hendelse.id, infoSvarDTO)

        tilbakekrevingHendelseRepo.markerInfoBehovSomBehandlet(hendelse.id, svar)
    }

    private fun Sak.håndterBehandlingEndret(hendelse: TilbakekrevingBehandlingEndretHendelse) {
        val utbetaling = hendelse.eksternBehandlingId?.let { utbetalinger.hentUtbetalingForUuid(it) }

        if (utbetaling == null) {
            logger.error { "Fant ingen utbetaling for id ${hendelse.eksternBehandlingId} på sak $id" }
            tilbakekrevingHendelseRepo.markerSomBehandletMedFeil(
                hendelse.id,
                "Fant ikke utbetaling for tilbakeBehandlingId",
            )
            return
        }

        val tilbakekrevingBehandling = TilbakekrevingBehandling(
            id = TilbakekrevingId.random(),
            sakId = this.id,
            utbetalingId = utbetaling.id,
            tilbakeBehandlingId = hendelse.tilbakeBehandlingId,
            opprettet = nå(clock),
            status = hendelse.behandlingsstatus,
            url = hendelse.url,
            kravgrunnlagTotalPeriode = hendelse.fullstendigPeriode,
            totaltFeilutbetaltBeløp = hendelse.totaltFeilutbetaltBeløp,
            varselSendt = hendelse.varselSendt,
        )

        logger.info { "Lagrer tilbakekrevingbehandling ${tilbakekrevingBehandling.id} for sak $id basert på hendelse ${hendelse.id}" }

        sessionFactory.withTransactionContext { tx ->
            tilbakekrevingBehandlingRepo.lagre(tilbakekrevingBehandling, tx)
            tilbakekrevingHendelseRepo.markerEndringSomBehandlet(hendelse.id, tx)
        }
    }

    private fun MeldekortBehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == MeldekortBehandlingStatus.GODKJENT) {
            "Meldekortet må være godkjent for å produsere svar på info-behov - id: $id, status: $status"
        }

        return TilbakekrevingInfoSvarDTO(
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = behov.kravgrunnlagReferanse,
                årsak = TilbakekrevingInfoSvarDTO.TilbakekrevingRevurderingÅrsak.KORRIGERING,
                årsakTilFeilutbetaling = this.begrunnelse?.verdi,
                vedtaksdato = this.iverksattTidspunkt!!.toLocalDate(),
            ),
            utvidPerioder = listOf(
                TilbakekrevingUtvidPeriode(
                    kravgrunnlagPeriode = TilbakekrevingPeriodeDTO(
                        fom = this.beregning!!.fraOgMed,
                        tom = this.beregning!!.tilOgMed,
                    ),
                    vedtaksperiode = TilbakekrevingPeriodeDTO(
                        fom = this.periode.fraOgMed,
                        tom = this.periode.tilOgMed,
                    ),
                ),
            ),
            behandlendeEnhet = this.navkontor.kontornummer,
        )
    }

    private fun Rammebehandling.tilSvarDTO(behov: TilbakekrevingInfoBehovHendelse): TilbakekrevingInfoSvarDTO {
        require(this.status == Rammebehandlingsstatus.VEDTATT) {
            "Rammebehandlingen må være vedtatt for å produsere svar på info-behov - id: $id, status: $status"
        }

        return TilbakekrevingInfoSvarDTO(
            eksternFagsakId = behov.eksternFagsakId,
            hendelseOpprettet = nå(clock),
            mottaker = TilbakekrevingMottaker(
                ident = this.fnr.verdi,
            ),
            revurdering = TilbakekrevingRevurdering(
                behandlingId = behov.kravgrunnlagReferanse,
                årsak = TilbakekrevingInfoSvarDTO.TilbakekrevingRevurderingÅrsak.NYE_OPPLYSNINGER,
                årsakTilFeilutbetaling = this.begrunnelseVilkårsvurdering?.verdi,
                vedtaksdato = this.iverksattTidspunkt!!.toLocalDate(),
            ),
            utvidPerioder = listOf(
                TilbakekrevingUtvidPeriode(
                    kravgrunnlagPeriode = TilbakekrevingPeriodeDTO(
                        fom = this.utbetaling!!.beregning.fraOgMed,
                        tom = this.utbetaling!!.beregning.tilOgMed,
                    ),
                    vedtaksperiode = TilbakekrevingPeriodeDTO(
                        fom = this.vedtaksperiode!!.fraOgMed,
                        tom = this.vedtaksperiode!!.tilOgMed,
                    ),
                ),
            ),
            behandlendeEnhet = this.utbetaling!!.navkontor.kontornummer,
        )
    }
}
