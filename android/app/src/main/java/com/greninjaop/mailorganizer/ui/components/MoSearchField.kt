package com.greninjaop.mailorganizer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.theme.rememberGlassPalette
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * The Gmail-style search pill shared by every primary destination
 * (2026-10-13): a rounded, indicator-free [TextField] with a leading
 * search icon and a trailing clear button. Typing filters locally via
 * [onValueChange]; the keyboard's search action fires [onSearchSubmit]
 * (the mailbox and the other destinations open the dedicated Search
 * screen with the typed query).
 *
 * 2026-10-13 (Breeze redesign): frosted-glass pill — translucent bar
 * color + hairline border, so the background artwork shows through.
 */
@Composable
fun MoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val glass = rememberGlassPalette()
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
        shape = RoundedCornerShape(MoSpacing.xxl),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = glass.bar,
            unfocusedContainerColor = glass.bar,
            disabledContainerColor = glass.bar,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = glass.barBorder,
                shape = RoundedCornerShape(MoSpacing.xxl),
            ),
    )
}
