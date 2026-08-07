package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.routes

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlagebehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortType
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknadstype
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKilde
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingStatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Fane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Ventestatus
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.finnGyldigeKommandoer
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.BenkV2Respons
import no.nav.tiltakspenger.saksbehandling.benk.v2.service.TilgangsfiltrertBenkV2Oversikt
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandler.tilDTO
import java.math.BigDecimal

/**
 * Wiretypene for benk v2.
 *
 * Feltnavnene er kontrakten mot frontendens `lib/benk/v2/typer`, og fellesfeltene ligger flatt på hver rad — ikke under et `felles`-objekt — fordi det er slik frontenden leser dem.
 */

data class BenkV2ResponsDTO(
    val tab: BenkV2FaneDTO,
    val antallPerTab: Map<BenkV2FaneDTO, Int>,
    val oversikt: BenkV2OversiktDTO,
)

data class BenkV2OversiktDTO(
    val behandlinger: List<BenkV2BehandlingDTO>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
    val limit: Int,
)

enum class BenkV2FaneDTO {
    SØKNADER,
    REVURDERINGER,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,
}

enum class BenkV2BehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    KLAR_TIL_FERDIGSTILLING,
}

enum class BenkSøknadstypeDTO {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

enum class BenkSøknadsbehandlingResultatDTO {
    INNVILGELSE,
    AVSLAG,
    IKKE_VALGT,
}

enum class BenkRevurderingResultatDTO {
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
}

enum class BenkKlagebehandlingResultatDTO {
    AVVIST,
    OMGJØR,
    OPPRETTHOLDT,
}

enum class BenkTilbakekrevingStatusDTO {
    OPPRETTET,
    TIL_FORHÅNDSVARSEL,
    UNDER_FORHÅNDSVARSLING,
    TIL_BEHANDLING,
    UNDER_BEHANDLING,
    TIL_GODKJENNING,
    UNDER_GODKJENNING,
}

enum class BenkTilbakekrevingKildeDTO {
    RAMMEVEDTAK,
    MELDEKORT,
}

enum class BenkV2BehandlingstypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    KLAGEBEHANDLING,
    TILBAKEKREVING,
}

data class BenkV2VentestatusDTO(
    val erSattPåVent: Boolean,
    val begrunnelse: String?,
    val frist: String?,
)

data class BenkV2PeriodeDTO(
    val fraOgMed: String,
    val tilOgMed: String,
)

sealed interface BenkV2BehandlingDTO {
    val type: BenkV2BehandlingstypeDTO
    val id: String
    val sakId: String
    val fnr: String
    val saksnummer: String
    val startet: String
    val sistEndret: String
    val saksbehandler: String?
    val beslutter: String?
    val erUnderkjent: Boolean
    val ventestatus: BenkV2VentestatusDTO
}

data class BenkSøknadsbehandlingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.SØKNADSBEHANDLING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkV2BehandlingsstatusDTO,
    val søknadstype: BenkSøknadstypeDTO,
    val kravtidspunkt: String,
    val resultat: BenkSøknadsbehandlingResultatDTO?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

data class BenkRevurderingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.REVURDERING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkV2BehandlingsstatusDTO,
    val resultat: BenkRevurderingResultatDTO?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

data class BenkMeldekortDTO(
    override val type: BenkV2BehandlingstypeDTO,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkV2BehandlingsstatusDTO,
    val periode: BenkV2PeriodeDTO,
    val beløp: Int?,
    val mottattTidspunkt: String?,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

data class BenkKlagebehandlingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.KLAGEBEHANDLING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkV2BehandlingsstatusDTO,
    val kravtidspunkt: String,
    val resultat: BenkKlagebehandlingResultatDTO?,
) : BenkV2BehandlingDTO

