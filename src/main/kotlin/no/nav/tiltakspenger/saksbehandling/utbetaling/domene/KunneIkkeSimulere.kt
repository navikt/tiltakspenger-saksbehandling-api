package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

sealed interface KunneIkkeSimulere {
    object UkjentFeil : KunneIkkeSimulere

    /** OS har åpningstider. Typisk mandag til fredag fra 6 til 21. Men det hender den er stengt på helligdager og vedlikeholdsdager også. */
    object Stengt : KunneIkkeSimulere
}
