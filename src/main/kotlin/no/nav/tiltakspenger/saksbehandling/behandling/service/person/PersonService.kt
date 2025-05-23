package no.nav.tiltakspenger.saksbehandling.behandling.service.person

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.person.PersonGateway
import no.nav.tiltakspenger.saksbehandling.person.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

class PersonService(
    private val personRepo: PersonRepo,
    private val personClient: PersonGateway,
) {
    val logger = KotlinLogging.logger {}

    fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr {
        return personRepo.hentFnrForBehandlingId(behandlingId)
    }

    fun hentFnrForSakId(sakId: SakId): Fnr {
        return personRepo.hentFnrForSakId(sakId)
            ?: throw IkkeFunnetException("Fant ikke fnr på sakId: $sakId")
    }

    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr {
        return personRepo.hentFnrForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke fnr for saksnummer: $saksnummer")
    }

    fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr {
        return personRepo.hentFnrForMeldekortId(meldekortId)
            ?: throw IkkeFunnetException("Fant ikke fnr på meldekortId: $meldekortId")
    }

    fun hentFnrForSøknadId(søknadId: SøknadId): Fnr {
        return personRepo.hentFnrForSøknadId(søknadId)
            ?: throw IkkeFunnetException("Fant ikke fnr på søknadId: søknadId")
    }

    /**
     * Merk at denne ikke skal gjøre noen tilgangskontroll.
     */
    suspend fun hentEnkelPersonFnr(fnr: Fnr): Either<KunneIkkeHenteEnkelPerson, EnkelPerson> {
        // TODO post-mvp jah: Her burde klienten logget feilen og gitt en Left.
        return Either.catch {
            personClient.hentEnkelPerson(fnr)
        }.mapLeft {
            logger.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved kall mot PDL. Se sikkerlogg for mer kontekst." }
            Sikkerlogg.error(it) { "Feil ved kall mot PDL for fnr: $fnr." }
            KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl
        }
    }

    suspend fun hentNavn(fnr: Fnr): Navn {
        personClient.hentEnkelPerson(fnr).let {
            return Navn(it.fornavn, it.mellomnavn, it.etternavn)
        }
    }

    suspend fun hentPersonopplysninger(fnr: Fnr): PersonopplysningerSøker {
        return personClient.hentPerson(fnr)
    }
}
