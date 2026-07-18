package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.Opplysningstype
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseDb
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseRepository
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.repo.PersonhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.util.UUID

class PersonhendelseService(
    private val sakRepo: SakRepo,
    private val personhendelseRepository: PersonhendelseRepository,
    private val personKlient: PersonKlient,
    private val statistikkService: StatistikkService,
) {
    private val log = KotlinLogging.logger { }

    suspend fun behandlePersonhendelse(
        personhendelse: Personhendelse,
    ): Either<KunneIkkeBehandlePersonhendelse, Unit> {
        try {
            // pdl.leesah-v1 inneholder mange opplysningstyper vi ikke bryr oss om (NAVN_V1, FOLKEREGISTERIDENTIFIKATOR_V1, BOSTEDSADRESSE_V1, ...).
            // Vi siler dem ut her, før vi går mot DB-en, og lar bare de typene vi faktisk håndterer passere.
            // Enum-en [Opplysningstype] inneholder per design kun de støttede typene.
            when (personhendelse.opplysningstype) {
                Opplysningstype.DOEDSFALL_V1.name,
                Opplysningstype.ADRESSEBESKYTTELSE_V1.name,
                -> Unit

                else -> return KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()
            }

            // TODO jah: Denne filtrerer vekk blant annet ANNULLERINGER, som vi i høyeste grad vil ta vare på.
            if (personhendelse.doedsfall == null && personhendelse.adressebeskyttelse == null) {
                log.info { "Kan ikke behandle hendelse med id ${personhendelse.hendelseId} og opplysningstype ${personhendelse.opplysningstype} fordi alle felter mangler. Type: ${personhendelse.endringstype}" }
                return KunneIkkeBehandlePersonhendelse.PayloadMangler.left()
            }

            // Vi bryr oss kun om kode 6 (STRENGT_FORTROLIG[_UTLAND]) på adressebeskyttelse-hendelser.
            // Kode 6 endrer hvordan sak rapporteres til statistikk (skjerming av lokasjon o.l.)
            // TODO jah: Dersom en bruker går fra kode 6 til noe annet, burde vi oppdatere statistikken i de tilfellene og?
            // og er den eneste graderingen som krever at vi oppretter skjermings-/oppgavebehov.
            // FORTROLIG (kode 7) og UGRADERT trigger ingen oppgave hos oss, da Oslo får disse opp i tp-sak.
            if (personhendelse.adressebeskyttelse != null) {
                when (personhendelse.adressebeskyttelse.gradering) {
                    Gradering.STRENGT_FORTROLIG,
                    Gradering.STRENGT_FORTROLIG_UTLAND,
                    -> Unit

                    else -> return KunneIkkeBehandlePersonhendelse.AdressebeskyttelseErIkkeKode6.left()
                }
            }

            // personidenter er en liste av identer (fnr, d-nummer, aktørId — historiske og aktive) som alle peker på samme person.
            // Vi vet ikke formatet på de enkelte identene fra leesah, så vi sender alle strengene rett til DB-en og matcher mot sak.fnr.
            val personidenter = personhendelse.personidenter.map { it.trim() }.toNonEmptyListOrNull()
                ?: run {
                    // Forventer ikke at dette skjer i praksis.
                    log.warn { "Personhendelse mangler personidenter, ignorerer. HendelseId ${personhendelse.hendelseId}, opplysningstype ${personhendelse.opplysningstype}" }
                    return KunneIkkeBehandlePersonhendelse.PersonidenterMangler.left()
                }
            val (fnr, sakId) = sakRepo.hentSakIdForPersonidenter(personidenter)
                ?: run {
                    // Vi logger ikke disse, da de aller fleste hendelsene ikke har en sak hos oss.
                    return KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()
                }
            try {
                val lagredeHendelser = personhendelseRepository.hent(sakId)
                // TODO jah: Dette blir ikke riktig.
                // Det er greit å deduppe på hendelseId, men her forkaster vi potensielt viktig informasjon.
                // Dette må gjøres om litt mer helhetlig.
                if (lagredeHendelser.find { it.opplysningstype.name == personhendelse.opplysningstype } != null) {
                    log.info { "Har allerede lagret hendelse av samme type for fnr, hendelsesId ${personhendelse.hendelseId}, ignorerer" }
                    return KunneIkkeBehandlePersonhendelse.HendelseAlleredeLagret.left()
                }
                if (personhendelse.opplysningstype == Opplysningstype.ADRESSEBESKYTTELSE_V1.name) {
                    log.info { "Håndterer hendelse om adressebeskyttelse med hendelsesId ${personhendelse.hendelseId}" }
                    if (!harKode6(fnr)) {
                        log.info { "Har ikke kode 6, hendelsesId ${personhendelse.hendelseId}, ignorerer" }
                        return KunneIkkeBehandlePersonhendelse.IkkeKode6IPdl.left()
                    }
                    // TODO jah: Dette bør ikke skje samtidig som vi mottar en hendelse. a) lagre b) side-effekter.
                    // Mangler og en transaksjon.
                    oppdaterStatistikk(sakId, personhendelse)
                }
                personhendelseRepository.lagre(personhendelse.toPersonhendelseDb(fnr, sakId))
                log.info { "Lagret hendelse for hendelseId ${personhendelse.hendelseId}" }
                return Unit.right()
            } catch (e: Exception) {
                Sikkerlogg.error(e) { "Noe gikk galt ved behandling av personhendelse med ident ${fnr.verdi}" }
                throw e
            }
        } catch (e: Exception) {
            log.error(e) { "Noe gikk galt ved mottak av personhendelse med hendelseId ${personhendelse.hendelseId}" }
            throw e
        }
    }

    private fun oppdaterStatistikk(
        sakId: SakId,
        personhendelse: Personhendelse,
    ) {
        log.info { "Person har adressebeskyttelse, oppdaterer statistikktabeller. HendelseId ${personhendelse.hendelseId}" }
        statistikkService.oppdaterAdressebeskyttelse(sakId)
        log.info { "Har oppdatert statistikktabell for personhendelse med hendelseId ${personhendelse.hendelseId}" }
    }

    private suspend fun harKode6(fnr: Fnr): Boolean {
        val pdlPerson = personKlient.hentEnkelPerson(fnr)
        return pdlPerson.strengtFortrolig || pdlPerson.strengtFortroligUtland
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
        } else if (adressebeskyttelse != null) {
            PersonhendelseType.Adressebeskyttelse(
                gradering = adressebeskyttelse.gradering.name,
            )
        } else {
            throw IllegalArgumentException("Ukjent hendelse for hendelseid $hendelseId og opplysningstype $opplysningstype")
        }
    }
}
