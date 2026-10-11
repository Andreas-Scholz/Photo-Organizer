package io.github.andreasscholz.photoorganizer.ui.folders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.model.FolderFilter

/** Lets the user restrict the review to certain folders, e.g. only the camera folder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSelectionScreen(
    onBack: () -> Unit,
    viewModel: FolderSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter = state.filter
    val allFolders = filter == FolderFilter.AllFolders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_folders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.folders_all)) },
                    supportingContent = { Text(stringResource(R.string.folders_all_summary)) },
                    trailingContent = { Switch(checked = allFolders, onCheckedChange = null) },
                    modifier = Modifier.toggleable(
                        value = allFolders,
                        role = Role.Switch,
                        onValueChange = viewModel::setAllFolders,
                    ),
                )
                HorizontalDivider()
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (state.folders.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.folders_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            items(state.folders, key = { it.relativePath }) { folder ->
                val selected = filter.includes(folder.relativePath)
                ListItem(
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text(folder.relativePath) },
                    leadingContent = {
                        Checkbox(checked = selected, onCheckedChange = null, enabled = !allFolders)
                    },
                    trailingContent = {
                        Text(pluralStringResource(R.plurals.folders_item_count, folder.itemCount, folder.itemCount))
                    },
                    modifier = Modifier.toggleable(
                        value = selected,
                        enabled = !allFolders,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.setFolderSelected(folder, it) },
                    ),
                )
            }
        }
    }
}
