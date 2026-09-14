package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle

/**
 * Erstatter personopplysninger og saksbehandlers fritekster i utgående DTO-er med en fast erstatningstekst.
 * Sladdingen styres av en tillatsliste over fagroller med tjenstlig behov for personopplysninger.
 * Brukere uten en slik rolle, i dag rollen UTVIKLER, får erstatningsteksten i stedet for verdien.
 * Utviklere skal feilsøke gjennom API-et, som gir auditlogg, i stedet for å slå opp direkte i databasen.
 * Samme erstatningstekst benyttes i alle felter, slik at den er lett å kjenne igjen og å teste.
 * Brev i PDF-format kan ikke sladdes felt for felt og avvises med 403 for brukere uten fagrolle.
 */

const val SLADDET_TEKST = "[Sladdet]"

val ROLLER_MED_PERSONINNSYN: Set<Saksbehandlerrolle> = setOf(
    Saksbehandlerrolle.SAKSBEHANDLER,
    Saksbehandlerrolle.BESLUTTER,
    Saksbehandlerrolle.VEILEDER,
    Saksbehandlerrolle.TILBAKEKREVING,
)

fun skalSladdeFor(saksbehandler: Saksbehandler): Boolean = saksbehandler.roller.none { it in ROLLER_MED_PERSONINNSYN }

fun AttesteringDTO.sladdet(): AttesteringDTO = this.copy(
    begrunnelse = begrunnelse?.let { SLADDET_TEKST },
)

fun AvbruttDTO.sladdet(): AvbruttDTO = this.copy(
    begrunnelse = SLADDET_TEKST,
)

fun VentestatusHendelseDTO.sladdet(): VentestatusHendelseDTO = this.copy(
    begrunnelse = SLADDET_TEKST,
)
