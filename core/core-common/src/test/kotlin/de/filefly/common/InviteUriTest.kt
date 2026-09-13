package de.filefly.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InviteUriTest {
    @Test
    fun `build then parse round-trips server and code`() {
        val uri = InviteUri.build("http://192.168.178.123:8000", "abc123XY")
        val parsed = InviteUri.parse(uri)
        assertThat(parsed).isNotNull()
        assertThat(parsed!!.server).isEqualTo("http://192.168.178.123:8000")
        assertThat(parsed.code).isEqualTo("abc123XY")
    }

    @Test
    fun `build strips trailing slash on server`() {
        val parsed = InviteUri.parse(InviteUri.build("http://host:8000/", "c0de"))
        assertThat(parsed!!.server).isEqualTo("http://host:8000")
    }

    @Test
    fun `parse rejects wrong scheme or host`() {
        assertThat(InviteUri.parse("https://invite?code=x")).isNull()
        assertThat(InviteUri.parse("filefly://other?code=x")).isNull()
        assertThat(InviteUri.parse("not a uri")).isNull()
        assertThat(InviteUri.parse(null)).isNull()
    }

    @Test
    fun `parse requires a code`() {
        assertThat(InviteUri.parse("filefly://invite?server=http://h")).isNull()
        assertThat(InviteUri.parse("filefly://invite")).isNull()
    }

    @Test
    fun `parse allows empty server`() {
        val parsed = InviteUri.parse("filefly://invite?code=onlycode")
        assertThat(parsed).isNotNull()
        assertThat(parsed!!.server).isEmpty()
        assertThat(parsed.code).isEqualTo("onlycode")
    }

    @Test
    fun `round-trips codes with url-unsafe characters`() {
        val code = "a+b/c=d&e"
        val parsed = InviteUri.parse(InviteUri.build("http://h:1", code))
        assertThat(parsed!!.code).isEqualTo(code)
    }
}
