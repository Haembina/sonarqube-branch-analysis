/*
 * Copyright (C) 2019 Michael Clarke
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

import java.io.Closeable;
import java.io.IOException;
import java.net.URLClassLoader;

/**
 * The class loader {@link ElevatedClassLoaderFactory} hands out: the plugin's jars with SonarQube core behind them,
 * falling back to the plugin's own loader for a class neither holds, such as one from a sibling plugin.
 *
 * <p>It owns the {@link URLClassLoader} over the plugin's jars, and {@link #close()} releases it. SonarQube never
 * signals a plugin unload, so in a running server nothing calls it and the loader lives until the JVM exits.
 */
final class ElevatedPluginClassLoader extends ClassLoader implements Closeable {

    private final URLClassLoader elevated;
    private final ClassLoader plugin;

    /**
     * @param elevated the plugin's jars, with SonarQube core as their parent; owned from here on
     * @param plugin   the plugin's own loader, asked when {@code elevated} has no such class
     */
    ElevatedPluginClassLoader(URLClassLoader elevated, ClassLoader plugin) {
        this.elevated = elevated;
        this.plugin = plugin;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return elevated.loadClass(name);
        } catch (ClassNotFoundException ex) {
            return plugin.loadClass(name);
        }
    }

    /** Releases the jars the elevated loader holds open. */
    @Override
    public void close() throws IOException {
        elevated.close();
    }
}
