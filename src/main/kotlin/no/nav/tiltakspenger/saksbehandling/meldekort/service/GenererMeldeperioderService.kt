package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class GenererMeldeperioderService(
    val sakRepo: SakRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    fun genererMeldeperioderForSaker(): List<SakId> {
        return Either.catch {
            val sakIDer: List<SakId> = sakRepo.hentSakerSomMåGenerereMeldeperioderFra(Sak.ikkeGenererEtter(clock))

            sakIDer.mapNotNull { sakId ->
                Either.catch {
                    val sak = sakRepo.hentForSakId(sakId)!!
                    val (sakMedNyeMeldeperioder, meldeperioder) = sak.genererMeldeperioder(clock)
                    sessionFactory.withTransactionContext { tx ->
                        sakRepo.oppdaterFørsteOgSisteDagSomGirRett(
                            sakId = sakId,
                            førsteDagSomGirRett = sakMedNyeMeldeperioder.førsteDagSomGirRett,
                            sisteDagSomGirRett = sakMedNyeMeldeperioder.sisteDagSomGirRett,
                            sessionContext = tx,
                        )
                        meldeperiodeRepo.lagre(meldeperioder, tx)
                    }

                    sakId
                }.getOrElse {
                    logger.error(it) { "Feil oppstod ved generering av nye meldeperioder for sak $sakId" }
                    null
                }
            }
        }.getOrElse {
            logger.error(it) { "Feil oppstod ved generering av nye meldeperioder" }
            emptyList()
        }
    }
}
