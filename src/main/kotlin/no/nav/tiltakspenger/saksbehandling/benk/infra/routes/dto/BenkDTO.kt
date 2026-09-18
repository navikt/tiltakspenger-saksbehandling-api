package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkAntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlagebehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekort
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOppsummering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversiktMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkPersonmarkører
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRad
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkResponsMedTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkResponsUtenTilgang
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurdering
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekreving
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkVentestatus
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdet

/**
 * Wiretypen for benk v2, splittet på om den innloggede brukeren har rolle for å se benken.
 * Uten rollen er svaret bare [harTilgang] = false; tellingene og radene utelates, fordi brukeren ikke har tjenstlig behov for dem.
 */
sealed interface BenkResponsDTO {
    val harTilgang: Boolean
}

/**
 * Feltnavnene er kontrakten mot frontendens `lib/benk/v2/typer`, og fellesfeltene ligger flatt på hver rad — ikke under et `felles`-objekt — fordi det er slik frontenden leser dem.
 */
data class BenkResponsMedTilgangDTO(
    val tab: BenkFaneDTO,
    val antallPerTab: Map<BenkFaneDTO, Int>,
    val oversikt: BenkOversiktDTO,
    /**
     * Satt når requesten ikke lot seg tolke og benken derfor svarer med en standardvisning.
     * Frontenden viser meldingen, slik at saksbehandler ser at filtrene ikke slo til.
     */
    val error: String? = null,
) : BenkResponsDTO {
    override val harTilgang: Boolean = true
}

/**
 * Svaret til en bruker uten benkrolle.
 * Frontenden skjuler benken og trenger ikke resten av payloaden.
 * */
data object BenkResponsUtenTilgangDTO : BenkResponsDTO {
    override val harTilgang: Boolean = false
}

data class BenkOversiktDTO(
    val behandlinger: List<BenkBehandlingDTO>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val oppsummering: BenkOppsummeringDTO,
    /**
     * Siden som ble spurt om, 0-basert.
     * Antall sider er `totalAntall` delt på [sideantall], rundet opp.
     */
    val side: Int,
    /** Fast sidestørrelse — kaller kan ikke be om en annen. */
    val sideantall: Int,
    /** Identene tildelt en rad i fanen, ufiltrert — valgene i benkens nedtrekksliste for saksbehandler/beslutter. */
    val saksbehandlere: List<String>,
    val besluttere: List<String>,
)

enum class BenkFaneDTO {
    SØKNADER,
    REVURDERINGER,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,
}

enum class BenkBehandlingsstatusDTO {
    UNDER_AUTOMATISK_BEHANDLING,
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    KLAR_TIL_FERDIGSTILLING,
}

enum class BenkBehandlingstypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
    KLAGEBEHANDLING,
    TILBAKEKREVING,
}

enum class BenkTilgangsvurderingDTO {
    HAR_TILGANG,
    HAR_IKKE_TILGANG,
}

/**
 * Grunnene benken kan oppgi for en rad uten tilgang.
 * Navnene er benkens egen kontrakt mot frontenden, ikke Tilgangsmaskinens koder.
 */
enum class BenkTilgangsårsakDTO {
    STRENGT_FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG_ADRESSE,
    SKJERMET,
    HABILITET,
    VERGEMÅL,
    GEOGRAFISK,
    UKJENT_BOSTED,
    PERSON_UTLAND,
    AVDØD,

    /** Tilgangsmaskinen avviste med en kode backend ikke kjenner; begrunnelsen sier fortsatt hva som skjedde. */
    UKJENT,

    IKKE_SAKSBEHANDLER_ELLER_BESLUTTER,
}

data class BenkTilgangsgrunnDTO(
    val årsak: BenkTilgangsårsakDTO,
    val begrunnelse: String,
)

data class BenkTilgangDTO(
    val vurdering: BenkTilgangsvurderingDTO,
    val grunn: BenkTilgangsgrunnDTO?,
)

/** Markørene gjelder personen uavhengig av om saksbehandleren har tilgang til raden. */
data class BenkPersonmarkørerDTO(
    val skjermet: Boolean,
    val kode6: Boolean,
    val kode7: Boolean,
)

data class BenkOppsummeringDTO(
    val antallMedTilgang: Int,
    val antallUtenTilgang: Int,
    val antallSkjermet: Int,
    val antallKode6: Int,
    val antallKode7: Int,
)

data class BenkVentestatusDTO(
    val erSattPåVent: Boolean,
    val begrunnelse: SladdbarVerdi<String?>,
    val frist: String?,
)

