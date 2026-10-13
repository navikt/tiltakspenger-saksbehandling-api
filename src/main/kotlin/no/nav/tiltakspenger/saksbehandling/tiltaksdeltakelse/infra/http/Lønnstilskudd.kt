package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelser

// TODO: Klassifiseringen av lønnstilskudd hører hjemme i tiltakspenger-libs, ved siden av kodetabellene som klassifiserer tiltakstypene.
//  Den ligger her inntil libs har en slik klassifisering, og skal fjernes herfra når den er på plass.
private val lønnstilskuddkoder: Set<String> = setOf(
    // Arena
    "FLEKSJOBB",
    "LONNTIL",
    "LONNTILAAP",
    "LONNTILL",
    "LONNTILS",
    "MIDLONTIL",
    "REAKTUFOR",
    "TIDSUBLONN",
    "UFØREPENLØ",
    "VARLONTIL",
    // Arena og Team Tiltak
    "FIREARIG_LONNSTILSKUDD",
    // Team Tiltak
    "MIDLERTIDIG_LONNSTILSKUDD",
    "VARIG_LONNSTILSKUDD",
)

/**
 * Lønnstilskudd (alle typer) gir ikke rett til tiltakspenger, men skal vises til saksbehandler.
 * Klassifiseres på kildens tiltakskode, siden libs ikke skiller lønnstilskudd fra andre tiltakstyper som ikke gir rett.
 */
val Tiltaksdeltakelse.GirIkkeRett.erLønnstilskudd: Boolean get() = tiltakskodeFraKilden in lønnstilskuddkoder

val Tiltaksdeltakelser.lønnstilskudd: List<Tiltaksdeltakelse.GirIkkeRett>
    get() = deltakelser.filterIsInstance<Tiltaksdeltakelse.GirIkkeRett>().filter { it.erLønnstilskudd }
