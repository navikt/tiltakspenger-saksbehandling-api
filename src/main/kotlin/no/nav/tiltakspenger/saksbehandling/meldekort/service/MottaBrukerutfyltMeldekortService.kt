package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando

class MottaBrukerutfyltMeldekortService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
) {
    val logger = KotlinLogging.logger { }

    fun mottaBrukerutfyltMeldekort(lagreKommando: LagreBrukersMeldekortKommando): Either<KanIkkeLagreBrukersMeldekort, Unit> {
        Either.catch {
            return brukersMeldekortRepo.lagre(lagreKommando).right()
        }.getOrElse {
            val meldekortId = lagreKommando.id
            val eksisterendeMeldekort = brukersMeldekortRepo.hentForMeldekortId(meldekortId)

            if (eksisterendeMeldekort == null) {
                with("Kunne ikke lagre brukers meldekort $meldekortId - Ukjent feil") {
                    logger.error { this }
                    sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                return KanIkkeLagreBrukersMeldekort.UkjentFeil.left()
            }

            if (kommandoMatcherMeldekort(lagreKommando, eksisterendeMeldekort)) {
                logger.info { "Meldekortet med id $meldekortId var allerede lagret med samme data" }
                return KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left()
            }

            with("Kunne ikke lagre brukers meldekort $meldekortId - Meldekortet er allerede lagret med andre data") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - ${it.message}" }
            }

            return KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left()
        }
    }

    private fun kommandoMatcherMeldekort(lagreKommando: LagreBrukersMeldekortKommando, meldekort: BrukersMeldekort): Boolean {
        return lagreKommando.id == meldekort.id &&
            lagreKommando.meldeperiodeId == meldekort.meldeperiode.id &&
            lagreKommando.sakId == meldekort.sakId &&
            lagreKommando.journalpostId == meldekort.journalpostId &&
            lagreKommando.dager == meldekort.dager &&
            lagreKommando.mottatt == meldekort.mottatt
    }
}

sealed interface KanIkkeLagreBrukersMeldekort {
    data object AlleredeLagretUtenDiff : KanIkkeLagreBrukersMeldekort
    data object AlleredeLagretMedDiff : KanIkkeLagreBrukersMeldekort
    data object UkjentFeil : KanIkkeLagreBrukersMeldekort
}
