package no.nav.tiltakspenger.saksbehandling.common

import no.nav.tiltakspenger.libs.auth.test.core.JwtGenerator
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.infra.http.KabalClientFake
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingFakeProducer
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.UtbetalingFakeKlient

abstract class TestApplicationContext(
    override val clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
) : ApplicationContext(
    gitHash = "fake-git-hash",
    clock = clock,
) {
    abstract fun leggTilPerson(
        fnr: Fnr,
        person: EnkelPerson,
        tiltaksdeltakelse: Tiltaksdeltakelse,
    )

    abstract val jwtGenerator: JwtGenerator

    abstract fun leggTilBruker(token: String, bruker: Bruker<*, *>)

    abstract fun oppdaterTiltaksdeltakelse(fnr: Fnr, tiltaksdeltakelse: Tiltaksdeltakelse?)

    abstract val tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient

    val kabalClientFake by lazy { KabalClientFake(clock) }

    protected val utbetalingFakeKlient by lazy {
        UtbetalingFakeKlient(sakContext.sakRepo, tilbakekrevingHendelseRepo, clock)
    }

    override val tilbakekrevingProducer by lazy {
        TilbakekrevingFakeProducer(
            tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            sakRepo = sakContext.sakRepo,
            clock = clock,
        )
    }
}
