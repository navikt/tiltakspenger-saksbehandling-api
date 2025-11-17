package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

/**
 * Tiltaksdeltakelse som også kan ha med [arrangørnavn]. Ettersom navnet til arrangøren kan inneholde geolokaliserende
 * informasjon tar vi med et flagg [harAdressebeskyttelse] som avgjør om feltet skal inkluderes.
 * Seperat fra [Tiltaksdeltakelse] som lagres i databasen, slik at vi unngår at [arrangørnavn] blir lagret ved en glipp
 *
 * se [Tiltaksdeltakelse]
 * @param eksternDeltakelseId mappes fra aktivitetId som vi mottar fra søknadsfrontenden (via søknad-api). Dette er tiltaksdeltakelseIDen og vil kun være forskjellig avhengig om den kommer fra Arena (TA1234567), Komet (UUID) eller team Tiltak (?). Kalles ekstern_id i databasen.
 * @param typeNavn Navn på tiltakstypen, f.eks. "Arbeidsforberedende trening"
 * @param gjennomføringId Ekstern id fra Valp. Dette er gjennomføringen sin ID, eksempelvis Rema 1000 i Strandveien. En person knyttes til en gjennomføring og det kalles da en deltakelse. Per nå mottar vi ikke denne fra Arena, men kun fra Komet.
 * @param deltakelseFraOgMed startdato for deltakelsen. Kan mangle, særlig når deltaker venter på oppstart
 * @param deltakelseTilOgMed sluttdato for deltakelsen. Kan mangle.
 */
data class TiltaksdeltakelseMedArrangørnavn(
    val harAdressebeskyttelse: Boolean,
    val eksternDeltakelseId: String,
    val gjennomføringId: String?,
    val typeNavn: String?,
    val typeKode: TiltakstypeSomGirRett,
    val arrangørnavnFørSensur: String?,
    val rettPåTiltakspenger: Boolean,
    val deltakelseFraOgMed: LocalDate?,
    val deltakelseTilOgMed: LocalDate?,
    val deltakelseStatus: TiltakDeltakerstatus,
    val deltakelseProsent: Float?,
    val antallDagerPerUke: Float?,
    val kilde: Tiltakskilde,
) {
    val arrangørnavn: String? by lazy { if (harAdressebeskyttelse) null else arrangørnavnFørSensur }
    val periode: Periode? by lazy {
        if (deltakelseFraOgMed != null && deltakelseTilOgMed != null) {
            Periode(deltakelseFraOgMed, deltakelseTilOgMed)
        } else {
            null
        }
    }
}
