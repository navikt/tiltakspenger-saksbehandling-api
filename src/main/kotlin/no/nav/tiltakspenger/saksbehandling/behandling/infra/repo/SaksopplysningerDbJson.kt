package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakelseDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.toDbJson
import java.time.LocalDate
import java.time.LocalDateTime

private data class SaksopplysningerDbJson(
    val fødselsdato: String,
    // TODO jah: Rename til tiltaksdeltakelse
    val tiltaksdeltagelse: List<TiltaksdeltakelseDb>,
    val ytelser: YtelserDbJson,
    val tiltakspengevedtakFraArena: TiltakspengevedtakFraArenaDbJson,
    val oppslagstidspunkt: LocalDateTime,
)

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
