package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

interface IverksettSøknadsbehandlingBuilder {
    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * @param resultat Innvilgelse eller avslag.
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperioder.totalPeriode),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
    ): Tuple4<Sak, Søknad, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, søknad, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            resultat = resultat,
            innvilgelsesperioder = innvilgelsesperioder,
            barnetillegg = barnetillegg,
            tiltaksdeltakelse = tiltaksdeltakelse,
            saksbehandler = saksbehandler,
        )
        taBehandling(tac, sak.id, behandlingId, beslutter)
        val (oppdatertSak, rammevedtak, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandlingId,
            beslutter = beslutter,
        )!!
        return Tuple4(
            oppdatertSak,
            søknad,
            rammevedtak,
            jsonResponse,
        )
    }

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.iverksettAutomatiskBehandletSøknadsbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(),
    ): Tuple4<Sak, Søknad, Rammevedtak, RammebehandlingDTOJson> {
        val (sak, søknad, søknadsbehandling) = opprettAutomatiskBehandlingKlarTilBeslutning(
            tac = tac,
            fnr = fnr,
            tiltaksdeltakelse = tiltaksdeltakelse,
        )
        taBehandling(tac, sak.id, søknadsbehandling.id, beslutter)
        val (oppdatertSak, rammevedtakSøknadsbehandling, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = søknadsbehandling.id,
            beslutter = beslutter,
        )!!
        return Tuple4(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            jsonResponse,
        )
    }
}
