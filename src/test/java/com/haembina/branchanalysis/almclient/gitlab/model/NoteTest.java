/*
 * Copyright (C) 2026 Haembina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.haembina.branchanalysis.almclient.gitlab.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class NoteTest {

    @Test
    void shouldReadWhetherANoteCanBeResolved() throws JsonProcessingException {
        Note note = new ObjectMapper().readValue("{\"id\":3,\"system\":false,\"author\":{\"username\":\"bot\"},"
                + "\"body\":\"text\",\"resolved\":false,\"resolvable\":true}", Note.class);

        assertThat(note.isResolvable()).isTrue();
        assertThat(note.isResolved()).isFalse();
        assertThat(note.getAuthor().getUsername()).isEqualTo("bot");
    }
}