data class BenkTilbakekrevingDTO(
    override val type: BenkV2BehandlingstypeDTO = BenkV2BehandlingstypeDTO.TILBAKEKREVING,
    override val id: String,
    override val sakId: String,
    override val fnr: String,
    override val saksnummer: String,
    override val startet: String,
    override val sistEndret: String,
    override val saksbehandler: String?,
    override val beslutter: String?,
    override val erUnderkjent: Boolean,
    override val ventestatus: BenkV2VentestatusDTO,
    val status: BenkTilbakekrevingStatusDTO,
    val beløp: BigDecimal,
    val kilde: BenkTilbakekrevingKildeDTO,
    val kravgrunnlagPeriode: BenkV2PeriodeDTO,
    val url: String,
    val gyldigeKommandoer: List<SaksbehandlerBehandlingKommandoDTO>,
) : BenkV2BehandlingDTO

fun BenkV2Fane.toDTO(): BenkV2FaneDTO = when (this) {
    BenkV2Fane.SØKNADER -> BenkV2FaneDTO.SØKNADER
    BenkV2Fane.REVURDERINGER -> BenkV2FaneDTO.REVURDERINGER
    BenkV2Fane.MELDEKORT -> BenkV2FaneDTO.MELDEKORT
    BenkV2Fane.KLAGE -> BenkV2FaneDTO.KLAGE
    BenkV2Fane.TILBAKEKREVING -> BenkV2FaneDTO.TILBAKEKREVING
}

fun BenkV2AntallPerFane.toDTO(): Map<BenkV2FaneDTO, Int> = mapOf(
    BenkV2FaneDTO.SØKNADER to søknader,
    BenkV2FaneDTO.REVURDERINGER to revurderinger,
    BenkV2FaneDTO.MELDEKORT to meldekort,
    BenkV2FaneDTO.KLAGE to klage,
    BenkV2FaneDTO.TILBAKEKREVING to tilbakekreving,
)

fun <T : BenkV2Behandling> BenkV2Respons<T>.toDTO(fane: BenkV2Fane, saksbehandler: Saksbehandler): BenkV2ResponsDTO =
    BenkV2ResponsDTO(
        tab = fane.toDTO(),
        antallPerTab = antallPerFane.toDTO(),
        oversikt = oversikt.toDTO(saksbehandler),
    )

private fun <T : BenkV2Behandling> TilgangsfiltrertBenkV2Oversikt<T>.toDTO(saksbehandler: Saksbehandler): BenkV2OversiktDTO = BenkV2OversiktDTO(
    behandlinger = behandlinger.map { it.toDTO(saksbehandler) },
    totalAntall = totalAntall,
    totalAntallUfiltrert = totalAntallUfiltrert,
    antallFiltrertPgaTilgang = antallFiltrertPgaTilgang,
    limit = limit,
)

