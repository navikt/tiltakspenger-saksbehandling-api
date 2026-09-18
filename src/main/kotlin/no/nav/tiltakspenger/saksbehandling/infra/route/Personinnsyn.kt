package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

/**
 * Fjerner personopplysninger og saksbehandlers fritekster fra utgående DTO-er.
 * Sladdingen styres av en tillatsliste over fagroller med tjenstlig behov for personopplysninger.
 * Brukere uten en slik rolle, i dag rollen UTVIKLER, får [SladdetVerdi] i stedet for verdien.
 * Utviklere skal feilsøke gjennom API-et, som gir auditlogg, i stedet for å slå opp direkte i databasen.
 * Et sladdbart felt er alltid pakket inn i [SladdbarVerdi], slik at frontenden ser forskjell på en sladdet og en manglende verdi uten å kjenne igjen en erstatningstekst.
 * Brev i PDF-format kan ikke sladdes felt for felt og avvises med 403 for brukere uten fagrolle.
 */

val ROLLER_MED_PERSONINNSYN: Set<Saksbehandlerrolle> = setOf(
    Saksbehandlerrolle.SAKSBEHANDLER,
    Saksbehandlerrolle.BESLUTTER,
    Saksbehandlerrolle.VEILEDER,
    Saksbehandlerrolle.TILBAKEKREVING,
)

// Veileder har ikke behov for å se benken, kun direkte oppslag på spesifikke brukere
val ROLLER_SOM_KAN_SE_BENK: Set<Saksbehandlerrolle> = setOf(
    Saksbehandlerrolle.SAKSBEHANDLER,
    Saksbehandlerrolle.BESLUTTER,
    Saksbehandlerrolle.TILBAKEKREVING,
    Saksbehandlerrolle.UTVIKLER,
)

fun skalSladdeFor(saksbehandler: Saksbehandler): Boolean = !harPersoninnsyn(saksbehandler)

fun harPersoninnsyn(saksbehandler: Saksbehandler): Boolean = saksbehandler.roller.any { it in ROLLER_MED_PERSONINNSYN }

fun kanSeBenken(saksbehandler: Saksbehandler): Boolean = saksbehandler.roller.any { it in ROLLER_SOM_KAN_SE_BENK }

fun AttesteringDTO.sladdet(): AttesteringDTO = this.copy(
    begrunnelse = SladdetVerdi,
)

fun AvbruttDTO.sladdet(): AvbruttDTO = this.copy(
    begrunnelse = SladdetVerdi,
)

fun VentestatusHendelseDTO.sladdet(): VentestatusHendelseDTO = this.copy(
    begrunnelse = SladdetVerdi,
)
