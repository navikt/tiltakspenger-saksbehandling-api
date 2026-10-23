package no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk

import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.infra.repo.SaksstatistikkPostgresRepo
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Én rad i `statistikk_sak`, tabellen der Team Sak (DVH) leser saksbehandlingsstatistikken vår via Datastream og BigQuery.
 * Team Sak eier grensesnittet og beskriver det i https://confluence.adeo.no/display/navdvh/Teknisk+beskrivelse+av+behov+til+felles+saksbehandlingsstatistikk
 *
 * Vi skriver én ny rad per hendelse på en behandling og oppdaterer aldri radene for å formidle endringer, fordi Team Sak bare leser nye rader.
 * Team Sak kjenner igjen en hendelse på `behandlingid` + `endrettidspunkt`; resending og retting står i `docs/statistikk/resending_team_sak.md`.
 * Unntakene er [SaksstatistikkPostgresRepo.oppdaterFnr] og [SaksstatistikkPostgresRepo.oppdaterAdressebeskyttelse], som oppdaterer eksisterende rader.
 *
 * KDoc på feltene starter med kolonnenavnet; kolonnene til felt uten KDoc står i insert-setningen i [SaksstatistikkPostgresRepo].
 * `teknisktidspunkt`, når hendelsen legges på grensesnittet, har ikke noe felt her; databasen setter den ved insert.
 * Radene lages i `rammebehandling/`, `klagebehandling/` og `meldekortbehandling/` ved siden av denne fila.
 * Automatiske meldekortbehandlinger gir ingen rader; Team Sak har bare bedt om de manuelle.
 *
 * TODO jah: Grensesnittet ønsker tidspunkter uten millisekunder, men alle tidspunktkolonnene lagres med mikrosekunder.
 * Kutter vi til hele sekunder, kan to hendelser på samme behandling i samme sekund få samme nøkkel hos Team Sak, så avklar med dem før noe endres.
 */
