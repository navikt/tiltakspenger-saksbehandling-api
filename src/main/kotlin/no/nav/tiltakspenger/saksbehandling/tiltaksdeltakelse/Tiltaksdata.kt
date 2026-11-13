package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

/**
 * Opplysninger om et tiltak i saksopplysningene som vi ikke lagrer, men slår opp hos kilden ved behov.
 *
 * @param internDeltakelseId null for deltakelser som fratar rett, siden vi ikke gir dem en intern id.
 * @param tittel hele tiltaksnavnet, typisk på formen "Tiltakstype hos Arrangør".
 * Null ved adressebeskyttelse, når kilden ikke har noen tittel, eller når deltakelsen ikke lenger finnes hos kilden.
 */
data class Tiltaksdata(
    val eksternDeltakelseId: String,
    val internDeltakelseId: TiltaksdeltakerId?,
    val tittel: String?,
)
