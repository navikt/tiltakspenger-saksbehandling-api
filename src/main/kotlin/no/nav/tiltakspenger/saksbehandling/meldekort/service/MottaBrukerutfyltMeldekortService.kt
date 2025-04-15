package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo

class MottaBrukerutfyltMeldekortService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
) {
    val logger = KotlinLogging.logger { }

    fun mottaBrukerutfyltMeldekort(kommando: LagreBrukersMeldekortKommando): Either<KanIkkeLagreBrukersMeldekort, Unit> {
        val meldekortId = kommando.id
        val meldeperiodeId = kommando.meldeperiodeId

        val meldeperiode = meldeperiodeRepo.hentForMeldeperiodeId(meldeperiodeId)

        requireNotNull(meldeperiode) {
            "Fant ikke meldeperioden $meldeperiodeId for meldekortet $meldekortId"
        }

        val nyttMeldekort = kommando.tilBrukersMeldekort(meldeperiode)

        Either.catch {
            brukersMeldekortRepo.lagre(nyttMeldekort)
        }.onLeft {
            val eksisterendeMeldekort = brukersMeldekortRepo.hentForMeldekortId(meldekortId)

            if (eksisterendeMeldekort == null) {
                with("Kunne ikke lagre brukers meldekort $meldekortId - Ukjent feil") {
                    logger.error { this }
                    sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                return KanIkkeLagreBrukersMeldekort.UkjentFeil.left()
            }

            if (nyttMeldekort == eksisterendeMeldekort) {
                logger.info { "Meldekortet med id $meldekortId var allerede lagret med samme data" }
                return KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left()
            }

            with("Kunne ikke lagre brukers meldekort $meldekortId - Meldekortet er allerede lagret med andre data") {
                logger.error { this }
                sikkerlogg.error(it) { "$this - ${it.message}" }
            }

            return KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left()
        }

        return Unit.right()
    }

//    private suspend fun behandleMeldekort(meldekort: BrukersMeldekort) {
//        val sakId = meldekort.sakId
//
//        // TODO: sjekk noe greier her :D
//        val sak = sakRepo.hentForSakId(sakId)!!
//
//        val navkontor = Either.catch {
//            navkontorService.hentOppfolgingsenhet(sak.fnr)
//        }.getOrElse {
//            with("Kunne ikke hente navkontor for sak $sakId") {
//                logger.error { this }
//                no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg.error(it) { "$this - fnr ${sak.fnr.verdi}" }
//            }
//            return
//        }
//    }
}

sealed interface KanIkkeLagreBrukersMeldekort {
    data object AlleredeLagretUtenDiff : KanIkkeLagreBrukersMeldekort
    data object AlleredeLagretMedDiff : KanIkkeLagreBrukersMeldekort
    data object UkjentFeil : KanIkkeLagreBrukersMeldekort
}
