package no.nav.tiltakspenger.vedtak.routes

import no.nav.tiltakspenger.vedtak.routes.behandling.OppdaterBegrunnelseBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.OppdaterFritekstBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.SendTilBeslutterBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.StartBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.behandling.TaBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.sak.OpprettSakRouteBuilder
import no.nav.tiltakspenger.vedtak.routes.søknad.MottaSøknadRouteBuilder

object RouteBuilder :
    OpprettSakRouteBuilder,
    MottaSøknadRouteBuilder,
    StartBehandlingBuilder,
    TaBehandlingBuilder,
    OppdaterFritekstBuilder,
    OppdaterBegrunnelseBuilder,
    SendTilBeslutterBuilder
