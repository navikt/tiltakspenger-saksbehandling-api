package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.SaksopplysningerDbJson.TiltaksdeltagelseDbJson
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.repo.toTiltakstypeSomGirRett
import java.time.LocalDate

private data class SaksopplysningerDbJson(
    val fødselsdato: String,
    val tiltaksdeltagelse: List<TiltaksdeltagelseDbJson>,
    val ytelser: YtelserDbJson,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArenaDbJson,
) {
    data class TiltaksdeltagelseDbJson(
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
    ) {
        fun toDomain(): Tiltaksdeltagelse {
            return Tiltaksdeltagelse(
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
            )
        }
    }
}

private fun Tiltaksdeltagelse.toDbJson(): TiltaksdeltagelseDbJson {
    return TiltaksdeltagelseDbJson(
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
    )
}

fun Saksopplysninger.toDbJson(): String {
    return SaksopplysningerDbJson(
        fødselsdato = fødselsdato.toString(),
        tiltaksdeltagelse = tiltaksdeltagelser.map { it.toDbJson() },
        ytelser = ytelser.toDbJson(),
        tiltakspengevedtakFraArena = tiltakspengevedtakFraArena.toDbJson(),
    ).let { serialize(it) }
}

fun String.toSaksopplysninger(saksopplysningsperiode: Periode?): Saksopplysninger {
    val dbJson = deserialize<SaksopplysningerDbJson>(this)
    return Saksopplysninger(
        fødselsdato = LocalDate.parse(dbJson.fødselsdato),
        tiltaksdeltagelser = Tiltaksdeltagelser(dbJson.tiltaksdeltagelse.map { it.toDomain() }),
        periode = saksopplysningsperiode,
        ytelser = dbJson.ytelser.toDomain(),
        tiltakspengevedtakFraArena = dbJson.tiltakspengevedtakFraArena.toDomain(),
    )
}
