package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Avvist
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingsresultatDbJson.KlagebehandlingsresultatDbEnum
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

private data class KlagebehandlingsresultatDbJson(
    val type: KlagebehandlingsresultatDbEnum,
    val omgjørBegrunnelse: String? = null,
    val omgjørÅrsak: KlagebehandlingsOmgjørÅrsakDbEnum? = null,
    val rammebehandlingId: String? = null,
    // TODO jah: Flytt avvisningsbrevtekst hit fra klagebehandlingstabellen
) {
    enum class KlagebehandlingsresultatDbEnum {
        AVVIST,
        OMGJØR,
        OPPRETTHOLDT,
    }

    fun toDomain(
        brevtekst: Brevtekster?,
    ): Klagebehandlingsresultat {
        return when (type) {
            KlagebehandlingsresultatDbEnum.AVVIST -> Avvist(
                brevtekst = brevtekst,
            )

            KlagebehandlingsresultatDbEnum.OMGJØR -> Omgjør(
                årsak = omgjørÅrsak!!.toDomain(),
                begrunnelse = Begrunnelse.create(omgjørBegrunnelse!!)!!,
                rammebehandlingId = rammebehandlingId?.let { BehandlingId.fromString(it) },
            )

            KlagebehandlingsresultatDbEnum.OPPRETTHOLDT -> Opprettholdt(
                årsak = omgjørÅrsak!!.toDomain(),
                begrunnelse = Begrunnelse.create(omgjørBegrunnelse!!)!!,
                brevtekst = brevtekst,
            )
        }
    }
}

fun Klagebehandlingsresultat.toDbJson(): String {
    return KlagebehandlingsresultatDbJson(
        type = when (this) {
            is Avvist -> KlagebehandlingsresultatDbEnum.AVVIST
            is Omgjør -> KlagebehandlingsresultatDbEnum.OMGJØR
            is Opprettholdt -> KlagebehandlingsresultatDbEnum.OPPRETTHOLDT
        },
        omgjørBegrunnelse = when (this) {
            is Omgjør -> this.begrunnelse.verdi
            is Opprettholdt -> this.begrunnelse.verdi
            is Avvist -> null
        },
        omgjørÅrsak = when (this) {
            is Omgjør -> this.årsak.toDbEnum()
            is Opprettholdt -> this.årsak.toDbEnum()
            is Avvist -> null
        },
        rammebehandlingId = (this as? Omgjør)?.rammebehandlingId?.toString(),
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(brevtekst: Brevtekster?): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain(brevtekst)
}
