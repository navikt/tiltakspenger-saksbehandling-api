package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.nonEmptyListOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.antallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelseDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.LocalDate

interface IverksettRevurderingBuilder {
    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettRevurderingInnvilgelse(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingInnvilgelsesperiode: Periode = søknadsbehandlingInnvilgelsesperiode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        antallDagerPerMeldeperiodeForRevurdering: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            revurderingInnvilgelsesperiode,
        ),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperiode),
        tiltaksdeltakelseRevurdering: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = revurderingInnvilgelsesperiode.fraOgMed,
            tom = revurderingInnvilgelsesperiode.tilOgMed,
        ),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Søknadsbehandling, Revurdering, RammebehandlingDTOJson> {
        return iverksettRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    valgteTiltaksdeltakelser = revurdering.tiltaksdeltakelseDTO(),
                    barnetillegg = barnetilleggRevurdering.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = revurdering.antallDagerPerMeldeperiodeDTO(
                        revurderingInnvilgelsesperiode,
                    ),
                )
            },
        ) {
            startRevurderingInnvilgelse(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
                revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
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
    suspend fun ApplicationTestBuilder.iverksettRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        stansFraOgMed: LocalDate = søknadsbehandlingInnvilgelsesperiode.fraOgMed,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Søknadsbehandling, Revurdering, RammebehandlingDTOJson> {
        return iverksettRevurdering(
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
            startRevurderingStans(
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
    suspend fun ApplicationTestBuilder.iverksettRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingInnvilgelsesperiode: Periode = søknadsbehandlingInnvilgelsesperiode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        antallDagerPerMeldeperiodeForRevurdering: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            revurderingInnvilgelsesperiode,
        ),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperiode),
        tiltaksdeltakelseRevurdering: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = revurderingInnvilgelsesperiode.fraOgMed,
            tom = revurderingInnvilgelsesperiode.tilOgMed,
        ),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Søknadsbehandling, Revurdering, RammebehandlingDTOJson> {
        return iverksettRevurdering(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            oppdaterBehandlingDTO = { revurdering ->
                OppdaterRevurderingDTO.Omgjøring(
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    valgteTiltaksdeltakelser = revurdering.tiltaksdeltakelseDTO(),
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetilleggRevurdering.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = revurdering.antallDagerPerMeldeperiodeDTO(
                        revurderingInnvilgelsesperiode,
                    ),
                )
            },
        ) {
            val (sak, søknad, søknadsbehandling, revurdering) = startRevurderingOmgjøring(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
                oppdaterTiltaksdeltakelsesperiode = revurderingInnvilgelsesperiode,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                fnr = fnr,
                sakId = sakId,
            )
            Tuple4(sak, søknad, søknadsbehandling, revurdering!!)
        }
    }

    suspend fun ApplicationTestBuilder.iverksettRevurdering(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        oppdaterBehandlingDTO: (Revurdering) -> OppdaterBehandlingDTO,
        startRevurdering: suspend () -> Tuple4<Sak, Søknad, Søknadsbehandling, Revurdering>,
    ): Tuple5<Sak, Søknad, Søknadsbehandling, Revurdering, RammebehandlingDTOJson> {
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
        val (oppdatertSak, oppdatertRevurdering, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            beslutter = beslutter,
        )!!
        return Tuple5(
            oppdatertSak,
            søknad,
            søknadsbehandling,
            oppdatertRevurdering as Revurdering,
            jsonResponse,
        )
    }
}
