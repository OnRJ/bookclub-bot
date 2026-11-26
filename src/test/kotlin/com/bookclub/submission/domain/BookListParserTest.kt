package com.bookclub.submission.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BookListParserTest {

    private val parser = BookListParser()

    @Test
    fun `should parse numbered list`() {
        val input = """
            1. Book One
            2. Book Two
            3. Book Three
            4. Book Four
            5. Book Five
            6. Book Six
            7. Book Seven
            8. Book Eight
            9. Book Nine
            10. Book Ten
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(10, result.size)
        assertEquals("Book One", result.first().title)
        assertEquals("Book Ten", result.last().title)
    }

    @Test
    fun `should parse semicolon separated list`() {
        val input = (1..10).joinToString("; ") {
            "Book $it"
        }

        val result = parser.parse(input)

        assertEquals(10, result.size)
    }

    @Test
    fun `should ignore empty lines`() {
        val input = (1..10).joinToString("\n\n") {
            "Book $it"
        }

        assertEquals(10, parser.parse(input).size)
    }

    @Test
    fun `should parse markdown titles and authors and ignore heading`() {
        val input = """
            ###
            1. **Идиот** — Достоевский
            2. **Бойцовский клуб** — Паланик
            3. **Тень ветра** — Сафон
            4. **Коллекционер** — Фаулз
            5. **Маленькая жизнь** — Янагихара
            6. **О дивный новый мир** — Хаксли
            7. **Зов предков** — Лондон
            8. **Исчезнувшая** — Флинн
            9. **Человек в поисках смысла** — Франкл
            10. **Вино из одуванчиков** — Брэдбери
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals("Идиот", result.first().title)
        assertEquals("Достоевский", result.first().author)
        assertEquals("Вино из одуванчиков", result.last().title)
        assertEquals("Брэдбери", result.last().author)
    }

    @Test
    fun `should reject fewer than ten books`() {
        val input = (1..9).joinToString("\n") {
            "Book $it"
        }

        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(input)
        }
    }

    @Test
    fun `should reject duplicate books`() {
        val input = """
            Book One
            Book Two
            Book Three
            Book Four
            Book Five
            Book Six
            Book Seven
            Book Eight
            Book Nine
            book one
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parser.parse(input)
        }
    }
}
