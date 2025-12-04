package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev

/**
 * Representerer en saksbehandler som fyller ut hele meldekortet, begrunnelsen og sender til beslutter.
 * Vi gjør ingen validering i denne klassen, det gjøres heller av [MeldekortBehandletManuelt]
 *
 * TODO John og Anders: Vurder om send til beslutter skal være en ren kommando som ikke inneholder [dager] eller [begrunnelse]
 */
class SendMeldekortTilBeslutterKommando(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val saksbehandler: Saksbehandler,
    val dager: OppdaterMeldekortKommando.Dager?,
    val begrunnelse: Begrunnelse?,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    val correlationId: CorrelationId,
) {
    val periode: Periode? = dager?.let { Periode(dager.first().dag, dager.last().dag) }
}
