package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.UtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtaksliste
import java.time.LocalDateTime

data class MeldekortVedtakDto(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val meldekortId: String,
    val kjedeId: String,
    val opprettet: LocalDateTime,
    val saksbehandler: String,
    val beslutter: String,
    val periode: PeriodeDTO,
    val beregningsperiode: PeriodeDTO,
    val dager: List<MeldekortDagDTO>,
    val beregning: MeldekortBeregningDTO,
    val automatiskBehandlet: Boolean,
    val erKorrigering: Boolean,
    val begrunnelse: String?,
    val journalpostId: String?,
)

fun Meldekortvedtak.toDto(): MeldekortVedtakDto = MeldekortVedtakDto(
    id = id.toString(),
    sakId = sakId.toString(),
    saksnummer = saksnummer.verdi,
    meldekortId = meldekortId.toString(),
    opprettet = opprettet,
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    periode = periode.toDTO(),
    beregningsperiode = beregningsperiode.toDTO(),
    beregning = beregning.tilMeldekortBeregningDTO(),
    automatiskBehandlet = erAutomatiskBehandlet,
    erKorrigering = erKorrigering,
    begrunnelse = begrunnelse,
    journalpostId = journalpostId?.toString(),
    // Disse to feltene skal fjernes på sikt
    kjedeId = meldekortbehandling.kjedeIdLegacy.toString(),
    dager = UtfyltMeldeperiode(
        meldekortbehandling.dagerLegacy.dager,
        meldekortbehandling.meldeperiodeLegacy,
    ).tilMeldekortDagerDTO(),
)

fun Meldekortvedtaksliste.toDto(): List<MeldekortVedtakDto> = map { it.toDto() }
