package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import mu.KotlinLogging
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
        val saker: List<Sak> = meldeperiodeRepo.hentSakerSomMåGenerereMeldeperioderFra(Sak.ikkeGenererEtter())
        val resultat = saker.map { sak ->
            Either.catch {
                val (sakMedNyeMeldeperioder, meldeperioder) = sak.genererMeldeperioder()
                sessionFactory.withTransactionContext { tx ->
                    sakRepo.oppdaterSisteDagSomGirRett(
                        sakId = sak.id,
                        sisteDagSomGirRett = sakMedNyeMeldeperioder.sisteDagSomGirRett,
                        sessionContext = tx,
                    )
                    meldeperiodeRepo.lagre(meldeperioder, tx)
                }
                sak.id
            }.mapLeft {
                logger.error(it) { "Feil oppstod ved generering av nye meldeperioder for sak ${sak.id}" }
                sak.id
            }
        }
        return resultat
    }
}
