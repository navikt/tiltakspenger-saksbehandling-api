package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.nonEmptyListOf
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import org.json.JSONObject
import java.time.LocalDate

interface IverksettSøknadsbehandlingBuilder {
    /**
     * Oppretter kun ny sak hvis sakId er null. Oppretter alltid ny søknad med søknadsbehandling.
     *
     * @param fnr ignoreres hvis sakId er satt
     * */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandling(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        vedtaksperiode: Periode = 1.til(10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            vedtaksperiode,
        ),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
        tiltaksdeltakelse: Tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, RammebehandlingDTOJson> {
        val (sak, søknad, behandlingId, _) = sendSøknadsbehandlingTilBeslutning(
            tac = tac,
            sakId = sakId,
            fnr = fnr,
            virkningsperiode = vedtaksperiode,
            resultat = resultat,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            barnetillegg = barnetillegg,
            tiltaksdeltakelse = tiltaksdeltakelse,
            saksbehandler = saksbehandler,
        )
        taBehandling(tac, sak.id, behandlingId, beslutter)
        val (oppdatertSak, oppdatertBehandling, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandlingId,
            beslutter = beslutter,
        )!!
        return Tuple4(
            oppdatertSak,
            søknad,
            oppdatertBehandling as Søknadsbehandling,
            jsonResponse,
        )
    }

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.iverksettAutomatiskBehandletSøknadsbehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = 1.til(10.april(2025)),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, RammebehandlingDTOJson> {
        val (sak, søknad, behandling) = opprettAutomatiskBehandlingKlarTilBeslutning(
            tac = tac,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
        )
        taBehandling(tac, sak.id, behandling.id, beslutter)
        val (oppdatertSak, oppdatertBehandling, jsonResponse) = iverksettForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = behandling.id,
            beslutter = beslutter,
        )!!
        return Tuple4(
            oppdatertSak,
            søknad,
            oppdatertBehandling as Søknadsbehandling,
            jsonResponse,
        )
    }
}
