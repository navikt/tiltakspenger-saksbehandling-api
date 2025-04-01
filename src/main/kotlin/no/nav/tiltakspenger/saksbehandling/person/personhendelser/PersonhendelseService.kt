package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import java.util.UUID

class PersonhendelseService(
    private val sakRepo: SakRepo,
    private val personhendelseRepository: PersonhendelseRepository,
) {
    private val log = KotlinLogging.logger { }

    fun behandlePersonhendelse(personhendelse: Personhendelse) {
        personhendelse.personidenter.forEach { ident ->
            val fnr = Fnr.tryFromString(ident) ?: return@forEach
            val saker = sakRepo.hentForFnr(fnr)
            if (saker.saker.isNotEmpty()) {
                val lagredeHendelser = personhendelseRepository.hent(fnr)
                if (lagredeHendelser.find { it.opplysningstype.name == personhendelse.opplysningstype } != null) {
                    log.info { "Har allerede lagret hendelse av samme type for fnr, hendelsesId ${personhendelse.hendelseId}, ignorerer" }
                    return
                }
                val sak = saker.saker.single()
                personhendelseRepository.lagre(personhendelse.toPersonhendelseDb(fnr, sak.id))
                log.info { "Lagret hendelse for hendelseId ${personhendelse.hendelseId}" }
            }
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
        if (doedsfall != null) {
            return PersonhendelseType.Doedsfall(
                doedsdato = doedsfall.doedsdato,
            )
        } else if (forelderBarnRelasjon != null) {
            return PersonhendelseType.ForelderBarnRelasjon(
                relatertPersonsIdent = forelderBarnRelasjon.relatertPersonsIdent,
                minRolleForPerson = forelderBarnRelasjon.minRolleForPerson,
            )
        } else {
            throw IllegalArgumentException("Ukjent hendelse for hendelseid $hendelseId og opplysningstype $opplysningstype")
        }
    }
}
