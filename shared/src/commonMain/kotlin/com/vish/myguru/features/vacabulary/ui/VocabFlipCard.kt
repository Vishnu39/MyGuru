package com.vish.myguru.features.vacabulary.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vish.myguru.features.vacabulary.model.Word

@Composable
fun VocabFlipCard(
    word: Word,
    modifier: Modifier = Modifier
) {
    // 1. Internal state tracking whether the card is face-up or face-down
    var isFlipped by remember { mutableStateOf(false) }

    // 2. Animate rotation angle from 0f (front) to 180f (back)
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "card_flip_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .graphicsLayer {
                rotationY = rotation
                // Camera distance creates simulated 3D perspective depth.
                // 12f * density matches a standard mobile focal length.
                cameraDistance = 12f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Disables ripple so the mechanical flip feels snappy
            ) {
                isFlipped = !isFlipped
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        // 3. Swap content at the 90-degree midpoint
        if (rotation <= 90f) {
            // FRONT FACE: German term + Article
            FrontCardFace(word = word)
        } else {
            // BACK FACE: English translation + Box details
            // Rotate back face by 180 degrees so text is not mirrored
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                BackCardFace(word = word)
            }
        }
    }
}

@Composable
private fun FrontCardFace(word: Word) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row hint
        Text(
            text = "Box ${word.boxLevel}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Center Word
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val fullGerman = if (!word.article.isNullOrBlank()) {
                "${word.article} ${word.germanTerm}"
            } else {
                word.germanTerm
            }

            Text(
                text = fullGerman,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bottom Tap Hint
        Text(
            text = "Tap to reveal translation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun BackCardFace(word: Word) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top label indicating back face
        Text(
            text = "Bedeutung (Meaning)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        // Center Translation
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = word.englishTranslation,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!word.article.isNullOrBlank()) {
                Text(
                    text = "Artikel: ${word.article}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Bottom Swipe Instructions
        Text(
            text = "← Repeat | Got it →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}