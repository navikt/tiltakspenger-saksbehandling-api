package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedKildeOgVerdi
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Utfall
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vilkår
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Vurdering
import java.time.LocalDate
import java.time.LocalDateTime

data class AntallDagerSaksopplysninger(
    // todo: Vi trenger informasjon om hvilken saksbehandler som har endret antall dager.
    val antallDagerSaksopplysningerFraSBH: List<PeriodeMedKildeOgVerdi<Int>> = emptyList(),
    val antallDagerSaksopplysningerFraRegister: List<PeriodeMedKildeOgVerdi<Int>>,
    val avklartAntallDager: List<PeriodeMedKildeOgVerdi<Int>> = emptyList(),
) {
    companion object {
        fun initAntallDagerSaksopplysning(
            antallDager: List<PeriodeMedKildeOgVerdi<Int>>,
            avklarteAntallDager: List<PeriodeMedKildeOgVerdi<Int>>,
        ): AntallDagerSaksopplysninger {
            return AntallDagerSaksopplysninger(
                antallDagerSaksopplysningerFraSBH = antallDager.filter { it.kilde == Kilde.SAKSB.toString() },
                antallDagerSaksopplysningerFraRegister = antallDager.filter { it.kilde != Kilde.SAKSB.toString() },
                avklartAntallDager = avklarteAntallDager,
            )
        }
    }
    fun avklar(): AntallDagerSaksopplysninger {
        val avklart = antallDagerSaksopplysningerFraSBH.ifEmpty { antallDagerSaksopplysningerFraRegister }
        return this.copy(
            avklartAntallDager = avklart,
        )
    }
}

data class Tiltak(
    val id: String,
    val gjennomføring: Gjennomføring,
    val deltakelseFom: LocalDate,
    val deltakelseTom: LocalDate,
    val deltakelseStatus: DeltakerStatus,
    val deltakelseDagerUke: Float?,
    val deltakelseProsent: Float?,
    val kilde: String,
    val registrertDato: LocalDateTime,
    val innhentet: LocalDateTime,
    val antallDagerSaksopplysninger: AntallDagerSaksopplysninger,
) {
    data class Gjennomføring(
        val id: String,
        val arrangørnavn: String,
        val typeNavn: String,
        val typeKode: String,
        val rettPåTiltakspenger: Boolean,
    )

    data class DeltakerStatus(
        val status: String,
        val rettTilÅASøke: Boolean,
    )

    fun brukerDeltarPåTiltak(status: String): Boolean {
        return status.equals("Gjennomføres", ignoreCase = true) ||
            status.equals("Deltar", ignoreCase = true)
    }

    fun brukerHarDeltattOgSluttet(status: String): Boolean {
        return status.equals("Har sluttet", ignoreCase = true) ||
            status.equals("Fullført", ignoreCase = true) ||
            status.equals("Avbrutt", ignoreCase = true) ||
            status.equals("Deltakelse avbrutt", ignoreCase = true) ||
            status.equals("Gjennomføring avbrutt", ignoreCase = true) ||
            status.equals("Gjennomføring avlyst", ignoreCase = true)
    }

    fun lagVurderingAvTiltakdeltagelse(utfall: Utfall, detaljer: String = ""): Vurdering {
        return when (utfall) {
            Utfall.OPPFYLT -> Vurdering.Oppfylt(
                vilkår = Vilkår.TILTAKSDELTAGELSE,
                kilde = Kilde.SAKSB, // TODO: Finn ut av dette
                detaljer = detaljer,
                fom = deltakelseFom,
                tom = deltakelseTom,
                grunnlagId = this.id,
            )

            Utfall.IKKE_OPPFYLT -> Vurdering.IkkeOppfylt(
                vilkår = Vilkår.TILTAKSDELTAGELSE,
                kilde = Kilde.SAKSB, // TODO: Finn ut av dette
                detaljer = detaljer,
                fom = deltakelseFom,
                tom = deltakelseTom,
                grunnlagId = this.id,
            )

            Utfall.KREVER_MANUELL_VURDERING -> Vurdering.KreverManuellVurdering(
                vilkår = Vilkår.TILTAKSDELTAGELSE,
                kilde = Kilde.SAKSB, // TODO: Finn ut av dette
                detaljer = detaljer,
                fom = deltakelseFom,
                tom = deltakelseTom,
                grunnlagId = this.id,
            )
        }
    }

    fun vilkårsvurderTiltaksdeltagelse(): Vurdering {
        val vurdering = if (gjennomføring.rettPåTiltakspenger) {
            if (brukerDeltarPåTiltak(deltakelseStatus.status)) {
                lagVurderingAvTiltakdeltagelse(Utfall.OPPFYLT)
            } else if (brukerHarDeltattOgSluttet(deltakelseStatus.status)) {
                if (deltakelseTom.isBefore(LocalDate.now())) {
                    lagVurderingAvTiltakdeltagelse(Utfall.OPPFYLT)
                } else {
                    lagVurderingAvTiltakdeltagelse(
                        Utfall.KREVER_MANUELL_VURDERING,
                        "Status tilsier at bruker har sluttet på tiltak, men tiltaksperioden er fremover i tid",
                    )
                }
            } else {
                lagVurderingAvTiltakdeltagelse(
                    Utfall.KREVER_MANUELL_VURDERING,
                    "Vi har mottatt en status vi ikke hånderer enda. Setter til manuell",
                )
            }
        } else {
            lagVurderingAvTiltakdeltagelse(Utfall.IKKE_OPPFYLT, "Tiltaket gir ikke rett på tiltakspenger")
        }

        return vurdering
    }
}
