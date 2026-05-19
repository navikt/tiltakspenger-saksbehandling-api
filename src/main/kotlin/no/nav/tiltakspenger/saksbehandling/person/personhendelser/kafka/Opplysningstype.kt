package no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka

/**
 * Subset av PDL sine opplysningstyper på pdl.leesah-v1 som vi faktisk håndterer.
 * Andre typer (f.eks. NAVN_V1, FOLKEREGISTERIDENTIFIKATOR_V1, FORELDERBARNRELASJON_V1, ...)
 * filtreres bort uten kall til databasen.
 */
enum class Opplysningstype {
    DOEDSFALL_V1,
    ADRESSEBESKYTTELSE_V1,
}
