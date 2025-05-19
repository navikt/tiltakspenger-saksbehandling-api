package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import java.time.LocalDate

/**
 * Dersom begrunnelse eller dager er null, så kan vi ikke endre dagene eller begrunnelsen på behandlingen. Siden disse oppdateres med oppdater-routen.
 * Men domenemodellen må sjekke at dager og begrunnelse er i gyldig tilstand før vi sender til beslutter.
 */
data class SendMeldekortTilBeslutterDTO(
    val dager: List<Dag>? = null,
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
    ): SendMeldekortTilBeslutterKommando {
        return SendMeldekortTilBeslutterKommando(
            sakId = sakId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            dager = dager?.let { dager ->
                OppdaterMeldekortKommando.Dager(
                    dager.map { dag ->
                        OppdaterMeldekortKommando.Dager.Dag(
                            dag = dag.dato,
                            status = dag.status,
                        )
                    }.toNonEmptyListOrNull()!!,
                )
            },
            meldekortId = meldekortId,
            begrunnelse = begrunnelse?.let { MeldekortBehandlingBegrunnelse(verdi = saniter(it)) },
        )
    }
}