private fun BenkV2Behandling.toDTO(saksbehandler: Saksbehandler): BenkV2BehandlingDTO = when (this) {
    is BenkSøknadsbehandling -> BenkSøknadsbehandlingDTO(
        id = id.toString(),
        sakId = felles.sakId.toString(),
        fnr = felles.fnr.verdi,
        saksnummer = felles.saksnummer.verdi,
        startet = felles.startet.toString(),
        sistEndret = felles.sistEndret.toString(),
        saksbehandler = felles.saksbehandler,
        beslutter = felles.beslutter,
        erUnderkjent = felles.erUnderkjent,
        ventestatus = felles.ventestatus.toDTO(),
        status = status.toDTO(),
        søknadstype = søknadstype.toDTO(),
        kravtidspunkt = kravtidspunkt.toString(),
        resultat = resultat?.toDTO(),
        gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
    )

    is BenkRevurdering -> BenkRevurderingDTO(
        id = id.toString(),
        sakId = felles.sakId.toString(),
        fnr = felles.fnr.verdi,
        saksnummer = felles.saksnummer.verdi,
        startet = felles.startet.toString(),
        sistEndret = felles.sistEndret.toString(),
        saksbehandler = felles.saksbehandler,
        beslutter = felles.beslutter,
        erUnderkjent = felles.erUnderkjent,
        ventestatus = felles.ventestatus.toDTO(),
        status = status.toDTO(),
        resultat = resultat?.toDTO(),
        gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
    )

    is BenkMeldekort -> BenkMeldekortDTO(
        id = id.toString(),
        sakId = felles.sakId.toString(),
        fnr = felles.fnr.verdi,
        saksnummer = felles.saksnummer.verdi,
        startet = felles.startet.toString(),
        sistEndret = felles.sistEndret.toString(),
        saksbehandler = felles.saksbehandler,
        beslutter = felles.beslutter,
        erUnderkjent = felles.erUnderkjent,
        ventestatus = felles.ventestatus.toDTO(),
        status = status.toDTO(),
        type = type.toDTO(),
        periode = BenkV2PeriodeDTO(periode.fraOgMed.toString(), periode.tilOgMed.toString()),
        beløp = beløp,
        mottattTidspunkt = mottattTidspunkt?.toString(),
        gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
    )

    is BenkKlagebehandling -> BenkKlagebehandlingDTO(
        id = id.toString(),
        sakId = felles.sakId.toString(),
        fnr = felles.fnr.verdi,
        saksnummer = felles.saksnummer.verdi,
        startet = felles.startet.toString(),
        sistEndret = felles.sistEndret.toString(),
        saksbehandler = felles.saksbehandler,
        beslutter = felles.beslutter,
        erUnderkjent = felles.erUnderkjent,
        ventestatus = felles.ventestatus.toDTO(),
        status = status.toDTO(),
        kravtidspunkt = kravtidspunkt.toString(),
        resultat = resultat?.toDTO(),
    )

    is BenkTilbakekreving -> BenkTilbakekrevingDTO(
        id = id.toString(),
        sakId = felles.sakId.toString(),
        fnr = felles.fnr.verdi,
        saksnummer = felles.saksnummer.verdi,
        startet = felles.startet.toString(),
        sistEndret = felles.sistEndret.toString(),
        saksbehandler = felles.saksbehandler,
        beslutter = felles.beslutter,
        erUnderkjent = felles.erUnderkjent,
        ventestatus = felles.ventestatus.toDTO(),
        status = status.toDTO(),
        beløp = beløp,
        kilde = kilde.toDTO(),
        kravgrunnlagPeriode = BenkV2PeriodeDTO(
            kravgrunnlagPeriode.fraOgMed.toString(),
            kravgrunnlagPeriode.tilOgMed.toString(),
        ),
        url = url,
        gyldigeKommandoer = finnGyldigeKommandoer(saksbehandler).tilDTO(),
    )
}

private fun BenkV2Ventestatus.toDTO(): BenkV2VentestatusDTO = BenkV2VentestatusDTO(
    erSattPåVent = erSattPåVent,
    begrunnelse = begrunnelse,
    frist = frist?.toString(),
)

private fun BenkV2Behandlingsstatus.toDTO(): BenkV2BehandlingsstatusDTO = when (this) {
    BenkV2Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BenkV2BehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
    BenkV2Behandlingsstatus.KLAR_TIL_BEHANDLING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_BEHANDLING
    BenkV2Behandlingsstatus.UNDER_BEHANDLING -> BenkV2BehandlingsstatusDTO.UNDER_BEHANDLING
    BenkV2Behandlingsstatus.KLAR_TIL_BESLUTNING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_BESLUTNING
    BenkV2Behandlingsstatus.UNDER_BESLUTNING -> BenkV2BehandlingsstatusDTO.UNDER_BESLUTNING
    BenkV2Behandlingsstatus.KLAR_TIL_FERDIGSTILLING -> BenkV2BehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING
}

private fun BenkSøknadstype.toDTO(): BenkSøknadstypeDTO = when (this) {
    BenkSøknadstype.DIGITAL -> BenkSøknadstypeDTO.DIGITAL
    BenkSøknadstype.PAPIR_SKJEMA -> BenkSøknadstypeDTO.PAPIR_SKJEMA
    BenkSøknadstype.PAPIR_FRIHAND -> BenkSøknadstypeDTO.PAPIR_FRIHAND
    BenkSøknadstype.MODIA -> BenkSøknadstypeDTO.MODIA
    BenkSøknadstype.ANNET -> BenkSøknadstypeDTO.ANNET
}