data class SaksstatistikkDTO(
    val sakId: String,
    val saksnummer: String,
    /** `behandlingid`: id-en til rammebehandlingen, klagebehandlingen eller meldekortbehandlingen. */
    val behandlingId: String,
    /**
     * `relatertbehandlingid`: behandlingen som utløste denne.
     * Søknadsbehandling og revurdering: alltid null.
     * Klage: behandlingen det klages på, når den er valgt i formkravene.
     * Meldekort: rammebehandlingen bak det første rammevedtaket meldekortbehandlingen bygger på, avklart med Team Sak i oktober 2026.
     */
    val relatertBehandlingId: String?,
    /** `fnr`: oppdateres på eksisterende rader når bruker får nytt fødselsnummer. */
    val fnr: String,
    /**
     * `mottatt_tidspunkt`: når behandlingen oppstod, og starten på saksbehandlingstiden Team Sak beregner.
     * Må være før eller lik [registrertTidspunkt], og eldre rader må sendes på nytt hvis verdien endres.
     * Søknadsbehandling: når søknaden ble mottatt.
     * Revurdering: når revurderingen ble opprettet.
     * Klage: når journalposten for klagen ble opprettet.
     * Meldekort: første gang et meldekort fra bruker i behandlingen gikk til manuell behandling (siste automatiske forsøk).
     * Uten et slikt meldekort (for eksempel ved papirmeldekort eller korrigering saksbehandler starter selv): når behandlingen ble opprettet.
     */
    val mottattTidspunkt: LocalDateTime,
    /** `registrerttidspunkt`: når behandlingen ble opprettet hos oss. */
    val registrertTidspunkt: LocalDateTime,
    /** `ferdigbehandlettidspunkt`: når behandlingen fikk vedtak eller ble avbrutt; ellers null. */
    val ferdigBehandletTidspunkt: LocalDateTime?,
    /**
     * `vedtaktidspunkt`: når vedtaket ble fattet; null for avbrutte og åpne behandlinger.
     * TODO jah: Jeg finner ikke denne i confluence-siden til navdvh.
     * Gir det mening og ta den vekk og heller bruke [ferdigBehandletTidspunkt]
     */
    val vedtakTidspunkt: LocalDateTime?,
    /**
     * `endrettidspunkt`, også kalt funksjonell tid: når behandlingen sist ble endret.
     * Sammen med [behandlingId] er dette nøkkelen Team Sak bruker for hendelsen.
     */
    val endretTidspunkt: LocalDateTime,
    /** `utbetalttidspunkt`: tidspunkt for første utbetaling; vi fyller det ikke ut. */
    val utbetaltTidspunkt: LocalDateTime?,
    /**
     * `soknadsformat`: hvordan henvendelsen kom inn; verdiene er avtalt i Slack: https://nav-it.slack.com/archives/C066TB6TFEH/p1765541455556029?thread_ts=1765190061.367509&cid=C066TB6TFEH
     * Søknadsbehandling: søknadstypen.
     * Revurdering: alltid `DIGITAL`.
     * Klage: innsendingskilden fra formkravene.
     * Meldekort: `DIGITAL` når det finnes et meldekort fra bruker som gikk til manuell behandling, ellers `ANNET`.
     */
    val søknadsformat: StatistikkFormat,
    /** `forventetoppstarttidspunkt`: første dag i vedtaksperioden for søknadsbehandlinger; null ellers. */
    val forventetOppstartTidspunkt: LocalDate?,
    /** `sakytelse`: alltid `IND` (individstønad). */
    val sakYtelse: String = "IND",
    val behandlingType: StatistikkBehandlingType,
    val behandlingStatus: StatistikkBehandlingStatus,
    /**
     * `behandlingresultat`: null mens behandlingen er åpen.
     * Meldekort: `GODKJENT` når behandlingen er iverksatt, `AVBRUTT` når den er avbrutt.
     */
    val behandlingResultat: StatistikkBehandlingResultat?,
    /** `resultatbegrunnelse`: årsaken til omgjøringen når en klage gir medhold; ellers null. */
    val resultatBegrunnelse: String?,
    /** `behandlingmetode`: alltid `MANUELL`. */
    val behandlingMetode: String = StatistikkBehandlingMetode.MANUELL.name,
    /** `opprettetav`: alltid `system`, eller `-5` ved strengt fortrolig adresse. */
    val opprettetAv: String,
    /** `saksbehandler`: NAV-ident, eller `-5` ved strengt fortrolig adresse. */
    val saksbehandler: String?,
    /** `ansvarligbeslutter`: NAV-ident, eller `-5` ved strengt fortrolig adresse; alltid null for klage. */
    val ansvarligBeslutter: String?,
    /** `tilbakekrevingsbelop`: bare for tilbakekreving, som Team Sak får fra tilbakeløsningen; alltid null hos oss. */
    val tilbakekrevingsbeløp: Double?,
    /** `funksjonellperiode_fra_og_med`: bare for tilbakekreving; alltid null hos oss. */
    val funksjonellPeriodeFom: LocalDate?,
    /** `funksjonellperiode_til_og_med`: bare for tilbakekreving; alltid null hos oss. */
    val funksjonellPeriodeTom: LocalDate?,
    val avsender: String = "tiltakspenger-saksbehandling-api",
    /** `versjon`: git-hashen til versjonen som lagde raden. */
    val versjon: String,
    /** `hendelse`: verdien fra [StatistikkhendelseType]. */
    val hendelse: String,
    /**
     * `behandling_aarsak`: årsaken til behandlingen.
     * Søknadsbehandling: søknad, eller overføring fra Arena.
     * Revurdering: hjemmelen ved stans og opphør; null ellers.
     * Klage: alltid `KLAGE`.
     * Meldekort: `MELDEKORT` når minst én meldeperiode ikke er utbetalt før, og behandlingen dermed holder igjen en første utbetaling; ellers `KORRIGERING`.
     */
    val behandlingAarsak: StatistikkBehandlingAarsak?,
    val relatertFagsystem: String = "TPSAK",
    val sakUtland: String = "NASJONAL",
    /** `ansvarligenhet`: alltid `0387`, eller `-5` ved strengt fortrolig adresse. */
    val ansvarligenhet: String,
)

