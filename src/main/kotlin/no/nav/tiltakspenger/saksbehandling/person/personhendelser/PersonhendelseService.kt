package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.person.PersonGateway
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import java.util.UUID

class PersonhendelseService(
    private val sakRepo: SakRepo,
    private val personhendelseRepository: PersonhendelseRepository,
    private val personGateway: PersonGateway,
    private val statistikkSakRepo: StatistikkSakRepo,
) {
    private val log = KotlinLogging.logger { }

    suspend fun behandlePersonhendelse(personhendelse: Personhendelse) {
        try {
            if (personhendelse.forelderBarnRelasjon == null && personhendelse.doedsfall == null && personhendelse.adressebeskyttelse == null) {
                log.info { "Kan ikke behandle hendelse med id ${personhendelse.hendelseId} og opplysningstype ${personhendelse.opplysningstype} fordi alle felter mangler. Type: ${personhendelse.endringstype}" }
                return
            }
            if (personhendelse.forelderBarnRelasjon != null && personhendelse.forelderBarnRelasjon.minRolleForPerson == "BARN") {
                return
            }
            if (personhendelse.adressebeskyttelse != null &&
                !(
                    personhendelse.adressebeskyttelse.gradering == Gradering.STRENGT_FORTROLIG ||
                        personhendelse.adressebeskyttelse.gradering == Gradering.STRENGT_FORTROLIG_UTLAND
                    )
            ) {
                return
            }
            personhendelse.personidenter.forEach { ident ->
                val fnr = Fnr.tryFromString(ident) ?: return@forEach
                val saker = sakRepo.hentForFnr(fnr)
                if (saker.saker.isNotEmpty()) {
                    val sak = saker.saker.single()
                    if (personhendelse.opplysningstype == Opplysningstype.ADRESSEBESKYTTELSE_V1.name) {
                        behandleAdressebeskyttelse(fnr, sak.id, personhendelse)
                    } else {
                        val lagredeHendelser = personhendelseRepository.hent(fnr)
                        if (lagredeHendelser.find { it.opplysningstype.name == personhendelse.opplysningstype } != null) {
                            log.info { "Har allerede lagret hendelse av samme type for fnr, hendelsesId ${personhendelse.hendelseId}, ignorerer" }
                            return
                        }
                        personhendelseRepository.lagre(personhendelse.toPersonhendelseDb(fnr, sak.id))
                        log.info { "Lagret hendelse for hendelseId ${personhendelse.hendelseId}" }
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved mottak av personhendelse med hendelseId ${personhendelse.hendelseId}" }
            throw e
        }
    }

    private suspend fun behandleAdressebeskyttelse(
        fnr: Fnr,
        sakId: SakId,
        personhendelse: Personhendelse,
    ) {
        log.info { "Håndterer hendelse om adressebeskyttelse med hendelsesId ${personhendelse.hendelseId}" }
        val pdlPerson = personGateway.hentEnkelPerson(fnr)
        if (pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland) {
            log.info { "Person har adressebeskyttelse, oppdaterer. HendelseId ${personhendelse.hendelseId}" }
            statistikkSakRepo.oppdaterAdressebeskyttelse(sakId)
            log.info { "Har oppdatert statistikktabell for personhendelse med hendelseId ${personhendelse.hendelseId}" }
        }
    }

    private fun Personhendelse.toPersonhendelseDb(fnr: Fnr, sakId: SakId): PersonhendelseDb {
        return PersonhendelseDb(
            id = UUID.randomUUID(),
            fnr = fnr,
            hendelseId = hendelseId,
            opplysningstype = Opplysningstype.valueOf(opplysningstype),
            personhendelseType = toPersonhendelseType(),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
        )
    }

    private fun Personhendelse.toPersonhendelseType(): PersonhendelseType {
        return if (doedsfall != null) {
            PersonhendelseType.Doedsfall(
                doedsdato = doedsfall.doedsdato,
            )
        } else if (forelderBarnRelasjon != null) {
            PersonhendelseType.ForelderBarnRelasjon(
                relatertPersonsIdent = forelderBarnRelasjon.relatertPersonsIdent,
                minRolleForPerson = forelderBarnRelasjon.minRolleForPerson,
            )
        } else {
            throw IllegalArgumentException("Ukjent hendelse for hendelseid $hendelseId og opplysningstype $opplysningstype")
        }
    }
}
