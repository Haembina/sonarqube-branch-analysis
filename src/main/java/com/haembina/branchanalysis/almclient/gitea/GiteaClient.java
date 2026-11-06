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
package com.haembina.branchanalysis.almclient.gitea;

import java.io.IOException;
import java.util.List;

import com.haembina.branchanalysis.almclient.gitea.model.Comment;
import com.haembina.branchanalysis.almclient.gitea.model.CommitStatus;
import com.haembina.branchanalysis.almclient.gitea.model.PullRequest;
import com.haembina.branchanalysis.almclient.gitea.model.User;

public interface GiteaClient {

    User getCurrentUser() throws IOException;

    PullRequest getPullRequest(String repository, long index) throws IOException;

    List<Comment> getComments(String repository, long index) throws IOException;

    void createComment(String repository, long index, String body) throws IOException;

    void editComment(String repository, long commentId, String body) throws IOException;

    void setCommitStatus(String repository, String sha, CommitStatus status) throws IOException;
}
