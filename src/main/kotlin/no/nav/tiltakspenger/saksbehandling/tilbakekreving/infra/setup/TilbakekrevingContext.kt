package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.setup

import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.Producer
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingHendelseRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.TilbakekrevingshendelsePolicy
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.jobb.BehandleTilbakekrevingHendelserJobb
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingKafkaProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingProducer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingBehandlingPostgresRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelsePostgresRepo
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.service.TilbakekrevingBehandlingTildelingService
import java.time.Clock

open class TilbakekrevingContext(
    sessionFactory: SessionFactory,
    sakRepo: SakRepo,
    sakService: SakService,
    clock: Clock,
    hendelsePolicy: TilbakekrevingshendelsePolicy,
) {
    open val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo by lazy {
        TilbakekrevingHendelsePostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    open val tilbakekrevingBehandlingRepo: TilbakekrevingBehandlingRepo by lazy {
        TilbakekrevingBehandlingPostgresRepo(
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
    }

    open val tilbakekrevingProducer: TilbakekrevingProducer by lazy {
        TilbakekrevingKafkaProducer(
            topic = Configuration.tilbakekrevingTopic,
            kafkaProducer = Producer(producerConfig = KafkaConfig.fraNaisEnv().producerConfig()),
        )
    }

    val tilbakekrevingBehandlingTildelingService by lazy {
        TilbakekrevingBehandlingTildelingService(
            sakService = sakService,
            tilbakekrevingBehandlingRepo = tilbakekrevingBehandlingRepo,
            sessionFactory = sessionFactory,
            clock = clock,
        )
    }

    val behandleTilbakekrevingHendelserJobb by lazy {
        BehandleTilbakekrevingHendelserJobb(
            tilbakekrevingHendelseRepo = tilbakekrevingHendelseRepo,
            tilbakekrevingBehandlingRepo = tilbakekrevingBehandlingRepo,
            sakRepo = sakRepo,
            tilbakekrevingProducer = tilbakekrevingProducer,
            sessionFactory = sessionFactory,
            clock = clock,
            hendelsePolicy = hendelsePolicy,
            frontendUrl = Configuration.saksbehandlingFrontendUrl,
        )
    }
}
