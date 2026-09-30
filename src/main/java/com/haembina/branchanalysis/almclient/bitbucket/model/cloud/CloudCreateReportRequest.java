/*
 * Copyright (C) 2020 Marvin Wichmann
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
package com.haembina.branchanalysis.almclient.bitbucket.model.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.haembina.branchanalysis.almclient.bitbucket.model.CodeInsightsReport;
import com.haembina.branchanalysis.almclient.bitbucket.model.ReportData;

import java.time.Instant;
import java.util.List;

public class CloudCreateReportRequest extends CodeInsightsReport {
    @JsonProperty("created_on")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    private final Instant createdDate;
    @JsonProperty("logo_url")
    private final String logoUrl;
    @JsonProperty("report_type")
    private final String reportType;
    @JsonProperty("remote_link_enabled")
    private final boolean remoteLinkEnabled;

    @JsonCreator
    public CloudCreateReportRequest(
            List<ReportData> data,
            String details,
            String title,
            String reporter,
            Instant createdDate,
            String link,
            String logoUrl,
            String reportType,
            String result) {
        super(data, details, title, reporter, link, result);
        this.createdDate = createdDate;
        this.logoUrl = logoUrl;
        this.reportType = reportType;
        this.remoteLinkEnabled = true;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getReportType() {
        return reportType;
    }

    public Boolean getRemoteLinkEnabled() {
        return remoteLinkEnabled;
    }
}
