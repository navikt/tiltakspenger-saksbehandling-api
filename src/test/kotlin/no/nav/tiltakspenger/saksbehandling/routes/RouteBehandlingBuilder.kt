package no.nav.tiltakspenger.saksbehandling.routes

import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt.AvbrytRammebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlePåNytt.BehandleSøknadPåNyttBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev.ForhåndsvisVedtaksbrevTestbuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettRammebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett.IverksettSøknadsbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater.OppdaterBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterSaksopplysninger.OppdaterSaksopplysningerBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartRevurderingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start.StartSøknadsbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.OvertaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta.TaBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendRevurderingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter.SendSøknadsbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.underkjenn.UnderkjennBehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt.AvbrytKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis.ForhåndsvisBrevKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.IverksettKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake.LeggKlagebehandlingTilbakeBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater.OppdaterKlagebehandlingBrevtekstBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater.OppdaterKlagebehandlingFormkravBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettRammebehandling.OpprettRammebehandlingForKlageBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta.OvertaKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.start.OpprettKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.TaKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.VurderKlagebehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.avbryt.AvbrytMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.brev.ForhåndsvisVedtaksbrevForMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.iverksett.IverksettMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbake.LeggTilbakeMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater.OppdaterMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett.OpprettMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overta.OvertaMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.sendTilBeslutning.SendMeldekortbehandlingTilBeslutningBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.ta.TaMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjenn.UnderkjennMeldekortbehandlingBuilder
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.HentSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.OpprettSakRouteBuilder
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.MottaSøknadRouteBuilder

object RouteBehandlingBuilder :
    OpprettSakRouteBuilder,
    HentSakRouteBuilder,
    MottaSøknadRouteBuilder,
    StartSøknadsbehandlingBuilder,
    OpprettKlagebehandlingBuilder,
    OvertaKlagebehandlingBuilder,
    LeggKlagebehandlingTilbakeBuilder,
    TaKlagebehandlingBuilder,
    OppdaterKlagebehandlingFormkravBuilder,
    VurderKlagebehandlingBuilder,
    OpprettRammebehandlingForKlageBuilder,
    OppdaterKlagebehandlingBrevtekstBuilder,
    AvbrytKlagebehandlingBuilder,
    IverksettKlagebehandlingBuilder,
    ForhåndsvisBrevKlagebehandlingBuilder,
    TaBehandlingBuilder,
    SendSøknadsbehandlingTilBeslutningBuilder,
    SendRevurderingTilBeslutningBuilder,
    UnderkjennBehandlingBuilder,
    IverksettRammebehandlingBuilder,
    IverksettSøknadsbehandlingBuilder,
    IverksettRevurderingBuilder,
    StartRevurderingBuilder,
    OppdaterSaksopplysningerBuilder,
    OvertaBehandlingBuilder,
    TaMeldekortbehandlingBuilder,
    BehandleSøknadPåNyttBuilder,
    ForhåndsvisVedtaksbrevTestbuilder,
    OppdaterBehandlingBuilder,
    AvbrytRammebehandlingBuilder,
    OpprettMeldekortbehandlingBuilder,
    AvbrytMeldekortbehandlingBuilder,
    LeggTilbakeMeldekortbehandlingBuilder,
    SendMeldekortbehandlingTilBeslutningBuilder,
    OppdaterMeldekortbehandlingBuilder,
    OvertaMeldekortbehandlingBuilder,
    IverksettMeldekortbehandlingBuilder,
    UnderkjennMeldekortbehandlingBuilder,
    ForhåndsvisVedtaksbrevForMeldekortbehandlingBuilder
