package com.mafia.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mafia.ui.theme.*

@Composable
fun HomeScreen(onSinglePlayer: () -> Unit, onMultiplayer: () -> Unit, onHowToPlay: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF07051A), Color(0xFF130D38), Color(0xFF07051A)))),
        contentAlignment = Alignment.Center
    ) {
        // Background glow behind logo
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .background(Brush.radialGradient(listOf(MafiaPurple.copy(0.15f), Color.Transparent)))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo mark — circle with border + glow
            Box(
                Modifier
                    .size(108.dp)
                    .scale(pulse)
                    .background(
                        Brush.radialGradient(
                            listOf(MafiaPurple.copy(0.4f), Color(0xFF1A0A3E).copy(0.7f))
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, MafiaPurple.copy(0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎭", fontSize = 50.sp)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "MAFIA",
                fontSize = 46.sp, fontWeight = FontWeight.Black,
                color = Color.White, letterSpacing = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Social Deduction Game",
                fontSize = 13.sp, color = MafiaPurple.copy(0.75f),
                letterSpacing = 3.sp, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(52.dp))

            Button(
                onClick = onSinglePlayer,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MafiaPurple),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("🕵️", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text("Single Player", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onMultiplayer,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("👥", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text("Play with Friends", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(28.dp))

            TextButton(onClick = onHowToPlay) {
                Text("How to Play  →", color = Color.White.copy(0.45f), fontSize = 13.sp, letterSpacing = 1.sp)
            }
        }
    }
}
