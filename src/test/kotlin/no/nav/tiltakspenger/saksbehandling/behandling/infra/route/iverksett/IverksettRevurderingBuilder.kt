package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple5
import arrow.core.nonEmptyListOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.DEFAULT_INTERN_TILTAKSDELTAKELSE_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.DEFAULT_TILTAK_DELTAKELSE_ID
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

interface IverksettRevurderingBuilder {
    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingInnvilgelsesperiode: Periode = søknadsbehandlingInnvilgelsesperiode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperiode),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = søknadsbehandlingInnvilgelsesperiode.fraOgMed,
            tom = søknadsbehandlingInnvilgelsesperiode.tilOgMed,
        ),
        innvilgelsesperioder: List<InnvilgelsesperiodeDTO> = listOf(
            InnvilgelsesperiodeDTO(
                periode = revurderingInnvilgelsesperiode.toDTO(),
                tiltaksdeltakelseId = tiltaksdeltakelse.eksternDeltakelseId,
                antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
                internDeltakelseId = tiltaksdeltakelse.internDeltakelseId?.toString(),
            ),
        ),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson> {
        return iverksettSøknadsbehandlingOgRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetilleggRevurdering.toBarnetilleggDTO(),
                )
            },
            startRevurdering = {
                iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
                    tac = tac,
                    søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
                    revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
                    saksbehandler = saksbehandler,
                    beslutter = beslutter,
                    fnr = fnr,
                    sakId = sakId,
                    tiltaksdeltakelse = tiltaksdeltakelse,
                )
            },
        )
    }

    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        stansFraOgMed: LocalDate = søknadsbehandlingInnvilgelsesperiode.fraOgMed,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson> {
        return iverksettSøknadsbehandlingOgRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,

            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = stansFraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                )
            },
        ) {
            iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                fnr = fnr,
                sakId = sakId,
            )
        }
    }

    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingInnvilgelsesperiode: Periode = søknadsbehandlingInnvilgelsesperiode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperiode),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = søknadsbehandlingInnvilgelsesperiode.fraOgMed,
            tom = søknadsbehandlingInnvilgelsesperiode.tilOgMed,
        ),
        innvilgelsesperioder: List<InnvilgelsesperiodeDTO> = listOf(
            InnvilgelsesperiodeDTO(
                periode = revurderingInnvilgelsesperiode.toDTO(),
                tiltaksdeltakelseId = tiltaksdeltakelse.eksternDeltakelseId,
                antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
                internDeltakelseId = tiltaksdeltakelse.internDeltakelseId?.toString(),
            ),
        ),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson>? {
        return iverksettSøknadsbehandlingOgRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Omgjøring(
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetilleggRevurdering.toBarnetilleggDTO(),
                )
            },
            startRevurdering = {
                iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                    tac = tac,
                    søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
                    oppdaterTiltaksdeltakelsesperiode = revurderingInnvilgelsesperiode,
                    saksbehandler = saksbehandler,
                    beslutter = beslutter,
                    fnr = fnr,
                    sakId = sakId,
                    tiltaksdeltakelse = tiltaksdeltakelse,
                )!!
            },
        )
    }

    /**
     * Oppretter en revurdering til omgjøring og iverksetter den.
     * Merk: Denne oppretter ikke sak, søknad eller søknadsbehandlingen.
     */
    suspend fun ApplicationTestBuilder.iverksettRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId,
        rammevedtakIdSomOmgjøres: VedtakId,
        innvilgelsesperiode: Periode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        innvilgelsesperioder: List<InnvilgelsesperiodeDTO> = listOf(
            InnvilgelsesperiodeDTO(
                periode = innvilgelsesperiode.toDTO(),
                tiltaksdeltakelseId = DEFAULT_TILTAK_DELTAKELSE_ID,
                antallDagerPerMeldeperiode = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
                internDeltakelseId = DEFAULT_INTERN_TILTAKSDELTAKELSE_ID,
            ),
        ),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperiode),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Triple<Sak, Rammevedtak, RammebehandlingDTOJson> {
        return iverksettRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Omgjøring(
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetilleggRevurdering.toBarnetilleggDTO(),
                )
            },
            startRevurdering = {
                startRevurderingOmgjøring(
                    tac = tac,
                    sakId = sakId,
                    rammevedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres,
                    saksbehandler = saksbehandler,
                )!!
            },
        )
    }

    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurdering(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        oppdaterBehandlingDTO: (Revurdering) -> OppdaterBehandlingDTO,
        startRevurdering: suspend () -> Tuple5<Sak, Søknad, Rammevedtak, Revurdering, RammebehandlingDTOJson>,
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling, revurdering) = startRevurdering()
        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = oppdaterBehandlingDTO(revurdering),
            saksbehandler = saksbehandler,
        )
        sendRevurderingStansTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )
        taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
        val (oppdatertSak, rammevedtakRevurdering, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            beslutter = beslutter,
        )!!
        return Tuple5(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            rammevedtakRevurdering,
            jsonResponse,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettRevurdering(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        oppdaterBehandlingDTO: (Revurdering) -> OppdaterBehandlingDTO,
        startRevurdering: suspend () -> Triple<Sak, Revurdering, RammebehandlingDTOJson>,
    ): Triple<Sak, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, revurdering, _) = startRevurdering()
        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = oppdaterBehandlingDTO(revurdering),
            saksbehandler = saksbehandler,
        )
        sendRevurderingStansTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )
        taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
        val (oppdatertSak, rammevedtak, jsonResponseForIverksettRevurdering) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            beslutter = beslutter,
        )!!
        return Triple(
            oppdatertSak,
            rammevedtak,
            jsonResponseForIverksettRevurdering,
        )
    }
}
