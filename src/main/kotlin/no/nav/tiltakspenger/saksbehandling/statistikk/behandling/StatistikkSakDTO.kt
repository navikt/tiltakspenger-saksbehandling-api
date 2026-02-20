package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import java.time.LocalDate
import java.time.LocalDateTime

// Grensenittet for denne tabellen eies av DVH og er definert her:
// https://confluence.adeo.no/display/navdvh/Teknisk+beskrivelse+av+behov+til+felles+saksbehandlingsstatistikk
data class StatistikkSakDTO(
    val sakId: String,
    val saksnummer: String,
    val behandlingId: String,
    /** Hvis behandlingen har oppstått med bakgrunn i en annen, skal den foregående behandlingen refereres til her. Når det gjelder klage skal denne vise til påklaget behandling. */
    val relatertBehandlingId: String?,
    val fnr: String,
    // TODO jah: Her skriver de at de ikke ønsker millisekunder. Men vi lagrer den med millisekunder. Bør vi gjøre en avsjekk med team statistikk sak? Bør gå over alle stedet vi bruker tidspunkt/LocalDateTime.
    /** Tidspunktet da behandlingen oppstår (eks. søknad mottas). Dette er starten på beregning av saksbehandlingstid. Denne verdien må være før eller samtidig som registrertTidspunkt. Dette feltet må være utfylt bør behandlingen avsluttes. Tidligere meldinger må re-sendes ved oppdatering av dette feltet. */
    val mottattTidspunkt: LocalDateTime,
    /** Tidspunkt da behandlingen første gang ble registrert i fagsystemet. Ved digitale søknader bør denne være tilnærmet lik mottatt tid. */
    val registrertTidspunkt: LocalDateTime,
    /** Tidspunkt når behandlinge ble avsluttet, enten avbrutt, henlagt, vedtak innvilget/avslått osv. */
    val ferdigBehandletTidspunkt: LocalDateTime?,
    /** TODO jah: Jeg finner ikke denne i confluence-siden til navdvh. Gir det mening og ta den vekk og heller bruke [ferdigBehandletTidspunkt] */
    val vedtakTidspunkt: LocalDateTime?,
    /** Også kalt funksjonell tid. Tidspunkt for siste endring på behandlingen. Ved første melding vil denne være lik registrert tid. */
    val endretTidspunkt: LocalDateTime,
    /** Tidspunkt for første utbetaling av ytelse. */
    val utbetaltTidspunkt: LocalDateTime?,
    /**
     * Formatet er ikke strukturert på noe vis per nå, bruker derfor verdiene som ble diskutert frem på slack
     * https://nav-it.slack.com/archives/C066TB6TFEH/p1765541455556029?thread_ts=1765190061.367509&cid=C066TB6TFEH
     */
    val søknadsformat: StatistikkFormat,

    /** Hvis systemet eller bruker har et forhold til når ytelsen normalt skal utbetales (planlagt uttak, ønsket oppstart etc.) */
    val forventetOppstartTidspunkt: LocalDate?,
    /** Tidspunktet da fagsystemet legger hendelsen på grensesnittet/topicen. */
    val tekniskTidspunkt: LocalDateTime,
    // IND
    val sakYtelse: String = "IND",
    val behandlingType: StatistikkBehandlingType,
    val behandlingStatus: StatistikkBehandlingStatus,
    val behandlingResultat: StatistikkBehandlingResultat?,
    // fylles ut ved klage, avvisning, avslag
    val resultatBegrunnelse: String?,
    // manuell, automatisk
    val behandlingMetode: String = StatistikkBehandlingMetode.MANUELL.name,
    // Settes til -5 hvis kode 6 kan være systembruker
    val opprettetAv: String,
    // Settes til -5 hvis kode 6
    val saksbehandler: String?,
    // Settes til -5 hvis kode 6
    val ansvarligBeslutter: String?,
    /** Kun for tilbakekreving: beløp som skal tilbakekreves */
    val tilbakekrevingsbeløp: Double?,
    /** Kun for tilbakekreving: funksjonell periode for tilbakekreving */
    val funksjonellPeriodeFom: LocalDate?,
    /** Kun for tilbakekreving: funksjonell periode for tilbakekreving */
    val funksjonellPeriodeTom: LocalDate?,
    val avsender: String = "tiltakspenger-saksbehandling-api",
    // commit hash
    val versjon: String,
    val hendelse: String,
    /** Årsaken til behandlingen */
    val behandlingAarsak: StatistikkBehandlingAarsak?,
    val relatertFagsystem: String = "TPSAK",
    val sakUtland: String = "NASJONAL",
    val ansvarligenhet: String,
)

// PAPIR skal ikke brukes mer, men beholdes fordi den finnes i databasen
enum class StatistikkFormat {
    PAPIR,
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
}

enum class StatistikkBehandlingStatus {
    UNDER_BEHANDLING,
    UNDER_BESLUTNING,
    FERDIG_BEHANDLET,
    AVSLUTTET,
}

// FØRSTEGANGSBEHANDLING skal ikke brukes mer, men beholdes fordi den finnes i databasen
enum class StatistikkBehandlingType {
    FØRSTEGANGSBEHANDLING,
    SØKNADSBEHANDLING,
    REVURDERING,
    KLAGE,
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
}
