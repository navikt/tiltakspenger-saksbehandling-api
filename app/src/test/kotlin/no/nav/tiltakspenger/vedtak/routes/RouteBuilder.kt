package no.nav.tiltakspenger.vedtak.routes

import no.nav.tiltakspenger.vedtak.routes.behandling.begrunnelse.OppdaterBegrunnelseBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.fritekst.OppdaterFritekstBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.iverksett.IverksettBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.sendtilbake.SendTilbakeBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.start.StartBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.tabehandling.TaBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.tilbeslutter.SendFørstegangsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.vedtak.routes.revurdering.SendRevurderingTilBeslutterBuilder
import no.nav.tiltakspenger.vedtak.routes.revurdering.StartRevurderingBuilder
import no.nav.tiltakspenger.vedtak.routes.sak.OpprettSakRouteBuilder
import no.nav.tiltakspenger.vedtak.routes.søknad.MottaSøknadRouteBuilder

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
