package no.nav.tiltakspenger.saksbehandling.lokal.søknad.infra.route

import io.github.serpro69.kfaker.faker
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.common.withBody
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.lokal.LocalApplicationContext
import no.nav.tiltakspenger.saksbehandling.lokal.infra.route.DEV_ROUTE
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTOMapper.tilDomenePdl

private const val PATH = "$DEV_ROUTE/soknad/ny"

private data class NySøknadBody(
    val fnr: String?,
    val deltakelsesperiode: PeriodeDbJson?,
    val barnetillegg: List<BarnetilleggDTO>,
)

fun Route.nySøknadDevRoute(applicationContext: LocalApplicationContext) {
    val faker = faker {
        fakerConfig {
            randomSeed = Fnr.random().verdi.toLong()
            locale = "nb-NO"
        }
    }

    post(PATH) {
        call.withBody<NySøknadBody> { body ->
            val fnr = body.fnr?.let { Fnr.tryFromString(it) }
            val barnetillegg = body.barnetillegg.map {
                val fødselsdato = it.fødselsdato ?: faker.person.birthDate(13)
                val fornavn = it.fornavn ?: faker.name.firstName()
                val etternavn = it.etternavn ?: faker.name.lastName()
                BarnetilleggDTO(
                    fnr = it.fnr ?: Fnr.random().verdi,
                    fødselsdato = fødselsdato,
                    fornavn = fornavn,
                    mellomnavn = null,
                    etternavn = etternavn,
                    oppholderSegIEØS = it.oppholderSegIEØS,
                )
            }.map { it.tilDomenePdl() }

            if (fnr == null) {
                val saksnummer = nySakMedNySøknad(
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                    applicationContext = applicationContext,
                    barnetillegg = barnetillegg,
                    tiltaksdeltakerRepo = applicationContext.tiltakContext.tiltaksdeltakerRepo,
                )
                call.respondText(saksnummer.verdi)
            } else {
                val saksnummer = nySøknadForFnr(
                    fnr = fnr,
                    applicationContext = applicationContext,
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                    barnetillegg = barnetillegg,
                    tiltaksdeltakerRepo = applicationContext.tiltakContext.tiltaksdeltakerRepo,
                )
                call.respondText(saksnummer.verdi)
            }
        }
    }
}
