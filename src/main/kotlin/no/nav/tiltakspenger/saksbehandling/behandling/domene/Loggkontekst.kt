package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.CorrelationId

/**
 * Felles loggkontekst for en behandling, uten personidentifiserende felter.
 * Gir en logglinje nok kontekst til at et enkelt treff kan følges opp direkte, uten å måtte sammenstille flere linjer.
 * Send med [correlationId] der den er tilgjengelig, slik at linjen kan korreleres på tvers av tjenester.
 */
fun AttesterbarBehandling.loggkontekst(correlationId: CorrelationId? = null): String {
    return listOfNotNull(
        "sakId: $sakId",
        "saksnummer: $saksnummer",
        "behandlingId: $id",
        saksbehandler?.let { "saksbehandler: $it" },
        beslutter?.let { "beslutter: $it" },
        correlationId?.let { "correlationId: $it" },
    ).joinToString(", ")
}
