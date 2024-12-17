package no.nav.tiltakspenger.saksbehandling.domene.tiltak

import no.nav.tiltakspenger.felles.TiltakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett

/**
 * @param eksternDeltagelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltagelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeNavn f.eks. Jobbklubb, Arbeidsmarkedsopplæring (gruppe), Oppfølging, Arbeidstrening
 * @param gjennomføringId Ekstern id. Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien. En person knyttes til en gjennomføring og det kalles da en deltagelse. Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 */
data class Tiltak(
    val id: TiltakId,
    val eksternDeltagelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String,
    val typeKode: TiltakstypeSomGirRett,
    val rettPåTiltakspenger: Boolean,
    val deltakelsesperiode: Periode,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
)
