package no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.setup

import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.Producer
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.IdenthendelseService
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb.IdenthendelseJobb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseProducer
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

open class IdenthendelseContext(
    sessionFactory: SessionFactory,
    sakRepo: SakRepo,
    søknadRepo: SøknadRepo,
    statistikkService: StatistikkService,
    clock: Clock,
) {
    open val identhendelseRepository: IdenthendelseRepository by lazy {
        IdenthendelseRepository(
            sessionFactory = sessionFactory as PostgresSessionFactory,
            clock = clock,
        )
    }

    val identhendelseService: IdenthendelseService by lazy {
        IdenthendelseService(
            sakRepo = sakRepo,
            identhendelseRepository = identhendelseRepository,
        )
    }

    open val identhendelseProducer: IdenthendelseProducer by lazy {
        IdenthendelseKafkaProducer(
            kafkaProducer = Producer(producerConfig = KafkaConfig.fraNaisEnv().producerConfig()),
            topic = Configuration.identhendelseTopic,
        )
    }

    val identhendelseJobb by lazy {
        IdenthendelseJobb(
            identhendelseRepository = identhendelseRepository,
            identhendelseProducer = identhendelseProducer,
            sakRepo = sakRepo,
            søknadRepo = søknadRepo,
            statistikkService = statistikkService,
            sessionFactory = sessionFactory,
        )
    }
}
