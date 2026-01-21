package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingsresultatDbJson.KlagebehandlingsresultatDbEnum
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

private data class KlagebehandlingsresultatDbJson(
    val type: KlagebehandlingsresultatDbEnum,
    val omgjørBegrunnelse: String? = null,
    val omgjørÅrsak: KlagebehandlingsOmgjørÅrsakDbEnum? = null,
    // TODO jah: Flytt avvisningsbrevtekst hit fra klagebehandlingstabellen
) {
    enum class KlagebehandlingsresultatDbEnum {
        AVVIST,
        OMGJØR,
    }

    fun toDomain(
        brevtekst: Brevtekster?,
    ): Klagebehandlingsresultat {
        return when (type) {
            KlagebehandlingsresultatDbEnum.AVVIST -> Klagebehandlingsresultat.Avvist(
                brevtekst = brevtekst,
            )

            KlagebehandlingsresultatDbEnum.OMGJØR -> Klagebehandlingsresultat.Omgjør(
                årsak = omgjørÅrsak!!.toDomain(),
                begrunnelse = Begrunnelse.create(omgjørBegrunnelse!!)!!,
            )
        }
    }
}

fun Klagebehandlingsresultat.toDbJson(): String {
    return KlagebehandlingsresultatDbJson(
        type = when (this) {
            is Klagebehandlingsresultat.Avvist -> KlagebehandlingsresultatDbEnum.AVVIST
            is Klagebehandlingsresultat.Omgjør -> KlagebehandlingsresultatDbEnum.OMGJØR
        },
        omgjørBegrunnelse = (this as? Klagebehandlingsresultat.Omgjør)?.begrunnelse?.verdi,
        omgjørÅrsak = (this as? Klagebehandlingsresultat.Omgjør)?.årsak?.toDbEnum(),
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(brevtekst: Brevtekster?): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain(brevtekst)
}
