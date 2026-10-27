package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import java.time.LocalDate

/**
 * Lagres i `forlengelse`-kolonnen på `behandling`.
 * Innvilgelsesperiodene og barnetillegget lagres i de samme kolonnene som for andre innvilgelser.
 */
private data class ForlengelseDbJson(
    val rammevedtakId: String,
    val forlengesFraOgMed: LocalDate,
    val harEndretBarnetillegg: Boolean,
    val kanIkkeUtledeBarnetillegg: Boolean = false,
)

fun Revurderingsresultat.Forlengelse.tilForlengelseDbJson(): String = serialize(
    ForlengelseDbJson(
        rammevedtakId = forlenger.rammevedtakId.toString(),
        forlengesFraOgMed = forlenger.forlengesFraOgMed,
        harEndretBarnetillegg = harEndretBarnetillegg,
        kanIkkeUtledeBarnetillegg = kanIkkeUtledeBarnetillegg,
    ),
)

fun String.tilForlengelse(
    innvilgelsesperioder: Innvilgelsesperioder?,
    barnetillegg: Barnetillegg?,
): Revurderingsresultat.Forlengelse {
    val json = deserialize<ForlengelseDbJson>(this)
    return Revurderingsresultat.Forlengelse(
        forlenger = ForlengerRammevedtak(
            rammevedtakId = VedtakId.fromString(json.rammevedtakId),
            forlengesFraOgMed = json.forlengesFraOgMed,
        ),
        innvilgelsesperioder = innvilgelsesperioder,
        barnetillegg = barnetillegg,
        harEndretBarnetillegg = json.harEndretBarnetillegg,
        kanIkkeUtledeBarnetillegg = json.kanIkkeUtledeBarnetillegg,
    )
}