sealed interface BenkBehandlingDTO {
    val type: BenkBehandlingstypeDTO
    val id: String
    val sakId: SladdbarVerdi<String>
    val fnr: SladdbarVerdi<String>
    val saksnummer: SladdbarVerdi<String>
    val startet: String
    val sistEndret: String
    val saksbehandler: String?
    val beslutter: String?
    val erUnderkjent: Boolean
    val ventestatus: BenkVentestatusDTO
    val tilgang: BenkTilgangDTO
    val personmarkører: BenkPersonmarkørerDTO
}

fun BenkFane.toDTO(): BenkFaneDTO = when (this) {
    BenkFane.SØKNADER -> BenkFaneDTO.SØKNADER
    BenkFane.REVURDERINGER -> BenkFaneDTO.REVURDERINGER
    BenkFane.MELDEKORT -> BenkFaneDTO.MELDEKORT
    BenkFane.KLAGE -> BenkFaneDTO.KLAGE
    BenkFane.TILBAKEKREVING -> BenkFaneDTO.TILBAKEKREVING
}

fun BenkAntallPerFane.toDTO(): Map<BenkFaneDTO, Int> = mapOf(
    BenkFaneDTO.SØKNADER to søknader,
    BenkFaneDTO.REVURDERINGER to revurderinger,
    BenkFaneDTO.MELDEKORT to meldekort,
    BenkFaneDTO.KLAGE to klage,
    BenkFaneDTO.TILBAKEKREVING to tilbakekreving,
)

fun <T : BenkBehandling> BenkResponsMedTilgang<T>.toDTO(
    fane: BenkFane,
    saksbehandler: Saksbehandler,
    error: String? = null,
): BenkResponsMedTilgangDTO =
    BenkResponsMedTilgangDTO(
        tab = fane.toDTO(),
        antallPerTab = antallPerFane.toDTO(),
        oversikt = oversikt.toDTO(saksbehandler),
        error = error,
    ).sladdetFor(saksbehandler)

fun BenkResponsUtenTilgang.toDTO(): BenkResponsUtenTilgangDTO = BenkResponsUtenTilgangDTO

private fun <T : BenkBehandling> BenkOversiktMedTilgang<T>.toDTO(saksbehandler: Saksbehandler): BenkOversiktDTO = BenkOversiktDTO(
    behandlinger = rader.map { it.toDTO(saksbehandler) },
    totalAntall = totalAntall,
    totalAntallUfiltrert = totalAntallUfiltrert,
    oppsummering = oppsummering.toDTO(),
    side = side,
    sideantall = sideantall,
    saksbehandlere = saksbehandlere,
    besluttere = besluttere,
)

private fun <T : BenkBehandling> BenkRad<T>.toDTO(saksbehandler: Saksbehandler): BenkBehandlingDTO {
    val tilgangDTO = tilgang.toDTO(saksbehandler)
    val personmarkørerDTO = personmarkører.toDTO()
    val behandlingDTO = when (val behandling = behandling) {
        is BenkSøknadsbehandling -> behandling.toDTO(saksbehandler, tilgangDTO, personmarkørerDTO)
        is BenkRevurdering -> behandling.toDTO(saksbehandler, tilgangDTO, personmarkørerDTO)
        is BenkMeldekort -> behandling.toDTO(saksbehandler, tilgangDTO, personmarkørerDTO)
        is BenkKlagebehandling -> behandling.toDTO(tilgangDTO, personmarkørerDTO)
        is BenkTilbakekreving -> behandling.toDTO(saksbehandler, tilgangDTO, personmarkørerDTO)
    }
    return when (tilgang) {
        TilgangsvurderingBulk.Godkjent -> behandlingDTO
        is TilgangsvurderingBulk.Avvist -> behandlingDTO.sladdet()
    }
}

private fun TilgangsvurderingBulk.toDTO(saksbehandler: Saksbehandler): BenkTilgangDTO {
    if (!saksbehandler.erSaksbehandlerEllerBeslutter) {
        return BenkTilgangDTO(
            vurdering = BenkTilgangsvurderingDTO.HAR_IKKE_TILGANG,
            grunn = BenkTilgangsgrunnDTO(
                årsak = BenkTilgangsårsakDTO.IKKE_SAKSBEHANDLER_ELLER_BESLUTTER,
                begrunnelse = "Har ikke rolle for å se behandlinger fra benken",
            ),
        )
    }

    return when (this) {
        TilgangsvurderingBulk.Godkjent -> BenkTilgangDTO(
            vurdering = BenkTilgangsvurderingDTO.HAR_TILGANG,
            grunn = null,
        )

        is TilgangsvurderingBulk.Avvist -> BenkTilgangDTO(
            vurdering = BenkTilgangsvurderingDTO.HAR_IKKE_TILGANG,
            grunn = BenkTilgangsgrunnDTO(
                årsak = årsak.toDTO(),
                begrunnelse = begrunnelse,
            ),
        )
    }
}

