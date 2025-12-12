package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingsresultatDbJson.KlagebehandlingsresultatDbEnum

private data class KlagebehandlingsresultatDbJson(
    val type: String,
) {
    enum class KlagebehandlingsresultatDbEnum {
        AVVIST,
    }

    fun toDomain(): Klagebehandlingsresultat {
        return when (KlagebehandlingsresultatDbEnum.valueOf(type)) {
            KlagebehandlingsresultatDbEnum.AVVIST -> Klagebehandlingsresultat.AVVIST
        }
    }
}

fun Klagebehandlingsresultat.toDbJson(): String {
    return KlagebehandlingsresultatDbJson(
        type = when (this) {
            Klagebehandlingsresultat.AVVIST -> KlagebehandlingsresultatDbEnum.AVVIST.name
        },
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain()
}
