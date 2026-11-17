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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalysisCleanerScheduleTest {

    private final AnalysisCleaner cleaner = mock();
    private final ScheduledExecutorService executor = mock();
    private final ScheduledFuture<?> runs = mock();

    @Test
    void cleansFiveMinutesAfterStartAndHourlyAfterThat() {
        doReturn(runs).when(executor).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        AnalysisCleanerSchedule underTest = new AnalysisCleanerSchedule(cleaner, () -> executor);

        underTest.start();

        ArgumentCaptor<Runnable> task = ArgumentCaptor.captor();
        verify(executor).scheduleWithFixedDelay(task.capture(), eq(5L), eq(60L), eq(TimeUnit.MINUTES));
        task.getValue().run();
        verify(cleaner).cleanAll();
    }

    @Test
    void stopsTheTimerWhenSonarQubeStops() {
        doReturn(runs).when(executor).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
        AnalysisCleanerSchedule underTest = new AnalysisCleanerSchedule(cleaner, () -> executor);
        underTest.start();

        underTest.stop();

        verify(runs).cancel(true);
        verify(executor).shutdownNow();
    }

    @Test
    void stopsQuietlyWhenItNeverStarted() {
        AnalysisCleanerSchedule underTest = new AnalysisCleanerSchedule(cleaner);

        assertThatNoException().isThrownBy(underTest::stop);
    }

    @Test
    void runsOnADaemonThreadOfItsOwn() {
        AnalysisCleanerSchedule underTest = new AnalysisCleanerSchedule(cleaner);

        underTest.start();
        underTest.stop();

        assertThatNoException().isThrownBy(underTest::stop);
    }
}
