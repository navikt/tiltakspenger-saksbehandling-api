package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.tilOppdaterKommandoStatus
import java.time.LocalDate

data class OppdaterMeldekortbehandlingDTO(
    val meldeperioder: List<OppdatertMeldeperiodeDTO>,
    val begrunnelse: String?,
    val tekstTilVedtaksbrev: String?,
    val skalSendeVedtaksbrev: Boolean,
) {

    data class OppdatertMeldeperiodeDTO(
        val dager: List<OppdaterMeldekortdagDTO>,
        val kjedeId: String,
    )

    data class OppdaterMeldekortdagDTO(
        val dato: LocalDate,
        val status: MeldekortDagStatusDTO,
    )

    fun toDomain(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        sakId: SakId,
        correlationId: CorrelationId,
    ): OppdaterMeldekortbehandlingKommando {
        return OppdaterMeldekortbehandlingKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            meldeperioder = this.meldeperioder.map { mp ->
                OppdatertMeldeperiode(
                    kjedeId = MeldeperiodeKjedeId(mp.kjedeId),
                    dager = mp.dager.map { dag ->
                        OppdatertDag(
                            dag = dag.dato,
                            status = dag.status.tilOppdaterKommandoStatus(),
                        )
                    }.toNonEmptyListOrThrow(),
                )
            }.toNonEmptyListOrThrow(),
            meldekortId = meldekortId,
            begrunnelse = begrunnelse?.let { Begrunnelse.create(it) },
            fritekstTilVedtaksbrev = tekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }
}
