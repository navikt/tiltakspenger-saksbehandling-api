package no.nav.tiltakspenger.vedtak.repository.behandling.kvp

import no.nav.tiltakspenger.saksbehandling.domene.behandling.kravdato.KravdatoSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.Saksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkårssett
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vurdering
import no.nav.tiltakspenger.vedtak.db.deserialize
import no.nav.tiltakspenger.vedtak.db.serialize

/**
 * Har ansvar for å serialisere/deserialisere Vilkårssett til og fra json for lagring i database.
 */
private class VilkårssettJson(
    val kvpVilkår: KVPVilkårDbJson,
)

internal fun String.toVilkårssett(
    saksopplysninger: List<Saksopplysning>,
    vilkårsvurderinger: List<Vurdering>,
    kravdatoSaksopplysninger: KravdatoSaksopplysninger,
): Vilkårssett {
    val vilkårssettJson = deserialize<VilkårssettJson>(this)
    return Vilkårssett(
        kvpVilkår = vilkårssettJson.kvpVilkår.toDomain(),
        saksopplysninger = saksopplysninger,
        vilkårsvurderinger = vilkårsvurderinger,
        kravdatoSaksopplysninger = kravdatoSaksopplysninger,
    )
}

internal fun Vilkårssett.toDbJson(): String {
    return serialize(VilkårssettJson(kvpVilkår = kvpVilkår.toDbJson()))
}