private fun BenkSøknadsbehandlingResultat.toDTO(): BenkSøknadsbehandlingResultatDTO = when (this) {
    BenkSøknadsbehandlingResultat.INNVILGELSE -> BenkSøknadsbehandlingResultatDTO.INNVILGELSE
    BenkSøknadsbehandlingResultat.AVSLAG -> BenkSøknadsbehandlingResultatDTO.AVSLAG
    BenkSøknadsbehandlingResultat.IKKE_VALGT -> BenkSøknadsbehandlingResultatDTO.IKKE_VALGT
}

private fun BenkRevurderingResultat.toDTO(): BenkRevurderingResultatDTO = when (this) {
    BenkRevurderingResultat.STANS -> BenkRevurderingResultatDTO.STANS
    BenkRevurderingResultat.REVURDERING_INNVILGELSE -> BenkRevurderingResultatDTO.REVURDERING_INNVILGELSE
    BenkRevurderingResultat.OMGJØRING -> BenkRevurderingResultatDTO.OMGJØRING
    BenkRevurderingResultat.OMGJØRING_OPPHØR -> BenkRevurderingResultatDTO.OMGJØRING_OPPHØR
    BenkRevurderingResultat.OMGJØRING_IKKE_VALGT -> BenkRevurderingResultatDTO.OMGJØRING_IKKE_VALGT
}

private fun BenkKlagebehandlingResultat.toDTO(): BenkKlagebehandlingResultatDTO = when (this) {
    BenkKlagebehandlingResultat.AVVIST -> BenkKlagebehandlingResultatDTO.AVVIST
    BenkKlagebehandlingResultat.OMGJØR -> BenkKlagebehandlingResultatDTO.OMGJØR
    BenkKlagebehandlingResultat.OPPRETTHOLDT -> BenkKlagebehandlingResultatDTO.OPPRETTHOLDT
}

private fun BenkMeldekortType.toDTO(): BenkV2BehandlingstypeDTO = when (this) {
    BenkMeldekortType.MELDEKORTBEHANDLING -> BenkV2BehandlingstypeDTO.MELDEKORTBEHANDLING
    BenkMeldekortType.INNSENDT_MELDEKORT -> BenkV2BehandlingstypeDTO.INNSENDT_MELDEKORT
    BenkMeldekortType.KORRIGERT_MELDEKORT -> BenkV2BehandlingstypeDTO.KORRIGERT_MELDEKORT
}

private fun BenkTilbakekrevingStatus.toDTO(): BenkTilbakekrevingStatusDTO = when (this) {
    BenkTilbakekrevingStatus.OPPRETTET -> BenkTilbakekrevingStatusDTO.OPPRETTET
    BenkTilbakekrevingStatus.TIL_FORHÅNDSVARSEL -> BenkTilbakekrevingStatusDTO.TIL_FORHÅNDSVARSEL
    BenkTilbakekrevingStatus.UNDER_FORHÅNDSVARSLING -> BenkTilbakekrevingStatusDTO.UNDER_FORHÅNDSVARSLING
    BenkTilbakekrevingStatus.TIL_BEHANDLING -> BenkTilbakekrevingStatusDTO.TIL_BEHANDLING
    BenkTilbakekrevingStatus.UNDER_BEHANDLING -> BenkTilbakekrevingStatusDTO.UNDER_BEHANDLING
    BenkTilbakekrevingStatus.TIL_GODKJENNING -> BenkTilbakekrevingStatusDTO.TIL_GODKJENNING
    BenkTilbakekrevingStatus.UNDER_GODKJENNING -> BenkTilbakekrevingStatusDTO.UNDER_GODKJENNING
}

private fun BenkTilbakekrevingKilde.toDTO(): BenkTilbakekrevingKildeDTO = when (this) {
    BenkTilbakekrevingKilde.RAMMEVEDTAK -> BenkTilbakekrevingKildeDTO.RAMMEVEDTAK
    BenkTilbakekrevingKilde.MELDEKORT -> BenkTilbakekrevingKildeDTO.MELDEKORT
}