enum class StatistikkFormat {
    DIGITAL,
    PAPIR_SKJEMA,
    PAPIR_FRIHAND,
    MODIA,
    ANNET,
}

enum class StatistikkBehandlingResultat {
    INNVILGET,
    AVSLAG,
    STANS,
    FORLENGELSE,
    AVBRUTT,
    OPPHØRT,
    AVVIST,
    MEDHOLD,
    OPPRETTHOLDT,
    GODKJENT,
}

enum class StatistikkBehandlingStatus {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    UNDER_BESLUTNING,
    UNDERKJENT_BESLUTNING,

    /** Gjelder f.eks. klager som ikke er eller har vært oversendt til klageinstansen */
    FERDIG_BEHANDLET,
    OVERSENDT_KA,
    AVSLUTTET,
}

// FØRSTEGANGSBEHANDLING skal ikke brukes mer, men beholdes fordi den finnes i databasen
enum class StatistikkBehandlingType {
    FØRSTEGANGSBEHANDLING,
    SØKNADSBEHANDLING,
    REVURDERING,
    KLAGE,
    MELDEKORT,
}

enum class StatistikkBehandlingMetode {
    MANUELL,
    AUTOMATISK,
}

enum class StatistikkBehandlingAarsak {
    SOKNAD,
    DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK,
    ALDER,
    LIVSOPPHOLDYTELSER,
    KVALIFISERINGSPROGRAMMET,
    INTRODUKSJONSPROGRAMMET,
    LONN_FRA_TILTAKSARRANGOR,
    LONN_FRA_ANDRE,
    INSTITUSJONSOPPHOLD,
    IKKE_LOVLIG_OPPHOLD,
    FREMMET_FOR_SENT,

    FORLENGELSE_FRA_ARENA,
    SOKNADSBEHANDLING_FRA_ARENA,
    OVERLAPPENDE_TILTAK_I_ARENA,
    OVERFORT_FRA_ARENA,

    KLAGE,

    /** Meldekortbehandling med minst én meldeperiode som ikke er utbetalt før. */
    MELDEKORT,

    /** Meldekortbehandling der alle meldeperiodene er utbetalt før. */
    KORRIGERING,
}

enum class StatistikkResultatBegrunnelse {
    FEIL_LOVANVENDELSE,
    FEIL_REGELVERKSFORSTAAELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    PROSESSUELL_FEIL,
    ANNET,
}

enum class StatistikkhendelseType(val value: String) {
    OPPRETTET_BEHANDLING("opprettet_behandling"),
    OPPRETTET_REVURDERING("opprettet_revurdering"),
    SENDT_TIL_BESLUTTER("sendt_til_beslutter"),
    UNDERKJENT_BEHANDLING("underkjent_behandling"),
    OPPDATERT_SAKSBEHANDLER_BESLUTTER("oppdatert_saksbehandler_beslutter"),
    AVSLUTTET_BEHANDLING("avsluttet_behandling"),
    IVERKSATT_BEHANDLING("iverksatt_behandling"),
    BEHANDLING_SATT_PA_VENT("behandling_satt_på_vent"),
    BEHANDLING_GJENOPPTATT("behandling_gjenopptatt"),
    SOKNAD_BEHANDLET_PA_NYTT("søknad_behandlet_på_nytt"),
    OVERSENDT_KA("klagebehandling_oversendt_ka"),
    SAKSBEHANDLER_ANGRER("saksbehandler_angrer"),
    AUTOMATISK_TILBAKESENDT_TIL_SAKSBEHANDLER("automatisk_tilbakesendt_til_saksbehandler"),
}
