package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import java.time.LocalDateTime

data class GenererMeldekortVedtakBrevCommand(
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val saksbehandler: String?,
    val beslutter: String?,
    val meldekortbehandlingId: MeldekortId,
    val beregningsperiode: Periode?,
    val tiltaksdeltakelser: Tiltaksdeltakelser,
    val iverksattTidspunkt: LocalDateTime?,
    val erKorrigering: Boolean,
    val beregninger: List<Pair<MeldeperiodeBeregning?, MeldeperiodeBeregning>>?,
    val totaltBeløp: Int?,
    val tekstTilVedtaksbrev: NonBlankString?,
    val forhåndsvisning: Boolean,
)
