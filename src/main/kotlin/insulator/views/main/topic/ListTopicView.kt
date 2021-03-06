package insulator.views.main.topic

import insulator.viewmodel.main.topic.ListTopicViewModel
import insulator.viewmodel.main.topic.TopicViewModel
import insulator.views.common.searchBox
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.SelectionMode
import javafx.scene.layout.Priority
import tornadofx.* // ktlint-disable no-wildcard-imports

class ListTopicView : View("Topics") {

    private val viewModel: ListTopicViewModel by inject()
    private val searchItem = SimpleStringProperty()

    override val root = vbox(spacing = 5.0) {
        searchBox(searchItem)
        listview<String> {
            cellFormat { graphic = label(it) }
            onDoubleClick {
                if (this.selectedItem == null) return@onDoubleClick
                val scope = Scope()
                tornadofx.setInScope(TopicViewModel(this.selectedItem!!), scope)
                find<TopicView>(scope).openWindow()
            }
            itemsProperty().set(
                SortedFilteredList(viewModel.topicList).apply {
                    filterWhen(searchItem) { p, i -> i.toLowerCase().contains(p.toLowerCase()) }
                }.filteredItems
            )
            selectionModel.selectionMode = SelectionMode.SINGLE
            vgrow = Priority.ALWAYS
        }
    }
}