private fun TilgangsvurderingAvvistÅrsak.toDTO(): BenkTilgangsårsakDTO = when (this) {
    TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG -> BenkTilgangsårsakDTO.STRENGT_FORTROLIG_ADRESSE
    TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND -> BenkTilgangsårsakDTO.STRENGT_FORTROLIG_UTLAND
    TilgangsvurderingAvvistÅrsak.FORTROLIG -> BenkTilgangsårsakDTO.FORTROLIG_ADRESSE
    TilgangsvurderingAvvistÅrsak.SKJERMET -> BenkTilgangsårsakDTO.SKJERMET
    TilgangsvurderingAvvistÅrsak.HABILITET -> BenkTilgangsårsakDTO.HABILITET
    TilgangsvurderingAvvistÅrsak.VERGE -> BenkTilgangsårsakDTO.VERGEMÅL
    TilgangsvurderingAvvistÅrsak.GEOGRAFISK -> BenkTilgangsårsakDTO.GEOGRAFISK
    TilgangsvurderingAvvistÅrsak.UKJENT_BOSTED -> BenkTilgangsårsakDTO.UKJENT_BOSTED
    TilgangsvurderingAvvistÅrsak.PERSON_UTLAND -> BenkTilgangsårsakDTO.PERSON_UTLAND
    TilgangsvurderingAvvistÅrsak.AVDØD -> BenkTilgangsårsakDTO.AVDØD
    TilgangsvurderingAvvistÅrsak.UKJENT -> BenkTilgangsårsakDTO.UKJENT
}

private fun BenkPersonmarkører.toDTO(): BenkPersonmarkørerDTO = BenkPersonmarkørerDTO(
    skjermet = skjermet,
    kode6 = kode6,
    kode7 = kode7,
)

private fun BenkOppsummering.toDTO(): BenkOppsummeringDTO = BenkOppsummeringDTO(
    antallMedTilgang = antallMedTilgang,
    antallUtenTilgang = antallUtenTilgang,
    antallSkjermet = antallSkjermet,
    antallKode6 = antallKode6,
    antallKode7 = antallKode7,
)

fun BenkVentestatus.toDTO(): BenkVentestatusDTO = BenkVentestatusDTO(
    erSattPåVent = erSattPåVent,
    begrunnelse = begrunnelse.ikkeSladdet(),
    frist = frist?.toString(),
)

fun BenkBehandlingsstatus.toDTO(): BenkBehandlingsstatusDTO = when (this) {
    BenkBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING -> BenkBehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING
    BenkBehandlingsstatus.KLAR_TIL_BEHANDLING -> BenkBehandlingsstatusDTO.KLAR_TIL_BEHANDLING
    BenkBehandlingsstatus.UNDER_BEHANDLING -> BenkBehandlingsstatusDTO.UNDER_BEHANDLING
    BenkBehandlingsstatus.KLAR_TIL_BESLUTNING -> BenkBehandlingsstatusDTO.KLAR_TIL_BESLUTNING
    BenkBehandlingsstatus.UNDER_BESLUTNING -> BenkBehandlingsstatusDTO.UNDER_BESLUTNING
    BenkBehandlingsstatus.KLAR_TIL_FERDIGSTILLING -> BenkBehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING
}

fun BenkBehandlingsstatusDTO.tilDomene(): BenkBehandlingsstatus = when (this) {
    BenkBehandlingsstatusDTO.UNDER_AUTOMATISK_BEHANDLING -> BenkBehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
    BenkBehandlingsstatusDTO.KLAR_TIL_BEHANDLING -> BenkBehandlingsstatus.KLAR_TIL_BEHANDLING
    BenkBehandlingsstatusDTO.UNDER_BEHANDLING -> BenkBehandlingsstatus.UNDER_BEHANDLING
    BenkBehandlingsstatusDTO.KLAR_TIL_BESLUTNING -> BenkBehandlingsstatus.KLAR_TIL_BESLUTNING
    BenkBehandlingsstatusDTO.UNDER_BESLUTNING -> BenkBehandlingsstatus.UNDER_BESLUTNING
    BenkBehandlingsstatusDTO.KLAR_TIL_FERDIGSTILLING -> BenkBehandlingsstatus.KLAR_TIL_FERDIGSTILLING
}
