package com.lunaiptv.core.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uEntryClassificationTest {

    private fun entry(
        name: String = "Test",
        groupTitle: String? = null,
        type: String? = null,
        tvgType: String? = null,
    ) = M3uEntry(
        name = name,
        streamUrl = "http://example.com/stream",
        logo = null,
        groupTitle = groupTitle,
        tvgId = null,
        tvgChno = null,
        type = type,
        tvgType = tvgType,
        catchup = null,
        catchupSource = null,
        catchupDays = null,
    )

    // --- Explicit type attribute tests ---

    @Test
    fun explicit_type_vod_isVod() {
        assertTrue(entry(type = "vod").isVod)
    }

    @Test
    fun explicit_type_movie_isVod() {
        assertTrue(entry(type = "movie").isVod)
    }

    @Test
    fun explicit_type_series_isSeries() {
        val e = entry(type = "series")
        assertTrue(e.isSeries)
        assertTrue(e.isVod) // isVod includes isSeries
    }

    @Test
    fun explicit_tvgType_vod_isVod() {
        assertTrue(entry(tvgType = "vod").isVod)
    }

    @Test
    fun explicit_tvgType_series_isSeries() {
        assertTrue(entry(tvgType = "series").isSeries)
    }

    @Test
    fun no_type_no_group_is_neither() {
        val e = entry()
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    // --- Group-title heuristic tests (movies) ---

    @Test
    fun groupTitle_movies_isVod() {
        assertTrue(entry(groupTitle = "Movies").isVod)
    }

    @Test
    fun groupTitle_movie_isVod() {
        assertTrue(entry(groupTitle = "movie").isVod)
    }

    @Test
    fun groupTitle_Movie_isVod() {
        assertTrue(entry(groupTitle = "Movie").isVod)
    }

    @Test
    fun groupTitle_VOD_isVod() {
        assertTrue(entry(groupTitle = "VOD").isVod)
    }

    @Test
    fun groupTitle_film_isVod() {
        assertTrue(entry(groupTitle = "Film").isVod)
    }

    @Test
    fun groupTitle_films_isVod() {
        assertTrue(entry(groupTitle = "Films").isVod)
    }

    @Test
    fun groupTitle_cinema_isVod() {
        assertTrue(entry(groupTitle = "Cinema").isVod)
    }

    @Test
    fun groupTitle_peliculas_isVod() {
        assertTrue(entry(groupTitle = "Peliculas").isVod)
    }

    @Test
    fun groupTitle_pelicula_isVod() {
        assertTrue(entry(groupTitle = "Pelicula").isVod)
    }

    @Test
    fun groupTitle_cine_isVod() {
        assertTrue(entry(groupTitle = "Cine").isVod)
    }

    @Test
    fun groupTitle_HD_Movies_isVod() {
        assertTrue(entry(groupTitle = "HD Movies").isVod)
    }

    @Test
    fun groupTitle_4K_Movies_isVod() {
        assertTrue(entry(groupTitle = "4K Movies").isVod)
    }

    @Test
    fun groupTitle_SD_Movies_isVod() {
        assertTrue(entry(groupTitle = "SD Movies").isVod)
    }

    @Test
    fun groupTitle_video_on_demand_isVod() {
        assertTrue(entry(groupTitle = "Video On Demand").isVod)
    }

    // --- Group-title heuristic tests (series) ---

    @Test
    fun groupTitle_series_isSeries() {
        assertTrue(entry(groupTitle = "Series").isSeries)
    }

    @Test
    fun groupTitle_TV_Shows_isSeries() {
        assertTrue(entry(groupTitle = "TV Shows").isSeries)
    }

    @Test
    fun groupTitle_Drama_isSeries() {
        assertTrue(entry(groupTitle = "Drama").isSeries)
    }

    @Test
    fun groupTitle_Episodio_isSeries() {
        assertTrue(entry(groupTitle = "Episodio").isSeries)
    }

    @Test
    fun groupTitle_Series_HD_isSeries() {
        assertTrue(entry(groupTitle = "Series HD").isSeries)
    }

    // --- Negative tests (should NOT match) ---

    @Test
    fun groupTitle_Sports_is_neither() {
        val e = entry(groupTitle = "Sports")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun groupTitle_News_is_neither() {
        val e = entry(groupTitle = "News")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun groupTitle_Kids_is_neither() {
        val e = entry(groupTitle = "Kids")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun groupTitle_Entertainment_is_neither() {
        val e = entry(groupTitle = "Entertainment")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun groupTitle_Documentary_is_neither() {
        val e = entry(groupTitle = "Documentary")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun null_groupTitle_is_neither() {
        val e = entry(groupTitle = null)
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun empty_groupTitle_is_neither() {
        val e = entry(groupTitle = "")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    @Test
    fun groupTitle_with_live_prefix_is_neither() {
        val e = entry(groupTitle = "Live TV")
        assertFalse(e.isSeries)
        assertFalse(e.isVod)
    }

    // --- Series takes priority over VOD ---

    @Test
    fun groupTitle_series_movies_is_series_not_vod_alone() {
        val e = entry(groupTitle = "Series Movies")
        assertTrue(e.isSeries)
        assertTrue(e.isVod) // isVod returns true because isSeries is true
    }

    // --- Case insensitivity ---

    @Test
    fun groupTitle_case_insensitive_movies() {
        assertTrue(entry(groupTitle = "MOVIES").isVod)
        assertTrue(entry(groupTitle = "MoViEs").isVod)
    }

    @Test
    fun groupTitle_case_insensitive_series() {
        assertTrue(entry(groupTitle = "SERIES").isSeries)
        assertTrue(entry(groupTitle = "SeRiEs").isSeries)
    }

    // --- Contains matching ---

    @Test
    fun groupTitle_contains_movies_in_longer_string() {
        assertTrue(entry(groupTitle = "Best Movies 2024").isVod)
    }

    @Test
    fun groupTitle_contains_series_in_longer_string() {
        assertTrue(entry(groupTitle = "My Series Collection").isSeries)
    }

    @Test
    fun groupTitle_action_movies_isVod() {
        assertTrue(entry(groupTitle = "Action Movies").isVod)
    }

    @Test
    fun groupTitle_comedy_series_isSeries() {
        assertTrue(entry(groupTitle = "Comedy Series").isSeries)
    }

    // --- Real-world playlist patterns ---

    @Test
    fun real_world_pattern_tvg_type_attribute() {
        assertTrue(entry(tvgType = "movie").isVod)
        assertTrue(entry(tvgType = "series").isSeries)
    }

    @Test
    fun real_world_pattern_vod_group_no_type_attr() {
        val e = entry(groupTitle = "VOD | English")
        assertTrue(e.isVod)
    }

    @Test
    fun real_world_pattern_series_group_no_type_attr() {
        val e = entry(groupTitle = "Series | Spanish")
        assertTrue(e.isSeries)
    }

    @Test
    fun real_world_pattern_movies_group_with_special_chars() {
        assertTrue(entry(groupTitle = "★ Movies ★").isVod)
    }

    @Test
    fun real_world_pattern_hd_series() {
        assertTrue(entry(groupTitle = "HD Series").isSeries)
    }
}
