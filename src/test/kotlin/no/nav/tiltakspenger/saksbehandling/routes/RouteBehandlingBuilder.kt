package no.nav.tiltakspenger.saksbehandling.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.begrunnelse.OppdaterBegrunnelseBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.fritekst.OppdaterFritekstBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.sendtilbake.SendTilbakeBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartSøknadsbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.OvertaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.TaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendRevurderingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendSøknadsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.TaMeldekortBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.OpprettSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.MottaSøknadRouteBuilder

object RouteBehandlingBuilder :
    OpprettSakRouteBuilder,
    MottaSøknadRouteBuilder,
    StartSøknadsbehandlingBuilder,
    TaBehandlingBuilder,
    OppdaterFritekstBuilder,
    OppdaterBegrunnelseBuilder,
    SendSøknadsbehandlingTilBeslutningBuilder,
    SendRevurderingTilBeslutningBuilder,
    SendTilbakeBehandlingBuilder,
    IverksettBehandlingBuilder,
    StartRevurderingBuilder,
    OppdaterSaksopplysningerBuilder,
    OvertaBehandlingBuilder,
    TaMeldekortBehandlingBuilder
