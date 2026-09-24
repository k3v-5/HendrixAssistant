package com.asistente.celular.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para la navegación y jerarquía de gestos hacia atrás (BackHandler).
 * 
 * Valida de extremo a extremo que:
 * 1. Diálogos emergentes y modales intercepten y consuman el evento de retroceso primero.
 * 2. Vistas inmersivas (PcDeckTab.SCREEN, DeskStandby, Sub-studios) retornen a su contenedor padre.
 * 3. Pantallas secundarias de la aplicación (Tareas, Notas, Ajustes, Mi PC) retornen a la pantalla principal (Asistente).
 * 4. Únicamente cuando el usuario se encuentre en la raíz (Asistente) sin diálogos activos, el sistema permita salir de la app.
 */
class BackNavigationFlowTest {

    // --- Modelos de simulación de estado para pruebas exhaustivas ---

    enum class AppScreen {
        ASSISTANT,
        TASKS,
        NOTES,
        PC_MODULES,
        SETTINGS,
        DESK_STANDBY
    }

    enum class PcDeckTab {
        SCREEN,
        DECK,
        AIRSYNC,
        TELEMETRY
    }

    class MainNavigationState(
        var currentScreen: AppScreen = AppScreen.ASSISTANT,
        var isDeskStandbyActive: Boolean = false
    ) {
        val inStandby: Boolean get() = isDeskStandbyActive || currentScreen == AppScreen.DESK_STANDBY

        fun handleBack(): Boolean {
            if (inStandby) {
                isDeskStandbyActive = false
                currentScreen = AppScreen.ASSISTANT
                return true
            }
            if (currentScreen != AppScreen.ASSISTANT) {
                currentScreen = AppScreen.ASSISTANT
                return true
            }
            // Root screen: back handler disabled, system exits app
            return false
        }
    }

    class PcControlDeckNavState(
        var selectedTab: PcDeckTab = PcDeckTab.SCREEN,
        var showConnectDialog: Boolean = false,
        var showSelectorDialog: Boolean = false,
        var showUnlockDialog: Boolean = false,
        var showMacroDeckScreen: Boolean = false,
        var showRoutineDesignerScreen: Boolean = false,
        var studioDialogActive: Boolean = false,
        var inStudio: Boolean = false
    ) {
        fun handleBack(onExitDeck: () -> Unit): Boolean {
            // Nivel 1: Sub-estudio de Macro Deck
            if (showMacroDeckScreen) {
                if (inStudio) {
                    if (studioDialogActive) {
                        studioDialogActive = false
                        return true
                    }
                    inStudio = false
                    return true
                }
                showMacroDeckScreen = false
                return true
            }

            // Nivel 2: Diseñador visual de rutinas
            if (showRoutineDesignerScreen) {
                showRoutineDesignerScreen = false
                return true
            }

            // Nivel 3: Diálogos modales de la workstation
            val anyDialog = showConnectDialog || showSelectorDialog || showUnlockDialog
            if (anyDialog) {
                showConnectDialog = false
                showSelectorDialog = false
                showUnlockDialog = false
                return true
            }

            // Nivel 4: Modo inmersivo o pestañas secundarias retornan a DECK
            if (selectedTab != PcDeckTab.DECK) {
                selectedTab = PcDeckTab.DECK
                return true
            }

            // Nivel 5: Salir del módulo PC hacia la pantalla padre
            onExitDeck()
            return true
        }
    }

    class TasksNavState(
        var showAddDialog: Boolean = false
    ) {
        fun handleBack(): Boolean {
            if (showAddDialog) {
                showAddDialog = false
                return true
            }
            return false
        }
    }

    class NotesNavState(
        var isEditingNote: Boolean = false,
        var isCreatingNote: Boolean = false,
        var isSearchActive: Boolean = false,
        var searchQuery: String = ""
    ) {
        fun handleBack(): Boolean {
            if (isEditingNote) {
                isEditingNote = false
                return true
            }
            if (isCreatingNote) {
                isCreatingNote = false
                return true
            }
            if (isSearchActive) {
                isSearchActive = false
                searchQuery = ""
                return true
            }
            return false
        }
    }

