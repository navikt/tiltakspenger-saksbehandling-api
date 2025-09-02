package no.nav.tiltakspenger.saksbehandling.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlePåNytt.BehandleSøknadPåNyttBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.ForhåndsvisVedtaksbrevTestbuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater.OppdaterBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.OvertaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.TaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendRevurderingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendSøknadsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.UnderkjennBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.util.SøknadsbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.TaMeldekortBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.OpprettSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.MottaSøknadRouteBuilder

object RouteBehandlingBuilder :
    OpprettSakRouteBuilder,
    MottaSøknadRouteBuilder,
    SøknadsbehandlingBuilder,
    TaBehandlingBuilder,
    SendSøknadsbehandlingTilBeslutningBuilder,
    SendRevurderingTilBeslutningBuilder,
    UnderkjennBehandlingBuilder,
    IverksettBehandlingBuilder,
    StartRevurderingBuilder,
    OppdaterSaksopplysningerBuilder,
    OvertaBehandlingBuilder,
    TaMeldekortBehandlingBuilder,
    BehandleSøknadPåNyttBuilder,
    ForhåndsvisVedtaksbrevTestbuilder,
    OppdaterBehandlingBuilder
