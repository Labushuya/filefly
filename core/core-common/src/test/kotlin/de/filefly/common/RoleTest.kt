package de.filefly.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoleTest {
    @Test
    fun `parses known roles`() {
        assertThat(Role.fromString("admin")).isEqualTo(Role.ADMIN)
        assertThat(Role.fromString("USER")).isEqualTo(Role.USER)
        assertThat(Role.fromString("service")).isEqualTo(Role.SERVICE)
        assertThat(Role.fromString("guest")).isEqualTo(Role.GUEST)
    }

    @Test
    fun `unknown falls back`() {
        assertThat(Role.fromString(null)).isEqualTo(Role.UNKNOWN)
        assertThat(Role.fromString("root")).isEqualTo(Role.UNKNOWN)
    }

    @Test
    fun `permissions per role`() {
        assertThat(Role.ADMIN.canDelete).isTrue()
        assertThat(Role.USER.canDelete).isFalse()
        assertThat(Role.USER.canMkdir).isTrue()
        assertThat(Role.SERVICE.canMkdir).isFalse()
        assertThat(Role.GUEST.canUpload).isTrue()
        assertThat(Role.UNKNOWN.canUpload).isFalse()
    }
}
