package no.nav.tiltakspenger.vedtak.routes

import no.nav.tiltakspenger.vedtak.routes.behandling.StartBehandlingBuilder
import no.nav.tiltakspenger.vedtak.routes.sak.OpprettSakRouteBuilder
import no.nav.tiltakspenger.vedtak.routes.søknad.MottaSøknadRouteBuilder

object RouteBuilder :
    OpprettSakRouteBuilder,
    MottaSøknadRouteBuilder,
    StartBehandlingBuilder
