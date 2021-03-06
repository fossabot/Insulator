package insulator.views.configurations

import insulator.di.GlobalConfiguration
import insulator.lib.configuration.model.Cluster
import insulator.styles.Controls
import insulator.styles.Titles
import insulator.viewmodel.configurations.ClusterModel
import insulator.viewmodel.configurations.ClusterViewModel
import insulator.viewmodel.configurations.ListClusterViewModel
import insulator.views.common.ICON_SETTINGS_SVG
import insulator.views.main.MainView
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.layout.BorderPane
import tornadofx.* // ktlint-disable no-wildcard-imports

class ListClusterView : View("Insulator") {

    private val viewModel: ListClusterViewModel by inject()

    override val root = vbox(spacing = 15) {
        label("Clusters") { addClass(Titles.h1) }
        listview(viewModel.clustersProperty) {
            cellFormat { cluster ->
                graphic = buildClusterCell(cluster)
                onMouseClicked = EventHandler {
                    GlobalConfiguration.currentCluster = cluster
                    val scope = Scope()
                    setInScope(ClusterViewModel(ClusterModel(cluster)), scope)
                    find<MainView>(scope).openWindow(owner = null)
                    close()
                }
            }
        }
        hbox(alignment = Pos.CENTER_RIGHT) {
            button {
                alignment = Pos.CENTER_RIGHT
                text = "Add new cluster"
                val scope = Scope()
                setInScope(ClusterViewModel(ClusterModel(Cluster.Empty)), scope)
                action { find<ClusterView>(scope).openWindow() }
            }
        }
        addClass(Controls.view)
    }

    private fun buildClusterCell(cluster: Cluster): BorderPane {
        return borderpane {
            maxHeight = 80.0
            center = vbox(alignment = Pos.CENTER_LEFT) {
                label(cluster.name) { addClass(Titles.h2) }
                label(cluster.endpoint) { addClass(Titles.h3) }
            }
            right = vbox(alignment = Pos.CENTER) {
                button {
                    addClass(Controls.iconButton)
                    graphic = SVGIcon(ICON_SETTINGS_SVG, 18)
                    onMouseClicked = EventHandler {
                        val scope = Scope()
                        setInScope(ClusterViewModel(ClusterModel(cluster)), scope)
                        find<ClusterView>(scope).openWindow()
                    }
                }
            }
        }
    }

    override fun onDock() {
        super.onDock()
        setWindowMinSize(380.0, 500.0)
        setWindowMaxSize(380.0, 500.0)
        super.currentStage?.resizableProperty()?.value = false
    }
}
