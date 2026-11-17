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
package com.haembina.branchanalysis.housekeeping;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;

@ServerSide
public class AnalysisCleanerSchedule implements Startable {

    static final long FIRST_RUN_MINUTES = 5;
    static final long INTERVAL_MINUTES = 60;

    private final AnalysisCleaner analysisCleaner;
    private final Supplier<ScheduledExecutorService> executorFactory;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> runs;

    public AnalysisCleanerSchedule(AnalysisCleaner analysisCleaner) {
        this(analysisCleaner, () -> Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "haembina-analysis-cleaner");
            thread.setDaemon(true);
            return thread;
        }));
    }

    AnalysisCleanerSchedule(AnalysisCleaner analysisCleaner, Supplier<ScheduledExecutorService> executorFactory) {
        this.analysisCleaner = analysisCleaner;
        this.executorFactory = executorFactory;
    }

    @Override
    public void start() {
        executor = executorFactory.get();
        runs = executor.scheduleWithFixedDelay(analysisCleaner::cleanAll, FIRST_RUN_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        if (executor != null) {
            runs.cancel(true);
            executor.shutdownNow();
        }
    }
}