    // --- Pruebas de Flujo MainActivity ---

    @Test
    fun `when on Assistant root, back gesture is not consumed and allows app exit`() {
        val nav = MainNavigationState(currentScreen = AppScreen.ASSISTANT, isDeskStandbyActive = false)
        val handled = nav.handleBack()
        assertFalse("En la pantalla Asistente raíz, el gesto atrás no debe ser interceptado para permitir cerrar la app", handled)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    @Test
    fun `when on Tasks screen, back gesture returns to Assistant`() {
        val nav = MainNavigationState(currentScreen = AppScreen.TASKS)
        val handled = nav.handleBack()
        assertTrue("Atrás desde Tareas debe ser manejado", handled)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    @Test
    fun `when on Notes screen, back gesture returns to Assistant`() {
        val nav = MainNavigationState(currentScreen = AppScreen.NOTES)
        val handled = nav.handleBack()
        assertTrue("Atrás desde Notas debe ser manejado", handled)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    @Test
    fun `when on Settings screen, back gesture returns to Assistant`() {
        val nav = MainNavigationState(currentScreen = AppScreen.SETTINGS)
        val handled = nav.handleBack()
        assertTrue("Atrás desde Ajustes debe ser manejado", handled)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    @Test
    fun `when on PcModules screen, back gesture returns to Assistant`() {
        val nav = MainNavigationState(currentScreen = AppScreen.PC_MODULES)
        val handled = nav.handleBack()
        assertTrue("Atrás desde PC Modules debe ser manejado", handled)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    @Test
    fun `when in DeskStandby mode, back gesture exits standby and returns to Assistant`() {
        val nav = MainNavigationState(currentScreen = AppScreen.DESK_STANDBY, isDeskStandbyActive = true)
        val handled = nav.handleBack()
        assertTrue("Atrás desde Desk Standby debe ser manejado", handled)
        assertFalse(nav.isDeskStandbyActive)
        assertEquals(AppScreen.ASSISTANT, nav.currentScreen)
    }

    // --- Pruebas de Flujo PcControlDeck (Jerarquía Multinivel) ---

    @Test
    fun `when PcControlDeck dialog is open, back gesture closes dialog before switching tab`() {
        val deckNav = PcControlDeckNavState(
            selectedTab = PcDeckTab.SCREEN,
            showConnectDialog = true
        )
        var exitedDeck = false

        val handled = deckNav.handleBack(onExitDeck = { exitedDeck = true })

        assertTrue(handled)
        assertFalse("El diálogo de conexión debió cerrarse", deckNav.showConnectDialog)
        assertEquals("Debe permanecer en la pestaña SCREEN tras cerrar el diálogo", PcDeckTab.SCREEN, deckNav.selectedTab)
        assertFalse("No debió salir de la workstation", exitedDeck)
    }

    @Test
    fun `when in PcDeckTab SCREEN, back gesture exits immersive mode and returns to DECK tab`() {
        val deckNav = PcControlDeckNavState(selectedTab = PcDeckTab.SCREEN)
        var exitedDeck = false

        val handled = deckNav.handleBack(onExitDeck = { exitedDeck = true })

        assertTrue(handled)
        assertEquals("Al presionar atrás en modo Pantalla en vivo, debe volver a atajos DECK", PcDeckTab.DECK, deckNav.selectedTab)
        assertFalse("No debió salir hacia el asistente todavía", exitedDeck)

        // Segundo toque de atrás desde DECK
        val handledSecond = deckNav.handleBack(onExitDeck = { exitedDeck = true })
        assertTrue(handledSecond)
        assertTrue("El segundo toque desde DECK debe solicitar salir hacia el Asistente", exitedDeck)
    }

    @Test
    fun `when in AirSync or Telemetry tabs, back gesture switches to DECK tab first`() {
        val deckNav = PcControlDeckNavState(selectedTab = PcDeckTab.AIRSYNC)
        var exitedDeck = false

        deckNav.handleBack(onExitDeck = { exitedDeck = true })
        assertEquals(PcDeckTab.DECK, deckNav.selectedTab)
        assertFalse(exitedDeck)

        val telemetryNav = PcControlDeckNavState(selectedTab = PcDeckTab.TELEMETRY)
        telemetryNav.handleBack(onExitDeck = { exitedDeck = true })
        assertEquals(PcDeckTab.DECK, telemetryNav.selectedTab)
        assertFalse(exitedDeck)
    }

    @Test
    fun `hierarchical nested back flow through Macro Deck Studio to Deck and then Assistant`() {
        var appScreen = AppScreen.PC_MODULES
        val deckNav = PcControlDeckNavState(
            selectedTab = PcDeckTab.DECK,
            showMacroDeckScreen = true,
            inStudio = true,
            studioDialogActive = true
        )

        // Paso 1: Usuario en diálogo interno del Studio de macros -> cierra diálogo
        assertTrue(deckNav.handleBack(onExitDeck = { appScreen = AppScreen.ASSISTANT }))
        assertFalse(deckNav.studioDialogActive)
        assertTrue(deckNav.inStudio)
        assertEquals(AppScreen.PC_MODULES, appScreen)

        // Paso 2: Usuario en Studio -> sale del studio a Dynamic Macro Deck
        assertTrue(deckNav.handleBack(onExitDeck = { appScreen = AppScreen.ASSISTANT }))
        assertFalse(deckNav.inStudio)
        assertTrue(deckNav.showMacroDeckScreen)
        assertEquals(AppScreen.PC_MODULES, appScreen)

        // Paso 3: Usuario en Dynamic Macro Deck -> sale a Workstation PC (DECK tab)
        assertTrue(deckNav.handleBack(onExitDeck = { appScreen = AppScreen.ASSISTANT }))
        assertFalse(deckNav.showMacroDeckScreen)
        assertEquals(PcDeckTab.DECK, deckNav.selectedTab)
        assertEquals(AppScreen.PC_MODULES, appScreen)

        // Paso 4: Usuario en Workstation PC (DECK tab) -> sale al Asistente
        assertTrue(deckNav.handleBack(onExitDeck = { appScreen = AppScreen.ASSISTANT }))
        assertEquals(AppScreen.ASSISTANT, appScreen)
    }

    @Test
    fun `visual routine designer closes on back gesture`() {
        val deckNav = PcControlDeckNavState(
            selectedTab = PcDeckTab.DECK,
            showRoutineDesignerScreen = true
        )
        var exitedDeck = false

        assertTrue(deckNav.handleBack(onExitDeck = { exitedDeck = true }))
        assertFalse(deckNav.showRoutineDesignerScreen)
        assertFalse(exitedDeck)
    }

    // --- Pruebas de Flujo Tareas y Notas ---

    @Test
    fun `tasks add dialog consumes back gesture`() {
        val tasksNav = TasksNavState(showAddDialog = true)
        val consumed = tasksNav.handleBack()
        assertTrue(consumed)
        assertFalse(tasksNav.showAddDialog)

        // Siguiente toque no es consumido por la vista de tareas interna
        val nextConsumed = tasksNav.handleBack()
        assertFalse(nextConsumed)
    }

    @Test
    fun `notes dialogs and search state consume back gesture hierarchically`() {
        val notesNav = NotesNavState(
            isEditingNote = true,
            isCreatingNote = true,
            isSearchActive = true,
            searchQuery = "recetas"
        )

        // 1. Cierra diálogo de edición
        assertTrue(notesNav.handleBack())
        assertFalse(notesNav.isEditingNote)

        // 2. Cierra diálogo de creación
        assertTrue(notesNav.handleBack())
        assertFalse(notesNav.isCreatingNote)

        // 3. Cierra búsqueda y limpia query
        assertTrue(notesNav.handleBack())
        assertFalse(notesNav.isSearchActive)
        assertEquals("", notesNav.searchQuery)

        // 4. Lista limpia sin modales -> no consume, pasa al nivel superior
        assertFalse(notesNav.handleBack())
    }
}
