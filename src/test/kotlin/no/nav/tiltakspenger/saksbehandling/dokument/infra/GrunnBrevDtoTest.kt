package no.nav.tiltakspenger.saksbehandling.dokument.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import org.junit.jupiter.api.Test

class GrunnBrevDtoTest {
    /**
     * Kodene er kontrakten mot `lib/hjemmeltekster.typ` i tiltakspenger-pdfgenrs.
     * Testen bruker strenger, slik at en omdøping av enumverdiene ikke kan endre kontrakten uten at testen feiler.
     */
    @Test
    fun `kodene er de samme som pdfgenrs bruker`() {
        GrunnBrevDto.entries.map { it.name } shouldBe listOf(
            "DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK",
            "ALDER",
            "LIVSOPPHOLDYTELSE",
            "KVALIFISERINGSPROGRAMMET",
            "INTRODUKSJONSPROGRAMMET",
            "LØNN_FRA_TILTAKSARRANGØR",
            "LØNN_FRA_ANDRE",
            "INSTITUSJONSOPPHOLD",
            "IKKE_LOVLIG_OPPHOLD",
            "FREMMET_FOR_SENT",
            "FORLENGET_VENTETID",
        )
    }

    @Test
    fun `mapper hvert avslagsgrunnlag til riktig kode`() {
        Avslagsgrunnlag.entries.associateWith { it.tilGrunnBrevDto() } shouldBe mapOf(
            Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak to GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
            Avslagsgrunnlag.Alder to GrunnBrevDto.ALDER,
            Avslagsgrunnlag.Livsoppholdytelser to GrunnBrevDto.LIVSOPPHOLDYTELSE,
            Avslagsgrunnlag.Kvalifiseringsprogrammet to GrunnBrevDto.KVALIFISERINGSPROGRAMMET,
            Avslagsgrunnlag.Introduksjonsprogrammet to GrunnBrevDto.INTRODUKSJONSPROGRAMMET,
            Avslagsgrunnlag.LønnFraTiltaksarrangør to GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR,
            Avslagsgrunnlag.LønnFraAndre to GrunnBrevDto.LØNN_FRA_ANDRE,
            Avslagsgrunnlag.Institusjonsopphold to GrunnBrevDto.INSTITUSJONSOPPHOLD,
            Avslagsgrunnlag.FremmetForSent to GrunnBrevDto.FREMMET_FOR_SENT,
            Avslagsgrunnlag.ForlengetVentetid to GrunnBrevDto.FORLENGET_VENTETID,
        )
    }

    @Test
    fun `mapper hver hjemmel for stans til riktig kode`() {
        HjemmelForStans.entries.associateWith { it.tilGrunnBrevDto() } shouldBe mapOf(
            HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak to GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
            HjemmelForStans.Alder to GrunnBrevDto.ALDER,
            HjemmelForStans.Livsoppholdytelser to GrunnBrevDto.LIVSOPPHOLDYTELSE,
            HjemmelForStans.Kvalifiseringsprogrammet to GrunnBrevDto.KVALIFISERINGSPROGRAMMET,
            HjemmelForStans.Introduksjonsprogrammet to GrunnBrevDto.INTRODUKSJONSPROGRAMMET,
            HjemmelForStans.LønnFraTiltaksarrangør to GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR,
            HjemmelForStans.LønnFraAndre to GrunnBrevDto.LØNN_FRA_ANDRE,
            HjemmelForStans.Institusjonsopphold to GrunnBrevDto.INSTITUSJONSOPPHOLD,
            HjemmelForStans.IkkeLovligOpphold to GrunnBrevDto.IKKE_LOVLIG_OPPHOLD,
        )
    }

    @Test
    fun `mapper hver hjemmel for opphør til riktig kode`() {
        HjemmelForOpphør.entries.associateWith { it.tilGrunnBrevDto() } shouldBe mapOf(
            HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak to GrunnBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
            HjemmelForOpphør.Alder to GrunnBrevDto.ALDER,
            HjemmelForOpphør.Livsoppholdytelser to GrunnBrevDto.LIVSOPPHOLDYTELSE,
            HjemmelForOpphør.Kvalifiseringsprogrammet to GrunnBrevDto.KVALIFISERINGSPROGRAMMET,
            HjemmelForOpphør.Introduksjonsprogrammet to GrunnBrevDto.INTRODUKSJONSPROGRAMMET,
            HjemmelForOpphør.LønnFraTiltaksarrangør to GrunnBrevDto.LØNN_FRA_TILTAKSARRANGØR,
            HjemmelForOpphør.LønnFraAndre to GrunnBrevDto.LØNN_FRA_ANDRE,
            HjemmelForOpphør.Institusjonsopphold to GrunnBrevDto.INSTITUSJONSOPPHOLD,
            HjemmelForOpphør.IkkeLovligOpphold to GrunnBrevDto.IKKE_LOVLIG_OPPHOLD,
            HjemmelForOpphør.FremmetForSent to GrunnBrevDto.FREMMET_FOR_SENT,
        )
    }
}
