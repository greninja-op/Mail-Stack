package com.greninjaop.mailstack.tablet.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.greninjaop.mailstack.tablet.mail.TabletMailWorkspace
import com.greninjaop.mailstack.tablet.workspace.TabletActionsPane
import com.greninjaop.mailstack.tablet.workspace.TabletCategoriesPane
import com.greninjaop.mailstack.tablet.workspace.TabletCompaniesPane
import com.greninjaop.mailstack.tablet.workspace.TabletHomePane

/**
 * Primary destinations, same order as the phone bottom bar:
 * Home, Mail, Categories, Companies, Actions.
 *
 * Companies uses AccountBox because material-icons-core has no Business glyph.
 * The label carries the meaning.
 */
enum class TabletDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Filled.Home),
    Mail("Mail", Icons.Filled.Email),
    Categories("Categories", Icons.Filled.List),
    Companies("Companies", Icons.Filled.AccountBox),
    Actions("Actions", Icons.Filled.CheckCircle),
}

/**
 * One destination's body.
 *
 * Mail honors the shell's split. On a narrow window only the list is shown,
 * because [TabletMailWorkspace] places list and detail in a row whenever both
 * flags are true. At 600dp and above the shell's drag handle is the seam
 * between those panes.
 */
@Composable
fun TabletDestinationContent(
    destination: TabletDestination,
    wide: Boolean,
    listFraction: Float,
    showList: Boolean,
    showDetail: Boolean,
) {
    when (destination) {
        TabletDestination.Home -> TabletHomePane()
        TabletDestination.Mail -> TabletMailWorkspace(
            listFraction = if (wide) listFraction else 1f,
            showList = if (wide) showList else true,
            showDetail = if (wide) showDetail else false,
            stackOnSelect = !wide,
        )
        TabletDestination.Categories -> TabletCategoriesPane()
        TabletDestination.Companies -> TabletCompaniesPane()
        TabletDestination.Actions -> TabletActionsPane()
    }
}
