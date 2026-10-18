package com.example.feature.objetivos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.feature.objetivos.domain.model.GoalFilterTab
import com.example.feature.objetivos.domain.model.GoalSortOption

@Composable
fun GoalFilterBar(
    selectedTab: GoalFilterTab,
    onTabSelected: (GoalFilterTab) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedSort: GoalSortOption,
    onSortSelected: (GoalSortOption) -> Unit,
    availableCategories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    availableAccounts: List<AccountEntity>,
    selectedAccount: String?,
    onAccountSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Horizontal Status Tabs in Activos Dark Blue
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goal_status_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(GoalFilterTab.entries.toTypedArray()) { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) primaryBlue else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) accentCyan else cardBorder
                    )
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) textPrimary else textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Search input + Sort & Dropdown filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Buscar objetivo...", fontSize = 13.sp, color = textSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedIndicatorColor = accentCyan,
                    unfocusedIndicatorColor = cardBorder,
                    cursorColor = accentCyan
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("goal_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort button
            Box {
                Surface(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar",
                            tint = accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    GoalSortOption.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sort.label,
                                    fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sort == selectedSort) primaryBlue else Color.Unspecified
                                )
                            },
                            onClick = {
                                showSortMenu = false
                                onSortSelected(sort)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Advanced filter button (Category & Account)
            Box {
                val hasActiveFilter = selectedCategory != null || selectedAccount != null
                Surface(
                    onClick = { showCategoryMenu = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasActiveFilter) Color(0xFF0F2544) else cardBg,
                    border = BorderStroke(1.dp, if (hasActiveFilter) accentCyan else cardBorder),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilter) accentCyan else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas las categorías", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            showCategoryMenu = false
                            onCategorySelected(null)
                        }
                    )
                    availableCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showCategoryMenu = false
                                onCategorySelected(cat)
                            }
                        )
                    }
                }
            }
        }
    }
}
