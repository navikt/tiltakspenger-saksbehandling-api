package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.SaksopplysningerDbJson.TiltaksdeltakelseDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toTiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toTiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toTiltakstypeSomGirRett
import java.time.LocalDate
import java.time.LocalDateTime

private data class SaksopplysningerDbJson(
    val fødselsdato: String,
    // TODO jah: Rename til tiltaksdeltakelse
    val tiltaksdeltagelse: List<TiltaksdeltakelseDbJson>,
    val ytelser: YtelserDbJson,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArenaDbJson,
    val oppslagstidspunkt: LocalDateTime,
) {
    data class TiltaksdeltakelseDbJson(
        val eksternDeltagelseId: String,
        val gjennomføringId: String?,
        val typeNavn: String,
        val typeKode: String,
        val deltagelseFraOgMed: LocalDate?,
        val deltagelseTilOgMed: LocalDate?,
        val deltakelseStatus: String,
        val deltakelseProsent: Float?,
        val antallDagerPerUke: Float?,
        val kilde: String,
        val rettPåTiltakspenger: Boolean,
        val deltidsprosentGjennomforing: Double? = null,
    ) {
        fun toDomain(): Tiltaksdeltakelse {
            return Tiltaksdeltakelse(
                eksternDeltagelseId = eksternDeltagelseId,
                gjennomføringId = gjennomføringId,
                typeNavn = typeNavn,
                typeKode = typeKode.toTiltakstypeSomGirRett(),
                deltagelseFraOgMed = deltagelseFraOgMed,
                deltagelseTilOgMed = deltagelseTilOgMed,
                deltakelseStatus = deltakelseStatus.toTiltakDeltakerstatus(),
                deltakelseProsent = deltakelseProsent,
                antallDagerPerUke = antallDagerPerUke,
                kilde = kilde.toTiltakskilde(),
                rettPåTiltakspenger = rettPåTiltakspenger,
                deltidsprosentGjennomforing = deltidsprosentGjennomforing,
            )
        }
    }
}

private fun Tiltaksdeltakelse.toDbJson(): TiltaksdeltakelseDbJson {
    return TiltaksdeltakelseDbJson(
        eksternDeltagelseId = this.eksternDeltagelseId,
        gjennomføringId = this.gjennomføringId,
        typeNavn = this.typeNavn,
        typeKode = this.typeKode.toDb(),
        deltagelseFraOgMed = this.deltagelseFraOgMed,
        deltagelseTilOgMed = this.deltagelseTilOgMed,
        deltakelseStatus = this.deltakelseStatus.toDb(),
        deltakelseProsent = this.deltakelseProsent,
        antallDagerPerUke = this.antallDagerPerUke,
        kilde = this.kilde.toDb(),
        rettPåTiltakspenger = this.rettPåTiltakspenger,
        deltidsprosentGjennomforing = this.deltidsprosentGjennomforing,
    )
}

fun Saksopplysninger.toDbJson(): String {
    return SaksopplysningerDbJson(
        fødselsdato = fødselsdato.toString(),
        tiltaksdeltagelse = tiltaksdeltakelser.map { it.toDbJson() },
        ytelser = ytelser.toDbJson(),
        tiltakspengevedtakFraArena = tiltakspengevedtakFraArena.toDbJson(),
        oppslagstidspunkt = oppslagstidspunkt,
    ).let { serialize(it) }
}

fun String.toSaksopplysninger(): Saksopplysninger {
    val dbJson = deserialize<SaksopplysningerDbJson>(this)
    return Saksopplysninger(
        fødselsdato = LocalDate.parse(dbJson.fødselsdato),
        tiltaksdeltakelser = Tiltaksdeltakelser(dbJson.tiltaksdeltagelse.map { it.toDomain() }),
        ytelser = dbJson.ytelser.toDomain(),
        tiltakspengevedtakFraArena = dbJson.tiltakspengevedtakFraArena.toDomain(),
        oppslagstidspunkt = dbJson.oppslagstidspunkt,
    )
}
