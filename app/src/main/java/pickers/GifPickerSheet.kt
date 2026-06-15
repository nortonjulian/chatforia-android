package com.chatforia.android.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatforia.android.tenor.TenorGifDto
import com.chatforia.android.tenor.TenorRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.chatforia.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerSheet(
    tenorRepository: TenorRepository,
    onDismiss: () -> Unit,
    onGifSelected: (TenorGifDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<TenorGifDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadTrending() {
        scope.launch {
            isLoading = true
            error = null

            try {
                gifs = tenorRepository.trendingGifs()
            } catch (e: Exception) {
                error = e.message ?: "Failed to load GIFs."
            } finally {
                isLoading = false
            }
        }
    }

    fun search() {
        val trimmed = query.trim()

        if (trimmed.isEmpty()) {
            loadTrending()
            return
        }

        scope.launch {
            isLoading = true
            error = null

            try {
                gifs = tenorRepository.searchGifs(trimmed)
            } catch (e: Exception) {
                error = e.message ?: "Failed to search GIFs."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTrending()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp, max = 680.dp)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.android_gif_picker_search_gifs),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.android_gif_picker_search_tenor))
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.android_chats_search)
                    )
                },
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            search()
                        }
                    ) {
                        Text(stringResource(R.string.android_chats_search))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gifs) { gif ->
                    AsyncImage(
                        model = gif.previewUrl ?: gif.url,
                        contentDescription = "GIF",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onGifSelected(gif)
                            }
                    )
                }
            }
        }
    }
}