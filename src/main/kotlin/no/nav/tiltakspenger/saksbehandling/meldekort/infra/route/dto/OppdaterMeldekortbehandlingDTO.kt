package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev.Companion.toFritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import java.time.LocalDate

data class OppdaterMeldekortbehandlingDTO(
    val dager: List<Dag>,
    val begrunnelse: String?,
    val tekstTilVedtaksbrev: String?,
    val skalSendeVedtaksbrev: Boolean,
) {
    data class Dag(
        val dato: LocalDate,
        val status: OppdaterMeldekortbehandlingKommando.Status,
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
            dager = OppdaterMeldekortbehandlingKommando.Dager(
                this.dager.map { dag ->
                    OppdaterMeldekortbehandlingKommando.Dager.Dag(
                        dag = dag.dato,
                        status = dag.status,
                    )
                }.toNonEmptyListOrNull()!!,
            ),
            meldekortId = meldekortId,
            begrunnelse = begrunnelse?.let { Begrunnelse.create(it) },
            fritekstTilVedtaksbrev = tekstTilVedtaksbrev?.toFritekstTilVedtaksbrev(),
            skalSendeVedtaksbrev = skalSendeVedtaksbrev,
        )
    }
}
