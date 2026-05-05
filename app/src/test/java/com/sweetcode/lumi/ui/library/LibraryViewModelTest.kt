package com.sweetcode.lumi.ui.library

import io.github.amiigood.lumi.lumi.data.model.ItemMetadata
import io.github.amiigood.lumi.lumi.data.model.MediaFormat
import io.github.amiigood.lumi.lumi.data.model.MediaItem
import io.github.amiigood.lumi.lumi.data.model.MediaType
import io.github.amiigood.lumi.lumi.data.repository.LibraryRepository
import io.github.amiigood.lumi.lumi.ui.library.LibraryEntry
import io.github.amiigood.lumi.lumi.ui.library.LibraryFilter
import io.github.amiigood.lumi.lumi.ui.library.LibrarySort
import io.github.amiigood.lumi.lumi.ui.library.LibraryViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()


    }

    private fun item(
        id: String,
        title: String,
        collection: String? = null,
        type: MediaType = MediaType.MANGA,
        addedAt: Long = 0,
        currentPage: Int = 0,
        pageCount: Int = 100
    ) = MediaItem(
        id = id,
        title = title,
        author = null,
        coverPath = null,
        filePath = "/test/$id",
        format = MediaFormat.CBZ,
        type = type,
        collection = collection,
        pageCount = pageCount,
        currentPage = currentPage,
        lastReadAt = null,
        addedAt = addedAt,
        metadata = ItemMetadata()
    )

    @Test
    fun `agrupa items con misma coleccion como Series`() = runTest {
        val items = listOf(
            item("1", "Berserk Tomo 1", collection = "Berserk"),
            item("2", "Berserk Tomo 2", collection = "Berserk"),
            item("3", "One Piece Tomo 1", collection = "One Piece"),
            item("4", "One Piece Tomo 2", collection = "One Piece")
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()

        val entries = vm.state.value.entries
        assertEquals(2, entries.size)
        assertTrue(entries.all { it is LibraryEntry.Series })
    }

    @Test
    fun `item unico en coleccion se muestra como Single`() = runTest {
        val items = listOf(
            item("1", "Solo Tomo", collection = "Berserk")
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()

        val entries = vm.state.value.entries
        assertEquals(1, entries.size)
        assertTrue(entries.first() is LibraryEntry.Single)
    }

    @Test
    fun `filtra por tipo MANGA`() = runTest {
        val items = listOf(
            item("1", "Manga A", type = MediaType.MANGA),
            item("2", "Comic B", type = MediaType.COMIC),
            item("3", "Book C", type = MediaType.BOOK)
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()
        vm.setFilter(LibraryFilter.MANGA)

        val entries = vm.state.value.entries
        assertEquals(1, entries.size)
        assertEquals("Manga A", (entries.first() as LibraryEntry.Single).item.title)
    }

    @Test
    fun `query filtra por titulo`() = runTest {
        val items = listOf(
            item("1", "Berserk"),
            item("2", "One Piece"),
            item("3", "Naruto")
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()
        vm.setQuery("naru")

        val entries = vm.state.value.entries
        assertEquals(1, entries.size)
        assertEquals("Naruto", (entries.first() as LibraryEntry.Single).item.title)
    }

    @Test
    fun `featured prioriza items en progreso`() = runTest {
        val items = listOf(
            item("1", "No empezado", currentPage = 0, pageCount = 100),
            item("2", "En progreso", currentPage = 50, pageCount = 100),
            item("3", "Otro en progreso", currentPage = 30, pageCount = 100)
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()

        assertEquals("En progreso", vm.state.value.featured?.title)
    }

    @Test
    fun `sort alfabetico ordena correctamente`() = runTest {
        val items = listOf(
            item("1", "Zelda"),
            item("2", "Akira"),
            item("3", "Mario")
        )
        every { repository.items } returns MutableStateFlow(items)
        coEvery { repository.loadIfNeeded() } returns Unit

        val vm = LibraryViewModel(repository)
        advanceUntilIdle()
        vm.setSort(LibrarySort.ALPHABETICAL)

        val titles = vm.state.value.entries.map {
            (it as LibraryEntry.Single).item.title
        }
        assertEquals(listOf("Akira", "Mario", "Zelda"), titles)
    }
}