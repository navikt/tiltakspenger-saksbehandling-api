package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandling.ports.SakRepo

class GenererMeldeperioderService(
    val sakRepo: SakRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    fun genererMeldeperioderForSaker(): List<Either<SakId, SakId>> {
        val sakIDer: List<SakId> = meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(Sak.ikkeGenererEtter())
        val resultat = sakIDer.map { sakId ->
            Either.catch {
                val sak = sakRepo.hentForSakId(sakId)!!
                val (sakMedNyeMeldeperioder, meldeperioder) = sak.genererMeldeperioder()
                sessionFactory.withTransactionContext { tx ->
                    sakRepo.oppdaterSisteDagSomGirRett(
                        sakId = sakId,
                        sisteDagSomGirRett = sakMedNyeMeldeperioder.sisteDagSomGirRett,
                        sessionContext = tx,
                    )
                    meldeperiodeRepo.lagre(meldeperioder, tx)
                }
                sakId
            }.mapLeft {
                logger.error(it) { "Feil oppstod ved generering av nye meldeperioder for sak $sakId" }
                sakId
            }
        }
        return resultat
    }
}
