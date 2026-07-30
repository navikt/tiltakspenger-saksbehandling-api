package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

interface SendSøknadsbehandlingTilBeslutningBuilder {

    /** Oppretter ny sak (hvis sakId er null), søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutning(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        skalSendeVedtaksbrev: Boolean = true,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        // Utledes fra innvilgelsesperiodene slik at flyten registrerer samme deltakelse som innvilges; en frisk deltakelse ville fått ny id.
        tiltaksdeltakelse: Tiltaksdeltakelse = innvilgelsesperioder.valgteTiltaksdeltagelser.verdier.distinct().single(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
    ): Tuple4<Sak, Søknad, RammebehandlingId, String> {
        val (sak, søknad, behandling) = when (resultat) {
            SøknadsbehandlingsresultatType.INNVILGELSE -> opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                sakId = sakId,
                fnr = fnr,
                saksbehandler = saksbehandler,
                skalSendeVedtaksbrev = skalSendeVedtaksbrev,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            SøknadsbehandlingsresultatType.AVSLAG -> opprettSøknadsbehandlingUnderBehandlingMedAvslag(
                tac,
                fnr,
                saksbehandler,
            )
        }

        val sakId = sak.id
        val behandlingId = behandling.id

        return Tuple4(
            sak,
            søknad,
            behandlingId,
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            )!!,
        )
    }

    /**
     * Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING`.
     * Returnerer null dersom responsen ikke er 200 OK.
     */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/sendtilbeslutning",
            jwt = jwt,
            forventet = forventet,
        )
        if (response.statusCode != 200) return null
        return response.body
    }
}
