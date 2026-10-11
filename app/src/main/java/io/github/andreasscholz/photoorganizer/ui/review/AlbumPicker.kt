package io.github.andreasscholz.photoorganizer.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.model.Album

/** Bottom sheet for choosing an existing album or creating a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPicker(
    albums: List<Album>,
    onAlbumSelected: (Album) -> Unit,
    onDismiss: () -> Unit,
) {
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.album_picker_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.album_new)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    colors = itemColors,
                    modifier = Modifier.clickable { showNewAlbumDialog = true },
                )
                HorizontalDivider()
            }
            items(albums, key = { it.relativePath }) { album ->
                ListItem(
                    headlineContent = { Text(album.name) },
                    supportingContent = { Text(album.relativePath) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
                    colors = itemColors,
                    modifier = Modifier.clickable { onAlbumSelected(album) },
                )
            }
        }
    }

    if (showNewAlbumDialog) {
        NewAlbumDialog(
            onCreate = { name ->
                showNewAlbumDialog = false
                onAlbumSelected(Album.create(name))
            },
            onDismiss = { showNewAlbumDialog = false },
        )
    }
}

@Composable
private fun NewAlbumDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.album_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.album_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = Album.isValidName(name)) {
                Text(stringResource(R.string.album_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
