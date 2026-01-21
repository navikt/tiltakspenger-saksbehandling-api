package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
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
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        revurderingInnvilgelsesperioder: Innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(revurderingInnvilgelsesperioder.totalPeriode),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperioder.perioder),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
            tac = tac,
            søknadsbehandlingInnvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
            oppdatertTiltaksdeltakelse = oppdatertTiltaksdeltakelse,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            fnr = fnr,
            sakId = sakId,
        )

        oppdaterRevurderingInnvilgelse(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            innvilgelsesperioder = revurderingInnvilgelsesperioder,
            barnetillegg = barnetilleggRevurdering,
        )

        sendRevurderingTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )

        taBehandling(tac, sak.id, revurdering.id, saksbehandler = beslutter())

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

    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        stansFraOgMed: LocalDate = innvilgelsesperioder.totalPeriode.fraOgMed,
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
            tac = tac,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            fnr = fnr,
            sakId = sakId,
            innvilgelsesperioder = innvilgelsesperioder,
        )

        oppdaterRevurderingStans(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            saksbehandler = saksbehandler,
            begrunnelseVilkårsvurdering = null,
            fritekstTilVedtaksbrev = null,
            valgteHjemler = setOf(ValgtHjemmelForStans.Alder),
            stansFraOgMed = stansFraOgMed,
            harValgtStansFraFørsteDagSomGirRett = false,
        )

        sendRevurderingTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )

        taBehandling(tac, sak.id, revurdering.id, saksbehandler = beslutter())

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

    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        revurderingInnvilgelsesperioder: Innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperioder.totalPeriode),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Tuple5<Sak, Søknad, Rammevedtak, Rammevedtak, RammebehandlingDTOJson>? {
        val (sak, søknad, søknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
            tac = tac,
            søknadsbehandlingInnvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            fnr = fnr,
            sakId = sakId,
        )!!

        oppdaterRevurderingOmgjøring(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            innvilgelsesperioder = revurderingInnvilgelsesperioder,
            barnetillegg = barnetilleggRevurdering,
            saksbehandler = saksbehandler,
        )

        sendRevurderingTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )

        taBehandling(tac, sak.id, revurdering.id, saksbehandler = beslutter)

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

    /**
     * Oppretter en revurdering til omgjøring og iverksetter den.
     * Merk: Denne oppretter ikke sak, søknad eller søknadsbehandlingen.
     */
    suspend fun ApplicationTestBuilder.iverksettRevurderingOmgjøring(
        tac: TestApplicationContext,
        sakId: SakId,
        rammevedtakIdSomOmgjøres: VedtakId,
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        barnetilleggRevurdering: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
        fritekstTilVedtaksbrev: String? = "brevtekst revurdering",
        begrunnelseVilkårsvurdering: String? = "begrunnelse revurdering",
    ): Triple<Sak, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, revurdering, _) = startRevurderingOmgjøring(
            tac = tac,
            sakId = sakId,
            rammevedtakIdSomOmgjøres = rammevedtakIdSomOmgjøres,
            saksbehandler = saksbehandler,
        )!!

        oppdaterRevurderingOmgjøring(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetilleggRevurdering,
            saksbehandler = saksbehandler,
        )

        sendRevurderingTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
        )

        taBehandling(tac, sak.id, revurdering.id, saksbehandler = beslutter())

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
