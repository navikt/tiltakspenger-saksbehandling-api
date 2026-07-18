package no.nav.tiltakspenger.saksbehandling.person.personhendelser

/**
 * Forventede grunner til at en personhendelse fra `pdl.leesah-v1` ikke ble lagret/behandlet.
 * Dette er ikke "feil" i klassisk forstand — de aller fleste records vi konsumerer havner her
 * (vi mottar hendelser for hele Norges befolkning men bryr oss bare om et lite subset).
 *
 * Uventede ting (DB-feil, PDL-feil, ...) kastes som exception og er ikke representert her.
 */
sealed interface KunneIkkeBehandlePersonhendelse {
    /** Opplysningstypen er ikke en av de vi støtter (DOEDSFALL_V1 / ADRESSEBESKYTTELSE_V1). */
    data object OpplysningstypeIkkeStøttet : KunneIkkeBehandlePersonhendelse

    /** Hendelsen mangler både doedsfall- og adressebeskyttelse-payload. */
    data object PayloadMangler : KunneIkkeBehandlePersonhendelse

    /**
     * Adressebeskyttelse-hendelse med en gradering vi ikke håndterer.
     * Vi bryr oss kun om kode 6 (STRENGT_FORTROLIG og STRENGT_FORTROLIG_UTLAND) — det er den eneste graderingen som påvirker hvordan vi rapporterer saken til statistikk og som krever skjermings-/oppgavebehov.
     * FORTROLIG (kode 7) og UGRADERT trigger ingen handling hos oss og forkastes her.
     */
    data object AdressebeskyttelseErIkkeKode6 : KunneIkkeBehandlePersonhendelse

    /** Ingen av personidentene i hendelsen matcher en sak hos oss. */
    data object IngenSakForPersonidenter : KunneIkkeBehandlePersonhendelse

    /** Vi har allerede lagret en hendelse av samme opplysningstype for denne saken. */
    data object HendelseAlleredeLagret : KunneIkkeBehandlePersonhendelse

    /** Adressebeskyttelse-hendelse, men oppslag i PDL viser at personen ikke har kode 6. */
    data object IkkeKode6IPdl : KunneIkkeBehandlePersonhendelse

    data object PersonidenterMangler : KunneIkkeBehandlePersonhendelse
}
