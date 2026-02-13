@file:Suppress("PackageDirectoryMismatch")
// Må ligge i samme pakke som Hjemmel, men trenger ikke ligge i samme mappe.

package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface Klagehjemmel : Hjemmel {
    sealed interface KlageArbeidsmarkedsloven :
        Klagehjemmel,
        Hjemmel.ArbeidsmarkedslovenHjemmel
    sealed interface KlageTiltakspengeforskriften :
        Klagehjemmel,
        Hjemmel.TiltakspengeforskriftenHjemmel
    sealed interface KlageForvaltningsloven :
        Klagehjemmel,
        Hjemmel.ForvaltningslovenHjemmel
    sealed interface KlageFolketrygdloven :
        Klagehjemmel,
        Hjemmel.FolketrygdlovenHjemmel
    sealed interface KlageForeldelsesloven :
        Klagehjemmel,
        Hjemmel.ForeldelseslovenHjemmel
}
