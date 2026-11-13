package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.utledPrioriteringForBrukersMeldekort
import java.time.Clock

class MottaBrukerutfyltMeldekortService(
    private val brukersMeldekortRepo: BrukersMeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sakRepo: SakRepo,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    fun mottaBrukerutfyltMeldekort(kommando: LagreBrukersMeldekortKommando): Either<KanIkkeLagreBrukersMeldekort, Unit> {
        val meldekortId = kommando.id
        val meldeperiodeId = kommando.meldeperiodeId

        val meldeperiode = meldeperiodeRepo.hentForMeldeperiodeId(meldeperiodeId)

        requireNotNull(meldeperiode) {
            "Fant ikke meldeperioden $meldeperiodeId for meldekortet $meldekortId"
        }

        val sak = requireNotNull(sakRepo.hentForSakId(kommando.sakId)) {
            "Fant ikke saken ${kommando.sakId} for meldekortet $meldekortId"
        }

        val prioritering = sak.utledPrioriteringForBrukersMeldekort(meldekortId, meldeperiode.kjedeId)

        val nyttMeldekort = kommando.tilNyttBrukersMeldekort(
            meldeperiode,
            prioritering = prioritering,
            clock = clock,
        )

        Either.catch {
            brukersMeldekortRepo.lagre(nyttMeldekort)
        }.onLeft {
            val eksisterendeMeldekort = brukersMeldekortRepo.hentForMeldekortId(meldekortId)

            if (eksisterendeMeldekort == null) {
                with("Kunne ikke lagre brukers meldekort $meldekortId - Ukjent feil") {
                    logger.error(it) { this }
                    Sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                return KanIkkeLagreBrukersMeldekort.UkjentFeil.left()
            }

            if (kommando.matcherBrukersMeldekort(eksisterendeMeldekort)) {
                logger.info { "Meldekortet med id $meldekortId var allerede lagret med samme data" }
                return KanIkkeLagreBrukersMeldekort.AlleredeLagretUtenDiff.left()
            }

            with("Kunne ikke lagre brukers meldekort $meldekortId - Meldekortet er allerede lagret med andre data") {
                logger.error(it) { this }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }

            return KanIkkeLagreBrukersMeldekort.AlleredeLagretMedDiff.left()
        }

        if (prioritering != null) {
            logger.info { "Meldekortet $meldekortId på sak ${sak.id} er en korrigering mottatt før utbetalingen av meldeperioden er iverksatt, og prioriteres i benken" }
        }

        return Unit.right()
    }
}

sealed interface KanIkkeLagreBrukersMeldekort {
    data object AlleredeLagretUtenDiff : KanIkkeLagreBrukersMeldekort

    data object AlleredeLagretMedDiff : KanIkkeLagreBrukersMeldekort

    data object UkjentFeil : KanIkkeLagreBrukersMeldekort
}
