package no.nav.tiltakspenger.saksbehandling.ytelser.infra.route

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Frontenden viser [ytelsetype] som den er, så teksten er skrevet for saksbehandlere.
 * [kildetekst] er teksten fra sokos-utbetaldata, som saksbehandlere kan sammenligne med andre flater som viser utbetalinger.
 * TODO: Send koden og la frontenden eie visningsteksten.
 */
data class YtelseDTO(
    val ytelsetype: String,
    val kildetekst: String?,
    val perioder: List<PeriodeDTO>,
)

fun Ytelse.toDTO(): YtelseDTO = YtelseDTO(
    ytelsetype = when (this) {
        is Ytelse.Kjent -> ytelsetype.toDTO()
        is Ytelse.Ukjent -> "Ukjent ytelse"
    },
    kildetekst = kildetekst,
    perioder = perioder.map { it.toDTO() },
)

private fun Ytelsetype.toDTO(): String = when (this) {
    Ytelsetype.AAP -> "Arbeidsavklaringspenger"
    Ytelsetype.AFP -> "Avtalefestet pensjon"
    Ytelsetype.ALDERSPENSJON -> "Alderspensjon"
    Ytelsetype.DAGPENGER -> "Dagpenger"
    Ytelsetype.ENSLIG_FORSORGER -> "Stønad til enslig mor eller far"
    Ytelsetype.ETTERLATTEPENSJON -> "Gjenlevendepensjon"
    Ytelsetype.FORELDREPENGER -> "Foreldrepenger"
    Ytelsetype.FORELDREPENREFARBGIV -> "Foreldrepenger refundert til arbeidsgiver"
    Ytelsetype.KOMPENSASJON -> "Kompensasjon for tapt inntekt"
    Ytelsetype.KRIGSPENSJON -> "Krigspensjon"
    Ytelsetype.LONNSKOMPENSASJON -> "Lønnskompensasjon for permitterte"
    Ytelsetype.OMSORGSPENGER -> "Omsorgspenger"
    Ytelsetype.OMSTILLINGSSTONAD -> "Omstillingsstønad"
    Ytelsetype.OPPLAERINGSPENGER -> "Opplæringspenger"
    Ytelsetype.PEN_REINDRIFTSSAMER -> "Pensjon for reindriftsutøvere"
    Ytelsetype.PLEIEPENGER -> "Pleiepenger"
    Ytelsetype.PLEIEPENGERREFARBGIV -> "Pleiepenger refundert til arbeidsgiver"
    Ytelsetype.REFUSJON -> "Refusjon"
    Ytelsetype.REFUSJON_PST -> "Refusjon PST"
    Ytelsetype.REFUSJON_SPK -> "Refusjon fra Statens pensjonskasse"
    Ytelsetype.REFUSJONSKRAV -> "Refusjonskrav"
    Ytelsetype.SPK -> "Pensjon fra Statens pensjonskasse"
    Ytelsetype.SUPPLERENDE_STONAD -> "Supplerende stønad"
    Ytelsetype.SVANGERSKAPSPENGER -> "Svangerskapspenger"
    Ytelsetype.SYKEPENGER -> "Sykepenger"
    Ytelsetype.SYKEPENGERREFARBGIV -> "Sykepenger refundert til arbeidsgiver"
    Ytelsetype.TJENESTEPENSJON -> "Tjenestepensjon"
    Ytelsetype.TILTAKSPENGER -> "Tiltakspenger"
    Ytelsetype.UFOREPENSJON -> "Uførepensjon"
    Ytelsetype.UFORETRYGD -> "Uføretrygd"
    Ytelsetype.UNGDOMSPROGRAMYTELSE -> "Ungdomsprogramytelse"
}
