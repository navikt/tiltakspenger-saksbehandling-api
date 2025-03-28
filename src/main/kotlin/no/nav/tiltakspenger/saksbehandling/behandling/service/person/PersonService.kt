package no.nav.tiltakspenger.saksbehandling.behandling.service.person

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.PersonRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
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

    fun hentFnrForBehandlingId(behandlingId: BehandlingId): Fnr =
        personRepo.hentFnrForBehandlingId(behandlingId)
            ?: throw IkkeFunnetException("Fant ikke fnr på behandlingId: $behandlingId")

    fun hentFnrForSakId(sakId: SakId): Fnr =
        personRepo.hentFnrForSakId(sakId)
            ?: throw IkkeFunnetException("Fant ikke fnr på sakId: $sakId")

    fun hentFnrForSaksnummer(saksnummer: Saksnummer): Fnr =
        personRepo.hentFnrForSaksnummer(saksnummer)
            ?: throw IkkeFunnetException("Fant ikke fnr for saksnummer: $saksnummer")

    fun hentFnrForMeldekortId(meldekortId: MeldekortId): Fnr =
        personRepo.hentFnrForMeldekortId(meldekortId)
            ?: throw IkkeFunnetException("Fant ikke fnr på meldekortId: $meldekortId")

    fun hentFnrForSøknadId(søknadId: SøknadId): Fnr =
        personRepo.hentFnrForSøknadId(søknadId)
            ?: throw IkkeFunnetException("Fant ikke fnr på søknadId: søknadId")

    suspend fun hentEnkelPersonFnr(fnr: Fnr): Either<no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson, EnkelPerson> {
        // TODO post-mvp jah: Her burde klienten logget feilen og gitt en Left.
        return Either.catch {
            personClient.hentEnkelPerson(fnr)
        }.mapLeft {
            logger.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Feil ved kall mot PDL. Se sikkerlogg for mer kontekst." }
            sikkerlogg.error(it) { "Feil ved kall mot PDL for fnr: $fnr." }
            no.nav.tiltakspenger.saksbehandling.behandling.service.person.KunneIkkeHenteEnkelPerson.FeilVedKallMotPdl
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
