package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import java.time.LocalDate

data class OppdaterMeldekortDTO(
    val dager: List<Dag>,
    val begrunnelse: String? = null,
) {
    data class Dag(
        val dato: LocalDate,
        val status: OppdaterMeldekortKommando.Status,
    )

    fun toDomain(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        sakId: SakId,
        correlationId: CorrelationId,
    ): OppdaterMeldekortKommando {
        return OppdaterMeldekortKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            dager = OppdaterMeldekortKommando.Dager(
                this.dager.map { dag ->
                    OppdaterMeldekortKommando.Dager.Dag(
                        dag = dag.dato,
                        status = dag.status,
                    )
                }.toNonEmptyListOrNull()!!,
            ),
            meldekortId = meldekortId,
            begrunnelse = begrunnelse?.let { MeldekortBehandlingBegrunnelse(verdi = it) },
        )
    }
}
