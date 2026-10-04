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
package com.haembina.branchanalysis.classloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

class ElevatedPluginClassLoaderTest {

    @Test
    void shouldCloseTheElevatedLoaderItOwns() throws IOException {
        boolean[] closed = {false};
        URLClassLoader elevated = new URLClassLoader(new URL[]{}) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        new ElevatedPluginClassLoader(elevated, getClass().getClassLoader()).close();

        assertThat(closed[0]).isTrue();
    }
}
