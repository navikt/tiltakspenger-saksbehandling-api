package no.nav.tiltakspenger.saksbehandling.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.begrunnelse.OppdaterBegrunnelseBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.fritekst.OppdaterFritekstBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.sendtilbake.SendTilbakeBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tabehandling.TaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendFørstegangsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.routes.revurdering.SendRevurderingTilBeslutterBuilder
import no.nav.tiltakspenger.saksbehandling.routes.revurdering.StartRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.OpprettSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.MottaSøknadRouteBuilder

object RouteBuilder :
    OpprettSakRouteBuilder,
    MottaSøknadRouteBuilder,
    StartBehandlingBuilder,
    TaBehandlingBuilder,
    OppdaterFritekstBuilder,
    OppdaterBegrunnelseBuilder,
    SendFørstegangsbehandlingTilBeslutningBuilder,
    SendRevurderingTilBeslutterBuilder,
    SendTilbakeBuilder,
    IverksettBehandlingBuilder,
    StartRevurderingBuilder,
    OppdaterSaksopplysningerBuilder
