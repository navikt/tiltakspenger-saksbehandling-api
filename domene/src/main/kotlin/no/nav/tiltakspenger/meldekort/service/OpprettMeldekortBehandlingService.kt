package no.nav.tiltakspenger.meldekort.service

import arrow.core.getOrElse
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.opprettMeldekortBehandling
import no.nav.tiltakspenger.meldekort.ports.MeldekortRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class OpprettMeldekortBehandlingService(
    val sakService: SakService,
    val meldekortRepo: MeldekortRepo,
    val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun opprettBehandling(
        hendelseId: HendelseId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ) {
        logger.info { "Oppretter behandling av $hendelseId på sak $sakId" }

        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            throw IllegalArgumentException("Kunne ikke hente sak med id $sakId")
        }.also {
            if (it.hentMeldekortBehandling(hendelseId) != null) {
                throw IllegalStateException("Det finnes allerede en behandling av $hendelseId: ${it.id}")
            }
        }

        val meldeperiode = sak.hentMeldeperiode(hendelseId = hendelseId)
            ?: throw IllegalArgumentException("Fant ingen meldeperiode med id $hendelseId for sak $sakId")

        val meldekortBehandling = sak.opprettMeldekortBehandling(meldeperiode)

        sessionFactory.withTransactionContext { tx ->
            meldekortRepo.lagre(meldekortBehandling)
        }
    }
}
