package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.feature.project.data.dto.ModelsResponseDto
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Срезка префикса — единственное место, где клиент чинит расхождение `/models`
 * с `POST /generate`: полный URI не годится ни для яндексовых моделей (уехал бы
 * двойной префикс), ни для OpenAI (не опознали бы провайдера).
 */
internal class ModelMappersTest {

    @Test
    fun `у яндексовой модели срезается gpt-схема и каталог, версия остаётся`() {
        val models = parse(
            """{"data":[{"id":"gpt://b1gttnvmtpd0tagdlb7o/deepseek-v4-flash/latest"}]}""",
        )

        assertEquals(listOf(GenerationModel.DefaultId), models.map { it.id })
    }

    @Test
    fun `у OpenAI-записи срезается провайдер — иначе она уехала бы в Yandex API`() {
        val models = parse("""{"data":[{"id":"gpt://openai/gpt-5.6-luna","owned_by":"OpenAI"}]}""")

        assertEquals(listOf(GenerationModel(id = "gpt-5.6-luna", owner = "OpenAI")), models)
    }

    @Test
    fun `строка без gpt-схемы уже короткая — отдаём как есть`() {
        val models = parse("""{"data":[{"id":"deepseek-v4-flash/latest"}]}""")

        assertEquals(listOf(GenerationModel(id = GenerationModel.DefaultId, owner = null)), models)
    }

    @Test
    fun `записи без id и с пустым id отсеиваются`() {
        // Бэкенд их не отдаёт, но контракт этого не обещает: у ModelDto оба
        // поля nullable.
        val models = parse(
            """{"data":[{"owned_by":"OpenAI"},{"id":""},{"id":"gpt://folder"},{"id":"qwen3-235b-a22b-fp8/latest"}]}""",
        )

        assertEquals(listOf("qwen3-235b-a22b-fp8/latest"), models.map { it.id })
    }

    @Test
    fun `дубли схлопываются по уже срезанному id`() {
        // Бэкенд дописывает OpenAI-запись с проверкой на дубль, но сверяет
        // полные URI — после срезки они могут совпасть.
        val models = parse(
            """{"data":[{"id":"gpt://openai/gpt-5.6-luna","owned_by":"OpenAI"},{"id":"gpt-5.6-luna"}]}""",
        )

        assertEquals(listOf(GenerationModel(id = "gpt-5.6-luna", owner = "OpenAI")), models)
    }

    @Test
    fun `пустой data — валидный ответ, а не ошибка разбора`() {
        assertEquals(emptyList<GenerationModel>(), parse("""{"object":"list","data":[]}"""))
    }

    @Test
    fun `data отсутствует — тоже пустой список`() {
        assertEquals(emptyList<GenerationModel>(), parse("""{"object":"list"}"""))
    }

    @Test
    fun `пустой owned_by подзаголовка не даёт`() {
        val models = parse("""{"data":[{"id":"gpt-oss-120b/latest","owned_by":"  "}]}""")

        assertEquals(listOf<String?>(null), models.map { it.owner })
    }

    private fun parse(json: String): List<GenerationModel> =
        ApiJson.decodeFromString<ModelsResponseDto>(json).toDomain()
}
