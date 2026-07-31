/*
 * ************************************************************************
 *  INextApiService.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.moviepedia

import org.videolan.moviepedia.models.body.ScrobbleBody
import org.videolan.moviepedia.models.body.ScrobbleBodyBatch
import org.videolan.moviepedia.models.identify.IdentifyBatchResult
import org.videolan.moviepedia.models.identify.IdentifyResult
import org.videolan.moviepedia.models.identify.MoviepediaMedia
import org.videolan.moviepedia.models.media.cast.CastResult

interface IMoviepediaApiService {

    suspend fun searchMedia(body: ScrobbleBody): IdentifyResult

    suspend fun searchMediaBatch(body: List<ScrobbleBodyBatch>): List<IdentifyBatchResult>

    suspend fun getMedia(mediaId: String): MoviepediaMedia

    suspend fun getMediaCast(mediaId: String): CastResult
}
