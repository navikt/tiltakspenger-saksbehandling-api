package no.nav.tiltakspenger.saksbehandling.person.identhendelser.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseDto
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.IdenthendelseKafkaProducer
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseDb
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo.IdenthendelseRepository

class IdenthendelseJobb(
    private val identhendelseRepository: IdenthendelseRepository,
    private val identhendelseKafkaProducer: IdenthendelseKafkaProducer,
    private val sakRepo: SakRepo,
    private val søknadRepo: SøknadRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,

) {
    private val log = KotlinLogging.logger {}

    fun behandleIdenthendelser() {
        val identhendelser = identhendelseRepository.hentAlleSomIkkeErBehandlet()
        identhendelser.forEach { identhendelse ->
            try {
                if (identhendelse.produsertHendelse == null) {
                    identhendelseKafkaProducer.produserIdenthendelse(identhendelse.id, identhendelse.toIdenthendelseDto())
                    identhendelseRepository.oppdaterProdusertHendelse(identhendelse.id)
                    log.info { "Oppdatert produsert_hendelse for identhendelse med id ${identhendelse.id}" }
                }

                val harProdusertHendelse = identhendelse.produsertHendelse != null || identhendelseRepository.hent(identhendelse.id)?.produsertHendelse != null
                if (harProdusertHendelse && identhendelse.oppdatertDatabase == null) {
                    oppdaterFnr(
                        gammeltFnr = identhendelse.gammeltFnr,
                        nyttFnr = identhendelse.nyttFnr,
                    )
                    identhendelseRepository.oppdaterOppdatertDatabase(identhendelse.id)
                    log.info { "Oppdatert oppdatert_database for identhendelse med id ${identhendelse.id}" }
                }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved behandling av identhendelse med id ${identhendelse.id}" }
            }
        }
    }

    private fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr) {
        sakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        søknadRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        statistikkSakRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
        statistikkStønadRepo.oppdaterFnr(gammeltFnr = gammeltFnr, nyttFnr = nyttFnr)
    }

    private fun IdenthendelseDb.toIdenthendelseDto() =
        IdenthendelseDto(
            gammeltFnr = gammeltFnr.verdi,
            nyttFnr = nyttFnr.verdi,
        )
}
