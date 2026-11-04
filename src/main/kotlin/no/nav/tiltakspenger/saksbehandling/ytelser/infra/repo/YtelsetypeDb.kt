package no.nav.tiltakspenger.saksbehandling.ytelser.infra.repo

import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/** Våre egne lagringskoder, uavhengige av teksten kilden sender; endres ikke uten migrering. */
fun Ytelsetype.toDb(): String = when (this) {
    Ytelsetype.AAP -> "AAP"
    Ytelsetype.AFP -> "AFP"
    Ytelsetype.ALDERSPENSJON -> "ALDERSPENSJON"
    Ytelsetype.DAGPENGER -> "DAGPENGER"
    Ytelsetype.ENSLIG_FORSORGER -> "ENSLIG_FORSORGER"
    Ytelsetype.ETTERLATTEPENSJON -> "ETTERLATTEPENSJON"
    Ytelsetype.FORELDREPENGER -> "FORELDREPENGER"
    Ytelsetype.FORELDREPENREFARBGIV -> "FORELDREPENREFARBGIV"
    Ytelsetype.KOMPENSASJON -> "KOMPENSASJON"
    Ytelsetype.KRIGSPENSJON -> "KRIGSPENSJON"
    Ytelsetype.LONNSKOMPENSASJON -> "LONNSKOMPENSASJON"
    Ytelsetype.OMSORGSPENGER -> "OMSORGSPENGER"
    Ytelsetype.OMSTILLINGSSTONAD -> "OMSTILLINGSSTONAD"
    Ytelsetype.OPPLAERINGSPENGER -> "OPPLAERINGSPENGER"
    Ytelsetype.PEN_REINDRIFTSSAMER -> "PEN_REINDRIFTSSAMER"
    Ytelsetype.PLEIEPENGER -> "PLEIEPENGER"
    Ytelsetype.PLEIEPENGERREFARBGIV -> "PLEIEPENGERREFARBGIV"
    Ytelsetype.REFUSJON -> "REFUSJON"
    Ytelsetype.REFUSJON_PST -> "REFUSJON_PST"
    Ytelsetype.REFUSJON_SPK -> "REFUSJON_SPK"
    Ytelsetype.REFUSJONSKRAV -> "REFUSJONSKRAV"
    Ytelsetype.SPK -> "SPK"
    Ytelsetype.SUPPLERENDE_STONAD -> "SUPPLERENDE_STONAD"
    Ytelsetype.SVANGERSKAPSPENGER -> "SVANGERSKAPSPENGER"
    Ytelsetype.SYKEPENGER -> "SYKEPENGER"
    Ytelsetype.SYKEPENGERREFARBGIV -> "SYKEPENGERREFARBGIV"
    Ytelsetype.TJENESTEPENSJON -> "TJENESTEPENSJON"
    Ytelsetype.TILTAKSPENGER -> "TILTAKSPENGER"
    Ytelsetype.UFOREPENSJON -> "UFOREPENSJON"
    Ytelsetype.UFORETRYGD -> "UFORETRYGD"
    Ytelsetype.UNGDOMSPROGRAMYTELSE -> "UNGDOMSPROGRAMYTELSE"
}

private val ytelsetypePerDbKode: Map<String, Ytelsetype> = Ytelsetype.entries.associateBy { it.toDb() }

fun String.tilYtelsetypeFraDb(): Ytelsetype = ytelsetypePerDbKode.getValue(this)
