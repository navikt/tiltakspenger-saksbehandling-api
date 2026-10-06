package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.ÅpenPeriode
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arenastatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Arrangør
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Deltakelsesomfang
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.EksternDeltakelseId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.GjennomføringId
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kometstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TeamTiltakstatus
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testStatusOpprettet

/**
 * Deltakelsen slik tiltakshistorikk ville levert den, til fakes og tester som trenger libs-domenet.
 * Rundturen tilbake via `tilTiltaksdeltakelseIntern` gir samme verdi, så lenge statusen finnes hos kilden.
 * Kildestatusen velges slik at den tolkes til [TiltaksdeltakelseIntern.deltakelseStatus] uavhengig av dato.
 * Tittel og arrangør finnes ikke i den interne modellen, og blir tomme.
 */
fun TiltaksdeltakelseIntern.tilLibsDeltakelse(): Tiltaksdeltakelse.GirRett {
    val id = EksternDeltakelseId(eksternDeltakelseId)
    val kildestatus = tilKildestatus()
    val tiltakstype = TiltakstypeSomGirRett.valueOf(typeKode.name)
    val arrangør = Arrangør(hovedenhet = null, underenhet = null)
    val omfang = Deltakelsesomfang(
        deltakelsesprosent = deltakelseProsent,
        dagerPerUke = antallDagerPerUke,
        deltidsprosentPåGjennomføring = deltidsprosentGjennomforing?.toFloat(),
    )
    val gjennomføringId = gjennomføringId?.let { GjennomføringId(it) }
    val fraOgMed = deltakelseFraOgMed
    val tilOgMed = deltakelseTilOgMed
    return if (fraOgMed != null && tilOgMed != null) {
        Tiltaksdeltakelse.GirRett.MedPeriode(
            id = id,
            kildestatus = kildestatus,
            tiltakstype = tiltakstype,
            tiltakstypenavn = typeNavn,
            tiltakskodeFraKilden = typeKode.name,
            tittel = null,
            arrangør = arrangør,
            omfang = omfang,
            gjennomføringId = gjennomføringId,
            periode = Periode(fraOgMed, tilOgMed),
        )
    } else {
        Tiltaksdeltakelse.GirRett.UtenPeriode(
            id = id,
            kildestatus = kildestatus,
            tiltakstype = tiltakstype,
            tiltakstypenavn = typeNavn,
            tiltakskodeFraKilden = typeKode.name,
            tittel = null,
            arrangør = arrangør,
            omfang = omfang,
            gjennomføringId = gjennomføringId,
            periode = ÅpenPeriode(fraOgMed, tilOgMed),
        )
    }
}

private fun TiltaksdeltakelseIntern.tilKildestatus(): Kildestatus {
    val status = deltakelseStatus
    val ukjentHosKilden = { throw IllegalArgumentException("$kilde har ingen status som tolkes til $status") }
    return when (kilde) {
        Tiltakskilde.Komet -> Kometstatus.Kjent(status.tilKometstatusType(), årsak = null, opprettet = testStatusOpprettet)

        Tiltakskilde.Arena -> Arenastatus.Kjent(
            when (status) {
                TiltakDeltakerstatus.Deltar -> Arenastatus.Type.TAKKET_JA_TIL_TILBUD

                TiltakDeltakerstatus.Fullført -> Arenastatus.Type.FULLFORT

                TiltakDeltakerstatus.Avbrutt -> Arenastatus.Type.DELTAKELSE_AVBRUTT

                TiltakDeltakerstatus.IkkeAktuell -> Arenastatus.Type.IKKE_AKTUELL

                TiltakDeltakerstatus.Feilregistrert -> Arenastatus.Type.FEILREGISTRERT

                TiltakDeltakerstatus.SøktInn -> Arenastatus.Type.AKTUELL

                TiltakDeltakerstatus.Venteliste -> Arenastatus.Type.VENTELISTE

                TiltakDeltakerstatus.VenterPåOppstart -> Arenastatus.Type.TILBUD

                TiltakDeltakerstatus.HarSluttet,
                TiltakDeltakerstatus.PåbegyntRegistrering,
                TiltakDeltakerstatus.Vurderes,
                -> ukjentHosKilden()
            },
        )

        Tiltakskilde.TeamTiltak -> TeamTiltakstatus.Kjent(
            when (status) {
                TiltakDeltakerstatus.PåbegyntRegistrering -> TeamTiltakstatus.Type.PAABEGYNT

                TiltakDeltakerstatus.SøktInn -> TeamTiltakstatus.Type.MANGLER_GODKJENNING

                TiltakDeltakerstatus.VenterPåOppstart -> TeamTiltakstatus.Type.KLAR_FOR_OPPSTART

                TiltakDeltakerstatus.Deltar -> TeamTiltakstatus.Type.GJENNOMFORES

                TiltakDeltakerstatus.HarSluttet -> TeamTiltakstatus.Type.AVSLUTTET

                TiltakDeltakerstatus.Avbrutt -> TeamTiltakstatus.Type.AVBRUTT

                TiltakDeltakerstatus.IkkeAktuell -> TeamTiltakstatus.Type.ANNULLERT

                TiltakDeltakerstatus.Fullført,
                TiltakDeltakerstatus.Feilregistrert,
                TiltakDeltakerstatus.Venteliste,
                TiltakDeltakerstatus.Vurderes,
                -> ukjentHosKilden()
            },
        )
    }
}

private fun TiltakDeltakerstatus.tilKometstatusType(): Kometstatus.Type = when (this) {
    TiltakDeltakerstatus.Deltar -> Kometstatus.Type.DELTAR
    TiltakDeltakerstatus.HarSluttet -> Kometstatus.Type.HAR_SLUTTET
    TiltakDeltakerstatus.Fullført -> Kometstatus.Type.FULLFORT
    TiltakDeltakerstatus.Avbrutt -> Kometstatus.Type.AVBRUTT
    TiltakDeltakerstatus.IkkeAktuell -> Kometstatus.Type.IKKE_AKTUELL
    TiltakDeltakerstatus.Feilregistrert -> Kometstatus.Type.FEILREGISTRERT
    TiltakDeltakerstatus.PåbegyntRegistrering -> Kometstatus.Type.PABEGYNT_REGISTRERING
    TiltakDeltakerstatus.SøktInn -> Kometstatus.Type.SOKT_INN
    TiltakDeltakerstatus.Venteliste -> Kometstatus.Type.VENTELISTE
    TiltakDeltakerstatus.VenterPåOppstart -> Kometstatus.Type.VENTER_PA_OPPSTART
    TiltakDeltakerstatus.Vurderes -> Kometstatus.Type.VURDERES
}
