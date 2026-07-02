package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.RammebehandlingId

/**
 * Forventede feil ved avbryting av en søknadsbehandling eller revurdering (se [Rammebehandling.avbryt] og [no.nav.tiltakspenger.saksbehandling.sak.Sak.avbrytSøknadOgBehandling]).
 *
 * Typen samler forståelsen av feilen: hva som gikk galt ([beskrivelse]) og hvor alvorlig den er for oss som drifter tjenesten ([loggnivå]).
 * Den saksbehandlervendte teksten og HTTP-statusen ligger derimot i route-laget (`AvbrytSøknadOgBehandlingRoute.tilStatusOgErrorJson`), siden det er en transport-/presentasjonsdetalj.
 *
 * [beskrivelse] er bevisst fri for personopplysninger (kun interne IDer og tilstand) slik at den trygt kan logges i vanlig logg.
 */
sealed interface KunneIkkeAvbryteBehandling {

    /**
     * Kjernebeskrivelsen av hva som gikk galt.
     * Fri for personopplysninger, men kan inneholde interne IDer.
     */
    val beskrivelse: String

    /**
     * Hvor alvorlig feilen er for oss som drifter tjenesten, og dermed hvilket nivå [beskrivelse] skal logges på.
     *
     * [Loggnivå.INFO] brukes for helt forventede feil som en utvikler ikke ønsker å se som en WARN - typisk støy som følge av klientens oppførsel (f.eks. at avbryt-knappen sendes flere ganger).
     * [Loggnivå.WARN] brukes for forventede feil som kan være verdt et blikk (f.eks. at klienten opererer på en behandling vi ikke finner), men som ikke krever at vi gjør noe.
     * [Loggnivå.ERROR] brukes for feil som ikke skal kunne oppstå eller som er uventede, og som vi bør undersøke.
     */
    val loggnivå: Loggnivå

    /**
     * Behandlingen finnes ikke på saken (feil/ukjent id).
     * Saksbehandler bør laste personoversikten på nytt og prøve igjen på riktig behandling.
     * Et nytt forsøk på den samme id-en vil ikke hjelpe.
     *
     * Logges som [Loggnivå.WARN]: `behandlingId` kommer fra request-body, altså klientkontrollert input.
     * Foreløpig har vi ikke kode for å slette behandlinger, så dette skal ikke inntreffe annet enn hvis vi har kodet noe feil eller saksbehandler har endret behandlingId manuelt.
     */
    data class FantIkkeBehandling(
        val behandlingId: RammebehandlingId,
    ) : KunneIkkeAvbryteBehandling {
        override val beskrivelse: String =
            "Fant ikke behandling med id $behandlingId på saken."
        override val loggnivå: Loggnivå = Loggnivå.WARN
    }

    /**
     * Behandlingen er i en tilstand den ikke kan avbrytes fra (allerede vedtatt eller avbrutt).
     * Behandlingen er altså allerede avsluttet, så et nytt forsøk gir samme svar.
     * Dette skjer typisk når avbryt-knappen sendes flere ganger, eller når en annen saksbehandler allerede har avsluttet behandlingen.
     *
     * Logges som [Loggnivå.INFO]: en helt forventet situasjon (typisk dobbeltklikk på avbryt-knappen) som ikke krever oppfølging, og som en utvikler ikke ønsker å se som en WARN.
     */
    data class BehandlingKanIkkeAvbrytesITilstanden(
        val behandlingId: RammebehandlingId,
        val status: Rammebehandlingsstatus,
    ) : KunneIkkeAvbryteBehandling {
        override val beskrivelse: String =
            "Kan ikke avbryte behandling med id $behandlingId i tilstanden $status."
        override val loggnivå: Loggnivå = Loggnivå.INFO
    }

    /** Nivå [beskrivelse] skal logges på i vanlig logg. */
    enum class Loggnivå {
        INFO,
        WARN,
        ERROR,
    }
}
