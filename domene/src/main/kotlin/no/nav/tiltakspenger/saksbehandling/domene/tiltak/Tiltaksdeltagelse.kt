package no.nav.tiltakspenger.saksbehandling.domene.tiltak

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett

/**
 * @param eksternDeltagelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltagelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeNavn TODO jah Burde renames til tiltaksnavn eller gjennomføringsnavn (ta opp med Tia). Kan inneholde geolokaasjon, eksempelvis Rema 1000 i Strandveien.
 * @param gjennomføringId Ekstern id. Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien. En person knyttes til en gjennomføring og det kalles da en deltagelse. Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 */
data class Tiltaksdeltagelse(
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRett,
    val rettPåTiltakspenger: Boolean,
    /** TODO John og Anders: Det er ikke en garanti at vi har både fraOgMed og tilOgMed fra Arena. Foreslår at vi endrer denne til nullable fraOgMed og tilOgMed; og dokumenterer det godt. */
    val deltakelsesperiode: Periode,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
)
