package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import java.time.Clock

// TODO: må kjøre en gang til i hvert miljø for å oppdatere for eksisterende vedtak, kan så slettes
class GenererMeldeperioderService(
    val sakRepo: SakRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    fun genererMeldeperioderForSaker(): List<SakId> {
        return Either.catch {
            val sakIDer: List<SakId> =
                sakRepo.hentSakerSomMåGenerereMeldeperioderFra()

            logger.debug { "Fant ${sakIDer.size} saker som det skal genereres meldeperioder fra" }

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

                    logger.info { "Genererte meldeperioder for sak $sakId - før: ${sak.meldeperiodeKjeder.meldeperioder.size} - etter: ${meldeperioder.size}" }

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
