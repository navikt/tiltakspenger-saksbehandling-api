package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response

import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Teksten sokos-utbetaldata sender for typene vi henter; tjenesten sender beskrivelsen av ytelsen, ikke en kode.
 * «Statens pensjonkasse» er en tidligere stavemåte.
 */
private val ytelsetypePerTekst: Map<String, Ytelsetype> = mapOf(
    "Arbeidsavklaringspenger" to Ytelsetype.AAP,
    "Avtalefestet pensjon" to Ytelsetype.AFP,
    "Alderspensjon" to Ytelsetype.ALDERSPENSJON,
    "Dagpenger" to Ytelsetype.DAGPENGER,
    "Enslig forsørger" to Ytelsetype.ENSLIG_FORSORGER,
    "Etterlattepensjon" to Ytelsetype.ETTERLATTEPENSJON,
    "Foreldrepenger" to Ytelsetype.FORELDREPENGER,
    "Foreldrepenger refusjon arbeidsgiver" to Ytelsetype.FORELDREPENREFARBGIV,
    "Kompensasjon inntektstap" to Ytelsetype.KOMPENSASJON,
    "Krigspensjon" to Ytelsetype.KRIGSPENSJON,
    "Lønnskompensasjon" to Ytelsetype.LONNSKOMPENSASJON,
    "Omsorgspenger" to Ytelsetype.OMSORGSPENGER,
    "Omstillingsstønad" to Ytelsetype.OMSTILLINGSSTONAD,
    "Opplæringspenger" to Ytelsetype.OPPLAERINGSPENGER,
    "Pensjon reindriftssamer" to Ytelsetype.PEN_REINDRIFTSSAMER,
    "Pleiepenger" to Ytelsetype.PLEIEPENGER,
    "Pleiepenger refusjon arbeidgiver" to Ytelsetype.PLEIEPENGERREFARBGIV,
    "Refusjon" to Ytelsetype.REFUSJON,
    "Refusjon PST" to Ytelsetype.REFUSJON_PST,
    "Refusjon SPK" to Ytelsetype.REFUSJON_SPK,
    "Refusjonskrav" to Ytelsetype.REFUSJONSKRAV,
    "Statens pensjonskasse" to Ytelsetype.SPK,
    "Statens pensjonkasse" to Ytelsetype.SPK,
    "Supplerende_stønad" to Ytelsetype.SUPPLERENDE_STONAD,
    "Svangerskapspenger" to Ytelsetype.SVANGERSKAPSPENGER,
    "Sykepenger" to Ytelsetype.SYKEPENGER,
    "Sykepenger refusjon arbeidsgiver" to Ytelsetype.SYKEPENGERREFARBGIV,
    "Tjenestepensjon" to Ytelsetype.TJENESTEPENSJON,
    "Tiltakspenger" to Ytelsetype.TILTAKSPENGER,
    "Uførepensjon" to Ytelsetype.UFOREPENSJON,
    "Uføretrygd" to Ytelsetype.UFORETRYGD,
    "Ungdomsprogram ytelse" to Ytelsetype.UNGDOMSPROGRAMYTELSE,
)

/** Tekstene vi henter, inkludert gamle stavemåter. */
val henteteYtelsestyper: Set<String> = ytelsetypePerTekst.keys

/** Null når teksten ikke er en type vi henter. */
fun String.tilYtelsetype(): Ytelsetype? = ytelsetypePerTekst[this]
