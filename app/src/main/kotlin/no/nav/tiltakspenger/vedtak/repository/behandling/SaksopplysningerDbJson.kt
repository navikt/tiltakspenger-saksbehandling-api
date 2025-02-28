package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.vedtak.repository.behandling.SaksopplysningerDbJson.TiltaksdeltagelseDbJson
import no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse.toDb
import no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse.toTiltakDeltakerstatus
import no.nav.tiltakspenger.vedtak.repository.behandling.tiltakDeltagelse.toTiltakskilde
import no.nav.tiltakspenger.vedtak.repository.tiltak.toDb
import no.nav.tiltakspenger.vedtak.repository.tiltak.toTiltakstypeSomGirRett
import java.time.LocalDate

private data class SaksopplysningerDbJson(
    val fødselsdato: String,
    val tiltaksdeltagelse: TiltaksdeltagelseDbJson,
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
        tiltaksdeltagelse = tiltaksdeltagelse.toDbJson(),
    ).let { serialize(it) }
}

fun String.toSaksopplysninger(): Saksopplysninger {
    val dbJson = deserialize<SaksopplysningerDbJson>(this)
    return Saksopplysninger(
        fødselsdato = LocalDate.parse(dbJson.fødselsdato),
        tiltaksdeltagelse = dbJson.tiltaksdeltagelse.toDomain(),
    )
}
