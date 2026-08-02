package no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo.IdenthendelseDb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.infra.repo.IdenthendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseDto
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.util.UUID

class IdenthendelseJobb(
    private val identhendelseRepository: IdenthendelseRepository,
    private val identhendelseKafkaProducer: IdenthendelseKafkaProducer,
    private val sakRepo: SakRepo,
    private val søknadRepo: SøknadRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    private val log = KotlinLogging.logger {}

    fun behandleIdenthendelser() {
        val identhendelseIder = identhendelseRepository.hentIderSomIkkeErBehandlet()
        identhendelseIder.forEach { id ->
            try {
                behandleIdenthendelse(id)
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved behandling av identhendelse med id $id" }
            }
        }
    }

    fun behandleIdenthendelse(identhendelseId: UUID) {
        val identhendelse = identhendelseRepository.hent(identhendelseId) ?: return

        if (identhendelse.produsertHendelse == null) {
            identhendelseKafkaProducer.produserIdenthendelse(identhendelse.id, identhendelse.toIdenthendelseDto())
            identhendelseRepository.oppdaterProdusertHendelse(identhendelse.id)
            log.info { "Oppdatert produsert_hendelse for identhendelse med id ${identhendelse.id}" }
        }

        if (identhendelse.oppdatertDatabase == null) {
            oppdaterFnr(
                gammeltFnr = identhendelse.gammeltFnr,
                nyttFnr = identhendelse.nyttFnr,
            )
            identhendelseRepository.oppdaterOppdatertDatabase(identhendelse.id)
            log.info { "Oppdatert oppdatert_database for identhendelse med id ${identhendelse.id}" }
        }
    }

    private fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sessionFactory.withTransactionContext { tx ->
            sakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr, tx)
            søknadRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr, tx)
            statistikkService.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr, tx)
        }
    }

    private fun IdenthendelseDb.toIdenthendelseDto() =
        IdenthendelseDto(
            gammeltFnr = gammeltFnr.verdi,
            nyttFnr = nyttFnr.verdi,
        )
}
