package no.nav.tiltakspenger.saksbehandling

import io.github.serpro69.kfaker.faker
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.respondOk
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenererLokalKabalHendelseCommand
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.LokalHendelseUtfall
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTOMapper.tilDomenePdl
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySakMedNySøknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySøknadForFnr

internal const val DEV_ROUTE = "/dev"

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    val applicationContext = applicationContext as LocalApplicationContext

    data class NySøknadBody(
        val fnr: String?,
        val deltakelsesperiode: PeriodeDbJson?,
        val barnetillegg: List<BarnetilleggDTO>,
    )

    val faker = faker {
        fakerConfig {
            randomSeed = Fnr.random().verdi.toLong()
            locale = "nb-NO"
        }
    }

    post("$DEV_ROUTE/soknad/ny") {
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
                )
                call.respondText(saksnummer.verdi)
            }
        }
    }

    data class NyKlagehendelseBody(
        val klagebehandlingId: String,
        val type: String,
        val utfall: String,
    )

    post("$DEV_ROUTE/klage/hendelse") {
        call.withBody<NyKlagehendelseBody> { body ->
            applicationContext.lokalKabalHendelseService.genererHendelse(
                command = GenererLokalKabalHendelseCommand(
                    type = body.type,
                    utfall = LokalHendelseUtfall.valueOf(body.utfall),
                    klagebehandlingId = KlagebehandlingId.fromString(body.klagebehandlingId),
                ),
            )
            call.respondOk()
        }
    }
}
