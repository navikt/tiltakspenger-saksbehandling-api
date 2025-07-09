package no.nav.tiltakspenger.saksbehandling.behandling.util

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

interface BehandlingBuilder {
    // oppretter sak, søknad og behandling som er klar til manuell behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingKlarTilBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad, behandling, _) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
            tac = tac,
            fnr = fnr,
            virkingsperiode = virkingsperiode,
        )
        behandling.copy(
            status = Behandlingsstatus.KLAR_TIL_BEHANDLING,
            saksbehandler = null,
        ).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Tuple4(sak, søknad, behandling, "")
    }

    // oppretter sak, søknad og behandling som er under manuell behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad, behandling, _) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
            tac = tac,
            fnr = fnr,
            virkingsperiode = virkingsperiode,
        )
        behandling.copy(
            status = Behandlingsstatus.UNDER_BEHANDLING,
            saksbehandler = saksbehandler.navIdent,
        ).also {
            tac.behandlingContext.behandlingRepo.lagre(it)
        }
        return Tuple4(sak, søknad, behandling, "")
    }

    // oppretter sak, søknad og automatisk behandling som er klar til beslutning
    suspend fun ApplicationTestBuilder.opprettAutomatiskBehandlingKlarTilBeslutning(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = virkingsperiode)
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(søknad, CorrelationId.generate())
        tac.behandlingContext.delautomatiskBehandlingService.behandleAutomatisk(behandling, CorrelationId.generate())
        return Tuple4(sak, søknad, behandling, "")
    }

    // oppretter sak, søknad og behandling som er under automatisk behandling
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
    ): Tuple4<Sak, Søknad, Søknadsbehandling, String> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = virkingsperiode)
        val behandling = tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(søknad, CorrelationId.generate())
        return Tuple4(sak, søknad, behandling, "")
    }
}
