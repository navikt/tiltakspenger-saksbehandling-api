package no.nav.tiltakspenger.saksbehandling.routes

import no.nav.tiltakspenger.saksbehandling.routes.behandling.begrunnelse.OppdaterBegrunnelseBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.fritekst.OppdaterFritekstBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.iverksett.IverksettBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.sendtilbake.SendTilbakeBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.start.StartBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.tabehandling.TaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.routes.behandling.tilbeslutter.SendFørstegangsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.routes.revurdering.SendRevurderingTilBeslutterBuilder
import no.nav.tiltakspenger.saksbehandling.routes.revurdering.StartRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.routes.sak.OpprettSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.routes.søknad.MottaSøknadRouteBuilder

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
